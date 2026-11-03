package org.jeecg.modules.wms.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.entity.WmsStocktakingDetail;
import org.jeecg.modules.wms.vo.WmsStocktakingSkuSelectVO;

/**
 * @Description: 盘库单明细 Mapper 接口
 * @Author: jeecg-boot
 * @Date: 2026-11-03
 * @Version: V1.0
 */
public interface WmsStocktakingDetailMapper extends BaseMapper<WmsStocktakingDetail> {

    /**
     * 根据主单ID查询有效明细列表
     */
    List<WmsStocktakingDetail> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 盘库商品选择器多维分页查询
     */
    IPage<WmsStocktakingSkuSelectVO> selectSkuPageForStocktaking(
            Page<WmsStocktakingSkuSelectVO> page,
            @Param("categoryId") Integer categoryId,
            @Param("skuCode") String skuCode,
            @Param("skuName") String skuName,
            @Param("spuCode") String spuCode,
            @Param("spuName") String spuName,
            @Param("warehouseId") Integer warehouseId);

    /**
     * 查询某仓库下所有有库存的 SKU 列表（用于一键导入全仓商品）
     */
    List<WmsStocktakingSkuSelectVO> selectAllSkusInWarehouse(@Param("warehouseId") Integer warehouseId);
}
