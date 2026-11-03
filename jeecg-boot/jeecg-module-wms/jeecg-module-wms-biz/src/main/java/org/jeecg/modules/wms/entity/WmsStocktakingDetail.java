package org.jeecg.modules.wms.entity;

import java.io.Serializable;
import java.math.BigDecimal;
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
 * @Description: 盘库单明细（子表）
 * @Author: jeecg-boot
 * @Date: 2026-11-03
 * @Version: V1.0
 */
@Data
@TableName("wms_stocktaking_detail")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "盘库单明细")
public class WmsStocktakingDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /** 盘库单ID */
    @Schema(description = "盘库单ID")
    private Long orderId;

    /** 盘库单号 */
    @Excel(name = "盘库单号", width = 20)
    @Schema(description = "盘库单号")
    private String orderNo;

    /** SKU规格ID */
    @Excel(name = "商品SKU", width = 15, dictTable = "wms_item_sku_v", dicText = "text", dicCode = "id")
    @Dict(dictTable = "wms_item_sku_v", dicText = "text", dicCode = "id")
    @Schema(description = "SKU规格ID")
    private Long skuId;

    /** 规格编号 */
    @Excel(name = "规格编号", width = 15)
    @Schema(description = "规格编号")
    private String skuCode;

    /** 规格名 */
    @Excel(name = "规格名", width = 18)
    @Schema(description = "规格名")
    private String skuName;

    /** 商品名 */
    @Excel(name = "商品名", width = 20)
    @Schema(description = "商品名")
    private String spuName;

    /** 单位 */
    @Excel(name = "单位", width = 10)
    @Schema(description = "单位")
    private String unit;

    /** 账面库存 */
    @Excel(name = "账面库存", width = 12)
    @Schema(description = "账面库存")
    private Integer bookCount;

    /** 单价(元) */
    @Excel(name = "单价", width = 12)
    @Schema(description = "单价(元)")
    private BigDecimal unitPrice;

    /** 实际库存(件/个，未录入时可为空) */
    @Excel(name = "实际库存", width = 12)
    @Schema(description = "实际库存(件/个)")
    private Integer actualCount;

    /** 实际金额(元) */
    @Excel(name = "实际金额", width = 15)
    @Schema(description = "实际金额(元)")
    private BigDecimal actualAmount;

    /** 盈亏数 = 实际库存 - 账面库存 */
    @Excel(name = "盈亏数", width = 12)
    @Schema(description = "盈亏数")
    private Integer discrepancyCount;

    /** 实际盈亏金额 = 盈亏数 * 单价 */
    @Excel(name = "实际盈亏金额", width = 15)
    @Schema(description = "实际盈亏金额(元)")
    private BigDecimal discrepancyAmount;

    /** 创建人 */
    @Schema(description = "创建人")
    private String createBy;

    /** 创建时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    /** 修改人 */
    @Schema(description = "修改人")
    private String updateBy;

    /** 修改时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private Date updateTime;

    /** 是否删除(1未删除 2已删除) */
    @TableLogic(value = "1", delval = "2")
    @Schema(description = "是否删除(1未删除 2已删除)")
    private Integer isDel;
}
