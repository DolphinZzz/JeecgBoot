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
 * @Description: 工序操作步骤明细表
 * @Author: jeecg-boot
 * @Date: 2026-11-05
 * @Version: V1.0
 */
@Data
@TableName("mes_process_step_operation")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "工序操作步骤明细表")
public class MesProcessStepOperation implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**所属工序ID（外键，关联 mes_process_step.id）*/
    @Schema(description = "所属工序ID")
    private Long processStepId;

    /**操作步骤顺序编号（1,2,3...）*/
    @Excel(name = "顺序编号", width = 12)
    @Schema(description = "操作步骤顺序编号（1,2,3...）")
    private Integer stepOrder;

    /**步骤说明，如“检查外观是否有划痕”*/
    @Excel(name = "步骤说明", width = 30)
    @Schema(description = "步骤说明")
    private String stepDescription;

    /**辅助设备，如“放大镜”、“测试仪”*/
    @Excel(name = "辅助设备", width = 20)
    @Schema(description = "辅助设备")
    private String auxiliaryEquipment;

    /**辅助材料，如“酒精棉片”、“清洁布”*/
    @Excel(name = "辅助材料", width = 20)
    @Schema(description = "辅助材料")
    private String auxiliaryMaterials;

    /**备注*/
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
