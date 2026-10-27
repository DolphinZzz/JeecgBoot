package org.jeecg.modules.wms.controller;

import java.util.Arrays;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsBrand;
import org.jeecg.modules.wms.service.IWmsBrandService;

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
 * @Description: 品牌管理
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
@Tag(name = "品牌管理")
@RestController
@RequestMapping("/wms/wmsBrand")
@Slf4j
public class WmsBrandController extends JeecgController<WmsBrand, IWmsBrandService> {
    @Autowired
    private IWmsBrandService wmsBrandService;

    /**
     * 分页列表查询
     */
    @Operation(summary = "品牌管理-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsBrand>> queryPageList(WmsBrand wmsBrand,
                                                  @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                  @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                  HttpServletRequest req) {
        QueryWrapper<WmsBrand> queryWrapper = QueryGenerator.initQueryWrapper(wmsBrand, req.getParameterMap());
        if (oConvertUtils.isEmpty(req.getParameter("column"))) {
            queryWrapper.orderByAsc("sort").orderByAsc("id");
        }
        Page<WmsBrand> page = new Page<>(pageNo, pageSize);
        IPage<WmsBrand> pageList = wmsBrandService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     */
    @AutoLog(value = "品牌管理-添加")
    @Operation(summary = "品牌管理-添加")
    @RequiresPermissions("wms:wms_brand:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody WmsBrand wmsBrand) {
        wmsBrandService.save(wmsBrand);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "品牌管理-编辑")
    @Operation(summary = "品牌管理-编辑")
    @RequiresPermissions("wms:wms_brand:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody WmsBrand wmsBrand) {
        wmsBrandService.updateById(wmsBrand);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "品牌管理-通过id删除")
    @Operation(summary = "品牌管理-通过id删除")
    @RequiresPermissions("wms:wms_brand:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) Integer id) {
        wmsBrandService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "品牌管理-批量删除")
    @Operation(summary = "品牌管理-批量删除")
    @RequiresPermissions("wms:wms_brand:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.wmsBrandService.removeByIds(Arrays.stream(ids.split(",")).map(Integer::parseInt).collect(Collectors.toList()));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "品牌管理-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsBrand> queryById(@RequestParam(name = "id", required = true) Integer id) {
        WmsBrand wmsBrand = wmsBrandService.getById(id);
        if (wmsBrand == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(wmsBrand);
    }

    /**
     * 导出excel
     */
    @RequiresPermissions("wms:wms_brand:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, WmsBrand wmsBrand) {
        return super.exportXls(request, wmsBrand, WmsBrand.class, "品牌管理");
    }

    /**
     * 通过excel导入数据
     */
    @RequiresPermissions("wms:wms_brand:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, WmsBrand.class);
    }
}
