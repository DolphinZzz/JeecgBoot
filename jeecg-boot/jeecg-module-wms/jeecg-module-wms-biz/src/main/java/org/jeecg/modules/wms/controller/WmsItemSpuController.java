package org.jeecg.modules.wms.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.entity.enmus.ExcelType;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.jeecg.common.system.vo.LoginUser;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsItemSpu;
import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.vo.WmsItemSpuPage;
import org.jeecg.modules.wms.service.IWmsItemSpuService;
import org.jeecg.modules.wms.service.IWmsItemSkuService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.apache.shiro.authz.annotation.RequiresPermissions;

/**
 * @Description: 商品管理
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
@Tag(name = "商品管理")
@RestController
@RequestMapping("/wms/wmsItemSpu")
@Slf4j
public class WmsItemSpuController {

    @Autowired
    private IWmsItemSpuService wmsItemSpuService;

    @Autowired
    private IWmsItemSkuService wmsItemSkuService;

    /**
     * 分页列表查询
     */
    @Operation(summary = "商品管理-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsItemSpu>> queryPageList(WmsItemSpu wmsItemSpu,
                                                    @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                    @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                    HttpServletRequest req) {
        QueryWrapper<WmsItemSpu> queryWrapper = QueryGenerator.initQueryWrapper(wmsItemSpu, req.getParameterMap());
        if (oConvertUtils.isEmpty(req.getParameter("column"))) {
            queryWrapper.orderByDesc("id");
        }
        Page<WmsItemSpu> page = new Page<>(pageNo, pageSize);
        IPage<WmsItemSpu> pageList = wmsItemSpuService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     */
    @AutoLog(value = "商品管理-添加")
    @Operation(summary = "商品管理-添加")
    @RequiresPermissions("wms:wms_item_spu:add")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody WmsItemSpuPage wmsItemSpuPage) {
        WmsItemSpu wmsItemSpu = new WmsItemSpu();
        BeanUtils.copyProperties(wmsItemSpuPage, wmsItemSpu);
        wmsItemSpuService.saveMain(wmsItemSpu, wmsItemSpuPage.getWmsItemSkuList());
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "商品管理-编辑")
    @Operation(summary = "商品管理-编辑")
    @RequiresPermissions("wms:wms_item_spu:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<String> edit(@RequestBody WmsItemSpuPage wmsItemSpuPage) {
        WmsItemSpu wmsItemSpu = new WmsItemSpu();
        BeanUtils.copyProperties(wmsItemSpuPage, wmsItemSpu);
        WmsItemSpu wmsItemSpuEntity = wmsItemSpuService.getById(wmsItemSpu.getId());
        if (wmsItemSpuEntity == null) {
            return Result.error("未找到对应数据");
        }
        wmsItemSpuService.updateMain(wmsItemSpu, wmsItemSpuPage.getWmsItemSkuList());
        return Result.OK("编辑成功!");
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "商品管理-通过id删除")
    @Operation(summary = "商品管理-通过id删除")
    @RequiresPermissions("wms:wms_item_spu:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) Long id) {
        wmsItemSpuService.delMain(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "商品管理-批量删除")
    @Operation(summary = "商品管理-批量删除")
    @RequiresPermissions("wms:wms_item_spu:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        this.wmsItemSpuService.delBatchMain(Arrays.stream(ids.split(",")).map(Long::valueOf).collect(Collectors.toList()));
        return Result.OK("批量删除成功！");
    }

    /**
     * 删除某个商品下选中的 SKU
     */
    @AutoLog(value = "商品管理-删除SKU")
    @Operation(summary = "商品管理-删除SKU")
    @RequiresPermissions("wms:wms_item_spu:edit")
    @DeleteMapping(value = "/deleteSkuBatch")
    public Result<String> deleteSkuBatch(@RequestParam(name = "spuId") Long spuId, @RequestParam(name = "ids") String ids) {
        List<Long> skuIds = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(item -> item.matches("\\d+"))
                .map(Long::valueOf)
                .collect(Collectors.toList());
        wmsItemSpuService.deleteSkuBatch(spuId, skuIds);
        return Result.OK("删除成功!");
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "商品管理-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsItemSpu> queryById(@RequestParam(name = "id", required = true) Long id) {
        WmsItemSpu wmsItemSpu = wmsItemSpuService.getById(id);
        if (wmsItemSpu == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(wmsItemSpu);
    }

    /**
     * SKU通过主表ID查询
     */
    @Operation(summary = "商品SKU-主表ID查询")
    @GetMapping(value = "/queryWmsItemSkuByMainId")
    public Result<IPage<WmsItemSku>> queryWmsItemSkuListByMainId(@RequestParam(name = "id", required = true) Long id) {
        List<WmsItemSku> wmsItemSkuList = wmsItemSkuService.selectByMainId(id);
        IPage<WmsItemSku> page = new Page<>();
        page.setRecords(wmsItemSkuList);
        page.setTotal(wmsItemSkuList.size());
        return Result.OK(page);
    }

    /**
     * 导出excel
     */
    @RequiresPermissions("wms:wms_item_spu:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, WmsItemSpu wmsItemSpu) {
        QueryWrapper<WmsItemSpu> queryWrapper = QueryGenerator.initQueryWrapper(wmsItemSpu, request.getParameterMap());
        LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String selections = request.getParameter("selections");
        if (oConvertUtils.isNotEmpty(selections)) {
            queryWrapper.in("id", Arrays.asList(selections.split(",")));
        }
        List<WmsItemSpu> wmsItemSpuList = wmsItemSpuService.list(queryWrapper);
        List<WmsItemSpuPage> pageList = new ArrayList<>();
        for (WmsItemSpu main : wmsItemSpuList) {
            WmsItemSpuPage vo = new WmsItemSpuPage();
            BeanUtils.copyProperties(main, vo);
            vo.setWmsItemSkuList(wmsItemSkuService.selectByMainId(main.getId()));
            pageList.add(vo);
        }
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        mv.addObject(NormalExcelConstants.FILE_NAME, "商品管理列表");
        mv.addObject(NormalExcelConstants.CLASS, WmsItemSpuPage.class);
        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("商品管理数据", "导出人:" + sysUser.getRealname(), "商品管理", ExcelType.XSSF));
        mv.addObject(NormalExcelConstants.DATA_LIST, pageList);
        return mv;
    }

    /**
     * 通过excel导入数据
     */
    @RequiresPermissions("wms:wms_item_spu:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            MultipartFile file = entity.getValue();
            ImportParams params = new ImportParams();
            params.setTitleRows(2);
            params.setHeadRows(1);
            params.setNeedSave(true);
            try {
                List<WmsItemSpuPage> list = ExcelImportUtil.importExcel(file.getInputStream(), WmsItemSpuPage.class, params);
                for (WmsItemSpuPage page : list) {
                    WmsItemSpu po = new WmsItemSpu();
                    BeanUtils.copyProperties(page, po);
                    wmsItemSpuService.saveMain(po, page.getWmsItemSkuList());
                }
                return Result.OK("文件导入成功！数据行数:" + list.size());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Result.error("文件导入失败:" + e.getMessage());
            } finally {
                try {
                    file.getInputStream().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Result.OK("文件导入失败！");
    }
}
