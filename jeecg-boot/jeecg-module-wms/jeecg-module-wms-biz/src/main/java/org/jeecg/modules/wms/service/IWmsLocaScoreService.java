package org.jeecg.modules.wms.service;

import java.util.Map;

/**
 * @Description: 库位 ABC 等级与分值计算服务
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
public interface IWmsLocaScoreService {

    /**
     * 计算并更新指定仓库的库位得分与 ABC 等级
     *
     * @param warehouseId 仓库ID
     * @return 统计结果摘要
     */
    Map<String, Object> recalculateWarehouseLocaScore(Integer warehouseId);
}
