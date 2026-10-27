package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.entity.WmsItemSpu;
import org.jeecg.modules.wms.mapper.WmsItemSkuMapper;
import org.jeecg.modules.wms.mapper.WmsItemSpuMapper;
import org.jeecg.modules.wms.service.IWmsItemSpuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Description: 商品管理
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
@Service
public class WmsItemSpuServiceImpl extends ServiceImpl<WmsItemSpuMapper, WmsItemSpu> implements IWmsItemSpuService {

    private static final int TITLE_MAX_LENGTH = 100;
    private static final int SUB_TITLE_MAX_LENGTH = 200;
    private static final int DESCRIPTION_MAX_LENGTH = 500;
    private static final int CODE_MAX_LENGTH = 100;
    private static final Set<String> WEIGHT_UNITS = Set.of("mg", "g", "kg");
    private static final Set<String> LENGTH_UNITS = Set.of("mm", "cm", "dm", "m");
    private static final Set<String> VOLUME_UNITS = Set.of("cm³", "dm³", "m³");

    @Autowired
    private WmsItemSkuMapper wmsItemSkuMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMain(WmsItemSpu entity, List<WmsItemSku> skuList) {
        prepareInsert(entity);
        baseMapper.insert(entity);
        insertSkuList(entity.getId(), skuList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMain(WmsItemSpu entity, List<WmsItemSku> skuList) {
        normalizeSpu(entity);
        baseMapper.updateById(entity);
        wmsItemSkuMapper.delete(new LambdaQueryWrapper<WmsItemSku>().eq(WmsItemSku::getSpuId, entity.getId()));
        insertSkuList(entity.getId(), skuList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delMain(Long id) {
        wmsItemSkuMapper.delete(new LambdaQueryWrapper<WmsItemSku>().eq(WmsItemSku::getSpuId, id));
        baseMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delBatchMain(Collection<? extends Serializable> idList) {
        for (Serializable id : idList) {
            delMain(Long.valueOf(id.toString()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSkuBatch(Long spuId, List<Long> skuIds) {
        if (spuId == null || skuIds == null || skuIds.isEmpty()) {
            throw new JeecgBootException("请选择要删除的商品信息");
        }
        wmsItemSkuMapper.delete(new LambdaQueryWrapper<WmsItemSku>().eq(WmsItemSku::getSpuId, spuId).in(WmsItemSku::getId, skuIds));
    }

    /**
     * 补齐新增必填项。id 留给数据库自增，is_del 默认 1。
     */
    private void prepareInsert(WmsItemSpu entity) {
        entity.setId(null);
        normalizeSpu(entity);
        if (entity.getIsDel() == null) {
            entity.setIsDel(1);
        }
    }

    private void normalizeSpu(WmsItemSpu entity) {
        if (entity == null) {
            throw new JeecgBootException("请输入商品名称");
        }
        String title = entity.getTitle() == null ? "" : entity.getTitle().trim();
        if (oConvertUtils.isEmpty(title)) {
            throw new JeecgBootException("请输入商品名称");
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new JeecgBootException("商品名称不能超过100个字符");
        }
        entity.setTitle(title);
        String subTitle = entity.getSubTitle() == null ? "" : entity.getSubTitle().trim();
        if (oConvertUtils.isEmpty(subTitle)) {
            throw new JeecgBootException("请输入商品编码");
        }
        if (subTitle.length() > SUB_TITLE_MAX_LENGTH) {
            throw new JeecgBootException("商品编码不能超过200个字符");
        }
        entity.setSubTitle(subTitle);
        entity.setWeightUnit(requireMeasureUnit(entity.getWeightUnit(), WEIGHT_UNITS, "重量单位"));
        entity.setLengthUnit(requireMeasureUnit(entity.getLengthUnit(), LENGTH_UNITS, "长度单位"));
        entity.setVolumeUnit(requireMeasureUnit(entity.getVolumeUnit(), VOLUME_UNITS, "体积单位"));
        if (entity.getDescription() == null) {
            return;
        }
        String description = entity.getDescription().trim();
        if (description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new JeecgBootException("商品描述不能超过500个字符");
        }
        entity.setDescription(oConvertUtils.isEmpty(description) ? null : description);
    }

    private String requireMeasureUnit(String value, Set<String> allowed, String label) {
        String unit = value == null ? "" : value.trim();
        if (oConvertUtils.isEmpty(unit) || !allowed.contains(unit)) {
            throw new JeecgBootException("请选择" + label);
        }
        return unit;
    }

    private void insertSkuList(Long spuId, List<WmsItemSku> skuList) {
        if (skuList == null || skuList.isEmpty()) {
            return;
        }
        Set<String> codes = new HashSet<>();
        for (WmsItemSku sku : skuList) {
            if (sku == null) {
                continue;
            }
            String code = sku.getCode() == null ? "" : sku.getCode().trim();
            if (oConvertUtils.isEmpty(code)) {
                throw new JeecgBootException("请输入商品条码");
            }
            if (code.length() > CODE_MAX_LENGTH) {
                throw new JeecgBootException("商品条码不能超过100个字符");
            }
            if (!codes.add(code)) {
                throw new JeecgBootException("同一商品下商品条码不能重复：" + code);
            }
            sku.setId(null);
            sku.setCode(code);
            sku.setSpuId(spuId);
            if (sku.getIsDel() == null) {
                sku.setIsDel(1);
            }
            wmsItemSkuMapper.insert(sku);
        }
    }
}
