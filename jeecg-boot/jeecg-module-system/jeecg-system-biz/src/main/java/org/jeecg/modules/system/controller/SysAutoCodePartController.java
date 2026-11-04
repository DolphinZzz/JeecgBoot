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
import org.jeecg.modules.system.entity.SysAutoCodePart;
import org.jeecg.modules.system.service.ISysAutoCodePartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;

/**
 * @Description: 系统编码规则组成表 Controller
 * @Author: jeecg-boot
 * @Date: 2026-11-04
 * @Version: V1.0
 */
@Tag(name = "系统编码规则组成")
@RestController
@RequestMapping("/sys/autoCodePart")
@Slf4j
public class SysAutoCodePartController extends JeecgController<SysAutoCodePart, ISysAutoCodePartService> {

    @Autowired
    private ISysAutoCodePartService sysAutoCodePartService;

    /**
     * 分页列表查询
     *
     * @param sysAutoCodePart
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @AutoLog(value = "系统编码规则组成-分页列表查询")
    @Operation(summary = "系统编码规则组成-分页列表查询")
    @GetMapping(value = "/list")
    public Result<?> queryPageList(SysAutoCodePart sysAutoCodePart,
                                   @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                   HttpServletRequest req) {
        QueryWrapper<SysAutoCodePart> queryWrapper = QueryGenerator.initQueryWrapper(sysAutoCodePart, req.getParameterMap());
        // 默认按分段序号升序排序
        queryWrapper.orderByAsc("part_index");
        Page<SysAutoCodePart> page = new Page<>(pageNo, pageSize);
        IPage<SysAutoCodePart> pageList = sysAutoCodePartService.page(page, queryWrapper);
        return Result.ok(pageList);
    }

    /**
     * 添加
     *
     * @param sysAutoCodePart
     * @return
     */
    @AutoLog(value = "系统编码规则组成-添加")
    @Operation(summary = "系统编码规则组成-添加")
    @PostMapping(value = "/add")
    public Result<?> add(@RequestBody SysAutoCodePart sysAutoCodePart) {
        if ("SERIALNO".equalsIgnoreCase(sysAutoCodePart.getPartType())) {
            if (sysAutoCodePart.getSerialNowNo() == null && sysAutoCodePart.getSerialStartNo() != null) {
                sysAutoCodePart.setSerialNowNo(sysAutoCodePart.getSerialStartNo());
            }
        }
        sysAutoCodePartService.save(sysAutoCodePart);
        return Result.ok("添加成功！");
    }

    /**
     * 编辑
     *
     * @param sysAutoCodePart
     * @return
     */
    @AutoLog(value = "系统编码规则组成-编辑")
    @Operation(summary = "系统编码规则组成-编辑")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<?> edit(@RequestBody SysAutoCodePart sysAutoCodePart) {
        sysAutoCodePartService.updateById(sysAutoCodePart);
        return Result.ok("编辑成功!");
    }

    /**
     * 通过id删除
     *
     * @param id
     * @return
     */
    @AutoLog(value = "系统编码规则组成-通过id删除")
    @Operation(summary = "系统编码规则组成-通过id删除")
    @DeleteMapping(value = "/delete")
    public Result<?> delete(@RequestParam(name = "id", required = true) String id) {
        sysAutoCodePartService.removeById(id);
        return Result.ok("删除成功!");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @AutoLog(value = "系统编码规则组成-批量删除")
    @Operation(summary = "系统编码规则组成-批量删除")
    @DeleteMapping(value = "/deleteBatch")
    public Result<?> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        sysAutoCodePartService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.ok("批量删除成功！");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    @AutoLog(value = "系统编码规则组成-通过id查询")
    @Operation(summary = "系统编码规则组成-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<?> queryById(@RequestParam(name = "id", required = true) String id) {
        SysAutoCodePart sysAutoCodePart = sysAutoCodePartService.getById(id);
        return Result.ok(sysAutoCodePart);
    }

    /**
     * 导出excel
     *
     * @param request
     * @param sysAutoCodePart
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, SysAutoCodePart sysAutoCodePart) {
        return super.exportXls(request, sysAutoCodePart, SysAutoCodePart.class, "系统编码规则组成");
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
        return super.importExcel(request, response, SysAutoCodePart.class);
    }
}
