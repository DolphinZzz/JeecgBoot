package org.jeecg.modules.wms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.wms.entity.WmsLogisticsCompany;

import java.util.List;

/**
 * @Description: 物流公司服务接口
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
public interface IWmsLogisticsCompanyService extends IService<WmsLogisticsCompany> {

    /**
     * 查询所有启用的物流承运商列表
     */
    List<WmsLogisticsCompany> listActiveCompanies();
}
