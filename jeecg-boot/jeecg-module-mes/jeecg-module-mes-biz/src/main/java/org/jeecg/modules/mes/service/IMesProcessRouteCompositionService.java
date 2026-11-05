package org.jeecg.modules.mes.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.mes.entity.MesProcessRouteComposition;

import java.util.List;

/**
 * @Description: MES-工艺工序组成表 Service
 * @Author: jeecg-boot
 * @Date: 2026-11-05
 * @Version: V1.0
 */
public interface IMesProcessRouteCompositionService extends IService<MesProcessRouteComposition> {

    /**
     * 根据工艺路线ID查询组成工序列表（自动按sequence_no升序计算填充工序编码、工序名称、下一道工序信息）
     * @param routeId 工艺路线ID
     * @return 组成工序列表
     */
    List<MesProcessRouteComposition> listByRouteIdWithNextStep(Long routeId);

    /**
     * 新增组成工序（含关键工序唯一性、工序重复、序号重复校验）
     * @param composition 组成工序
     */
    void addComposition(MesProcessRouteComposition composition);

    /**
     * 修改组成工序（含关键工序唯一性、工序重复、序号重复校验）
     * @param composition 组成工序
     */
    void editComposition(MesProcessRouteComposition composition);
}
