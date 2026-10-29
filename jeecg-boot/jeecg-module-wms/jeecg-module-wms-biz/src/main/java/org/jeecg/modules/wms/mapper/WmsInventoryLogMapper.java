package org.jeecg.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.entity.WmsInventoryLog;
import org.jeecg.modules.wms.vo.WmsInventoryLogPage;
import org.jeecg.modules.wms.vo.WmsInventoryLogQuery;

/**
 * @Description: 库存变动流水
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
public interface WmsInventoryLogMapper extends BaseMapper<WmsInventoryLog> {

    /**
     * 流水查询：带出仓库 / 库位 / SKU 编码，可按库存行 ID 穿透。
     */
    IPage<WmsInventoryLogPage> selectLogPage(Page<WmsInventoryLogPage> page, @Param("q") WmsInventoryLogQuery query);
}
