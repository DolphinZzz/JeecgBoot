package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsWarehouse;
import org.jeecg.modules.wms.entity.WmsWarehouseArea;
import org.jeecg.modules.wms.enums.WmsWarehouseTypeEnum;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseMapper;
import org.jeecg.modules.wms.service.IWmsWarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 仓库管理
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
@Service
public class WmsWarehouseServiceImpl extends ServiceImpl<WmsWarehouseMapper, WmsWarehouse> implements IWmsWarehouseService {

    private static final int NAME_MAX_LENGTH = 200;

    @Autowired
    private WmsWarehouseAreaMapper warehouseAreaMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WmsWarehouse entity) {
        prepareInsert(entity);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(WmsWarehouse entity) {
        normalize(entity);
        return super.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<WmsWarehouse> entityList, int batchSize) {
        for (WmsWarehouse entity : entityList) {
            prepareInsert(entity);
        }
        return super.saveBatch(entityList, batchSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        checkNoAreaUnderWarehouse(List.of(id));
        return super.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(Collection<?> list) {
        checkNoAreaUnderWarehouse(list);
        return super.removeByIds(list);
    }

    /**
     * 仓库下仍存在库区时禁止删除，避免留下无法归属的库区与库位
     */
    private void checkNoAreaUnderWarehouse(Collection<?> warehouseIds) {
        if (warehouseIds == null || warehouseIds.isEmpty()) {
            return;
        }
        long areaCount = warehouseAreaMapper.selectCount(new LambdaQueryWrapper<WmsWarehouseArea>()
                .in(WmsWarehouseArea::getWarehouseId, warehouseIds));
        if (areaCount > 0) {
            throw new JeecgBootException("所选仓库下仍存在 " + areaCount + " 个库区，请先删除库区后再删除仓库");
        }
    }

    private void prepareInsert(WmsWarehouse entity) {
        entity.setId(null);
        normalize(entity);
        if (entity.getIsDel() == null) {
            entity.setIsDel(1);
        }
    }

    private void normalize(WmsWarehouse entity) {
        if (entity == null) {
            throw new JeecgBootException("请输入仓库名称");
        }
        String name = entity.getName() == null ? "" : entity.getName().trim();
        if (oConvertUtils.isEmpty(name)) {
            throw new JeecgBootException("请输入仓库名称");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new JeecgBootException("仓库名称不能超过200个字符");
        }
        entity.setName(name);

        if (!WmsWarehouseTypeEnum.isValid(entity.getWarehouseType())) {
            throw new JeecgBootException("仓库类型不合法：" + entity.getWarehouseType());
        }
        entity.setWarehouseType(WmsWarehouseTypeEnum.of(entity.getWarehouseType()).getCode());

        if (entity.getSort() == null) {
            entity.setSort(1);
        }
        if (entity.getEnableStatus() == null) {
            entity.setEnableStatus(1);
        }
    }
}
