package org.jeecg.modules.wms.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.query.QueryGenerator;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.modules.wms.entity.WmsWarehouseOperations;
import org.jeecg.modules.wms.entity.WmsWarehouseOperationsDetails;
import org.jeecg.modules.wms.service.IWmsWarehouseOperationsService;
import org.jeecg.modules.wms.vo.WmsOperationForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Description: 库内作业（移库移位 / 补货）
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Tag(name = "库内作业")
@RestController
@RequestMapping("/wms/wmsWarehouseOperations")
@Slf4j
public class WmsWarehouseOperationsController {

    @Autowired
    private IWmsWarehouseOperationsService wmsWarehouseOperationsService;

    /**
     * 分页列表查询
     */
    @Operation(summary = "库内作业-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsWarehouseOperations>> queryPageList(WmsWarehouseOperations wmsWarehouseOperations,
                                                               @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                               @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                               HttpServletRequest req) {
        QueryWrapper<WmsWarehouseOperations> queryWrapper = QueryGenerator.initQueryWrapper(wmsWarehouseOperations, req.getParameterMap());
        queryWrapper.orderByDesc("id");
        Page<WmsWarehouseOperations> page = new Page<>(pageNo, pageSize);
        return Result.OK(wmsWarehouseOperationsService.page(page, queryWrapper));
    }

    /**
     * 新建移库 / 补货作业单
     */
    @AutoLog(value = "库内作业-新建作业单")
    @Operation(summary = "库内作业-新建作业单")
    @RequiresPermissions("wms:wms_warehouse_operations:add")
    @PostMapping(value = "/add")
    public Result<WmsWarehouseOperations> add(@RequestBody WmsOperationForm form) {
        WmsWarehouseOperations operation = wmsWarehouseOperationsService.createOperation(form);
        return Result.OK("作业单[" + operation.getJobCode() + "]已生成，确认后执行", operation);
    }

    /**
     * 执行作业单
     */
    @AutoLog(value = "库内作业-执行作业单")
    @Operation(summary = "库内作业-执行作业单")
    @RequiresPermissions("wms:wms_warehouse_operations:execute")
    @PostMapping(value = "/execute")
    public Result<Map<String, Object>> execute(@RequestParam(name = "id", required = true) Long id) {
        Map<String, Object> result = wmsWarehouseOperationsService.execute(id);
        return Result.OK((String) result.getOrDefault("message", "作业单执行完成"), result);
    }

    /**
     * 取消作业单
     */
    @AutoLog(value = "库内作业-取消作业单")
    @Operation(summary = "库内作业-取消作业单")
    @RequiresPermissions("wms:wms_warehouse_operations:cancel")
    @PostMapping(value = "/cancel")
    public Result<String> cancel(@RequestParam(name = "id", required = true) Long id) {
        wmsWarehouseOperationsService.cancel(id);
        return Result.OK("已取消");
    }

    /**
     * 作业单明细
     * 明细不分页，但必须以 IPage 返回：字典切面只翻译 IPage 内的记录，
     * 直接返回 List 会导致库位、SKU、货主等 @Dict 字段没有 _dictText。
     */
    @Operation(summary = "库内作业-作业单明细")
    @GetMapping(value = "/queryDetails")
    public Result<IPage<WmsWarehouseOperationsDetails>> queryDetails(@RequestParam(name = "id", required = true) Long id) {
        List<WmsWarehouseOperationsDetails> details = wmsWarehouseOperationsService.listDetails(id);
        Page<WmsWarehouseOperationsDetails> page = new Page<>(1, details.size() == 0 ? 1 : details.size(), details.size());
        page.setRecords(details);
        return Result.OK(page);
    }

    /**
     * 按拣货区补货上下限自动生成补货单
     */
    @AutoLog(value = "库内作业-生成补货单")
    @Operation(summary = "库内作业-生成补货单")
    @RequiresPermissions("wms:wms_warehouse_operations:generateReplenish")
    @PostMapping(value = "/generateReplenish")
    public Result<WmsWarehouseOperations> generateReplenish(@RequestParam(name = "warehouseId", required = true) Integer warehouseId,
                                                            @RequestParam(name = "skuIds", required = false) String skuIds) {
        List<Long> ids = null;
        if (skuIds != null && !skuIds.trim().isEmpty()) {
            ids = Arrays.stream(skuIds.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                    .map(Long::parseLong).collect(java.util.stream.Collectors.toList());
        }
        WmsWarehouseOperations operation = wmsWarehouseOperationsService.generateReplenish(warehouseId, ids);
        return Result.OK("补货单[" + operation.getJobCode() + "]已生成", operation);
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "库内作业-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsWarehouseOperations> queryById(@RequestParam(name = "id", required = true) Long id) {
        WmsWarehouseOperations operation = wmsWarehouseOperationsService.getById(id);
        if (operation == null) {
            return Result.error("未找到对应数据");
        }
        operation.setDetailsList(wmsWarehouseOperationsService.listDetails(id));
        return Result.OK(operation);
    }
}
