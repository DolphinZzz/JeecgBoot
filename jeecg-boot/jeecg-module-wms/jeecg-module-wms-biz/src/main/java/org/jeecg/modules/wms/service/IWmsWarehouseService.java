package org.jeecg.modules.wms.service;

import org.jeecg.modules.wms.entity.WmsWarehouse;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.Serializable;
import java.util.Collection;

/**
 * @Description: 仓库管理
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
public interface IWmsWarehouseService extends IService<WmsWarehouse> {

    /**
     * 删除仓库：仓库下存在库区时禁止删除，避免产生孤儿库区/库位
     */
    @Override
    boolean removeById(Serializable id);

    /**
     * 批量删除仓库：任意一个仓库下存在库区时整体拒绝
     */
    @Override
    boolean removeByIds(Collection<?> list);
}
