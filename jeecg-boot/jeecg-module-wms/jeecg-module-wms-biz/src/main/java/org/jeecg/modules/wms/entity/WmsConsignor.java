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
 * @Description: 货主信息
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@TableName("wms_consignor")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "货主信息")
public class WmsConsignor implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键，由数据库自增生成*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Integer id;

    /**货主编码：建单时不填则由服务端按 HZ + 6 位序号生成，唯一*/
    @Excel(name = "货主编码", width = 16)
    @Schema(description = "货主编码")
    private String consignorCode;

    /**货主名称*/
    @Excel(name = "货主名称", width = 30)
    @Schema(description = "货主名称")
    private String consignorName;

    /**货主类型(1自有货主 2第三方货主)*/
    @Excel(name = "货主类型", width = 14, dicCode = "wms_consignor_type")
    @Dict(dicCode = "wms_consignor_type")
    @Schema(description = "货主类型(1自有货主 2第三方货主)")
    private Integer consignorType;

    /**联系人*/
    @Excel(name = "联系人", width = 15)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "联系人")
    private String linkName;

    /**联系电话*/
    @Excel(name = "联系电话", width = 15)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "联系电话")
    private String linkPhone;

    /**所在省份*/
    @Excel(name = "所在省份", width = 15, dictTable = "base_province", dicText = "name", dicCode = "id")
    @Dict(dictTable = "base_province", dicText = "name", dicCode = "id")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "所在省份")
    private Integer provinceId;

    /**所在城市*/
    @Excel(name = "所在城市", width = 15, dictTable = "base_city", dicText = "name", dicCode = "id")
    @Dict(dictTable = "base_city", dicText = "name", dicCode = "id")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "所在城市")
    private Integer cityCode;

    /**详细地址*/
    @Excel(name = "详细地址", width = 30)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "详细地址")
    private String address;

    /**启用状态(1启用 0停用)。停用的货主不再出现在入库单等下拉里，历史单据不受影响*/
    @Excel(name = "启用状态", width = 10, dicCode = "valid_status")
    @Dict(dicCode = "valid_status")
    @Schema(description = "启用状态(1启用 0停用)")
    private Integer enableStatus;

    /**备注*/
    @Excel(name = "备注", width = 30)
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    @Schema(description = "备注")
    private String remark;

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
