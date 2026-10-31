package org.jeecg.modules.wms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.wms.dto.ErpOutboundOrderCreateDto;
import org.jeecg.modules.wms.entity.WmsOutboundOrder;
import org.jeecg.modules.wms.service.IWmsOutboundOrderService;
import org.jeecg.modules.wms.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Description: 出库单控制器（出库全链路闭环操作入口）
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Tag(name = "出库单管理")
@RestController
@RequestMapping("/wms/wmsOutboundOrder")
@Slf4j
public class WmsOutboundOrderController {

    @Autowired
    private IWmsOutboundOrderService outboundOrderService;

    /**
     * 出库单分页列表
     */
    @Operation(summary = "出库单-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsOutboundPage>> queryPageList(WmsOutboundQuery query,
                                                        @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                        @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(outboundOrderService.queryOutboundPage(query, pageNo, pageSize));
    }

    /**
     * 出库单详情
     */
    @Operation(summary = "出库单-详情查询")
    @GetMapping(value = "/queryById")
    public Result<WmsOutboundPage> queryById(@RequestParam(name = "id") Long id) {
        return Result.OK(outboundOrderService.getOutboundDetail(id));
    }

    /**
     * 出库单明细列表
     */
    @Operation(summary = "出库单-明细列表查询")
    @GetMapping(value = "/queryDetails")
    public Result<List<WmsOutboundDetailView>> queryDetails(@RequestParam(name = "id") Long id) {
        return Result.OK(outboundOrderService.listDetails(id));
    }

    /**
     * 新建出库单
     */
    @AutoLog(value = "出库单-新建")
    @Operation(summary = "出库单-新建")
    @RequiresPermissions("wms:wms_outbound_order:add")
    @PostMapping(value = "/add")
    public Result<WmsOutboundOrder> add(@RequestBody WmsOutboundCreateForm form) {
        WmsOutboundOrder order = outboundOrderService.createOutboundOrder(form);
        return Result.OK("出库单[" + order.getOutboundCode() + "]创建成功", order);
    }

    /**
     * ERP/MES外部系统推送创建出库单（支持Feign及HTTP）
     */
    @AutoLog(value = "出库单-ERP推送创建")
    @Operation(summary = "出库单-ERP推送创建")
    @PostMapping(value = "/createFromErp")
    public Result<WmsOutboundOrderResultVo> createFromErp(@RequestBody ErpOutboundOrderCreateDto dto) {
        WmsOutboundOrderResultVo result = outboundOrderService.createFromErp(dto);
        return Result.OK("出库单[" + result.getOutboundCode() + "]创建成功", result);
    }

    /**
     * 库存预检与分配锁库（FEFO近效期优先、临期拦截、工单批次强校验）
     */
    @AutoLog(value = "出库单-库存预检分配")
    @Operation(summary = "出库单-库存预检分配")
    @RequiresPermissions("wms:wms_outbound_order:allocate")
    @PostMapping(value = "/allocate")
    public Result<WmsOutboundAllocateResultVo> allocate(@RequestParam(name = "id") Long id) {
        WmsOutboundAllocateResultVo result = outboundOrderService.allocateInventory(id);
        return Result.OK(result.getMessage(), result);
    }

    /**
     * 实拣确认（SN校验与拣货锁定）
     */
    @AutoLog(value = "出库单-实拣确认")
    @Operation(summary = "出库单-实拣确认")
    @RequiresPermissions("wms:wms_outbound_order:pickConfirm")
    @PostMapping(value = "/confirmPick")
    public Result<String> confirmPick(@RequestBody WmsPickConfirmForm form) {
        outboundOrderService.confirmPick(form);
        return Result.OK("实拣确认成功");
    }

    /**
     * 出库复核确认
     */
    @AutoLog(value = "出库单-复核确认")
    @Operation(summary = "出库单-复核确认")
    @RequiresPermissions("wms:wms_outbound_order:reviewConfirm")
    @PostMapping(value = "/confirmReview")
    public Result<String> confirmReview(@RequestBody WmsReviewConfirmForm form) {
        outboundOrderService.confirmReview(form);
        return Result.OK("复核确认成功");
    }

    /**
     * 打包称重（理论重量防漏发比对）
     */
    @AutoLog(value = "出库单-打包称重")
    @Operation(summary = "出库单-打包称重")
    @RequiresPermissions("wms:wms_outbound_order:packageWeight")
    @PostMapping(value = "/packageAndWeigh")
    public Result<String> packageAndWeigh(@RequestBody WmsPackageWeightForm form) {
        outboundOrderService.packageAndWeigh(form);
        return Result.OK("打包称重成功");
    }

    /**
     * 物流发运出库确认（库存真实扣减与SN出库）
     */
    @AutoLog(value = "出库单-物流发货")
    @Operation(summary = "出库单-物流发货")
    @RequiresPermissions("wms:wms_outbound_order:shipConfirm")
    @PostMapping(value = "/confirmShip")
    public Result<String> confirmShip(@RequestBody WmsShipConfirmForm form) {
        outboundOrderService.confirmShip(form);
        return Result.OK("出库发货确认成功，已扣减库存并核销");
    }

    /**
     * 取消出库单（逆向闭环）
     */
    @AutoLog(value = "出库单-取消")
    @Operation(summary = "出库单-取消")
    @RequiresPermissions("wms:wms_outbound_order:cancel")
    @PostMapping(value = "/cancel")
    public Result<String> cancel(@RequestParam(name = "id") Long id,
                                 @RequestParam(name = "reason", required = false) String reason) {
        outboundOrderService.cancelOutboundOrder(id, reason);
        return Result.OK("出库单已取消");
    }

    /**
     * 删除出库单（仅限待分配状态草稿单据软删除）
     */
    @AutoLog(value = "出库单-删除")
    @Operation(summary = "出库单-删除")
    @RequiresPermissions("wms:wms_outbound_order:delete")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id") Long id) {
        WmsOutboundOrder order = outboundOrderService.getById(id);
        if (order == null) {
            return Result.error("出库单不存在");
        }
        if (order.getStatus() != 10 && order.getStatus() != 91) {
            return Result.error("只有待分配(10)或已取消(91)的出库单允许删除");
        }
        outboundOrderService.removeById(id);
        return Result.OK("删除成功");
    }
}
