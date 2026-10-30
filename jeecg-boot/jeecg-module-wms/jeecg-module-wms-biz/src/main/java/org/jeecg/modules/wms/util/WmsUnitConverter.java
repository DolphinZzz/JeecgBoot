package org.jeecg.modules.wms.util;

import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.entity.WmsItemSpu;

/**
 * 商品计量 → 库位基准单位换算。
 *
 * 库位侧的长宽高、容积、承重固定按 mm / cm³ / g 存（见 WmsWarehouseAreaLoca），
 * 商品侧的长宽高、体积、重量按 SPU 上选择的 lengthUnit / volumeUnit / weightUnit 存。
 * 两边做容积、承重比较前必须先换算，否则「m/cm」当「mm」用会差 1000 倍。
 *
 * SPU 没选单位（历史数据）时按基准单位 mm / cm³ / g 处理，即认为已经是库位单位。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 */
public final class WmsUnitConverter {

    private static final int MAX_VALUE = Integer.MAX_VALUE;

    private WmsUnitConverter() {
    }

    /** 长度 → mm */
    public static int lengthToMm(Integer value, WmsItemSpu spu) {
        if (value == null || value <= 0) {
            return 0;
        }
        String unit = spu == null || spu.getLengthUnit() == null ? "mm" : spu.getLengthUnit().trim().toLowerCase();
        switch (unit) {
            case "cm":
                return clamp((long) value * 10);
            case "dm":
                return clamp((long) value * 100);
            case "m":
                return clamp((long) value * 1000);
            default:
                return value;
        }
    }

    /** 重量 → g */
    public static int weightToG(Integer value, WmsItemSpu spu) {
        if (value == null || value <= 0) {
            return 0;
        }
        String unit = spu == null || spu.getWeightUnit() == null ? "g" : spu.getWeightUnit().trim().toLowerCase();
        switch (unit) {
            case "mg":
                // 不足 1 g 按 1 g 计，避免被当成「不占承重」
                return Math.max(1, value / 1000);
            case "kg":
                return clamp((long) value * 1000);
            default:
                return value;
        }
    }

    /** 体积 → cm³ */
    public static int volumeToCm3(Integer value, WmsItemSpu spu) {
        if (value == null || value <= 0) {
            return 0;
        }
        String unit = spu == null || spu.getVolumeUnit() == null ? "cm³" : spu.getVolumeUnit().trim().toLowerCase();
        switch (unit) {
            case "dm³":
            case "dm3":
            case "l":
                return clamp((long) value * 1000);
            case "m³":
            case "m3":
                return clamp((long) value * 1_000_000L);
            default:
                return value;
        }
    }

    /**
     * 单件体积（cm³）：优先取商品体积，没维护时用长宽高估算。
     * 返回 0 表示不占容积，调用方按「不限」处理。
     */
    public static int skuUnitVolumeCm3(WmsItemSku sku, WmsItemSpu spu) {
        if (sku == null) {
            return 0;
        }
        int volume = volumeToCm3(sku.getProductVolume(), spu);
        if (volume > 0) {
            return volume;
        }
        long l = lengthToMm(sku.getProductLength(), spu);
        long w = lengthToMm(sku.getProductWidth(), spu);
        long h = lengthToMm(sku.getProductHeight(), spu);
        return l > 0 && w > 0 && h > 0 ? clamp(l * w * h / 1000L) : 0;
    }

    /** 单件重量（g），返回 0 表示不占承重 */
    public static int skuUnitWeightG(WmsItemSku sku, WmsItemSpu spu) {
        return sku == null ? 0 : weightToG(sku.getProductWeight(), spu);
    }

    private static int clamp(long value) {
        return (int) Math.min(value, MAX_VALUE);
    }
}
