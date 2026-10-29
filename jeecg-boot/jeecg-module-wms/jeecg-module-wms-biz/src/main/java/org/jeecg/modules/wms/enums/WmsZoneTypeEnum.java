package org.jeecg.modules.wms.enums;

/**
 * 分区类型（方案 3.5.2）。与字典 wms_zone_type 的 item_value 一一对应。
 *
 * 商品侧由 SKU/SPU 属性推导（{@link #derive}），库区侧存在
 * wms_warehouse_area.zone_type 上，两者按 {@link #fallbacks()} 匹配。
 * 推导结果与库区分区必须能对上，否则硬过滤阶段直接排除。
 */
public enum WmsZoneTypeEnum {

    /** 贵品：spu.is_valuable=1 或单价 ≥ zone_valuable_price */
    VALUABLE("VALUABLE", "贵品"),

    /** 大件：单件体积 / 重量 / 最长边超过阈值 */
    LARGE("LARGE", "大件"),

    /** 小件：单件体积 ≤ zone_small_volume */
    SMALL("SMALL", "小件"),

    /** 通用：以上都不满足 */
    GENERAL("GENERAL", "通用");

    private final String code;
    private final String desc;

    WmsZoneTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static WmsZoneTypeEnum of(String code) {
        if (code == null || code.trim().isEmpty()) {
            return GENERAL;
        }
        for (WmsZoneTypeEnum e : values()) {
            if (e.code.equalsIgnoreCase(code.trim())) {
                return e;
            }
        }
        return GENERAL;
    }

    /**
     * 找不到对应分区时允许兜底到的分区（方案 3.5.2 兜底表）。
     * 贵品、大件不允许兜底——安全与物理尺寸上放不进去就是放不进去。
     */
    public WmsZoneTypeEnum[] fallbacks() {
        switch (this) {
            case SMALL:
                return new WmsZoneTypeEnum[]{SMALL, GENERAL};
            case GENERAL:
                return new WmsZoneTypeEnum[]{GENERAL, SMALL};
            default:
                return new WmsZoneTypeEnum[]{this};
        }
    }

    /**
     * 商品分区推导，命中即停，优先级：贵品 → 大件 → 小件 → 通用。
     *
     * @param valuable      spu.is_valuable = 1
     * @param price         单价（取不到按 0）
     * @param valuablePrice 贵品单价阈值 zone_valuable_price
     * @param volume        单件体积 cm³（已换算到基准单位）
     * @param weight        单件重量 g（已换算到基准单位）
     * @param maxSide       最长边 mm（已换算到基准单位）
     * @param largeVolume   大件体积阈值 zone_large_volume
     * @param largeWeight   大件重量阈值 zone_large_weight
     * @param largeSide     大件边长阈值 zone_large_side
     * @param smallVolume   小件体积阈值 zone_small_volume
     */
    public static WmsZoneTypeEnum derive(boolean valuable, double price, double valuablePrice,
                                         int volume, int weight, int maxSide,
                                         int largeVolume, int largeWeight, int largeSide, int smallVolume) {
        if (valuable || price >= valuablePrice) {
            return VALUABLE;
        }
        if (volume >= largeVolume || weight >= largeWeight || maxSide >= largeSide) {
            return LARGE;
        }
        if (volume <= smallVolume) {
            return SMALL;
        }
        return GENERAL;
    }
}
