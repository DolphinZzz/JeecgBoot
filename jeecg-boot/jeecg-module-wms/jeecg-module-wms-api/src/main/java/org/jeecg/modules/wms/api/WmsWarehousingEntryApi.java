package org.jeecg.modules.wms.api;

import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.wms.api.fallback.WmsWarehousingEntryFallback;
import org.jeecg.modules.wms.dto.ErpWarehousingEntryCreateDto;
import org.jeecg.modules.wms.vo.WmsWarehousingEntryResultVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Description: WMS 入库单 Feign 客户端（供 ERP、MES 等外部服务通过 HTTP 协议调用）
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@FeignClient(contextId = "wmsWarehousingEntryRemoteApi", value = "jeecg-wms", fallbackFactory = WmsWarehousingEntryFallback.class)
public interface WmsWarehousingEntryApi {

    /**
     * ERP系统通过HTTP协议推送采购入库单给WMS进行创建
     *
     * @param dto 入库单信息（含ERP采购单号、仓库、货主、入库商品明细等，批次号未传时WMS自动生成）
     * @return 创建成功的WMS入库单结果（入库单号、批次号等）
     */
    @PostMapping(value = "/wms/wmsWarehousingEntry/createFromErp")
    Result<WmsWarehousingEntryResultVo> createFromErp(@RequestBody ErpWarehousingEntryCreateDto dto);
}
