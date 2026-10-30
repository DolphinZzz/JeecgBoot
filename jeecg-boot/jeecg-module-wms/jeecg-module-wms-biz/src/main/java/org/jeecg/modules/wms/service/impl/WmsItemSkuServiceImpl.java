package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.mapper.WmsItemSkuMapper;
import org.jeecg.modules.wms.service.IWmsItemSkuService;
import org.jeecg.modules.wms.vo.WmsItemSkuSelectVo;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;

/**
 * @Description: 商品SKU
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
@Service
public class WmsItemSkuServiceImpl extends ServiceImpl<WmsItemSkuMapper, WmsItemSku> implements IWmsItemSkuService {

    @Override
    public List<WmsItemSku> selectByMainId(Long mainId) {
        return baseMapper.selectByMainId(mainId);
    }

    @Override
    public IPage<WmsItemSkuSelectVo> selectSkuPageForSelect(Integer pageNo, Integer pageSize, String spuName, String skuCode, Integer categoryId) {
        Page<WmsItemSkuSelectVo> page = new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize);
        return baseMapper.selectSkuPageForSelect(page, spuName, skuCode, categoryId);
    }
}
