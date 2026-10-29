package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsStrategyConfig;
import org.jeecg.modules.wms.mapper.WmsStrategyConfigMapper;
import org.jeecg.modules.wms.service.IWmsStrategyConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * @Description: 仓库策略参数配置
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
@Service
public class WmsStrategyConfigServiceImpl extends ServiceImpl<WmsStrategyConfigMapper, WmsStrategyConfig> implements IWmsStrategyConfigService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WmsStrategyConfig entity) {
        prepareInsert(entity);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(WmsStrategyConfig entity) {
        normalize(entity);
        checkUniqueKey(entity);
        return super.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<WmsStrategyConfig> entityList, int batchSize) {
        for (WmsStrategyConfig entity : entityList) {
            prepareInsert(entity);
        }
        return super.saveBatch(entityList, batchSize);
    }

    private void prepareInsert(WmsStrategyConfig entity) {
        entity.setId(null);
        normalize(entity);
        checkUniqueKey(entity);
        if (entity.getIsDel() == null) {
            entity.setIsDel(1);
        }
    }

    private void normalize(WmsStrategyConfig entity) {
        if (entity == null) {
            throw new JeecgBootException("策略配置对象不能为空");
        }
        if (entity.getWarehouseId() == null) {
            entity.setWarehouseId(0);
        }
        String key = entity.getConfigKey() == null ? "" : entity.getConfigKey().trim();
        if (oConvertUtils.isEmpty(key)) {
            throw new JeecgBootException("参数键不能为空");
        }
        entity.setConfigKey(key);

        if (oConvertUtils.isEmpty(entity.getConfigGroup())) {
            entity.setConfigGroup("GENERAL");
        } else {
            entity.setConfigGroup(entity.getConfigGroup().trim().toUpperCase());
        }
        if (entity.getConfigValue() != null) {
            entity.setConfigValue(entity.getConfigValue().trim());
        }
    }

    private void checkUniqueKey(WmsStrategyConfig entity) {
        LambdaQueryWrapper<WmsStrategyConfig> query = new LambdaQueryWrapper<WmsStrategyConfig>()
                .eq(WmsStrategyConfig::getWarehouseId, entity.getWarehouseId())
                .eq(WmsStrategyConfig::getConfigKey, entity.getConfigKey());
        if (entity.getId() != null) {
            query.ne(WmsStrategyConfig::getId, entity.getId());
        }
        long count = this.count(query);
        if (count > 0) {
            String scope = entity.getWarehouseId() == 0 ? "全局" : ("仓库ID " + entity.getWarehouseId());
            throw new JeecgBootException("在" + scope + "下已存在参数键[" + entity.getConfigKey() + "]");
        }
    }

    @Override
    public String getConfigValue(Integer warehouseId, String configKey, String defaultValue) {
        if (oConvertUtils.isEmpty(configKey)) {
            return defaultValue;
        }

        // 1. 尝试获取仓库级配置
        if (warehouseId != null && warehouseId > 0) {
            WmsStrategyConfig warehouseConfig = this.getOne(new LambdaQueryWrapper<WmsStrategyConfig>()
                    .eq(WmsStrategyConfig::getWarehouseId, warehouseId)
                    .eq(WmsStrategyConfig::getConfigKey, configKey.trim())
                    .last("LIMIT 1"), false);
            if (warehouseConfig != null && oConvertUtils.isNotEmpty(warehouseConfig.getConfigValue())) {
                return warehouseConfig.getConfigValue();
            }
        }

        // 2. 尝试获取全局默认配置 (warehouse_id = 0)
        WmsStrategyConfig globalConfig = this.getOne(new LambdaQueryWrapper<WmsStrategyConfig>()
                .and(w -> w.eq(WmsStrategyConfig::getWarehouseId, 0).or().isNull(WmsStrategyConfig::getWarehouseId))
                .eq(WmsStrategyConfig::getConfigKey, configKey.trim())
                .last("LIMIT 1"), false);
        if (globalConfig != null && oConvertUtils.isNotEmpty(globalConfig.getConfigValue())) {
            return globalConfig.getConfigValue();
        }

        return defaultValue;
    }

    @Override
    public Double getDoubleConfig(Integer warehouseId, String configKey, Double defaultValue) {
        String val = getConfigValue(warehouseId, configKey, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Integer getIntConfig(Integer warehouseId, String configKey, Integer defaultValue) {
        String val = getConfigValue(warehouseId, configKey, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Boolean getBooleanConfig(Integer warehouseId, String configKey, Boolean defaultValue) {
        String val = getConfigValue(warehouseId, configKey, null);
        if (val == null) {
            return defaultValue;
        }
        val = val.trim().toLowerCase();
        return "1".equals(val) || "true".equals(val) || "yes".equals(val) || "on".equals(val);
    }
}
