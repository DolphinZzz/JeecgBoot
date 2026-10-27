package org.jeecg.modules.wms.service;

import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.entity.WmsItemSpu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 商品管理
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
public interface IWmsItemSpuService extends IService<WmsItemSpu> {

    /**
     * 添加一对多
     */
    void saveMain(WmsItemSpu entity, List<WmsItemSku> skuList);

    /**
     * 修改一对多
     */
    void updateMain(WmsItemSpu entity, List<WmsItemSku> skuList);

    /**
     * 删除一对多
     */
    void delMain(Long id);

    /**
     * 批量删除一对多
     */
    void delBatchMain(Collection<? extends Serializable> idList);

    /**
     * 删除某个商品下选中的 SKU
     */
    void deleteSkuBatch(Long spuId, List<Long> skuIds);
}
