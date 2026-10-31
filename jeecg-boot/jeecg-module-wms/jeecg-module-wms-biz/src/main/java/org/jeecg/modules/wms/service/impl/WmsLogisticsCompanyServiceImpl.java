package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.wms.entity.WmsLogisticsCompany;
import org.jeecg.modules.wms.mapper.WmsLogisticsCompanyMapper;
import org.jeecg.modules.wms.service.IWmsLogisticsCompanyService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description: 物流公司服务实现
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Service
public class WmsLogisticsCompanyServiceImpl extends ServiceImpl<WmsLogisticsCompanyMapper, WmsLogisticsCompany>
        implements IWmsLogisticsCompanyService {

    @Override
    public List<WmsLogisticsCompany> listActiveCompanies() {
        return list(new LambdaQueryWrapper<WmsLogisticsCompany>()
                .eq(WmsLogisticsCompany::getIsDel, 1)
                .orderByAsc(WmsLogisticsCompany::getSort)
                .orderByAsc(WmsLogisticsCompany::getId));
    }
}
