package org.jeecg.modules.wms.service.impl;

import org.jeecg.modules.wms.entity.*;
import org.jeecg.modules.wms.enums.WmsOutboundStatusEnum;
import org.jeecg.modules.wms.enums.WmsPickModeEnum;
import org.jeecg.modules.wms.enums.WmsWaveStatusEnum;
import org.jeecg.modules.wms.mapper.*;
import org.jeecg.modules.wms.vo.WmsWaveGenerateForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 出库波次服务单元测试：
 * 验证按「承运商 + 温区」双维度分组分波算法及波次拣货播种生命周期流转
 */
@ExtendWith(MockitoExtension.class)
public class WmsWaveOrderServiceImplTest {

    @Mock
    private WmsWaveOrderMapper waveOrderMapper;

    @Mock
    private WmsOutboundOrderMapper outboundOrderMapper;

    @Mock
    private WmsOutboundOrderDetailsMapper outboundOrderDetailsMapper;

    @Mock
    private WmsItemSkuMapper itemSkuMapper;

    @Mock
    private WmsItemSpuMapper itemSpuMapper;

    @InjectMocks
    private WmsWaveOrderServiceImpl waveOrderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(waveOrderService, "baseMapper", waveOrderMapper);
    }

    @Test
    @DisplayName("自动分波：按[承运商 + 存储温区]双维度聚合分组生成波次单")
    void testGenerateWaves_GroupedByLogisticsAndTemperature() {
        // 准备 3 个出库单：
        // 订单1: 顺丰(1), 常温
        // 订单2: 顺丰(1), 冷藏
        // 订单3: 京东(2), 常温
        WmsOutboundOrder o1 = new WmsOutboundOrder()
                .setId(101L)
                .setOutboundCode("OB001")
                .setWarehouseId(1)
                .setStatus(WmsOutboundStatusEnum.ALLOCATED.getCode())
                .setPickMode(WmsPickModeEnum.WAVE_SEED.getCode())
                .setLogisticsCompanyId(1L)
                .setTotalCount(10)
                .setIsDel(1);

        WmsOutboundOrder o2 = new WmsOutboundOrder()
                .setId(102L)
                .setOutboundCode("OB002")
                .setWarehouseId(1)
                .setStatus(WmsOutboundStatusEnum.ALLOCATED.getCode())
                .setPickMode(WmsPickModeEnum.WAVE_SEED.getCode())
                .setLogisticsCompanyId(1L)
                .setTotalCount(5)
                .setIsDel(1);

        WmsOutboundOrder o3 = new WmsOutboundOrder()
                .setId(103L)
                .setOutboundCode("OB003")
                .setWarehouseId(1)
                .setStatus(WmsOutboundStatusEnum.ALLOCATED.getCode())
                .setPickMode(WmsPickModeEnum.WAVE_SEED.getCode())
                .setLogisticsCompanyId(2L)
                .setTotalCount(8)
                .setIsDel(1);

        when(outboundOrderMapper.selectList(any())).thenReturn(Arrays.asList(o1, o2, o3));

        // 明细与温区映射
        WmsOutboundOrderDetails d1 = new WmsOutboundOrderDetails().setOutboundOrderId(101L).setSkuId(1L).setIsDel(1);
        WmsOutboundOrderDetails d2 = new WmsOutboundOrderDetails().setOutboundOrderId(102L).setSkuId(2L).setIsDel(1);
        WmsOutboundOrderDetails d3 = new WmsOutboundOrderDetails().setOutboundOrderId(103L).setSkuId(1L).setIsDel(1);
        when(outboundOrderDetailsMapper.selectList(any())).thenReturn(Arrays.asList(d1, d2, d3));

        WmsItemSku sku1 = new WmsItemSku().setId(1L).setSpuId(11L);
        WmsItemSku sku2 = new WmsItemSku().setId(2L).setSpuId(12L);
        when(itemSkuMapper.selectBatchIds(any())).thenReturn(Arrays.asList(sku1, sku2));

        WmsItemSpu spu1 = new WmsItemSpu().setId(11L).setTitle("常温饮料").setStorageCondition("常温");
        WmsItemSpu spu2 = new WmsItemSpu().setId(12L).setTitle("冷藏酸奶").setStorageCondition("冷藏");
        when(itemSpuMapper.selectBatchIds(any())).thenReturn(Arrays.asList(spu1, spu2));

        WmsWaveGenerateForm form = new WmsWaveGenerateForm();
        form.setWarehouseId(1);

        List<WmsWaveOrder> waves = waveOrderService.generateWaves(form);

        // 验证生成 3 个波次单（顺丰+常温、顺丰+冷藏、京东+常温）
        assertNotNull(waves);
        assertEquals(3, waves.size());

        // 验证波次保存与出库单 wave_id 回写
        verify(waveOrderMapper, times(3)).insert(any(WmsWaveOrder.class));
        verify(outboundOrderMapper, times(3)).updateById(any(WmsOutboundOrder.class));
    }

    @Test
    @DisplayName("完成波次分货播种：波次状态流转至5已完成，关联出库单推进至50待复核")
    void testCompleteWaveDistribute_AdvanceOrdersToWaitReview() {
        Long waveId = 500L;
        WmsWaveOrder wave = new WmsWaveOrder()
                .setId(waveId)
                .setWaveCode("WV202610310001")
                .setStatus(WmsWaveStatusEnum.PICKING.getCode())
                .setIsDel(1);
        when(waveOrderMapper.selectById(waveId)).thenReturn(wave);

        WmsOutboundOrder order = new WmsOutboundOrder()
                .setId(101L)
                .setWaveId(waveId)
                .setStatus(WmsOutboundStatusEnum.PICKING.getCode())
                .setIsDel(1);
        when(outboundOrderMapper.selectList(any())).thenReturn(Collections.singletonList(order));

        WmsOutboundOrderDetails detail = new WmsOutboundOrderDetails()
                .setId(201L)
                .setOutboundOrderId(101L)
                .setAllocatedCount(10)
                .setPickedCount(0)
                .setIsDel(1);
        when(outboundOrderDetailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail));

        waveOrderService.completeWaveDistribute(waveId);

        // 验证波次状态变更为 5 已完成
        assertEquals(WmsWaveStatusEnum.COMPLETED.getCode(), wave.getStatus());
        // 验证明细实拣数量自动回填
        assertEquals(10, detail.getPickedCount());
        assertEquals(2, detail.getPickStatus());
        // 验证出库单状态推进为 50 拣货完成待复核
        assertEquals(WmsOutboundStatusEnum.WAIT_REVIEW.getCode(), order.getStatus());
    }
}
