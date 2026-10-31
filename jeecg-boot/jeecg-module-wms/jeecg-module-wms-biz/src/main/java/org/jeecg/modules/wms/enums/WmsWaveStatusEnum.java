package org.jeecg.modules.wms.enums;

/**
 * 波次状态。与字典 wms_wave_status 的 item_value 一一对应。
 *
 * 流转：1 待拣货 → 2 拣货中 → 3 已拣货 → 4 分货中 → 5 已完成。9 已取消。
 */
public enum WmsWaveStatusEnum {

    /** 待拣货 */
    PENDING_PICK(1, "待拣货"),

    /** 拣货中 */
    PICKING(2, "拣货中"),

    /** 已拣货 */
    PICKED(3, "已拣货"),

    /** 分货中 */
    DISTRIBUTING(4, "分货中"),

    /** 已完成 */
    COMPLETED(5, "已完成"),

    /** 已取消 */
    CANCELLED(9, "已取消");

    private final int code;
    private final String desc;

    WmsWaveStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static WmsWaveStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (WmsWaveStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }

    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
