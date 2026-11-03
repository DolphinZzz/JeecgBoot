package org.jeecg.modules.wms.service;

import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;

/**
 * @Description: 统一落库兼容性与校验服务（上架、移库共用）
 * 1. 目标库位存在，未禁用、未盘点锁定；
 * 2. 质量状态与库区类别匹配（良品只能进存储区/拣货区，不良品只能进退货区）；
 * 3. 存储温区与商品要求匹配；
 * 4. 库区分区匹配（贵品/大件/小件/通用）；
 * 5. 绑定SKU匹配（固定库位只允许绑定的SKU）；
 * 6. 一库位一SKU一批次规则（STORAGE/PICKING严禁混SKU、严禁混批次；RETURN允许混放）；
 * 7. 剩余容积与剩余承重充足（未配置时跳过）。
 * 不再读写 max_count 与 SPU 规划容量！
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
public interface IWmsLocaCompatibilityService {

    /**
     * 目标库位校验（默认良品）
     */
    WmsWarehouseAreaLoca checkTarget(Integer targetLocaId, Long skuId, String batchNumber, int count);

    /**
     * 统一落库校验（带质量状态，良品=1，待检=2，不良品=3）
     */
    WmsWarehouseAreaLoca checkInbound(Integer targetLocaId, Long skuId, String batchNumber, int count, Integer qualityStatus);

    /**
     * 目标库位是否满足兼容性（默认良品，不抛异常）
     */
    boolean isCompatible(Integer targetLocaId, Long skuId, String batchNumber, int count);

    /**
     * 目标库位是否满足兼容性（带质量状态，不抛异常）
     */
    boolean isCompatible(Integer targetLocaId, Long skuId, String batchNumber, int count, Integer qualityStatus);

    /**
     * 刷新库位占用（根据在库 wms_inventory 重新计算已用体积、重量与状态，空闲时重置非固定库位绑定）
     */
    void refreshLocaOccupancy(Integer locaId);
}

