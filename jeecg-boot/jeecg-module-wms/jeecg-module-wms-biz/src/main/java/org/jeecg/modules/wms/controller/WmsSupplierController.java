package org.jeecg.modules.wms.controller;

import java.util.Arrays;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.wms.entity.WmsSupplier;
import org.jeecg.modules.wms.service.IWmsSupplierService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import org.jeecg.common.system.base.controller.JeecgController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.apache.shiro.authz.annotation.RequiresPermissions;

/**
 * @Description: 供应商信息
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
@Tag(name = "供应商信息")
@RestController
@RequestMapping("/wms/wmsSupplier")
@Slf4j
public class WmsSupplierController extends JeecgController<WmsSupplier, IWmsSupplierService> {
    @Autowired
    private IWmsSupplierService wmsSupplierService;

    /**
     * 分页列表查询
     */
    @Operation(summary = "供应商信息-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsSupplier>> queryPageList(WmsSupplier wmsSupplier,
                                                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                       HttpServletRequest req) {
        QueryWrapper<WmsSupplier> queryWrapper = QueryGenerator.initQueryWrapper(wmsSupplier, req.getParameterMap());
        Page<WmsSupplier> page = new Page<>(pageNo, pageSize);
        IPage<WmsSupplier> pageList = wmsSupplierService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     */
    @AutoLog(value = "供应商信息-添加")
    @Operation(summary = "供应商信息-添加")
    @RequiresPermissions("wms:wms_supplier:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody WmsSupplier wmsSupplier) {
        wmsSupplierService.save(wmsSupplier);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "供应商信息-编辑")
    @Operation(summary = "供应商信息-编辑")
    @RequiresPermissions("wms:wms_supplier:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody WmsSupplier wmsSupplier) {
        wmsSupplierService.updateById(wmsSupplier);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "供应商信息-通过id删除")
    @Operation(summary = "供应商信息-通过id删除")
    @RequiresPermissions("wms:wms_supplier:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) Integer id) {
        wmsSupplierService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "供应商信息-批量删除")
    @Operation(summary = "供应商信息-批量删除")
    @RequiresPermissions("wms:wms_supplier:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.wmsSupplierService.removeByIds(Arrays.stream(ids.split(",")).map(Integer::parseInt).collect(Collectors.toList()));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "供应商信息-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsSupplier> queryById(@RequestParam(name = "id", required = true) Integer id) {
        WmsSupplier wmsSupplier = wmsSupplierService.getById(id);
        if (wmsSupplier == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(wmsSupplier);
    }

    /**
     * 导出excel
     */
    @RequiresPermissions("wms:wms_supplier:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, WmsSupplier wmsSupplier) {
        return super.exportXls(request, wmsSupplier, WmsSupplier.class, "供应商信息");
    }

    /**
     * 通过excel导入数据
     */
    @RequiresPermissions("wms:wms_supplier:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, WmsSupplier.class);
    }
}
