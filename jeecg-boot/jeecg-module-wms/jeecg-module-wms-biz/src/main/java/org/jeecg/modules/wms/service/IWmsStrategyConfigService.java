package org.jeecg.modules.wms.service;

import org.jeecg.modules.wms.entity.WmsStrategyConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @Description: 仓库策略参数配置
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
public interface IWmsStrategyConfigService extends IService<WmsStrategyConfig> {

    /**
     * 获取配置值（层级优先级：仓库级配置覆盖全局 warehouse_id=0 配置）
     *
     * @param warehouseId 仓库ID（可为null或0，表示只取全局配置）
     * @param configKey 参数键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getConfigValue(Integer warehouseId, String configKey, String defaultValue);

    /**
     * 获取浮点数值配置
     */
    Double getDoubleConfig(Integer warehouseId, String configKey, Double defaultValue);

    /**
     * 获取整数值配置
     */
    Integer getIntConfig(Integer warehouseId, String configKey, Integer defaultValue);

    /**
     * 获取布尔值配置 (1/true/yes 为 true)
     */
    Boolean getBooleanConfig(Integer warehouseId, String configKey, Boolean defaultValue);
}
