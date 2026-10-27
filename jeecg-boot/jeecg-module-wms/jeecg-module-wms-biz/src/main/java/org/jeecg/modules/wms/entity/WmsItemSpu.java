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
 * @Description: 商品管理
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
@Data
@TableName("wms_item_spu")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "商品管理")
public class WmsItemSpu implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键，由数据库自增生成*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    /**商品名称*/
    @Excel(name = "商品名称", width = 15)
    @Schema(description = "商品名称")
    private String title;

    /**商品编码*/
    @Excel(name = "商品编码", width = 15)
    @Schema(description = "商品编码")
    private String subTitle;

    /**一级类别*/
    @Excel(name = "商品类别", width = 15, dictTable = "wms_item_category", dicText = "name", dicCode = "id")
    @Dict(dictTable = "wms_item_category", dicText = "name", dicCode = "id")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "一级类别")
    private Integer categoryId1;

    /**二级类别*/
    @Excel(name = "二级类别", width = 15, dictTable = "wms_item_category", dicText = "name", dicCode = "id")
    @Dict(dictTable = "wms_item_category", dicText = "name", dicCode = "id")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "二级类别")
    private Integer categoryId2;

    /**三级类别*/
    @Excel(name = "三级类别", width = 15, dictTable = "wms_item_category", dicText = "name", dicCode = "id")
    @Dict(dictTable = "wms_item_category", dicText = "name", dicCode = "id")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "三级类别")
    private Integer categoryId3;

    /**商品描述*/
    @Excel(name = "商品描述", width = 30)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "商品描述")
    private String description;

    /**商品详情，页面不展示*/
    @Schema(description = "商品详情", hidden = true)
    private String content;

    /**品牌*/
    @Excel(name = "品牌", width = 15, dictTable = "wms_brand", dicText = "name", dicCode = "id")
    @Dict(dictTable = "wms_brand", dicText = "name", dicCode = "id")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "品牌")
    private Integer brandId;

    /**供应商*/
    @Excel(name = "供应商名称", width = 15, dictTable = "wms_supplier", dicText = "supplier_name", dicCode = "id")
    @Dict(dictTable = "wms_supplier", dicText = "supplier_name", dicCode = "id")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "供应商")
    private Integer supplierId;

    /**重量单位，商品下所有规格共用*/
    @Excel(name = "重量单位", width = 15)
    @Schema(description = "重量单位 mg/g/kg")
    private String weightUnit;

    /**长度单位，长度、宽度、高度共用*/
    @Excel(name = "长度单位", width = 15)
    @Schema(description = "长度单位 mm/cm/dm/m")
    private String lengthUnit;

    /**体积单位*/
    @Excel(name = "体积单位", width = 15)
    @Schema(description = "体积单位 cm³/dm³/m³")
    private String volumeUnit;

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
