package org.jeecg.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.entity.WmsWarehousingEntryUp;
import org.jeecg.modules.wms.vo.WmsPutawayPage;
import org.jeecg.modules.wms.vo.WmsPutawayQuery;

/**
 * @Description: 入库上架记录
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
public interface WmsWarehousingEntryUpMapper extends BaseMapper<WmsWarehousingEntryUp> {

    /**
     * 上架记录分页查询：带出入库单号、库位编码、商品编码与名称，
     * 以及推荐库位编码，方便直接看偏离情况。
     */
    IPage<WmsPutawayPage> selectPutawayPage(Page<WmsPutawayPage> page, @Param("q") WmsPutawayQuery query);
}
