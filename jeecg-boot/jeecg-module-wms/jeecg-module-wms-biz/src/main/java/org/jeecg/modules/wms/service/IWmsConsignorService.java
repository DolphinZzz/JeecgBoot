package org.jeecg.modules.wms.service;

import org.jeecg.modules.wms.entity.WmsConsignor;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @Description: 货主信息
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
public interface IWmsConsignorService extends IService<WmsConsignor> {

    /**
     * 删除前校验：货主已出现在入库单 / 库存 / 库内作业 / 盘点等业务单据里时不允许删除。
     * 货主是库存维度之一，删掉会让已有库存的归属变成孤儿数据。
     *
     * @param id 货主ID
     */
    void checkDeletable(Integer id);
}
