package org.jeecg.modules.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.wms.entity.WmsWaveOrder;
import org.jeecg.modules.wms.vo.WmsWaveDetailView;
import org.jeecg.modules.wms.vo.WmsWaveGenerateForm;
import org.jeecg.modules.wms.vo.WmsWavePage;
import org.jeecg.modules.wms.vo.WmsWaveQuery;

import java.util.List;

/**
 * @Description: 出库波次服务接口
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
public interface IWmsWaveOrderService extends IService<WmsWaveOrder> {

    /**
     * 波次分页列表
     */
    IPage<WmsWavePage> queryWavePage(WmsWaveQuery query, Integer pageNo, Integer pageSize);

    /**
     * 查询波次详情（含波次主信息、关联出库单列表、汇总拣货任务行）
     */
    WmsWaveDetailView getWaveDetail(Long waveId);

    /**
     * 自动/指定分波生成波次单（决策 D3：按「承运商 + 温区」分组）
     */
    List<WmsWaveOrder> generateWaves(WmsWaveGenerateForm form);

    /**
     * 开始波次拣货（波次置2拣货中，出库单置40拣货中）
     */
    void startWavePick(Long waveId, Integer empId);

    /**
     * 完成波次分货播种（波次置5已完成，出库单置50待复核）
     */
    void completeWaveDistribute(Long waveId);

    /**
     * 取消波次（解除出库单绑定，恢复出库单状态）
     */
    void cancelWave(Long waveId);
}
