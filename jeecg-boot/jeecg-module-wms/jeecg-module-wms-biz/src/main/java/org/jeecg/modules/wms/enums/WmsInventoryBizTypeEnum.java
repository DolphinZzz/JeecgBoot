package org.jeecg.modules.wms.enums;

/**
 * 库存变动业务类型。与字典 wms_inventory_biz_type 的 item_value 一一对应，
 * 库存流水 wms_inventory_log.biz_type 只允许取这里的值，禁止散落魔法数字。
 */
public enum WmsInventoryBizTypeEnum {

    /** 入库上架：收货质检后的落库 */
    INBOUND_PUTAWAY(1, "入库上架"),

    /** 出库扣减：拣货出库 */
    OUTBOUND_DEDUCT(2, "出库扣减"),

    /** 移库移出：源库位扣减 */
    MOVE_OUT(3, "移库移出"),

    /** 移库移入：目标库位增加 */
    MOVE_IN(4, "移库移入"),

    /** 补货：存储区移到拣货区 */
    REPLENISH(5, "补货"),

    /** 盘点调账：按实盘数调整 */
    STOCKTAKING_ADJUST(6, "盘点调账"),

    /** 冻结 */
    FREEZE(7, "冻结"),

    /** 解冻 */
    UNFREEZE(8, "解冻"),

    /** 锁定（出库/移库预占） */
    LOCK(9, "锁定"),

    /** 解锁 */
    UNLOCK(10, "解锁");

    private final int code;
    private final String desc;

    WmsInventoryBizTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static WmsInventoryBizTypeEnum of(Integer code) {
        if (code != null) {
            for (WmsInventoryBizTypeEnum value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
        }
        throw new IllegalArgumentException("未知的库存变动业务类型: " + code);
    }
}
