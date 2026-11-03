package org.jeecg.modules.mes.service;

import org.jeecg.modules.mes.entity.MesWorkshop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @Description: mes_workshop
 * @Author: jeecg-boot
 * @Date:   2026-11-03
 * @Version: V1.0
 */
public interface IMesWorkshopService extends IService<MesWorkshop> {

    /**
     * 获取下一个车间编码，格式WSxxxx，xxxx为4位数字，从1001开始
     *
     * @return 车间编码
     */
    String getNextWorkshopCode();
}
