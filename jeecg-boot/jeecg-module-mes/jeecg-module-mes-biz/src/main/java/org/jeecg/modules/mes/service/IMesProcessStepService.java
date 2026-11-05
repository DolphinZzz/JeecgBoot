package org.jeecg.modules.mes.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.mes.entity.MesProcessStep;

import java.util.List;

/**
 * @Description: 生产工序主表 Service
 * @Author: jeecg-boot
 * @Date: 2026-11-05
 * @Version: V1.0
 */
public interface IMesProcessStepService extends IService<MesProcessStep> {

    /**
     * 查询所有启用的生产工序列表（供工艺组成下拉选择）
     * @return 启用工序列表
     */
    List<MesProcessStep> listEnabledSteps();
}
