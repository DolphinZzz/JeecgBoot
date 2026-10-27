package org.jeecg.modules.wms.service.impl;

import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.mapper.WmsItemSkuMapper;
import org.jeecg.modules.wms.service.IWmsItemSkuService;
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
}
