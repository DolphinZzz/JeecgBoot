package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.entity.WmsWarehouseArea;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.service.IWmsLocaScoreService;
import org.jeecg.modules.wms.service.IWmsStrategyConfigService;
import org.jeecg.modules.wms.service.IWmsWarehouseAreaLocaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 库位 ABC 等级与分值计算实现
 * 依据公式：S_i = w_d * (1 - d_i / d_max) + w_e * E_i
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
@Slf4j
@Service
public class WmsLocaScoreServiceImpl implements IWmsLocaScoreService {

    @Autowired
    private WmsWarehouseAreaMapper warehouseAreaMapper;

    @Autowired
    private WmsWarehouseAreaLocaMapper warehouseAreaLocaMapper;

    @Autowired
    private IWmsWarehouseAreaLocaService warehouseAreaLocaService;

    @Autowired
    private IWmsStrategyConfigService strategyConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recalculateWarehouseLocaScore(Integer warehouseId) {
        if (warehouseId == null) {
            throw new JeecgBootException("请指定需要计算库位等级的仓库ID");
        }

        // 1. 获取权重及比例策略配置
        double wd = strategyConfigService.getDoubleConfig(warehouseId, "loca_weight_distance", 0.6);
        double we = strategyConfigService.getDoubleConfig(warehouseId, "loca_weight_ergonomic", 0.4);
        double ratioA = strategyConfigService.getDoubleConfig(warehouseId, "loca_a_ratio", 0.20);
        double ratioB = strategyConfigService.getDoubleConfig(warehouseId, "loca_b_ratio", 0.30);

        // 保证权重合法
        if (wd + we <= 0) {
            wd = 0.6;
            we = 0.4;
        }

        // 2. 只有 STORAGE / PICKING 库区的库位参与计算
        List<WmsWarehouseArea> eligibleAreas = warehouseAreaMapper.selectList(new LambdaQueryWrapper<WmsWarehouseArea>()
                .eq(WmsWarehouseArea::getWarehouseId, warehouseId)
                .in(WmsWarehouseArea::getCategory, Arrays.asList("STORAGE", "PICKING")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("warehouseId", warehouseId);
        result.put("weightDistance", wd);
        result.put("weightErgonomic", we);
        result.put("ratioA", ratioA);
        result.put("ratioB", ratioB);

        if (eligibleAreas.isEmpty()) {
            result.put("totalCandidateLocations", 0);
            result.put("message", "当前仓库无存储区(STORAGE)或拣货区(PICKING)，未执行评分计算");
            return result;
        }

        Set<Integer> areaIds = eligibleAreas.stream().map(WmsWarehouseArea::getId).collect(Collectors.toSet());

        List<WmsWarehouseAreaLoca> candidateLocas = warehouseAreaLocaMapper.selectList(new LambdaQueryWrapper<WmsWarehouseAreaLoca>()
                .eq(WmsWarehouseAreaLoca::getWarehouseId, warehouseId)
                .in(WmsWarehouseAreaLoca::getWarehouseAreaId, areaIds));

        if (candidateLocas.isEmpty()) {
            result.put("totalCandidateLocations", 0);
            result.put("message", "当前仓库合格库区下暂无库位数据");
            return result;
        }

        // 3. 计算同仓库参与计算的库位中最大的拣货距离 d_max
        int dMax = 0;
        for (WmsWarehouseAreaLoca loca : candidateLocas) {
            int d = loca.getPickDistance() == null ? 0 : Math.max(0, loca.getPickDistance());
            if (d > dMax) {
                dMax = d;
            }
        }
        result.put("maxDistance", dMax);

        // 4. 计算每个库位的得分 S_i
        class LocaScoreHolder {
            final WmsWarehouseAreaLoca loca;
            final double score;

            LocaScoreHolder(WmsWarehouseAreaLoca loca, double score) {
                this.loca = loca;
                this.score = score;
            }
        }

        List<LocaScoreHolder> scoredList = new ArrayList<>(candidateLocas.size());
        for (WmsWarehouseAreaLoca loca : candidateLocas) {
            int di = loca.getPickDistance() == null ? 0 : Math.max(0, loca.getPickDistance());
            double distFactor = 1.0;
            if (dMax > 0) {
                distFactor = 1.0 - ((double) di / dMax);
                if (distFactor < 0) {
                    distFactor = 0;
                }
            }

            // 人体工学系数 E_i: 1 黄金层 1.0; 2 次优层 0.6; 3 不便层 0.2
            double ei = getErgonomicFactor(loca.getErgonomicLevel());

            double score = (wd * distFactor) + (we * ei);
            scoredList.add(new LocaScoreHolder(loca, score));
        }

        // 5. 按得分降序排列；得分相同按库位编号升序
        scoredList.sort((o1, o2) -> {
            int cmp = Double.compare(o2.score, o1.score);
            if (cmp != 0) {
                return cmp;
            }
            String c1 = o1.loca.getLocaCode() == null ? "" : o1.loca.getLocaCode();
            String c2 = o2.loca.getLocaCode() == null ? "" : o2.loca.getLocaCode();
            return c1.compareTo(c2);
        });

        int totalCount = scoredList.size();
        int countA = (int) Math.round(totalCount * ratioA);
        int countB = (int) Math.round(totalCount * ratioB);

        // 边界调整
        if (countA <= 0 && totalCount > 0 && ratioA > 0) {
            countA = 1;
        }
        if (countA + countB > totalCount) {
            countB = totalCount - countA;
        }
        int countC = Math.max(0, totalCount - countA - countB);

        List<WmsWarehouseAreaLoca> updates = new ArrayList<>(totalCount);
        for (int i = 0; i < totalCount; i++) {
            LocaScoreHolder holder = scoredList.get(i);
            WmsWarehouseAreaLoca updateEntity = new WmsWarehouseAreaLoca();
            updateEntity.setId(holder.loca.getId());
            updateEntity.setLocaScore(BigDecimal.valueOf(holder.score).setScale(4, RoundingMode.HALF_UP));

            if (i < countA) {
                updateEntity.setAbcClass("A");
            } else if (i < countA + countB) {
                updateEntity.setAbcClass("B");
            } else {
                updateEntity.setAbcClass("C");
            }
            updates.add(updateEntity);
        }

        // 批量回写数据库
        warehouseAreaLocaService.updateBatchById(updates);

        result.put("totalCandidateLocations", totalCount);
        result.put("countA", countA);
        result.put("countB", countB);
        result.put("countC", countC);
        result.put("message", String.format("成功重算 %d 个库位等级：A类 %d 个，B类 %d 个，C类 %d 个", totalCount, countA, countB, countC));
        log.info("Warehouse {} recalculateLocaScore completed: total={}, A={}, B={}, C={}", warehouseId, totalCount, countA, countB, countC);
        return result;
    }

    private double getErgonomicFactor(Integer ergonomicLevel) {
        if (ergonomicLevel == null) {
            return 0.6; // 默认次优层
        }
        switch (ergonomicLevel) {
            case 1:
                return 1.0; // 黄金层
            case 2:
                return 0.6; // 次优层
            case 3:
                return 0.2; // 不便层
            default:
                return 0.6;
        }
    }
}
