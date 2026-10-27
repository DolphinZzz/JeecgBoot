package org.jeecg.modules.wms.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.entity.WmsItemSku;
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
}
