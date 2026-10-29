package org.jeecg.modules.wms.enums;

/**
 * 入库单状态。与字典 wms_entry_status 的 item_value 一一对应（方案 3.2.2 状态机）。
 *
 * 流转：10待到货 → 20收货清点中 → 30待质检 → 40质检完成待上架 → 50上架中 → 60已完成。
 * 异常分支：超收未放行 → 90异常挂起，主管放行后回到 30。
 * 终态：60 已完成、99 已取消。
 *
 * 注意：二期之前的历史数据用的是 1~5，迁移脚本已按 1→10 2→20 3→30 4→40 5→60 换算过。
 */
public enum WmsEntryStatusEnum {

    /** 待到货（ASN）：入库单已生成，货未到 */
    ARRIVING(10, "待到货"),

    /** 收货清点中：卸货、清点、分拣 */
    RECEIVING(20, "收货清点中"),

    /** 待质检：收货校验完成，等待 IQC */
    PENDING_QC(30, "待质检"),

    /** 质检完成待上架：合格品入待上架池，不合格品已判定 */
    PENDING_PUTAWAY(40, "质检完成待上架"),

    /** 上架中：已生成上架任务，等待确认落库 */
    PUTAWAY(50, "上架中"),

    /** 已完成入库：库存已落库 */
    COMPLETED(60, "已完成入库"),

    /** 异常挂起：超收待放行、SN 不符等 */
    ABNORMAL(90, "异常挂起"),

    /** 已取消：上游撤单 */
    CANCELLED(99, "已取消");

    private final int code;
    private final String desc;

    WmsEntryStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 是否终态（已完成 / 已取消），终态单据不允许再推进 */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
