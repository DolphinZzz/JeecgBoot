package org.jeecg.modules.wms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.service.IWmsInventoryService;
import org.jeecg.modules.wms.vo.WmsInventoryPage;
import org.jeecg.modules.wms.vo.WmsInventoryQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description: 实时库存（多维查询）
 * 库存只能由业务动作变动，不开放直接增删改。
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Tag(name = "实时库存")
@RestController
@RequestMapping("/wms/wmsInventory")
@Slf4j
public class WmsInventoryController {

    @Autowired
    private IWmsInventoryService wmsInventoryService;

    /**
     * 多维库存查询
     */
    @Operation(summary = "实时库存-多维查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsInventoryPage>> queryPageList(WmsInventoryQuery query,
                                                         @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                         @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(wmsInventoryService.queryInventoryPage(query, pageNo, pageSize));
    }

    /**
     * 通过id查询
     */
    @Operation(summary = "实时库存-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsInventory> queryById(@RequestParam(name = "id", required = true) Long id) {
        WmsInventory inventory = wmsInventoryService.getById(id);
        if (inventory == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(inventory);
    }

    /**
     * 库存总量核对：用于「库存数 = 流水累计净额」的验收
     */
    @AutoLog(value = "实时库存-总量核对")
    @Operation(summary = "实时库存-总量核对")
    @RequiresPermissions("wms:wms_inventory:verify")
    @GetMapping(value = "/sumTotal")
    public Result<Long> sumTotal(@RequestParam(name = "warehouseId", required = false) Integer warehouseId,
                                 @RequestParam(name = "skuId", required = false) Long skuId) {
        return Result.OK(wmsInventoryService.sumTotalCount(warehouseId, skuId));
    }
}
