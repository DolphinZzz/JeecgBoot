package org.jeecg.modules.wms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 库位商品规划与容量设置
 * @Author: jeecg-boot
 * @Date: 2026-11-01
 * @Version: V1.0
 */
@Data
@TableName("wms_loca_item_capacity")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "库位商品规划与容量设置")
public class WmsLocaItemCapacity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**所属仓库ID*/
    @Schema(description = "所属仓库ID")
    private Integer warehouseId;

    /**所属库区ID*/
    @Schema(description = "所属库区ID")
    private Integer warehouseAreaId;

    /**库位ID*/
    @Schema(description = "库位ID")
    private Integer warehouseAreaLocaId;

    /**商品SPU ID*/
    @Schema(description = "商品SPU ID")
    private Long spuId;

    /**最大可存放件数*/
    @Schema(description = "最大可存放件数")
    private Integer maxCount;

    /**配置备注*/
    @Schema(description = "配置备注")
    private String remark;

    /**创建人*/
    @Schema(description = "创建人")
    private String createBy;

    /**创建时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    /**更新人*/
    @Schema(description = "更新人")
    private String updateBy;

    /**更新时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private Date updateTime;

    /**是否删除 1未删除 2已删除*/
    @TableLogic(value = "1", delval = "2")
    @Schema(description = "是否删除 1未删除 2已删除")
    private Integer isDel;
}
