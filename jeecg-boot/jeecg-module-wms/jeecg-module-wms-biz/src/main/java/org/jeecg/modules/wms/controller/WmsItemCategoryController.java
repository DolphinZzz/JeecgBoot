package org.jeecg.modules.wms.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsItemCategory;
import org.jeecg.modules.wms.service.IWmsItemCategoryService;

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
 * @Description: 商品类别
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
@Tag(name = "商品类别")
@RestController
@RequestMapping("/wms/wmsItemCategory")
@Slf4j
public class WmsItemCategoryController extends JeecgController<WmsItemCategory, IWmsItemCategoryService> {
    @Autowired
    private IWmsItemCategoryService wmsItemCategoryService;

    /**
     * 分页列表查询（树根节点）
     * 有搜索条件时(hasQuery=true)：搜索后归并到顶级节点
     * 无搜索条件时：只查所属类别为空的顶级节点
     */
    @Operation(summary = "商品类别-分页列表查询")
    @GetMapping(value = "/rootList")
    public Result<IPage<WmsItemCategory>> queryPageList(WmsItemCategory entity,
                                                         @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                         @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                         HttpServletRequest req) {
        String hasQuery = req.getParameter("hasQuery");
        if (hasQuery != null && "true".equals(hasQuery)) {
            entity.setParentId(null);
            QueryWrapper<WmsItemCategory> queryWrapper = QueryGenerator.initQueryWrapper(entity, req.getParameterMap());
            List<WmsItemCategory> list = wmsItemCategoryService.queryTreeListNoPage(queryWrapper);
            IPage<WmsItemCategory> pageList = new Page<>(1, Math.max(list.size(), 10), list.size());
            pageList.setRecords(list);
            return Result.OK(pageList);
        }
        Integer parentId = entity.getParentId();
        entity.setParentId(null);
        QueryWrapper<WmsItemCategory> queryWrapper = QueryGenerator.initQueryWrapper(entity, req.getParameterMap());
        if (parentId == null || parentId == 0) {
            queryWrapper.and(wrapper -> wrapper.isNull("parent_id").or().eq("parent_id", 0));
        } else {
            queryWrapper.eq("parent_id", parentId);
        }
        applyDefaultOrder(queryWrapper, req);
        Page<WmsItemCategory> page = new Page<>(pageNo, pageSize);
        IPage<WmsItemCategory> pageList = wmsItemCategoryService.page(page, queryWrapper);
        wmsItemCategoryService.fillHasChild(pageList.getRecords());
        return Result.OK(pageList);
    }

    /**
     * 获取子数据（展开树节点时调用）
     */
    @Operation(summary = "商品类别-获取子数据")
    @GetMapping(value = "/childList")
    public Result<IPage<WmsItemCategory>> queryChildList(WmsItemCategory entity, HttpServletRequest req) {
        QueryWrapper<WmsItemCategory> queryWrapper = QueryGenerator.initQueryWrapper(entity, req.getParameterMap());
        applyDefaultOrder(queryWrapper, req);
        List<WmsItemCategory> list = wmsItemCategoryService.list(queryWrapper);
        wmsItemCategoryService.fillHasChild(list);
        IPage<WmsItemCategory> pageList = new Page<>(1, Math.max(list.size(), 10), list.size());
        pageList.setRecords(list);
        return Result.OK(pageList);
    }

