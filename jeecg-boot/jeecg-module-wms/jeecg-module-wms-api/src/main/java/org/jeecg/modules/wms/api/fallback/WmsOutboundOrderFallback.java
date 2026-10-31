package org.jeecg.modules.wms.api.fallback;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.wms.api.WmsOutboundOrderApi;
import org.jeecg.modules.wms.dto.ErpOutboundOrderCreateDto;
import org.jeecg.modules.wms.vo.WmsOutboundOrderResultVo;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Description: WMS 出库单 Feign 熔断降级
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Slf4j
@Component
public class WmsOutboundOrderFallback implements FallbackFactory<WmsOutboundOrderApi> {
    @Setter
    private Throwable cause;

    @Override
    public WmsOutboundOrderApi create(Throwable throwable) {
        log.error("WMS出库单微服务接口调用失败：{}", throwable != null ? throwable.getMessage() : cause, throwable);
        return new WmsOutboundOrderApi() {
            @Override
            public Result<WmsOutboundOrderResultVo> createFromErp(ErpOutboundOrderCreateDto dto) {
                String erpOrderNo = dto != null ? dto.getErpOrderNo() : null;
                log.error("ERP推送出库单[erpOrderNo={}]失败熔断降级", erpOrderNo);
                return Result.error("WMS服务繁忙或不可用，ERP出库单[" + erpOrderNo + "]推送失败");
            }
        };
    }
}
