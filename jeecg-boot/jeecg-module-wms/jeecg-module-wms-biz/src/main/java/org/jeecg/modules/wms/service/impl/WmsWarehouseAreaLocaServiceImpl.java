package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsWarehouseArea;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.service.IWmsWarehouseAreaLocaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * @Description: 库位管理
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
@Service
public class WmsWarehouseAreaLocaServiceImpl extends ServiceImpl<WmsWarehouseAreaLocaMapper, WmsWarehouseAreaLoca> implements IWmsWarehouseAreaLocaService {

    private static final int LOCA_CODE_MAX_LENGTH = 100;

    @Autowired
    private WmsWarehouseAreaMapper warehouseAreaMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WmsWarehouseAreaLoca entity) {
        prepareInsert(entity);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(WmsWarehouseAreaLoca entity) {
        normalize(entity);
        checkUniqueLocaCode(entity);
        return super.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<WmsWarehouseAreaLoca> entityList, int batchSize) {
        for (WmsWarehouseAreaLoca entity : entityList) {
            prepareInsert(entity);
        }
        return super.saveBatch(entityList, batchSize);
    }

    private void prepareInsert(WmsWarehouseAreaLoca entity) {
        entity.setId(null);
        normalize(entity);
        checkUniqueLocaCode(entity);
        if (entity.getIsDel() == null) {
            entity.setIsDel(1);
        }
    }

    private void normalize(WmsWarehouseAreaLoca entity) {
        if (entity == null) {
            throw new JeecgBootException("库位数据不能为空");
        }
        String locaCode = entity.getLocaCode() == null ? "" : entity.getLocaCode().trim();
        if (oConvertUtils.isEmpty(locaCode)) {
            throw new JeecgBootException("库位编号不能为空");
        }
        if (locaCode.length() > LOCA_CODE_MAX_LENGTH) {
            throw new JeecgBootException("库位编号不能超过100个字符");
        }
        entity.setLocaCode(locaCode);

        if (entity.getWarehouseAreaId() == null) {
            throw new JeecgBootException("请选择所属库区");
        }

        // 关联库区信息，同步仓库ID与默认存储条件
        WmsWarehouseArea area = warehouseAreaMapper.selectById(entity.getWarehouseAreaId());
        if (area == null) {
            throw new JeecgBootException("所属库区不存在");
        }
        if (entity.getWarehouseId() == null) {
            entity.setWarehouseId(area.getWarehouseId());
        }
        if (oConvertUtils.isEmpty(entity.getStorageCondition()) && oConvertUtils.isNotEmpty(area.getStorageCondition())) {
            entity.setStorageCondition(area.getStorageCondition());
        }

        // 默认状态设置
        if (entity.getLocaStatus() == null) {
            entity.setLocaStatus(1); // 1 空闲
        }
        if (entity.getAllowMixSku() == null) {
            entity.setAllowMixSku(0);
        }
        if (entity.getAllowMixBatch() == null) {
            entity.setAllowMixBatch(0);
        }
        if (entity.getUsedVolume() == null) {
            entity.setUsedVolume(0);
        }
        if (entity.getUsedWeight() == null) {
            entity.setUsedWeight(0);
        }
        if (entity.getErgonomicLevel() == null) {
            entity.setErgonomicLevel(2); // 默认次优层
        }

        // 计算容积 mm³ -> cm³: (len * width * height) / 1000
        if ((entity.getLocalVolume() == null || entity.getLocalVolume() == 0)
                && entity.getLocaLen() != null && entity.getLocaLen() > 0
                && entity.getLocaWidth() != null && entity.getLocaWidth() > 0
                && entity.getLocaHeight() != null && entity.getLocaHeight() > 0) {
            long vol = ((long) entity.getLocaLen() * entity.getLocaWidth() * entity.getLocaHeight()) / 1000L;
            entity.setLocalVolume((int) Math.min(vol, Integer.MAX_VALUE));
        }
    }

    private void checkUniqueLocaCode(WmsWarehouseAreaLoca entity) {
        LambdaQueryWrapper<WmsWarehouseAreaLoca> query = new LambdaQueryWrapper<WmsWarehouseAreaLoca>()
                .eq(WmsWarehouseAreaLoca::getWarehouseId, entity.getWarehouseId())
                .eq(WmsWarehouseAreaLoca::getLocaCode, entity.getLocaCode());
        if (entity.getId() != null) {
            query.ne(WmsWarehouseAreaLoca::getId, entity.getId());
        }
        long count = this.count(query);
        if (count > 0) {
            throw new JeecgBootException("当前仓库下已存在相同编号的库位[" + entity.getLocaCode() + "]");
        }
    }
}
