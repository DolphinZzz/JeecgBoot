package org.jeecg.modules.wms.entity;

import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: 盘点损溢记录（盘亏 / 盘盈）
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
@TableName("wms_stocktaking_loss")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "盘点损溢记录")
public class WmsStocktakingLoss implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**损失数量（正数，方向由 lossType 决定）*/
    @Schema(description = "损失数量")
    private Integer lossCount;

    /**损失类型(1破损 2丢失)*/
    @Dict(dicCode = "wms_loss_category")
    @Schema(description = "损失类型(1破损 2丢失)")
    private Integer lossCategory;

    /**损溢类型(1盘亏 2盘盈)*/
    @Dict(dicCode = "wms_loss_type")
    @Schema(description = "损溢类型(1盘亏 2盘盈)")
    private Integer lossType;

    /**盘点记录ID（wms_stocktaking_info.id）*/
    @Schema(description = "盘点记录ID")
    private Long stocktakingInfo;

    /**批次号*/
    @Schema(description = "批次号")
    private String batchNumber;

    /**操作员工ID*/
    @Schema(description = "操作员工ID")
    private Integer empId;

    /**SaaS标识*/
    @Schema(description = "SaaS标识", hidden = true)
    private String saasId;

    /**创建人*/
    @Schema(description = "创建人")
    private String createBy;

    /**创建时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    /**修改人*/
    @Schema(description = "修改人")
    private String updateBy;

    /**修改时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private Date updateTime;

    /**是否删除 1未删除 2已删除*/
    @TableLogic(value = "1", delval = "2")
    @Schema(description = "是否删除 1未删除 2已删除")
    private Integer isDel;
}
