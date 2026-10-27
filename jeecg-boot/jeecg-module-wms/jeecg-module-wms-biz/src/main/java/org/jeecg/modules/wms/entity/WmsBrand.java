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
import org.jeecgframework.poi.excel.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: 品牌管理
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
@Data
@TableName("wms_brand")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "品牌管理")
public class WmsBrand implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键，由数据库自增生成*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Integer id;

    /**品牌名字*/
    @Excel(name = "品牌名字", width = 15)
    @Schema(description = "品牌名字")
    private String name;

    /**排序*/
    @Excel(name = "排序", width = 15)
    @Schema(description = "排序")
    private Integer sort;

    /**SaaS标识，业务不使用，页面不展示*/
    @Schema(description = "SaaS标识", hidden = true)
    private String saasId;

    /**创建人*/
    @Schema(description = "创建人")
    private String createBy;

    /**创建日期*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建日期")
    private Date createTime;

    /**更新人*/
    @Schema(description = "更新人")
    private String updateBy;

    /**更新日期*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新日期")
    private Date updateTime;

    /**是否删除 1未删除 2已删除*/
    @TableLogic(value = "1", delval = "2")
    @Schema(description = "是否删除 1未删除 2已删除")
    private Integer isDel;
}
