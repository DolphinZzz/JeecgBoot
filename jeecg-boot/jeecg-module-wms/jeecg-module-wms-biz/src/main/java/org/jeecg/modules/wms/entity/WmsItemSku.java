package org.jeecg.modules.wms.entity;

import java.io.Serializable;
import java.math.BigDecimal;
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
 * @Description: 商品SKU
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
@Data
@TableName("wms_item_sku")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "商品SKU")
public class WmsItemSku implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键，由数据库自增生成*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    /**商品价格*/
    @Excel(name = "商品价格", width = 15)
    @Schema(description = "商品价格")
    private BigDecimal price;

    /**商品成本*/
    @Excel(name = "商品成本", width = 15)
    @Schema(description = "商品成本")
    private BigDecimal realPrice;

    /**主表ID（外键）*/
    @Schema(description = "Spu关联ID")
    private Long spuId;

    /**商品条码，同一商品下唯一*/
    @Excel(name = "商品条码", width = 15)
    @Schema(description = "商品条码")
    private String code;

    /**SKU标题，页面不展示*/
    @Schema(description = "商品标题", hidden = true)
    private String title;

    /**整型冗余列，页面不使用。规格中的商品单位在 spec，测量单位在商品主表。*/
    @Schema(description = "冗余单位列，页面不使用", hidden = true)
    private Integer unit;

    /**商品重量*/
    @Excel(name = "商品重量", width = 15)
    @Schema(description = "商品重量")
    private Integer productWeight;

    /**商品长度*/
    @Excel(name = "商品长度", width = 15)
    @Schema(description = "商品长度")
    private Integer productLength;

    /**商品宽度*/
    @Excel(name = "商品宽度", width = 15)
    @Schema(description = "商品宽度")
    private Integer productWidth;

    /**商品高度*/
    @Excel(name = "商品高度", width = 15)
    @Schema(description = "商品高度")
    private Integer productHeight;

    /**商品体积*/
    @Excel(name = "商品体积", width = 15)
    @Schema(description = "商品体积")
    private Integer productVolume;

    /**其他规格，页面不展示*/
    @Schema(description = "其他规格", hidden = true)
    private String spec;

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
