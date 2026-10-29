package org.jeecg.modules.wms.controller;

import java.util.Arrays;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.wms.entity.WmsWarehouseArea;
import org.jeecg.modules.wms.service.IWmsWarehouseAreaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.system.base.controller.JeecgController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * @Description: 库区管理
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
@Tag(name = "库区管理")
@RestController
@RequestMapping("/wms/wmsWarehouseArea")
@Slf4j
public class WmsWarehouseAreaController extends JeecgController<WmsWarehouseArea, IWmsWarehouseAreaService> {
    @Autowired
    private IWmsWarehouseAreaService wmsWarehouseAreaService;

    /**
     * 分页列表查询
     */
    @Operation(summary = "库区管理-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsWarehouseArea>> queryPageList(WmsWarehouseArea wmsWarehouseArea,
                                                        @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                        @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                        HttpServletRequest req) {
        QueryWrapper<WmsWarehouseArea> queryWrapper = QueryGenerator.initQueryWrapper(wmsWarehouseArea, req.getParameterMap());
        Page<WmsWarehouseArea> page = new Page<>(pageNo, pageSize);
        IPage<WmsWarehouseArea> pageList = wmsWarehouseAreaService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     */
    @AutoLog(value = "库区管理-添加")
    @Operation(summary = "库区管理-添加")
    @RequiresPermissions("wms:wms_warehouse_area:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody WmsWarehouseArea wmsWarehouseArea) {
        wmsWarehouseAreaService.save(wmsWarehouseArea);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "库区管理-编辑")
    @Operation(summary = "库区管理-编辑")
    @RequiresPermissions("wms:wms_warehouse_area:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody WmsWarehouseArea wmsWarehouseArea) {
        wmsWarehouseAreaService.updateById(wmsWarehouseArea);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "库区管理-通过id删除")
    @Operation(summary = "库区管理-通过id删除")
    @RequiresPermissions("wms:wms_warehouse_area:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) Integer id) {
        wmsWarehouseAreaService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "库区管理-批量删除")
    @Operation(summary = "库区管理-批量删除")
    @RequiresPermissions("wms:wms_warehouse_area:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.wmsWarehouseAreaService.removeByIds(Arrays.stream(ids.split(",")).map(Integer::parseInt).collect(Collectors.toList()));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "库区管理-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsWarehouseArea> queryById(@RequestParam(name = "id", required = true) Integer id) {
        WmsWarehouseArea wmsWarehouseArea = wmsWarehouseAreaService.getById(id);
        if (wmsWarehouseArea == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(wmsWarehouseArea);
    }

    /**
     * 导出excel
     */
    @RequiresPermissions("wms:wms_warehouse_area:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, WmsWarehouseArea wmsWarehouseArea) {
        return super.exportXls(request, wmsWarehouseArea, WmsWarehouseArea.class, "库区信息");
    }

    /**
     * 通过excel导入数据
     */
    @RequiresPermissions("wms:wms_warehouse_area:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, WmsWarehouseArea.class);
    }
}
