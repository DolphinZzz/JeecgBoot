package org.jeecg.modules.mes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.mes.entity.MesProcessRouteComposition;
import org.jeecg.modules.mes.entity.MesProcessStep;
import org.jeecg.modules.mes.mapper.MesProcessRouteCompositionMapper;
import org.jeecg.modules.mes.service.IMesProcessRouteCompositionService;
import org.jeecg.modules.mes.service.IMesProcessStepService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: MES-工艺工序组成表 ServiceImpl
 * @Author: jeecg-boot
 * @Date: 2026-11-05
 * @Version: V1.0
 */
@Service
public class MesProcessRouteCompositionServiceImpl extends ServiceImpl<MesProcessRouteCompositionMapper, MesProcessRouteComposition> implements IMesProcessRouteCompositionService {

    @Autowired
    private IMesProcessStepService mesProcessStepService;

    @Override
    public List<MesProcessRouteComposition> listByRouteIdWithNextStep(Long routeId) {
        if (routeId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<MesProcessRouteComposition> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MesProcessRouteComposition::getRouteId, routeId)
                .orderByAsc(MesProcessRouteComposition::getSequenceNo)
                .orderByAsc(MesProcessRouteComposition::getId);
        List<MesProcessRouteComposition> list = this.list(queryWrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询关联的生产工序信息
        Set<Long> stepIds = list.stream()
                .map(MesProcessRouteComposition::getProcessStepId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, MesProcessStep> stepMap = new HashMap<>();
        if (!stepIds.isEmpty()) {
            List<MesProcessStep> steps = mesProcessStepService.listByIds(stepIds);
            for (MesProcessStep step : steps) {
                stepMap.put(step.getId(), step);
            }
        }

        // 先填充当前工序编码和名称
        for (MesProcessRouteComposition item : list) {
            MesProcessStep step = stepMap.get(item.getProcessStepId());
            if (step != null) {
                item.setProcessCode(step.getProcessCode());
                item.setProcessName(step.getProcessName());
            }
        }

        // 按顺序填充下一道工序名称与编码
        for (int i = 0; i < list.size(); i++) {
            MesProcessRouteComposition current = list.get(i);
            if (i + 1 < list.size()) {
                MesProcessRouteComposition next = list.get(i + 1);
                current.setNextProcessCode(next.getProcessCode());
                current.setNextProcessName(next.getProcessName() != null ? next.getProcessName() : "-");
            } else {
                current.setNextProcessCode(null);
                current.setNextProcessName("-");
            }
        }

        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addComposition(MesProcessRouteComposition composition) {
        validateComposition(composition, false);
        if (composition.getSequenceNo() == null) {
            composition.setSequenceNo(1);
        }
        if (composition.getGanttColor() == null || composition.getGanttColor().trim().isEmpty()) {
            composition.setGanttColor("#00AEF3");
        }
        if (composition.getIsCriticalProcess() == null) {
            composition.setIsCriticalProcess(0);
        }
        if (composition.getNeedQcConfirmation() == null) {
            composition.setNeedQcConfirmation(0);
        }
        if (composition.getSetupTime() == null) {
            composition.setSetupTime(0);
        }
        if (composition.getWaitTime() == null) {
            composition.setWaitTime(0);
        }
        if (composition.getCreateTime() == null) {
            composition.setCreateTime(new Date());
        }
        if (composition.getIsDel() == null) {
            composition.setIsDel(1);
        }
        this.save(composition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editComposition(MesProcessRouteComposition composition) {
        if (composition.getId() == null) {
            throw new JeecgBootException("记录ID不能为空");
        }
        validateComposition(composition, true);
        composition.setUpdateTime(new Date());
        this.updateById(composition);
    }

    /**
     * 校验组成工序的业务边界：
     * 1. 同一工艺路线下只允许有一个关键工序（is_critical_process = 1）
     * 2. 同一工艺路线下不允许重复添加同一道工序
     * 3. 同一工艺路线下序号不允许重复
     */
    private void validateComposition(MesProcessRouteComposition composition, boolean isEdit) {
        if (composition.getRouteId() == null) {
            throw new JeecgBootException("所属工艺路线ID不能为空");
        }
        if (composition.getProcessStepId() == null) {
            throw new JeecgBootException("请选择工序");
        }

        Long excludeId = isEdit ? composition.getId() : null;

        // 1. 校验工序是否重复
        LambdaQueryWrapper<MesProcessRouteComposition> stepDupWrapper = new LambdaQueryWrapper<>();
        stepDupWrapper.eq(MesProcessRouteComposition::getRouteId, composition.getRouteId())
                .eq(MesProcessRouteComposition::getProcessStepId, composition.getProcessStepId());
        if (excludeId != null) {
            stepDupWrapper.ne(MesProcessRouteComposition::getId, excludeId);
        }
        if (this.count(stepDupWrapper) > 0) {
            throw new JeecgBootException("当前工艺路线中已存在该工序，请勿重复添加！");
        }

        // 2. 校验序号是否重复
        if (composition.getSequenceNo() != null) {
            LambdaQueryWrapper<MesProcessRouteComposition> seqDupWrapper = new LambdaQueryWrapper<>();
            seqDupWrapper.eq(MesProcessRouteComposition::getRouteId, composition.getRouteId())
                    .eq(MesProcessRouteComposition::getSequenceNo, composition.getSequenceNo());
            if (excludeId != null) {
                seqDupWrapper.ne(MesProcessRouteComposition::getId, excludeId);
            }
            if (this.count(seqDupWrapper) > 0) {
                throw new JeecgBootException("当前工艺路线中已存在相同的工序序号，请调整序号！");
            }
        }

        // 3. 校验关键工序唯一性：整个工艺路线只允许有一个关键工序
        if (Integer.valueOf(1).equals(composition.getIsCriticalProcess())) {
            LambdaQueryWrapper<MesProcessRouteComposition> keyWrapper = new LambdaQueryWrapper<>();
            keyWrapper.eq(MesProcessRouteComposition::getRouteId, composition.getRouteId())
                    .eq(MesProcessRouteComposition::getIsCriticalProcess, 1);
            if (excludeId != null) {
                keyWrapper.ne(MesProcessRouteComposition::getId, excludeId);
            }
            if (this.count(keyWrapper) > 0) {
                throw new JeecgBootException("整个工艺只允许有一个关键工序！");
            }
        }
    }
}
