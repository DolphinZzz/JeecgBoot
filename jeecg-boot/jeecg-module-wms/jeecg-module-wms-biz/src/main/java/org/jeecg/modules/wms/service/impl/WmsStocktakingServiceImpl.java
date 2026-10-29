package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.entity.WmsStocktakingInfo;
import org.jeecg.modules.wms.entity.WmsStocktakingLoss;
import org.jeecg.modules.wms.entity.WmsWarehouseArea;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.enums.WmsInventoryBizTypeEnum;
import org.jeecg.modules.wms.enums.WmsStocktakingStatusEnum;
import org.jeecg.modules.wms.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.mapper.WmsStocktakingInfoMapper;
import org.jeecg.modules.wms.mapper.WmsStocktakingLossMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.service.IWmsInventoryService;
import org.jeecg.modules.wms.service.IWmsStocktakingService;
import org.jeecg.modules.wms.vo.WmsInventoryDimension;
import org.jeecg.modules.wms.vo.WmsStocktakingCountForm;
import org.jeecg.modules.wms.vo.WmsStocktakingCreateForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description: 盘点作业
 *
 * 盘点范围来自 wms_inventory（只盘有库存的维度），生成时把账面数快照进 book_count，
 * 因此生成后到结案之间即便有出入库，也以快照为准，差异可追溯。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Slf4j
