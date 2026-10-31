package org.jeecg.modules.wms.entity;

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
 * @Description: 物流公司表
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@TableName("wms_logistics_company")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "物流公司表")
public class WmsLogisticsCompany implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    /** 物流公司名字 */
    @Excel(name = "物流公司名称", width = 20)
    @Schema(description = "物流公司名字")
    private String companyName;

    /** 物流运输最低费用(分) */
    @Excel(name = "最低运费", width = 12)
    @Schema(description = "物流运输最低费用")
    private Integer minPrice;

    /** 联系人 */
    @Excel(name = "联系人", width = 15)
    @Schema(description = "联系人")
    private String linkUser;

    /** 联系电话 */
    @Excel(name = "联系电话", width = 15)
    @Schema(description = "联系电话")
    private String linkePhone;

    /** 排序 */
    @Schema(description = "排序")
    private Integer sort;

    /** SaaS标识 */
    @Schema(description = "SaaS标识", hidden = true)
    private String saasId;

    /** 创建时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    /** 创建人账号 */
    @Schema(description = "创建人账号")
    private String createBy;

    /** 修改时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private Date updateTime;

    /** 修改人账号 */
    @Schema(description = "修改人账号")
    private String updateBy;

    /** 是否删除 1未删除 2已删除 */
    @TableLogic(value = "1", delval = "2")
    @Schema(description = "是否删除 1未删除 2已删除")
    private Integer isDel;
}
