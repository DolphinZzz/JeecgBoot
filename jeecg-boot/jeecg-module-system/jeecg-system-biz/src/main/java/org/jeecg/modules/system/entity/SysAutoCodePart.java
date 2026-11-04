package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 系统编码规则组成表
 * @Author: jeecg-boot
 * @Date: 2026-11-04
 * @Version: V1.0
 */
@Data
@TableName("sys_auto_code_part")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "系统编码规则组成表")
public class SysAutoCodePart implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 规则ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(description = "规则ID")
    private Long ruleId;

    /**
     * 分段序号
     */
    @Excel(name = "分段序号", width = 15)
    @Schema(description = "分段序号")
    private Integer partIndex;

    /**
     * 分段类型
     */
    @Excel(name = "分段类型", width = 15, dicCode = "sys_autocode_parttype")
    @Dict(dicCode = "sys_autocode_parttype")
    @Schema(description = "分段类型")
    private String partType;

    /**
     * 分段编码
     */
    @Excel(name = "分段编码", width = 15)
    @Schema(description = "分段编码")
    private String partCode;

    /**
     * 分段名称
     */
    @Excel(name = "分段名称", width = 15)
    @Schema(description = "分段名称")
    private String partName;

    /**
     * 分段长度
     */
    @Excel(name = "分段长度", width = 15)
    @Schema(description = "分段长度")
    private Integer partLength;

    /**
     * 日期时间格式
     */
    @Excel(name = "日期时间格式", width = 15)
    @Schema(description = "日期时间格式")
    private String dateFormat;

    /**
     * 输入字符
     */
    @Excel(name = "输入字符", width = 15)
    @Schema(description = "输入字符")
    private String inputCharacter;

    /**
     * 固定字符
     */
    @Excel(name = "固定字符", width = 15)
    @Schema(description = "固定字符")
    private String fixCharacter;

    /**
     * 起始流水号
     */
    @Excel(name = "起始流水号", width = 15)
    @Schema(description = "起始流水号")
    private Integer serialStartNo;

    /**
     * 流水号步长
     */
    @Excel(name = "流水号步长", width = 15)
    @Schema(description = "流水号步长")
    private Integer serialStep;

    /**
     * 当前流水号
     */
    @Excel(name = "当前流水号", width = 15)
    @Schema(description = "当前流水号")
    private Integer serialNowNo;

    /**
     * 是否循环（Y/N）
     */
    @Excel(name = "是否循环", width = 15)
    @Schema(description = "是否循环")
    private String cycleFlag;

    /**
     * 循环方式
     */
    @Excel(name = "循环方式", width = 15, dicCode = "sys_autocode_cyclemethod")
    @Dict(dicCode = "sys_autocode_cyclemethod")
    @Schema(description = "循环方式")
    private String cycleMethod;

    /**
     * 备注
     */
    @Excel(name = "备注", width = 15)
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
