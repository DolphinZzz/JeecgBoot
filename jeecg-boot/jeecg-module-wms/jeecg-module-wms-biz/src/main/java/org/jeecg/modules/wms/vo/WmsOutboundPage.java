package org.jeecg.modules.wms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jeecg.common.aspect.annotation.Dict;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 出库单分页列表返回视图
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "出库单分页列表视图")
public class WmsOutboundPage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @Schema(description = "主键ID")
    private Long id;

    /** 出库单号 */
    @Schema(description = "出库单号")
    private String outboundCode;

    /** 出库类型(1销售出库 2MES工单领料 3采购退货 4调拨出库) */
    @Dict(dicCode = "wms_outbound_type")
    @Schema(description = "出库类型")
    private Integer outboundType;

    /** 拣货模式(1摘果法 2波次播种法) */
    @Dict(dicCode = "wms_pick_mode")
    @Schema(description = "拣货模式")
    private Integer pickMode;

    /** 波次ID */
    @Schema(description = "波次ID")
    private Long waveId;

    /** 波次号 */
    @Schema(description = "波次号")
    private String waveCode;

    /** 上游ERP单号 */
    @Schema(description = "上游ERP单号")
    private String erpOrderNo;

    /** MES生产工单号 */
    @Schema(description = "MES生产工单号")
    private String mesWorkOrderNo;

    /** 客户ID */
    @Dict(dictTable = "wms_customer", dicText = "customer_name", dicCode = "id")
    @Schema(description = "客户ID")
    private Long customerId;

    /** 客户名称 */
    @Schema(description = "客户名称")
    private String customerName;

    /** 仓库ID */
    @Dict(dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Schema(description = "仓库ID")
    private Integer warehouseId;

    /** 仓库名称 */
    @Schema(description = "仓库名称")
    private String warehouseName;

    /** 仓库类型 */
    @Schema(description = "仓库类型")
    private String warehouseType;

    /** 货主ID */
    @Dict(dictTable = "wms_consignor", dicText = "consignor_name", dicCode = "id")
    @Schema(description = "货主ID")
    private Long consignorId;

    /** 货主名称 */
    @Schema(description = "货主名称")
    private String consignorName;

    /** 出库状态 */
    @Dict(dicCode = "wms_outbound_status")
    @Schema(description = "出库状态")
    private Integer status;

    /** 计划出库总件数 */
    @Schema(description = "计划出库总件数")
    private Integer totalCount;

    /** 总重量 */
    @Schema(description = "总重量")
    private Integer totalWeight;

    /** 总体积 */
    @Schema(description = "总体积")
    private Integer totalVolume;

    /** 订单金额 */
    @Schema(description = "订单金额")
    private Integer price;

    /** 物流公司ID */
    @Dict(dictTable = "wms_logistics_company", dicText = "company_name", dicCode = "id")
    @Schema(description = "物流公司ID")
    private Long logisticsCompanyId;

    /** 物流公司名称 */
    @Schema(description = "物流公司名称")
    private String logisticsCompanyName;

    /** 物流单号 */
    @Schema(description = "物流单号")
    private String logisticsNumber;

    /** 物流运费(元) */
    @Schema(description = "物流运费(元)")
    private Double freightFee;

    /** 发运出库时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发运出库时间")
    private Date shipTime;

    /** 发货确认人账号 */
    @Schema(description = "发货确认人账号")
    private String shipBy;

    /** 明细行数 */
    @Schema(description = "明细行数")
    private Integer detailRows;

    /** 已分配总件数 */
    @Schema(description = "已分配总件数")
    private Integer allocatedTotal;

    /** 实拣总件数 */
    @Schema(description = "实拣总件数")
    private Integer pickedTotal;

    /** 复核总件数 */
    @Schema(description = "复核总件数")
    private Integer reviewTotal;

    /** 创建时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    /** 创建人 */
    @Schema(description = "创建人")
    private String createBy;
}
