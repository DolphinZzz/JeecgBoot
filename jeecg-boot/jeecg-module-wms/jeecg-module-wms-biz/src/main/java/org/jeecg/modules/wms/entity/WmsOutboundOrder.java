package org.jeecg.modules.wms.entity;

import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: 出库单（单头）
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@TableName("wms_outbound_order")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "出库单")
public class WmsOutboundOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /** 出库单号 */
    @Excel(name = "出库单号", width = 22)
    @Schema(description = "出库单号")
    private String outboundCode;

    /** 出库类型(1销售出库 2MES工单领料 3采购退货 4调拨出库) */
    @Excel(name = "出库类型", width = 15, dicCode = "wms_outbound_type")
    @Dict(dicCode = "wms_outbound_type")
    @Schema(description = "出库类型(1销售出库 2MES工单领料 3采购退货 4调拨出库)")
    private Integer outboundType;

    /** 拣货模式(1摘果法 2波次播种法) */
    @Excel(name = "拣货模式", width = 15, dicCode = "wms_pick_mode")
    @Dict(dicCode = "wms_pick_mode")
    @Schema(description = "拣货模式(1摘果法 2波次播种法)")
    private Integer pickMode;

    /** 波次ID */
    @Schema(description = "波次ID")
    private Long waveId;

    /** 上游ERP单号 */
    @Schema(description = "上游ERP单号")
    private String erpOrderNo;

    /** MES生产工单号 */
    @Schema(description = "MES生产工单号")
    private String mesWorkOrderNo;

    /** 客户ID */
    @Excel(name = "客户", width = 18, dictTable = "wms_customer", dicText = "customer_name", dicCode = "id")
    @Dict(dictTable = "wms_customer", dicText = "customer_name", dicCode = "id")
    @Schema(description = "客户ID")
    private Long customerId;

    /** 仓库ID */
    @Excel(name = "仓库", width = 15, dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Dict(dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Schema(description = "仓库ID")
    private Integer warehouseId;

    /** 货主ID */
    @Excel(name = "货主", width = 18, dictTable = "wms_consignor", dicText = "consignor_name", dicCode = "id")
    @Dict(dictTable = "wms_consignor", dicText = "consignor_name", dicCode = "id")
    @Schema(description = "货主ID")
    private Long consignorId;

    /** 发票代码 */
    @Schema(description = "发票代码")
    private String invoiceCode;

    /** 出库状态(10待分配 20已分配 30待补货 40拣货中 50待复核 60待打包 70待发货 80已发货 90已签收 91已取消 92异常挂起) */
    @Excel(name = "状态", width = 15, dicCode = "wms_outbound_status")
    @Dict(dicCode = "wms_outbound_status")
    @Schema(description = "出库状态")
    private Integer status;

    /** 总件数 */
    @Excel(name = "总件数", width = 12)
    @Schema(description = "总件数")
    private Integer totalCount;

    /** 总重量 */
    @Schema(description = "总重量")
    private Integer totalWeight;

    /** 总体积 */
    @Schema(description = "总体积")
    private Integer totalVolume;

    /** 经办员工ID */
    @Schema(description = "经办员工ID")
    private Integer empId;

    /** 订单金额 */
    @Schema(description = "订单金额")
    private Integer price;

    /** 物流公司ID */
    @Excel(name = "物流公司", width = 18, dictTable = "wms_logistics_company", dicText = "company_name", dicCode = "id")
    @Dict(dictTable = "wms_logistics_company", dicText = "company_name", dicCode = "id")
    @Schema(description = "物流公司ID")
    private Long logisticsCompanyId;

    /** 物流单号 */
    @Excel(name = "物流单号", width = 20)
    @Schema(description = "物流单号")
    private String logisticsNumber;

    /** 物流运费(元) */
    @Excel(name = "物流运费", width = 12)
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

    /** SaaS标识 */
    @Schema(description = "SaaS标识", hidden = true)
    private String saasId;

    /** 创建时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    /** 创建人账号 */
    @Schema(description = "创建人账号")
    private String createBy;

    /** 修改时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private Date updateTime;

    /** 修改人账号 */
    @Schema(description = "修改人账号")
    private String updateBy;

    /** 是否删除 1未删除 2已删除 */
    @TableLogic(value = "1", delval = "2")
    @Schema(description = "是否删除 1未删除 2已删除")
    private Integer isDel = 1;
}
