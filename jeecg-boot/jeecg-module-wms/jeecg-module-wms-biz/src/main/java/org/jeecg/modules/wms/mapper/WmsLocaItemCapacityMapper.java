package org.jeecg.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.entity.WmsLocaItemCapacity;

import java.util.List;

/**
 * @Description: 库位商品规划与容量设置 Mapper
 * @Author: jeecg-boot
 * @Date: 2026-11-01
 * @Version: V1.0
 */
@Mapper
public interface WmsLocaItemCapacityMapper extends BaseMapper<WmsLocaItemCapacity> {

    /**
     * 查询指定库位的有效商品规划列表
     */
    List<WmsLocaItemCapacity> selectByLocaId(@Param("locaId") Integer locaId);

    /**
     * 批量查询多个库位的有效规划列表
     */
    List<WmsLocaItemCapacity> selectByLocaIds(@Param("locaIds") List<Integer> locaIds);

    /**
     * 查询指定仓库下绑定了指定商品 SPU 的有效规划列表
     */
    List<WmsLocaItemCapacity> selectByWarehouseAndSpu(@Param("warehouseId") Integer warehouseId, @Param("spuId") Long spuId);
}
