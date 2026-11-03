package org.jeecg.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.entity.WmsStocktakingOrder;

/**
 * @Description: 盘库单 Mapper 接口
 * @Author: jeecg-boot
 * @Date: 2026-11-03
 * @Version: V1.0
 */
public interface WmsStocktakingOrderMapper extends BaseMapper<WmsStocktakingOrder> {

    /**
     * 根据单号获取单据
     */
    WmsStocktakingOrder selectByOrderNo(@Param("orderNo") String orderNo);
}
