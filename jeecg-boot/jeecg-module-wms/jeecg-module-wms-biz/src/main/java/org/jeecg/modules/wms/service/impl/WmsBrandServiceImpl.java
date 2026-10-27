package org.jeecg.modules.wms.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsBrand;
import org.jeecg.modules.wms.mapper.WmsBrandMapper;
import org.jeecg.modules.wms.service.IWmsBrandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.Collection;

/**
 * @Description: 品牌管理
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
@Service
public class WmsBrandServiceImpl extends ServiceImpl<WmsBrandMapper, WmsBrand> implements IWmsBrandService {

    private static final int NAME_MAX_LENGTH = 100;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WmsBrand entity) {
        prepareInsert(entity);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(WmsBrand entity) {
        normalize(entity);
        return super.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<WmsBrand> entityList, int batchSize) {
        for (WmsBrand entity : entityList) {
            prepareInsert(entity);
        }
        return super.saveBatch(entityList, batchSize);
    }

    /**
     * 补齐新增必填项。id 留给数据库自增，is_del 默认 1。
     */
    private void prepareInsert(WmsBrand entity) {
        entity.setId(null);
        normalize(entity);
        if (entity.getIsDel() == null) {
            entity.setIsDel(1);
        }
    }

    private void normalize(WmsBrand entity) {
        if (entity == null) {
            throw new JeecgBootException("请输入品牌名字");
        }
        String name = entity.getName() == null ? "" : entity.getName().trim();
        if (oConvertUtils.isEmpty(name)) {
            throw new JeecgBootException("请输入品牌名字");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new JeecgBootException("品牌名字不能超过100个字符");
        }
        entity.setName(name);
    }
}
