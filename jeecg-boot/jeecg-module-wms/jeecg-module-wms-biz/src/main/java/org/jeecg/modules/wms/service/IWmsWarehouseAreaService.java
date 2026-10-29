package org.jeecg.modules.wms.service;

import org.jeecg.modules.wms.entity.WmsWarehouseArea;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.Serializable;
import java.util.Collection;

/**
 * @Description: 库区管理
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
public interface IWmsWarehouseAreaService extends IService<WmsWarehouseArea> {

    /**
     * 删除库区：库区下存在库位时禁止删除
     */
    @Override
    boolean removeById(Serializable id);

    /**
     * 批量删除库区：任意库区下存在库位时整体拒绝
     */
    @Override
    boolean removeByIds(Collection<?> list);
}
