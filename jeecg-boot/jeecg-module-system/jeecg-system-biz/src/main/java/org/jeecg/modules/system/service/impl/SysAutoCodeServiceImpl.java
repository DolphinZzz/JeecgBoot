package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.boot.starter.lock.annotation.JLock;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.system.api.ISysAutoCodeApi;
import org.jeecg.modules.system.entity.SysAutoCodePart;
import org.jeecg.modules.system.entity.SysAutoCodeResult;
import org.jeecg.modules.system.entity.SysAutoCodeRule;
import org.jeecg.modules.system.service.ISysAutoCodePartService;
import org.jeecg.modules.system.service.ISysAutoCodeResultService;
import org.jeecg.modules.system.service.ISysAutoCodeRuleService;
import org.jeecg.modules.system.service.ISysAutoCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @Description: 系统自动编码生成核心服务实现类
 * 实现了 ISysAutoCodeService（核心服务）与 ISysAutoCodeApi（本地/微服务统一接口）
 * 采用 SQL 周期前缀匹配机制（gen_date LIKE '前缀%'）实现高效直观的周期重置与递增判定
 * @Author: jeecg-boot
 * @Date: 2026-11-04
 * @Version: V1.0
 */
@Slf4j
@Service("sysAutoCodeApi")
public class SysAutoCodeServiceImpl implements ISysAutoCodeService, ISysAutoCodeApi {

    /**
     * 生成结果记录中统一存储 14 位完整紧凑时间（yyyyMMddHHmmss）
     * 天然支持按年(4位)、按月(6位)、按日(8位)、按时(10位)、按分(12位)的前缀模糊查询匹配
     */
    public static final String COMPACT_DATETIME_PATTERN = "yyyyMMddHHmmss";

    @Autowired
    private ISysAutoCodeRuleService sysAutoCodeRuleService;

    @Autowired
    private ISysAutoCodePartService sysAutoCodePartService;

    @Autowired
    private ISysAutoCodeResultService sysAutoCodeResultService;

    @Override
    public Result<String> getAutoCode(String ruleCode) {
        return getAutoCode(ruleCode, null);
    }

