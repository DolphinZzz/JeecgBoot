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
import org.jeecg.common.aspect.annotation.Dict;
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

    /**默认质检方式(1免检 2抽检 3全检)*/
    @Excel(name = "质检方式", width = 12, dicCode = "wms_qc_type")
    @Dict(dicCode = "wms_qc_type")
    @Schema(description = "默认质检方式(1免检 2抽检 3全检)")
    private Integer qcType;

    /**是否SN管理(1是 0否，决策 D2：按商品开关)*/
    @Excel(name = "SN管理", width = 10)
    @Schema(description = "是否SN管理(1是 0否)")
    private Integer isSnManaged;

    /**拣货区补货触发下限*/
    @Excel(name = "补货下限", width = 12)
    @Schema(description = "拣货区补货触发下限")
    private Integer minStock;

    /**拣货区最大安全库存*/
    @Excel(name = "安全库存上限", width = 12)
    @Schema(description = "拣货区最大安全库存")
    private Integer maxStock;

    /**上架策略(FIXED固定 ABC驱动 NEAREST就近)，为空用仓库默认*/
    @Excel(name = "上架策略", width = 12, dicCode = "wms_putaway_strategy")
    @Dict(dicCode = "wms_putaway_strategy")
    @Schema(description = "上架策略(FIXED固定 ABC驱动 NEAREST就近)")
    private String putawayStrategy;

    /**SKU标题，页面不展示*/
    @Schema(description = "商品标题", hidden = true)
    private String title;

    /**整型冗余列，页面不使用。规格中的商品单位在 spec，测量单位在商品主表。*/
    @Schema(description = "冗余单位列，页面不使用", hidden = true)
    private Integer unit;

    /**商品重量，单位取 SPU 的 weightUnit*/
    @Excel(name = "商品重量", width = 15)
    @Schema(description = "商品重量")
    private Integer productWeight;

    /**商品长度，单位取 SPU 的 lengthUnit*/
    @Excel(name = "商品长度", width = 15)
    @Schema(description = "商品长度")
    private Integer productLength;

    /**商品宽度，单位取 SPU 的 lengthUnit*/
    @Excel(name = "商品宽度", width = 15)
    @Schema(description = "商品宽度")
    private Integer productWidth;

    /**商品高度，单位取 SPU 的 lengthUnit*/
    @Excel(name = "商品高度", width = 15)
    @Schema(description = "商品高度")
    private Integer productHeight;

    /**商品体积，单位取 SPU 的 volumeUnit*/
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
