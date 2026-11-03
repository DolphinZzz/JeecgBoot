package org.jeecg.modules.wms.service.impl;

import org.jeecg.common.exception.JeecgBootException;
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
import org.jeecg.modules.wms.vo.WmsLocaCapacityItemVO;
import org.jeecg.modules.wms.vo.WmsLocaCapacitySaveDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 库位商品规划与容量设置单元测试
 */
@ExtendWith(MockitoExtension.class)
class WmsLocaItemCapacityServiceImplTest {

    @Mock
    private WmsLocaItemCapacityMapper baseMapper;

    @Mock
    private WmsWarehouseAreaLocaMapper locaMapper;

    @Mock
    private WmsItemSpuMapper spuMapper;

    @Mock
    private WmsItemSkuMapper skuMapper;

    @Mock
    private WmsInventoryMapper inventoryMapper;

    @InjectMocks
    private WmsLocaItemCapacityServiceImpl capacityService;

    private WmsWarehouseAreaLoca testLoca;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(capacityService, "baseMapper", baseMapper);
        testLoca = new WmsWarehouseAreaLoca()
                .setId(101)
                .setWarehouseId(1)
                .setWarehouseAreaId(10)
                .setLocaCode("A-01-01")
                .setLocaStatus(1)
                .setAllowMixSku(0);
    }

    @Test
    @DisplayName("测试超过3个SPU时抛出业务异常")
    void testSaveCapacityExceedsMaxSpuLimit() {
        when(locaMapper.selectById(101)).thenReturn(testLoca);

        WmsLocaCapacitySaveDTO dto = new WmsLocaCapacitySaveDTO()
                .setLocaId(101)
                .setItems(Arrays.asList(
                        new WmsLocaCapacitySaveDTO.CapacityItem().setSpuId(1L).setMaxCount(100),
                        new WmsLocaCapacitySaveDTO.CapacityItem().setSpuId(2L).setMaxCount(50),
                        new WmsLocaCapacitySaveDTO.CapacityItem().setSpuId(3L).setMaxCount(30),
                        new WmsLocaCapacitySaveDTO.CapacityItem().setSpuId(4L).setMaxCount(20) // 第4个
                ));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> capacityService.saveLocaCapacity(dto));
        assertTrue(ex.getMessage().contains("最多只能配置 3 个商品"));
        verify(baseMapper, never()).insert(any(WmsLocaItemCapacity.class));
    }

    @Test
    @DisplayName("测试配置1~3个SPU保存成功且多SPU时自动设置allowMixSku=1")
    void testSaveCapacitySuccessAndEnableMixSku() {
        when(locaMapper.selectById(101)).thenReturn(testLoca);

        WmsLocaCapacitySaveDTO dto = new WmsLocaCapacitySaveDTO()
                .setLocaId(101)
                .setItems(Arrays.asList(
                        new WmsLocaCapacitySaveDTO.CapacityItem().setSpuId(1L).setMaxCount(100),
                        new WmsLocaCapacitySaveDTO.CapacityItem().setSpuId(2L).setMaxCount(50)
                ));

        capacityService.saveLocaCapacity(dto);

        // 验证旧记录被清除并插入两条新记录
        verify(baseMapper, times(2)).insert(any(WmsLocaItemCapacity.class));
        // 验证库位被自动设置为允许混SKU (allowMixSku=1)
        verify(locaMapper).updateById(argThat((WmsWarehouseAreaLoca loca) -> loca.getId().equals(101) && Integer.valueOf(1).equals(loca.getAllowMixSku())));
    }

    @Test
    @DisplayName("测试容量为0或负数时被拦截")
    void testSaveCapacityZeroOrNegativeCount() {
        when(locaMapper.selectById(101)).thenReturn(testLoca);

        WmsLocaCapacitySaveDTO dto = new WmsLocaCapacitySaveDTO()
                .setLocaId(101)
                .setItems(Collections.singletonList(
                        new WmsLocaCapacitySaveDTO.CapacityItem().setSpuId(1L).setMaxCount(0)
                ));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> capacityService.saveLocaCapacity(dto));
        assertTrue(ex.getMessage().contains("最大可存放件数必须大于0"));
    }

    @Test
    @DisplayName("测试查询库位规划列表并核算剩余容量")
    void testListCapacityByLocaId() {
        WmsLocaItemCapacity cap1 = new WmsLocaItemCapacity()
                .setId(1L)
                .setWarehouseAreaLocaId(101)
                .setSpuId(10L)
                .setMaxCount(100);
        when(baseMapper.selectByLocaId(101)).thenReturn(Collections.singletonList(cap1));

        WmsItemSpu spu10 = new WmsItemSpu().setId(10L).setTitle("测试手机").setSubTitle("SPU-PHONE");
        when(spuMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(spu10));

        // 模拟库存：此库位下有 SKU 1001 (属于 SPU 10)，库存 30 件
        WmsInventory inv = new WmsInventory()
                .setWarehouseAreaLocaId(101)
                .setSkuId(1001L)
                .setTotalCount(30);
        when(inventoryMapper.selectList(any())).thenReturn(Collections.singletonList(inv));

        WmsItemSku sku = new WmsItemSku().setId(1001L).setSpuId(10L);
        when(skuMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(sku));

        List<WmsLocaCapacityItemVO> vos = capacityService.listCapacityByLocaId(101);
        assertEquals(1, vos.size());
        WmsLocaCapacityItemVO vo = vos.get(0);
        assertEquals(10L, vo.getSpuId());
        assertEquals("SPU-PHONE", vo.getSpuCode());
        assertEquals("测试手机", vo.getSpuName());
        assertEquals(100, vo.getMaxCount());
        assertEquals(30, vo.getCurrentCount(), "在库件数应为 30");
        assertEquals(70, vo.getRemainCount(), "剩余容量应为 100 - 30 = 70");
    }
}
