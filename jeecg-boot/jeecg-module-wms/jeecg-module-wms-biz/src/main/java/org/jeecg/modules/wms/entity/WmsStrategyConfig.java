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
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: 仓库策略参数配置
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
@Data
@TableName("wms_strategy_config")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "仓库策略参数配置")
public class WmsStrategyConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID，数据库自增*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**仓库ID(0为全局默认，仓库级配置覆盖全局)*/
    @Excel(name = "所属仓库", width = 15, dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Dict(dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Schema(description = "仓库ID(0为全局默认，仓库级配置覆盖全局)")
    private Integer warehouseId;

    /**参数分组(ABC 商品ABC / LOCA 库位等级 / PUTAWAY 上架 / ZONE 分区)*/
    @Excel(name = "参数分组", width = 15)
    @Schema(description = "参数分组(ABC / LOCA / PUTAWAY / ZONE)")
    private String configGroup;

    /**参数键*/
    @Excel(name = "参数键", width = 20)
    @Schema(description = "参数键")
    private String configKey;

    /**参数值*/
    @Excel(name = "参数值", width = 20)
    @Schema(description = "参数值")
    private String configValue;

    /**参数说明*/
    @Excel(name = "参数说明", width = 30)
    @Schema(description = "参数说明")
    private String remark;

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