    @Override
    public Result<String> getAutoCode(String ruleCode, String inputCharacter) {
        try {
            String code = generateCode(ruleCode, inputCharacter);
            return Result.ok(code);
        } catch (Exception e) {
            log.error("生成编码异常, ruleCode: {}, inputCharacter: {}, error: {}", ruleCode, inputCharacter, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @Override
    public String generateCode(String ruleCode) {
        return generateCode(ruleCode, null);
    }

    @Override
    @JLock(lockKey = {"'auto_code:' + #ruleCode"}, waitTime = 5000, expireSeconds = 10000)
    @Transactional(rollbackFor = Exception.class)
    public String generateCode(String ruleCode, String inputCharacter) {
        if (StringUtils.isBlank(ruleCode)) {
            throw new JeecgBootBizTipException("规则编码不能为空！");
        }

        // 1. 获取启用的规则配置
        LambdaQueryWrapper<SysAutoCodeRule> ruleWrapper = new LambdaQueryWrapper<>();
        ruleWrapper.eq(SysAutoCodeRule::getRuleCode, ruleCode)
                   .eq(SysAutoCodeRule::getEnableFlag, 1);
        SysAutoCodeRule rule = sysAutoCodeRuleService.getOne(ruleWrapper);
        if (rule == null) {
            throw new JeecgBootBizTipException("未找到启用的编码规则：" + ruleCode);
        }

        // 2. 获取规则分段组成列表（根据分段序号升序排序）
        LambdaQueryWrapper<SysAutoCodePart> partWrapper = new LambdaQueryWrapper<>();
        partWrapper.eq(SysAutoCodePart::getRuleId, rule.getId())
                   .orderByAsc(SysAutoCodePart::getPartIndex);
        List<SysAutoCodePart> parts = sysAutoCodePartService.list(partWrapper);
        if (parts == null || parts.isEmpty()) {
            throw new JeecgBootBizTipException("编码规则 [" + ruleCode + "] 下未配置任何规则组成！");
        }

        Date nowDate = new Date();
        StringBuilder codeBuilder = new StringBuilder();
        Integer lastSerialNoRecorded = null;
        Integer genIndex = 1;

        // 3. 逐个分段计算并拼接编码
        for (SysAutoCodePart part : parts) {
            String pType = StringUtils.trimToEmpty(part.getPartType()).toUpperCase();
            Integer partLength = part.getPartLength();

            switch (pType) {
                case "FIXCHAR":
                    // 固定字符：使用数据库字段 fix_character
                    String fixChar = StringUtils.defaultString(part.getFixCharacter(), "");
                    codeBuilder.append(controlLength(fixChar, partLength, false));
                    break;

                case "INPUTCHAR":
                    // 输入字符：优先使用入参，其次使用数据库字段 input_character
                    String inChar = StringUtils.isNotBlank(inputCharacter) ? inputCharacter : part.getInputCharacter();
                    inChar = StringUtils.defaultString(inChar, "");
                    codeBuilder.append(controlLength(inChar, partLength, false));
                    break;

                case "NOWDATE":
                    // 当前日期时间：日期时间格式使用数据库字段 date_format
                    String format = StringUtils.isNotBlank(part.getDateFormat()) ? part.getDateFormat() : "yyyyMMdd";
                    String dateStr;
                    try {
                        dateStr = new SimpleDateFormat(format).format(nowDate);
                    } catch (Exception e) {
                        log.warn("日期格式 [{}] 非法，降级使用 yyyyMMdd", format);
                        dateStr = new SimpleDateFormat("yyyyMMdd").format(nowDate);
                    }
                    codeBuilder.append(controlLength(dateStr, partLength, false));
                    break;

                case "SERIALNO":
                    // 流水号：根据 SQL 周期前缀筛选判断重置或累加
                    int serialNo = calculateNextSerialNo(rule, part, inputCharacter, nowDate);
                    lastSerialNoRecorded = serialNo;

                    // 同步更新规则组成表的当前流水号 serial_now_no
                    part.setSerialNowNo(serialNo);
                    sysAutoCodePartService.updateById(part);

                    // 按照分段长度控制流水号（不足左侧补0）
                    String serialStr = controlLength(String.valueOf(serialNo), partLength, true);
                    codeBuilder.append(serialStr);
                    break;

                default:
                    log.warn("未知的规则组成类型: [{}]，忽略该分段", pType);
                    break;
            }
        }

        String finalCode = codeBuilder.toString();

        // 4. 是否补齐处理
        if ("Y".equalsIgnoreCase(rule.getIsPadded()) && rule.getMaxLength() != null && rule.getMaxLength() > 0) {
            String padCharStr = StringUtils.isNotEmpty(rule.getPaddedChar()) ? rule.getPaddedChar() : "0";
            char padChar = padCharStr.charAt(0);
            if ("L".equalsIgnoreCase(rule.getPaddedMethod())) {
                finalCode = StringUtils.leftPad(finalCode, rule.getMaxLength(), padChar);
            } else if ("R".equalsIgnoreCase(rule.getPaddedMethod())) {
                finalCode = StringUtils.rightPad(finalCode, rule.getMaxLength(), padChar);
            }
        }

        // 5. 编码规则全局最大长度控制：最多不能超过编码规则下最大长度
        if (rule.getMaxLength() != null && rule.getMaxLength() > 0 && finalCode.length() > rule.getMaxLength()) {
            finalCode = finalCode.substring(0, rule.getMaxLength());
        }

        // 6. 持久化生成记录到 sys_auto_code_result
        try {
            SysAutoCodeResult result = new SysAutoCodeResult();
            result.setRuleId(rule.getId());
            // 结果表存储 14 位紧凑格式（yyyyMMddHHmmss）
            result.setGenDate(new SimpleDateFormat(COMPACT_DATETIME_PATTERN).format(nowDate));
            result.setLastResult(finalCode);
            result.setLastSerialNo(lastSerialNoRecorded);
            result.setLastInputChar(inputCharacter);
            result.setGenIndex(genIndex);
            sysAutoCodeResultService.save(result);
        } catch (Exception e) {
            log.error("保存编码生成结果记录异常: {}", e.getMessage(), e);
        }

        log.info("自动编码生成完成 - 规则编码: [{}], 传入字符: [{}], 生成结果: [{}]", ruleCode, inputCharacter, finalCode);
        return finalCode;
    }

    /**
     * 计算下一个流水号（采用 SQL 周期前缀匹配模式）
     * 1. 不循环（cycleFlag = 'N'）：全局递增，永久不重置
     * 2. 循环（cycleFlag = 'Y'）：
     *    - OTHER（按业务入参）：以 inputCharacter 为条件匹配
     *    - YEAR / MONTH / DAY / HOUR / MINUTE：按 14 位时间格式的前缀匹配当前周期记录
     *      有记录 -> 老周期，累加步长；
     *      无记录 -> 新周期，重置为起始号。
     *
     * @param rule 编码规则
     * @param part 规则组成（流水号分段）
     * @param inputCharacter 传入字符
     * @param nowDate 当前时间
     * @return 当前计算出的流水号数字
     */
    private int calculateNextSerialNo(SysAutoCodeRule rule, SysAutoCodePart part, String inputCharacter, Date nowDate) {
        int startNo = (part.getSerialStartNo() != null && part.getSerialStartNo() > 0) ? part.getSerialStartNo() : 1;
        int step = (part.getSerialStep() != null && part.getSerialStep() > 0) ? part.getSerialStep() : 1;
        String cycleFlag = StringUtils.trimToEmpty(part.getCycleFlag()).toUpperCase();
        String cycleMethod = StringUtils.trimToEmpty(part.getCycleMethod()).toUpperCase();

        // 1. 不循环模式（cycleFlag != 'Y'）：全局终身递增，永久不重置
        if (!"Y".equals(cycleFlag)) {
            LambdaQueryWrapper<SysAutoCodeResult> query = new LambdaQueryWrapper<>();
            query.eq(SysAutoCodeResult::getRuleId, rule.getId())
                 .isNotNull(SysAutoCodeResult::getLastSerialNo)
                 .orderByDesc(SysAutoCodeResult::getId)
                 .last("LIMIT 1");
            SysAutoCodeResult last = sysAutoCodeResultService.getOne(query);
            return (last != null && last.getLastSerialNo() != null) ? last.getLastSerialNo() + step : startNo;
        }

        // 2. 循环模式：按业务传入字符循环（cycle_method = 'OTHER'）
        if ("OTHER".equals(cycleMethod)) {
            LambdaQueryWrapper<SysAutoCodeResult> query = new LambdaQueryWrapper<>();
            query.eq(SysAutoCodeResult::getRuleId, rule.getId());
            if (StringUtils.isNotBlank(inputCharacter)) {
                query.eq(SysAutoCodeResult::getLastInputChar, inputCharacter);
            } else {
                query.and(w -> w.isNull(SysAutoCodeResult::getLastInputChar).or().eq(SysAutoCodeResult::getLastInputChar, ""));
            }
            query.isNotNull(SysAutoCodeResult::getLastSerialNo)
                 .orderByDesc(SysAutoCodeResult::getId)
                 .last("LIMIT 1");
            SysAutoCodeResult last = sysAutoCodeResultService.getOne(query);
            // 查到该入参的历史记录 -> 同一业务入参周期，累加；查不到 -> 新入参周期，重置为起始号
            return (last != null && last.getLastSerialNo() != null) ? last.getLastSerialNo() + step : startNo;
        }

        // 3. 循环模式：按时间维度前缀模糊匹配（YEAR, MONTH, DAY, HOUR, MINUTE, WEEK）
        String datePrefix;
        switch (cycleMethod) {
            case "YEAR":
                datePrefix = new SimpleDateFormat("yyyy").format(nowDate);
                break;
            case "MONTH":
                datePrefix = new SimpleDateFormat("yyyyMM").format(nowDate);
                break;
            case "DAY":
                datePrefix = new SimpleDateFormat("yyyyMMdd").format(nowDate);
                break;
            case "HOUR":
                datePrefix = new SimpleDateFormat("yyyyMMddHH").format(nowDate);
                break;
            case "MINUTE":
                datePrefix = new SimpleDateFormat("yyyyMMddHHmm").format(nowDate);
                break;
            case "WEEK_CHINA":
            case "WEEK_USA":
                // 周循环：按年+周数作为前缀
                Calendar cal = Calendar.getInstance();
                cal.setTime(nowDate);
                if ("WEEK_CHINA".equals(cycleMethod)) {
                    cal.setFirstDayOfWeek(Calendar.MONDAY);
                    cal.setMinimalDaysInFirstWeek(4);
                } else {
                    cal.setFirstDayOfWeek(Calendar.SUNDAY);
                    cal.setMinimalDaysInFirstWeek(1);
                }
                datePrefix = cal.getWeekYear() + String.format("_W%02d", cal.get(Calendar.WEEK_OF_YEAR));
                break;
            default:
                datePrefix = new SimpleDateFormat("yyyyMMdd").format(nowDate);
                break;
        }

        // 核心 SQL：WHERE rule_id = ? AND gen_date LIKE concat(#{datePrefix}, '%') ORDER BY id DESC LIMIT 1
        LambdaQueryWrapper<SysAutoCodeResult> query = new LambdaQueryWrapper<>();
        query.eq(SysAutoCodeResult::getRuleId, rule.getId())
             .likeRight(SysAutoCodeResult::getGenDate, datePrefix)
             .isNotNull(SysAutoCodeResult::getLastSerialNo)
             .orderByDesc(SysAutoCodeResult::getId)
             .last("LIMIT 1");
        SysAutoCodeResult last = sysAutoCodeResultService.getOne(query);

        if (last != null && last.getLastSerialNo() != null) {
            // 【老周期，继续递增】：数据库中已查到该周期的生成记录
            return last.getLastSerialNo() + step;
        } else {
            // 【新周期，重置归零】：数据库中查不到该周期的记录（跨天/跨月/跨年了）
            log.info("规则 [{}] 流水号进入新周期前缀 [{}]，流水号重置为起始号 [{}]", rule.getRuleCode(), datePrefix, startNo);
            return startNo;
        }
    }

    /**
     * 根据分段长度控制组成编码
     *
     * @param rawValue 原始字符串
     * @param partLength 分段长度
     * @param isSerial 是否为流水号
     * @return 格式化后的分段字符串
     */
    private String controlLength(String rawValue, Integer partLength, boolean isSerial) {
        if (rawValue == null) {
            rawValue = "";
        }
        if (partLength == null || partLength <= 0) {
            return rawValue;
        }

        if (isSerial) {
            // 流水号：如果长度不足，左侧以 '0' 补齐
            if (rawValue.length() < partLength) {
                return StringUtils.leftPad(rawValue, partLength, '0');
            } else if (rawValue.length() > partLength) {
                // 超出分段长度时截断
                return rawValue.substring(rawValue.length() - partLength);
            }
            return rawValue;
        } else {
            // 普通字符（固定字符/输入字符/日期）：超出分段长度截断
            if (rawValue.length() > partLength) {
                return rawValue.substring(0, partLength);
            }
            return rawValue;
        }
    }
}
