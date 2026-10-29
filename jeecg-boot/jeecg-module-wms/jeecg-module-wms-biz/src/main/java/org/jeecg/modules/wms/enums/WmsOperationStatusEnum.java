package org.jeecg.modules.wms.enums;

/**
 * 库内作业单状态。与字典 wms_operation_status 的 item_value 一一对应。
 */
public enum WmsOperationStatusEnum {

    /** 未确认：作业单已生成，等待人工执行 */
    PENDING(1, "未确认"),

    /** 已执行：库存已完成扣增 */
    EXECUTED(2, "已执行"),

    /** 已取消 */
    CANCELLED(3, "已取消");

    private final int code;
    private final String desc;

    WmsOperationStatusEnum(int code, String desc) {
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
