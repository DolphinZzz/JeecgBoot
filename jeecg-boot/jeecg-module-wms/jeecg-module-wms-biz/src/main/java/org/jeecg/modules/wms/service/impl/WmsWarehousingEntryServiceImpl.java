package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsConsignor;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.entity.WmsItemSn;
import org.jeecg.modules.wms.entity.WmsItemSpu;
import org.jeecg.modules.wms.entity.WmsQualityCheck;
import org.jeecg.modules.wms.entity.WmsSupplier;
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
import org.jeecg.modules.wms.mapper.WmsSupplierMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.mapper.WmsConsignorMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseMapper;
import org.jeecg.modules.wms.mapper.WmsWarehousingEntryDetailsMapper;
import org.jeecg.modules.wms.mapper.WmsWarehousingEntryDetailsSortMapper;
import org.jeecg.modules.wms.mapper.WmsWarehousingEntryMapper;
import org.jeecg.modules.wms.mapper.WmsWarehousingEntryUpMapper;
import org.jeecg.modules.wms.service.IWmsInventoryService;
import org.jeecg.modules.wms.service.IWmsPutawayService;
import org.jeecg.modules.wms.service.IWmsWarehousingEntryService;
import org.jeecg.modules.wms.dto.ErpWarehousingEntryCreateDto;
import org.jeecg.modules.wms.dto.ErpWarehousingEntryDetailDto;
import org.jeecg.modules.wms.util.WmsUnitConverter;
import org.jeecg.modules.wms.vo.WmsEntryCreateForm;
import org.jeecg.modules.wms.vo.WmsEntryDetailForm;
import org.jeecg.modules.wms.vo.WmsEntryDetailView;
import org.jeecg.modules.wms.vo.WmsEntryPage;
import org.jeecg.modules.wms.vo.WmsEntryQuery;
import org.jeecg.modules.wms.vo.WmsInventoryDimension;
import org.jeecg.modules.wms.vo.WmsPutawayConfirmForm;
import org.jeecg.modules.wms.vo.WmsPutawayPage;
import org.jeecg.modules.wms.vo.WmsPutawayPlan;
import org.jeecg.modules.wms.vo.WmsPutawayPlanLine;
import org.jeecg.modules.wms.vo.WmsPutawayRequest;
import org.jeecg.modules.wms.vo.WmsQcReturnForm;
import org.jeecg.modules.wms.vo.WmsQcShipReturnForm;
import org.jeecg.modules.wms.vo.WmsQcSubmitForm;
import org.jeecg.modules.wms.vo.WmsReceiveForm;
import org.jeecg.modules.wms.vo.WmsWarehousingEntryResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
    private static final String RETURN_CODE_PREFIX = "TH";
    /** 破损退货单号前缀（收货清点检出破损时自动生成） */
    private static final String DAMAGE_CODE_PREFIX = "PD";
    /** 质检单来源：质检 */
    private static final int QC_SOURCE_QC = 1;
    /** 质检单来源：收货破损 */
    private static final int QC_SOURCE_DAMAGE = 2;
    /** 库区类型：退货不良区 */
    private static final String AREA_RETURN = "RETURN";
    /** 质检退货状态：0无需退货 1待退货处理 2已放置退货区待退供应商 3已退还供应商 */
    private static final int QC_RETURN_STATUS_NONE = 0;
    private static final int QC_RETURN_STATUS_PENDING = 1;
    private static final int QC_RETURN_STATUS_STORED = 2;
    private static final int QC_RETURN_STATUS_SHIPPED = 3;
    /** SN 状态：已退货 */
    private static final int SN_RETURNED = 6;

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
    private WmsConsignorMapper consignorMapper;

    @Autowired
    private WmsSupplierMapper supplierMapper;

    @Autowired
    private WmsWarehouseAreaMapper areaMapper;

    @Autowired
    private WmsWarehouseAreaLocaMapper locaMapper;

    @Autowired
    private IWmsInventoryService inventoryService;

    @Autowired
    private IWmsPutawayService putawayService;

    @Autowired
    private org.jeecg.modules.wms.service.IWmsLocaCompatibilityService locaCompatibilityService;

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
        // 货主必须来自货主档案且处于启用状态；停用的货主只是不再接新单，历史单据照常可查
        WmsConsignor consignor = consignorMapper.selectById(form.getConsignorId());
        if (consignor == null) {
            throw new JeecgBootException("货主不存在，请先在「基础数据-货主信息」中维护");
        }
        if (consignor.getEnableStatus() != null && consignor.getEnableStatus() != 1) {
            throw new JeecgBootException("货主「" + consignor.getConsignorName() + "」已停用，不能建新入库单");
        }
        int entryType = form.getEntryType() == null ? 1 : form.getEntryType();
        Date now = new Date();
        // 批次号自动生成：若用户未传则后端统一生成整单唯一批次
        String batchNumber = oConvertUtils.isEmpty(form.getBatchNumber())
                ? nextBatchNumber() : form.getBatchNumber().trim();
        Date predictedArrival = form.getPredictedArrivalTime() == null ? now : form.getPredictedArrivalTime();

        int totalCount = 0;
        List<WmsWarehousingEntryDetails> detailEntities = new ArrayList<>();
        for (WmsEntryDetailForm line : form.getDetails()) {
            if (line == null || line.getSkuId() == null) {
                throw new JeecgBootException("明细行的商品信息不能为空");
            }
            if (line.getCount() == null || line.getCount() <= 0) {
                throw new JeecgBootException("明细行的计划数量必须大于0");
            }
            WmsItemSku sku = skuMapper.selectById(line.getSkuId());
            if (sku == null) {
                throw new JeecgBootException("明细行引用的商品信息不存在：" + line.getSkuId());
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
    @Transactional(rollbackFor = Exception.class)
    public WmsWarehousingEntryResultVo createEntryFromErp(ErpWarehousingEntryCreateDto dto) {
        if (dto == null) {
            throw new JeecgBootException("ERP推送入库单参数不能为空");
        }
        if (oConvertUtils.isEmpty(dto.getErpPoNo())) {
            throw new JeecgBootException("ERP采购单号(erpPoNo)不能为空");
        }
        if (dto.getWarehouseId() == null) {
            throw new JeecgBootException("请指定入库仓库(warehouseId)");
        }
        if (dto.getConsignorId() == null) {
            throw new JeecgBootException("请指定货主(consignorId)");
        }
        if (dto.getDetails() == null || dto.getDetails().isEmpty()) {
            throw new JeecgBootException("请至少推送一行入库商品明细");
        }

        // 幂等校验：同一货主下同一 ERP 采购单号若已存在对应有效入库单，直接返回已有单据信息
        LambdaQueryWrapper<WmsWarehousingEntry> existQuery = new LambdaQueryWrapper<WmsWarehousingEntry>()
                .eq(WmsWarehousingEntry::getConsignorId, dto.getConsignorId())
                .eq(WmsWarehousingEntry::getErpPoNo, dto.getErpPoNo().trim())
                .ne(WmsWarehousingEntry::getStatus, WmsEntryStatusEnum.CANCELLED.getCode())
                .orderByDesc(WmsWarehousingEntry::getId)
                .last("LIMIT 1");
        WmsWarehousingEntry exist = baseMapper.selectOne(existQuery);
        if (exist != null) {
            log.info("ERP采购单号[{}]已存在对应入库单[{}]，直接返回已有入库单信息", dto.getErpPoNo(), exist.getEntryCode());
            return WmsWarehousingEntryResultVo.builder()
                    .entryId(exist.getId())
                    .entryCode(exist.getEntryCode())
                    .erpPoNo(exist.getErpPoNo())
                    .batchNumber(exist.getBatchNumber())
                    .status(exist.getStatus())
                    .totalCount(exist.getTotalCount())
                    .build();
        }

        WmsWarehouse warehouse = warehouseMapper.selectById(dto.getWarehouseId());
        if (warehouse == null) {
            throw new JeecgBootException("入库仓库不存在");
        }
        WmsConsignor consignor = consignorMapper.selectById(dto.getConsignorId());
        if (consignor == null) {
            throw new JeecgBootException("货主不存在，请先在「基础数据-货主信息」中维护");
        }
        if (consignor.getEnableStatus() != null && consignor.getEnableStatus() != 1) {
            throw new JeecgBootException("货主「" + consignor.getConsignorName() + "」已停用，不能建新入库单");
        }

        int entryType = dto.getEntryType() == null ? 1 : dto.getEntryType();
        Date now = new Date();
        // 批次号自动生成：若 ERP 未指定则系统自动生成整单唯一批次号
        String batchNumber = oConvertUtils.isEmpty(dto.getBatchNumber())
                ? nextBatchNumber() : dto.getBatchNumber().trim();
        Date predictedArrival = dto.getPredictedArrivalTime() == null ? now : dto.getPredictedArrivalTime();

        int totalCount = 0;
        List<WmsWarehousingEntryDetails> detailEntities = new ArrayList<>();
        for (ErpWarehousingEntryDetailDto line : dto.getDetails()) {
            if (line == null || line.getSkuId() == null) {
                throw new JeecgBootException("明细行的商品信息不能为空");
            }
            if (line.getCount() == null || line.getCount() <= 0) {
                throw new JeecgBootException("明细行的计划数量必须大于0");
            }
            WmsItemSku sku = skuMapper.selectById(line.getSkuId());
            if (sku == null) {
                throw new JeecgBootException("明细行引用的商品信息不存在：" + line.getSkuId());
            }
            totalCount += line.getCount();
            detailEntities.add(new WmsWarehousingEntryDetails()
                    .setSkuId(line.getSkuId())
                    .setBatchNumber(batchNumber)
                    .setProdDate(line.getProdDate())
                    .setExpireDate(resolveExpireDate(sku, line.getProdDate(), line.getExpireDate()))
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
                .setErpPoNo(dto.getErpPoNo().trim())
                .setMesWorkOrderNo(dto.getMesWorkOrderNo())
                .setConsignorId(dto.getConsignorId())
                .setBatchNumber(batchNumber)
                .setTotalCount(totalCount)
                .setPredictedArrivalTime(predictedArrival)
                .setWarehouseId(dto.getWarehouseId())
                .setDamageCount(0)
                .setSurpassCount(0)
                .setOverReceiveAuditStatus(WmsOverReceiveStatusEnum.NONE.getCode())
                .setStatus(WmsEntryStatusEnum.ARRIVING.getCode())
                .setLackCount(0)
                .setWaitSupplementFlag(0)
                .setCreateTime(now)
                .setCreateBy(currentUsername() != null ? currentUsername() : "ERP-PUSH")
                .setIsDel(1);
        baseMapper.insert(entry);

        for (WmsWarehousingEntryDetails detail : detailEntities) {
            detail.setWarehousingEntryId(entry.getId());
            detailsMapper.insert(detail);
        }

        return WmsWarehousingEntryResultVo.builder()
                .entryId(entry.getId())
                .entryCode(entry.getEntryCode())
                .erpPoNo(entry.getErpPoNo())
                .batchNumber(entry.getBatchNumber())
                .status(entry.getStatus())
                .totalCount(entry.getTotalCount())
                .build();
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
                    .setDamageCount(damage)
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

            // 破损件在收货时即分流到退货不良区货架，并生成可追溯的破损退货记录，
            // 后续由质检单页面完成「联系供应商 → 确认退还出库」的闭环。
            if (damage > 0) {
                placeDamageIntoReturnArea(entry, detail, damage, now);
            }
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
        boolean hasGoodPiece = false;
        for (WmsWarehousingEntryDetails detail : details) {
            int received = detail.getReceivedCount() == null ? 0 : detail.getReceivedCount();
            int damage = detail.getDamageCount() == null ? 0 : detail.getDamageCount();
            // 破损件已分流到退货不良区，只对完好件送检，避免破损品重复计入质检结论
            int goodCount = received - damage;
            if (goodCount > 0) {
                hasGoodPiece = true;
            }
            if (goodCount <= 0) {
                // 完全没收到货 / 整批破损的明细不进质检，避免生成 0 件的空质检单
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
            int checkCount = goodCount;
            WmsQualityCheck record = new WmsQualityCheck()
                    .setQcCode(nextCode(QC_CODE_PREFIX))
                    .setWarehousingEntryId(entryId)
                    .setEntryDetailsId(detail.getId())
                    .setMesWorkOrderNo(entry.getMesWorkOrderNo())
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setQcType(qcType.getCode())
                    .setSource(QC_SOURCE_QC)
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

        if (!hasGoodPiece) {
            // 整单没有完好件（全部破损或少收为 0）：无需质检与上架，
            // 破损件已入退货区，入库单直接按结单条件判定（满足则置 60 已完成）
            entry.setStatus(WmsEntryStatusEnum.PENDING_PUTAWAY.getCode()).setUpdateTime(now);
            baseMapper.updateById(entry);
            checkAndCompleteEntryIfEligible(entryId, now);

            Map<String, Object> noQcResult = new LinkedHashMap<>();
            noQcResult.put("entryCode", entry.getEntryCode());
            noQcResult.put("status", entry.getStatus());
            noQcResult.put("statusText", statusDesc(entry.getStatus()));
            noQcResult.put("qcCreated", 0);
            noQcResult.put("qcExempt", 0);
            noQcResult.put("message", "整单无完好件可送检，破损件已入退货不良区，请到质检单页面联系供应商退还");
            return noQcResult;
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

    /** 免检放行：直接落合格结论并回写明细（破损件已入退货区，不计入合格品） */
    private void passExempt(WmsQualityCheck record, WmsWarehousingEntryDetails detail, Date now) {
        int goodCount = detailGoodCount(detail);
        qualityCheckMapper.updateById(new WmsQualityCheck()
                .setId(record.getId())
                .setSampleCount(0)
                .setPassCount(goodCount)
                .setFailCount(0)
                .setStatus(WmsQcStatusEnum.PASS.getCode())
                .setQcTime(now)
                .setUpdateTime(now));
        detailsMapper.updateById(new WmsWarehousingEntryDetails()
                .setId(detail.getId())
                .setQcStatus(WmsQcStatusEnum.PASS.getCode())
                .setQcPassCount(goodCount)
                .setQcFailCount(0)
                .setUpdateTime(now));
    }

    /** 明细可送检 / 可上架的完好件数量 = 实收 − 收货破损 */
    private int detailGoodCount(WmsWarehousingEntryDetails detail) {
        int received = detail.getReceivedCount() == null ? 0 : detail.getReceivedCount();
        int damage = detail.getDamageCount() == null ? 0 : detail.getDamageCount();
        return Math.max(0, received - damage);
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

            // 不合格退货处理
            Integer returnLocaId = line != null && line.getReturnLocaId() != null
                    ? line.getReturnLocaId() : form.getReturnLocaId();
            String returnRemark = line != null && oConvertUtils.isNotEmpty(line.getReturnRemark())
                    ? line.getReturnRemark().trim() : (form.getReturnRemark() != null ? form.getReturnRemark().trim() : null);

            int returnStatus = QC_RETURN_STATUS_NONE;
            String returnOrderNo = record.getReturnOrderNo();
            if (failCount > 0) {
                if (oConvertUtils.isEmpty(returnOrderNo)) {
                    returnOrderNo = nextCode(RETURN_CODE_PREFIX);
                }
                if (returnLocaId != null) {
                    doPlaceInReturnArea(record, returnLocaId, failCount, returnOrderNo, returnRemark, now);
                    returnStatus = QC_RETURN_STATUS_STORED;
                } else {
                    returnStatus = QC_RETURN_STATUS_PENDING;
                }
            }

            qualityCheckMapper.updateById(new WmsQualityCheck()
                    .setId(qcId)
                    .setQcType(qcType.getCode())
                    .setSampleCount(sampleCount)
                    .setPassCount(passCount)
                    .setFailCount(failCount)
                    .setFailReason(oConvertUtils.isNotEmpty(failReason) ? failReason : record.getFailReason())
                    .setReturnOrderNo(returnOrderNo)
                    .setReturnLocaId(returnLocaId)
                    .setReturnStatus(returnStatus)
                    .setReturnRemark(returnRemark)
                    .setReturnHandleTime(returnStatus >= QC_RETURN_STATUS_STORED ? now : null)
                    .setReturnHandleBy(returnStatus >= QC_RETURN_STATUS_STORED ? currentUsername() : null)
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

        // 全部明细都出了结论后推进单据状态
        if (entryId != null) {
            List<WmsWarehousingEntryDetails> details = detailsMapper.selectList(
                    new LambdaQueryWrapper<WmsWarehousingEntryDetails>()
                            .eq(WmsWarehousingEntryDetails::getWarehousingEntryId, entryId));
            boolean allDecided = details.stream()
                    .filter(d -> d.getReceivedCount() != null && d.getReceivedCount() > 0)
                    .allMatch(d -> d.getQcStatus() != null && d.getQcStatus() != WmsQcStatusEnum.PENDING.getCode()
                            && d.getQcStatus() != WmsQcStatusEnum.CHECKING.getCode());
            if (allDecided) {
                int totalPass = details.stream().mapToInt(this::qcPassOf).sum();
                if (totalPass == 0) {
                    // 全单无合格品，直接入库完成；不合格品/破损品作为独立退货分支异步处置
                    baseMapper.updateById(new WmsWarehousingEntry()
                            .setId(entryId)
                            .setStatus(WmsEntryStatusEnum.COMPLETED.getCode())
                            .setUpdateTime(now));
                } else {
                    baseMapper.updateById(new WmsWarehousingEntry()
                            .setId(entryId)
                            .setStatus(WmsEntryStatusEnum.PENDING_PUTAWAY.getCode())
                            .setUpdateTime(now));
                }
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
        if (details.isEmpty()) {
            throw buildNoPutawayReason(entryId);
        }
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
            if (plan.getLines().isEmpty() || plan.getRemainCount() > 0) {
                // 没有合规落点（或只排上了一部分）：补一行「待人工指定库位」的占位，
                // 页面才有行可编辑。不这样做整张表是空的，作业员连强制指定都无从下手。
                plan.addLine(new WmsPutawayPlanLine()
                        .setDetailId(detail.getId())
                        .setCount(plan.getLines().isEmpty() ? remain : plan.getRemainCount())
                        .setCapacity(0)
                        .setNeedManual(true));
            }
            if (merged == null) {
                merged = plan;
            } else {
                plan.getLines().forEach(merged::addLine);
                merged.setRemainCount(merged.getRemainCount() + plan.getRemainCount());
                merged.setNeedCount(merged.getNeedCount() + plan.getNeedCount());
                // 整单层面的空因取第一条说明问题的（多明细时后面的原因通常雷同）
                if (oConvertUtils.isEmpty(merged.getEmptyReason())) {
                    merged.setEmptyReason(plan.getEmptyReason());
                }
                merged.setRejectedCount(merged.getRejectedCount() + plan.getRejectedCount());
            }
        }
        if (merged == null) {
            throw buildNoPutawayReason(entryId);
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
            throw buildNoPutawayReason(entry.getId());
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
            WmsPutawayRequest putawayRequest = new WmsPutawayRequest()
                    .setWarehouseId(entry.getWarehouseId())
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setCount(count)
                    .setQualityStatus(QUALITY_GOOD)
                    .setConsignorId(entry.getConsignorId());

            // 人工强制指定：分区/温区/混放/容积都不再拦，只守跨仓与库位不可用两条，
            // 但必须填偏离原因留痕——跳过业务约束这件事本身要被审计。
            boolean forced = Boolean.TRUE.equals(line.getForce());
            WmsWarehouseAreaLoca loca;
            if (forced) {
                if (oConvertUtils.isEmpty(line.getDeviateReason())) {
                    throw new JeecgBootException(String.format(
                            "明细[%s]强制指定库位必须填写强制上架原因", detail.getSkuId()));
                }
                loca = putawayService.checkForcedLoca(putawayRequest, targetLocaId);
            } else {
                // 人工指定的库位照样要过第 1 层硬过滤（IN-03/IN-05/IN-06 全在这里）
                loca = putawayService.checkTargetLoca(putawayRequest, targetLocaId);
            }

            WmsItemSku sku = skuMapper.selectById(detail.getSkuId());
            WmsItemSpu skuSpu = sku == null || sku.getSpuId() == null ? null : spuMapper.selectById(sku.getSpuId());
            // 商品侧按 SPU 单位存，库位占用体积/重量按 cm³ / g 累加，先换算再累加
            int unitVolume = WmsUnitConverter.skuUnitVolumeCm3(sku, skuSpu);
            int unitWeight = WmsUnitConverter.skuUnitWeightG(sku, skuSpu);

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

            // 上架记录 + 偏离留痕：强制指定无论是否命中推荐都算偏离，必须留原因
            boolean deviate = forced || recommendLocaId == null || !recommendLocaId.equals(targetLocaId);
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
                    .setDeviateReason(deviate
                            ? (forced ? "[强制上架] " + line.getDeviateReason().trim()
                                      : resolveDeviateReason(line.getDeviateReason()))
                            : null)
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
        boolean allPassDone = details.stream()
                .allMatch(d -> putawayCountOf(d.getId()) >= qcPassOf(d));
        if (allPassDone) {
            checkAndCompleteEntryIfEligible(entry.getId(), now);
            WmsWarehousingEntry refreshed = baseMapper.selectById(entry.getId());
            entry.setStatus(refreshed.getStatus());
        } else {
            entry.setStatus(WmsEntryStatusEnum.PUTAWAY.getCode()).setUpdateTime(now);
            baseMapper.updateById(entry);
        }
        boolean allDone = entry.getStatus() == WmsEntryStatusEnum.COMPLETED.getCode();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entryCode", entry.getEntryCode());
        result.put("status", entry.getStatus());
        result.put("statusText", statusDesc(entry.getStatus()));
        result.put("putawayCount", totalPutaway);
        result.put("deviateRows", deviateRows);
        result.put("message", allDone
                ? String.format("入库完成：上架 %d 件，其中偏离推荐 %d 行", totalPutaway, deviateRows)
                : (allPassDone
                    ? String.format("上架 %d 件，本单合格品已全部上架完毕。剩余不合格品请在「质检单」中进行退货安置以完成结单", totalPutaway)
                    : String.format("上架 %d 件，仍有未上架的合格品，单据保持上架中", totalPutaway)));
        return result;
    }

    @Override
    public List<WmsPutawayPage> listPutawayRecords(Long entryId) {
        return entryUpMapper.selectListByEntryId(entryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> handleQcReturn(WmsQcReturnForm form) {
        if (form == null || form.getQcId() == null) {
            throw new JeecgBootException("请指定质检单");
        }
        if (form.getReturnLocaId() == null) {
            throw new JeecgBootException("请选择退货货架库位");
        }
        WmsQualityCheck qc = qualityCheckMapper.selectById(form.getQcId());
        if (qc == null) {
            throw new JeecgBootException("质检单不存在: " + form.getQcId());
        }
        int failCount = qc.getFailCount() == null ? 0 : qc.getFailCount();
        if (failCount <= 0) {
            throw new JeecgBootException("质检单[" + qc.getQcCode() + "]无不合格品，无需退货处理");
        }
        if (qc.getReturnStatus() != null && qc.getReturnStatus() == QC_RETURN_STATUS_SHIPPED) {
            throw new JeecgBootException("质检单[" + qc.getQcCode() + "]已完成退还供应商，不能重复处置");
        }
        Date now = new Date();
        String returnOrderNo = qc.getReturnOrderNo();
        if (oConvertUtils.isEmpty(returnOrderNo)) {
            returnOrderNo = nextCode(RETURN_CODE_PREFIX);
        }
        String remark = form.getReturnRemark() != null ? form.getReturnRemark().trim() : null;

        // 若尚未落库退货区，执行落库
        if (qc.getReturnStatus() == null || qc.getReturnStatus() < QC_RETURN_STATUS_STORED) {
            doPlaceInReturnArea(qc, form.getReturnLocaId(), failCount, returnOrderNo, remark, now);
        }

        // 更新质检单
        qualityCheckMapper.updateById(new WmsQualityCheck()
                .setId(qc.getId())
                .setReturnLocaId(form.getReturnLocaId())
                .setReturnStatus(QC_RETURN_STATUS_STORED)
                .setReturnOrderNo(returnOrderNo)
                .setReturnRemark(remark)
                .setReturnHandleTime(now)
                .setReturnHandleBy(currentUsername())
                .setUpdateTime(now));

        // 检查关联的入库单是否可以结单
        if (qc.getWarehousingEntryId() != null) {
            checkAndCompleteEntryIfEligible(qc.getWarehousingEntryId(), now);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("qcCode", qc.getQcCode());
        result.put("returnOrderNo", returnOrderNo);
        result.put("returnStatus", QC_RETURN_STATUS_STORED);
        result.put("message", String.format("退货处理成功：已将 %d 件不合格品放置到退货货架区，退货单号 %s", failCount, returnOrderNo));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> shipQcReturn(WmsQcShipReturnForm form) {
        if (form == null || form.getQcId() == null) {
            throw new JeecgBootException("请指定质检单");
        }
        WmsQualityCheck qc = qualityCheckMapper.selectById(form.getQcId());
        if (qc == null) {
            throw new JeecgBootException("质检单不存在: " + form.getQcId());
        }
        if (qc.getReturnStatus() == null || qc.getReturnStatus() != QC_RETURN_STATUS_STORED) {
            throw new JeecgBootException("只有「已入退货区待退供应商」的记录可以确认退还，当前状态：" + qc.getReturnStatus());
        }
        if (qc.getReturnLocaId() == null) {
            throw new JeecgBootException("未指定退货货架库位，无法出库扣减");
        }
        WmsWarehouseAreaLoca loca = locaMapper.selectById(qc.getReturnLocaId());
        if (loca == null) {
            throw new JeecgBootException("退货库位不存在: " + qc.getReturnLocaId());
        }
        int failCount = qc.getFailCount() == null ? 0 : qc.getFailCount();
        Date now = new Date();
        WmsWarehousingEntry entry = baseMapper.selectById(qc.getWarehousingEntryId());
        WmsWarehousingEntryDetails detail = detailsMapper.selectById(qc.getEntryDetailsId());
        String supplierName = "";
        if (detail != null && detail.getSupplierId() != null) {
            WmsSupplier supplier = supplierMapper.selectById(detail.getSupplierId());
            if (supplier != null && oConvertUtils.isNotEmpty(supplier.getSupplierName())) {
                supplierName = supplier.getSupplierName();
            }
        }

        // 扣减退货货架区的不良品库存
        WmsInventoryDimension dim = new WmsInventoryDimension()
                .setWarehouseId(loca.getWarehouseId())
                .setWarehouseAreaId(loca.getWarehouseAreaId())
                .setWarehouseAreaLocaId(loca.getId())
                .setConsignorId(entry == null ? null : entry.getConsignorId())
                .setSkuId(qc.getSkuId())
                .setBatchNumber(qc.getBatchNumber())
                .setQualityStatus(QUALITY_BAD)
                .setProdDate(detail == null ? null : detail.getProdDate())
                .setExpireDate(detail == null ? null : detail.getExpireDate());

        String shipRemark = form.getShipRemark() != null ? form.getShipRemark().trim() : "";
        String logRemark = String.format("确认退还供应商出库：退货单[%s]，供应商[%s]%s",
                qc.getReturnOrderNo(),
                oConvertUtils.isNotEmpty(supplierName) ? supplierName : "无",
                oConvertUtils.isNotEmpty(shipRemark) ? "，发运说明：" + shipRemark : "");

        inventoryService.applyChange(dim, -failCount, 0, 0,
                WmsInventoryBizTypeEnum.OUTBOUND_DEDUCT,
                qc.getReturnOrderNo(),
                logRemark);

        WmsItemSku sku = skuMapper.selectById(qc.getSkuId());
        WmsItemSpu spu = sku == null || sku.getSpuId() == null ? null : spuMapper.selectById(sku.getSpuId());
        int unitVolume = WmsUnitConverter.skuUnitVolumeCm3(sku, spu);
        int unitWeight = WmsUnitConverter.skuUnitWeightG(sku, spu);
        touchLocaUsage(loca.getId(), -unitVolume * failCount, -unitWeight * failCount);

        // SN 状态流转为已退货 (6)
        if (detail != null && entry != null) {
            registerReturnedSnOnShip(detail, entry, countOf(failCount), now);
        }

        String updatedRemark = oConvertUtils.isNotEmpty(qc.getReturnRemark())
                ? qc.getReturnRemark() + (oConvertUtils.isNotEmpty(shipRemark) ? " | 出库发运：" + shipRemark : " | 已退还供应商")
                : (oConvertUtils.isNotEmpty(shipRemark) ? "出库发运：" + shipRemark : "已退还供应商");

        qualityCheckMapper.updateById(new WmsQualityCheck()
                .setId(qc.getId())
                .setReturnStatus(QC_RETURN_STATUS_SHIPPED)
                .setReturnRemark(updatedRemark)
                .setUpdateTime(now));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("qcCode", qc.getQcCode());
        result.put("returnOrderNo", qc.getReturnOrderNo());
        result.put("returnStatus", QC_RETURN_STATUS_SHIPPED);
        result.put("message", String.format("退还供应商完成：退货单号 %s，出库扣减 %d 件不良品", qc.getReturnOrderNo(), failCount));
        return result;
    }

    @Override
    public List<WmsWarehouseAreaLoca> listReturnLocaOptions(Long qcId, Integer warehouseId) {
        Integer targetWhId = warehouseId;
        if (targetWhId == null && qcId != null) {
            WmsQualityCheck qc = qualityCheckMapper.selectById(qcId);
            if (qc != null && qc.getWarehousingEntryId() != null) {
                WmsWarehousingEntry entry = baseMapper.selectById(qc.getWarehousingEntryId());
                if (entry != null) {
                    targetWhId = entry.getWarehouseId();
                }
            }
        }
        if (targetWhId == null) {
            throw new JeecgBootException("请指定质检单或仓库");
        }
        // 查询该仓库 category = 'RETURN' 的库区
        List<WmsWarehouseArea> returnAreas = areaMapper.selectList(new LambdaQueryWrapper<WmsWarehouseArea>()
                .eq(WmsWarehouseArea::getWarehouseId, targetWhId)
                .eq(WmsWarehouseArea::getCategory, AREA_RETURN)
                .orderByAsc(WmsWarehouseArea::getId));
        if (returnAreas.isEmpty()) {
            // 如果仓库没有退货不良区，自动创建一个默认退货库区和库位
            WmsWarehouseArea defaultArea = new WmsWarehouseArea()
                    .setWarehouseId(targetWhId)
                    .setName("退货不良品区")
                    .setCategory(AREA_RETURN)
                    .setStorageCondition("NORMAL")
                    .setCreateTime(new Date())
                    .setCreateBy("system")
                    .setIsDel(1);
            areaMapper.insert(defaultArea);
            returnAreas = Collections.singletonList(defaultArea);
        }
        List<Integer> areaIds = returnAreas.stream().map(WmsWarehouseArea::getId).collect(Collectors.toList());
        List<WmsWarehouseAreaLoca> locas = locaMapper.selectList(new LambdaQueryWrapper<WmsWarehouseAreaLoca>()
                .eq(WmsWarehouseAreaLoca::getWarehouseId, targetWhId)
                .in(WmsWarehouseAreaLoca::getWarehouseAreaId, areaIds)
                .notIn(WmsWarehouseAreaLoca::getLocaStatus, 3, 4) // 排除盘点锁定与禁用
                .orderByAsc(WmsWarehouseAreaLoca::getLocaCode));
        if (locas.isEmpty()) {
            // 为退货区自动创建一个默认退货货架库位
            WmsWarehouseArea area = returnAreas.get(0);
            WmsWarehouseAreaLoca defaultLoca = new WmsWarehouseAreaLoca()
                    .setWarehouseId(targetWhId)
                    .setWarehouseAreaId(area.getId())
                    .setLocaCode("R-01-01")
                    .setLocaStatus(1)
                    .setLocalVolume(10000000)
                    .setLocalBearingCapacity(5000000)
                    .setUsedVolume(0)
                    .setUsedWeight(0)
                    .setTunnelNumber("1")
                    .setShelfNumber("1")
                    .setLayerNumber("1")
                    .setPositionNumber("1")
                    .setAbcClass("C")
                    .setStorageCondition("NORMAL")
                    .setAllowMixSku(1)
                    .setAllowMixBatch(1)
                    .setCreateTime(new Date())
                    .setCreateBy("system")
                    .setIsDel(1);
            locaMapper.insert(defaultLoca);
            locas = Collections.singletonList(defaultLoca);
        }
        return locas;
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
                if (planLine.getLocaId() == null) {
                    // 待人工指定的占位行没有落点，整单提交时必须由页面带上库位，这里跳过
                    continue;
                }
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

    /** 刷新库位占用体积、重量、状态与绑定 */
    private void touchLocaUsage(Integer locaId, int addVolume, int addWeight) {
        if (locaCompatibilityService != null && locaId != null) {
            locaCompatibilityService.refreshLocaOccupancy(locaId);
        }
    }

    /** 取该明细最近一条分拣行 */
    private WmsWarehousingEntryDetailsSort findSortRow(Long detailId) {
        return detailsSortMapper.selectOne(new LambdaQueryWrapper<WmsWarehousingEntryDetailsSort>()
                .eq(WmsWarehousingEntryDetailsSort::getWarehousingEntryDetailsId, detailId)
                .orderByDesc(WmsWarehousingEntryDetailsSort::getId)
                .last("LIMIT 1"), false);
    }

    /**
     * 明细已上架的良品数量。
     * 破损件 / 质检不合格品也会写 wms_warehousing_entry_up 留痕（qualityStatus = 3），
     * 这里必须只统计良品，否则会把它们算进「已上架」导致入库单提前结单。
     */
    private int putawayCountOf(Long detailId) {
        List<WmsWarehousingEntryUp> rows = entryUpMapper.selectList(
                new LambdaQueryWrapper<WmsWarehousingEntryUp>()
                        .eq(WmsWarehousingEntryUp::getEntryDetailsId, detailId)
                        .eq(WmsWarehousingEntryUp::getStatus, UP_DONE));
        return rows.stream()
                .filter(r -> r.getQualityStatus() == null || r.getQualityStatus() == QUALITY_GOOD)
                .mapToInt(r -> countOf(r.getCount()))
                .sum();
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
        return resolveExpireDate(sku, line != null ? line.getProdDate() : null, line != null ? line.getExpireDate() : null);
    }

    private Date resolveExpireDate(WmsItemSku sku, Date prodDate, Date expireDate) {
        if (expireDate != null) {
            return expireDate;
        }
        if (prodDate == null || sku == null || sku.getSpuId() == null) {
            return null;
        }
        WmsItemSpu spu = spuMapper.selectById(sku.getSpuId());
        if (spu == null || spu.getShelfLifeDays() == null || spu.getShelfLifeDays() <= 0) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(prodDate);
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

    /**
     * 将质检不合格品放置到退货不良货架区（RETURN库区）
     */
    private void doPlaceInReturnArea(WmsQualityCheck qc, Integer returnLocaId, int count,
                                     String returnOrderNo, String remark, Date now) {
        if (returnLocaId == null) {
            throw new JeecgBootException("请指定退货货架库位");
        }
        WmsWarehouseAreaLoca loca = locaMapper.selectById(returnLocaId);
        if (loca == null) {
            throw new JeecgBootException("指定的退货库位不存在: " + returnLocaId);
        }
        if (loca.getLocaStatus() != null && loca.getLocaStatus() == 4) {
            throw new JeecgBootException("退货库位[" + loca.getLocaCode() + "]已禁用");
        }
        if (loca.getLocaStatus() != null && loca.getLocaStatus() == 3) {
            throw new JeecgBootException("退货库位[" + loca.getLocaCode() + "]正在盘点锁定");
        }
        WmsWarehouseArea area = areaMapper.selectById(loca.getWarehouseAreaId());
        if (area == null || !AREA_RETURN.equals(area.getCategory())) {
            throw new JeecgBootException("库位[" + loca.getLocaCode() + "]所在库区类型非退货不良区(RETURN)，不合格品只能放入退货货架区");
        }
        WmsWarehousingEntry entry = baseMapper.selectById(qc.getWarehousingEntryId());
        if (entry != null && !entry.getWarehouseId().equals(loca.getWarehouseId())) {
            throw new JeecgBootException("退货库位[" + loca.getLocaCode() + "]与入库单所属仓库不一致");
        }

        WmsWarehousingEntryDetails detail = detailsMapper.selectById(qc.getEntryDetailsId());
        String supplierName = "";
        if (detail != null && detail.getSupplierId() != null) {
            WmsSupplier supplier = supplierMapper.selectById(detail.getSupplierId());
            if (supplier != null && oConvertUtils.isNotEmpty(supplier.getSupplierName())) {
                supplierName = supplier.getSupplierName();
            }
        }

        WmsItemSku sku = skuMapper.selectById(qc.getSkuId());
        WmsItemSpu spu = sku == null || sku.getSpuId() == null ? null : spuMapper.selectById(sku.getSpuId());
        int unitVolume = WmsUnitConverter.skuUnitVolumeCm3(sku, spu);
        int unitWeight = WmsUnitConverter.skuUnitWeightG(sku, spu);

        // 1. 库存更新：不良品库存增加 (qualityStatus = 3)
        WmsInventoryDimension dim = new WmsInventoryDimension()
                .setWarehouseId(loca.getWarehouseId())
                .setWarehouseAreaId(loca.getWarehouseAreaId())
                .setWarehouseAreaLocaId(loca.getId())
                .setConsignorId(entry == null ? null : entry.getConsignorId())
                .setSkuId(qc.getSkuId())
                .setBatchNumber(qc.getBatchNumber())
                .setQualityStatus(QUALITY_BAD)
                .setProdDate(detail == null ? null : detail.getProdDate())
                .setExpireDate(detail == null ? null : detail.getExpireDate());

        String logRemark = String.format("质检不合格品放置退货区：库位[%s]，供应商[%s]，退货单号[%s]%s",
                loca.getLocaCode(),
                oConvertUtils.isNotEmpty(supplierName) ? supplierName : "无",
                returnOrderNo,
                oConvertUtils.isNotEmpty(remark) ? "，处置说明：" + remark : "");

        WmsInventory inventory = inventoryService.applyChange(dim, count, 0, 0,
                WmsInventoryBizTypeEnum.INBOUND_PUTAWAY,
                entry == null ? qc.getQcCode() : entry.getEntryCode(),
                logRemark);

        // 2. 占用体积与重量更新
        if (inventory != null) {
            inventoryMapper.updateUsage(inventory.getId(),
                    countOf(inventory.getUsedVolume()) + unitVolume * count,
                    countOf(inventory.getUsedWeight()) + unitWeight * count);
        }
        touchLocaUsage(loca.getId(), unitVolume * count, unitWeight * count);

        // 3. 上架记录（标记为不良品 3）
        WmsWarehousingEntryDetailsSort sortRow = findSortRow(qc.getEntryDetailsId());
        entryUpMapper.insert(new WmsWarehousingEntryUp()
                .setEntryDetailsId(qc.getEntryDetailsId())
                .setWarehousingEntryId(qc.getWarehousingEntryId())
                .setSkuId(qc.getSkuId())
                .setBatchNumber(qc.getBatchNumber())
                .setWarehouseId(loca.getWarehouseId())
                .setWarehouseAreaId(loca.getWarehouseAreaId())
                .setWarehouseAreaLocaId(loca.getId())
                .setCount(count)
                .setQualityStatus(QUALITY_BAD)
                .setStrategyUsed("RETURN")
                .setRecommendLocaId(loca.getId())
                .setIsDeviate(0)
                .setDeviateReason(null)
                .setIsOverflow(0)
                .setDetailsSortId(sortRow != null ? sortRow.getId() : null)
                .setStatus(UP_DONE)
                .setCreateTime(now)
                .setCreateBy(currentUsername())
                .setIsDel(1));

        // 4. SN 登记为在库不良（如果有 SN）
        if (detail != null && entry != null) {
            registerBadSnOnReturn(detail, entry, loca.getId(), count, entry.getWarehouseId(), now);
        }
    }

    /**
     * 收货清点检出破损：破损件直接分流到退货不良区（RETURN）货架落库，
     * 并生成一条来源为「收货破损」的退货记录（复用质检单的退货处置 / 退还供应商流程）。
     */
    private void placeDamageIntoReturnArea(WmsWarehousingEntry entry, WmsWarehousingEntryDetails detail,
                                          int damage, Date now) {
        // 幂等：同一明细已生成过破损退货记录就不再重复落库（重复收货时避免库存翻倍）
        long exists = qualityCheckMapper.selectCount(new LambdaQueryWrapper<WmsQualityCheck>()
                .eq(WmsQualityCheck::getEntryDetailsId, detail.getId())
                .eq(WmsQualityCheck::getSource, QC_SOURCE_DAMAGE));
        if (exists > 0) {
            log.info("明细[{}]已存在破损退货记录，跳过重复落库", detail.getId());
            return;
        }

        List<WmsWarehouseAreaLoca> returnLocas = listReturnLocaOptions(null, entry.getWarehouseId());
        if (returnLocas == null || returnLocas.isEmpty()) {
            throw new JeecgBootException("当前仓库没有可用的退货不良区货架库位，无法安置破损件");
        }
        WmsWarehouseAreaLoca loca = returnLocas.get(0);

        String supplierName = "";
        if (detail.getSupplierId() != null) {
            WmsSupplier supplier = supplierMapper.selectById(detail.getSupplierId());
            if (supplier != null && oConvertUtils.isNotEmpty(supplier.getSupplierName())) {
                supplierName = supplier.getSupplierName();
            }
        }

        WmsItemSku sku = skuMapper.selectById(detail.getSkuId());
        WmsItemSpu spu = sku == null || sku.getSpuId() == null ? null : spuMapper.selectById(sku.getSpuId());
        int unitVolume = WmsUnitConverter.skuUnitVolumeCm3(sku, spu);
        int unitWeight = WmsUnitConverter.skuUnitWeightG(sku, spu);

        String qcCode = nextCode(DAMAGE_CODE_PREFIX);
        String returnOrderNo = nextCode(RETURN_CODE_PREFIX);

        // 1. 库存：退货不良区增加不良品库存（qualityStatus = 3）
        WmsInventoryDimension dim = new WmsInventoryDimension()
                .setWarehouseId(loca.getWarehouseId())
                .setWarehouseAreaId(loca.getWarehouseAreaId())
                .setWarehouseAreaLocaId(loca.getId())
                .setConsignorId(entry.getConsignorId())
                .setSkuId(detail.getSkuId())
                .setBatchNumber(detail.getBatchNumber())
                .setQualityStatus(QUALITY_BAD)
                .setProdDate(detail.getProdDate())
                .setExpireDate(detail.getExpireDate());
        String logRemark = String.format("收货清点破损入退货区：库位[%s]，供应商[%s]，破损单号[%s]，数量[%d]",
                loca.getLocaCode(),
                oConvertUtils.isNotEmpty(supplierName) ? supplierName : "无",
                returnOrderNo, damage);
        WmsInventory inventory = inventoryService.applyChange(dim, damage, 0, 0,
                WmsInventoryBizTypeEnum.INBOUND_PUTAWAY, entry.getEntryCode(), logRemark);
        if (inventory != null) {
            inventoryMapper.updateUsage(inventory.getId(),
                    countOf(inventory.getUsedVolume()) + unitVolume * damage,
                    countOf(inventory.getUsedWeight()) + unitWeight * damage);
        }
        touchLocaUsage(loca.getId(), unitVolume * damage, unitWeight * damage);

        // 2. 上架记录留痕（不良品 3），供明细/追溯查询
        WmsWarehousingEntryDetailsSort sortRow = findSortRow(detail.getId());
        entryUpMapper.insert(new WmsWarehousingEntryUp()
                .setEntryDetailsId(detail.getId())
                .setWarehousingEntryId(entry.getId())
                .setSkuId(detail.getSkuId())
                .setBatchNumber(detail.getBatchNumber())
                .setWarehouseId(loca.getWarehouseId())
                .setWarehouseAreaId(loca.getWarehouseAreaId())
                .setWarehouseAreaLocaId(loca.getId())
                .setCount(damage)
                .setQualityStatus(QUALITY_BAD)
                .setStrategyUsed("DAMAGE_RETURN")
                .setRecommendLocaId(loca.getId())
                .setIsDeviate(0)
                .setIsOverflow(0)
                .setDetailsSortId(sortRow != null ? sortRow.getId() : null)
                .setStatus(UP_DONE)
                .setCreateTime(now)
                .setCreateBy(currentUsername())
                .setIsDel(1));

        // 3. 破损件 SN 登记为在库不良
        registerBadSnOnReturn(detail, entry, loca.getId(), damage, entry.getWarehouseId(), now);

        // 4. 生成破损退货记录（负数/不合格结论 + 已入退货区状态），
        //    质检单页面即可联系供应商并执行「确认退还供应商」出库核减
        qualityCheckMapper.insert(new WmsQualityCheck()
                .setQcCode(qcCode)
                .setWarehousingEntryId(entry.getId())
                .setEntryDetailsId(detail.getId())
                .setMesWorkOrderNo(entry.getMesWorkOrderNo())
                .setSkuId(detail.getSkuId())
                .setBatchNumber(detail.getBatchNumber())
                .setQcType(WmsQcTypeEnum.FULL.getCode())
                .setSource(QC_SOURCE_DAMAGE)
                .setCheckCount(damage)
                .setSampleCount(damage)
                .setPassCount(0)
                .setFailCount(damage)
                .setFailReason("【收货破损】收货清点检出破损 " + damage + " 件")
                .setReturnOrderNo(returnOrderNo)
                .setReturnLocaId(loca.getId())
                .setReturnStatus(QC_RETURN_STATUS_STORED)
                .setReturnRemark("破损件已放置退货不良区，待联系供应商退换")
                .setReturnHandleTime(now)
                .setReturnHandleBy(currentUsername())
                .setQcTime(now)
                .setStatus(WmsQcStatusEnum.FAIL.getCode())
                .setCreateTime(now)
                .setCreateBy(currentUsername())
                .setIsDel(1));
    }

    /** 退货入库时登记不良品 SN */
    private void registerBadSnOnReturn(WmsWarehousingEntryDetails detail, WmsWarehousingEntry entry,
                                       Integer locaId, int count, Integer warehouseId, Date now) {
        WmsItemSku sku = skuMapper.selectById(detail.getSkuId());
        boolean snManaged = sku != null && sku.getIsSnManaged() != null && sku.getIsSnManaged() == 1;
        if (!snManaged) {
            return;
        }
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
                    .setQualityStatus(QUALITY_BAD)
                    .setStatus(SN_IN_STOCK_BAD)
                    .setUpdateTime(now));
            done++;
        }
        for (int i = done; i < count; i++) {
            itemSnMapper.insert(new WmsItemSn()
                    .setSnCode("BAD-" + entry.getEntryCode() + "-" + detail.getId() + "-" + (i + 1)
                            + "-" + ThreadLocalRandom.current().nextInt(1000, 9999))
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setWarehouseId(warehouseId)
                    .setWarehouseAreaLocaId(locaId)
                    .setQualityStatus(QUALITY_BAD)
                    .setEntryOrderId(entry.getId())
                    .setMesWorkOrderNo(entry.getMesWorkOrderNo())
                    .setStatus(SN_IN_STOCK_BAD)
                    .setCreateTime(now)
                    .setCreateBy(currentUsername())
                    .setIsDel(1));
        }
    }

    /** 确认退还供应商时将 SN 状态置为已退货 */
    private void registerReturnedSnOnShip(WmsWarehousingEntryDetails detail, WmsWarehousingEntry entry,
                                          int count, Date now) {
        WmsItemSku sku = skuMapper.selectById(detail.getSkuId());
        boolean snManaged = sku != null && sku.getIsSnManaged() != null && sku.getIsSnManaged() == 1;
        if (!snManaged) {
            return;
        }
        List<WmsItemSn> badSnList = itemSnMapper.selectList(new LambdaQueryWrapper<WmsItemSn>()
                .eq(WmsItemSn::getSkuId, detail.getSkuId())
                .eq(WmsItemSn::getBatchNumber, detail.getBatchNumber())
                .eq(WmsItemSn::getStatus, SN_IN_STOCK_BAD)
                .orderByAsc(WmsItemSn::getId)
                .last("LIMIT " + count));
        for (WmsItemSn sn : badSnList) {
            itemSnMapper.updateById(new WmsItemSn()
                    .setId(sn.getId())
                    .setStatus(SN_RETURNED)
                    .setUpdateTime(now));
        }
    }

    /**
     * 检查入库单是否满足结单条件：
     * 入库单核心关注良品入库流程，所有合格品上架完成即算入库单完成（置为 60 已完成入库）。
     * 不合格品与破损品作为独立的退货分支异步流转（质检单/退货单负责退还供应商出库核减），互不阻塞。
     */
    private void checkAndCompleteEntryIfEligible(Long entryId, Date now) {
        WmsWarehousingEntry entry = baseMapper.selectById(entryId);
        if (entry == null || entry.getStatus() == WmsEntryStatusEnum.COMPLETED.getCode()) {
            return;
        }
        List<WmsWarehousingEntryDetails> details = detailsMapper.selectList(
                new LambdaQueryWrapper<WmsWarehousingEntryDetails>()
                        .eq(WmsWarehousingEntryDetails::getWarehousingEntryId, entryId));
        boolean allPassPutaway = details.stream()
                .allMatch(d -> putawayCountOf(d.getId()) >= qcPassOf(d));

        if (allPassPutaway) {
            entry.setStatus(WmsEntryStatusEnum.COMPLETED.getCode()).setUpdateTime(now);
            baseMapper.updateById(entry);
        }
    }

    private JeecgBootException buildNoPutawayReason(Long entryId) {
        List<WmsWarehousingEntryDetails> all = detailsMapper.selectList(
                new LambdaQueryWrapper<WmsWarehousingEntryDetails>()
                        .eq(WmsWarehousingEntryDetails::getWarehousingEntryId, entryId));
        int totalPass = all.stream().mapToInt(this::qcPassOf).sum();
        int totalPutaway = all.stream().mapToInt(d -> putawayCountOf(d.getId())).sum();
        int totalFail = all.stream().mapToInt(d -> countOf(d.getQcFailCount())).sum();

        if (totalPass > 0 && totalPutaway >= totalPass) {
            if (totalFail > 0) {
                return new JeecgBootException(String.format("本单合格品（共%d件）已全部上架完毕。剩余 %d 件不合格品，请前往「质检单」进行退货处置以完成结单",
                        totalPass, totalFail));
            } else {
                return new JeecgBootException(String.format("本单合格品（共%d件）已全部上架完毕", totalPass));
            }
        }
        if (totalPass == 0) {
            if (totalFail > 0) {
                return new JeecgBootException(String.format("本单质检无合格品（不合格 %d 件），不合格品请前往「质检单」进行退货处置", totalFail));
            } else {
                return new JeecgBootException("本单尚无质检合格品，请先完成质检");
            }
        }
        return new JeecgBootException("本单没有可上架的合格品");
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
