package org.jeecg.modules.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.enums.WmsInventoryBizTypeEnum;
import org.jeecg.modules.wms.vo.WmsInventoryDimension;
import org.jeecg.modules.wms.vo.WmsInventoryPage;
import org.jeecg.modules.wms.vo.WmsInventoryQuery;

/**
 * @Description: 多维实时库存
 *
 * 所有库存变动都必须走 {@link #applyChange}，由它统一维护
 * 「available + locked + frozen = total」恒等式并写入库存流水，
 * 业务模块不得自行 update wms_inventory。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
public interface IWmsInventoryService extends IService<WmsInventory> {

    /**
     * 多维库存查询。
     */
    IPage<WmsInventoryPage> queryInventoryPage(WmsInventoryQuery query, Integer pageNo, Integer pageSize);

    /**
     * 按维度取库存行，不存在返回 null。
     */
    WmsInventory getByDimension(WmsInventoryDimension dimension);

    /**
     * 按维度取库存行，加行锁（必须在事务内调用）。
     */
    WmsInventory getByDimensionForUpdate(WmsInventoryDimension dimension);

    /**
     * 按维度取库存行，不存在则以 0 库存创建。
     */
    WmsInventory getOrCreate(WmsInventoryDimension dimension);

    /**
     * 库存变动：total 变动 delta，locked / frozen 单独变动，available 由恒等式推导。
     * 写一条 wms_inventory_log。
     *
     * @param dimension   库存维度
     * @param totalDelta  物理库存变动（正增负减）
     * @param lockedDelta 锁定库存变动
     * @param frozenDelta 冻结库存变动
     * @param bizType     业务类型
     * @param bizNo       关联单号
     * @param remark      备注
     * @return 变动后的库存行
     */
    WmsInventory applyChange(WmsInventoryDimension dimension, int totalDelta, int lockedDelta, int frozenDelta,
                             WmsInventoryBizTypeEnum bizType, String bizNo, String remark);

    /**
     * 盘点调账：把库存行的 total 直接调整到 targetTotal（available 随之变化）。
     */
    WmsInventory adjustToTotal(WmsInventoryDimension dimension, int targetTotal,
                               WmsInventoryBizTypeEnum bizType, String bizNo, String remark);

    /**
     * 同一仓库（可限定 SKU）下所有库存行的 total_count 之和，用于与流水累计净额对账。
     */
    Long sumTotalCount(Integer warehouseId, Long skuId);
}
