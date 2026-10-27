package org.jeecg.modules.wms.entity;

import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: 商品类别
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
@Data
@TableName("wms_item_category")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "商品类别")
public class WmsItemCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键，由数据库自增生成*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Integer id;

    /**分类名字*/
    @Excel(name = "分类名字", width = 20)
    @Schema(description = "分类名字")
    private String name;

    /**排序*/
    @Excel(name = "排序", width = 10)
    @Schema(description = "排序")
    private Integer sort;

    /**
     * 所属类别。空值表示顶级类别。
     * 更新策略为 ALWAYS，清空所属类别时才能把 parent_id 写成 NULL。
     */
    @Excel(name = "所属类别", width = 20, dictTable = "wms_item_category", dicText = "name", dicCode = "id")
    @Dict(dictTable = "wms_item_category", dicText = "name", dicCode = "id")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "所属类别，空为顶级")
    private Integer parentId;

    /**是否有下级。表无 has_child 列，查询后按子记录计算，不入库。*/
    @TableField(exist = false)
    @Schema(description = "是否有下级 1有 0无")
    private String hasChild;

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
