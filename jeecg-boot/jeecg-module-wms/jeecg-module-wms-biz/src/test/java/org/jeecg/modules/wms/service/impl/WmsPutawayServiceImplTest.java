package org.jeecg.modules.wms.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.entity.WmsItemSpu;
import org.jeecg.modules.wms.entity.WmsWarehouseArea;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.entity.WmsWarehouse;
import org.jeecg.modules.wms.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.mapper.WmsItemSkuMapper;
import org.jeecg.modules.wms.mapper.WmsItemSpuMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseMapper;
import org.jeecg.modules.wms.service.IWmsStrategyConfigService;
import org.jeecg.modules.wms.vo.WmsPutawayLocaOption;
import org.jeecg.modules.wms.vo.WmsPutawayPlan;
import org.jeecg.modules.wms.vo.WmsPutawayRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 上架策略引擎：三层结构中每一条硬规则都要单独守住。
 *
 * 验收要点（方案 3.5 / 7.4）：
 * 1. 硬过滤对任何策略都生效，排序策略不能绕过；
 * 2. 分区兜底只在方案允许的方向上生效（大件/贵品不兜底）；
 * 3. 数量拆分按 cap_i 拆到多个库位，放不完的记 remainCount，不静默丢弃；
 * 4. FIXED 的溢出部分要落 overflow 标记。
 */
@ExtendWith(MockitoExtension.class)
public class WmsPutawayServiceImplTest {

    private static final Integer WAREHOUSE_ID = 1;
    private static final Long SKU_ID = 100L;

    @Mock
    private WmsWarehouseAreaLocaMapper locaMapper;

    @Mock
    private WmsWarehouseAreaMapper areaMapper;

    @Mock
    private WmsItemSkuMapper skuMapper;

    @Mock
    private WmsItemSpuMapper spuMapper;

    @Mock
    private WmsInventoryMapper inventoryMapper;

    @Mock
    private WmsWarehouseMapper warehouseMapper;

    @Mock
    private IWmsStrategyConfigService strategyConfigService;

    @InjectMocks
    private WmsPutawayServiceImpl putawayService;

    private WmsItemSku sku;

    @BeforeEach
    void setUp() {
        // 一个普通小件：体积 1000 cm³、重 500 g，正好卡在小件阈值边界
        sku = new WmsItemSku()
                .setId(SKU_ID)
                .setSpuId(1L)
                .setPrice(new BigDecimal("100"))
                .setProductVolume(1000)
                .setProductWeight(500)
                .setProductLength(10)
                .setProductWidth(10)
                .setProductHeight(10);
        // 策略参数默认值：与 wms_strategy_config 的 PUTAWAY / ZONE 分组一致
        lenient().when(strategyConfigService.getConfigValue(anyInt(), eq("putaway_default_strategy"), anyString()))
                .thenReturn("ABC");
        lenient().when(strategyConfigService.getBooleanConfig(anyInt(), eq("putaway_allow_upgrade_occupy"), anyBoolean()))
                .thenReturn(false);
        lenient().when(strategyConfigService.getBooleanConfig(anyInt(), eq("putaway_prefer_same_sku"), anyBoolean()))
                .thenReturn(true);
        lenient().when(strategyConfigService.getBooleanConfig(anyInt(), eq("fixed_loca_lend_when_empty"), anyBoolean()))
                .thenReturn(false);
        lenient().when(strategyConfigService.getDoubleConfig(anyInt(), eq("zone_valuable_price"), anyDouble()))
                .thenReturn(5000D);
        lenient().when(strategyConfigService.getIntConfig(anyInt(), eq("zone_large_volume"), anyInt()))
                .thenReturn(125000);
        lenient().when(strategyConfigService.getIntConfig(anyInt(), eq("zone_large_weight"), anyInt()))
                .thenReturn(25000);
        lenient().when(strategyConfigService.getIntConfig(anyInt(), eq("zone_large_side"), anyInt()))
                .thenReturn(1200);
        lenient().when(strategyConfigService.getIntConfig(anyInt(), eq("zone_small_volume"), anyInt()))
                .thenReturn(1000);
    }

    private WmsItemSpu spu(String storageCondition, int isValuable) {
        return new WmsItemSpu().setId(1L).setStorageCondition(storageCondition).setIsValuable(isValuable);
    }

