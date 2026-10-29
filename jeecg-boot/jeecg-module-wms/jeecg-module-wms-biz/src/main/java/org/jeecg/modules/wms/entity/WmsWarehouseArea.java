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
 * @Description: 库区管理
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
@Data
@TableName("wms_warehouse_area")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "库区管理")
public class WmsWarehouseArea implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID，数据库自增*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    /**库区名称*/
    @Excel(name = "库区名称", width = 20)
    @Schema(description = "库区名称")
    private String name;

    /**所属仓库ID*/
    @Excel(name = "所属仓库", width = 15, dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Dict(dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Schema(description = "所属仓库ID")
    private Integer warehouseId;

    /**库区类型(字典 wms_warehouse_area_category)*/
    @Excel(name = "库区类型", width = 15, dicCode = "wms_warehouse_area_category")
    @Dict(dicCode = "wms_warehouse_area_category")
    @Schema(description = "库区类型(RECEIVING收货暂存 QC待检 STORAGE存储 PICKING拣货 RETURN退货不良 SHIPPING集货待发)")
    private String category;

    /**存储条件(字典 wms_storage_condition)*/
    @Excel(name = "存储条件", width = 15, dicCode = "wms_storage_condition")
    @Dict(dicCode = "wms_storage_condition")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "存储条件(NORMAL常温 COOL阴凉 COLD冷藏 FROZEN冷冻 ESD防静电 HAZARD危化)")
    private String storageCondition;

    /**ABC分类 A/B/C*/
    @Excel(name = "ABC分类", width = 10, dicCode = "wms_abc_class")
    @Dict(dicCode = "wms_abc_class")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "ABC分类 A/B/C")
    private String abcClass;

    /**分区类型(字典 wms_zone_type)*/
    @Excel(name = "分区类型", width = 15, dicCode = "wms_zone_type")
    @Dict(dicCode = "wms_zone_type")
    @Schema(description = "分区类型(GENERAL通用 SMALL小件 LARGE大件 VALUABLE贵品)")
    private String zoneType;

    /**SaaS标识*/
    @Schema(description = "SaaS标识", hidden = true)
    private String saasId;

    /**创建人*/
    @Schema(description = "创建人")
    private String createBy;

    /**创建时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    /**修改人*/
    @Schema(description = "修改人")
    private String updateBy;

    /**修改时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private Date updateTime;

    /**是否删除 1未删除 2已删除*/
    @TableLogic(value = "1", delval = "2")
    @Schema(description = "是否删除 1未删除 2已删除")
    private Integer isDel;
}
