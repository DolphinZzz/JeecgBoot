package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsConsignor;
import org.jeecg.modules.wms.mapper.WmsConsignorMapper;
import org.jeecg.modules.wms.service.IWmsConsignorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;

/**
 * @Description: 货主信息
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Service
public class WmsConsignorServiceImpl extends ServiceImpl<WmsConsignorMapper, WmsConsignor> implements IWmsConsignorService {

    /** 货主编码前缀 */
    private static final String CODE_PREFIX = "HZ";
    /** 编码流水号位数 */
    private static final int CODE_SEQ_LENGTH = 6;
    private static final int NAME_MAX_LENGTH = 100;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WmsConsignor entity) {
        prepareInsert(entity);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(WmsConsignor entity) {
        normalize(entity);
        // 编码是业务上对外可见的标识，编辑时不允许改（页面也不放出该字段）
        entity.setConsignorCode(null);
        return super.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<WmsConsignor> entityList, int batchSize) {
        for (WmsConsignor entity : entityList) {
            prepareInsert(entity);
        }
        return super.saveBatch(entityList, batchSize);
    }

    @Override
    public void checkDeletable(Integer id) {
        if (id == null) {
            throw new JeecgBootException("请指定要删除的货主");
        }
        long refs = baseMapper.countReferences(id.longValue());
        if (refs > 0) {
            throw new JeecgBootException("该货主已被业务数据引用（入库/库存/流水/库内作业/盘点），不能删除；如需停用请把启用状态改为停用");
        }
    }

    /**
     * 补齐新增必填项：id 交给数据库自增，编码没填就发一个，状态默认启用。
     */
    private void prepareInsert(WmsConsignor entity) {
        entity.setId(null);
        normalize(entity);
        String code = entity.getConsignorCode() == null ? "" : entity.getConsignorCode().trim();
        if (oConvertUtils.isEmpty(code)) {
            entity.setConsignorCode(nextCode());
        } else {
            checkCodeUnique(code, null);
            entity.setConsignorCode(code);
        }
        if (entity.getEnableStatus() == null) {
            entity.setEnableStatus(1);
        }
        if (entity.getConsignorType() == null) {
            entity.setConsignorType(1);
        }
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(new Date());
        }
        if (entity.getIsDel() == null) {
            entity.setIsDel(1);
        }
    }

    private void normalize(WmsConsignor entity) {
        if (entity == null) {
            throw new JeecgBootException("请输入货主名称");
        }
        String name = entity.getConsignorName() == null ? "" : entity.getConsignorName().trim();
        if (oConvertUtils.isEmpty(name)) {
            throw new JeecgBootException("请输入货主名称");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new JeecgBootException("货主名称不能超过100个字符");
        }
        entity.setConsignorName(name);
    }

    /**
     * 编码查重。已逻辑删除的记录也算占用：编码会写进历史单据，不能被后来者复用。
     */
    private void checkCodeUnique(String code, Integer excludeId) {
        if (baseMapper.countByCode(code, excludeId) > 0) {
            throw new JeecgBootException("货主编码已存在：" + code);
        }
    }

    /** 货主编码：HZ + 6 位序号，在库内最大序号上 +1 */
    private String nextCode() {
        String max = baseMapper.selectMaxConsignorCode(CODE_PREFIX);
        int seq = 1;
        if (max != null && max.length() > CODE_PREFIX.length()) {
            try {
                seq = Integer.parseInt(max.substring(CODE_PREFIX.length())) + 1;
            } catch (NumberFormatException ignored) {
                // 历史编码不规整时退回从 1 开始，由唯一校验兜底
                seq = 1;
            }
        }
        return CODE_PREFIX + String.format("%0" + CODE_SEQ_LENGTH + "d", seq);
    }
}
