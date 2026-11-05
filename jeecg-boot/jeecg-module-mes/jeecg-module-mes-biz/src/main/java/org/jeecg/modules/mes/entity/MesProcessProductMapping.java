package org.jeecg.modules.mes.entity;

import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: 工艺关联产品表
 * @Author: jeecg-boot
 * @Date: 2026-11-05
 * @Version: V1.0
 */
@Data
@TableName("mes_process_product_mapping")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "工艺关联产品表")
public class MesProcessProductMapping implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**产品物料编码，如 IF20250312005*/
    @Excel(name = "产品物料编码", width = 18)
    @Schema(description = "产品物料编码")
    private String productId;

    /**产品物料名称，如 螺丝刀刀柄【黑色】*/
    @Excel(name = "产品物料名称", width = 22)
    @Schema(description = "产品物料名称")
    private String productName;

    /**单位，如 个、套、千克等*/
    @Excel(name = "单位", width = 10)
    @Schema(description = "单位")
    private String unit;

    /**规格型号，如 “长15cm, 黑色”*/
    @Excel(name = "规格型号", width = 25)
    @Schema(description = "规格型号")
    private String specification;

    /**所属工艺路线ID（外键，关联 mes_process_route.id）*/
    @Schema(description = "所属工艺路线ID")
    private Long routeId;

    /**工艺路线编码，便于展示*/
    @Schema(description = "工艺路线编码")
    private String routeCode;

    /**工艺路线名称，便于展示*/
    @Schema(description = "工艺路线名称")
    private String routeName;

    /**该映射的版本号*/
    @Schema(description = "版本号")
    private String version;

    /**是否为默认工艺：1=是，0=否*/
    @Schema(description = "是否为默认工艺：1=是，0=否")
    private Integer isDefault;

    /**备注，如特殊要求*/
    @Excel(name = "备注", width = 25)
    @Schema(description = "备注")
    private String remark;

    /**创建时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    /**创建人账号*/
    @Schema(description = "创建人账号")
    private String createBy;

    /**修改时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private Date updateTime;

    /**修改人账号*/
    @Schema(description = "修改人账号")
    private String updateBy;

    /**是否删除：1=未删除，2=已删除*/
    @TableLogic(value = "1", delval = "2")
    @Schema(description = "是否删除：1=未删除，2=已删除")
    private Integer isDel;
}
