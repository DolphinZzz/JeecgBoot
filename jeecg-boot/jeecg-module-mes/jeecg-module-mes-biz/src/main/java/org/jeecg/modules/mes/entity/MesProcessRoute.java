package org.jeecg.modules.mes.entity;

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
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: 工艺路线表
 * @Author: jeecg-boot
 * @Date: 2026-11-05
 * @Version: V1.0
 */
@Data
@TableName("mes_process_route")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "工艺路线表")
public class MesProcessRoute implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**工艺路线编码，唯一标识*/
    @Excel(name = "工艺路线编号", width = 18)
    @Schema(description = "工艺路线编号")
    private String routeCode;

    /**工艺路线名称*/
    @Excel(name = "工艺路线名称", width = 20)
    @Schema(description = "工艺路线名称")
    private String routeName;

    /**工艺路线说明*/
    @Excel(name = "工艺路线说明", width = 30)
    @Schema(description = "工艺路线说明")
    private String description;

    /**工艺路线版本号*/
    @Schema(description = "工艺路线版本号")
    private String version;

    /**是否启用：1=是，2=否*/
    @Excel(name = "是否启用", width = 12, replace = {"是_1", "否_2"})
    @Schema(description = "是否启用：1=是，2=否")
    private Integer isEnabled;

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
}
