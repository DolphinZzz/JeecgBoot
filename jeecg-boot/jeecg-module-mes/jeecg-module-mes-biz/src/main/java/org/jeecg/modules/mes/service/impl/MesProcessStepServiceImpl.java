package org.jeecg.modules.mes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.mes.entity.MesProcessStep;
import org.jeecg.modules.mes.mapper.MesProcessStepMapper;
import org.jeecg.modules.mes.service.IMesProcessStepService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description: 生产工序主表 ServiceImpl
 * @Author: jeecg-boot
 * @Date: 2026-11-05
 * @Version: V1.0
 */
@Service
public class MesProcessStepServiceImpl extends ServiceImpl<MesProcessStepMapper, MesProcessStep> implements IMesProcessStepService {

    @Override
    public List<MesProcessStep> listEnabledSteps() {
        LambdaQueryWrapper<MesProcessStep> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MesProcessStep::getIsEnabled, 1)
                .orderByAsc(MesProcessStep::getSortOrder)
                .orderByAsc(MesProcessStep::getId);
        return this.list(queryWrapper);
    }
}
