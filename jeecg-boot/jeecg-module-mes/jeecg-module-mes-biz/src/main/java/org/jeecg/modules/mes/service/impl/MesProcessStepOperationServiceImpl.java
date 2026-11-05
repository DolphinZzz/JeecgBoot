package org.jeecg.modules.mes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.mes.entity.MesProcessStepOperation;
import org.jeecg.modules.mes.mapper.MesProcessStepOperationMapper;
import org.jeecg.modules.mes.service.IMesProcessStepOperationService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * @Description: 工序操作步骤明细表 ServiceImpl
 * @Author: jeecg-boot
 * @Date: 2026-11-05
 * @Version: V1.0
 */
@Service
public class MesProcessStepOperationServiceImpl extends ServiceImpl<MesProcessStepOperationMapper, MesProcessStepOperation> implements IMesProcessStepOperationService {

    @Override
    public List<MesProcessStepOperation> listByProcessStepId(Long processStepId) {
        if (processStepId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<MesProcessStepOperation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MesProcessStepOperation::getProcessStepId, processStepId)
                .orderByAsc(MesProcessStepOperation::getStepOrder)
                .orderByAsc(MesProcessStepOperation::getId);
        return this.list(queryWrapper);
    }
}
