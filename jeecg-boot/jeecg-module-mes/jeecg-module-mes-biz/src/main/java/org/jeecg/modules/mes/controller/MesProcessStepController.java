package org.jeecg.modules.mes.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.mes.entity.MesProcessStep;
import org.jeecg.modules.mes.entity.MesProcessStepOperation;
import org.jeecg.modules.mes.service.IMesProcessStepOperationService;
import org.jeecg.modules.mes.service.IMesProcessStepService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description: 工序设置（生产工序主表及操作步骤明细表）
 * @Author: jeecg-boot
 * @Date: 2026-11-05
 * @Version: V1.0
 */
@Tag(name = "MES-工序设置")
@RestController
@RequestMapping("/mes/mesProcessStep")
@Slf4j
public class MesProcessStepController extends JeecgController<MesProcessStep, IMesProcessStepService> {

    @Autowired
    private IMesProcessStepService mesProcessStepService;

    @Autowired
    private IMesProcessStepOperationService mesProcessStepOperationService;

    /**
     * 分页列表查询
     */
    @Operation(summary = "生产工序-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<MesProcessStep>> queryPageList(MesProcessStep mesProcessStep,
                                                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                       HttpServletRequest req) {
        QueryWrapper<MesProcessStep> queryWrapper = QueryGenerator.initQueryWrapper(mesProcessStep, req.getParameterMap());
        queryWrapper.orderByAsc("sort_order").orderByDesc("create_time");
        Page<MesProcessStep> page = new Page<>(pageNo, pageSize);
        IPage<MesProcessStep> pageList = mesProcessStepService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 查询所有启用的工序列表（供工艺组成下拉选择）
     */
    @Operation(summary = "生产工序-查询全部启用工序")
    @GetMapping(value = "/listEnabled")
    public Result<List<MesProcessStep>> listEnabled() {
        return Result.OK(mesProcessStepService.listEnabledSteps());
    }

    /**
     * 添加生产工序（返回保存后的实体，支持前端模态框原地展开操作步骤Tab）
     */
    @AutoLog(value = "生产工序-添加")
    @Operation(summary = "生产工序-添加")
    @RequiresPermissions("mes:mes_process_step:add")
    @PostMapping(value = "/add")
    public Result<MesProcessStep> add(@RequestBody MesProcessStep mesProcessStep) {
        if (mesProcessStep.getProcessCode() == null || mesProcessStep.getProcessCode().trim().isEmpty()) {
            return Result.error("工序编码不能为空！");
        }
        if (mesProcessStep.getProcessName() == null || mesProcessStep.getProcessName().trim().isEmpty()) {
            return Result.error("工序名称不能为空！");
        }
        LambdaQueryWrapper<MesProcessStep> dupWrapper = new LambdaQueryWrapper<>();
        dupWrapper.eq(MesProcessStep::getProcessCode, mesProcessStep.getProcessCode().trim());
        if (mesProcessStepService.count(dupWrapper) > 0) {
            return Result.error("工序编码已存在！");
        }
        if (mesProcessStep.getIsEnabled() == null) {
            mesProcessStep.setIsEnabled(1);
        }
        if (mesProcessStep.getSortOrder() == null) {
            mesProcessStep.setSortOrder(1);
        }
        if (mesProcessStep.getCreateTime() == null) {
            mesProcessStep.setCreateTime(new Date());
        }
        if (mesProcessStep.getIsDel() == null) {
            mesProcessStep.setIsDel(1);
        }
        mesProcessStepService.save(mesProcessStep);
        return Result.OK("添加成功！", mesProcessStep);
    }

    /**
     * 编辑生产工序
     */
    @AutoLog(value = "生产工序-编辑")
    @Operation(summary = "生产工序-编辑")
    @RequiresPermissions("mes:mes_process_step:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<MesProcessStep> edit(@RequestBody MesProcessStep mesProcessStep) {
        if (mesProcessStep.getId() == null) {
            return Result.error("主键ID不能为空！");
        }
        if (mesProcessStep.getProcessCode() != null && !mesProcessStep.getProcessCode().trim().isEmpty()) {
            LambdaQueryWrapper<MesProcessStep> dupWrapper = new LambdaQueryWrapper<>();
            dupWrapper.eq(MesProcessStep::getProcessCode, mesProcessStep.getProcessCode().trim())
                    .ne(MesProcessStep::getId, mesProcessStep.getId());
            if (mesProcessStepService.count(dupWrapper) > 0) {
                return Result.error("工序编码已存在！");
            }
        }
        mesProcessStep.setUpdateTime(new Date());
        mesProcessStepService.updateById(mesProcessStep);
        return Result.OK("编辑成功!", mesProcessStep);
    }

    /**
     * 通过id删除生产工序（同步逻辑删除关联操作步骤）
     */
    @AutoLog(value = "生产工序-通过id删除")
    @Operation(summary = "生产工序-通过id删除")
    @RequiresPermissions("mes:mes_process_step:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) Long id) {
        mesProcessStepService.removeById(id);
        LambdaQueryWrapper<MesProcessStepOperation> opWrapper = new LambdaQueryWrapper<>();
        opWrapper.eq(MesProcessStepOperation::getProcessStepId, id);
        mesProcessStepOperationService.remove(opWrapper);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除生产工序
     */
    @AutoLog(value = "生产工序-批量删除")
    @Operation(summary = "生产工序-批量删除")
    @RequiresPermissions("mes:mes_process_step:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .filter(s -> !s.trim().isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
        mesProcessStepService.removeByIds(idList);
        if (!idList.isEmpty()) {
            LambdaQueryWrapper<MesProcessStepOperation> opWrapper = new LambdaQueryWrapper<>();
            opWrapper.in(MesProcessStepOperation::getProcessStepId, idList);
            mesProcessStepOperationService.remove(opWrapper);
        }
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询生产工序
     */
    @Operation(summary = "生产工序-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<MesProcessStep> queryById(@RequestParam(name = "id", required = true) Long id) {
        MesProcessStep mesProcessStep = mesProcessStepService.getById(id);
        if (mesProcessStep == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(mesProcessStep);
    }

    // ==================== 子表：工序操作步骤明细 (mes_process_step_operation) ====================

    /**
     * 查询某工序下的操作步骤列表
     */
    @Operation(summary = "工序操作步骤-列表查询")
    @GetMapping(value = "/operation/list")
    public Result<List<MesProcessStepOperation>> listOperations(@RequestParam(name = "processStepId", required = true) Long processStepId) {
        List<MesProcessStepOperation> list = mesProcessStepOperationService.listByProcessStepId(processStepId);
        return Result.OK(list);
    }

    /**
     * 添加工序操作步骤
     */
    @AutoLog(value = "工序操作步骤-添加")
    @Operation(summary = "工序操作步骤-添加")
    @RequiresPermissions("mes:mes_process_step:edit")
    @PostMapping(value = "/operation/add")
    public Result<String> addOperation(@RequestBody MesProcessStepOperation operation) {
        if (operation.getProcessStepId() == null) {
            return Result.error("所属工序ID不能为空！");
        }
        if (operation.getStepDescription() == null || operation.getStepDescription().trim().isEmpty()) {
            return Result.error("步骤说明不能为空！");
        }
        if (operation.getStepOrder() == null) {
            operation.setStepOrder(1);
        }
        if (operation.getCreateTime() == null) {
            operation.setCreateTime(new Date());
        }
        if (operation.getIsDel() == null) {
            operation.setIsDel(1);
        }
        mesProcessStepOperationService.save(operation);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑工序操作步骤
     */
    @AutoLog(value = "工序操作步骤-编辑")
    @Operation(summary = "工序操作步骤-编辑")
    @RequiresPermissions("mes:mes_process_step:edit")
    @RequestMapping(value = "/operation/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> editOperation(@RequestBody MesProcessStepOperation operation) {
        if (operation.getId() == null) {
            return Result.error("主键ID不能为空！");
        }
        operation.setUpdateTime(new Date());
        mesProcessStepOperationService.updateById(operation);
        return Result.OK("修改成功！");
    }

    /**
     * 删除工序操作步骤
     */
    @AutoLog(value = "工序操作步骤-删除")
    @Operation(summary = "工序操作步骤-删除")
    @RequiresPermissions("mes:mes_process_step:edit")
    @DeleteMapping(value = "/operation/delete")
    public Result<String> deleteOperation(@RequestParam(name = "id", required = true) Long id) {
        mesProcessStepOperationService.removeById(id);
        return Result.OK("删除成功！");
    }

    /**
     * 批量删除工序操作步骤
     */
    @AutoLog(value = "工序操作步骤-批量删除")
    @Operation(summary = "工序操作步骤-批量删除")
    @RequiresPermissions("mes:mes_process_step:edit")
    @DeleteMapping(value = "/operation/deleteBatch")
    public Result<String> deleteBatchOperation(@RequestParam(name = "ids", required = true) String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .filter(s -> !s.trim().isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
        mesProcessStepOperationService.removeByIds(idList);
        return Result.OK("批量删除成功！");
    }

    /**
     * 导出excel
     */
    @RequiresPermissions("mes:mes_process_step:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, MesProcessStep mesProcessStep) {
        return super.exportXls(request, mesProcessStep, MesProcessStep.class, "生产工序");
    }

    /**
     * 通过excel导入数据
     */
    @RequiresPermissions("mes:mes_process_step:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, MesProcessStep.class);
    }
}
