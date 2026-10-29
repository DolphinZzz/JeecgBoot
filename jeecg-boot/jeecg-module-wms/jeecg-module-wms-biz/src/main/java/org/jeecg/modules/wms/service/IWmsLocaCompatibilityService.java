package org.jeecg.modules.wms.service;

import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;

/**
 * @Description: 库位兼容性校验（方案 3.4.2 的四重校验）
 * 1. 目标库位未禁用、未盘点锁定；
 * 2. 存储温区与商品要求匹配；
 * 3. 混放策略允许（混 SKU / 混批次 / 固定库位绑定）；
 * 4. 剩余容积与剩余承重充足（库位未配置容积或承重时跳过对应项）。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
public interface IWmsLocaCompatibilityService {

    /**
     * 校验通过则返回，否则抛出 JeecgBootException 并说明原因。
     *
     * @param targetLocaId 目标库位ID
     * @param skuId        SKU ID
     * @param batchNumber  批次号
     * @param count        移入数量
     * @return 目标库位
     */
    WmsWarehouseAreaLoca checkTarget(Integer targetLocaId, Long skuId, String batchNumber, int count);

    /**
     * 目标库位是否满足兼容性（用于自动选位，不抛异常）。
     */
    boolean isCompatible(Integer targetLocaId, Long skuId, String batchNumber, int count);
}
