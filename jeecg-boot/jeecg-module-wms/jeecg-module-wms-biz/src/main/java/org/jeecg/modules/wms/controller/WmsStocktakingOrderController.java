package org.jeecg.modules.wms.controller;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.wms.entity.WmsStocktakingOrder;
import org.jeecg.modules.wms.service.IWmsStocktakingOrderService;
import org.jeecg.modules.wms.vo.WmsStocktakingOrderForm;
import org.jeecg.modules.wms.vo.WmsStocktakingSkuSelectVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Description: 盘库单管理 Controller
 * @Author: jeecg-boot
 * @Date: 2026-11-03
 * @Version: V1.0
 */
@Tag(name = "盘库单管理")
@RestController
@RequestMapping("/wms/stocktakingOrder")
@Slf4j
public class WmsStocktakingOrderController {

    @Autowired
    private IWmsStocktakingOrderService stocktakingOrderService;

    /**
     * 分页查询盘库单列表
     */
    @Operation(summary = "盘库单-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsStocktakingOrder>> queryPageList(
            @RequestParam(name = "orderNo", required = false) String orderNo,
            @RequestParam(name = "warehouseId", required = false) Integer warehouseId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<WmsStocktakingOrder> page = new Page<>(pageNo, pageSize);
        IPage<WmsStocktakingOrder> pageList = stocktakingOrderService.queryPageList(page, orderNo, warehouseId, status, startDate, endDate);
        return Result.OK(pageList);
    }

    /**
     * 根据ID查询盘库单详情（包含主表和子表明细）
     */
    @Operation(summary = "盘库单-根据ID查询详情")
    @GetMapping(value = "/queryById")
    public Result<WmsStocktakingOrderForm> queryById(@RequestParam(name = "id", required = true) Long id) {
        return Result.OK(stocktakingOrderService.queryDetailById(id));
    }

    /**
     * 暂存盘库单（草稿模式，不影响物理库存）
     */
    @AutoLog(value = "盘库单-暂存草稿")
    @Operation(summary = "盘库单-暂存草稿")
    @PostMapping(value = "/saveDraft")
    public Result<WmsStocktakingOrder> saveDraft(@RequestBody WmsStocktakingOrderForm form) {
        WmsStocktakingOrder order = stocktakingOrderService.saveDraft(form);
        return Result.OK("盘库单暂存成功！", order);
    }

    /**
     * 完成盘点（仅锁定实盘数与盈亏统计，不自动触发库存调账）
     */
    @AutoLog(value = "盘库单-完成盘点")
    @Operation(summary = "盘库单-完成盘点")
    @PostMapping(value = "/complete")
    public Result<WmsStocktakingOrder> complete(@RequestBody WmsStocktakingOrderForm form) {
        WmsStocktakingOrder order = stocktakingOrderService.complete(form);
        return Result.OK("盘点已完成，实盘数据已锁定！", order);
    }

    /**
     * 生成盘盈入库单并完成入库调账
     */
    @AutoLog(value = "盘库单-生成盘盈入库")
    @Operation(summary = "盘库单-生成盘盈入库")
    @PostMapping(value = "/createProfitEntry")
    public Result<String> createProfitEntry(@RequestParam(name = "orderId", required = true) Long orderId) {
        String rkCode = stocktakingOrderService.createProfitEntry(orderId);
        return Result.OK("盘盈入库单生成成功，单号: " + rkCode, rkCode);
    }

    /**
     * 生成盘亏出库单并完成出库调账
     */
    @AutoLog(value = "盘库单-生成盘亏出库")
    @Operation(summary = "盘库单-生成盘亏出库")
    @PostMapping(value = "/createLossOutbound")
    public Result<String> createLossOutbound(@RequestParam(name = "orderId", required = true) Long orderId) {
        String ckCode = stocktakingOrderService.createLossOutbound(orderId);
        return Result.OK("盘亏出库单生成成功，单号: " + ckCode, ckCode);
    }

    /**
     * 盘库商品选择器多维检索分页查询
     */
    @Operation(summary = "盘库商品选择器-多维分页列表查询")
    @GetMapping(value = "/selectSkuPage")
    public Result<IPage<WmsStocktakingSkuSelectVO>> selectSkuPage(
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @RequestParam(name = "skuCode", required = false) String skuCode,
            @RequestParam(name = "skuName", required = false) String skuName,
            @RequestParam(name = "spuCode", required = false) String spuCode,
            @RequestParam(name = "spuName", required = false) String spuName,
            @RequestParam(name = "warehouseId", required = false) Integer warehouseId,
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        Page<WmsStocktakingSkuSelectVO> page = new Page<>(pageNo, pageSize);
        IPage<WmsStocktakingSkuSelectVO> result = stocktakingOrderService.selectSkuPageForStocktaking(
                page, categoryId, skuCode, skuName, spuCode, spuName, warehouseId);
        return Result.OK(result);
    }

    /**
     * 一键导入仓库下所有有库存的商品明细
     */
    @Operation(summary = "盘库单-一键导入仓库所有商品")
    @GetMapping(value = "/importWarehouseAllSkus")
    public Result<List<WmsStocktakingSkuSelectVO>> importWarehouseAllSkus(
            @RequestParam(name = "warehouseId", required = true) Integer warehouseId) {
        return Result.OK(stocktakingOrderService.importWarehouseAllSkus(warehouseId));
    }

    /**
     * 获取指定仓库下某SKU当前的账面库存
     */
    @Operation(summary = "盘库单-获取SKU在库账面库存")
    @GetMapping(value = "/getSkuBookStock")
    public Result<Long> getSkuBookStock(
            @RequestParam(name = "warehouseId", required = true) Integer warehouseId,
            @RequestParam(name = "skuId", required = true) Long skuId) {
        return Result.OK(stocktakingOrderService.getSkuBookStock(warehouseId, skuId));
    }

    /**
     * 删除盘库单（仅草稿状态允许删除）
     */
    @AutoLog(value = "盘库单-删除草稿")
    @Operation(summary = "盘库单-删除草稿")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id", required = true) Long id) {
        stocktakingOrderService.deleteOrder(id);
        return Result.OK("删除成功！");
    }
}
