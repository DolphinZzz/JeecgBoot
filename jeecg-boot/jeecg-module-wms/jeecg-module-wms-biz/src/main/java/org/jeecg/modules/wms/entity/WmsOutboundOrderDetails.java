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
 * @Description: 出库单明细
 *
 * target_warehouse_* 历史列名源自调拨场景，实际语义为分配的仓库/库区/库位
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@TableName("wms_outbound_order_details")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "出库单明细")
public class WmsOutboundOrderDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /** 出库单ID */
    @Schema(description = "出库单ID")
    private Long outboundOrderId;

    /** 规格ID */
    @Excel(name = "商品SKU", width = 15, dictTable = "wms_item_sku", dicText = "sku_name", dicCode = "id")
    @Dict(dictTable = "wms_item_sku", dicText = "sku_name", dicCode = "id")
    @Schema(description = "规格ID")
    private Long skuId;

    /** 货主ID */
    @Schema(description = "货主ID")
    private Long consignorId;

    /** 分配批次号 */
    @Excel(name = "批次号", width = 16)
    @Schema(description = "分配批次号")
    private String batchNumber;

    /** 有效期至 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "有效期至")
    private Date expireDate;

    /** 计划出库数量 */
    @Excel(name = "计划出库数量", width = 12)
    @Schema(description = "计划出库数量")
    private Integer outCount;

    /** 已分配数量 */
    @Excel(name = "已分配数量", width = 12)
    @Schema(description = "已分配数量")
    private Integer allocatedCount;

    /** 实拣数量 */
    @Excel(name = "实拣数量", width = 12)
    @Schema(description = "实拣数量")
    private Integer pickedCount;

    /** 复核数量 */
    @Excel(name = "复核数量", width = 12)
    @Schema(description = "复核数量")
    private Integer reviewCount;

    /** 复核状态(0未复核 1通过 2不通过) */
    @Excel(name = "复核状态", width = 12, dicCode = "wms_review_status")
    @Dict(dicCode = "wms_review_status")
    @Schema(description = "复核状态(0未复核 1通过 2不通过)")
    private Integer reviewStatus;

    /** 缺货处理(1剔除缺货行 2整单挂起) */
    @Dict(dicCode = "wms_shortage_handle")
    @Schema(description = "缺货处理(1剔除缺货行 2整单挂起)")
    private Integer shortageHandle;

    /** 分配目标仓库ID */
    @Schema(description = "分配目标仓库ID")
    private Integer targetWarehouseId;

    /** 分配目标库区ID */
    @Schema(description = "分配目标库区ID")
    private Integer targetWarehouseAreaId;

    /** 分配目标库位ID */
    @Schema(description = "分配目标库位ID")
    private Integer targetWarehouseAreaLocaId;

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
    private Integer isDel;
}
