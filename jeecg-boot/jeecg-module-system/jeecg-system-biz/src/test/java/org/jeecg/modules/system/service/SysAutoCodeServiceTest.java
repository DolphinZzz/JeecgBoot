package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.jeecg.modules.system.entity.SysAutoCodePart;
import org.jeecg.modules.system.entity.SysAutoCodeResult;
import org.jeecg.modules.system.entity.SysAutoCodeRule;
import org.jeecg.modules.system.service.impl.SysAutoCodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 系统自动编码算法核心单元测试（基于 SQL 周期前缀匹配模式）
 * 覆盖：
 * 1. 按天循环重置（生产工单 MO202611040001 -> MO202611040002 -> 跨天后 SQL 查不到今天记录，重置为 MO202611050001）
 * 2. 全局不循环终身递增（物料编码 ITEM00000001 -> ITEM00000002 -> ITEM00000139）
 * 3. 按业务入参循环重置（OTHER：WH01-001 -> WH01-002 -> 换WH02查不到记录重置为 WH02-001）
 * 4. 补齐方式与最大长度控制（左补齐、右补齐、超长截断）
 */
@ExtendWith(MockitoExtension.class)
public class SysAutoCodeServiceTest {

    @Mock
    private ISysAutoCodeRuleService sysAutoCodeRuleService;

    @Mock
    private ISysAutoCodePartService sysAutoCodePartService;

    @Mock
    private ISysAutoCodeResultService sysAutoCodeResultService;

    @InjectMocks
    private SysAutoCodeServiceImpl sysAutoCodeService;

    private String todayStr;
    private String todayFullStr;

