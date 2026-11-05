package org.jeecg.common.system.api;

import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.ServiceNameConstants;
import org.jeecg.common.system.api.factory.SysAutoCodeApiFallbackFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @Description: 系统自动编码生成 Feign API 接口（微服务远程调用）
 *
 * @ConditionalOnMissingClass("org.jeecg.modules.system.service.impl.SysAutoCodeServiceImpl") => 本地有实现类时（系统服务或单体），不实例化 Feign 接口
 * @Author: jeecg-boot
 * @Date: 2026-11-04
 * @Version: V1.0
 */
@Component
@FeignClient(
    contextId = "sysAutoCodeRemoteApi",
    value = ServiceNameConstants.SERVICE_SYSTEM,
    fallbackFactory = SysAutoCodeApiFallbackFactory.class
)
@ConditionalOnMissingClass("org.jeecg.modules.system.service.impl.SysAutoCodeServiceImpl")
public interface ISysAutoCodeApi {

    /**
     * 根据规则编码生成编号
     *
     * @param ruleCode 规则编码
     * @return Result<String>
     */
    @GetMapping("/sys/autocode/get/{ruleCode}")
    Result<String> getAutoCode(@PathVariable("ruleCode") String ruleCode);

    /**
     * 根据规则编码和传入字符生成编号
     *
     * @param ruleCode 规则编码
     * @param inputCharacter 传入字符
     * @return Result<String>
     */
    @GetMapping("/sys/autocode/get/{ruleCode}/{inputCharacter}")
    Result<String> getAutoCode(@PathVariable("ruleCode") String ruleCode, @PathVariable("inputCharacter") String inputCharacter);
}
