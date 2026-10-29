package org.jeecg.modules.wms.enums;

/**
 * 上架排序策略（方案 3.5.1 第 2 层）。与字典 wms_putaway_strategy 一一对应。
 *
 * 「分区上架」不是独立策略，而是第 1 层硬过滤里的 zone_type 匹配，
 * 所以这里只有三种排序规则。SKU 未配置时取
 * wms_strategy_config.putaway_default_strategy（默认 ABC）。
 */
public enum WmsPutawayStrategyEnum {

    /** 固定库位：先放绑定库位，放不下的部分按 fixed_overflow_strategy 溢出 */
    FIXED("FIXED", "固定库位"),

    /** ABC 驱动：等级匹配 → 同品集中 → 库位得分 → 容量贴合 */
    ABC("ABC", "ABC驱动"),

    /** 就近上架：同品集中（可关）→ 离收货区距离 */
    NEAREST("NEAREST", "就近上架");

    private final String code;
    private final String desc;

    WmsPutawayStrategyEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按编码取枚举。null / 空串 / 未知编码一律兜底 ABC，
     * 因为字典允许商品不上策略，仓库默认值本身也可能是脏数据。
     */
    public static WmsPutawayStrategyEnum of(String code) {
        if (code == null) {
            return ABC;
        }
        String target = code.trim();
        for (WmsPutawayStrategyEnum e : values()) {
            if (e.code.equalsIgnoreCase(target)) {
                return e;
            }
        }
        return ABC;
    }
}
