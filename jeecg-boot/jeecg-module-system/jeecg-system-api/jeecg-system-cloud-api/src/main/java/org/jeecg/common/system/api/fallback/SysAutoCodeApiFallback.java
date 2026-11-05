package org.jeecg.common.system.api.fallback;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.api.ISysAutoCodeApi;

/**
 * @Description: 系统自动编码生成 Feign 降级处理
 * @Author: jeecg-boot
 * @Date: 2026-11-04
 * @Version: V1.0
 */
@Slf4j
public class SysAutoCodeApiFallback implements ISysAutoCodeApi {

    @Setter
    private Throwable cause;

    @Override
    public Result<String> getAutoCode(String ruleCode) {
        log.error("Feign调用[getAutoCode]失败，规则编码: {}", ruleCode, cause);
        return Result.error("获取编码失败: " + (cause != null ? cause.getMessage() : "服务熔断降级"));
    }

    @Override
    public Result<String> getAutoCode(String ruleCode, String inputCharacter) {
        log.error("Feign调用[getAutoCode]失败，规则编码: {}, 传入字符: {}", ruleCode, inputCharacter, cause);
        return Result.error("获取编码失败: " + (cause != null ? cause.getMessage() : "服务熔断降级"));
    }
}