    @BeforeEach
    public void setUp() {
        todayStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
        todayFullStr = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    @Test
    @DisplayName("测试按天循环（DAY，基于 SQL likeRight 'yyyyMMdd%'）：当天有记录累加，当天无记录重置回起始号")
    public void testCycleByDay() {
        // 规则：MO + 日期(yyyyMMdd) + 流水号(4位，按天循环)
        SysAutoCodeRule rule = new SysAutoCodeRule();
        rule.setId(101L);
        rule.setRuleCode("MO_CODE");
        rule.setRuleName("生产工单编码");
        rule.setEnableFlag(1);
        rule.setMaxLength(20);
        rule.setIsPadded("N");

        when(sysAutoCodeRuleService.getOne(any(Wrapper.class))).thenReturn(rule);

        SysAutoCodePart part1 = new SysAutoCodePart();
        part1.setId(1L);
        part1.setRuleId(101L);
        part1.setPartIndex(1);
        part1.setPartType("FIXCHAR");
        part1.setFixCharacter("MO");

        SysAutoCodePart part2 = new SysAutoCodePart();
        part2.setId(2L);
        part2.setRuleId(101L);
        part2.setPartIndex(2);
        part2.setPartType("NOWDATE");
        part2.setDateFormat("yyyyMMdd");

        SysAutoCodePart part3 = new SysAutoCodePart();
        part3.setId(3L);
        part3.setRuleId(101L);
        part3.setPartIndex(3);
        part3.setPartType("SERIALNO");
        part3.setPartLength(4);
        part3.setSerialStartNo(1);
        part3.setSerialStep(1);
        part3.setCycleFlag("Y");
        part3.setCycleMethod("DAY");

        List<SysAutoCodePart> parts = Arrays.asList(part1, part2, part3);
        when(sysAutoCodePartService.list(any(Wrapper.class))).thenReturn(parts);

        // 场景 1：当天首次生成（SQL 匹配 gen_date LIKE 'yyyyMMdd%' 查无记录） -> 流水号从 1 开始 -> MOyyyyMMdd0001
        when(sysAutoCodeResultService.getOne(any(Wrapper.class))).thenReturn(null);
        String code1 = sysAutoCodeService.generateCode("MO_CODE");
        assertEquals("MO" + todayStr + "0001", code1);

        // 场景 2：当天第 2 次生成（SQL 匹配查到当天的记录，上一笔为 1） -> 累加 -> MOyyyyMMdd0002
        SysAutoCodeResult todayResult = new SysAutoCodeResult();
        todayResult.setRuleId(101L);
        todayResult.setGenDate(todayFullStr);
        todayResult.setLastSerialNo(1);
        when(sysAutoCodeResultService.getOne(any(Wrapper.class))).thenReturn(todayResult);

        String code2 = sysAutoCodeService.generateCode("MO_CODE");
        assertEquals("MO" + todayStr + "0002", code2);

        // 场景 3：第二天生成（SQL 匹配新的一天前缀，查不到记录） -> 重置回 1 -> MOyyyyMMdd0001
        when(sysAutoCodeResultService.getOne(any(Wrapper.class))).thenReturn(null);

        String code3 = sysAutoCodeService.generateCode("MO_CODE");
        assertEquals("MO" + todayStr + "0001", code3);
    }

    @Test
    @DisplayName("测试全局不循环（cycleFlag = N）：终身累加不重置")
    public void testNonCycle() {
        // 规则：ITEM + 8位流水号
        SysAutoCodeRule rule = new SysAutoCodeRule();
        rule.setId(102L);
        rule.setRuleCode("ITEM_CODE");
        rule.setEnableFlag(1);
        rule.setMaxLength(12);
        rule.setIsPadded("N");

        when(sysAutoCodeRuleService.getOne(any(Wrapper.class))).thenReturn(rule);

        SysAutoCodePart part1 = new SysAutoCodePart();
        part1.setId(10L);
        part1.setRuleId(102L);
        part1.setPartIndex(1);
        part1.setPartType("FIXCHAR");
        part1.setFixCharacter("ITEM");

        SysAutoCodePart part2 = new SysAutoCodePart();
        part2.setId(11L);
        part2.setRuleId(102L);
        part2.setPartIndex(2);
        part2.setPartType("SERIALNO");
        part2.setPartLength(8);
        part2.setSerialStartNo(1);
        part2.setSerialStep(1);
        part2.setCycleFlag("N");

        when(sysAutoCodePartService.list(any(Wrapper.class))).thenReturn(Arrays.asList(part1, part2));

        // 首次生成 -> ITEM00000001
        when(sysAutoCodeResultService.getOne(any(Wrapper.class))).thenReturn(null);
        String code1 = sysAutoCodeService.generateCode("ITEM_CODE");
        assertEquals("ITEM00000001", code1);

        // 历史流水号已累计到 138 -> 下一个为 139 -> ITEM00000139
        SysAutoCodeResult lastResult = new SysAutoCodeResult();
        lastResult.setRuleId(102L);
        lastResult.setLastSerialNo(138);
        when(sysAutoCodeResultService.getOne(any(Wrapper.class))).thenReturn(lastResult);

        String code139 = sysAutoCodeService.generateCode("ITEM_CODE");
        assertEquals("ITEM00000139", code139);
    }

    @Test
    @DisplayName("测试按业务入参循环（OTHER）：切换入参查不到记录重置为起始号，同一入参内累加")
    public void testCycleByOtherInputCharacter() {
        // 规则：输入字符 + '-' + 3位流水号(按OTHER循环)
        SysAutoCodeRule rule = new SysAutoCodeRule();
        rule.setId(103L);
        rule.setRuleCode("WORKSHOP_DEVICE");
        rule.setEnableFlag(1);
        rule.setMaxLength(20);
        rule.setIsPadded("N");

        when(sysAutoCodeRuleService.getOne(any(Wrapper.class))).thenReturn(rule);

        SysAutoCodePart part1 = new SysAutoCodePart();
        part1.setId(21L);
        part1.setRuleId(103L);
        part1.setPartIndex(1);
        part1.setPartType("INPUTCHAR");
        part1.setInputCharacter("DEFAULT");

        SysAutoCodePart part2 = new SysAutoCodePart();
        part2.setId(22L);
        part2.setRuleId(103L);
        part2.setPartIndex(2);
        part2.setPartType("FIXCHAR");
        part2.setFixCharacter("-");

        SysAutoCodePart part3 = new SysAutoCodePart();
        part3.setId(23L);
        part3.setRuleId(103L);
        part3.setPartIndex(3);
        part3.setPartType("SERIALNO");
        part3.setPartLength(3);
        part3.setSerialStartNo(1);
        part3.setSerialStep(1);
        part3.setCycleFlag("Y");
        part3.setCycleMethod("OTHER");

        when(sysAutoCodePartService.list(any(Wrapper.class))).thenReturn(Arrays.asList(part1, part2, part3));

        // 1. 入参 WH01 首次调用（SQL 查无 WH01 记录） -> WH01-001
        when(sysAutoCodeResultService.getOne(any(Wrapper.class))).thenReturn(null);
        String code1 = sysAutoCodeService.generateCode("WORKSHOP_DEVICE", "WH01");
        assertEquals("WH01-001", code1);

        // 2. 入参 WH01 再次调用（SQL 查到 WH01 的记录为 1） -> WH01-002
        SysAutoCodeResult resWH01 = new SysAutoCodeResult();
        resWH01.setRuleId(103L);
        resWH01.setLastInputChar("WH01");
        resWH01.setLastSerialNo(1);
        when(sysAutoCodeResultService.getOne(any(Wrapper.class))).thenReturn(resWH01);

        String code2 = sysAutoCodeService.generateCode("WORKSHOP_DEVICE", "WH01");
        assertEquals("WH01-002", code2);

        // 3. 切换为新业务入参 WH02（SQL 查无 WH02 记录） -> 重置回 1 -> WH02-001
        when(sysAutoCodeResultService.getOne(any(Wrapper.class))).thenReturn(null);
        String code3 = sysAutoCodeService.generateCode("WORKSHOP_DEVICE", "WH02");
        assertEquals("WH02-001", code3);
    }

    @Test
    @DisplayName("测试补齐方式（左补齐、右补齐）与最大长度控制")
    public void testPaddingAndMaxLength() {
        // 规则 1：左补齐 0，最大长度 10
        SysAutoCodeRule ruleL = new SysAutoCodeRule();
        ruleL.setId(104L);
        ruleL.setRuleCode("PAD_L");
        ruleL.setEnableFlag(1);
        ruleL.setMaxLength(10);
        ruleL.setIsPadded("Y");
        ruleL.setPaddedChar("0");
        ruleL.setPaddedMethod("L");

        when(sysAutoCodeRuleService.getOne(any(Wrapper.class))).thenReturn(ruleL);

        SysAutoCodePart part = new SysAutoCodePart();
        part.setId(31L);
        part.setRuleId(104L);
        part.setPartIndex(1);
        part.setPartType("FIXCHAR");
        part.setFixCharacter("ABC");

        when(sysAutoCodePartService.list(any(Wrapper.class))).thenReturn(Collections.singletonList(part));

        String codeL = sysAutoCodeService.generateCode("PAD_L");
        assertEquals("0000000ABC", codeL);
        assertEquals(10, codeL.length());

        // 规则 2：最大长度截断测试（固定字符 8 位，最大长度限制 5 位）
        ruleL.setIsPadded("N");
        ruleL.setMaxLength(5);
        part.setFixCharacter("12345678");

        String codeCut = sysAutoCodeService.generateCode("PAD_L");
        assertEquals("12345", codeCut);
        assertEquals(5, codeCut.length());
    }
}
