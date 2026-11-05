package org.jeecg.common.system.api.factory;

import org.jeecg.common.system.api.ISysAutoCodeApi;
import org.jeecg.common.system.api.fallback.SysAutoCodeApiFallback;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Description: 系统自动编码生成 Feign 降级工厂
 * @Author: jeecg-boot
 * @Date: 2026-11-04
 * @Version: V1.0
 */
@Component
public class SysAutoCodeApiFallbackFactory implements FallbackFactory<ISysAutoCodeApi> {

    @Override
    public ISysAutoCodeApi create(Throwable cause) {
        SysAutoCodeApiFallback fallback = new SysAutoCodeApiFallback();
        fallback.setCause(cause);
        return fallback;
    }
}
