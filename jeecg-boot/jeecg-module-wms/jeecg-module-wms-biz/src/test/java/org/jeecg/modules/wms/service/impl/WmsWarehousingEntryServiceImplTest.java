package org.jeecg.modules.wms.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.dto.ErpWarehousingEntryCreateDto;
import org.jeecg.modules.wms.dto.ErpWarehousingEntryDetailDto;
import org.jeecg.modules.wms.entity.WmsConsignor;
import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.entity.WmsQualityCheck;
import org.jeecg.modules.wms.entity.WmsWarehouse;
import org.jeecg.modules.wms.entity.WmsWarehousingEntry;
import org.jeecg.modules.wms.entity.WmsWarehousingEntryDetails;
import org.jeecg.modules.wms.enums.WmsEntryStatusEnum;
import org.jeecg.modules.wms.enums.WmsOverReceiveStatusEnum;
import org.jeecg.modules.wms.enums.WmsQcStatusEnum;
import org.jeecg.modules.wms.vo.WmsWarehousingEntryResultVo;
import org.jeecg.modules.wms.vo.WmsEntryQuery;
import java.util.List;
import org.jeecg.modules.wms.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.mapper.WmsItemSkuMapper;
import org.jeecg.modules.wms.mapper.WmsItemSnMapper;
import org.jeecg.modules.wms.mapper.WmsItemSpuMapper;
import org.jeecg.modules.wms.entity.WmsSupplier;
import org.jeecg.modules.wms.entity.WmsWarehouseArea;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.enums.WmsInventoryBizTypeEnum;
import org.jeecg.modules.wms.mapper.WmsSupplierMapper;
import org.jeecg.modules.wms.vo.WmsQcReturnForm;
import org.jeecg.modules.wms.vo.WmsQcShipReturnForm;
import org.jeecg.modules.wms.mapper.WmsQualityCheckMapper;
import org.jeecg.modules.wms.mapper.WmsConsignorMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseMapper;
import org.jeecg.modules.wms.mapper.WmsWarehousingEntryDetailsMapper;
import org.jeecg.modules.wms.mapper.WmsWarehousingEntryDetailsSortMapper;
import org.jeecg.modules.wms.mapper.WmsWarehousingEntryMapper;
import org.jeecg.modules.wms.mapper.WmsWarehousingEntryUpMapper;
import org.jeecg.modules.wms.service.IWmsInventoryService;
import org.jeecg.modules.wms.service.IWmsPutawayService;
import org.jeecg.modules.wms.vo.WmsEntryCreateForm;
import org.jeecg.modules.wms.vo.WmsQcSubmitForm;
import org.jeecg.modules.wms.vo.WmsReceiveForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 入库闭环的状态机与硬规则。
 *
 * 验收要点（方案 3.2.3 / 7.2）：
 * IN-01 禁止跳步：未收货不能提交质检、未质检不能上架；
 * IN-02 工业仓生产入库质检必须绑定 MES 工单；
 * IN-04 超收按仓型分级：工业仓硬拦截，普通仓挂 90 等审批；
 * IN-08 少收自动登记待补料。
 */
@ExtendWith(MockitoExtension.class)
public class WmsWarehousingEntryServiceImplTest {

    private static final Long ENTRY_ID = 1L;

    @Mock
    private WmsWarehousingEntryMapper entryMapper;

    @Mock
    private WmsWarehousingEntryDetailsMapper detailsMapper;

    @Mock
    private WmsWarehousingEntryDetailsSortMapper detailsSortMapper;

    @Mock
    private WmsWarehousingEntryUpMapper entryUpMapper;

    @Mock
    private WmsQualityCheckMapper qualityCheckMapper;

    @Mock
    private WmsInventoryMapper inventoryMapper;

    @Mock
    private WmsItemSkuMapper skuMapper;

    @Mock
    private WmsItemSpuMapper spuMapper;

    @Mock
    private WmsItemSnMapper itemSnMapper;

    @Mock
    private WmsWarehouseMapper warehouseMapper;

    @Mock
    private WmsConsignorMapper consignorMapper;

    @Mock
    private WmsSupplierMapper supplierMapper;

    @Mock
    private WmsWarehouseAreaMapper areaMapper;

    @Mock
    private WmsWarehouseAreaLocaMapper locaMapper;

    @Mock
    private IWmsInventoryService inventoryService;

    @Mock
    private IWmsPutawayService putawayService;

    @InjectMocks
    private WmsWarehousingEntryServiceImpl entryService;

    private WmsWarehousingEntry entry;

