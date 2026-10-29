package org.jeecg.modules.wms.controller;

import java.util.Arrays;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.wms.entity.WmsStrategyConfig;
import org.jeecg.modules.wms.service.IWmsStrategyConfigService;
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
 * @Description: 仓库策略参数配置
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
@Tag(name = "仓库策略参数配置")
@RestController
@RequestMapping("/wms/wmsStrategyConfig")
@Slf4j
public class WmsStrategyConfigController extends JeecgController<WmsStrategyConfig, IWmsStrategyConfigService> {
    @Autowired
    private IWmsStrategyConfigService wmsStrategyConfigService;

    /**
     * 分页列表查询
     */
    @Operation(summary = "策略参数配置-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsStrategyConfig>> queryPageList(WmsStrategyConfig wmsStrategyConfig,
                                                         @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                         @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                         HttpServletRequest req) {
        QueryWrapper<WmsStrategyConfig> queryWrapper = QueryGenerator.initQueryWrapper(wmsStrategyConfig, req.getParameterMap());
        Page<WmsStrategyConfig> page = new Page<>(pageNo, pageSize);
        IPage<WmsStrategyConfig> pageList = wmsStrategyConfigService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     */
    @AutoLog(value = "策略参数配置-添加")
    @Operation(summary = "策略参数配置-添加")
    @RequiresPermissions("wms:wms_strategy_config:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody WmsStrategyConfig wmsStrategyConfig) {
        wmsStrategyConfigService.save(wmsStrategyConfig);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "策略参数配置-编辑")
    @Operation(summary = "策略参数配置-编辑")
    @RequiresPermissions("wms:wms_strategy_config:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody WmsStrategyConfig wmsStrategyConfig) {
        wmsStrategyConfigService.updateById(wmsStrategyConfig);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "策略参数配置-通过id删除")
    @Operation(summary = "策略参数配置-通过id删除")
    @RequiresPermissions("wms:wms_strategy_config:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) Long id) {
        wmsStrategyConfigService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "策略参数配置-批量删除")
    @Operation(summary = "策略参数配置-批量删除")
    @RequiresPermissions("wms:wms_strategy_config:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.wmsStrategyConfigService.removeByIds(Arrays.stream(ids.split(",")).map(Long::parseLong).collect(Collectors.toList()));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "策略参数配置-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsStrategyConfig> queryById(@RequestParam(name = "id", required = true) Long id) {
        WmsStrategyConfig wmsStrategyConfig = wmsStrategyConfigService.getById(id);
        if (wmsStrategyConfig == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(wmsStrategyConfig);
    }

    /**
     * 导出excel
     */
    @RequiresPermissions("wms:wms_strategy_config:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, WmsStrategyConfig wmsStrategyConfig) {
        return super.exportXls(request, wmsStrategyConfig, WmsStrategyConfig.class, "策略参数配置");
    }

    /**
     * 通过excel导入数据
     */
    @RequiresPermissions("wms:wms_strategy_config:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, WmsStrategyConfig.class);
    }
}
