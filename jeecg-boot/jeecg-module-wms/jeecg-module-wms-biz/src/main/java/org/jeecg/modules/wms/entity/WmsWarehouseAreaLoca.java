package org.jeecg.modules.wms.entity;

import java.io.Serializable;
import java.math.BigDecimal;
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
 * @Description: 库位管理
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
@Data
@TableName("wms_warehouse_area_loca")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "库位管理")
public class WmsWarehouseAreaLoca implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID，数据库自增*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    /**所属仓库ID*/
    @Excel(name = "所属仓库", width = 15, dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Dict(dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Schema(description = "所属仓库ID")
    private Integer warehouseId;

    /**所属库区ID*/
    @Excel(name = "所属库区", width = 15, dictTable = "wms_warehouse_area", dicText = "name", dicCode = "id")
    @Dict(dictTable = "wms_warehouse_area", dicText = "name", dicCode = "id")
    @Schema(description = "所属库区ID")
    private Integer warehouseAreaId;

    /**库位编号*/
    @Excel(name = "库位编号", width = 15)
    @Schema(description = "库位编号")
    private String locaCode;

    /**库位长度(mm)*/
    @Excel(name = "长度(mm)", width = 12)
    @Schema(description = "库位长度(mm)")
    private Integer locaLen;

    /**库位宽度(mm)*/
    @Excel(name = "宽度(mm)", width = 12)
    @Schema(description = "库位宽度(mm)")
    private Integer locaWidth;

    /**库位高度(mm)*/
    @Excel(name = "高度(mm)", width = 12)
    @Schema(description = "库位高度(mm)")
    private Integer locaHeight;

    /**库位容积(cm³)*/
    @Excel(name = "容积(cm³)", width = 12)
    @Schema(description = "库位容积(cm³)")
    private Integer localVolume;

    /**库位承重(g)*/
    @Excel(name = "承重(g)", width = 12)
    @Schema(description = "库位承重(g)")
    private Integer localBearingCapacity;

    /**巷道号*/
    @Excel(name = "巷道号", width = 10)
    @Schema(description = "巷道号")
    private String tunnelNumber;

    /**货架号*/
    @Excel(name = "货架号", width = 10)
    @Schema(description = "货架号")
    private String shelfNumber;

    /**层号*/
    @Excel(name = "层号", width = 10)
    @Schema(description = "层号")
    private String layerNumber;

    /**ABC分类 A/B/C*/
    @Excel(name = "ABC分类", width = 10, dicCode = "wms_abc_class")
    @Dict(dicCode = "wms_abc_class")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "ABC分类 A/B/C")
    private String abcClass;

    /**存储条件(字典 wms_storage_condition)*/
    @Excel(name = "存储条件", width = 15, dicCode = "wms_storage_condition")
    @Dict(dicCode = "wms_storage_condition")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "存储条件")
    private String storageCondition;

    /**库位状态(1空闲 2占用 3盘点锁定 4禁用)*/
    @Excel(name = "库位状态", width = 12, dicCode = "wms_loca_status")
    @Dict(dicCode = "wms_loca_status")
    @Schema(description = "库位状态(1空闲 2占用 3盘点锁定 4禁用)")
    private Integer locaStatus;

    /**是否允许混SKU(1是 0否)*/
    @Excel(name = "混SKU", width = 10)
    @Schema(description = "是否允许混SKU(1是 0否)")
    private Integer allowMixSku;

    /**是否允许混批次(1是 0否)*/
    @Excel(name = "混批次", width = 10)
    @Schema(description = "是否允许混批次(1是 0否)")
    private Integer allowMixBatch;

    /**已用体积*/
    @Schema(description = "已用体积")
    private Integer usedVolume;

    /**已用承重*/
    @Schema(description = "已用承重")
    private Integer usedWeight;

    /**到拣货/复核区路径距离(米)*/
    @Excel(name = "拣货距离(m)", width = 12)
    @Schema(description = "到拣货/复核区路径距离(米)")
    private Integer pickDistance;

    /**到收货区路径距离(米)*/
    @Excel(name = "收货距离(m)", width = 12)
    @Schema(description = "到收货区路径距离(米)")
    private Integer receiveDistance;

    /**人体工学层级(1黄金层 2次优层 3不便层)*/
    @Excel(name = "人体工学层级", width = 12, dicCode = "wms_ergonomic_level")
    @Dict(dicCode = "wms_ergonomic_level")
    @Schema(description = "人体工学层级(1黄金层 2次优层 3不便层)")
    private Integer ergonomicLevel;

    /**库位得分(越高越好取)，由库位等级计算回写*/
    @Excel(name = "库位得分", width = 12)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "库位得分")
    private BigDecimal locaScore;

    /**固定库位绑定的SKU，为空表示非固定库位*/
    @Excel(name = "固定SKU", width = 15, dictTable = "wms_item_sku", dicText = "code", dicCode = "id")
    @Dict(dictTable = "wms_item_sku", dicText = "code", dicCode = "id")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "固定库位绑定的SKU ID")
    private Long fixedSkuId;

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
