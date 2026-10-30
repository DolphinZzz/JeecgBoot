package org.jeecg.modules.wms.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.vo.WmsItemSkuSelectVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @Description: 商品SKU
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
public interface WmsItemSkuMapper extends BaseMapper<WmsItemSku> {

    /**
     * 通过主表id查询未删除的子表数据
     */
    List<WmsItemSku> selectByMainId(@Param("mainId") Long mainId);

    /**
     * 取 SKU 在指定仓库的当前生效 ABC 等级（wms_abc_class_result.is_current = 1）。
     * 没有计算结果时返回 null，由上架策略引擎兜底到 SPU 的等级。
     */
    String selectCurrentAbcClass(@Param("warehouseId") Integer warehouseId, @Param("skuId") Long skuId);

    /**
     * 商品选择器分页查询（支持商品名称、编码、类别筛选）
     */
    IPage<WmsItemSkuSelectVo> selectSkuPageForSelect(
            Page<WmsItemSkuSelectVo> page,
            @Param("spuName") String spuName,
            @Param("skuCode") String skuCode,
            @Param("categoryId") Integer categoryId
    );
}
