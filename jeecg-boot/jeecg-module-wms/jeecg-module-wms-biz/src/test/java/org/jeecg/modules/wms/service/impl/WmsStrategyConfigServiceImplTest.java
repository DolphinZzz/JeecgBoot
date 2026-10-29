package org.jeecg.modules.wms.service.impl;

import org.jeecg.modules.wms.entity.WmsStrategyConfig;
import org.jeecg.modules.wms.mapper.WmsStrategyConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WmsStrategyConfigServiceImplTest {

    @Mock
    private WmsStrategyConfigMapper strategyConfigMapper;

    @InjectMocks
    private WmsStrategyConfigServiceImpl strategyConfigService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(strategyConfigService, "baseMapper", strategyConfigMapper);
    }

    @Test
    void testGetConfigValueWithWarehouseOverride() {
        WmsStrategyConfig warehouseConfig = new WmsStrategyConfig()
                .setId(10L)
                .setWarehouseId(1)
                .setConfigKey("loca_weight_distance")
                .setConfigValue("0.7");

        doReturn(warehouseConfig).when(strategyConfigMapper).selectOne(any(), anyBoolean());

        String val = strategyConfigService.getConfigValue(1, "loca_weight_distance", "0.5");
        assertEquals("0.7", val);
        assertEquals(0.7, strategyConfigService.getDoubleConfig(1, "loca_weight_distance", 0.5));
    }

    @Test
    void testGetConfigValueFallbackToGlobal() {
        WmsStrategyConfig globalConfig = new WmsStrategyConfig()
                .setId(1L)
                .setWarehouseId(0)
                .setConfigKey("loca_a_ratio")
                .setConfigValue("0.25");

        doReturn(null)
                .doReturn(globalConfig)
                .when(strategyConfigMapper).selectOne(any(), anyBoolean());

        String val = strategyConfigService.getConfigValue(2, "loca_a_ratio", "0.20");
        assertEquals("0.25", val);
        assertEquals(0.25, strategyConfigService.getDoubleConfig(2, "loca_a_ratio", 0.20));
    }

    @Test
    void testGetConfigValueDefaultWhenNotFound() {
        doReturn(null).when(strategyConfigMapper).selectOne(any(), anyBoolean());

        String val = strategyConfigService.getConfigValue(3, "non_existent_key", "default_val");
        assertEquals("default_val", val);
        assertEquals(99.0, strategyConfigService.getDoubleConfig(3, "non_existent_key", 99.0));
        assertEquals(10, strategyConfigService.getIntConfig(3, "non_existent_key", 10));
        assertTrue(strategyConfigService.getBooleanConfig(3, "non_existent_key", true));
    }
}