@Service
public class WmsStocktakingServiceImpl extends ServiceImpl<WmsStocktakingInfoMapper, WmsStocktakingInfo>
        implements IWmsStocktakingService {

    /** 差异审批：无差异 */
    private static final int AUDIT_NONE = 0;
    /** 差异审批：待审批 */
    private static final int AUDIT_PENDING = 1;
    /** 差异审批：已通过 */
    private static final int AUDIT_PASSED = 2;
    /** 差异审批：已驳回 */
    private static final int AUDIT_REJECTED = 3;
    /** 盘点类型：滚动盘点 */
    private static final int CATEGORY_ROLLING = 1;
    /** 损溢类型：盘亏 */
    private static final int LOSS_TYPE_SHORTAGE = 1;
    /** 损溢类型：盘盈 */
    private static final int LOSS_TYPE_SURPLUS = 2;
    /** 损溢分类：差异（不细分破损/丢失）*/
    private static final int LOSS_CATEGORY_DIFFERENCE = 1;
    /** 库位状态：盘点锁定 */
    private static final int LOCA_STATUS_STOCKTAKING = 3;
    /** 库位状态：占用 */
    private static final int LOCA_STATUS_OCCUPIED = 2;

    @Autowired
    private WmsInventoryMapper inventoryMapper;

    @Autowired
    private WmsStocktakingLossMapper lossMapper;

    @Autowired
    private WmsWarehouseAreaMapper areaMapper;

    @Autowired
    private WmsWarehouseAreaLocaMapper locaMapper;

    @Autowired
    private IWmsInventoryService inventoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createStocktaking(WmsStocktakingCreateForm form) {
        if (form == null || form.getWarehouseId() == null) {
            throw new JeecgBootException("请指定盘点仓库");
        }
        String batchNo = "ST" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        List<WmsInventory> rows = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                .eq(WmsInventory::getWarehouseId, form.getWarehouseId())
                .gt(WmsInventory::getTotalCount, 0));

        // 按库区 / 库位范围过滤
        if (form.getWarehouseAreaId() != null) {
            rows = rows.stream()
                    .filter(r -> form.getWarehouseAreaId().equals(r.getWarehouseAreaId()))
                    .collect(Collectors.toList());
        }
        if (form.getLocaIds() != null && !form.getLocaIds().isEmpty()) {
            rows = rows.stream()
                    .filter(r -> form.getLocaIds().contains(r.getWarehouseAreaLocaId()))
                    .collect(Collectors.toList());
        }
        if (rows.isEmpty()) {
            throw new JeecgBootException("所选范围内没有库存，无需盘点");
        }

        int category = form.getCategory() == null ? CATEGORY_ROLLING : form.getCategory();
        Date now = new Date();
        List<WmsStocktakingInfo> records = new ArrayList<>(rows.size());
        for (WmsInventory row : rows) {
            records.add(new WmsStocktakingInfo()
                    .setBatchNo(batchNo)
                    .setTargetWarehouseId(row.getWarehouseId())
                    .setTargetWarehouseAreaId(row.getWarehouseAreaId())
                    .setTargetWarehouseAreaLocaId(row.getWarehouseAreaLocaId())
                    .setSkuId(row.getSkuId())
                    .setConsignorId(row.getConsignorId())
                    .setBatchNumber(row.getBatchNumber())
                    .setExpireDate(row.getExpireDate())
                    .setQualityStatus(row.getQualityStatus())
                    .setBookCount(row.getTotalCount() == null ? 0 : row.getTotalCount())
                    .setCorrectCount(null)
                    .setDiscrepancyCount(null)
                    .setAuditStatus(AUDIT_NONE)
                    .setCategory(category)
                    .setStatus(WmsStocktakingStatusEnum.PENDING.getCode())
                    .setCreateTime(now)
                    .setIsDel(1));
        }
        this.saveBatch(records);

        // 库位加盘点锁，防止盘点期间被移入 / 移出
        lockLocas(rows.stream().map(WmsInventory::getWarehouseAreaLocaId).distinct().collect(Collectors.toList()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchNo", batchNo);
        result.put("recordCount", records.size());
        result.put("message", "已生成盘点批次 " + batchNo + "，共 " + records.size() + " 条盘点记录，相关库位已锁定");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitCount(WmsStocktakingCountForm form) {
        if (form == null || form.getId() == null || form.getCorrectCount() == null) {
            throw new JeecgBootException("请填写实盘数量");
        }
        if (form.getCorrectCount() < 0) {
            throw new JeecgBootException("实盘数量不能为负数");
        }
        WmsStocktakingInfo info = this.getById(form.getId());
        if (info == null) {
            throw new JeecgBootException("盘点记录不存在");
        }
        if (info.getStatus() != null && info.getStatus() == WmsStocktakingStatusEnum.CONFIRMED.getCode()) {
            throw new JeecgBootException("该记录已确认结案，不能再修改实盘数");
        }
        int book = info.getBookCount() == null ? 0 : info.getBookCount();
        int discrepancy = form.getCorrectCount() - book;
        info.setCorrectCount(form.getCorrectCount())
                .setDiscrepancyCount(discrepancy)
                .setAuditStatus(discrepancy == 0 ? AUDIT_NONE : AUDIT_PENDING)
                .setStatus(WmsStocktakingStatusEnum.COUNTED.getCode())
                .setUpdateTime(new Date());
        this.updateById(info);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, boolean pass, String reason) {
        WmsStocktakingInfo info = this.getById(id);
        if (info == null) {
            throw new JeecgBootException("盘点记录不存在");
        }
        if (info.getAuditStatus() == null || info.getAuditStatus() != AUDIT_PENDING) {
            throw new JeecgBootException("只有「待审批」的差异记录可以审批");
        }
        info.setAuditStatus(pass ? AUDIT_PASSED : AUDIT_REJECTED)
                .setAuditBy(currentUsername())
                .setAuditTime(new Date())
                .setUpdateTime(new Date());
        if (oConvertUtils.isNotEmpty(reason)) {
            // 审批意见无独立列，追加到备注之外不落库；驳回原因由前端展示在操作日志中
            log.info("盘点记录 {} 审批：{}，原因：{}", id, pass ? "通过" : "驳回", reason);
        }
        this.updateById(info);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirm(Long id) {
        WmsStocktakingInfo info = this.getById(id);
        if (info == null) {
            throw new JeecgBootException("盘点记录不存在");
        }
        if (info.getStatus() != null && info.getStatus() == WmsStocktakingStatusEnum.CONFIRMED.getCode()) {
            throw new JeecgBootException("该记录已确认结案");
        }
        if (info.getStatus() == null || info.getStatus() != WmsStocktakingStatusEnum.COUNTED.getCode()) {
            throw new JeecgBootException("请先录入实盘数量再结案");
        }
        if (info.getCorrectCount() == null) {
            throw new JeecgBootException("该记录尚未录入实盘数量");
        }
        int discrepancy = info.getDiscrepancyCount() == null ? 0 : info.getDiscrepancyCount();
        if (discrepancy != 0 && info.getAuditStatus() != null && info.getAuditStatus() == AUDIT_PENDING) {
            throw new JeecgBootException("存在差异，请先完成差异审批");
        }
        if (discrepancy != 0 && info.getAuditStatus() != null && info.getAuditStatus() == AUDIT_REJECTED) {
            throw new JeecgBootException("差异已被驳回，不能按实盘数调账；如需调整请重新盘点");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        int surplus = discrepancy > 0 ? discrepancy : 0;
        int shortage = discrepancy < 0 ? -discrepancy : 0;
        if (discrepancy != 0) {
            WmsInventoryDimension dim = new WmsInventoryDimension()
                    .setWarehouseId(info.getTargetWarehouseId())
                    .setWarehouseAreaId(info.getTargetWarehouseAreaId())
                    .setWarehouseAreaLocaId(info.getTargetWarehouseAreaLocaId())
                    .setConsignorId(info.getConsignorId())
                    .setSkuId(info.getSkuId())
                    .setBatchNumber(info.getBatchNumber())
                    .setQualityStatus(info.getQualityStatus());
            inventoryService.adjustToTotal(dim, info.getCorrectCount(), WmsInventoryBizTypeEnum.STOCKTAKING_ADJUST,
                    info.getBatchNo(), "盘点调账：" + (discrepancy > 0 ? "盘盈 " : "盘亏 ") + Math.abs(discrepancy));

            // 损溢记录：盘盈 / 盘亏各一条
            WmsStocktakingLoss loss = new WmsStocktakingLoss()
                    .setLossCount(Math.abs(discrepancy))
                    .setLossCategory(LOSS_CATEGORY_DIFFERENCE)
                    .setLossType(discrepancy > 0 ? LOSS_TYPE_SURPLUS : LOSS_TYPE_SHORTAGE)
                    .setStocktakingInfo(info.getId())
                    .setBatchNumber(info.getBatchNumber())
                    .setCreateTime(new Date())
                    .setIsDel(1);
            lossMapper.insert(loss);
        }

        info.setStatus(WmsStocktakingStatusEnum.CONFIRMED.getCode())
                .setUpdateTime(new Date());
        this.updateById(info);

        // 同批次全部结案后释放库位盘点锁
        releaseLockIfBatchDone(info.getBatchNo());

        result.put("id", info.getId());
        result.put("surplusCount", surplus);
        result.put("shortageCount", shortage);
        result.put("message", discrepancy == 0 ? "无差异，已结案" : "差异 " + discrepancy + "，已调账并生成损溢记录");
        return result;
    }

    @Override
    public IPage<WmsStocktakingInfo> queryByBatch(String batchNo, String status, Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<WmsStocktakingInfo> query = new LambdaQueryWrapper<WmsStocktakingInfo>()
                .orderByDesc(WmsStocktakingInfo::getId);
        if (oConvertUtils.isNotEmpty(batchNo)) {
            query.eq(WmsStocktakingInfo::getBatchNo, batchNo);
        }
        if (oConvertUtils.isNotEmpty(status)) {
            query.eq(WmsStocktakingInfo::getStatus, Integer.parseInt(status));
        }
        return this.page(new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), query);
    }

    /**
     * 盘点库位加锁。占用中的库位置为「盘点锁定」，空库位保持原状态。
     */
    private void lockLocas(List<Integer> locaIds) {
        for (Integer locaId : locaIds) {
            WmsWarehouseAreaLoca loca = locaMapper.selectById(locaId);
            if (loca == null) {
                continue;
            }
            if (loca.getLocaStatus() != null && loca.getLocaStatus() == LOCA_STATUS_STOCKTAKING) {
                continue;
            }
            locaMapper.updateById(new WmsWarehouseAreaLoca().setId(locaId).setLocaStatus(LOCA_STATUS_STOCKTAKING));
        }
    }

    /**
     * 同批次全部结案后，把仍处于盘点锁定的库位释放回占用。
     */
    private void releaseLockIfBatchDone(String batchNo) {
        long pending = this.count(new LambdaQueryWrapper<WmsStocktakingInfo>()
                .eq(WmsStocktakingInfo::getBatchNo, batchNo)
                .ne(WmsStocktakingInfo::getStatus, WmsStocktakingStatusEnum.CONFIRMED.getCode()));
        if (pending > 0) {
            return;
        }
        List<WmsStocktakingInfo> all = this.list(new LambdaQueryWrapper<WmsStocktakingInfo>()
                .eq(WmsStocktakingInfo::getBatchNo, batchNo));
        List<Integer> locaIds = all.stream().map(WmsStocktakingInfo::getTargetWarehouseAreaLocaId)
                .distinct().collect(Collectors.toList());
        for (Integer locaId : locaIds) {
            WmsWarehouseAreaLoca loca = locaMapper.selectById(locaId);
            if (loca == null || loca.getLocaStatus() == null || loca.getLocaStatus() != LOCA_STATUS_STOCKTAKING) {
                continue;
            }
            locaMapper.updateById(new WmsWarehouseAreaLoca().setId(locaId).setLocaStatus(LOCA_STATUS_OCCUPIED));
        }
    }

    private String currentUsername() {
        try {
            Object principal = org.apache.shiro.SecurityUtils.getSubject().getPrincipal();
            if (principal instanceof org.jeecg.common.system.vo.LoginUser) {
                return ((org.jeecg.common.system.vo.LoginUser) principal).getUsername();
            }
        } catch (Exception e) {
            log.debug("获取登录用户失败", e);
        }
        return null;
    }
}
