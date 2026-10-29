package org.jeecg.modules.wms.enums;

/**
 * 质检状态。入库明细 wms_warehousing_entry_details.qc_status 与质检单
 * wms_quality_check.status 共用这套编码（字典 wms_qc_status）。
 */
public enum WmsQcStatusEnum {

    /** 待质检：已收货，尚未出质检结论 */
    PENDING(1, "待质检"),

    /** 质检中：已开始录入，未出结论 */
    CHECKING(2, "质检中"),

    /** 合格：全部合格 */
    PASS(3, "合格"),

    /** 不合格：全部不合格，只能进退货区 */
    FAIL(4, "不合格"),

    /** 部分合格：合格品可上架，不合格品分流退货区 */
    PARTIAL(5, "部分合格");

    private final int code;
    private final String desc;

    WmsQcStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 是否已出结论（合格 / 不合格 / 部分合格），出了结论才能进上架环节 */
    public boolean isDecided() {
        return this == PASS || this == FAIL || this == PARTIAL;
    }

    /** 按送检数 / 合格数推导状态：免检或全部合格 = 合格，全部不合格 = 不合格，其余 = 部分合格 */
    public static WmsQcStatusEnum ofCounts(int checkCount, int passCount) {
        if (passCount <= 0) {
            return FAIL;
        }
        if (passCount >= checkCount) {
            return PASS;
        }
        return PARTIAL;
    }
}
