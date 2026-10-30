package org.jeecg.modules.mes.api;
import org.jeecg.modules.mes.api.fallback.MesHelloFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "jeecg-mes", fallbackFactory = MesHelloFallback.class)
public interface MesHelloApi {

    /**
     * mes hello 微服务接口
     * @param
     * @return
     */
    @GetMapping(value = "/mes/hello")
    String callHello();
}
