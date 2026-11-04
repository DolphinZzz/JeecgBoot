package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.system.entity.SysAutoCodeRule;
import org.jeecg.modules.system.service.ISysAutoCodeRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;

/**
 * @Description: 系统编码规则表 Controller
 * @Author: jeecg-boot
 * @Date: 2026-11-04
 * @Version: V1.0
 */
@Tag(name = "系统编码规则")
@RestController
@RequestMapping("/sys/autoCodeRule")
@Slf4j
public class SysAutoCodeRuleController extends JeecgController<SysAutoCodeRule, ISysAutoCodeRuleService> {

    @Autowired
    private ISysAutoCodeRuleService sysAutoCodeRuleService;

    /**
     * 分页列表查询
     *
     * @param sysAutoCodeRule
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @AutoLog(value = "系统编码规则-分页列表查询")
    @Operation(summary = "系统编码规则-分页列表查询")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(SysAutoCodeRule sysAutoCodeRule,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<SysAutoCodeRule> queryWrapper = QueryGenerator.initQueryWrapper(sysAutoCodeRule, req.getParameterMap());
        Page<SysAutoCodeRule> page = new Page<>(pageNo, pageSize);
        IPage<SysAutoCodeRule> pageList = sysAutoCodeRuleService.page(page, queryWrapper);
        return Result.ok(pageList);
    }

    /**
     * 添加
     *
     * @param sysAutoCodeRule
     * @return
     */
    @AutoLog(value = "系统编码规则-添加")
    @Operation(summary = "系统编码规则-添加")
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody SysAutoCodeRule sysAutoCodeRule) {
        sysAutoCodeRuleService.save(sysAutoCodeRule);
        return Result.ok("添加成功！");
    }

    /**
     * 编辑
     *
     * @param sysAutoCodeRule
     * @return
     */
    @AutoLog(value = "系统编码规则-编辑")
    @Operation(summary = "系统编码规则-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<?> edit(@RequestBody SysAutoCodeRule sysAutoCodeRule) {
        sysAutoCodeRuleService.updateById(sysAutoCodeRule);
        return Result.ok("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "系统编码规则-通过id删除")
    @Operation(summary = "系统编码规则-通过id删除")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id", required = true) String id) {
        sysAutoCodeRuleService.removeById(id);
        return Result.ok("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "系统编码规则-批量删除")
    @Operation(summary = "系统编码规则-批量删除")
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        sysAutoCodeRuleService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.ok("批量删除成功！");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @AutoLog(value = "系统编码规则-通过id查询")
    @Operation(summary = "系统编码规则-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id", required = true) String id) {
        SysAutoCodeRule sysAutoCodeRule = sysAutoCodeRuleService.getById(id);
        return Result.ok(sysAutoCodeRule);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param sysAutoCodeRule
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, SysAutoCodeRule sysAutoCodeRule) {
        return super.exportXls(request, sysAutoCodeRule, SysAutoCodeRule.class, "系统编码规则");
    }

    /**
     * 通过excel导入数据
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, SysAutoCodeRule.class);
    }
}
