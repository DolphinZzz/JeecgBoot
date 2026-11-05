package org.jeecg.modules.mes.entity;

import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: MES-工艺工序组成表
 * @Author: jeecg-boot
 * @Date: 2026-11-05
 * @Version: V1.0
 */
@Data
@TableName("mes_process_route_composition")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "MES-工艺工序组成表")
public class MesProcessRouteComposition implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**所属工艺路线ID（外键，关联 mes_process_route.id）*/
    @Schema(description = "所属工艺路线ID")
    private Long routeId;

    /**工序ID（外键，关联 mes_process_step.id）*/
    @Schema(description = "工序ID")
    private Long processStepId;

    /**在工艺路线中的序号（1,2,3...）*/
    @Excel(name = "序号", width = 10)
    @Schema(description = "在工艺路线中的序号（1,2,3...）")
    private Integer sequenceNo;

    /**与下一道工序的关系：S-to-S / F-to-F / S-to-F / F-to-S*/
    @Excel(name = "与下一道工序关系", width = 18)
    @Schema(description = "与下一道工序关系")
    private String relationWithNext;

    /**甘特图显示颜色，如 #00AEF3*/
    @Excel(name = "甘特图显示颜色", width = 15)
    @Schema(description = "甘特图显示颜色")
    private String ganttColor;

    /**是否为关键工序：1=是，0=否*/
    @Excel(name = "是否关键工序", width = 15, replace = {"是_1", "否_0"})
    @Schema(description = "是否为关键工序：1=是，0=否")
    private Integer isCriticalProcess;

    /**是否需要质检确认：1=是，0=否*/
    @Excel(name = "是否需要质检确认", width = 15, replace = {"是_1", "否_0"})
    @Schema(description = "是否需要质检确认：1=是，0=否")
    private Integer needQcConfirmation;

    /**准备时间（小时）*/
    @Excel(name = "准备时间", width = 12)
    @Schema(description = "准备时间")
    private Integer setupTime;

    /**等待时间（小时）*/
    @Excel(name = "等待时间", width = 12)
    @Schema(description = "等待时间")
    private Integer waitTime;

    /**备注*/
    @Excel(name = "备注", width = 25)
    @Schema(description = "备注")
    private String remark;

    /**创建时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    /**创建人账号*/
    @Schema(description = "创建人账号")
    private String createBy;

    /**修改时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private Date updateTime;

    /**修改人账号*/
    @Schema(description = "修改人账号")
    private String updateBy;

    /**是否删除：1=未删除，2=已删除*/
    @TableLogic(value = "1", delval = "2")
    @Schema(description = "是否删除：1=未删除，2=已删除")
    private Integer isDel;

    /**工序编码（非表字段，展示用）*/
    @TableField(exist = false)
    @Schema(description = "工序编码")
    private String processCode;

    /**工序名称（非表字段，展示用）*/
    @TableField(exist = false)
    @Schema(description = "工序名称")
    private String processName;

    /**下一道工序编码（非表字段，展示用）*/
    @TableField(exist = false)
    @Schema(description = "下一道工序编码")
    private String nextProcessCode;

    /**下一道工序名称（非表字段，展示用）*/
    @TableField(exist = false)
    @Schema(description = "下一道工序名称")
    private String nextProcessName;
}
