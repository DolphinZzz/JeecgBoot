package org.jeecg.common.system.api;

import org.jeecg.common.api.vo.Result;

/**
 * @Description: 系统自动编码生成 API 接口（本地直接调用）
 * @Author: jeecg-boot
 * @Date: 2026-11-04
 * @Version: V1.0
 */
public interface ISysAutoCodeApi {

    /**
     * 根据规则编码生成编号
     *
     * @param ruleCode 规则编码
     * @return Result<String>
     */
    Result<String> getAutoCode(String ruleCode);

    /**
     * 根据规则编码和传入字符生成编号
     *
     * @param ruleCode 规则编码
     * @param inputCharacter 传入字符
     * @return Result<String>
     */
    Result<String> getAutoCode(String ruleCode, String inputCharacter);
}
