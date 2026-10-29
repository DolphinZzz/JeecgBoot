package org.jeecg.modules.wms.enums;

/**
 * 盘点记录状态。与字典 wms_stocktaking_status 的 item_value 一一对应。
 */
public enum WmsStocktakingStatusEnum {

    /** 待盘点：已生成盘点记录，尚未录入实盘数 */
    PENDING(1, "待盘点"),

    /** 已盘待审核：实盘数已录入，差异等待主管审批（无差异也会停在 2，由「结案」统一确认） */
    COUNTED(2, "已盘待审核"),

    /** 已确认：差异已审批并完成调账 */
    CONFIRMED(3, "已确认");

    private final int code;
    private final String desc;

    WmsStocktakingStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
