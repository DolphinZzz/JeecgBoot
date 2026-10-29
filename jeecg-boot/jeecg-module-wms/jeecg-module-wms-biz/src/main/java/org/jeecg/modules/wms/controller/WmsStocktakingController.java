package org.jeecg.modules.wms.controller;

import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.modules.wms.entity.WmsStocktakingInfo;
import org.jeecg.modules.wms.service.IWmsStocktakingService;
import org.jeecg.modules.wms.vo.WmsStocktakingCountForm;
import org.jeecg.modules.wms.vo.WmsStocktakingCreateForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Description: 盘点管理
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Tag(name = "盘点管理")
@RestController
@RequestMapping("/wms/wmsStocktaking")
@Slf4j
public class WmsStocktakingController {

    @Autowired
    private IWmsStocktakingService wmsStocktakingService;

    /**
     * 盘点记录分页查询
     */
    @Operation(summary = "盘点管理-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsStocktakingInfo>> queryPageList(@RequestParam(name = "batchNo", required = false) String batchNo,
                                                           @RequestParam(name = "status", required = false) String status,
                                                           @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                           @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(wmsStocktakingService.queryByBatch(batchNo, status, pageNo, pageSize));
    }

    /**
     * 生成盘点批次
     */
    @AutoLog(value = "盘点管理-生成盘点批次")
    @Operation(summary = "盘点管理-生成盘点批次")
    @RequiresPermissions("wms:wms_stocktaking:create")
    @PostMapping(value = "/create")
    public Result<Map<String, Object>> create(@RequestBody WmsStocktakingCreateForm form) {
        Map<String, Object> result = wmsStocktakingService.createStocktaking(form);
        return Result.OK((String) result.getOrDefault("message", "盘点批次已生成"), result);
    }

    /**
     * 录入实盘数
     */
    @AutoLog(value = "盘点管理-录入实盘数")
    @Operation(summary = "盘点管理-录入实盘数")
    @RequiresPermissions("wms:wms_stocktaking:count")
    @PostMapping(value = "/submitCount")
    public Result<String> submitCount(@RequestBody WmsStocktakingCountForm form) {
        wmsStocktakingService.submitCount(form);
        return Result.OK("实盘数已录入");
    }

    /**
     * 差异审批
     */
    @AutoLog(value = "盘点管理-差异审批")
    @Operation(summary = "盘点管理-差异审批")
    @RequiresPermissions("wms:wms_stocktaking:audit")
    @PostMapping(value = "/audit")
    public Result<String> audit(@RequestParam(name = "id", required = true) Long id,
                                @RequestParam(name = "pass", required = true) Boolean pass,
                                @RequestParam(name = "reason", required = false) String reason) {
        wmsStocktakingService.audit(id, pass, reason);
        return Result.OK(pass ? "差异已通过" : "差异已驳回");
    }

    /**
     * 结案调账
     */
    @AutoLog(value = "盘点管理-结案调账")
    @Operation(summary = "盘点管理-结案调账")
    @RequiresPermissions("wms:wms_stocktaking:confirm")
    @PostMapping(value = "/confirm")
    public Result<Map<String, Object>> confirm(@RequestParam(name = "id", required = true) Long id) {
        Map<String, Object> result = wmsStocktakingService.confirm(id);
        return Result.OK((String) result.getOrDefault("message", "已结案"), result);
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "盘点管理-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsStocktakingInfo> queryById(@RequestParam(name = "id", required = true) Long id) {
        WmsStocktakingInfo info = wmsStocktakingService.getById(id);
        if (info == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(info);
    }
}
