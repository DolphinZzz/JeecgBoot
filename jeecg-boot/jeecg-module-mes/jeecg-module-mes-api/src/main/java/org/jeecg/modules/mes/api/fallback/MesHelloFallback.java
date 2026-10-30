package org.jeecg.modules.mes.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.jeecg.modules.mes.api.MesHelloApi;
import lombok.Setter;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * @author JeecgBoot
 */
@Slf4j
@Component
public class MesHelloFallback implements FallbackFactory<MesHelloApi> {
    @Setter
    private Throwable cause;

    @Override
    public MesHelloApi create(Throwable throwable) {
        log.error("微服务接口调用失败： {}", cause);
        return null;
    }

}
