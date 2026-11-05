package org.jeecg.modules.mes.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.mes.entity.MesProcessStepOperation;

import java.util.List;

/**
 * @Description: 工序操作步骤明细表 Service
 * @Author: jeecg-boot
 * @Date: 2026-11-05
 * @Version: V1.0
 */
public interface IMesProcessStepOperationService extends IService<MesProcessStepOperation> {

    /**
     * 按工序ID查询操作步骤明细（按step_order升序）
     * @param processStepId 工序ID
     * @return 步骤列表
     */
    List<MesProcessStepOperation> listByProcessStepId(Long processStepId);
}
