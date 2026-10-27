package org.jeecg.modules.wms.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.jeecg.modules.wms.api.WmsHelloApi;
import lombok.Setter;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * @author JeecgBoot
 */
@Slf4j
@Component
public class WmsHelloFallback implements FallbackFactory<WmsHelloApi> {
    @Setter
    private Throwable cause;

    @Override
    public WmsHelloApi create(Throwable throwable) {
        log.error("微服务接口调用失败： {}", cause);
        return null;
    }

}
