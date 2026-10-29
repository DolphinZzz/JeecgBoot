package org.jeecg.modules.wms.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
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
 * @Description: 库内作业单（移库移位 / 补货 / 加工 / 取消返架）
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
@TableName("wms_warehouse_operations")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "库内作业单")
public class WmsWarehouseOperations implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**作业单号*/
    @Excel(name = "作业单号", width = 20)
    @Schema(description = "作业单号")
    private String jobCode;

    /**仓库ID*/
    @Excel(name = "所属仓库", width = 15, dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Dict(dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Schema(description = "仓库ID")
    private Integer warehouseId;

    /**作业类型(数据字典，保留既有字段，新逻辑以 operationType 为准)*/
    @Schema(description = "作业类型", hidden = true)
    private Integer category;

    /**作业子类(1移库移位 2补货 3加工 4取消返架)*/
    @Excel(name = "作业子类", width = 12, dicCode = "wms_operation_type")
    @Dict(dicCode = "wms_operation_type")
    @Schema(description = "作业子类(1移库移位 2补货 3加工 4取消返架)")
    private Integer operationType;

    /**来源(1人工创建 2ABC优化建议 3系统补货)*/
    @Excel(name = "作业来源", width = 12, dicCode = "wms_operation_source")
    @Dict(dicCode = "wms_operation_source")
    @Schema(description = "来源(1人工创建 2ABC优化建议 3系统补货)")
    private Integer sourceType;

    /**来源ABC计算批次ID*/
    @Schema(description = "来源ABC计算批次ID")
    private Long sourceBatchId;

    /**备注*/
    @Excel(name = "备注", width = 24)
    @Schema(description = "备注")
    private String remark;

    /**状态(1未确认 2已执行 3已取消)*/
    @Excel(name = "状态", width = 10, dicCode = "wms_operation_status")
    @Dict(dicCode = "wms_operation_status")
    @Schema(description = "状态(1未确认 2已执行 3已取消)")
    private Integer status;

    /**操作员工ID*/
    @Schema(description = "操作员工ID")
    private Integer empId;

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

    /**作业明细，列表不展示*/
    @TableField(exist = false)
    @Schema(description = "作业明细")
    private List<WmsWarehouseOperationsDetails> detailsList;
}
