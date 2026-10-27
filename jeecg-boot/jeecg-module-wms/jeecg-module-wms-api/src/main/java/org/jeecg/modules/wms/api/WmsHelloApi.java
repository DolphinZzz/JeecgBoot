package org.jeecg.modules.wms.api;
import org.jeecg.modules.wms.api.fallback.WmsHelloFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "jeecg-wms", fallbackFactory = WmsHelloFallback.class)
public interface WmsHelloApi {

    /**
     * wms hello 微服务接口
     * @param
     * @return
     */
    @GetMapping(value = "/wms/hello")
    String callHello();
}
