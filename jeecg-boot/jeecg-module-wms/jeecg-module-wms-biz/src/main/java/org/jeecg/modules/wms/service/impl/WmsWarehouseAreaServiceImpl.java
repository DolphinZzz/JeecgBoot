package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsWarehouseArea;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.service.IWmsWarehouseAreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 库区管理
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
@Service
public class WmsWarehouseAreaServiceImpl extends ServiceImpl<WmsWarehouseAreaMapper, WmsWarehouseArea> implements IWmsWarehouseAreaService {

    private static final int NAME_MAX_LENGTH = 200;

    @Autowired
    private WmsWarehouseAreaLocaMapper warehouseAreaLocaMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WmsWarehouseArea entity) {
        prepareInsert(entity);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(WmsWarehouseArea entity) {
        normalize(entity);
        if (entity.getId() != null) {
            WmsWarehouseArea old = this.getById(entity.getId());
            if (old != null && old.getWarehouseId() != null && !old.getWarehouseId().equals(entity.getWarehouseId())) {
                // 库区换仓库时，同步其下库位的仓库归属，避免库位挂在错误仓库
                warehouseAreaLocaMapper.update(null, new UpdateWrapper<WmsWarehouseAreaLoca>()
                        .eq("warehouse_area_id", entity.getId())
                        .set("warehouse_id", entity.getWarehouseId()));
            }
        }
        return super.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<WmsWarehouseArea> entityList, int batchSize) {
        for (WmsWarehouseArea entity : entityList) {
            prepareInsert(entity);
        }
        return super.saveBatch(entityList, batchSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        checkNoLocaUnderArea(List.of(id));
        return super.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(Collection<?> list) {
        checkNoLocaUnderArea(list);
        return super.removeByIds(list);
    }

    /**
     * 库区下仍存在库位时禁止删除，避免留下无法归属的库位
     */
    private void checkNoLocaUnderArea(Collection<?> areaIds) {
        if (areaIds == null || areaIds.isEmpty()) {
            return;
        }
        long locaCount = warehouseAreaLocaMapper.selectCount(new LambdaQueryWrapper<WmsWarehouseAreaLoca>()
                .in(WmsWarehouseAreaLoca::getWarehouseAreaId, areaIds));
        if (locaCount > 0) {
            throw new JeecgBootException("所选库区下仍存在 " + locaCount + " 个库位，请先删除库位后再删除库区");
        }
    }

    private void prepareInsert(WmsWarehouseArea entity) {
        entity.setId(null);
        normalize(entity);
        if (entity.getIsDel() == null) {
            entity.setIsDel(1);
        }
    }

    private void normalize(WmsWarehouseArea entity) {
        if (entity == null) {
            throw new JeecgBootException("请输入库区名称");
        }
        String name = entity.getName() == null ? "" : entity.getName().trim();
        if (oConvertUtils.isEmpty(name)) {
            throw new JeecgBootException("请输入库区名称");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new JeecgBootException("库区名称不能超过200个字符");
        }
        entity.setName(name);

        if (entity.getWarehouseId() == null) {
            throw new JeecgBootException("请选择所属仓库");
        }

        if (oConvertUtils.isEmpty(entity.getCategory())) {
            entity.setCategory("STORAGE");
        }
        if (oConvertUtils.isEmpty(entity.getZoneType())) {
            entity.setZoneType("GENERAL");
        }
    }
}
