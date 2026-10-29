package org.jeecg.modules.wms.enums;

/**
 * 仓库类型。与字典 wms_warehouse_type 的 item_value 一一对应。
 */
public enum WmsWarehouseTypeEnum {

    /** 普通仓：允许超收审批、不强制绑定 MES 工单 */
    NORMAL("NORMAL", "普通仓"),

    /** 工业制造仓：超收默认拦截、质检必须绑定 MES 工单、按工单摘果出库 */
    INDUSTRIAL("INDUSTRIAL", "工业制造仓");

    private final String code;
    private final String desc;

    WmsWarehouseTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按仓型编码取枚举。null / 空串按 NORMAL 兜底（兼容升级期间的历史数据），
     * 未知编码直接抛错，避免脏数据被静默当成普通仓。
     */
    public static WmsWarehouseTypeEnum of(String code) {
        if (code == null || code.trim().isEmpty()) {
            return NORMAL;
        }
        String target = code.trim();
        for (WmsWarehouseTypeEnum e : values()) {
            if (e.code.equalsIgnoreCase(target)) {
                return e;
            }
        }
        throw new IllegalArgumentException("未知仓库类型: " + code);
    }

    /** 判断编码是否是合法仓型（null / 空串视为合法，走 NORMAL 兜底） */
    public static boolean isValid(String code) {
        if (code == null || code.trim().isEmpty()) {
            return true;
        }
        for (WmsWarehouseTypeEnum e : values()) {
            if (e.code.equalsIgnoreCase(code.trim())) {
                return true;
            }
        }
        return false;
    }

    /** 是否强校验 MES 生产工单 */
    public boolean requireMesWorkOrder() {
        return this == INDUSTRIAL;
    }

    /** 超收默认是否硬拦截（普通仓走审批单，不硬拦截） */
    public boolean hardBlockOverReceive() {
        return this == INDUSTRIAL;
    }
}