    /**
     * 批量查询子节点（已展开节点刷新时批量加载子数据）
     */
    @Operation(summary = "商品类别-批量获取子数据")
    @GetMapping("/getChildListBatch")
    public Result<IPage<WmsItemCategory>> getChildListBatch(@RequestParam("parentIds") String parentIds) {
        try {
            List<Integer> parentIdList = Arrays.stream(parentIds.split(","))
                    .map(String::trim)
                    .filter(item -> oConvertUtils.isNotEmpty(item) && !item.contains("loadChild"))
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());
            QueryWrapper<WmsItemCategory> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("parent_id", parentIdList).orderByAsc("sort").orderByAsc("id");
            List<WmsItemCategory> list = parentIdList.isEmpty() ? List.of() : wmsItemCategoryService.list(queryWrapper);
            wmsItemCategoryService.fillHasChild(list);
            IPage<WmsItemCategory> pageList = new Page<>(1, Math.max(list.size(), 10), list.size());
            pageList.setRecords(list);
            return Result.OK(pageList);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.error("批量查询子节点失败：" + e.getMessage());
        }
    }

    /**
     * 加载节点的子数据（树选择组件）
     */
    @Operation(summary = "商品类别-加载子节点")
    @GetMapping(value = "/loadTreeChildren")
    public Result<List<SelectTreeModel>> loadTreeChildren(@RequestParam(name = "pid", required = false) String pid) {
        try {
            return Result.OK(wmsItemCategoryService.queryListByPid(parsePid(pid)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 加载顶级节点；async=false 时同步返回整棵树
     */
    @Operation(summary = "商品类别-加载树")
    @GetMapping(value = "/loadTreeRoot")
    public Result<List<SelectTreeModel>> loadTreeRoot(@RequestParam(name = "async") Boolean async,
                                                       @RequestParam(name = "pcode", required = false) String pcode) {
        try {
            List<SelectTreeModel> ls = wmsItemCategoryService.queryListByPid(parsePid(pcode));
            if (async == null || !async) {
                loadAllChildren(ls);
            }
            return Result.OK(ls);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    private void loadAllChildren(List<SelectTreeModel> ls) {
        for (SelectTreeModel tsm : ls) {
            List<SelectTreeModel> temp = wmsItemCategoryService.queryListByPid(parsePid(tsm.getKey()));
            if (temp != null && !temp.isEmpty()) {
                tsm.setChildren(temp);
                loadAllChildren(temp);
            }
        }
    }

    /**
     * 添加
     */
    @AutoLog(value = "商品类别-添加")
    @Operation(summary = "商品类别-添加")
    @RequiresPermissions("wms:wms_item_category:add")
    @PostMapping(value = "/add")
    public Result<WmsItemCategory> add(@RequestBody WmsItemCategory wmsItemCategory) {
        wmsItemCategoryService.addWmsItemCategory(wmsItemCategory);
        return Result.OK("添加成功！", wmsItemCategory);
    }

    /**
     * 编辑
     */
    @AutoLog(value = "商品类别-编辑")
    @Operation(summary = "商品类别-编辑")
    @RequiresPermissions("wms:wms_item_category:edit")
    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<WmsItemCategory> edit(@RequestBody WmsItemCategory wmsItemCategory) {
        wmsItemCategoryService.updateWmsItemCategory(wmsItemCategory);
        return Result.OK("编辑成功!", wmsItemCategory);
    }

    /**
     * 通过id删除
     */
    @AutoLog(value = "商品类别-通过id删除")
    @Operation(summary = "商品类别-通过id删除")
    @RequiresPermissions("wms:wms_item_category:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) Integer id) {
        wmsItemCategoryService.deleteWmsItemCategory(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除。逐条删除，以便同时删除下级。
     */
    @AutoLog(value = "商品类别-批量删除")
    @Operation(summary = "商品类别-批量删除")
    @RequiresPermissions("wms:wms_item_category:deleteBatch")
    @DeleteMapping(value = "/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids", required = true) String ids) {
        for (String id : ids.split(",")) {
            if (oConvertUtils.isEmpty(id) || id.contains("loadChild")) {
                continue;
            }
            wmsItemCategoryService.deleteWmsItemCategory(Integer.valueOf(id.trim()));
        }
        return Result.OK("批量删除成功!");
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "商品类别-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsItemCategory> queryById(@RequestParam(name = "id", required = true) Integer id) {
        WmsItemCategory wmsItemCategory = wmsItemCategoryService.getById(id);
        if (wmsItemCategory == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(wmsItemCategory);
    }

    /**
     * 导出excel
     */
    @RequiresPermissions("wms:wms_item_category:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, WmsItemCategory wmsItemCategory) {
        return super.exportXls(request, wmsItemCategory, WmsItemCategory.class, "商品类别");
    }

    /**
     * 通过excel导入数据
     */
    @RequiresPermissions("wms:wms_item_category:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, WmsItemCategory.class);
    }

    private void applyDefaultOrder(QueryWrapper<WmsItemCategory> queryWrapper, HttpServletRequest req) {
        if (oConvertUtils.isEmpty(req.getParameter("column"))) {
            queryWrapper.orderByAsc("sort").orderByAsc("id");
        }
    }

    private Integer parsePid(String pid) {
        if (oConvertUtils.isEmpty(pid) || "0".equals(pid) || "null".equals(pid)) {
            return null;
        }
        return Integer.valueOf(pid);
    }
}
