package org.jeecg.modules.wms.api;

import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.wms.api.fallback.WmsOutboundOrderFallback;
import org.jeecg.modules.wms.dto.ErpOutboundOrderCreateDto;
import org.jeecg.modules.wms.vo.WmsOutboundOrderResultVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Description: WMS 出库单 Feign 客户端（供 ERP、MES 等外部服务通过 HTTP/Feign 调用）
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@FeignClient(contextId = "wmsOutboundOrderRemoteApi", value = "jeecg-wms", fallbackFactory = WmsOutboundOrderFallback.class)
public interface WmsOutboundOrderApi {

    /**
     * ERP/MES系统通过HTTP协议推送出库单给WMS进行创建
     *
     * @param dto 出库单参数
     * @return 创建结果
     */
    @PostMapping(value = "/wms/wmsOutboundOrder/createFromErp")
    Result<WmsOutboundOrderResultVo> createFromErp(@RequestBody ErpOutboundOrderCreateDto dto);
}
