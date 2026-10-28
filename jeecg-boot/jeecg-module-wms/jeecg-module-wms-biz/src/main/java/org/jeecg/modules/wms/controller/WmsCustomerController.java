package org.jeecg.modules.wms.controller;

import java.util.Arrays;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.wms.entity.WmsCustomer;
import org.jeecg.modules.wms.service.IWmsCustomerService;
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
 * @Description: 客户信息
 * @Author: jeecg-boot
 * @Date: 2026-10-28
 * @Version: V1.0
 */
@Tag(name = "客户信息")
@RestController
@RequestMapping("/wms/wmsCustomer")
@Slf4j
public class WmsCustomerController extends JeecgController<WmsCustomer, IWmsCustomerService> {
    @Autowired
    private IWmsCustomerService wmsCustomerService;

    /**
     * 分页列表查询
     */
    @Operation(summary = "客户信息-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsCustomer>> queryPageList(WmsCustomer wmsCustomer,
                                                    @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                    @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                    HttpServletRequest req) {
        QueryWrapper<WmsCustomer> queryWrapper = QueryGenerator.initQueryWrapper(wmsCustomer, req.getParameterMap());
        Page<WmsCustomer> page = new Page<>(pageNo, pageSize);
        IPage<WmsCustomer> pageList = wmsCustomerService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     */
    @AutoLog(value = "客户信息-添加")
    @Operation(summary = "客户信息-添加")
    @RequiresPermissions("wms:wms_customer:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody WmsCustomer wmsCustomer) {
        wmsCustomerService.save(wmsCustomer);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "客户信息-编辑")
    @Operation(summary = "客户信息-编辑")
    @RequiresPermissions("wms:wms_customer:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody WmsCustomer wmsCustomer) {
        wmsCustomerService.updateById(wmsCustomer);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "客户信息-通过id删除")
    @Operation(summary = "客户信息-通过id删除")
    @RequiresPermissions("wms:wms_customer:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) Integer id) {
        wmsCustomerService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "客户信息-批量删除")
    @Operation(summary = "客户信息-批量删除")
    @RequiresPermissions("wms:wms_customer:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.wmsCustomerService.removeByIds(Arrays.stream(ids.split(",")).map(Integer::parseInt).collect(Collectors.toList()));
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "客户信息-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsCustomer> queryById(@RequestParam(name = "id", required = true) Integer id) {
        WmsCustomer wmsCustomer = wmsCustomerService.getById(id);
        if (wmsCustomer == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(wmsCustomer);
    }

    /**
     * 导出excel
     */
    @RequiresPermissions("wms:wms_customer:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, WmsCustomer wmsCustomer) {
        return super.exportXls(request, wmsCustomer, WmsCustomer.class, "客户信息");
    }

    /**
     * 通过excel导入数据
     */
    @RequiresPermissions("wms:wms_customer:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, WmsCustomer.class);
    }
}
