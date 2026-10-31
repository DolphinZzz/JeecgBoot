package org.jeecg.modules.wms.enums;

/**
 * 出库单状态。与字典 wms_outbound_status 的 item_value 一一对应（方案 3.3.2 状态机）。
 *
 * 正向流转：
 * 10 待分配 → 20 已分配/已锁库 → 40 拣货中 → 50 拣货完成待复核 → 60 复核通过待打包 → 70 已打包待发货 → 80 已发货 → 90 已签收
 * 分支与异常：
 * 分配缺货 → 30 待补货
 * 拣货/SN/复核异常 → 92 异常挂起
 * 取消订单 → 91 已取消
 */
public enum WmsOutboundStatusEnum {

    /** 待分配：单据已生成，未做库存预检 */
    PENDING_ALLOCATE(10, "待分配"),

    /** 已分配/已锁库：批次库位已分配，库存进入 locked_count */
    ALLOCATED(20, "已分配/已锁库"),

    /** 待补货：库存不足，整单或部分行挂起 */
    WAIT_REPLENISH(30, "待补货"),

    /** 拣货中：波次或摘果任务执行中 */
    PICKING(40, "拣货中"),

    /** 拣货完成待复核：实拣完成 */
    WAIT_REVIEW(50, "拣货完成待复核"),

    /** 复核通过待打包：复核确认通过 */
    REVIEWED(60, "复核通过待打包"),

    /** 已打包待发货：打包称重完成，待出库发运 */
    PACKED(70, "已打包待发货"),

    /** 已发货：库存已扣减，已回传上游 */
    SHIPPED(80, "已发货"),

    /** 已签收：物流签收回传 */
    SIGNED(90, "已签收"),

    /** 已取消：未发货取消，库存已释放 */
    CANCELLED(91, "已取消"),

    /** 异常挂起：缺货、SN不符、称重超差等 */
    EXCEPTION_SUSPEND(92, "异常挂起");

    private final int code;
    private final String desc;

    WmsOutboundStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static WmsOutboundStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (WmsOutboundStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }

    /** 是否终态（已发货/已签收/已取消） */
    public boolean isFinal() {
        return this == SHIPPED || this == SIGNED || this == CANCELLED;
    }
}
