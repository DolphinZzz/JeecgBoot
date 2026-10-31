package org.jeecg.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.entity.WmsWaveOrder;
import org.jeecg.modules.wms.vo.WmsWaveDetailView.WmsWavePickItem;
import org.jeecg.modules.wms.vo.WmsWavePage;
import org.jeecg.modules.wms.vo.WmsWaveQuery;

import java.util.List;

/**
 * @Description: 出库波次 Mapper 接口
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
public interface WmsWaveOrderMapper extends BaseMapper<WmsWaveOrder> {

    /**
     * 波次分页列表
     */
    IPage<WmsWavePage> selectWavePage(Page<WmsWavePage> page, @Param("q") WmsWaveQuery query);

    /**
     * 查询波次汇总待拣货行清单
     */
    List<WmsWavePickItem> selectWavePickItems(@Param("waveId") Long waveId);

    /**
     * 根据波次号查询
     */
    WmsWaveOrder selectByCode(@Param("waveCode") String waveCode);
}
