package org.jeecg.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.entity.WmsOutboundOrder;
import org.jeecg.modules.wms.vo.WmsOutboundPage;
import org.jeecg.modules.wms.vo.WmsOutboundQuery;

/**
 * @Description: 出库单 Mapper 接口
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
public interface WmsOutboundOrderMapper extends BaseMapper<WmsOutboundOrder> {

    /**
     * 出库单分页列表（带关联仓库、货主、客户、波次、承运商名称，以及汇总分配/实拣/复核件数）
     */
    IPage<WmsOutboundPage> selectOutboundPage(Page<WmsOutboundPage> page, @Param("q") WmsOutboundQuery query);

    /**
     * 根据出库单号查询
     */
    WmsOutboundOrder selectByCode(@Param("outboundCode") String outboundCode);
}
