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
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: 出库单称重记录表
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@TableName("wms_outbound_order_weights")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "出库单称重记录表")
public class WmsOutboundOrderWeights implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /** 称重单号 */
    @Schema(description = "称重单号")
    private String weightsCode;

    /** 出库单ID */
    @Schema(description = "出库单ID")
    private Long outboundOrderId;

    /** 出库单明细ID */
    @Schema(description = "出库单明细ID")
    private Long outboundOrderDetailId;

    /** 称重员工ID */
    @Schema(description = "称重员工ID")
    private Integer weightsEmpId;

    /** 称重时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "称重时间")
    private Date weightsTime;

    /** 称重质量(克) */
    @Schema(description = "称重质量(克)")
    private Integer weightVal;

    /** 称重单位 */
    @Schema(description = "称重单位")
    private Integer weightsUnit;

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
