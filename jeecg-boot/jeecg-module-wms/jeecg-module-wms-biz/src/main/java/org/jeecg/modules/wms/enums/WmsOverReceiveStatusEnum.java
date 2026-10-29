package org.jeecg.modules.wms.enums;

/**
 * 超收放行状态（决策 D4）。与字典 wms_over_receive_status 一一对应。
 *
 * 两种仓型共用这一个字段：
 * - INDUSTRIAL 工业仓：0 → 1 待主管确认 → 2 已放行 / 3 已驳回（未放行一律硬拦截）；
 * - NORMAL 普通仓：0 → 1 待审批 → 2 已通过 / 3 已驳回（未通过时单据挂 90 异常）。
 */
public enum WmsOverReceiveStatusEnum {

    /** 无需放行：本次收货没有超收 */
    NONE(0, "无需放行"),

    /** 待确认 / 待审批：超收已登记，等待主管处理 */
    PENDING(1, "待主管确认"),

    /** 已放行 / 已通过：允许按超收数量入库 */
    PASSED(2, "已放行"),

    /** 已驳回：超收部分不得入库，只能拒收或退货 */
    REJECTED(3, "已驳回");

    private final int code;
    private final String desc;

    WmsOverReceiveStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 超收是否已放行。读取时把 null 当作 0（无需放行），
     * 与表默认值保持一致。
     */
    public static boolean isPassed(Integer status) {
        return status != null && status == PASSED.code;
    }
}
