package org.jeecg.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.entity.WmsOutboundOrderDetails;
import org.jeecg.modules.wms.vo.WmsOutboundDetailView;

import java.util.List;

/**
 * @Description: 出库单明细 Mapper 接口
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
public interface WmsOutboundOrderDetailsMapper extends BaseMapper<WmsOutboundOrderDetails> {

    /**
     * 查询出库单下的全部明细视图（包含SKU、SPU、库位、货主名称等）
     */
    List<WmsOutboundDetailView> selectDetailViewsByOrderId(@Param("outboundOrderId") Long outboundOrderId);
}
