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
import org.jeecg.modules.mes.entity.MesProcessRoute;
import org.jeecg.modules.mes.entity.MesProcessRouteComposition;
import org.jeecg.modules.mes.service.IMesProcessRouteCompositionService;
import org.jeecg.modules.mes.service.IMesProcessRouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description: 工艺流程（工艺路线表及组成工序明细）
 * @Author: jeecg-boot
 * @Date: 2026-11-05
 * @Version: V1.0
 */
@Tag(name = "MES-工艺流程")
@RestController
@RequestMapping("/mes/mesProcessRoute")
@Slf4j
public class MesProcessRouteController extends JeecgController<MesProcessRoute, IMesProcessRouteService> {

    @Autowired
    private IMesProcessRouteService mesProcessRouteService;

    @Autowired
    private IMesProcessRouteCompositionService mesProcessRouteCompositionService;

    /**
     * 分页列表查询
     */
    @Operation(summary = "工艺路线-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<MesProcessRoute>> queryPageList(MesProcessRoute mesProcessRoute,
                                                        @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                        @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                        HttpServletRequest req) {
        QueryWrapper<MesProcessRoute> queryWrapper = QueryGenerator.initQueryWrapper(mesProcessRoute, req.getParameterMap());
        queryWrapper.orderByDesc("create_time");
        Page<MesProcessRoute> page = new Page<>(pageNo, pageSize);
        IPage<MesProcessRoute> pageList = mesProcessRouteService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加工艺路线（返回保存后的实体及主键id，供前端原地展开【组成工序】与【关联产品】Tab）
     */
    @AutoLog(value = "工艺路线-添加")
    @Operation(summary = "工艺路线-添加")
    @RequiresPermissions("mes:mes_process_route:add")
    @PostMapping(value = "/add")
    public Result<MesProcessRoute> add(@RequestBody MesProcessRoute mesProcessRoute) {
        if (mesProcessRoute.getRouteCode() == null || mesProcessRoute.getRouteCode().trim().isEmpty()) {
            return Result.error("工艺路线编号不能为空！");
        }
        if (mesProcessRoute.getRouteName() == null || mesProcessRoute.getRouteName().trim().isEmpty()) {
            return Result.error("工艺路线名称不能为空！");
        }
        LambdaQueryWrapper<MesProcessRoute> dupWrapper = new LambdaQueryWrapper<>();
        dupWrapper.eq(MesProcessRoute::getRouteCode, mesProcessRoute.getRouteCode().trim());
        if (mesProcessRouteService.count(dupWrapper) > 0) {
            return Result.error("工艺路线编号已存在！");
        }
        if (mesProcessRoute.getIsEnabled() == null) {
            mesProcessRoute.setIsEnabled(1);
        }
        if (mesProcessRoute.getVersion() == null || mesProcessRoute.getVersion().trim().isEmpty()) {
            mesProcessRoute.setVersion("v1.0");
        }
        if (mesProcessRoute.getCreateTime() == null) {
            mesProcessRoute.setCreateTime(new Date());
        }
        if (mesProcessRoute.getIsDel() == null) {
            mesProcessRoute.setIsDel(1);
        }
        mesProcessRouteService.save(mesProcessRoute);
        return Result.OK("添加成功！", mesProcessRoute);
    }

    /**
     * 编辑工艺路线
     */
    @AutoLog(value = "工艺路线-编辑")
    @Operation(summary = "工艺路线-编辑")
    @RequiresPermissions("mes:mes_process_route:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<MesProcessRoute> edit(@RequestBody MesProcessRoute mesProcessRoute) {
        if (mesProcessRoute.getId() == null) {
            return Result.error("主键ID不能为空！");
        }
        if (mesProcessRoute.getRouteCode() != null && !mesProcessRoute.getRouteCode().trim().isEmpty()) {
            LambdaQueryWrapper<MesProcessRoute> dupWrapper = new LambdaQueryWrapper<>();
            dupWrapper.eq(MesProcessRoute::getRouteCode, mesProcessRoute.getRouteCode().trim())
                    .ne(MesProcessRoute::getId, mesProcessRoute.getId());
            if (mesProcessRouteService.count(dupWrapper) > 0) {
                return Result.error("工艺路线编号已存在！");
            }
        }
        mesProcessRoute.setUpdateTime(new Date());
        mesProcessRouteService.updateById(mesProcessRoute);
        return Result.OK("编辑成功!", mesProcessRoute);
    }

    /**
     * 通过id删除工艺路线（同步逻辑删除关联组成工序）
     */
    @AutoLog(value = "工艺路线-通过id删除")
    @Operation(summary = "工艺路线-通过id删除")
    @RequiresPermissions("mes:mes_process_route:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) Long id) {
        mesProcessRouteService.removeById(id);
        LambdaQueryWrapper<MesProcessRouteComposition> compWrapper = new LambdaQueryWrapper<>();
        compWrapper.eq(MesProcessRouteComposition::getRouteId, id);
        mesProcessRouteCompositionService.remove(compWrapper);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除工艺路线
     */
    @AutoLog(value = "工艺路线-批量删除")
    @Operation(summary = "工艺路线-批量删除")
    @RequiresPermissions("mes:mes_process_route:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .filter(s -> !s.trim().isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
        mesProcessRouteService.removeByIds(idList);
        if (!idList.isEmpty()) {
            LambdaQueryWrapper<MesProcessRouteComposition> compWrapper = new LambdaQueryWrapper<>();
            compWrapper.in(MesProcessRouteComposition::getRouteId, idList);
            mesProcessRouteCompositionService.remove(compWrapper);
        }
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询工艺路线
     */
    @Operation(summary = "工艺路线-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<MesProcessRoute> queryById(@RequestParam(name = "id", required = true) Long id) {
        MesProcessRoute mesProcessRoute = mesProcessRouteService.getById(id);
        if (mesProcessRoute == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(mesProcessRoute);
    }

    // ==================== 子表：组成工序 (mes_process_route_composition) ====================

    /**
     * 查询某工艺路线下的组成工序列表（包含自动计算的下一道工序）
     */
    @Operation(summary = "工艺组成工序-列表查询")
    @GetMapping(value = "/composition/list")
    public Result<List<MesProcessRouteComposition>> listCompositions(@RequestParam(name = "routeId", required = true) Long routeId) {
        List<MesProcessRouteComposition> list = mesProcessRouteCompositionService.listByRouteIdWithNextStep(routeId);
        return Result.OK(list);
    }

    /**
     * 添加工艺组成工序
     */
    @AutoLog(value = "工艺组成工序-添加")
    @Operation(summary = "工艺组成工序-添加")
    @RequiresPermissions("mes:mes_process_route:edit")
    @PostMapping(value = "/composition/add")
    public Result<String> addComposition(@RequestBody MesProcessRouteComposition composition) {
        mesProcessRouteCompositionService.addComposition(composition);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑工艺组成工序
     */
    @AutoLog(value = "工艺组成工序-编辑")
    @Operation(summary = "工艺组成工序-编辑")
    @RequiresPermissions("mes:mes_process_route:edit")
    @RequestMapping(value = "/composition/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> editComposition(@RequestBody MesProcessRouteComposition composition) {
        mesProcessRouteCompositionService.editComposition(composition);
        return Result.OK("修改成功！");
    }

    /**
     * 删除工艺组成工序
     */
    @AutoLog(value = "工艺组成工序-删除")
    @Operation(summary = "工艺组成工序-删除")
    @RequiresPermissions("mes:mes_process_route:edit")
    @DeleteMapping(value = "/composition/delete")
    public Result<String> deleteComposition(@RequestParam(name = "id", required = true) Long id) {
        mesProcessRouteCompositionService.removeById(id);
        return Result.OK("删除成功！");
    }

    /**
     * 批量删除工艺组成工序
     */
    @AutoLog(value = "工艺组成工序-批量删除")
    @Operation(summary = "工艺组成工序-批量删除")
    @RequiresPermissions("mes:mes_process_route:edit")
    @DeleteMapping(value = "/composition/deleteBatch")
    public Result<String> deleteBatchComposition(@RequestParam(name = "ids", required = true) String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .filter(s -> !s.trim().isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
        mesProcessRouteCompositionService.removeByIds(idList);
        return Result.OK("批量删除成功！");
    }

    /**
     * 导出excel
     */
    @RequiresPermissions("mes:mes_process_route:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, MesProcessRoute mesProcessRoute) {
        return super.exportXls(request, mesProcessRoute, MesProcessRoute.class, "工艺路线");
    }

    /**
     * 通过excel导入数据
     */
    @RequiresPermissions("mes:mes_process_route:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, MesProcessRoute.class);
    }
}
