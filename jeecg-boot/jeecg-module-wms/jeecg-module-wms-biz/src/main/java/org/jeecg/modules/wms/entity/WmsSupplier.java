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
 * @Description: 供应商信息
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
@Data
@TableName("wms_supplier")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "供应商信息")
public class WmsSupplier implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键（表无自增，新增时由服务端按 MAX(id)+1 赋值）*/
    @TableId(type = IdType.INPUT)
    @Schema(description = "主键")
    private Integer id;

    /**供应商名字*/
    @Excel(name = "供应商名字", width = 15)
    @Schema(description = "供应商名字")
    private String supplierName;

    /**联系人名字*/
    @Excel(name = "联系人名字", width = 15)
    @Schema(description = "联系人名字")
    private String linkName;

    /**联系人手机号*/
    @Excel(name = "联系人手机号", width = 15)
    @Schema(description = "联系人手机号")
    private String linkPhone;

    /**省/直辖市*/
    @Excel(name = "省/直辖市", width = 15, dictTable = "base_province", dicText = "name", dicCode = "id")
    @Dict(dictTable = "base_province", dicText = "name", dicCode = "id")
    @Schema(description = "省/直辖市")
    private Integer provinceId;

    /**城市ID*/
    @Excel(name = "城市", width = 15, dictTable = "base_city", dicText = "name", dicCode = "id")
    @Dict(dictTable = "base_city", dicText = "name", dicCode = "id")
    @Schema(description = "城市ID")
    private Integer cityCode;

    /**详细地址*/
    @Excel(name = "详细地址", width = 15)
    @Schema(description = "详细地址")
    private String address;

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
