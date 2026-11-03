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
 * @Description: 盘库单（主表）
 * @Author: jeecg-boot
 * @Date: 2026-11-03
 * @Version: V1.0
 */
@Data
@TableName("wms_stocktaking_order")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "盘库单")
public class WmsStocktakingOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /** 盘库单号 */
    @Excel(name = "盘库单号", width = 22)
    @Schema(description = "盘库单号")
    private String orderNo;

    /** 盘库仓库ID */
    @Excel(name = "盘库仓库", width = 15, dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Dict(dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Schema(description = "盘库仓库ID")
    private Integer warehouseId;

    /** 单据日期 */
    @Excel(name = "单据日期", width = 15, format = "yyyy-MM-dd")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "单据日期")
    private Date orderDate;

    /** 实际总金额 */
    @Excel(name = "实际总金额", width = 15)
    @Schema(description = "实际总金额")
    private BigDecimal totalActualAmount;

    /** 账面总金额 */
    @Excel(name = "账面总金额", width = 15)
    @Schema(description = "账面总金额")
    private BigDecimal totalBookAmount;

    /** 账面总数量 */
    @Excel(name = "账面总数量", width = 12)
    @Schema(description = "账面总数量")
    private Integer totalBookCount;

    /** 实际总数量 */
    @Excel(name = "实际总数量", width = 12)
    @Schema(description = "实际总数量")
    private Integer totalActualCount;

    /** 盈亏总数量 */
    @Excel(name = "盈亏总数量", width = 12)
    @Schema(description = "盈亏总数量")
    private Integer totalDiscrepancyCount;

    /** 盘盈总数量 */
    @Excel(name = "盘盈总数量", width = 12)
    @Schema(description = "盘盈总数量")
    private Integer profitCount;

    /** 盘盈总金额 */
    @Excel(name = "盘盈总金额", width = 15)
    @Schema(description = "盘盈总金额")
    private BigDecimal profitAmount;

    /** 盘亏总数量 */
    @Excel(name = "盘亏总数量", width = 12)
    @Schema(description = "盘亏总数量")
    private Integer lossCount;

    /** 盘亏总金额 */
    @Excel(name = "盘亏总金额", width = 15)
    @Schema(description = "盘亏总金额")
    private BigDecimal lossAmount;

    /** 实际盈亏总金额 */
    @Excel(name = "实际盈亏总金额", width = 15)
    @Schema(description = "实际盈亏总金额")
    private BigDecimal totalDiscrepancyAmount;

    /** 备注 */
    @Excel(name = "备注", width = 25)
    @Schema(description = "备注")
    private String remark;

    /** 状态(0草稿/暂存 1已盘点 2已调账) */
    @Excel(name = "状态", width = 12, dicCode = "wms_stocktaking_order_status")
    @Dict(dicCode = "wms_stocktaking_order_status")
    @Schema(description = "状态(0草稿/暂存 1已盘点 2已调账)")
    private Integer status;

    /** 盘盈入库状态(0未生成 1已生成 2无需生成) */
    @Schema(description = "盘盈入库状态(0未生成 1已生成 2无需生成)")
    private Integer adjustInStatus;

    /** 生成的盘盈入库单号 */
    @Schema(description = "生成的盘盈入库单号")
    private String adjustInOrderNo;

    /** 盘亏出库状态(0未生成 1已生成 2无需生成) */
    @Schema(description = "盘亏出库状态(0未生成 1已生成 2无需生成)")
    private Integer adjustOutStatus;

    /** 生成的盘亏出库单号 */
    @Schema(description = "生成的盘亏出库单号")
    private String adjustOutOrderNo;

    /** 审批开关(0关闭 1开启) */
    @Schema(description = "审批开关(0关闭 1开启)")
    private Integer approvalSwitch;

    /** 所属部门 */
    @Schema(description = "所属部门")
    private String sysOrgCode;

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
