package org.jeecg.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.entity.WmsWarehousingEntry;
import org.jeecg.modules.wms.vo.WmsEntryPage;
import org.jeecg.modules.wms.vo.WmsEntryQuery;

/**
 * @Description: 入库单
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
public interface WmsWarehousingEntryMapper extends BaseMapper<WmsWarehousingEntry> {

    /**
     * 入库单分页查询：带出仓库名、货主名、仓库类型与已收/已上架汇总，
     * 避免列表逐行查询明细。
     */
    IPage<WmsEntryPage> selectEntryPage(Page<WmsEntryPage> page, @Param("q") WmsEntryQuery query);

    /**
     * 按单号取入库单（单号唯一）。
     */
    WmsWarehousingEntry selectByCode(@Param("entryCode") String entryCode);
}
