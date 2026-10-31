package org.jeecg.modules.wms.enums;

/**
 * 缺货处理方式。与字典 wms_shortage_handle 的 item_value 一一对应。
 */
public enum WmsShortageHandleEnum {

    /** 剔除缺货行：仅发有货部分，缺货行标记 */
    REMOVE_SHORTAGE(1, "剔除缺货行"),

    /** 整单挂起：整单挂起待补货 */
    SUSPEND_ORDER(2, "整单挂起待补货");

    private final int code;
    private final String desc;

    WmsShortageHandleEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static WmsShortageHandleEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (WmsShortageHandleEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
