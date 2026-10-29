package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.entity.WmsItemSn;
import org.jeecg.modules.wms.entity.WmsItemSpu;
import org.jeecg.modules.wms.entity.WmsQualityCheck;
import org.jeecg.modules.wms.entity.WmsWarehouse;
import org.jeecg.modules.wms.entity.WmsWarehouseArea;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.entity.WmsWarehousingEntry;
import org.jeecg.modules.wms.entity.WmsWarehousingEntryDetails;
import org.jeecg.modules.wms.entity.WmsWarehousingEntryDetailsSort;
import org.jeecg.modules.wms.entity.WmsWarehousingEntryUp;
import org.jeecg.modules.wms.enums.WmsEntryStatusEnum;
import org.jeecg.modules.wms.enums.WmsInventoryBizTypeEnum;
import org.jeecg.modules.wms.enums.WmsOverReceiveStatusEnum;
import org.jeecg.modules.wms.enums.WmsQcStatusEnum;
import org.jeecg.modules.wms.enums.WmsQcTypeEnum;
import org.jeecg.modules.wms.enums.WmsWarehouseTypeEnum;
import org.jeecg.modules.wms.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.mapper.WmsItemSkuMapper;
import org.jeecg.modules.wms.mapper.WmsItemSnMapper;
import org.jeecg.modules.wms.mapper.WmsItemSpuMapper;
import org.jeecg.modules.wms.mapper.WmsQualityCheckMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseMapper;
import org.jeecg.modules.wms.mapper.WmsWarehousingEntryDetailsMapper;
import org.jeecg.modules.wms.mapper.WmsWarehousingEntryDetailsSortMapper;
import org.jeecg.modules.wms.mapper.WmsWarehousingEntryMapper;
import org.jeecg.modules.wms.mapper.WmsWarehousingEntryUpMapper;
import org.jeecg.modules.wms.service.IWmsInventoryService;
import org.jeecg.modules.wms.service.IWmsPutawayService;
import org.jeecg.modules.wms.service.IWmsWarehousingEntryService;
import org.jeecg.modules.wms.vo.WmsEntryCreateForm;
import org.jeecg.modules.wms.vo.WmsEntryDetailForm;
import org.jeecg.modules.wms.vo.WmsEntryDetailView;
import org.jeecg.modules.wms.vo.WmsEntryPage;
import org.jeecg.modules.wms.vo.WmsEntryQuery;
import org.jeecg.modules.wms.vo.WmsInventoryDimension;
import org.jeecg.modules.wms.vo.WmsPutawayConfirmForm;
import org.jeecg.modules.wms.vo.WmsPutawayPlan;
import org.jeecg.modules.wms.vo.WmsPutawayPlanLine;
import org.jeecg.modules.wms.vo.WmsPutawayRequest;
import org.jeecg.modules.wms.vo.WmsQcSubmitForm;
import org.jeecg.modules.wms.vo.WmsReceiveForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * @Description: 入库闭环实现
 *
 * 所有库存变动都走 IWmsInventoryService.applyChange，本类不直接 update wms_inventory
 * 的数量列，保证「available = total - locked - frozen」与流水在同一个事务里成立。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Slf4j
