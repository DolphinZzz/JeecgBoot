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
import org.jeecg.common.aspect.annotation.Dict;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: 出库波次表
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@TableName("wms_wave_order")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "出库波次表")
public class WmsWaveOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /** 波次号 */
    @Excel(name = "波次号", width = 22)
    @Schema(description = "波次号")
    private String waveCode;

    /** 仓库ID */
    @Excel(name = "仓库", width = 15, dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Dict(dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Schema(description = "仓库ID")
    private Integer warehouseId;

    /** 拣货模式(1摘果法 2播种法) */
    @Excel(name = "拣货模式", width = 15, dicCode = "wms_pick_mode")
    @Dict(dicCode = "wms_pick_mode")
    @Schema(description = "拣货模式(1摘果法 2播种法)")
    private Integer pickMode;

    /** 承运商ID */
    @Excel(name = "承运商", width = 18, dictTable = "wms_logistics_company", dicText = "company_name", dicCode = "id")
    @Dict(dictTable = "wms_logistics_company", dicText = "company_name", dicCode = "id")
    @Schema(description = "承运商ID")
    private Long logisticsCompanyId;

    /** 存储温区 */
    @Excel(name = "存储温区", width = 15, dicCode = "wms_storage_condition")
    @Dict(dicCode = "wms_storage_condition")
    @Schema(description = "存储温区")
    private String storageCondition;

    /** 订单数 */
    @Excel(name = "订单数", width = 12)
    @Schema(description = "订单数")
    private Integer orderCount;

    /** SKU种数 */
    @Excel(name = "SKU种数", width = 12)
    @Schema(description = "SKU种数")
    private Integer skuCount;

    /** 总件数 */
    @Excel(name = "总件数", width = 12)
    @Schema(description = "总件数")
    private Integer totalCount;

    /** 状态(1待拣货 2拣货中 3已拣货 4分货中 5已完成 9已取消) */
    @Excel(name = "状态", width = 15, dicCode = "wms_wave_status")
    @Dict(dicCode = "wms_wave_status")
    @Schema(description = "状态")
    private Integer status;

    /** 作业员工ID */
    @Schema(description = "作业员工ID")
    private Integer empId;

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
