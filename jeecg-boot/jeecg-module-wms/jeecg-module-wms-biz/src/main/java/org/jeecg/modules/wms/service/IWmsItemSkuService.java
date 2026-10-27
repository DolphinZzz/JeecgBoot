package org.jeecg.modules.wms.service;

import org.jeecg.modules.wms.entity.WmsItemSku;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @Description: 商品SKU
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
public interface IWmsItemSkuService extends IService<WmsItemSku> {

    /**
     * 通过主表id查询未删除的SKU
     */
    List<WmsItemSku> selectByMainId(Long mainId);
}
