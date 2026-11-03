package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.entity.WmsItemSpu;
import org.jeecg.modules.wms.entity.WmsLocaItemCapacity;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.mapper.WmsItemSkuMapper;
import org.jeecg.modules.wms.mapper.WmsItemSpuMapper;
import org.jeecg.modules.wms.mapper.WmsLocaItemCapacityMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.service.IWmsLocaItemCapacityService;
import org.jeecg.modules.wms.vo.WmsLocaCapacityItemVO;
import org.jeecg.modules.wms.vo.WmsLocaCapacitySaveDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 库位商品规划与容量设置服务实现
 * @Author: jeecg-boot
 * @Date: 2026-11-01
 * @Version: V1.0
 */
@Slf4j
@Service
public class WmsLocaItemCapacityServiceImpl extends ServiceImpl<WmsLocaItemCapacityMapper, WmsLocaItemCapacity>
        implements IWmsLocaItemCapacityService {

    private static final int MAX_SPU_PER_LOCA = 3;

    @Autowired
    private WmsWarehouseAreaLocaMapper locaMapper;

    @Autowired
    private WmsItemSpuMapper spuMapper;

    @Autowired
    private WmsItemSkuMapper skuMapper;

    @Autowired
    private WmsInventoryMapper inventoryMapper;

    @Override
    public List<WmsLocaCapacityItemVO> listCapacityByLocaId(Integer locaId) {
        if (locaId == null) {
            return Collections.emptyList();
        }
        List<WmsLocaItemCapacity> list = baseMapper.selectByLocaId(locaId);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        return buildCapacityItemVOs(locaId, list);
    }

    @Override
    public Map<Integer, List<WmsLocaCapacityItemVO>> mapCapacityByLocaIds(List<Integer> locaIds) {
        if (locaIds == null || locaIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<WmsLocaItemCapacity> allCapacities = baseMapper.selectByLocaIds(locaIds);
        if (allCapacities.isEmpty()) {
            return Collections.emptyMap();
        }

        // 按库位分组
        Map<Integer, List<WmsLocaItemCapacity>> grouped = allCapacities.stream()
                .collect(Collectors.groupingBy(WmsLocaItemCapacity::getWarehouseAreaLocaId));

        // 收集所有用到的 SPU
        Set<Long> spuIds = allCapacities.stream().map(WmsLocaItemCapacity::getSpuId).collect(Collectors.toSet());
        Map<Long, WmsItemSpu> spuMap = spuIds.isEmpty() ? Collections.emptyMap() :
                spuMapper.selectBatchIds(spuIds).stream().collect(Collectors.toMap(WmsItemSpu::getId, s -> s));

        // 查出这些库位上的所有在库库存以汇总每个 SPU 当前在库数量
        List<WmsInventory> inventories = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                .in(WmsInventory::getWarehouseAreaLocaId, locaIds)
                .gt(WmsInventory::getTotalCount, 0));

        // 建立 skuId -> spuId 映射
        Set<Long> skuIds = inventories.stream().map(WmsInventory::getSkuId).collect(Collectors.toSet());
        Map<Long, Long> skuToSpuMap = new HashMap<>();
        if (!skuIds.isEmpty()) {
            List<WmsItemSku> skus = skuMapper.selectBatchIds(skuIds);
            for (WmsItemSku sku : skus) {
                skuToSpuMap.put(sku.getId(), sku.getSpuId());
            }
        }

        // 统计 (locaId + "_" + spuId) -> currentCount
        Map<String, Integer> locaSpuStockMap = new HashMap<>();
        for (WmsInventory inv : inventories) {
            Long spuId = skuToSpuMap.get(inv.getSkuId());
            if (spuId != null && inv.getTotalCount() != null && inv.getTotalCount() > 0) {
                String key = inv.getWarehouseAreaLocaId() + "_" + spuId;
                locaSpuStockMap.merge(key, inv.getTotalCount(), Integer::sum);
            }
        }

        Map<Integer, List<WmsLocaCapacityItemVO>> resultMap = new HashMap<>();
        for (Map.Entry<Integer, List<WmsLocaItemCapacity>> entry : grouped.entrySet()) {
            Integer lId = entry.getKey();
            List<WmsLocaCapacityItemVO> voList = new ArrayList<>();
            for (WmsLocaItemCapacity cap : entry.getValue()) {
                WmsItemSpu spu = spuMap.get(cap.getSpuId());
                int current = locaSpuStockMap.getOrDefault(lId + "_" + cap.getSpuId(), 0);
                int remain = Math.max(0, cap.getMaxCount() - current);
                voList.add(new WmsLocaCapacityItemVO()
                        .setId(cap.getId())
                        .setSpuId(cap.getSpuId())
                        .setSpuCode(spu != null ? spu.getSubTitle() : "")
                        .setSpuName(spu != null ? spu.getTitle() : ("商品" + cap.getSpuId()))
                        .setMaxCount(cap.getMaxCount())
                        .setCurrentCount(current)
                        .setRemainCount(remain)
                        .setRemark(cap.getRemark()));
            }
            resultMap.put(lId, voList);
        }
        return resultMap;
    }

    private List<WmsLocaCapacityItemVO> buildCapacityItemVOs(Integer locaId, List<WmsLocaItemCapacity> list) {
        Set<Long> spuIds = list.stream().map(WmsLocaItemCapacity::getSpuId).collect(Collectors.toSet());
        Map<Long, WmsItemSpu> spuMap = spuIds.isEmpty() ? Collections.emptyMap() :
                spuMapper.selectBatchIds(spuIds).stream().collect(Collectors.toMap(WmsItemSpu::getId, s -> s));

        // 查此库位在库库存
        List<WmsInventory> inventories = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                .eq(WmsInventory::getWarehouseAreaLocaId, locaId)
                .gt(WmsInventory::getTotalCount, 0));

        Set<Long> skuIds = inventories.stream().map(WmsInventory::getSkuId).collect(Collectors.toSet());
        Map<Long, Long> skuToSpuMap = new HashMap<>();
        if (!skuIds.isEmpty()) {
            List<WmsItemSku> skus = skuMapper.selectBatchIds(skuIds);
            for (WmsItemSku sku : skus) {
                skuToSpuMap.put(sku.getId(), sku.getSpuId());
            }
        }

        Map<Long, Integer> spuStockMap = new HashMap<>();
        for (WmsInventory inv : inventories) {
            Long spuId = skuToSpuMap.get(inv.getSkuId());
            if (spuId != null && inv.getTotalCount() != null && inv.getTotalCount() > 0) {
                spuStockMap.merge(spuId, inv.getTotalCount(), Integer::sum);
            }
        }

        List<WmsLocaCapacityItemVO> vos = new ArrayList<>();
        for (WmsLocaItemCapacity cap : list) {
            WmsItemSpu spu = spuMap.get(cap.getSpuId());
            int current = spuStockMap.getOrDefault(cap.getSpuId(), 0);
            int remain = Math.max(0, cap.getMaxCount() - current);
            vos.add(new WmsLocaCapacityItemVO()
                    .setId(cap.getId())
                    .setSpuId(cap.getSpuId())
                    .setSpuCode(spu != null ? spu.getSubTitle() : "")
                    .setSpuName(spu != null ? spu.getTitle() : ("商品" + cap.getSpuId()))
                    .setMaxCount(cap.getMaxCount())
                    .setCurrentCount(current)
                    .setRemainCount(remain)
                    .setRemark(cap.getRemark()));
        }
        return vos;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLocaCapacity(WmsLocaCapacitySaveDTO dto) {
        if (dto == null || dto.getLocaId() == null) {
            throw new JeecgBootException("请指定目标库位");
        }
        WmsWarehouseAreaLoca loca = locaMapper.selectById(dto.getLocaId());
        if (loca == null) {
            throw new JeecgBootException("库位不存在");
        }

        List<WmsLocaCapacitySaveDTO.CapacityItem> items = dto.getItems() == null ? Collections.emptyList() : dto.getItems();

        // 核心约束 1：一个库位最多只能配置 3 个 SPU
        long distinctSpuCount = items.stream()
                .filter(i -> i.getSpuId() != null)
                .map(WmsLocaCapacitySaveDTO.CapacityItem::getSpuId)
                .distinct()
                .count();

        if (distinctSpuCount > MAX_SPU_PER_LOCA) {
            throw new JeecgBootException("单个库位最多只能配置 " + MAX_SPU_PER_LOCA + " 个商品(SPU)！");
        }

        // 核心约束 2：容量必须 > 0
        for (WmsLocaCapacitySaveDTO.CapacityItem item : items) {
            if (item.getSpuId() == null) {
                throw new JeecgBootException("请选择规划存放的商品");
            }
            if (item.getMaxCount() == null || item.getMaxCount() <= 0) {
                throw new JeecgBootException("商品最大可存放件数必须大于0");
            }
        }

        String username = currentUsername();
        Date now = new Date();

        // 逻辑删除该库位已有的规划配置
        baseMapper.delete(new LambdaUpdateWrapper<WmsLocaItemCapacity>()
                .eq(WmsLocaItemCapacity::getWarehouseAreaLocaId, loca.getId()));

        // 插入新的规划项
        Set<Long> seenSpus = new HashSet<>();
        for (WmsLocaCapacitySaveDTO.CapacityItem item : items) {
            if (!seenSpus.add(item.getSpuId())) {
                continue; // 去重
            }
            WmsLocaItemCapacity cap = new WmsLocaItemCapacity()
                    .setWarehouseId(loca.getWarehouseId())
                    .setWarehouseAreaId(loca.getWarehouseAreaId())
                    .setWarehouseAreaLocaId(loca.getId())
                    .setSpuId(item.getSpuId())
                    .setMaxCount(item.getMaxCount())
                    .setRemark(item.getRemark())
                    .setCreateBy(username)
                    .setCreateTime(now)
                    .setUpdateBy(username)
                    .setUpdateTime(now)
                    .setIsDel(1);
            baseMapper.insert(cap);
        }

        // 若配置了多个SPU，自动将该库位的 allowMixSku 置为 1 (允许混SKU)
        if (distinctSpuCount > 1 && (loca.getAllowMixSku() == null || loca.getAllowMixSku() != 1)) {
            locaMapper.updateById(new WmsWarehouseAreaLoca()
                    .setId(loca.getId())
                    .setAllowMixSku(1)
                    .setUpdateTime(now)
                    .setUpdateBy(username));
        }
    }

    @Override
    public WmsLocaItemCapacity getByLocaAndSpu(Integer locaId, Long spuId) {
        if (locaId == null || spuId == null) {
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<WmsLocaItemCapacity>()
                .eq(WmsLocaItemCapacity::getWarehouseAreaLocaId, locaId)
                .eq(WmsLocaItemCapacity::getSpuId, spuId)
                .eq(WmsLocaItemCapacity::getIsDel, 1)
                .last("LIMIT 1"));
    }

    @Override
    public List<WmsLocaItemCapacity> listByWarehouseAndSpu(Integer warehouseId, Long spuId) {
        if (spuId == null) {
            return Collections.emptyList();
        }
        return baseMapper.selectByWarehouseAndSpu(warehouseId, spuId);
    }

    private String currentUsername() {
        try {
            Object principal = SecurityUtils.getSubject().getPrincipal();
            if (principal instanceof LoginUser) {
                return ((LoginUser) principal).getUsername();
            }
        } catch (Exception e) {
            log.debug("获取当前登录用户失败", e);
        }
        return "system";
    }
}
