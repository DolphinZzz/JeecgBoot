package org.jeecg.modules.wms.controller;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.wms.entity.WmsWarehousingEntry;
import org.jeecg.modules.wms.mapper.WmsWarehousingEntryUpMapper;
import org.jeecg.modules.wms.service.IWmsPutawayService;
import org.jeecg.modules.wms.service.IWmsWarehousingEntryService;
import org.jeecg.modules.wms.vo.WmsPutawayConfirmForm;
import org.jeecg.modules.wms.vo.WmsPutawayLocaOption;
import org.jeecg.modules.wms.vo.WmsPutawayPage;
import org.jeecg.modules.wms.vo.WmsPutawayPlan;
import org.jeecg.modules.wms.vo.WmsPutawayQuery;
import org.jeecg.modules.wms.vo.WmsPutawayRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * @Description: 上架作业（推荐库位 / 确认落库 / 上架记录）
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Tag(name = "上架作业")
@RestController
@RequestMapping("/wms/wmsPutaway")
@Slf4j
public class WmsPutawayController {

    @Autowired
    private IWmsWarehousingEntryService entryService;

    @Autowired
    private WmsWarehousingEntryUpMapper entryUpMapper;

    @Autowired
    private IWmsPutawayService putawayService;

    /**
     * 上架记录分页查询
     */
    @Operation(summary = "上架作业-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsPutawayPage>> queryPageList(WmsPutawayQuery query,
                                                       @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(entryUpMapper.selectPutawayPage(new Page<>(pageNo, pageSize), query));
    }

    /**
     * 上架推荐（不落库，页面用来展示推荐库位）
     */
    @Operation(summary = "上架作业-推荐库位")
    @RequiresPermissions("wms:wms_warehousing_entry_up:recommend")
    @GetMapping(value = "/recommend")
    public Result<WmsPutawayPlan> recommend(@RequestParam(name = "id", required = true) Long id) {
        return Result.OK(entryService.recommendPutaway(id));
    }

    /**
     * 人工指定库位的下拉选项（只列本仓，按库区分组，标注硬过滤结论）
     */
    @Operation(summary = "上架作业-可选库位")
    @RequiresPermissions("wms:wms_warehousing_entry_up:recommend")
    @GetMapping(value = "/locaOptions")
    public Result<List<WmsPutawayLocaOption>> locaOptions(@RequestParam(name = "id", required = true) Long id,
                                                          @RequestParam(name = "skuId", required = false) Long skuId,
                                                          @RequestParam(name = "count", required = false, defaultValue = "1") Integer count,
                                                          @RequestParam(name = "batchNumber", required = false) String batchNumber) {
        WmsWarehousingEntry entry = entryService.getById(id);
        if (entry == null) {
            return Result.error("入库单不存在");
        }
        return Result.OK(putawayService.listLocaOptions(new WmsPutawayRequest()
                .setWarehouseId(entry.getWarehouseId())
                .setSkuId(skuId)
                .setBatchNumber(batchNumber)
                .setCount(count == null ? 1 : count)
                .setQualityStatus(1)
                .setConsignorId(entry.getConsignorId())));
    }

    /**
     * 确认上架落库
     */
    @AutoLog(value = "上架作业-确认上架")
    @Operation(summary = "上架作业-确认上架")
    @RequiresPermissions("wms:wms_warehousing_entry_up:confirm")
    @PostMapping(value = "/confirm")
    public Result<Map<String, Object>> confirm(@RequestBody WmsPutawayConfirmForm form) {
        Map<String, Object> result = entryService.confirmPutaway(form);
        return Result.OK((String) result.getOrDefault("message", "上架完成"), result);
    }
}
