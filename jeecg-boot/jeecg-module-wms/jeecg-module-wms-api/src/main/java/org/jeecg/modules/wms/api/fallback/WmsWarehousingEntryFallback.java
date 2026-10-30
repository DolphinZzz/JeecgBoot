package org.jeecg.modules.wms.api.fallback;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.wms.api.WmsWarehousingEntryApi;
import org.jeecg.modules.wms.dto.ErpWarehousingEntryCreateDto;
import org.jeecg.modules.wms.vo.WmsWarehousingEntryResultVo;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Description: WMS 入库单 Feign 熔断降级
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Slf4j
@Component
public class WmsWarehousingEntryFallback implements FallbackFactory<WmsWarehousingEntryApi> {
    @Setter
    private Throwable cause;

    @Override
    public WmsWarehousingEntryApi create(Throwable throwable) {
        log.error("WMS入库单微服务接口调用失败：{}", throwable != null ? throwable.getMessage() : cause, throwable);
        return new WmsWarehousingEntryApi() {
            @Override
            public Result<WmsWarehousingEntryResultVo> createFromErp(ErpWarehousingEntryCreateDto dto) {
                String erpPoNo = dto != null ? dto.getErpPoNo() : null;
                log.error("ERP推送入库单[erpPoNo={}]失败熔断降级", erpPoNo);
                return Result.error("WMS服务繁忙或不可用，ERP采购单[" + erpPoNo + "]入库推送失败");
            }
        };
    }
}
