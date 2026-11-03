package org.jeecg.modules.wms.vo;

import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description: 入库单查询条件
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
public class WmsEntryQuery {

    /**入库单号（模糊）*/
    private String entryCode;

    /**入库类型(1采购入库 2MES生产入库 3销退入库 4调拨入库)*/
    private Integer entryType;

    /**仓库ID*/
    private Integer warehouseId;

    /**货主ID*/
    private Long consignorId;

    /**批次号（模糊）*/
    private String batchNumber;

    /**MES生产工单号（模糊）*/
    private String mesWorkOrderNo;

    /**上游ERP采购单号（模糊）*/
    private String erpPoNo;

    /**入库单状态(支持单状态如 10，或逗号分隔的多状态如 40,50)*/
    private String status;

    /**是否只看未完成的单据（status 非 60/99），默认 false 看全部*/
    private Boolean onlyOpen;

    /**
     * 解析 status 为整数列表，兼容单状态与逗号分隔的多状态
     */
    public List<Integer> getStatusList() {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        try {
            return Arrays.stream(status.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