    private WmsWarehouseArea area(Integer id, String category, String zoneType, String storageCondition) {
        return new WmsWarehouseArea().setId(id).setWarehouseId(WAREHOUSE_ID)
                .setCategory(category).setZoneType(zoneType).setStorageCondition(storageCondition);
    }

    private WmsWarehouseAreaLoca loca(Integer id, String code, Integer areaId, int volume, int bearing) {
        return new WmsWarehouseAreaLoca().setId(id).setWarehouseId(WAREHOUSE_ID)
                .setWarehouseAreaId(areaId).setLocaCode(code)
                .setLocalVolume(volume).setLocalBearingCapacity(bearing)
                .setUsedVolume(0).setUsedWeight(0)
                .setLocaStatus(1).setAllowMixSku(0).setAllowMixBatch(1)
                .setStorageCondition(null).setErgonomicLevel(2);
    }

    private WmsPutawayRequest request(int count) {
        return new WmsPutawayRequest().setWarehouseId(WAREHOUSE_ID).setSkuId(SKU_ID)
                .setBatchNumber("B001").setCount(count).setQualityStatus(1).setConsignorId(1L);
    }

    /** 公共桩：一个 STORAGE/GENERAL 库区 + 一个 SMALL 库区 */
    private void stubAreas(WmsWarehouseArea... areas) {
        when(areaMapper.selectList(any())).thenReturn(Arrays.asList(areas));
        lenient().when(areaMapper.selectById(anyInt())).thenAnswer(inv -> {
            Integer id = inv.getArgument(0);
            return Arrays.stream(areas).filter(a -> a.getId().equals(id)).findFirst().orElse(null);
        });
    }

