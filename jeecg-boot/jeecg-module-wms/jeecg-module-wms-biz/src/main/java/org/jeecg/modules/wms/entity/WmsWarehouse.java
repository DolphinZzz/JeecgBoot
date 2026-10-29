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
 * @Description: 仓库管理
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
@Data
@TableName("wms_warehouse")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "仓库管理")
public class WmsWarehouse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID，数据库自增*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    /**仓库名称*/
    @Excel(name = "仓库名称", width = 20)
    @Schema(description = "仓库名称")
    private String name;

    /**仓库类型(NORMAL 普通仓 / INDUSTRIAL 工业制造仓)*/
    @Excel(name = "仓库类型", width = 15, dicCode = "wms_warehouse_type")
    @Dict(dicCode = "wms_warehouse_type")
    @Schema(description = "仓库类型(NORMAL 普通仓 / INDUSTRIAL 工业制造仓)")
    private String warehouseType;

    /**所在省份ID*/
    @Excel(name = "所在省份", width = 15, dictTable = "base_province", dicText = "name", dicCode = "id")
    @Dict(dictTable = "base_province", dicText = "name", dicCode = "id")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "所在省份ID")
    private Integer provinceId;

    /**所在省份名称*/
    @Schema(description = "所在省份名称")
    private String provinceName;

    /**所在城市ID*/
    @Excel(name = "所在城市", width = 15, dictTable = "base_city", dicText = "name", dicCode = "id")
    @Dict(dictTable = "base_city", dicText = "name", dicCode = "id")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "所在城市ID")
    private Integer cityId;

    /**所在城市名称*/
    @Schema(description = "所在城市名称")
    private String cityName;

    /**详细地址*/
    @Excel(name = "详细地址", width = 25)
    @Schema(description = "详细地址")
    private String address;

    /**经纬度/位置标识*/
    @Excel(name = "位置标识", width = 15)
    @Schema(description = "经纬度/位置标识")
    private String location;

    /**负责人用户ID(sys_user.id)*/
    @Excel(name = "负责人", width = 15, dictTable = "sys_user", dicText = "realname", dicCode = "id")
    @Dict(dictTable = "sys_user", dicText = "realname", dicCode = "id")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "负责人用户ID")
    private String personChargeId;

    /**负责人姓名*/
    @Schema(description = "负责人姓名")
    private String personChargeName;

    /**负责人电话*/
    @Excel(name = "负责人电话", width = 15)
    @Schema(description = "负责人电话")
    private String managerPhone;

    /**排序*/
    @Excel(name = "排序", width = 10)
    @Schema(description = "排序")
    private Integer sort;

    /**启用状态(1启用 0停用)*/
    @Excel(name = "启用状态", width = 10)
    @Schema(description = "启用状态(1启用 0停用)")
    private Integer enableStatus;

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
