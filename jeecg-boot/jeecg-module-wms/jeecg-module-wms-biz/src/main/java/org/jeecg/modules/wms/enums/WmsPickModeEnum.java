package org.jeecg.modules.wms.enums;

/**
 * 拣货模式。与字典 wms_pick_mode 的 item_value 一一对应。
 */
public enum WmsPickModeEnum {

    /** 摘果法：按订单/工单逐行拣货 */
    PIECE_PICK(1, "摘果法"),

    /** 波次播种法：按波次汇总拣货，后置播种分货 */
    WAVE_SEED(2, "波次播种法");

    private final int code;
    private final String desc;

    WmsPickModeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static WmsPickModeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (WmsPickModeEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
