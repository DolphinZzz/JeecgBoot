package org.jeecg.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.entity.WmsWarehousingEntryDetails;
import org.jeecg.modules.wms.vo.WmsEntryDetailView;

/**
 * @Description: 入库单明细
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
public interface WmsWarehousingEntryDetailsMapper extends BaseMapper<WmsWarehousingEntryDetails> {

    /**
     * 按入库单取明细，带出商品编码 / 名称 / 默认质检方式与已上架数，
     * 顺带把 SKU 的 SN 管理开关带出来供收货页面判断是否强制扫码。
     */
    List<WmsEntryDetailView> selectDetailViews(@Param("entryId") Long entryId);
}
