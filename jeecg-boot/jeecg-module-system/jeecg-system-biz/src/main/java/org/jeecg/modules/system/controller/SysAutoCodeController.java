package org.jeecg.modules.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.api.ISysAutoCodeApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description: 系统自动编码生成控制器
 * 提供 HTTP 接口供前端或第三方系统调用，支持单体与微服务网关路由
 * 遵循 JeecgBoot 系统服务 /sys/ 路由规范（微服务网关路由断言默认匹配 /sys/**）
 * @Author: jeecg-boot
 * @Date: 2026-11-04
 * @Version: V1.0
 */
@Slf4j
@Tag(name = "系统自动编码生成接口")
@RestController
@RequestMapping({"/sys/autocode", "/system/autocode"})
public class SysAutoCodeController {

    @Autowired
    private ISysAutoCodeApi sysAutoCodeApi;

    /**
     * 根据规则编码生成编号
     * 示例：GET /sys/autocode/get/WORKSHOP_CODE
     *
     * @param ruleCode 规则编码
     * @return 生成的编号结果
     */
    @Operation(summary = "根据规则编码生成编号")
    @GetMapping("/get/{ruleCode}")
    public Result<String> getAutoCode(
            @Parameter(description = "规则编码", required = true) @PathVariable("ruleCode") String ruleCode) {
        log.info("REST 请求生成自动编码, ruleCode: {}", ruleCode);
        return sysAutoCodeApi.getAutoCode(ruleCode);
    }

    /**
     * 根据规则编码与传入字符生成编号
     * 示例：GET /sys/autocode/get/WORKSHOP_CODE/WH01
     *
     * @param ruleCode 规则编码
     * @param inputCharacter 传入字符
     * @return 生成的编号结果
     */
    @Operation(summary = "根据规则编码与传入字符生成编号")
    @GetMapping("/get/{ruleCode}/{inputCharacter}")
    public Result<String> getAutoCodeWithInput(
            @Parameter(description = "规则编码", required = true) @PathVariable("ruleCode") String ruleCode,
            @Parameter(description = "传入字符", required = true) @PathVariable("inputCharacter") String inputCharacter) {
        log.info("REST 请求生成自动编码, ruleCode: {}, inputCharacter: {}", ruleCode, inputCharacter);
        return sysAutoCodeApi.getAutoCode(ruleCode, inputCharacter);
    }
}
