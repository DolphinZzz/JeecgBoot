package org.jeecg.modules.wms.enums;

/**
 * 出库明细复核状态。与字典 wms_review_status 的 item_value 一一对应。
 */
public enum WmsReviewStatusEnum {

    /** 未复核 */
    UNREVIEWED(0, "未复核"),

    /** 复核通过 */
    PASS(1, "复核通过"),

    /** 复核不通过 */
    REJECT(2, "复核不通过");

    private final int code;
    private final String desc;

    WmsReviewStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static WmsReviewStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (WmsReviewStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
