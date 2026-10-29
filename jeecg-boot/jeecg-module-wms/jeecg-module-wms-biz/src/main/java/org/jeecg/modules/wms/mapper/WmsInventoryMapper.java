package org.jeecg.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.vo.WmsInventoryDimension;
import org.jeecg.modules.wms.vo.WmsInventoryPage;
import org.jeecg.modules.wms.vo.WmsInventoryQuery;

/**
 * @Description: 多维实时库存
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
public interface WmsInventoryMapper extends BaseMapper<WmsInventory> {

    /**
     * 库存中心分页查询：一次带出仓库 / 库区 / 库位 / SKU 编码与效期状态，
     * 避免逐行走表字典翻译。
     */
    IPage<WmsInventoryPage> selectInventoryPage(Page<WmsInventoryPage> page, @Param("q") WmsInventoryQuery query);

    /**
     * 同一仓库（可限定 SKU）下所有库存行的 total_count 之和，用于流水与库存对账。
     */
    Long sumTotalCount(@Param("warehouseId") Integer warehouseId, @Param("skuId") Long skuId);

    /**
     * 按唯一键取库存行并加行锁，必须在事务内调用。
     */
    WmsInventory selectByDimensionForUpdate(@Param("d") WmsInventoryDimension dimension);

    /**
     * 精确回写四个数量并自增 version。调用方必须已持有该行锁。
     */
    int updateCounts(@Param("id") Long id,
                     @Param("totalCount") int totalCount,
                     @Param("availableCount") int availableCount,
                     @Param("lockedCount") int lockedCount,
                     @Param("frozenCount") int frozenCount,
                     @Param("isDel") int isDel);

    /**
     * 回写库存行的占用体积与占用重量。入库上架时调用，
     * 供库位剩余容积 / 承重的后续校验使用。
     */
    int updateUsage(@Param("id") Long id,
                    @Param("usedVolume") int usedVolume,
                    @Param("usedWeight") int usedWeight);
}
