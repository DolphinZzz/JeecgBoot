package org.jeecg.modules.mes.service.impl;

import org.jeecg.modules.mes.entity.MesWorkshop;
import org.jeecg.modules.mes.mapper.MesWorkshopMapper;
import org.jeecg.modules.mes.service.IMesWorkshopService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Description: mes_workshop
 * @Author: jeecg-boot
 * @Date:   2026-11-03
 * @Version: V1.0
 */
@Service
public class MesWorkshopServiceImpl extends ServiceImpl<MesWorkshopMapper, MesWorkshop> implements IMesWorkshopService {

    @Override
    public String getNextWorkshopCode() {
        // 查询数据库中所有以 WS 开头的车间编码（包括已占用编码，避免唯一键冲突）
        List<MesWorkshop> list = this.lambdaQuery()
                .select(MesWorkshop::getWorkshopCode)
                .likeRight(MesWorkshop::getWorkshopCode, "WS")
                .list();
        Set<Integer> occupiedSeqs = new HashSet<>();
        if (list != null && !list.isEmpty()) {
            for (MesWorkshop item : list) {
                String code = item.getWorkshopCode();
                if (code != null && code.matches("^WS\\d{4,}$")) {
                    try {
                        int num = Integer.parseInt(code.substring(2));
                        occupiedSeqs.add(num);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        // 从 1001 开始探测最小未被占用的编号（自动填补空缺，遇到已有编码自动跳过）
        int targetSeq = 1001;
        while (occupiedSeqs.contains(targetSeq)) {
            targetSeq++;
        }
        return String.format("WS%04d", targetSeq);
    }
}
