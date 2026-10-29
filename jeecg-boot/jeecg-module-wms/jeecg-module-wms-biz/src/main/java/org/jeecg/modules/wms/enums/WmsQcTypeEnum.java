package org.jeecg.modules.wms.enums;

/**
 * 质检方式。与字典 wms_qc_type 的 item_value 一一对应。
 * 默认值挂在 wms_item_sku.qc_type 上，收货生成质检单时按商品带出，允许人工改。
 */
public enum WmsQcTypeEnum {

    /** 免检：不抽样，整批按合格放行 */
    EXEMPT(1, "免检"),

    /** 抽检：按比例抽，抽检合格即整批判合格 */
    SAMPLE(2, "抽检"),

    /** 全检：逐件检 */
    FULL(3, "全检");

    /** 抽检比例（送检数的 10%，至少 1 件），抽检方式下使用 */
    private static final double SAMPLE_RATIO = 0.1;

    private final int code;
    private final String desc;

    WmsQcTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static WmsQcTypeEnum of(Integer code) {
        if (code == null) {
            return SAMPLE;
        }
        for (WmsQcTypeEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return SAMPLE;
    }

    /**
     * 按质检方式计算默认抽检数量。
     * 免检返回 0，全检返回全部，抽检按 10% 向上取整且至少 1 件。
     */
    public int defaultSampleCount(int checkCount) {
        if (checkCount <= 0) {
            return 0;
        }
        switch (this) {
            case EXEMPT:
                return 0;
            case FULL:
                return checkCount;
            default:
                return Math.max(1, (int) Math.ceil(checkCount * SAMPLE_RATIO));
        }
    }
}
