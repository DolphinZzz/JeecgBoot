package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 系统编码规则表
 * @Author: jeecg-boot
 * @Date: 2026-11-04
 * @Version: V1.0
 */
@Data
@TableName("sys_auto_code_rule")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "系统编码规则表")
public class SysAutoCodeRule implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 规则编码
     */
    @Excel(name = "规则编码", width = 15)
    @Schema(description = "规则编码")
    private String ruleCode;

    /**
     * 规则名称
     */
    @Excel(name = "规则名称", width = 20)
    @Schema(description = "规则名称")
    private String ruleName;

    /**
     * 编码描述
     */
    @Excel(name = "描述", width = 25)
    @Schema(description = "描述")
    private String ruleDesc;

    /**
     * 最大长度
     */
    @Excel(name = "最大长度", width = 15)
    @Schema(description = "最大长度")
    private Integer maxLength;

    /**
     * 是否补齐（Y-是 / N-否）
     */
    @Excel(name = "是否补齐", width = 15, replace = {"是_Y", "否_N"})
    @Schema(description = "是否补齐(Y/N)")
    private String isPadded;

    /**
     * 补齐字符
     */
    @Excel(name = "补齐字符", width = 15)
    @Schema(description = "补齐字符")
    private String paddedChar;

    /**
     * 补齐方式（L-左补齐 / R-右补齐）
     */
    @Excel(name = "补齐方式", width = 15, replace = {"左补齐_L", "右补齐_R"})
    @Schema(description = "补齐方式(L/R)")
    private String paddedMethod;

    /**
     * 是否启用（1-是 / 0-否）
     */
    @Excel(name = "是否可用", width = 15, replace = {"是_1", "否_0"})
    @Schema(description = "是否启用(1/0)")
    private Integer enableFlag;

    /**
     * 备注
     */
    @Excel(name = "备注", width = 25)
    @Schema(description = "备注")
    private String remark;

    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String createBy;

    /**
     * 创建时间
     */
    @Excel(name = "创建时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 修改人
     */
    @Schema(description = "修改人")
    private String updateBy;

    /**
     * 修改时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private Date updateTime;
}