@Service
public class WmsWarehousingEntryServiceImpl
        extends ServiceImpl<WmsWarehousingEntryMapper, WmsWarehousingEntry>
        implements IWmsWarehousingEntryService {

    /** 明细状态：已卸货 */
    private static final int DETAIL_UNLOADED = 3;
    /** 明细状态：完成分拣 */
    private static final int DETAIL_SORTED = 5;
    /** 分拣状态：已完成分拣 */
    private static final int SORT_DONE = 2;
    /** 分拣状态：已上架 */
    private static final int SORT_PUTAWAY = 3;
    /** 上架状态：未上架 */
    private static final int UP_PENDING = 1;
    /** 上架状态：已上架 */
    private static final int UP_DONE = 2;
    /** 质量状态：良品 */
    private static final int QUALITY_GOOD = 1;
    /** 质量状态：不良品 */
    private static final int QUALITY_BAD = 3;
    /** SN 状态：在库良品 */
    private static final int SN_IN_STOCK_GOOD = 2;
    /** SN 状态：在库不良 */
    private static final int SN_IN_STOCK_BAD = 3;
    /** 库位状态：空闲 */
    private static final int LOCA_STATUS_FREE = 1;
    /** 库位状态：占用 */
    private static final int LOCA_STATUS_OCCUPIED = 2;
    /** 少收待补料：是 */
    private static final int WAIT_SUPPLEMENT_YES = 1;
    /** 单号前缀 */
    private static final String ENTRY_CODE_PREFIX = "RK";
    private static final String QC_CODE_PREFIX = "QC";

    @Autowired
    private WmsWarehousingEntryDetailsMapper detailsMapper;

    @Autowired
    private WmsWarehousingEntryDetailsSortMapper detailsSortMapper;

    @Autowired
    private WmsWarehousingEntryUpMapper entryUpMapper;

    @Autowired
    private WmsQualityCheckMapper qualityCheckMapper;

    @Autowired
    private WmsInventoryMapper inventoryMapper;

    @Autowired
    private WmsItemSkuMapper skuMapper;

    @Autowired
    private WmsItemSpuMapper spuMapper;

    @Autowired
    private WmsItemSnMapper itemSnMapper;

    @Autowired
    private WmsWarehouseMapper warehouseMapper;

    @Autowired
    private WmsWarehouseAreaMapper areaMapper;

    @Autowired
    private WmsWarehouseAreaLocaMapper locaMapper;

    @Autowired
    private IWmsInventoryService inventoryService;

    @Autowired
    private IWmsPutawayService putawayService;

    // ==================================================================
    // 建单
    // ==================================================================

    @Override
    public IPage<WmsEntryPage> queryEntryPage(WmsEntryQuery query, Integer pageNo, Integer pageSize) {
        Page<WmsEntryPage> page = new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize);
        return baseMapper.selectEntryPage(page, query == null ? new WmsEntryQuery() : query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsWarehousingEntry createEntry(WmsEntryCreateForm form) {
        if (form == null || form.getDetails() == null || form.getDetails().isEmpty()) {
            throw new JeecgBootException("请至少填写一行入库明细");
        }
        if (form.getWarehouseId() == null) {
            throw new JeecgBootException("请指定入库仓库");
        }
        if (form.getConsignorId() == null) {
            throw new JeecgBootException("请指定货主");
        }
        WmsWarehouse warehouse = warehouseMapper.selectById(form.getWarehouseId());
        if (warehouse == null) {
            throw new JeecgBootException("入库仓库不存在");
        }
        int entryType = form.getEntryType() == null ? 1 : form.getEntryType();
        // IN-02 的第一道闸：工业仓的生产入库建单时就要求带工单号，别等到质检才拦
        if (WmsWarehouseTypeEnum.of(warehouse.getWarehouseType()).requireMesWorkOrder()
                && entryType == 2 && oConvertUtils.isEmpty(form.getMesWorkOrderNo())) {
            throw new JeecgBootException("工业制造仓的生产入库必须填写 MES 生产工单号");
        }

        Date now = new Date();
        String batchNumber = oConvertUtils.isEmpty(form.getBatchNumber())
                ? nextBatchNumber() : form.getBatchNumber().trim();
        Date predictedArrival = form.getPredictedArrivalTime() == null ? now : form.getPredictedArrivalTime();

        int totalCount = 0;
        List<WmsWarehousingEntryDetails> detailEntities = new ArrayList<>();
        for (WmsEntryDetailForm line : form.getDetails()) {
            if (line == null || line.getSkuId() == null) {
                throw new JeecgBootException("明细行的商品SKU不能为空");
            }
            if (line.getCount() == null || line.getCount() <= 0) {
                throw new JeecgBootException("明细行的计划数量必须大于0");
            }
            WmsItemSku sku = skuMapper.selectById(line.getSkuId());
            if (sku == null) {
                throw new JeecgBootException("明细行引用的商品SKU不存在：" + line.getSkuId());
            }
            totalCount += line.getCount();
            detailEntities.add(new WmsWarehousingEntryDetails()
                    .setSkuId(line.getSkuId())
                    .setBatchNumber(batchNumber)
                    .setProdDate(line.getProdDate())
                    .setExpireDate(resolveExpireDate(sku, line))
                    .setCount(line.getCount())
                    .setReceivedCount(0)
                    .setSupplierId(line.getSupplierId() == null
                            ? defaultSupplierId(sku) : line.getSupplierId())
                    .setPrice(toCents(line.getPrice()))
                    .setRealArrivalTime(null)
                    .setPredictedArrivalTime(line.getPredictedArrivalTime() == null
                            ? predictedArrival : line.getPredictedArrivalTime())
                    .setStatus(1)
                    .setQcStatus(WmsQcStatusEnum.PENDING.getCode())
                    .setQcPassCount(0)
                    .setQcFailCount(0)
                    .setCreateTime(now)
                    .setIsDel(1));
        }

        WmsWarehousingEntry entry = new WmsWarehousingEntry()
                .setEntryCode(nextCode(ENTRY_CODE_PREFIX))
                .setEntryType(entryType)
                .setErpPoNo(form.getErpPoNo())
                .setMesWorkOrderNo(form.getMesWorkOrderNo())
                .setConsignorId(form.getConsignorId())
                .setBatchNumber(batchNumber)
                .setTotalCount(totalCount)
                .setPredictedArrivalTime(predictedArrival)
                .setWarehouseId(form.getWarehouseId())
                .setDamageCount(0)
                .setSurpassCount(0)
                .setOverReceiveAuditStatus(WmsOverReceiveStatusEnum.NONE.getCode())
                .setStatus(WmsEntryStatusEnum.ARRIVING.getCode())
                .setLackCount(0)
                .setWaitSupplementFlag(0)
                .setCreateTime(now)
                .setCreateBy(currentUsername())
                .setIsDel(1);
        baseMapper.insert(entry);

        for (WmsWarehousingEntryDetails detail : detailEntities) {
            detail.setWarehousingEntryId(entry.getId());
            detailsMapper.insert(detail);
        }
        return entry;
    }

    @Override
    public List<WmsEntryDetailView> listDetails(Long entryId) {
        return detailsMapper.selectDetailViews(entryId);
    }

    // ==================================================================
    // 收货清点
    // ==================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> receive(WmsReceiveForm form) {
        if (form == null || form.getId() == null) {
            throw new JeecgBootException("请指定入库单");
        }
        WmsWarehousingEntry entry = getEntryForUpdate(form.getId());
        // IN-01：只有待到货 / 收货清点中 / 异常挂起（超收放行后）可以收货
        if (entry.getStatus() != WmsEntryStatusEnum.ARRIVING.getCode()
                && entry.getStatus() != WmsEntryStatusEnum.RECEIVING.getCode()
                && entry.getStatus() != WmsEntryStatusEnum.ABNORMAL.getCode()) {
            throw new JeecgBootException("当前状态[" + statusDesc(entry.getStatus()) + "]不允许收货");
        }
        if (form.getDetails() == null || form.getDetails().isEmpty()) {
            throw new JeecgBootException("请填写收货明细");
        }

        List<WmsWarehousingEntryDetails> details = detailsMapper.selectList(
                new LambdaQueryWrapper<WmsWarehousingEntryDetails>()
                        .eq(WmsWarehousingEntryDetails::getWarehousingEntryId, entry.getId())
                        .orderByAsc(WmsWarehousingEntryDetails::getId));
        Map<Long, WmsWarehousingEntryDetails> detailMap = details.stream()
                .collect(Collectors.toMap(WmsWarehousingEntryDetails::getId, d -> d, (a, b) -> a));

        Date now = new Date();
        int totalDamage = 0;
        int totalSurpass = 0;
        int totalLack = 0;
        // 已存在的分拣行按明细覆盖，避免重复收货时累加出双倍分拣量
        detailsSortMapper.delete(new LambdaQueryWrapper<WmsWarehousingEntryDetailsSort>()
                .in(WmsWarehousingEntryDetailsSort::getWarehousingEntryDetailsId, detailMap.keySet()));

        for (WmsReceiveForm.WmsReceiveLine line : form.getDetails()) {
            if (line == null || line.getDetailId() == null) {
                throw new JeecgBootException("收货明细行缺少明细ID");
            }
            WmsWarehousingEntryDetails detail = detailMap.get(line.getDetailId());
            if (detail == null) {
                throw new JeecgBootException("收货行引用了不属于本单的明细：" + line.getDetailId());
            }
            int received = line.getReceivedCount() == null ? 0 : line.getReceivedCount();
            int damage = line.getDamageCount() == null ? 0 : line.getDamageCount();
            if (received < 0 || damage < 0) {
                throw new JeecgBootException("实收数量与破损数量不能为负数");
            }
            if (damage > received) {
                throw new JeecgBootException(String.format(
                        "明细[%s]破损数 %d 不能大于实收数 %d", detail.getSkuId(), damage, received));
            }

            int plan = detail.getCount() == null ? 0 : detail.getCount();
            int surpass = Math.max(0, received - plan);
            int lack = Math.max(0, plan - received);
            totalDamage += damage;
            totalSurpass += surpass;
            totalLack += lack;

            // IN-07：SN 管理商品必须逐个扫码，件数对不上直接拒收
            verifySnCodes(line.getSnCodes(), detail, received - damage, entry);

            detailsMapper.updateById(new WmsWarehousingEntryDetails()
                    .setId(detail.getId())
                    .setReceivedCount(received)
                    .setRealArrivalTime(now)
                    .setUnloadEmpId(form.getUnloadEmpId())
                    .setUnloadTime(now)
                    .setStatus(DETAIL_SORTED)
                    .setUpdateTime(now));

            detailsSortMapper.insert(new WmsWarehousingEntryDetailsSort()
                    .setCount(received - damage)
                    .setWarehousingEntryDetailsId(detail.getId())
                    .setSkuId(detail.getSkuId())
                    .setStatus(SORT_DONE)
                    .setDamageCount(damage)
                    .setSurpassCount(surpass)
                    .setLackCount(lack)
                    .setCreateTime(now)
                    .setIsDel(1));
        }

        // 未提交的明细视为未到货：实收按 0、整批计少收，并落一条空分拣行，
        // 否则「超收 + 少收」加起来对不上整单计划数，IN-08 的待补料也会漏登记。
        for (WmsWarehousingEntryDetails detail : details) {
            boolean submitted = form.getDetails().stream()
                    .anyMatch(l -> l != null && detail.getId().equals(l.getDetailId()));
            if (submitted) {
                continue;
            }
            int plan = detail.getCount() == null ? 0 : detail.getCount();
            totalLack += plan;
            detailsMapper.updateById(new WmsWarehousingEntryDetails()
                    .setId(detail.getId())
                    .setReceivedCount(0)
                    .setStatus(DETAIL_UNLOADED)
                    .setUpdateTime(now));
            detailsSortMapper.insert(new WmsWarehousingEntryDetailsSort()
                    .setCount(0)
                    .setWarehousingEntryDetailsId(detail.getId())
                    .setSkuId(detail.getSkuId())
                    .setStatus(SORT_DONE)
                    .setDamageCount(0)
                    .setSurpassCount(0)
                    .setLackCount(plan)
                    .setCreateTime(now)
                    .setIsDel(1));
        }

        // IN-08：少收自动登记待补料
        entry.setDamageCount(totalDamage)
                .setSurpassCount(totalSurpass)
                .setLackCount(totalLack)
                .setWaitSupplementFlag(totalLack > 0 ? WAIT_SUPPLEMENT_YES : 0);

        WmsWarehouse warehouse = warehouseMapper.selectById(entry.getWarehouseId());
        WmsWarehouseTypeEnum warehouseType = WmsWarehouseTypeEnum.of(warehouse == null ? null : warehouse.getWarehouseType());

        String message;
        if (totalSurpass > 0 && warehouseType.hardBlockOverReceive()
                && !WmsOverReceiveStatusEnum.isPassed(entry.getOverReceiveAuditStatus())) {
            // IN-04 工业仓：超收一律拦截，挂 90 等主管放行
            entry.setStatus(WmsEntryStatusEnum.ABNORMAL.getCode())
                    .setOverReceiveAuditStatus(WmsOverReceiveStatusEnum.PENDING.getCode());
            message = String.format("收货已登记，超收 %d 件需主管放行后才能提交质检（工业制造仓默认拦截）", totalSurpass);
        } else if (totalSurpass > 0 && !warehouseType.hardBlockOverReceive()
                && !WmsOverReceiveStatusEnum.isPassed(entry.getOverReceiveAuditStatus())) {
            // IN-04 普通仓：挂 90 走超收审批
            entry.setStatus(WmsEntryStatusEnum.ABNORMAL.getCode())
                    .setOverReceiveAuditStatus(WmsOverReceiveStatusEnum.PENDING.getCode());
            message = String.format("收货已登记，超收 %d 件待审批通过后才能提交质检（普通仓走超收审批）", totalSurpass);
        } else {
            entry.setStatus(WmsEntryStatusEnum.RECEIVING.getCode());
            message = String.format("收货完成：实收 %d 件，破损 %d 件，超收 %d 件，少收 %d 件",
                    entry.getTotalCount() + totalSurpass, totalDamage, totalSurpass, totalLack);
        }
        entry.setUpdateTime(now);
        baseMapper.updateById(entry);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entryCode", entry.getEntryCode());
        result.put("status", entry.getStatus());
        result.put("statusText", statusDesc(entry.getStatus()));
        result.put("damageCount", totalDamage);
        result.put("surpassCount", totalSurpass);
        result.put("lackCount", totalLack);
        result.put("waitSupplement", totalLack > 0);
        result.put("overReceiveAuditStatus", entry.getOverReceiveAuditStatus());
        result.put("message", message);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitToQc(Long entryId) {
        WmsWarehousingEntry entry = getEntryForUpdate(entryId);
        if (entry.getStatus() == WmsEntryStatusEnum.ABNORMAL.getCode()) {
            throw new JeecgBootException("单据处于异常挂起，请先处理超收放行");
        }
        if (entry.getStatus() != WmsEntryStatusEnum.RECEIVING.getCode()) {
            throw new JeecgBootException("只有收货清点中的单据可以提交质检，当前状态："
                    + statusDesc(entry.getStatus()));
        }
        // IN-04：走到这里仍然有未放行的超收，说明刚才放行被驳回或状态没刷新，再挡一次
        int surpass = entry.getSurpassCount() == null ? 0 : entry.getSurpassCount();
        if (surpass > 0 && !WmsOverReceiveStatusEnum.isPassed(entry.getOverReceiveAuditStatus())) {
            throw new JeecgBootException("存在超收 " + surpass + " 件未放行，不能提交质检");
        }

        List<WmsWarehousingEntryDetails> details = detailsMapper.selectList(
                new LambdaQueryWrapper<WmsWarehousingEntryDetails>()
                        .eq(WmsWarehousingEntryDetails::getWarehousingEntryId, entryId)
                        .orderByAsc(WmsWarehousingEntryDetails::getId));
        if (details.isEmpty()) {
            throw new JeecgBootException("入库单没有明细，无法提交质检");
        }

        WmsWarehouse warehouse = warehouseMapper.selectById(entry.getWarehouseId());
        WmsWarehouseTypeEnum warehouseType = WmsWarehouseTypeEnum.of(
                warehouse == null ? null : warehouse.getWarehouseType());
        // IN-02：工业仓的生产入库质检必须绑定 MES 工单
        if (warehouseType.requireMesWorkOrder() && entry.getEntryType() != null && entry.getEntryType() == 2
                && oConvertUtils.isEmpty(entry.getMesWorkOrderNo())) {
            throw new JeecgBootException("工业制造仓的生产入库质检必须绑定 MES 生产工单号");
        }

        Date now = new Date();
        int created = 0;
        int exempt = 0;
        for (WmsWarehousingEntryDetails detail : details) {
            int received = detail.getReceivedCount() == null ? 0 : detail.getReceivedCount();
            if (received <= 0) {
                // 完全没收到货的明细不进质检，避免生成 0 件的空质检单
                continue;
            }
            // 已有未出结论的质检单就跳过，支持重复提交不重复生成
            long exists = qualityCheckMapper.selectCount(new LambdaQueryWrapper<WmsQualityCheck>()
                    .eq(WmsQualityCheck::getEntryDetailsId, detail.getId())
                    .in(WmsQualityCheck::getStatus,
                            WmsQcStatusEnum.PENDING.getCode(), WmsQcStatusEnum.CHECKING.getCode()));
            if (exists > 0) {
                continue;
            }
            WmsItemSku sku = skuMapper.selectById(detail.getSkuId());
            WmsQcTypeEnum qcType = WmsQcTypeEnum.of(sku == null ? null : sku.getQcType());
            int checkCount = received;
            WmsQualityCheck record = new WmsQualityCheck()
                    .setQcCode(nextCode(QC_CODE_PREFIX))
                    .setWarehousingEntryId(entryId)
                    .setEntryDetailsId(detail.getId())
                    .setMesWorkOrderNo(entry.getMesWorkOrderNo())
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setQcType(qcType.getCode())
                    .setCheckCount(checkCount)
                    .setSampleCount(qcType.defaultSampleCount(checkCount))
                    .setPassCount(0)
                    .setFailCount(0)
                    .setStatus(WmsQcStatusEnum.PENDING.getCode())
                    .setCreateTime(now)
                    .setCreateBy(currentUsername())
                    .setIsDel(1);
            qualityCheckMapper.insert(record);
            created++;
            // 免检明细当场放行，保证「收货 → 质检 → 上架」每一步都有留痕（IN-01）
            if (qcType == WmsQcTypeEnum.EXEMPT) {
                passExempt(record, detail, now);
                exempt++;
            }
        }

        if (created == 0 && exempt == 0) {
            throw new JeecgBootException("没有需要质检的明细（可能已全部生成过质检单）");
        }
        entry.setStatus(WmsEntryStatusEnum.PENDING_QC.getCode()).setUpdateTime(now);
        baseMapper.updateById(entry);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entryCode", entry.getEntryCode());
        result.put("status", entry.getStatus());
        result.put("statusText", statusDesc(entry.getStatus()));
        result.put("qcCreated", created);
        result.put("qcExempt", exempt);
        result.put("message", String.format("已生成 %d 张质检单，其中免检放行 %d 张", created, exempt));
        return result;
    }

    /** 免检放行：直接落合格结论并回写明细 */
    private void passExempt(WmsQualityCheck record, WmsWarehousingEntryDetails detail, Date now) {
        int received = detail.getReceivedCount() == null ? 0 : detail.getReceivedCount();
        qualityCheckMapper.updateById(new WmsQualityCheck()
                .setId(record.getId())
                .setSampleCount(0)
                .setPassCount(received)
                .setFailCount(0)
                .setStatus(WmsQcStatusEnum.PASS.getCode())
                .setQcTime(now)
                .setUpdateTime(now));
        detailsMapper.updateById(new WmsWarehousingEntryDetails()
                .setId(detail.getId())
                .setQcStatus(WmsQcStatusEnum.PASS.getCode())
                .setQcPassCount(received)
                .setQcFailCount(0)
                .setUpdateTime(now));
    }

    // ==================================================================
    // 质检
    // ==================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitQc(WmsQcSubmitForm form) {
        if (form == null || form.getQcIds() == null || form.getQcIds().isEmpty()) {
            throw new JeecgBootException("请指定要提交的质检单");
        }
        Map<Long, WmsQcSubmitForm.WmsQcSubmitLine> lineMap = new LinkedHashMap<>();
        if (form.getLines() != null) {
            for (WmsQcSubmitForm.WmsQcSubmitLine line : form.getLines()) {
                if (line != null && line.getQcId() != null) {
                    lineMap.put(line.getQcId(), line);
                }
            }
        }
        Date now = new Date();
        int passed = 0;
        int failed = 0;
        Long entryId = null;
        for (Long qcId : form.getQcIds()) {
            WmsQualityCheck record = qualityCheckMapper.selectById(qcId);
            if (record == null) {
                throw new JeecgBootException("质检单不存在：" + qcId);
            }
            if (entryId == null) {
                entryId = record.getWarehousingEntryId();
            }
            if (!entryId.equals(record.getWarehousingEntryId())) {
                throw new JeecgBootException("一次只能提交同一张入库单的质检单");
            }
            if (record.getStatus() == null
                    || (record.getStatus() != WmsQcStatusEnum.PENDING.getCode()
                    && record.getStatus() != WmsQcStatusEnum.CHECKING.getCode())) {
                throw new JeecgBootException("质检单[" + record.getQcCode() + "]已出结论，不能重复提交");
            }
            WmsQcSubmitForm.WmsQcSubmitLine line = lineMap.get(qcId);
            int checkCount = record.getCheckCount() == null ? 0 : record.getCheckCount();
            WmsQcTypeEnum qcType = line != null && line.getQcType() != null
                    ? WmsQcTypeEnum.of(line.getQcType()) : WmsQcTypeEnum.of(record.getQcType());
            int sampleCount = line != null && line.getSampleCount() != null
                    ? line.getSampleCount() : qcType.defaultSampleCount(checkCount);
            int passCount;
            int failCount;
            if (qcType == WmsQcTypeEnum.EXEMPT) {
                passCount = checkCount;
                failCount = 0;
            } else if (line != null && line.getPassCount() != null) {
                passCount = line.getPassCount();
                failCount = line.getFailCount() == null ? checkCount - passCount : line.getFailCount();
            } else {
                // 没给结果时按抽检合格处理，避免页面漏填导致质检卡死
                passCount = checkCount;
                failCount = 0;
            }
            if (passCount < 0 || failCount < 0 || passCount + failCount > checkCount) {
                throw new JeecgBootException(String.format(
                        "质检单[%s]的合格数 %d + 不合格数 %d 不能超过送检数 %d",
                        record.getQcCode(), passCount, failCount, checkCount));
            }
            if (sampleCount < 0 || sampleCount > checkCount) {
                throw new JeecgBootException("质检单[" + record.getQcCode() + "]抽检数不能超过送检数");
            }
            WmsQcStatusEnum status = WmsQcStatusEnum.ofCounts(checkCount, passCount);
            String failReason = line != null && oConvertUtils.isNotEmpty(line.getFailReason())
                    ? line.getFailReason().trim() : form.getFailReason();

            qualityCheckMapper.updateById(new WmsQualityCheck()
                    .setId(qcId)
                    .setQcType(qcType.getCode())
                    .setSampleCount(sampleCount)
                    .setPassCount(passCount)
                    .setFailCount(failCount)
                    .setFailReason(oConvertUtils.isNotEmpty(failReason) ? failReason : record.getFailReason())
                    .setQcEmpId(form.getQcEmpId())
                    .setQcTime(now)
                    .setStatus(status.getCode())
                    .setUpdateTime(now));

            // 明细回写质检结论，上架环节据此决定良品 / 不良品分流
            detailsMapper.updateById(new WmsWarehousingEntryDetails()
                    .setId(record.getEntryDetailsId())
                    .setQcStatus(status.getCode())
                    .setQcPassCount(passCount)
                    .setQcFailCount(failCount)
                    .setUpdateTime(now));
            if (failCount > 0) {
                failed++;
            } else {
                passed++;
            }
        }

        // 全部明细都出了结论才推进到 40，否则仍停在 30 等剩余质检
        if (entryId != null) {
            List<WmsWarehousingEntryDetails> details = detailsMapper.selectList(
                    new LambdaQueryWrapper<WmsWarehousingEntryDetails>()
                            .eq(WmsWarehousingEntryDetails::getWarehousingEntryId, entryId));
            boolean allDecided = details.stream()
                    .filter(d -> d.getReceivedCount() != null && d.getReceivedCount() > 0)
                    .allMatch(d -> d.getQcStatus() != null && d.getQcStatus() != WmsQcStatusEnum.PENDING.getCode()
                            && d.getQcStatus() != WmsQcStatusEnum.CHECKING.getCode());
            if (allDecided) {
                baseMapper.updateById(new WmsWarehousingEntry()
                        .setId(entryId)
                        .setStatus(WmsEntryStatusEnum.PENDING_PUTAWAY.getCode())
                        .setUpdateTime(now));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("passed", passed);
        result.put("failed", failed);
        result.put("message", String.format("质检提交完成：合格 %d 张，含不合格 %d 张", passed, failed));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditOverReceive(Long entryId, boolean pass, String reason) {
        WmsWarehousingEntry entry = getEntryForUpdate(entryId);
        if (entry.getStatus() != WmsEntryStatusEnum.ABNORMAL.getCode()) {
            throw new JeecgBootException("只有异常挂起的单据需要超收放行，当前状态：" + statusDesc(entry.getStatus()));
        }
        int auditStatus = entry.getOverReceiveAuditStatus() == null
                ? WmsOverReceiveStatusEnum.NONE.getCode() : entry.getOverReceiveAuditStatus();
        if (auditStatus != WmsOverReceiveStatusEnum.PENDING.getCode()
                && auditStatus != WmsOverReceiveStatusEnum.NONE.getCode()) {
            throw new JeecgBootException("超收已处理过，当前放行状态："
                    + (WmsOverReceiveStatusEnum.isPassed(entry.getOverReceiveAuditStatus()) ? "已放行" : "已驳回"));
        }
        Date now = new Date();
        if (pass) {
            // 放行后回到 30 待质检：超收件数同样要经过质检，不能绕过（IN-01）
            entry.setOverReceiveAuditStatus(WmsOverReceiveStatusEnum.PASSED.getCode())
                    .setStatus(WmsEntryStatusEnum.RECEIVING.getCode());
        } else {
            entry.setOverReceiveAuditStatus(WmsOverReceiveStatusEnum.REJECTED.getCode());
        }
        entry.setUpdateTime(now);
        baseMapper.updateById(entry);
        log.info("入库单 {} 超收{}，原因：{}", entry.getEntryCode(), pass ? "放行" : "驳回", reason);
    }

    // ==================================================================
    // 上架推荐与确认
    // ==================================================================

    @Override
    public WmsPutawayPlan recommendPutaway(Long entryId) {
        WmsWarehousingEntry entry = baseMapper.selectById(entryId);
        if (entry == null) {
            throw new JeecgBootException("入库单不存在");
        }
        if (entry.getStatus() != WmsEntryStatusEnum.PENDING_PUTAWAY.getCode()
                && entry.getStatus() != WmsEntryStatusEnum.PUTAWAY.getCode()) {
            throw new JeecgBootException("只有质检完成的单据可以上架，当前状态：" + statusDesc(entry.getStatus()));
        }
        List<WmsWarehousingEntryDetails> details = pendingPutawayDetails(entryId);
        WmsPutawayPlan merged = null;
        for (WmsWarehousingEntryDetails detail : details) {
            // 待上架量 = 质检合格数 - 已上架数（不是「实收 - 合格」，那样合格品全上不了）
            int remain = qcPassOf(detail) - putawayCountOf(detail.getId());
            if (remain <= 0) {
                // 无合格品可上架（全检不合格或已全部上架），跳过
                continue;
            }
            WmsPutawayPlan plan = putawayService.recommend(new WmsPutawayRequest()
                    .setWarehouseId(entry.getWarehouseId())
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setCount(remain)
                    .setQualityStatus(QUALITY_GOOD)
                    .setConsignorId(entry.getConsignorId()));
            // 引擎按单 SKU 算落点，合并成整单方案后页面要靠 detailId 分组，
            // 否则多明细的单据无法知道每个落点属于哪一行，确认上架时也拼不出请求。
            plan.getLines().forEach(l -> l.setDetailId(detail.getId()));
            if (merged == null) {
                merged = plan;
            } else {
                plan.getLines().forEach(merged::addLine);
                merged.setRemainCount(merged.getRemainCount() + plan.getRemainCount());
                merged.setNeedCount(merged.getNeedCount() + plan.getNeedCount());
            }
        }
        if (merged == null) {
            throw new JeecgBootException("本单没有可上架的合格品");
        }
        return merged;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirmPutaway(WmsPutawayConfirmForm form) {
        if (form == null || form.getId() == null) {
            throw new JeecgBootException("请指定入库单");
        }
        WmsWarehousingEntry entry = getEntryForUpdate(form.getId());
        if (entry.getStatus() != WmsEntryStatusEnum.PENDING_PUTAWAY.getCode()
                && entry.getStatus() != WmsEntryStatusEnum.PUTAWAY.getCode()) {
            throw new JeecgBootException("只有质检完成的单据可以上架，当前状态：" + statusDesc(entry.getStatus()));
        }
        List<WmsWarehousingEntryDetails> details = pendingPutawayDetails(entry.getId());
        if (details.isEmpty()) {
            throw new JeecgBootException("本单没有待上架的明细");
        }
        Map<Long, WmsWarehousingEntryDetails> detailMap = details.stream()
                .collect(Collectors.toMap(WmsWarehousingEntryDetails::getId, d -> d, (a, b) -> a));

        // 未指定上架行 = 按推荐方案整单上架，逐明细生成落点
        List<WmsPutawayConfirmForm.WmsPutawayLine> lines = form.getDetails();
        if (lines == null || lines.isEmpty()) {
            lines = buildLinesFromRecommend(entry, details);
        }

        Date now = new Date();
        WmsWarehousingEntryDetailsSort sortRow = null;
        int totalPutaway = 0;
        int deviateRows = 0;
        // 先把状态推到 50，让中间过程对别的操作可见（同事务，失败会一起回滚）
        baseMapper.updateById(new WmsWarehousingEntry()
                .setId(entry.getId())
                .setStatus(WmsEntryStatusEnum.PUTAWAY.getCode())
                .setUpdateTime(now));

        for (WmsPutawayConfirmForm.WmsPutawayLine line : lines) {
            if (line == null || line.getDetailId() == null) {
                throw new JeecgBootException("上架行缺少明细ID");
            }
            WmsWarehousingEntryDetails detail = detailMap.get(line.getDetailId());
            if (detail == null) {
                throw new JeecgBootException("上架行引用了不属于本单的明细：" + line.getDetailId());
            }
            int count = line.getCount() == null ? 0 : line.getCount();
            if (count <= 0) {
                throw new JeecgBootException("上架数量必须大于0");
            }
            int passCount = qcPassOf(detail);
            int already = putawayCountOf(detail.getId());
            if (already + count > passCount) {
                throw new JeecgBootException(String.format(
                        "明细[%s]超上架：合格 %d 件，已上架 %d 件，本次 %d 件",
                        detail.getSkuId(), passCount, already, count));
            }

            // 推荐值：同明细同数量档的系统建议库位，用来判断人工偏离
            WmsPutawayPlan plan = putawayService.recommend(new WmsPutawayRequest()
                    .setWarehouseId(entry.getWarehouseId())
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setCount(count)
                    .setQualityStatus(QUALITY_GOOD)
                    .setConsignorId(entry.getConsignorId()));
            Integer recommendLocaId = plan.getLines().isEmpty() ? null : plan.getLines().get(0).getLocaId();

            Integer targetLocaId = line.getLocaId() == null ? recommendLocaId : line.getLocaId();
            if (targetLocaId == null) {
                throw new JeecgBootException(String.format(
                        "明细[%s]没有可用库位，需人工指定后上架", detail.getSkuId()));
            }
            // 人工指定的库位照样要过第 1 层硬过滤（IN-03/IN-05/IN-06 全在这里）
            WmsWarehouseAreaLoca loca = putawayService.checkTargetLoca(new WmsPutawayRequest()
                    .setWarehouseId(entry.getWarehouseId())
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setCount(count)
                    .setQualityStatus(QUALITY_GOOD)
                    .setConsignorId(entry.getConsignorId()), targetLocaId);

            WmsItemSku sku = skuMapper.selectById(detail.getSkuId());
            int unitVolume = unitVolume(sku);
            int unitWeight = unitWeight(sku);

            // 落库：库存 + 流水（INBOUND_PUTAWAY）
            WmsInventoryDimension dim = new WmsInventoryDimension()
                    .setWarehouseId(entry.getWarehouseId())
                    .setWarehouseAreaId(loca.getWarehouseAreaId())
                    .setWarehouseAreaLocaId(loca.getId())
                    .setConsignorId(entry.getConsignorId())
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setQualityStatus(QUALITY_GOOD)
                    .setProdDate(detail.getProdDate())
                    .setExpireDate(detail.getExpireDate());
            WmsInventory inventory = inventoryService.applyChange(dim, count, 0, 0,
                    WmsInventoryBizTypeEnum.INBOUND_PUTAWAY, entry.getEntryCode(),
                    "入库上架：库位[" + loca.getLocaCode() + "]");
            // 占用体积 / 重量按实际件数累加，供后续库位的容积承重校验
            inventoryMapper.updateUsage(inventory.getId(),
                    countOf(inventory.getUsedVolume()) + unitVolume * count,
                    countOf(inventory.getUsedWeight()) + unitWeight * count);
            touchLocaUsage(loca.getId(), unitVolume * count, unitWeight * count);

            // 分拣行状态推进到已上架
            sortRow = findSortRow(detail.getId());
            if (sortRow != null) {
                detailsSortMapper.updateById(new WmsWarehousingEntryDetailsSort()
                        .setId(sortRow.getId())
                        .setStatus(SORT_PUTAWAY)
                        .setUpdateTime(now));
            }

            // 上架记录 + 偏离留痕
            boolean deviate = recommendLocaId != null && !recommendLocaId.equals(targetLocaId);
            boolean overflow = plan.getLines().stream()
                    .anyMatch(l -> l.getLocaId().equals(targetLocaId) && l.isOverflow());
            entryUpMapper.insert(new WmsWarehousingEntryUp()
                    .setEntryDetailsId(detail.getId())
                    .setWarehousingEntryId(entry.getId())
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setWarehouseId(entry.getWarehouseId())
                    .setWarehouseAreaId(loca.getWarehouseAreaId())
                    .setWarehouseAreaLocaId(loca.getId())
                    .setCount(count)
                    .setQualityStatus(QUALITY_GOOD)
                    .setStrategyUsed(plan.getStrategyUsed())
                    .setRecommendLocaId(recommendLocaId)
                    .setIsDeviate(deviate ? 1 : 0)
                    .setDeviateReason(deviate ? resolveDeviateReason(line.getDeviateReason()) : null)
                    .setIsOverflow(overflow ? 1 : 0)
                    .setDetailsSortId(sortRow == null ? null : sortRow.getId())
                    .setStatus(UP_DONE)
                    .setCreateTime(now)
                    .setCreateBy(currentUsername())
                    .setIsDel(1));
            if (deviate) {
                deviateRows++;
            }

            // SN 在库登记
            registerSnOnPutaway(detail, entry, loca.getId(), count, entry.getWarehouseId(), now);
            totalPutaway += count;
        }

        // 全部合格品都上架完了才置 60，否则停在 50 上架中
        boolean allDone = details.stream()
                .allMatch(d -> putawayCountOf(d.getId()) >= qcPassOf(d));
        entry.setStatus(allDone ? WmsEntryStatusEnum.COMPLETED.getCode() : WmsEntryStatusEnum.PUTAWAY.getCode())
                .setUpdateTime(now);
        baseMapper.updateById(entry);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entryCode", entry.getEntryCode());
        result.put("status", entry.getStatus());
        result.put("statusText", statusDesc(entry.getStatus()));
        result.put("putawayCount", totalPutaway);
        result.put("deviateRows", deviateRows);
        result.put("message", allDone
                ? String.format("入库完成：上架 %d 件，其中偏离推荐 %d 行", totalPutaway, deviateRows)
                : String.format("上架 %d 件，仍有未上架的合格品，单据保持上架中", totalPutaway));
        return result;
    }

    @Override
    public List<WmsWarehousingEntryUp> listPutawayRecords(Long entryId) {
        return entryUpMapper.selectList(new LambdaQueryWrapper<WmsWarehousingEntryUp>()
                .eq(WmsWarehousingEntryUp::getWarehousingEntryId, entryId)
                .orderByAsc(WmsWarehousingEntryUp::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long entryId) {
        WmsWarehousingEntry entry = getEntryForUpdate(entryId);
        if (entry.getStatus() == null || entry.getStatus() != WmsEntryStatusEnum.ARRIVING.getCode()) {
            throw new JeecgBootException("只有待到货的单据可以取消，当前状态：" + statusDesc(entry.getStatus()));
        }
        entry.setStatus(WmsEntryStatusEnum.CANCELLED.getCode()).setUpdateTime(new Date());
        baseMapper.updateById(entry);
    }

    // ==================================================================
    // 内部工具
    // ==================================================================

    /** 取单据并加行锁，防止同一张单被并发收货 / 上架 */
    private WmsWarehousingEntry getEntryForUpdate(Long entryId) {
        WmsWarehousingEntry entry = baseMapper.selectOne(new LambdaQueryWrapper<WmsWarehousingEntry>()
                .eq(WmsWarehousingEntry::getId, entryId)
                .last("FOR UPDATE"), false);
        if (entry == null) {
            throw new JeecgBootException("入库单不存在");
        }
        return entry;
    }

    /**
     * 待上架明细：已出质检结论、有合格品、且合格品还没上完。
     */
    private List<WmsWarehousingEntryDetails> pendingPutawayDetails(Long entryId) {
        List<WmsWarehousingEntryDetails> all = detailsMapper.selectList(
                new LambdaQueryWrapper<WmsWarehousingEntryDetails>()
                        .eq(WmsWarehousingEntryDetails::getWarehousingEntryId, entryId)
                        .orderByAsc(WmsWarehousingEntryDetails::getId));
        return all.stream()
                .filter(d -> qcPassOf(d) > 0)
                .filter(d -> putawayCountOf(d.getId()) < qcPassOf(d))
                .collect(Collectors.toList());
    }

    /** 按推荐方案生成整单上架行（每个推荐落点一行） */
    private List<WmsPutawayConfirmForm.WmsPutawayLine> buildLinesFromRecommend(
            WmsWarehousingEntry entry, List<WmsWarehousingEntryDetails> details) {
        List<WmsPutawayConfirmForm.WmsPutawayLine> lines = new ArrayList<>();
        for (WmsWarehousingEntryDetails detail : details) {
            int remain = qcPassOf(detail) - putawayCountOf(detail.getId());
            if (remain <= 0) {
                continue;
            }
            WmsPutawayPlan plan = putawayService.recommend(new WmsPutawayRequest()
                    .setWarehouseId(entry.getWarehouseId())
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setCount(remain)
                    .setQualityStatus(QUALITY_GOOD)
                    .setConsignorId(entry.getConsignorId()));
            for (WmsPutawayPlanLine planLine : plan.getLines()) {
                WmsPutawayConfirmForm.WmsPutawayLine line = new WmsPutawayConfirmForm.WmsPutawayLine();
                line.setDetailId(detail.getId());
                line.setCount(planLine.getCount());
                line.setLocaId(planLine.getLocaId());
                lines.add(line);
            }
            if (plan.getRemainCount() > 0) {
                throw new JeecgBootException(String.format(
                        "明细[%s]还有 %d 件没有可用库位，请人工指定库位后分批上架",
                        detail.getSkuId(), plan.getRemainCount()));
            }
        }
        return lines;
    }

    /**
     * IN-07：SN 码必填、不重复、且历史上不能在库或已出库。
     * 允许的重复是「本单本次新扫」，所以只拒绝命中在库 / 已出库状态的 SN。
     */
    private void verifySnCodes(List<String> snCodes, WmsWarehousingEntryDetails detail,
                               int needCount, WmsWarehousingEntry entry) {
        WmsItemSku sku = skuMapper.selectById(detail.getSkuId());
        boolean snManaged = sku != null && sku.getIsSnManaged() != null && sku.getIsSnManaged() == 1;
        List<String> codes = snCodes == null ? new ArrayList<>() : snCodes.stream()
                .filter(oConvertUtils::isNotEmpty)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toList());
        if (!snManaged) {
            // 非 SN 商品传了 SN 也不拦，但只登记第一个，避免误扫多件造成脏数据
            return;
        }
        if (codes.size() != needCount) {
            throw new JeecgBootException(String.format(
                    "商品[%s]启用SN管理，应扫描 %d 个SN，实际扫描 %d 个",
                    detail.getSkuId(), needCount, codes.size()));
        }
        List<String> distinct = codes.stream().distinct().collect(Collectors.toList());
        if (distinct.size() != codes.size()) {
            throw new JeecgBootException("SN 列表中有重复扫描的条码");
        }
        List<WmsItemSn> exists = itemSnMapper.selectList(new LambdaQueryWrapper<WmsItemSn>()
                .in(WmsItemSn::getSnCode, distinct)
                .in(WmsItemSn::getStatus, SN_IN_STOCK_GOOD, SN_IN_STOCK_BAD));
        if (!exists.isEmpty()) {
            throw new JeecgBootException("以下 SN 已在库，不能重复入库："
                    + exists.stream().map(WmsItemSn::getSnCode).collect(Collectors.joining("、")));
        }
    }

    /**
     * 上架时登记 SN：已扫过的更新为在库，没扫过的按件数补登记占位码。
     * 收货时扫的 SN 存在 wms_item_sn（status=1 收货待检，无库位），
     * 上架时回填库位并置在库状态。
     */
    private void registerSnOnPutaway(WmsWarehousingEntryDetails detail, WmsWarehousingEntry entry,
                                     Integer locaId, int count, Integer warehouseId, Date now) {
        WmsItemSku sku = skuMapper.selectById(detail.getSkuId());
        boolean snManaged = sku != null && sku.getIsSnManaged() != null && sku.getIsSnManaged() == 1;
        if (!snManaged) {
            return;
        }
        // 取本明细已收货但未上架的 SN（status=1 收货待检）
        List<WmsItemSn> pending = itemSnMapper.selectList(new LambdaQueryWrapper<WmsItemSn>()
                .eq(WmsItemSn::getSkuId, detail.getSkuId())
                .eq(WmsItemSn::getBatchNumber, detail.getBatchNumber())
                .eq(WmsItemSn::getStatus, 1)
                .isNull(WmsItemSn::getWarehouseAreaLocaId)
                .orderByAsc(WmsItemSn::getId)
                .last("LIMIT " + count));
        int done = 0;
        for (WmsItemSn sn : pending) {
            itemSnMapper.updateById(new WmsItemSn()
                    .setId(sn.getId())
                    .setWarehouseId(warehouseId)
                    .setWarehouseAreaLocaId(locaId)
                    .setQualityStatus(QUALITY_GOOD)
                    .setStatus(SN_IN_STOCK_GOOD)
                    .setUpdateTime(now));
            done++;
        }
        // 收货时没核对到 SN 的件数（老数据或非扫码收货）补一条占位记录，
        // 保证「在库件数 = SN 条数」，后续出库扫码才不至于对不上。
        for (int i = done; i < count; i++) {
            itemSnMapper.insert(new WmsItemSn()
                    .setSnCode(entry.getEntryCode() + "-" + detail.getId() + "-" + (i + 1)
                            + "-" + ThreadLocalRandom.current().nextInt(1000, 9999))
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setWarehouseId(warehouseId)
                    .setWarehouseAreaLocaId(locaId)
                    .setQualityStatus(QUALITY_GOOD)
                    .setEntryOrderId(entry.getId())
                    .setMesWorkOrderNo(entry.getMesWorkOrderNo())
                    .setStatus(SN_IN_STOCK_GOOD)
                    .setCreateTime(now)
                    .setCreateBy(currentUsername())
                    .setIsDel(1));
        }
    }

    /** 库位占用体积 / 重量累加，库位空 → 占用 */
    private void touchLocaUsage(Integer locaId, int addVolume, int addWeight) {
        WmsWarehouseAreaLoca loca = locaMapper.selectById(locaId);
        if (loca == null) {
            return;
        }
        locaMapper.updateById(new WmsWarehouseAreaLoca()
                .setId(locaId)
                .setUsedVolume(countOf(loca.getUsedVolume()) + addVolume)
                .setUsedWeight(countOf(loca.getUsedWeight()) + addWeight)
                .setLocaStatus(LOCA_STATUS_OCCUPIED)
                .setUpdateTime(new Date()));
    }

    /** 取该明细最近一条分拣行 */
    private WmsWarehousingEntryDetailsSort findSortRow(Long detailId) {
        return detailsSortMapper.selectOne(new LambdaQueryWrapper<WmsWarehousingEntryDetailsSort>()
                .eq(WmsWarehousingEntryDetailsSort::getWarehousingEntryDetailsId, detailId)
                .orderByDesc(WmsWarehousingEntryDetailsSort::getId)
                .last("LIMIT 1"), false);
    }

    /** 明细已上架数量 */
    private int putawayCountOf(Long detailId) {
        List<WmsWarehousingEntryUp> rows = entryUpMapper.selectList(
                new LambdaQueryWrapper<WmsWarehousingEntryUp>()
                        .eq(WmsWarehousingEntryUp::getEntryDetailsId, detailId)
                        .eq(WmsWarehousingEntryUp::getStatus, UP_DONE));
        return rows.stream().mapToInt(r -> countOf(r.getCount())).sum();
    }

    private int receivedOf(WmsWarehousingEntryDetails detail) {
        return countOf(detail.getReceivedCount());
    }

    private int qcPassOf(WmsWarehousingEntryDetails detail) {
        return countOf(detail.getQcPassCount());
    }

    private int countOf(Integer value) {
        return value == null ? 0 : value;
    }

    /** 有效期：明细给了就用，否则按 SPU 保质期天数从生产日期推 */
    private Date resolveExpireDate(WmsItemSku sku, WmsEntryDetailForm line) {
        if (line.getExpireDate() != null) {
            return line.getExpireDate();
        }
        if (line.getProdDate() == null || sku == null || sku.getSpuId() == null) {
            return null;
        }
        WmsItemSpu spu = spuMapper.selectById(sku.getSpuId());
        if (spu == null || spu.getShelfLifeDays() == null || spu.getShelfLifeDays() <= 0) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(line.getProdDate());
        calendar.add(Calendar.DAY_OF_MONTH, spu.getShelfLifeDays());
        return calendar.getTime();
    }

    /** 供应商默认取 SPU 上维护的供应商 */
    private Long defaultSupplierId(WmsItemSku sku) {
        if (sku == null || sku.getSpuId() == null) {
            return 0L;
        }
        WmsItemSpu spu = spuMapper.selectById(sku.getSpuId());
        return spu == null || spu.getSupplierId() == null ? 0L : spu.getSupplierId().longValue();
    }

    /** 单价按分存整数，元的小数部分四舍五入 */
    private Integer toCents(BigDecimal price) {
        if (price == null) {
            return 0;
        }
        return price.multiply(BigDecimal.valueOf(100)).setScale(0, java.math.RoundingMode.HALF_UP).intValue();
    }

    private int unitVolume(WmsItemSku sku) {
        int volume = sku == null ? 0 : countOf(sku.getProductVolume());
        if (volume > 0) {
            return volume;
        }
        int l = sku == null ? 0 : countOf(sku.getProductLength());
        int w = sku == null ? 0 : countOf(sku.getProductWidth());
        int h = sku == null ? 0 : countOf(sku.getProductHeight());
        return l > 0 && w > 0 && h > 0 ? l * w * h : 0;
    }

    private int unitWeight(WmsItemSku sku) {
        return sku == null ? 0 : countOf(sku.getProductWeight());
    }

    /** 偏离原因：人工填了就用手填的，没填给个默认说明，避免留空无法追溯 */
    private String resolveDeviateReason(String reason) {
        return oConvertUtils.isNotEmpty(reason) ? reason.trim() : "实际库位与系统推荐不一致，操作人手动指定";
    }

    private String statusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        for (WmsEntryStatusEnum e : WmsEntryStatusEnum.values()) {
            if (e.getCode() == status) {
                return e.getDesc();
            }
        }
        return "未知(" + status + ")";
    }

    /** 单号：前缀 + yyyyMMddHHmmss + 3 位随机数 */
    private String nextCode(String prefix) {
        String ts = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return prefix + ts + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    /** 批次号：B + yyyyMMdd + 4 位随机数 */
    private String nextBatchNumber() {
        String ts = new SimpleDateFormat("yyyyMMdd").format(new Date());
        return "B" + ts + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
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
