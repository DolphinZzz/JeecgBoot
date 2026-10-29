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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: 入库分拣明细（一行 = 一个入库明细的收货分拣结果）
 *
 * 收货清点时按明细生成，登记实际可分拣数以及破损 / 超出 / 缺少三个偏差数；
 * 上架记录通过 details_sort_id 关联到这里，方便按分拣批次核对差异。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@TableName("wms_warehousing_entry_details_sort")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "入库分拣明细")
public class WmsWarehousingEntryDetailsSort implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**分拣数量（实际可分拣入库的数量，= 实收 - 破损）*/
    @Schema(description = "分拣数量")
    private Integer count;

    /**入库单明细ID*/
    @Schema(description = "入库单明细ID")
    private Long warehousingEntryDetailsId;

    /**规格编号*/
    @Schema(description = "规格编号")
    private Long skuId;

    /**状态(1分拣中 2已完成分拣 3已上架)*/
    @Schema(description = "状态(1分拣中 2已完成分拣 3已上架)")
    private Integer status;

    /**破损个数（直入退货区，不参与上架）*/
    @Schema(description = "破损个数")
    private Integer damageCount;

    /**超出数量（超收件数）*/
    @Schema(description = "超出数量")
    private Integer surpassCount;

    /**缺少数量（少收件数）*/
    @Schema(description = "缺少数量")
    private Integer lackCount;

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
