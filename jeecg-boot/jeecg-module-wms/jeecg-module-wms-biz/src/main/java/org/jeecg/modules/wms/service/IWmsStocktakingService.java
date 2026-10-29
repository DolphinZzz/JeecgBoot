package org.jeecg.modules.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.Map;
import org.jeecg.modules.wms.entity.WmsStocktakingInfo;
import org.jeecg.modules.wms.vo.WmsStocktakingCountForm;
import org.jeecg.modules.wms.vo.WmsStocktakingCreateForm;

/**
 * @Description: 盘点作业（生成任务 / 录入实盘 / 差异审批 / 调账结案）
 *
 * 状态机：1 待盘点 → 2 已盘待审核 → 3 已确认（调账完成）。
 * 差异审批：auditStatus 0 无差异 / 1 待审批 / 2 已通过 / 3 已驳回。
 * 只有 auditStatus=2（或 0）的记录参与调账，驳回的记录维持账面不变。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
public interface IWmsStocktakingService extends IService<WmsStocktakingInfo> {

    /**
     * 按仓库 / 库区 / 库位范围生成盘点记录（账面数来自 wms_inventory）。
     *
     * @return 盘点批次号与记录数
     */
    Map<String, Object> createStocktaking(WmsStocktakingCreateForm form);

    /**
     * 录入实盘数，自动计算差异并置差异审批状态。
     */
    void submitCount(WmsStocktakingCountForm form);

    /**
     * 差异审批：pass=true 通过（auditStatus=2），false 驳回（auditStatus=3）。
     */
    void audit(Long id, boolean pass, String reason);

    /**
     * 审批通过后结案调账：把库存行 total 调到实盘数，写「盘点调账」流水并生成损溢记录。
     *
     * @return 调账摘要（调整行数、盘盈、盘亏）
     */
    Map<String, Object> confirm(Long id);

    /**
     * 按批次号查询盘点记录。
     */
    IPage<WmsStocktakingInfo> queryByBatch(String batchNo, String status, Integer pageNo, Integer pageSize);
}
