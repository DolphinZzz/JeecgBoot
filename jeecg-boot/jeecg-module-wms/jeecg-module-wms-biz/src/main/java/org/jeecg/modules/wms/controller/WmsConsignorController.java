package org.jeecg.modules.wms.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsConsignor;
import org.jeecg.modules.wms.service.IWmsConsignorService;

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
 * @Description: 货主信息
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Tag(name = "货主信息")
@RestController
@RequestMapping("/wms/wmsConsignor")
@Slf4j
public class WmsConsignorController extends JeecgController<WmsConsignor, IWmsConsignorService> {
    @Autowired
    private IWmsConsignorService wmsConsignorService;

    /**
     * 分页列表查询
     */
    @Operation(summary = "货主信息-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsConsignor>> queryPageList(WmsConsignor wmsConsignor,
                                                     @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                     @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                     HttpServletRequest req) {
        QueryWrapper<WmsConsignor> queryWrapper = QueryGenerator.initQueryWrapper(wmsConsignor, req.getParameterMap());
        Page<WmsConsignor> page = new Page<>(pageNo, pageSize);
        IPage<WmsConsignor> pageList = wmsConsignorService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     */
    @AutoLog(value = "货主信息-添加")
    @Operation(summary = "货主信息-添加")
    @RequiresPermissions("wms:wms_consignor:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody WmsConsignor wmsConsignor) {
        wmsConsignorService.save(wmsConsignor);
        return Result.OK("添加成功！", wmsConsignor.getConsignorCode());
    }

    /**
     * 编辑
     */
    @AutoLog(value = "货主信息-编辑")
    @Operation(summary = "货主信息-编辑")
    @RequiresPermissions("wms:wms_consignor:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody WmsConsignor wmsConsignor) {
        wmsConsignorService.updateById(wmsConsignor);
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "货主信息-通过id删除")
    @Operation(summary = "货主信息-通过id删除")
    @RequiresPermissions("wms:wms_consignor:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) Integer id) {
        wmsConsignorService.checkDeletable(id);
        wmsConsignorService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除：逐个校验引用，任何一个已被引用就整批不删。
     */
    @AutoLog(value = "货主信息-批量删除")
    @Operation(summary = "货主信息-批量删除")
    @RequiresPermissions("wms:wms_consignor:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        List<Integer> idList = Arrays.stream(ids.split(",")).map(String::trim)
                .filter(oConvertUtils::isNotEmpty).map(Integer::parseInt).collect(Collectors.toList());
        for (Integer id : idList) {
            wmsConsignorService.checkDeletable(id);
        }
        this.wmsConsignorService.removeByIds(idList);
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "货主信息-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsConsignor> queryById(@RequestParam(name = "id", required = true) Integer id) {
        WmsConsignor wmsConsignor = wmsConsignorService.getById(id);
        if (wmsConsignor == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(wmsConsignor);
    }

    /**
     * 导出excel
     */
    @RequiresPermissions("wms:wms_consignor:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, WmsConsignor wmsConsignor) {
        return super.exportXls(request, wmsConsignor, WmsConsignor.class, "货主信息");
    }

    /**
     * 通过excel导入数据
     */
    @RequiresPermissions("wms:wms_consignor:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, WmsConsignor.class);
    }
}
