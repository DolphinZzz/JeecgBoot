package org.jeecg.modules.wms.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.entity.WmsQualityCheck;
import org.jeecg.modules.wms.entity.WmsWarehouse;
import org.jeecg.modules.wms.entity.WmsWarehousingEntry;
import org.jeecg.modules.wms.entity.WmsWarehousingEntryDetails;
import org.jeecg.modules.wms.enums.WmsEntryStatusEnum;
import org.jeecg.modules.wms.enums.WmsOverReceiveStatusEnum;
import org.jeecg.modules.wms.enums.WmsQcStatusEnum;
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
                detail(1L, 100).setReceivedCount(100).setQcStatus(WmsQcStatusEnum.PASS.getCode())));

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
                .setStrategyUsed("ABC").setZoneType("GENERAL").setNeedCount(12).setRemainCount(0)
                .addLine(new org.jeecg.modules.wms.vo.WmsPutawayPlanLine()
                        .setLocaId(6).setLocaCode("L-06").setCount(12).setCapacity(416)));

        org.jeecg.modules.wms.vo.WmsPutawayPlan plan = entryService.recommendPutaway(ENTRY_ID);

        assertEquals(12, plan.getNeedCount(), "12 件合格品应全部进入上架推荐");
        assertEquals(12, plan.plannedCount());
        assertEquals(1L, plan.getLines().get(0).getDetailId(), "落点要回填明细ID，页面才能按明细分组");
        verify(putawayService).recommend(ArgumentMatchers.argThat(r -> r.getCount() == 12));
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
}
