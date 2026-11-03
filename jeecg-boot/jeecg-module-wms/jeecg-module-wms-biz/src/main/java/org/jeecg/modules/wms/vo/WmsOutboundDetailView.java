package org.jeecg.modules.wms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jeecg.common.aspect.annotation.Dict;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 出库单明细视图
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "出库单明细视图")
public class WmsOutboundDetailView implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 明细ID */
    @Schema(description = "明细ID")
    private Long id;

    /** 出库单ID */
    @Schema(description = "出库单ID")
    private Long outboundOrderId;

    /** 规格ID */
    @Schema(description = "规格ID")
    private Long skuId;

    /** SKU编码 */
    @Schema(description = "SKU编码")
    private String skuCode;

    /** SKU名称 */
    @Schema(description = "SKU名称")
    private String skuName;

    /** 商品条码 */
    @Schema(description = "商品条码")
    private String barCode;

    /** SPU名称 */
    @Schema(description = "SPU名称")
    private String spuName;

    /** 规格 */
    @Schema(description = "规格")
    private String spec;

    /** 存储条件 */
    @Dict(dicCode = "wms_storage_condition")
    @Schema(description = "存储条件")
    private String storageCondition;

    /** 是否SN管理(1是 0否) */
    @Schema(description = "是否SN管理(1是 0否)")
    private Integer isSnManaged;

    /** 商品单重 */
    @Schema(description = "商品单重")
    private Integer productWeight;

    /** 重量单位 */
    @Schema(description = "重量单位")
    private String weightUnit;

    /** 单件基准重量(克) */
    @Schema(description = "单件基准重量(克)")
    private Integer unitWeightG;

    /** 单件重量(千克) */
    @Schema(description = "单件重量(千克)")
    private Double unitWeightKg;

    /** 临期禁出阈值天数 */
    @Schema(description = "临期禁出阈值天数")
    private Integer nearExpireDays;

    /** 货主ID */
    @Schema(description = "货主ID")
    private Long consignorId;

    /** 货主名称 */
    @Schema(description = "货主名称")
    private String consignorName;

    /** 分配批次号 */
    @Schema(description = "分配批次号")
    private String batchNumber;

    /** 有效期至 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "有效期至")
    private Date expireDate;

    /** 计划出库数量 */
    @Schema(description = "计划出库数量")
    private Integer outCount;

    /** 已分配数量 */
    @Schema(description = "已分配数量")
    private Integer allocatedCount;

    /** 实拣数量 */
    @Schema(description = "实拣数量")
    private Integer pickedCount;

    /** 复核数量 */
    @Schema(description = "复核数量")
    private Integer reviewCount;

    /** 复核状态(0未复核 1通过 2不通过) */
    @Dict(dicCode = "wms_review_status")
    @Schema(description = "复核状态")
    private Integer reviewStatus;

    /** 缺货处理(1剔除缺货行 2整单挂起) */
    @Dict(dicCode = "wms_shortage_handle")
    @Schema(description = "缺货处理")
    private Integer shortageHandle;

    /** 分配目标仓库ID */
    @Schema(description = "分配目标仓库ID")
    private Integer targetWarehouseId;

    /** 分配目标库区ID */
    @Schema(description = "分配目标库区ID")
    private Integer targetWarehouseAreaId;

    /** 库区名称 */
    @Schema(description = "库区名称")
    private String areaName;

    /** 分配目标库位ID */
    @Schema(description = "分配目标库位ID")
    private Integer targetWarehouseAreaLocaId;

    /** 库位编码 */
    @Schema(description = "库位编码")
    private String locaCode;

    /** 拣货状态(1未拣货 2已拣货) */
    @Schema(description = "拣货状态(1未拣货 2已拣货)")
    private Integer pickStatus;

    /** 拣货人ID */
    @Schema(description = "拣货人ID")
    private Integer pickEmpId;

    /** 拣货完成时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "拣货完成时间")
    private Date pickTime;
}
