package org.jeecg.modules.wms.enums;

/**
 * 出库类型。与字典 wms_outbound_type 的 item_value 一一对应。
 */
public enum WmsOutboundTypeEnum {

    /** 销售出库 */
    SALE(1, "销售出库"),

    /** MES工单领料 */
    MES_WORK_ORDER(2, "MES工单领料"),

    /** 采购退货 */
    PURCHASE_RETURN(3, "采购退货"),

    /** 调拨出库 */
    TRANSFER(4, "调拨出库");

    private final int code;
    private final String desc;

    WmsOutboundTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static WmsOutboundTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (WmsOutboundTypeEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
