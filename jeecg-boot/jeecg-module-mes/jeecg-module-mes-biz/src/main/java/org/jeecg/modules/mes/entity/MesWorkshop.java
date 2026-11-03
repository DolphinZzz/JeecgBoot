package org.jeecg.modules.mes.entity;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import org.jeecg.common.constant.ProvinceCityArea;
import org.jeecg.common.util.SpringContextUtils;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: mes_workshop
 * @Author: jeecg-boot
 * @Date:   2026-11-03
 * @Version: V1.0
 */
@Data
@TableName("mes_workshop")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description="mes_workshop")
public class MesWorkshop implements Serializable {
    private static final long serialVersionUID = 1L;

	/**主键ID*/
	@TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;
	/**车间编码*/
	@Excel(name = "车间编码", width = 15)
    @Schema(description = "车间编码")
    private String workshopCode;
	/**车间名称*/
	@Excel(name = "车间名称", width = 15)
    @Schema(description = "车间名称")
    private String workshopName;
	/**面积（平方米）*/
	@Excel(name = "面积（平方米）", width = 15)
    @Schema(description = "面积（平方米）")
    private Integer area;
	/**负责人*/
	@Excel(name = "负责人", width = 15, dictTable = "sys_user", dicText = "realname", dicCode = "id")
	@Dict(dictTable = "sys_user", dicText = "realname", dicCode = "id")
    @Schema(description = "负责人")
    private String responsiblePersonId;
	/**是否启用*/
    @Excel(name = "是否启用", width = 15,replace = {"是_1","否_0"} )
    @Schema(description = "是否启用")
    private Integer isEnabled;
	/**备注*/
	@Excel(name = "备注", width = 15)
    @Schema(description = "备注")
    private String remarks;
	/**创建时间*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;
	/**创建人账号*/
    @Schema(description = "创建人账号")
    private String createBy;
	/**修改时间*/
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private Date updateTime;
	/**修改人账号*/
    @Schema(description = "修改人账号")
    private String updateBy;
	/**是否删除 1 未删除 2 已删除*/
	@Excel(name = "是否删除 1 未删除 2 已删除", width = 15)
    @Schema(description = "是否删除 1 未删除 2 已删除")
    private Integer isDel;
}
