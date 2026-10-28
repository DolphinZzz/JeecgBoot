package org.jeecg.modules.wms.service.impl;

import org.jeecg.modules.wms.entity.WmsCustomer;
import org.jeecg.modules.wms.mapper.WmsCustomerMapper;
import org.jeecg.modules.wms.service.IWmsCustomerService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: 客户信息
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
@Service
public class WmsCustomerServiceImpl extends ServiceImpl<WmsCustomerMapper, WmsCustomer> implements IWmsCustomerService {
}
