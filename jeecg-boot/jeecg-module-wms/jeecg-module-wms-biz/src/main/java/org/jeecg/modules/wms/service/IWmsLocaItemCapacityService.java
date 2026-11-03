package org.jeecg.modules.wms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.wms.entity.WmsLocaItemCapacity;
import org.jeecg.modules.wms.vo.WmsLocaCapacityItemVO;
import org.jeecg.modules.wms.vo.WmsLocaCapacitySaveDTO;

import java.util.List;
import java.util.Map;

/**
 * @Description: 库位商品规划与容量设置服务接口
 * @Author: jeecg-boot
 * @Date: 2026-11-01
 * @Version: V1.0
 */
public interface IWmsLocaItemCapacityService extends IService<WmsLocaItemCapacity> {

    /**
     * 查询指定库位已配置的商品列表及当前在库/剩余容量
     *
     * @param locaId 库位ID
     * @return 商品规划及容量明细
     */
    List<WmsLocaCapacityItemVO> listCapacityByLocaId(Integer locaId);

    /**
     * 批量查询多个库位的商品规划映射 (locaId -> List<WmsLocaCapacityItemVO>)
     *
     * @param locaIds 库位ID列表
     * @return 库位与规划列表映射
     */
    Map<Integer, List<WmsLocaCapacityItemVO>> mapCapacityByLocaIds(List<Integer> locaIds);

    /**
     * 批量保存库位商品规划
     * 核心业务约束：
     * 1. 单库位规划商品(SPU)最多不超过 3 个；
     * 2. 每项商品容量必须大于 0；
     * 3. 库位若配置多个商品自动将 allowMixSku 置为 1。
     *
     * @param dto 规划保存入参
     */
    void saveLocaCapacity(WmsLocaCapacitySaveDTO dto);

    /**
     * 获取指定库位针对指定 SPU 的规划容量限制（若未规划返回 null）
     *
     * @param locaId 库位ID
     * @param spuId  商品SPU ID
     * @return 规划记录
     */
    WmsLocaItemCapacity getByLocaAndSpu(Integer locaId, Long spuId);

    /**
     * 查询指定仓库下绑定了指定商品 SPU 的有效规划列表
     *
     * @param warehouseId 仓库ID（可为 null，表示不限仓库）
     * @param spuId       商品 SPU ID
     * @return 规划列表
     */
    List<WmsLocaItemCapacity> listByWarehouseAndSpu(Integer warehouseId, Long spuId);
}
