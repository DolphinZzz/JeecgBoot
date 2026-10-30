package org.jeecg.modules.wms.entity;

import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: 多维实时库存
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
@TableName("wms_inventory")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "多维实时库存")
public class WmsInventory implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**仓库ID*/
    @Schema(description = "仓库ID")
    private Integer warehouseId;

    /**库区ID*/
    @Schema(description = "库区ID")
    private Integer warehouseAreaId;

    /**库位ID*/
    @Schema(description = "库位ID")
    private Integer warehouseAreaLocaId;

    /**货主ID（对应 wms_consignor.id）*/
    @Schema(description = "货主ID")
    private Long consignorId;

    /**规格ID*/
    @Schema(description = "规格ID")
    private Long skuId;

    /**批次号，无批次时为空串（参与唯一键）*/
    @Schema(description = "批次号")
    private String batchNumber;

    /**生产日期*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "生产日期")
    private Date prodDate;

    /**有效期至*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "有效期至")
    private Date expireDate;

    /**质量状态(1良品 2待检 3不良品)*/
    @Dict(dicCode = "wms_quality_status")
    @Schema(description = "质量状态(1良品 2待检 3不良品)")
    private Integer qualityStatus;

    /**物理总库存*/
    @Schema(description = "物理总库存")
    private Integer totalCount;

    /**可用库存*/
    @Schema(description = "可用库存")
    private Integer availableCount;

    /**锁定库存(出库/移库预占)*/
    @Schema(description = "锁定库存")
    private Integer lockedCount;

    /**冻结库存(盘点/异常)*/
    @Schema(description = "冻结库存")
    private Integer frozenCount;

    /**占用体积(cm³)*/
    @Schema(description = "占用体积")
    private Integer usedVolume;

    /**占用重量(g)*/
    @Schema(description = "占用重量")
    private Integer usedWeight;

    /**乐观锁版本号*/
    @Version
    @Schema(description = "乐观锁版本号")
    private Integer version;

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
