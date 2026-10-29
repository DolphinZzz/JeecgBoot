package org.jeecg.modules.wms.vo;

import java.util.Date;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Description: 库存维度（与 wms_inventory 唯一键 uk_inv_dim 一致）
 * 库位 + SKU + 批次 + 货主 + 质量状态 五者才能定位到唯一库存行。
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
public class WmsInventoryDimension {

    /**仓库ID*/
    private Integer warehouseId;

    /**库区ID*/
    private Integer warehouseAreaId;

    /**库位ID*/
    private Integer warehouseAreaLocaId;

    /**货主ID，为空按 0 处理*/
    private Long consignorId;

    /**SKU ID*/
    private Long skuId;

    /**批次号，为空按空串处理*/
    private String batchNumber;

    /**质量状态，为空按 1（良品）处理*/
    private Integer qualityStatus;

    /**生产日期，新建库存行时写入*/
    private Date prodDate;

    /**有效期至，新建库存行时写入*/
    private Date expireDate;

    // 命名必须是 getXxxOrDefault()：mapper XML 里用 #{d.xxxOrDefault} 取值，
    // MyBatis 按 JavaBean 约定调用 getXxxOrDefault()，方法名少了 get 前缀会报
    // ReflectionException（no getter for property）。
    public long getConsignorIdOrDefault() {
        return consignorId == null ? 0L : consignorId;
    }

    public String getBatchNumberOrDefault() {
        return batchNumber == null ? "" : batchNumber;
    }

    public int getQualityStatusOrDefault() {
        return qualityStatus == null ? 1 : qualityStatus;
    }
}
