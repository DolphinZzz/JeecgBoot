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

import java.io.Serializable;

/**
 * @Description: 系统编码生成结果表
 * @Author: jeecg-boot
 * @Date: 2026-11-04
 * @Version: V1.0
 */
@Data
@TableName("sys_auto_code_result")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "系统编码生成结果表")
public class SysAutoCodeResult implements Serializable {
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
     * 生成日期
     */
    @Excel(name = "生成日期", width = 15)
    @Schema(description = "生成日期")
    private String genDate;

    /**
     * 生成索引
     */
    @Excel(name = "生成索引", width = 15)
    @Schema(description = "生成索引")
    private Integer genIndex;

    /**
     * 编码结果
     */
    @Excel(name = "编码结果", width = 20)
    @Schema(description = "编码结果")
    private String lastResult;

    /**
     * 部分流水号
     */
    @Excel(name = "流水号", width = 15)
    @Schema(description = "流水号")
    private Integer lastSerialNo;

    /**
     * 固定字符/传入字符
     */
    @Excel(name = "传入字符", width = 15)
    @Schema(description = "传入字符")
    private String lastInputChar;
}