    @BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 不是构造注入，Mockito 的 @InjectMocks 不会填它，
        // 必须手动塞进去，否则 getEntryForUpdate 会 NPE。
        ReflectionTestUtils.setField(entryService, "baseMapper", entryMapper);
        entry = new WmsWarehousingEntry()
                .setId(ENTRY_ID)
                .setEntryCode("RK202610300001")
                .setEntryType(1)
                .setWarehouseId(1)
                .setConsignorId(1L)
                .setBatchNumber("B001")
                .setTotalCount(100)
                .setStatus(WmsEntryStatusEnum.ARRIVING.getCode())
                .setOverReceiveAuditStatus(WmsOverReceiveStatusEnum.NONE.getCode())
                .setSurpassCount(0)
                .setLackCount(0)
                .setIsDel(1);
        lenient().when(entryMapper.selectOne(any(), anyBoolean())).thenReturn(entry);
    }

    private WmsWarehouse warehouse(String type) {
        return new WmsWarehouse().setId(1).setName("测试仓").setWarehouseType(type);
    }

    private WmsWarehousingEntryDetails detail(long id, int plan) {
        return new WmsWarehousingEntryDetails().setId(id).setWarehousingEntryId(ENTRY_ID)
                .setSkuId(100L).setBatchNumber("B001").setCount(plan)
                .setReceivedCount(0).setQcStatus(WmsQcStatusEnum.PENDING.getCode());
    }

    private WmsReceiveForm receiveForm(long detailId, int received, int damage) {
        WmsReceiveForm.WmsReceiveLine line = new WmsReceiveForm.WmsReceiveLine();
        line.setDetailId(detailId);
        line.setReceivedCount(received);
        line.setDamageCount(damage);
        line.setSnCodes(Collections.emptyList());
        WmsReceiveForm form = new WmsReceiveForm();
        form.setId(ENTRY_ID);
        form.setDetails(Collections.singletonList(line));
        return form;
    }

    // ---------------- 建单 ----------------

    /**
     * 货主必须是货主档案里已启用的一条。这里守住两道：档案不存在、档案已停用。
     * 停用只拦新单，历史单据的货主展示不受影响。
     */
    @Test
    void createRejectsUnknownConsignor() {
        when(warehouseMapper.selectById(1)).thenReturn(warehouse("NORMAL"));
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> entryService.createEntry(createForm()));
        assertTrue(ex.getMessage().contains("货主不存在"), ex.getMessage());
    }

    @Test
    void createRejectsDisabledConsignor() {
        when(warehouseMapper.selectById(1)).thenReturn(warehouse("NORMAL"));
        when(consignorMapper.selectById(1L)).thenReturn(new WmsConsignor()
                .setId(1).setConsignorName("停用货主").setEnableStatus(0));
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> entryService.createEntry(createForm()));
        assertTrue(ex.getMessage().contains("已停用"), ex.getMessage());
        verify(entryMapper, never()).insert(any(WmsWarehousingEntry.class));
    }

    private WmsEntryCreateForm createForm() {
        org.jeecg.modules.wms.vo.WmsEntryDetailForm line = new org.jeecg.modules.wms.vo.WmsEntryDetailForm();
        line.setSkuId(100L);
        line.setCount(10);
        WmsEntryCreateForm form = new WmsEntryCreateForm();
        form.setEntryType(1);
        form.setWarehouseId(1);
        form.setConsignorId(1L);
        form.setDetails(Collections.singletonList(line));
        return form;
    }

    // ---------------- 收货 ----------------

    @Test
    void receiveSetsStatusToReceiving() {
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail(1L, 100)));
        when(warehouseMapper.selectById(1)).thenReturn(warehouse("NORMAL"));
        when(skuMapper.selectById(100L)).thenReturn(new WmsItemSku().setId(100L));

        Map<String, Object> result = entryService.receive(receiveForm(1L, 100, 0));

        assertEquals(WmsEntryStatusEnum.RECEIVING.getCode(), result.get("status"));
        assertEquals(0, result.get("surpassCount"));
        assertEquals(0, result.get("lackCount"));
        verify(entryMapper).updateById(ArgumentMatchers.<WmsWarehousingEntry>argThat(e -> e.getStatus() == WmsEntryStatusEnum.RECEIVING.getCode()));
    }

    @Test
    void shortReceiveRegistersWaitSupplement() {
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail(1L, 100)));
        when(warehouseMapper.selectById(1)).thenReturn(warehouse("NORMAL"));
        when(skuMapper.selectById(100L)).thenReturn(new WmsItemSku().setId(100L));

        Map<String, Object> result = entryService.receive(receiveForm(1L, 80, 0));

        assertEquals(20, result.get("lackCount"));
        assertEquals(true, result.get("waitSupplement"));
        verify(entryMapper).updateById(ArgumentMatchers.<WmsWarehousingEntry>argThat(e ->
                e.getLackCount() != null && e.getLackCount() == 20
                        && e.getWaitSupplementFlag() != null && e.getWaitSupplementFlag() == 1));
    }

    @Test
    void industrialOverReceiveIsBlockedPendingApproval() {
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail(1L, 100)));
        when(warehouseMapper.selectById(1)).thenReturn(warehouse("INDUSTRIAL"));
        when(skuMapper.selectById(100L)).thenReturn(new WmsItemSku().setId(100L));

        Map<String, Object> result = entryService.receive(receiveForm(1L, 110, 0));

        assertEquals(10, result.get("surpassCount"));
        assertEquals(WmsEntryStatusEnum.ABNORMAL.getCode(), result.get("status"),
                "工业仓超收未放行必须挂起");
        assertEquals(WmsOverReceiveStatusEnum.PENDING.getCode(), result.get("overReceiveAuditStatus"));
    }

    @Test
    void normalWarehouseOverReceiveAlsoHangsForApproval() {
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail(1L, 100)));
        when(warehouseMapper.selectById(1)).thenReturn(warehouse("NORMAL"));
        when(skuMapper.selectById(100L)).thenReturn(new WmsItemSku().setId(100L));

        Map<String, Object> result = entryService.receive(receiveForm(1L, 105, 0));

        assertEquals(WmsEntryStatusEnum.ABNORMAL.getCode(), result.get("status"),
                "普通仓超收走审批，同样挂起");
        assertEquals(WmsOverReceiveStatusEnum.PENDING.getCode(), result.get("overReceiveAuditStatus"));
    }

    @Test
    void damageCannotExceedReceived() {
        lenient().when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail(1L, 100)));
        assertThrows(JeecgBootException.class, () -> entryService.receive(receiveForm(1L, 5, 9)));
    }

    @Test
    void receiveRejectedWhenStatusNotReceivable() {
        entry.setStatus(WmsEntryStatusEnum.COMPLETED.getCode());
        lenient().when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail(1L, 100)));
        assertThrows(JeecgBootException.class, () -> entryService.receive(receiveForm(1L, 100, 0)));
    }

    // ---------------- 超收放行 ----------------

    @Test
    void auditPassReturnsToReceiving() {
        entry.setStatus(WmsEntryStatusEnum.ABNORMAL.getCode())
                .setOverReceiveAuditStatus(WmsOverReceiveStatusEnum.PENDING.getCode())
                .setSurpassCount(10);

        entryService.auditOverReceive(ENTRY_ID, true, "客户同意");

        verify(entryMapper).updateById(ArgumentMatchers.<WmsWarehousingEntry>argThat(e ->
                e.getStatus() == WmsEntryStatusEnum.RECEIVING.getCode()
                        && e.getOverReceiveAuditStatus() == WmsOverReceiveStatusEnum.PASSED.getCode()));
    }

    @Test
    void auditRejectKeepsAbnormal() {
        entry.setStatus(WmsEntryStatusEnum.ABNORMAL.getCode())
                .setOverReceiveAuditStatus(WmsOverReceiveStatusEnum.PENDING.getCode())
                .setSurpassCount(10);

        entryService.auditOverReceive(ENTRY_ID, false, "不允许超收");

        verify(entryMapper).updateById(ArgumentMatchers.<WmsWarehousingEntry>argThat(e ->
                e.getStatus() == WmsEntryStatusEnum.ABNORMAL.getCode()
                        && e.getOverReceiveAuditStatus() == WmsOverReceiveStatusEnum.REJECTED.getCode()));
    }

    @Test
    void auditRejectedWhenNotAbnormal() {
        entry.setStatus(WmsEntryStatusEnum.RECEIVING.getCode());
        assertThrows(JeecgBootException.class,
                () -> entryService.auditOverReceive(ENTRY_ID, true, null));
    }

    // ---------------- 质检 ----------------

    @Test
    void submitToQcRejectedWhenStillArriving() {
        entry.setStatus(WmsEntryStatusEnum.ARRIVING.getCode());
        lenient().when(detailsMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThrows(JeecgBootException.class, () -> entryService.submitToQc(ENTRY_ID),
                "IN-01 禁止跳步：未收货不能提交质检");
    }

    @Test
    void submitToQcRejectedWhenOverReceiveNotPassed() {
        entry.setStatus(WmsEntryStatusEnum.RECEIVING.getCode())
                .setSurpassCount(10)
                .setOverReceiveAuditStatus(WmsOverReceiveStatusEnum.PENDING.getCode());
        assertThrows(JeecgBootException.class, () -> entryService.submitToQc(ENTRY_ID),
                "IN-04 超收未放行不能进质检");
    }

    @Test
    void industrialProductionRequiresMesWorkOrderAtQc() {
        entry.setStatus(WmsEntryStatusEnum.RECEIVING.getCode())
                .setEntryType(2)
                .setMesWorkOrderNo(null);
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(
                detail(1L, 100).setReceivedCount(100)));
        when(warehouseMapper.selectById(1)).thenReturn(warehouse("INDUSTRIAL"));

        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> entryService.submitToQc(ENTRY_ID));
        assertTrue(ex.getMessage().contains("MES"), "IN-02 工业仓生产入库必须绑定 MES 工单");
    }

    @Test
    void submitToQcCreatesRecordsAndMovesToPendingQc() {
        entry.setStatus(WmsEntryStatusEnum.RECEIVING.getCode());
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(
                detail(1L, 100).setReceivedCount(100)));
        when(warehouseMapper.selectById(1)).thenReturn(warehouse("NORMAL"));
        WmsItemSku sku = new WmsItemSku().setId(100L).setQcType(2);
        when(skuMapper.selectById(100L)).thenReturn(sku);
        when(qualityCheckMapper.selectCount(any())).thenReturn(0L);

        Map<String, Object> result = entryService.submitToQc(ENTRY_ID);

        assertEquals(1, result.get("qcCreated"));
        assertEquals(WmsEntryStatusEnum.PENDING_QC.getCode(), result.get("status"));
        verify(qualityCheckMapper).insert(ArgumentMatchers.<WmsQualityCheck>argThat(q ->
                q.getStatus() == WmsQcStatusEnum.PENDING.getCode()
                        && q.getCheckCount() == 100));
        verify(entryMapper).updateById(ArgumentMatchers.<WmsWarehousingEntry>argThat(e -> e.getStatus() == WmsEntryStatusEnum.PENDING_QC.getCode()));
    }

    @Test
    void exemptSkuIsAutoPassed() {
        entry.setStatus(WmsEntryStatusEnum.RECEIVING.getCode());
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(
                detail(1L, 100).setReceivedCount(100)));
        when(warehouseMapper.selectById(1)).thenReturn(warehouse("NORMAL"));
        when(skuMapper.selectById(100L)).thenReturn(new WmsItemSku().setId(100L).setQcType(1));
        when(qualityCheckMapper.selectCount(any())).thenReturn(0L);

        Map<String, Object> result = entryService.submitToQc(ENTRY_ID);

        assertEquals(1, result.get("qcExempt"), "免检明细应自动放行");
        verify(qualityCheckMapper).updateById(ArgumentMatchers.<WmsQualityCheck>argThat(q -> q.getStatus() == WmsQcStatusEnum.PASS.getCode()));
    }

    @Test
    void submitQcMovesToPendingPutawayWhenAllDecided() {
        WmsQualityCheck record = new WmsQualityCheck().setId(9L).setQcCode("QC001")
                .setWarehousingEntryId(ENTRY_ID).setEntryDetailsId(1L)
                .setSkuId(100L).setCheckCount(100).setQcType(2)
                .setStatus(WmsQcStatusEnum.PENDING.getCode());
        when(qualityCheckMapper.selectById(9L)).thenReturn(record);
        // 提交后实现会重新查明细判断「是否全部出结论」，mock 里直接给出已出结论的状态
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(
                detail(1L, 100).setReceivedCount(100).setQcPassCount(100).setQcStatus(WmsQcStatusEnum.PASS.getCode())));

        WmsQcSubmitForm form = new WmsQcSubmitForm();
        form.setQcIds(Collections.singletonList(9L));
        entryService.submitQc(form);

        verify(entryMapper).updateById(ArgumentMatchers.<WmsWarehousingEntry>argThat(e ->
                e.getStatus() == WmsEntryStatusEnum.PENDING_PUTAWAY.getCode()));
    }

    @Test
    void partialPassKeepsQcStatusPartial() {
        WmsQualityCheck record = new WmsQualityCheck().setId(9L).setQcCode("QC001")
                .setWarehousingEntryId(ENTRY_ID).setEntryDetailsId(1L)
                .setSkuId(100L).setCheckCount(100).setQcType(3)
                .setStatus(WmsQcStatusEnum.PENDING.getCode());
        when(qualityCheckMapper.selectById(9L)).thenReturn(record);
        when(detailsMapper.selectList(any())).thenReturn(Arrays.asList(
                detail(1L, 100).setReceivedCount(100)));

        WmsQcSubmitForm.WmsQcSubmitLine line = new WmsQcSubmitForm.WmsQcSubmitLine();
        line.setQcId(9L);
        line.setPassCount(90);
        line.setFailCount(10);
        WmsQcSubmitForm form = new WmsQcSubmitForm();
        form.setQcIds(Collections.singletonList(9L));
        form.setLines(Collections.singletonList(line));
        entryService.submitQc(form);

        verify(qualityCheckMapper).updateById(ArgumentMatchers.<WmsQualityCheck>argThat(q ->
                q.getStatus() == WmsQcStatusEnum.PARTIAL.getCode()
                        && q.getPassCount() == 90 && q.getFailCount() == 10));
    }

    @Test
    void qcCountsCannotExceedCheckCount() {
        WmsQualityCheck record = new WmsQualityCheck().setId(9L).setQcCode("QC001")
                .setWarehousingEntryId(ENTRY_ID).setEntryDetailsId(1L)
                .setSkuId(100L).setCheckCount(100).setQcType(3)
                .setStatus(WmsQcStatusEnum.PENDING.getCode());
        when(qualityCheckMapper.selectById(9L)).thenReturn(record);

        WmsQcSubmitForm.WmsQcSubmitLine line = new WmsQcSubmitForm.WmsQcSubmitLine();
        line.setQcId(9L);
        line.setPassCount(80);
        line.setFailCount(30);
        WmsQcSubmitForm form = new WmsQcSubmitForm();
        form.setQcIds(Collections.singletonList(9L));
        form.setLines(Collections.singletonList(line));

        assertThrows(JeecgBootException.class, () -> entryService.submitQc(form));
    }

    @Test
    void qcCannotBeSubmittedTwice() {
        WmsQualityCheck record = new WmsQualityCheck().setId(9L).setQcCode("QC001")
                .setWarehousingEntryId(ENTRY_ID).setEntryDetailsId(1L)
                .setSkuId(100L).setCheckCount(100).setQcType(2)
                .setStatus(WmsQcStatusEnum.PASS.getCode());
        when(qualityCheckMapper.selectById(9L)).thenReturn(record);

        WmsQcSubmitForm form = new WmsQcSubmitForm();
        form.setQcIds(Collections.singletonList(9L));
        assertThrows(JeecgBootException.class, () -> entryService.submitQc(form));
    }

    // ---------------- 上架 ----------------

    @Test
    void recommendRejectedBeforeQcDecided() {
        entry.setStatus(WmsEntryStatusEnum.PENDING_QC.getCode());
        when(entryMapper.selectById(ENTRY_ID)).thenReturn(entry);
        assertThrows(JeecgBootException.class, () -> entryService.recommendPutaway(ENTRY_ID),
                "IN-01 禁止跳步：未质检不能上架");
    }

    @Test
    void confirmPutawayRejectedBeforeQcDecided() {
        entry.setStatus(WmsEntryStatusEnum.RECEIVING.getCode());
        org.jeecg.modules.wms.vo.WmsPutawayConfirmForm form =
                new org.jeecg.modules.wms.vo.WmsPutawayConfirmForm();
        form.setId(ENTRY_ID);
        assertThrows(JeecgBootException.class, () -> entryService.confirmPutaway(form));
    }

    /**
     * 待上架量必须是「质检合格数 - 已上架数」。
     * 早先写成「实收 - 合格数」，合格品一般等于实收，差值恒为 0，
     * 结果整单永远报「本单没有可上架的合格品」，上架环节彻底走不通。
     */
    @Test
    void recommendUsesQcPassCountNotReceivedCount() {
        entry.setStatus(WmsEntryStatusEnum.PENDING_PUTAWAY.getCode());
        when(entryMapper.selectById(ENTRY_ID)).thenReturn(entry);
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(
                detail(1L, 12).setReceivedCount(12)
                        .setQcStatus(WmsQcStatusEnum.PASS.getCode())
                        .setQcPassCount(12).setQcFailCount(0)));
        when(entryUpMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(putawayService.recommend(any())).thenReturn(new org.jeecg.modules.wms.vo.WmsPutawayPlan()
                .setStrategyUsed("ABC").setNeedCount(12).setRemainCount(0)
                .addLine(new org.jeecg.modules.wms.vo.WmsPutawayPlanLine()
                        .setLocaId(6).setLocaCode("L-06").setCount(12).setCapacity(416)));

        org.jeecg.modules.wms.vo.WmsPutawayPlan plan = entryService.recommendPutaway(ENTRY_ID);

        assertEquals(12, plan.getNeedCount(), "12 件合格品应全部进入上架推荐");
        assertEquals(12, plan.plannedCount());
        assertEquals(1L, plan.getLines().get(0).getDetailId(), "落点要回填明细ID，页面才能按明细分组");
        verify(putawayService).recommend(ArgumentMatchers.argThat(r -> r.getCount() == 12));
    }

    /**
     * 推荐为空时必须仍给出一行「待人工指定」的占位。
     * 之前整单返回空 lines，页面上表格一行都没有，作业员连强制指定库位都无从下手。
     */
    @Test
    void recommendWithoutCandidatesEmitsManualLine() {
        entry.setStatus(WmsEntryStatusEnum.PENDING_PUTAWAY.getCode());
        when(entryMapper.selectById(ENTRY_ID)).thenReturn(entry);
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(
                detail(1L, 1).setReceivedCount(1)
                        .setQcStatus(WmsQcStatusEnum.PASS.getCode())
                        .setQcPassCount(1).setQcFailCount(0)));
        when(entryUpMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(putawayService.recommend(any())).thenReturn(new org.jeecg.modules.wms.vo.WmsPutawayPlan()
                .setStrategyUsed("ABC").setNeedCount(1).setRemainCount(1)
                .setEmptyReason("本仓 13 个库位均不满足，主要原因：库位状态异常"));

        org.jeecg.modules.wms.vo.WmsPutawayPlan plan = entryService.recommendPutaway(ENTRY_ID);

        assertEquals(1, plan.getLines().size(), "没有合规落点也要给出一行供人工指定");
        org.jeecg.modules.wms.vo.WmsPutawayPlanLine line = plan.getLines().get(0);
        assertTrue(line.isNeedManual());
        assertNull(line.getLocaId(), "占位行不带库位，必须由作业员自己选");
        assertEquals(1, line.getCount());
        assertEquals(1L, line.getDetailId());
        assertEquals("本仓 13 个库位均不满足，主要原因：库位状态异常", plan.getEmptyReason());
    }

    /** 强制指定库位但没填原因：必须拦住，审计要能解释为什么绕过了温区等条件 */
    @Test
    void forcedPutawayRequiresReason() {
        entry.setStatus(WmsEntryStatusEnum.PENDING_PUTAWAY.getCode());
        when(entryMapper.selectOne(any(), anyBoolean())).thenReturn(entry);
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(
                detail(1L, 1).setReceivedCount(1)
                        .setQcStatus(WmsQcStatusEnum.PASS.getCode())
                        .setQcPassCount(1).setQcFailCount(0)));
        when(putawayService.recommend(any())).thenReturn(new org.jeecg.modules.wms.vo.WmsPutawayPlan()
                .setStrategyUsed("ABC").setNeedCount(1).setRemainCount(1));

        org.jeecg.modules.wms.vo.WmsPutawayConfirmForm form = new org.jeecg.modules.wms.vo.WmsPutawayConfirmForm();
        form.setId(ENTRY_ID);
        org.jeecg.modules.wms.vo.WmsPutawayConfirmForm.WmsPutawayLine line =
                new org.jeecg.modules.wms.vo.WmsPutawayConfirmForm.WmsPutawayLine();
        line.setDetailId(1L);
        line.setCount(1);
        line.setLocaId(9);
        line.setForce(true);
        form.setDetails(Collections.singletonList(line));

        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> entryService.confirmPutaway(form));
        assertTrue(ex.getMessage().contains("强制上架原因"), ex.getMessage());
        verify(putawayService, never()).checkForcedLoca(any(), any());
    }

    @Test
    void cancelOnlyAllowedWhenArriving() {
        entry.setStatus(WmsEntryStatusEnum.RECEIVING.getCode());
        assertThrows(JeecgBootException.class, () -> entryService.cancel(ENTRY_ID));
    }

    @Test
    void cancelArrivingSucceeds() {
        entry.setStatus(WmsEntryStatusEnum.ARRIVING.getCode());
        entryService.cancel(ENTRY_ID);
        verify(entryMapper).updateById(ArgumentMatchers.<WmsWarehousingEntry>argThat(e -> e.getStatus() == WmsEntryStatusEnum.CANCELLED.getCode()));
    }

    @Test
    void erpCreateRequiresErpPoNo() {
        ErpWarehousingEntryCreateDto dto = new ErpWarehousingEntryCreateDto();
        dto.setWarehouseId(1);
        dto.setConsignorId(1L);
        assertThrows(JeecgBootException.class, () -> entryService.createEntryFromErp(dto));
    }

    @Test
    void erpCreateAutoGeneratesBatchNumberWhenEmpty() {
        when(warehouseMapper.selectById(1)).thenReturn(new WmsWarehouse().setId(1).setWarehouseType("NORMAL"));
        when(consignorMapper.selectById(1L)).thenReturn(new WmsConsignor().setId(1).setConsignorName("货主A").setEnableStatus(1));
        when(skuMapper.selectById(101L)).thenReturn(new WmsItemSku().setId(101L).setSpuId(1L));

        ErpWarehousingEntryCreateDto dto = new ErpWarehousingEntryCreateDto();
        dto.setErpPoNo("PO-20261031-001");
        dto.setWarehouseId(1);
        dto.setConsignorId(1L);
        ErpWarehousingEntryDetailDto detail = new ErpWarehousingEntryDetailDto();
        detail.setSkuId(101L);
        detail.setCount(10);
        dto.setDetails(Collections.singletonList(detail));

        WmsWarehousingEntryResultVo result = entryService.createEntryFromErp(dto);

        assertNotNull(result);
        assertEquals("PO-20261031-001", result.getErpPoNo());
        assertNotNull(result.getBatchNumber());
        assertTrue(result.getBatchNumber().startsWith("B"));
        assertEquals(10, result.getTotalCount());
        verify(entryMapper).insert(ArgumentMatchers.<WmsWarehousingEntry>argThat(e ->
                e.getBatchNumber().startsWith("B") && "PO-20261031-001".equals(e.getErpPoNo())));
    }

    @Test
    void erpCreatePreservesBatchNumberWhenProvided() {
        when(warehouseMapper.selectById(1)).thenReturn(new WmsWarehouse().setId(1).setWarehouseType("NORMAL"));
        when(consignorMapper.selectById(1L)).thenReturn(new WmsConsignor().setId(1).setConsignorName("货主A").setEnableStatus(1));
        when(skuMapper.selectById(101L)).thenReturn(new WmsItemSku().setId(101L).setSpuId(1L));

        ErpWarehousingEntryCreateDto dto = new ErpWarehousingEntryCreateDto();
        dto.setErpPoNo("PO-20261031-002");
        dto.setBatchNumber("CUSTOM-BATCH-888");
        dto.setWarehouseId(1);
        dto.setConsignorId(1L);
        ErpWarehousingEntryDetailDto detail = new ErpWarehousingEntryDetailDto();
        detail.setSkuId(101L);
        detail.setCount(20);
        dto.setDetails(Collections.singletonList(detail));

        WmsWarehousingEntryResultVo result = entryService.createEntryFromErp(dto);

        assertNotNull(result);
        assertEquals("CUSTOM-BATCH-888", result.getBatchNumber());
        assertEquals(20, result.getTotalCount());
    }

    // ---------------- 质检不合格退货与供应商闭环 ----------------

    @Test
    void submitQcWithFails_autoPendingReturn() {
        WmsQualityCheck record = new WmsQualityCheck().setId(10L).setQcCode("QC010")
                .setWarehousingEntryId(ENTRY_ID).setEntryDetailsId(1L)
                .setSkuId(100L).setCheckCount(50).setQcType(2)
                .setStatus(WmsQcStatusEnum.PENDING.getCode());
        when(qualityCheckMapper.selectById(10L)).thenReturn(record);
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(
                detail(1L, 50).setReceivedCount(50).setQcStatus(WmsQcStatusEnum.FAIL.getCode())));

        WmsQcSubmitForm.WmsQcSubmitLine line = new WmsQcSubmitForm.WmsQcSubmitLine();
        line.setQcId(10L);
        line.setPassCount(0);
        line.setFailCount(50);
        line.setFailReason("包装严重破损");
        WmsQcSubmitForm form = new WmsQcSubmitForm();
        form.setQcIds(Collections.singletonList(10L));
        form.setLines(Collections.singletonList(line));

        entryService.submitQc(form);

        verify(qualityCheckMapper).updateById(ArgumentMatchers.<WmsQualityCheck>argThat(q ->
                q.getId().equals(10L)
                        && q.getFailCount() == 50
                        && q.getReturnStatus() == 1
                        && q.getReturnOrderNo() != null
                        && q.getReturnOrderNo().startsWith("TH")));
    }

    @Test
    void submitQcWithFails_directPlacedInReturnArea() {
        WmsQualityCheck record = new WmsQualityCheck().setId(11L).setQcCode("QC011")
                .setWarehousingEntryId(ENTRY_ID).setEntryDetailsId(1L)
                .setSkuId(100L).setCheckCount(30).setQcType(2)
                .setStatus(WmsQcStatusEnum.PENDING.getCode());
        when(qualityCheckMapper.selectById(11L)).thenReturn(record);
        when(entryMapper.selectById(ENTRY_ID)).thenReturn(entry.setId(ENTRY_ID).setWarehouseId(1).setConsignorId(1L));
        WmsWarehousingEntryDetails d = detail(1L, 30).setReceivedCount(30).setSupplierId(88L);
        when(detailsMapper.selectById(1L)).thenReturn(d);
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(
                detail(1L, 30).setReceivedCount(30).setQcStatus(WmsQcStatusEnum.PARTIAL.getCode())));
        when(supplierMapper.selectById(88L)).thenReturn(new WmsSupplier().setId(88).setSupplierName("测试供应商A"));

        WmsWarehouseAreaLoca loca = new WmsWarehouseAreaLoca().setId(99).setWarehouseId(1).setWarehouseAreaId(10).setLocaCode("RET-01-01").setLocaStatus(1);
        when(locaMapper.selectById(99)).thenReturn(loca);
        WmsWarehouseArea area = new WmsWarehouseArea().setId(10).setWarehouseId(1).setCategory("RETURN");
        when(areaMapper.selectById(10)).thenReturn(area);

        WmsQcSubmitForm.WmsQcSubmitLine line = new WmsQcSubmitForm.WmsQcSubmitLine();
        line.setQcId(11L);
        line.setPassCount(20);
        line.setFailCount(10);
        line.setReturnLocaId(99);
        line.setReturnRemark("已联系供应商张三下周提货");
        WmsQcSubmitForm form = new WmsQcSubmitForm();
        form.setQcIds(Collections.singletonList(11L));
        form.setLines(Collections.singletonList(line));

        entryService.submitQc(form);

        verify(inventoryService).applyChange(any(), eq(10), eq(0), eq(0),
                eq(WmsInventoryBizTypeEnum.INBOUND_PUTAWAY), any(), contains("放置退货区"));
        verify(entryUpMapper).insert(ArgumentMatchers.<org.jeecg.modules.wms.entity.WmsWarehousingEntryUp>argThat(u ->
                u.getQualityStatus() != null && u.getQualityStatus() == 3 && u.getCount() == 10));
        verify(qualityCheckMapper).updateById(ArgumentMatchers.<WmsQualityCheck>argThat(q ->
                q.getId().equals(11L)
                        && q.getReturnStatus() == 2
                        && q.getReturnLocaId().equals(99)
                        && "已联系供应商张三下周提货".equals(q.getReturnRemark())));
    }

    @Test
    void submitQcWithFails_rejectNonReturnArea() {
        WmsQualityCheck record = new WmsQualityCheck().setId(12L).setQcCode("QC012")
                .setWarehousingEntryId(ENTRY_ID).setEntryDetailsId(1L)
                .setSkuId(100L).setCheckCount(10).setQcType(2)
                .setStatus(WmsQcStatusEnum.PENDING.getCode());
        when(qualityCheckMapper.selectById(12L)).thenReturn(record);
        WmsWarehouseAreaLoca storageLoca = new WmsWarehouseAreaLoca().setId(88).setWarehouseId(1).setWarehouseAreaId(20).setLocaCode("S-01-01").setLocaStatus(1);
        when(locaMapper.selectById(88)).thenReturn(storageLoca);
        WmsWarehouseArea storageArea = new WmsWarehouseArea().setId(20).setWarehouseId(1).setCategory("STORAGE");
        when(areaMapper.selectById(20)).thenReturn(storageArea);

        WmsQcSubmitForm.WmsQcSubmitLine line = new WmsQcSubmitForm.WmsQcSubmitLine();
        line.setQcId(12L);
        line.setPassCount(0);
        line.setFailCount(10);
        line.setReturnLocaId(88);
        WmsQcSubmitForm form = new WmsQcSubmitForm();
        form.setQcIds(Collections.singletonList(12L));
        form.setLines(Collections.singletonList(line));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> entryService.submitQc(form));
        assertTrue(ex.getMessage().contains("非退货不良区(RETURN)"));
    }

    @Test
    void handleQcReturn_placesInReturnAreaAndUpdatesStatus() {
        WmsQualityCheck record = new WmsQualityCheck().setId(13L).setQcCode("QC013")
                .setWarehousingEntryId(ENTRY_ID).setEntryDetailsId(1L)
                .setSkuId(100L).setCheckCount(20).setFailCount(20).setPassCount(0)
                .setReturnStatus(1)
                .setStatus(WmsQcStatusEnum.FAIL.getCode());
        when(qualityCheckMapper.selectById(13L)).thenReturn(record);
        when(entryMapper.selectById(ENTRY_ID)).thenReturn(entry.setId(ENTRY_ID).setWarehouseId(1).setConsignorId(1L));
        WmsWarehousingEntryDetails d = detail(1L, 20).setReceivedCount(20).setSupplierId(88L);
        when(detailsMapper.selectById(1L)).thenReturn(d);
        when(supplierMapper.selectById(88L)).thenReturn(new WmsSupplier().setId(88).setSupplierName("测试供应商A"));

        WmsWarehouseAreaLoca loca = new WmsWarehouseAreaLoca().setId(99).setWarehouseId(1).setWarehouseAreaId(10).setLocaCode("RET-01-01").setLocaStatus(1);
        when(locaMapper.selectById(99)).thenReturn(loca);
        WmsWarehouseArea area = new WmsWarehouseArea().setId(10).setWarehouseId(1).setCategory("RETURN");
        when(areaMapper.selectById(10)).thenReturn(area);

        WmsQcReturnForm form = new WmsQcReturnForm();
        form.setQcId(13L);
        form.setReturnLocaId(99);
        form.setReturnRemark("已联系顺丰退回供应商");

        Map<String, Object> result = entryService.handleQcReturn(form);
        assertNotNull(result);
        assertEquals(2, result.get("returnStatus"));
        verify(inventoryService).applyChange(any(), eq(20), eq(0), eq(0),
                eq(WmsInventoryBizTypeEnum.INBOUND_PUTAWAY), any(), contains("放置退货区"));
        verify(qualityCheckMapper).updateById(ArgumentMatchers.<WmsQualityCheck>argThat(q ->
                q.getId().equals(13L) && q.getReturnStatus() == 2 && q.getReturnLocaId().equals(99)));
    }

    @Test
    void shipQcReturn_deductsReturnInventoryAndSetsShipped() {
        WmsQualityCheck record = new WmsQualityCheck().setId(14L).setQcCode("QC014")
                .setWarehousingEntryId(ENTRY_ID).setEntryDetailsId(1L)
                .setSkuId(100L).setCheckCount(15).setFailCount(15).setPassCount(0)
                .setReturnLocaId(99)
                .setReturnOrderNo("TH20261031001")
                .setReturnStatus(2)
                .setStatus(WmsQcStatusEnum.FAIL.getCode());
        when(qualityCheckMapper.selectById(14L)).thenReturn(record);
        WmsWarehouseAreaLoca loca = new WmsWarehouseAreaLoca().setId(99).setWarehouseId(1).setWarehouseAreaId(10).setLocaCode("RET-01-01").setLocaStatus(2);
        when(locaMapper.selectById(99)).thenReturn(loca);
        when(entryMapper.selectById(ENTRY_ID)).thenReturn(entry.setId(ENTRY_ID).setWarehouseId(1).setConsignorId(1L));
        WmsWarehousingEntryDetails d = detail(1L, 15).setReceivedCount(15).setSupplierId(88L);
        when(detailsMapper.selectById(1L)).thenReturn(d);
        when(supplierMapper.selectById(88L)).thenReturn(new WmsSupplier().setId(88).setSupplierName("测试供应商A"));

        WmsQcShipReturnForm form = new WmsQcShipReturnForm();
        form.setQcId(14L);
        form.setShipRemark("顺丰单号 SF1234567890已发运");

        Map<String, Object> result = entryService.shipQcReturn(form);
        assertNotNull(result);
        assertEquals(3, result.get("returnStatus"));
        verify(inventoryService).applyChange(any(), eq(-15), eq(0), eq(0),
                eq(WmsInventoryBizTypeEnum.OUTBOUND_DEDUCT), eq("TH20261031001"), contains("确认退还供应商出库"));
        verify(qualityCheckMapper).updateById(ArgumentMatchers.<WmsQualityCheck>argThat(q ->
                q.getId().equals(14L) && q.getReturnStatus() == 3));
    }

    @Test
    void shipQcReturn_rejectIfNotStored() {
        WmsQualityCheck record = new WmsQualityCheck().setId(15L).setQcCode("QC015")
                .setReturnStatus(1);
        when(qualityCheckMapper.selectById(15L)).thenReturn(record);

        WmsQcShipReturnForm form = new WmsQcShipReturnForm();
        form.setQcId(15L);

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> entryService.shipQcReturn(form));
        assertTrue(ex.getMessage().contains("只有「已入退货区待退供应商」的记录可以确认退还"));
    }

    // ---------------- 收货破损入退货区 ----------------

    /** 准备一个可用的退货不良区货架库位 */
    private void mockReturnLoca() {
        when(areaMapper.selectList(any())).thenReturn(Collections.singletonList(
                new WmsWarehouseArea().setId(10).setWarehouseId(1).setCategory("RETURN")));
        when(locaMapper.selectList(any())).thenReturn(Collections.singletonList(
                new WmsWarehouseAreaLoca().setId(99).setWarehouseId(1).setWarehouseAreaId(10)
                        .setLocaCode("R-01-01").setLocaStatus(1)));
    }

    /**
     * 计划 2 件，实收 2 件（破损件也算到货，实收含破损），破损 1 件：
     * 破损数必须回写明细；破损件自动落退货不良区并生成破损退货记录，剩余 1 件才参与质检。
     */
    @Test
    void receivePersistsDamageAndPlacesIntoReturnArea() {
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(
                detail(1L, 2).setSupplierId(88L)));
        when(warehouseMapper.selectById(1)).thenReturn(warehouse("NORMAL"));
        when(skuMapper.selectById(100L)).thenReturn(new WmsItemSku().setId(100L));
        when(supplierMapper.selectById(88L)).thenReturn(new WmsSupplier().setId(88).setSupplierName("测试供应商A"));
        when(qualityCheckMapper.selectCount(any())).thenReturn(0L);
        mockReturnLoca();

        Map<String, Object> result = entryService.receive(receiveForm(1L, 2, 1));

        assertEquals(1, result.get("damageCount"), "整单破损数应汇总为 1");
        // 明细落库破损数
        verify(detailsMapper).updateById(ArgumentMatchers.<WmsWarehousingEntryDetails>argThat(d ->
                d.getId().equals(1L) && d.getDamageCount() != null && d.getDamageCount() == 1));
        // 破损件入退货不良区（不良品 3）
        verify(inventoryService).applyChange(any(), eq(1), eq(0), eq(0),
                eq(WmsInventoryBizTypeEnum.INBOUND_PUTAWAY), any(), contains("收货清点破损入退货区"));
        verify(entryUpMapper).insert(ArgumentMatchers.<org.jeecg.modules.wms.entity.WmsWarehousingEntryUp>argThat(u ->
                u.getQualityStatus() != null && u.getQualityStatus() == 3
                        && u.getCount() == 1 && "DAMAGE_RETURN".equals(u.getStrategyUsed())));
        // 生成可追溯的破损退货记录：已入退货区（2），等待退还供应商
        verify(qualityCheckMapper).insert(ArgumentMatchers.<WmsQualityCheck>argThat(q ->
                q.getSource() != null && q.getSource() == 2
                        && q.getFailCount() == 1
                        && q.getReturnStatus() == 2
                        && q.getReturnLocaId().equals(99)
                        && q.getReturnOrderNo() != null && q.getReturnOrderNo().startsWith("TH")
                        && q.getQcCode().startsWith("PD")));
    }

    /** 破损件不进入质检：送检数 = 实收 − 破损 */
    @Test
    void submitToQcExcludesDamagedFromCheckCount() {
        entry.setStatus(WmsEntryStatusEnum.RECEIVING.getCode());
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(
                detail(1L, 2).setReceivedCount(2).setDamageCount(1).setSupplierId(88L)));
        when(warehouseMapper.selectById(1)).thenReturn(warehouse("NORMAL"));
        when(skuMapper.selectById(100L)).thenReturn(new WmsItemSku().setId(100L).setQcType(2));
        when(qualityCheckMapper.selectCount(any())).thenReturn(0L);

        entryService.submitToQc(ENTRY_ID);

        verify(qualityCheckMapper).insert(ArgumentMatchers.<WmsQualityCheck>argThat(q ->
                q.getCheckCount() == 1 && q.getSampleCount() <= 1));
    }

    /** 整单全是破损：不生成质检单，破损件入退货区后入库单直接走结单判定 */
    @Test
    void allDamagedSkipsQcAndCompletes() {
        entry.setStatus(WmsEntryStatusEnum.RECEIVING.getCode());
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(
                detail(1L, 2).setReceivedCount(2).setDamageCount(2).setQcPassCount(0).setSupplierId(88L)));
        when(warehouseMapper.selectById(1)).thenReturn(warehouse("NORMAL"));
        when(entryMapper.selectById(ENTRY_ID)).thenReturn(entry.setId(ENTRY_ID).setWarehouseId(1).setConsignorId(1L));
        when(entryUpMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = entryService.submitToQc(ENTRY_ID);

        assertEquals(0, result.get("qcCreated"));
        assertTrue(String.valueOf(result.get("message")).contains("无完好件"));
        verify(qualityCheckMapper, never()).insert(any(WmsQualityCheck.class));
        // 结单判定直接改的是同一个 entry 实例，最终状态应为已完成
        assertEquals(WmsEntryStatusEnum.COMPLETED.getCode(), entry.getStatus());
        verify(entryMapper, atLeastOnce()).updateById(any(WmsWarehousingEntry.class));
    }

    /** 破损上架留痕不能算进「良品已上架」，否则入库单会提前结单 */
    @Test
    void putawayCountIgnoresDamagedRows() {
        entry.setStatus(WmsEntryStatusEnum.PENDING_PUTAWAY.getCode());
        when(entryMapper.selectById(ENTRY_ID)).thenReturn(entry);
        when(detailsMapper.selectList(any())).thenReturn(Collections.singletonList(
                detail(1L, 2).setReceivedCount(2).setDamageCount(1)
                        .setQcStatus(WmsQcStatusEnum.PARTIAL.getCode())
                        .setQcPassCount(1).setQcFailCount(0)));
        when(entryUpMapper.selectList(any())).thenReturn(Collections.singletonList(
                new org.jeecg.modules.wms.entity.WmsWarehousingEntryUp()
                        .setEntryDetailsId(1L).setCount(1).setQualityStatus(3).setStatus(2)));
        when(putawayService.recommend(any())).thenReturn(new org.jeecg.modules.wms.vo.WmsPutawayPlan()
                .setStrategyUsed("ABC").setNeedCount(1).setRemainCount(1));

        org.jeecg.modules.wms.vo.WmsPutawayPlan plan = entryService.recommendPutaway(ENTRY_ID);

        // 唯一一件良品还没上架，仍应给出上架推荐，而不是被破损留痕当成已上架而跳过
        assertEquals(1, plan.getLines().size());
        assertEquals(1, plan.getLines().get(0).getCount());
    }

    @Test
    void queryEntryPageSupportsMultiStatus() {
        WmsEntryQuery query = new WmsEntryQuery();
        query.setStatus("40,50");
        List<Integer> list = query.getStatusList();
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(40, list.get(0));
        assertEquals(50, list.get(1));

        when(entryMapper.selectEntryPage(any(), eq(query))).thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());
        com.baomidou.mybatisplus.core.metadata.IPage<org.jeecg.modules.wms.vo.WmsEntryPage> res = entryService.queryEntryPage(query, 1, 10);
        assertNotNull(res);
        verify(entryMapper).selectEntryPage(any(), eq(query));
    }
}