    @Test
    void recommendLineCarriesAreaNameForDisplay() {
        WmsWarehouseArea storage = area(1, "STORAGE", "GENERAL", "NORMAL").setName("常温存储区A");
        stubAreas(storage);
        when(locaMapper.selectList(any())).thenReturn(Collections.singletonList(loca(1, "L-01", 1, 100000, 100000)));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 0));

        WmsPutawayPlan plan = putawayService.recommend(request(10));

        // 推荐行也要能直接显示库区名，页面不该再拿 areaId 去猜
        assertEquals(1, plan.getLines().size());
        assertEquals("常温存储区A", plan.getLines().get(0).getAreaName());
    }

    @Test
    void disabledLocaMustBeExcluded() {
        stubAreas(area(1, "STORAGE", "GENERAL", "NORMAL"));
        WmsWarehouseAreaLoca disabled = loca(1, "L-01", 1, 100000, 100000).setLocaStatus(4);
        when(locaMapper.selectList(any())).thenReturn(Collections.singletonList(disabled));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 0));

        WmsPutawayPlan plan = putawayService.recommend(request(10));

        assertTrue(plan.getLines().isEmpty(), "禁用库位不能被推荐");
        assertEquals(10, plan.getRemainCount(), "没有可用库位时全部记为未安排");
    }

    @Test
    void stocktakingLockedLocaMustBeExcluded() {        stubAreas(area(1, "STORAGE", "GENERAL", "NORMAL"));
        WmsWarehouseAreaLoca locked = loca(1, "L-01", 1, 100000, 100000).setLocaStatus(3);
        when(locaMapper.selectList(any())).thenReturn(Collections.singletonList(locked));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 0));

        WmsPutawayPlan plan = putawayService.recommend(request(5));

        assertTrue(plan.getLines().isEmpty(), "盘点锁定的库位不能被推荐");
    }

    @Test
    void goodQualityMustNotGoToReturnArea() {
        // 良品只允许 STORAGE / PICKING，RETURN 区必须被排除（IN-03 的反向）
        stubAreas(area(1, "RETURN", "GENERAL", "NORMAL"));
        when(locaMapper.selectList(any())).thenReturn(
                Collections.singletonList(loca(1, "R-01", 1, 100000, 100000)));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 0));

        assertTrue(putawayService.recommend(request(1)).getLines().isEmpty());
    }

    @Test
    void badQualityMustOnlyGoToReturnArea() {
        stubAreas(area(1, "STORAGE", "GENERAL", "NORMAL"), area(2, "RETURN", "GENERAL", "NORMAL"));
        when(locaMapper.selectList(any())).thenReturn(Arrays.asList(
                loca(1, "L-01", 1, 100000, 100000),
                loca(2, "R-01", 2, 100000, 100000)));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 0));

        WmsPutawayPlan plan = putawayService.recommend(request(5).setQualityStatus(3));

        assertEquals(1, plan.getLines().size());
        assertEquals("R-01", plan.getLines().get(0).getLocaCode(), "不良品只能进 RETURN 库区");
    }

    @Test
    void storageConditionMismatchMustBeExcluded() {
        // 商品要冷藏，库区是常温 → 拒绝；温区不允许降级
        stubAreas(area(1, "STORAGE", "GENERAL", "NORMAL"));
        WmsWarehouseAreaLoca normal = loca(1, "L-01", 1, 100000, 100000);
        when(locaMapper.selectList(any())).thenReturn(Collections.singletonList(normal));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("COLD", 0));

        assertTrue(putawayService.recommend(request(1)).getLines().isEmpty(),
                "常温库位不能放冷藏商品");
    }

    @Test
    void matchStorageConditionSucceeds() {
        stubAreas(area(1, "STORAGE", "GENERAL", "COLD"));
        when(locaMapper.selectList(any())).thenReturn(
                Collections.singletonList(loca(1, "L-01", 1, 100000, 100000)));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("COLD", 0));

        WmsPutawayPlan plan = putawayService.recommend(request(1));
        assertEquals(1, plan.getLines().size());
        assertEquals("SMALL", plan.getZoneType(), "1000 cm³ 应推导为小件");
    }

    @Test
    void valuableZoneHasNoFallback() {
        // 贵品不允许兜底：只有 GENERAL 库区时必须报无可放库位
        stubAreas(area(1, "STORAGE", "GENERAL", "NORMAL"));
        when(locaMapper.selectList(any())).thenReturn(
                Collections.singletonList(loca(1, "L-01", 1, 100000, 100000)));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 1));

        WmsPutawayPlan plan = putawayService.recommend(request(3));
        assertEquals("VALUABLE", plan.getZoneType());
        assertTrue(plan.getLines().isEmpty(), "贵品不允许兜底到通用区");
        assertEquals(3, plan.getRemainCount());
    }

    @Test
    void smallZoneFallsBackToGeneral() {
        // 小件可以兜底到通用区
        stubAreas(area(1, "STORAGE", "GENERAL", "NORMAL"));
        when(locaMapper.selectList(any())).thenReturn(
                Collections.singletonList(loca(1, "L-01", 1, 100000, 100000)));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 0));

        WmsPutawayPlan plan = putawayService.recommend(request(3));
        assertEquals("SMALL", plan.getZoneType());
        assertEquals(1, plan.getLines().size(), "小件兜底到通用区应成功");
        assertEquals(3, plan.getLines().get(0).getCount());
    }

    @Test
    void mixSkuForbiddenBlocksLoca() {
        stubAreas(area(1, "STORAGE", "GENERAL", "NORMAL"));
        WmsWarehouseAreaLoca loca = loca(1, "L-01", 1, 100000, 100000).setAllowMixSku(0);
        when(locaMapper.selectList(any())).thenReturn(Collections.singletonList(loca));
        // 库位里已有别的 SKU 的库存
        when(inventoryMapper.selectList(any())).thenReturn(Collections.singletonList(
                new WmsInventory().setWarehouseAreaLocaId(1).setSkuId(999L)
                        .setBatchNumber("B001").setTotalCount(5).setQualityStatus(1)));
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 0));

        assertTrue(putawayService.recommend(request(1)).getLines().isEmpty(),
                "allow_mix_sku=0 的库位不能混放其它 SKU");
    }

    @Test
    void capacitySplitsAcrossLocas() {
        // 两个库位各只能放 5 件（5000 / 1000），要上架 12 件 → 5 + 5 + 剩 2
        stubAreas(area(1, "STORAGE", "GENERAL", "NORMAL"));
        when(locaMapper.selectList(any())).thenReturn(Arrays.asList(
                loca(1, "L-01", 1, 5000, 100000),
                loca(2, "L-02", 1, 5000, 100000)));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 0));

        WmsPutawayPlan plan = putawayService.recommend(request(12));

        assertEquals(2, plan.getLines().size(), "应拆到两个库位");
        assertEquals(5, plan.getLines().get(0).getCount());
        assertEquals(5, plan.getLines().get(1).getCount());
        assertEquals(2, plan.getRemainCount(), "放不完的量要如实记为未安排");
        assertEquals(10, plan.plannedCount());
    }

    @Test
    void volumeLimitWinsWhenTighterThanWeight() {
        // 体积只够 3 件（3000/1000），承重够 200 件 → cap 取体积限制
        stubAreas(area(1, "STORAGE", "GENERAL", "NORMAL"));
        when(locaMapper.selectList(any())).thenReturn(
                Collections.singletonList(loca(1, "L-01", 1, 3000, 100000)));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 0));

        WmsPutawayPlan plan = putawayService.recommend(request(10));
        assertEquals(3, plan.getLines().get(0).getCount());
        assertEquals(7, plan.getRemainCount());
    }

    @Test
    void fixedStrategyOverflowsToOtherLocas() {
        // SKU 用 FIXED 策略；固定库位只能放 2 件，其余溢出到普通库位并打标记
        sku.setPutawayStrategy("FIXED");
        stubAreas(area(1, "STORAGE", "GENERAL", "NORMAL"));
        WmsWarehouseAreaLoca fixed = loca(1, "L-01", 1, 2000, 100000).setFixedSkuId(SKU_ID);
        WmsWarehouseAreaLoca normal = loca(2, "L-02", 1, 100000, 100000);
        when(locaMapper.selectList(any())).thenReturn(Arrays.asList(fixed, normal));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 0));
        when(strategyConfigService.getConfigValue(anyInt(), eq("fixed_overflow_strategy"), anyString()))
                .thenReturn("ABC");

        WmsPutawayPlan plan = putawayService.recommend(request(5));

        assertEquals("FIXED", plan.getStrategyUsed());
        assertEquals(2, plan.getLines().size());
        assertEquals(2, plan.getLines().get(0).getCount(), "固定库位先放满");
        assertFalse(plan.getLines().get(0).isOverflow());
        assertEquals(3, plan.getLines().get(1).getCount(), "溢出部分放到普通库位");
        assertTrue(plan.getLines().get(1).isOverflow(), "溢出落点要打标记");
    }

    @Test
    void fixedLocaCannotBeUsedByOtherSku() {
        // 固定库位绑定别的 SKU，且不允许空位外借 → 直接排除
        stubAreas(area(1, "STORAGE", "GENERAL", "NORMAL"));
        WmsWarehouseAreaLoca fixed = loca(1, "L-01", 1, 100000, 100000).setFixedSkuId(999L);
        when(locaMapper.selectList(any())).thenReturn(Collections.singletonList(fixed));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 0));

        assertTrue(putawayService.recommend(request(1)).getLines().isEmpty(),
                "固定库位即使空着也不借给别的 SKU");
    }

    @Test
    void abcSortPrefersLocaWithHigherScore() {
        stubAreas(area(1, "STORAGE", "GENERAL", "NORMAL"));
        WmsWarehouseAreaLoca low = loca(1, "L-01", 1, 100000, 100000)
                .setLocaScore(new BigDecimal("0.1"));
        WmsWarehouseAreaLoca high = loca(2, "L-02", 1, 100000, 100000)
                .setLocaScore(new BigDecimal("0.9"));
        when(locaMapper.selectList(any())).thenReturn(Arrays.asList(low, high));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 0));

        WmsPutawayPlan plan = putawayService.recommend(request(3));
        assertEquals("L-02", plan.getLines().get(0).getLocaCode(),
                "ABC 策略下库位得分高的优先");
    }

    @Test
    void abcSortPrefersSameSkuOverScore() {
        // 同品集中优先于库位得分：已放同 SKU 的低分库位也排在前面
        stubAreas(area(1, "STORAGE", "GENERAL", "NORMAL"));
        WmsWarehouseAreaLoca low = loca(1, "L-01", 1, 100000, 100000)
                .setLocaScore(new BigDecimal("0.1"));
        WmsWarehouseAreaLoca high = loca(2, "L-02", 1, 100000, 100000)
                .setLocaScore(new BigDecimal("0.9"));
        when(locaMapper.selectList(any())).thenReturn(Arrays.asList(low, high));
        when(inventoryMapper.selectList(any())).thenReturn(Collections.singletonList(
                new WmsInventory().setWarehouseAreaLocaId(1).setSkuId(SKU_ID)
                        .setBatchNumber("B001").setTotalCount(3).setQualityStatus(1)));
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 0));

        WmsPutawayPlan plan = putawayService.recommend(request(3));
        assertEquals("L-01", plan.getLines().get(0).getLocaCode(),
                "同品集中优先于库位得分");
        assertTrue(plan.getLines().get(0).isSameSku());
    }

    @Test
    void nearestStrategySortsByReceiveDistance() {
        sku.setPutawayStrategy("NEAREST");
        stubAreas(area(1, "STORAGE", "GENERAL", "NORMAL"));
        WmsWarehouseAreaLoca far = loca(1, "L-01", 1, 100000, 100000).setReceiveDistance(90);
        WmsWarehouseAreaLoca near = loca(2, "L-02", 1, 100000, 100000).setReceiveDistance(5);
        when(locaMapper.selectList(any())).thenReturn(Arrays.asList(far, near));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 0));

        WmsPutawayPlan plan = putawayService.recommend(request(3));
        assertEquals("NEAREST", plan.getStrategyUsed());
        assertEquals("L-02", plan.getLines().get(0).getLocaCode(), "就近上架按距离升序");
    }

    @Test
    void upgradeOccupyBlockedWhenLocaLevelHigher() {
        // C 类商品不能占 A 类库位（putaway_allow_upgrade_occupy = 0）
        when(skuMapper.selectCurrentAbcClass(WAREHOUSE_ID, SKU_ID)).thenReturn("C");
        stubAreas(area(1, "STORAGE", "GENERAL", "NORMAL"));
        WmsWarehouseAreaLoca aClass = loca(1, "L-A", 1, 100000, 100000).setAbcClass("A");
        when(locaMapper.selectList(any())).thenReturn(Collections.singletonList(aClass));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 0));

        assertTrue(putawayService.recommend(request(1)).getLines().isEmpty(),
                "默认不允许低等级商品占用高等级库位");
    }

    @Test
    void checkTargetLocaRejectsNonReturnForBadQuality() {
        WmsItemSpu spu = spu("NORMAL", 0);
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu);
        when(locaMapper.selectById(1)).thenReturn(loca(1, "L-01", 1, 100000, 100000));
        when(areaMapper.selectById(1)).thenReturn(area(1, "STORAGE", "GENERAL", "NORMAL"));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());

        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> putawayService.checkTargetLoca(request(1).setQualityStatus(3), 1));
        assertTrue(ex.getMessage().contains("RETURN"), "不良品上架到非退货区要给出明确原因");
    }

    @Test
    void countMustBePositive() {
        assertThrows(JeecgBootException.class, () -> putawayService.recommend(request(0)));
    }

    @Test
    void emptyCandidatesReturnsRemainWithoutLines() {
        when(areaMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 0));

        WmsPutawayPlan plan = putawayService.recommend(request(4));
        assertNotNull(plan.getStrategyUsed());
        assertEquals(4, plan.getRemainCount());
        assertTrue(plan.getLines().isEmpty());
        // 推荐为空时必须给出可读原因，页面才能告诉作业员为什么放不下
        assertNotNull(plan.getEmptyReason(), "无落点时应给出原因");
        assertTrue(plan.getEmptyReason().contains("库区"), plan.getEmptyReason());
    }

    @Test
    void emptyReasonPointsAtZoneMismatch() {
        // 贵品商品放在只有 GENERAL 区的仓库：分区不匹配，且贵品不允许兜底
        stubAreas(area(1, "STORAGE", "GENERAL", "NORMAL"));
        when(locaMapper.selectList(any())).thenReturn(Collections.singletonList(loca(1, "L-01", 1, 100000, 100000)));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("NORMAL", 1));

        WmsPutawayPlan plan = putawayService.recommend(request(1));
        assertTrue(plan.getLines().isEmpty());
        assertEquals(1, plan.getRejectedCount());
        assertTrue(plan.getEmptyReason().contains("分区"), plan.getEmptyReason());
        assertTrue(plan.getEmptyReason().contains("贵品"), plan.getEmptyReason());
    }

    @Test
    void locaOptionsListWholeWarehouseWithRejectReasons() {
        WmsWarehouseArea storageArea = area(1, "STORAGE", "GENERAL", "NORMAL").setName("常温存储区A");
        WmsWarehouseArea smallArea = area(2, "PICKING", "SMALL", "NORMAL").setName("拣货区B");
        stubAreas(storageArea, smallArea);
        WmsWarehouseAreaLoca storage = loca(1, "L-01", 1, 100000, 100000)
                .setLocaLen(1200).setLocaWidth(1000).setLocaHeight(400)
                .setTunnelNumber("01").setShelfNumber("A").setLayerNumber("1").setPositionNumber("02");
        WmsWarehouseAreaLoca small = loca(2, "L-02", 2, 100000, 100000);
        when(locaMapper.selectList(any())).thenReturn(Arrays.asList(storage, small));
        when(inventoryMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(skuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu("COLD", 1));
        when(warehouseMapper.selectById(WAREHOUSE_ID)).thenReturn(new WmsWarehouse().setId(WAREHOUSE_ID).setName("上海一号仓"));

        List<WmsPutawayLocaOption> options = putawayService.listLocaOptions(request(1));

        // 下拉列全仓库位（不按分区过滤），但每个选项都带自己的硬过滤结论
        assertEquals(2, options.size());
        WmsPutawayLocaOption first = options.stream()
                .filter(o -> "L-01".equals(o.getLocaCode())).findFirst().orElseThrow();
        assertEquals("常温存储区A", first.getGroup(), "下拉应按库区名称分组");
        assertFalse(first.isAllowed(), "贵品冷库商品放在常温通用区应被标记为不可选");
        assertNotNull(first.getRejectReason(), "不可选的库位必须给出原因");
        assertEquals(Integer.valueOf(100000), first.getRemainVolume());
        // 库位选择器要靠这些字段让作业员认出具体格子，少一个都得回列表页查
        assertEquals("上海一号仓", first.getWarehouseName());
        assertEquals("常温存储区A", first.getAreaName());
        assertEquals("存储", first.getAreaCategoryText());
        assertEquals("通用", first.getZoneTypeText());
        assertEquals("空闲", first.getLocaStatusText());
        assertEquals(Integer.valueOf(1200), first.getLocaLen());
        assertEquals(Integer.valueOf(1000), first.getLocaWidth());
        assertEquals(Integer.valueOf(400), first.getLocaHeight());
        assertEquals("01", first.getTunnelNumber());
        assertEquals("A", first.getShelfNumber());
        assertEquals("1", first.getLayerNumber());
        assertEquals("02", first.getPositionNumber());
        // 剩余可放件数按单件体积 1000 cm³ 折算，承重 500g 不构成瓶颈
        assertEquals(Integer.valueOf(100), first.getRemainCount());
    }

    @Test
    void forcedLocaRejectsCrossWarehouse() {
        WmsWarehouseAreaLoca other = loca(9, "X-01", 1, 100000, 100000).setWarehouseId(99);
        when(locaMapper.selectById(9)).thenReturn(other);

        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> putawayService.checkForcedLoca(request(1), 9));
        assertTrue(ex.getMessage().contains("不属于本仓库"), ex.getMessage());
    }

    @Test
    void forcedLocaRejectsDisabledAndStocktakingLoca() {
        when(locaMapper.selectById(3)).thenReturn(loca(3, "L-03", 1, 1, 1).setLocaStatus(4));
        JeecgBootException disabled = assertThrows(JeecgBootException.class,
                () -> putawayService.checkForcedLoca(request(1), 3));
        assertTrue(disabled.getMessage().contains("已禁用"), disabled.getMessage());

        when(locaMapper.selectById(4)).thenReturn(loca(4, "L-04", 1, 1, 1).setLocaStatus(3));
        JeecgBootException locked = assertThrows(JeecgBootException.class,
                () -> putawayService.checkForcedLoca(request(1), 4));
        assertTrue(locked.getMessage().contains("盘点锁定"), locked.getMessage());
    }

    @Test
    void forcedLocaAllowsZoneAndStorageConditionMismatch() {
        // 强制上架的意义就在这里：分区/温区不匹配也能落库，前提是调用方记录原因
        WmsWarehouseAreaLoca loca = loca(5, "L-05", 1, 100000, 100000);
        when(locaMapper.selectById(5)).thenReturn(loca);

        WmsWarehouseAreaLoca result = putawayService.checkForcedLoca(request(1), 5);
        assertEquals(Integer.valueOf(5), result.getId());
    }
}
