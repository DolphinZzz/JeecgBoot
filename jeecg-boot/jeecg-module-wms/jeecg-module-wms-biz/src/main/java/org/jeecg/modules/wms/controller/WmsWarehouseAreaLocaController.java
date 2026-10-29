package org.jeecg.modules.wms.controller;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.service.IWmsLocaScoreService;
import org.jeecg.modules.wms.service.IWmsWarehouseAreaLocaService;
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
 * @Description: 库位管理
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
@Tag(name = "库位管理")
@RestController
@RequestMapping("/wms/wmsWarehouseAreaLoca")
@Slf4j
public class WmsWarehouseAreaLocaController extends JeecgController<WmsWarehouseAreaLoca, IWmsWarehouseAreaLocaService> {

    @Autowired
    private IWmsWarehouseAreaLocaService wmsWarehouseAreaLocaService;

    @Autowired
    private IWmsLocaScoreService wmsLocaScoreService;

    /**
     * 分页列表查询
     */
    @Operation(summary = "库位管理-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsWarehouseAreaLoca>> queryPageList(WmsWarehouseAreaLoca wmsWarehouseAreaLoca,
                                                            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                            HttpServletRequest req) {
        QueryWrapper<WmsWarehouseAreaLoca> queryWrapper = QueryGenerator.initQueryWrapper(wmsWarehouseAreaLoca, req.getParameterMap());
        Page<WmsWarehouseAreaLoca> page = new Page<>(pageNo, pageSize);
        IPage<WmsWarehouseAreaLoca> pageList = wmsWarehouseAreaLocaService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     */
    @AutoLog(value = "库位管理-添加")
    @Operation(summary = "库位管理-添加")
    @RequiresPermissions("wms:wms_warehouse_area_loca:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody WmsWarehouseAreaLoca wmsWarehouseAreaLoca) {
        wmsWarehouseAreaLocaService.save(wmsWarehouseAreaLoca);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "库位管理-编辑")
    @Operation(summary = "库位管理-编辑")
    @RequiresPermissions("wms:wms_warehouse_area_loca:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody WmsWarehouseAreaLoca wmsWarehouseAreaLoca) {
        wmsWarehouseAreaLocaService.updateById(wmsWarehouseAreaLoca);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "库位管理-通过id删除")
    @Operation(summary = "库位管理-通过id删除")
    @RequiresPermissions("wms:wms_warehouse_area_loca:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) Integer id) {
        wmsWarehouseAreaLocaService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "库位管理-批量删除")
    @Operation(summary = "库位管理-批量删除")
    @RequiresPermissions("wms:wms_warehouse_area_loca:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.wmsWarehouseAreaLocaService.removeByIds(Arrays.stream(ids.split(",")).map(Integer::parseInt).collect(Collectors.toList()));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "库位管理-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsWarehouseAreaLoca> queryById(@RequestParam(name = "id", required = true) Integer id) {
        WmsWarehouseAreaLoca wmsWarehouseAreaLoca = wmsWarehouseAreaLocaService.getById(id);
        if (wmsWarehouseAreaLoca == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(wmsWarehouseAreaLoca);
    }

    /**
     * 重算库位等级及得分
     */
    @AutoLog(value = "库位管理-重算库位等级")
    @Operation(summary = "库位管理-重算库位等级")
    @RequiresPermissions("wms:wms_warehouse_area_loca:recalcLocaScore")
    @PostMapping(value = "/recalcLocaScore")
    public Result<Map<String, Object>> recalcLocaScore(@RequestParam(name = "warehouseId", required = true) Integer warehouseId) {
        Map<String, Object> summary = wmsLocaScoreService.recalculateWarehouseLocaScore(warehouseId);
        return Result.OK((String) summary.getOrDefault("message", "重算完成"), summary);
    }

    /**
     * 导出excel
     */
    @RequiresPermissions("wms:wms_warehouse_area_loca:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, WmsWarehouseAreaLoca wmsWarehouseAreaLoca) {
        return super.exportXls(request, wmsWarehouseAreaLoca, WmsWarehouseAreaLoca.class, "库位信息");
    }

    /**
     * 通过excel导入数据
     */
    @RequiresPermissions("wms:wms_warehouse_area_loca:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, WmsWarehouseAreaLoca.class);
    }
}
