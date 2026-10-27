package org.jeecg.modules.wms.service.impl;

import org.jeecg.modules.wms.entity.WmsSupplier;
import org.jeecg.modules.wms.mapper.WmsSupplierMapper;
import org.jeecg.modules.wms.service.IWmsSupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.Collection;
import java.util.Set;

/**
 * @Description: 供应商信息
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
@Service
public class WmsSupplierServiceImpl extends ServiceImpl<WmsSupplierMapper, WmsSupplier> implements IWmsSupplierService {

    /** 直辖市：北京、上海、天津、重庆。下级是区，不保存城市。 */
    private static final Set<Integer> MUNICIPALITY_PROVINCE_IDS = Set.of(1, 2, 3, 4);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WmsSupplier entity) {
        prepareInsert(entity);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(WmsSupplier entity) {
        boolean municipality = clearCityForMunicipality(entity);
        boolean updated = super.updateById(entity);
        if (municipality && entity.getId() != null) {
            jdbcTemplate.update("UPDATE wms_supplier SET city_code = NULL WHERE id = ?", entity.getId());
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<WmsSupplier> entityList, int batchSize) {
        for (WmsSupplier entity : entityList) {
            prepareInsert(entity);
        }
        return super.saveBatch(entityList, batchSize);
    }

    /**
     * 补齐新增必填项。id 留给数据库自增，is_del 默认 1。
     */
    private void prepareInsert(WmsSupplier entity) {
        clearCityForMunicipality(entity);
        entity.setId(null);
        if (entity.getIsDel() == null) {
            entity.setIsDel(1);
        }
    }

    /**
     * 直辖市不保存城市（下级是区，不能当城市选）。
     *
     * @return 当前记录是否为直辖市
     */
    private boolean clearCityForMunicipality(WmsSupplier entity) {
        if (entity == null || entity.getProvinceId() == null || !MUNICIPALITY_PROVINCE_IDS.contains(entity.getProvinceId())) {
            return false;
        }
        entity.setCityCode(null);
        return true;
    }
}
