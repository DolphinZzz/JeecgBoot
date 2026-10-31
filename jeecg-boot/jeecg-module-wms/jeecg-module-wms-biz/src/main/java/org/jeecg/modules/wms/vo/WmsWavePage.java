package org.jeecg.modules.wms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jeecg.common.aspect.annotation.Dict;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description: 波次分页视图
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "波次分页列表视图")
public class WmsWavePage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @Schema(description = "主键ID")
    private Long id;

    /** 波次号 */
    @Schema(description = "波次号")
    private String waveCode;

    /** 仓库ID */
    @Dict(dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Schema(description = "仓库ID")
    private Integer warehouseId;

    /** 仓库名称 */
    @Schema(description = "仓库名称")
    private String warehouseName;

    /** 拣货模式(1摘果法 2播种法) */
    @Dict(dicCode = "wms_pick_mode")
    @Schema(description = "拣货模式")
    private Integer pickMode;

    /** 承运商ID */
    @Dict(dictTable = "wms_logistics_company", dicText = "company_name", dicCode = "id")
    @Schema(description = "承运商ID")
    private Long logisticsCompanyId;

    /** 承运商名称 */
    @Schema(description = "承运商名称")
    private String logisticsCompanyName;

    /** 存储温区 */
    @Dict(dicCode = "wms_storage_condition")
    @Schema(description = "存储温区")
    private String storageCondition;

    /** 订单数 */
    @Schema(description = "订单数")
    private Integer orderCount;

    /** SKU种数 */
    @Schema(description = "SKU种数")
    private Integer skuCount;

    /** 总件数 */
    @Schema(description = "总件数")
    private Integer totalCount;

    /** 状态(1待拣货 2拣货中 3已拣货 4分货中 5已完成 9已取消) */
    @Dict(dicCode = "wms_wave_status")
    @Schema(description = "状态")
    private Integer status;

    /** 作业员工ID */
    @Schema(description = "作业员工ID")
    private Integer empId;

    /** 创建时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    /** 创建人 */
    @Schema(description = "创建人")
    private String createBy;
}
