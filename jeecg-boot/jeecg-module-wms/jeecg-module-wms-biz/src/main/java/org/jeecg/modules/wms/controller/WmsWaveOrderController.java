package org.jeecg.modules.wms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.wms.entity.WmsWaveOrder;
import org.jeecg.modules.wms.service.IWmsWaveOrderService;
import org.jeecg.modules.wms.vo.WmsWaveDetailView;
import org.jeecg.modules.wms.vo.WmsWaveGenerateForm;
import org.jeecg.modules.wms.vo.WmsWavePage;
import org.jeecg.modules.wms.vo.WmsWaveQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Description: 出库波次控制器（波次聚合、播种分货管理）
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Tag(name = "出库波次管理")
@RestController
@RequestMapping("/wms/wmsWaveOrder")
@Slf4j
public class WmsWaveOrderController {

    @Autowired
    private IWmsWaveOrderService waveOrderService;

    /**
     * 波次分页列表查询
     */
    @Operation(summary = "波次-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsWavePage>> queryPageList(WmsWaveQuery query,
                                                    @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                    @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(waveOrderService.queryWavePage(query, pageNo, pageSize));
    }

    /**
     * 查询波次详情（含波次头、出库单清单、汇总拣货任务行）
     */
    @Operation(summary = "波次-详情查询")
    @GetMapping(value = "/queryById")
    public Result<WmsWaveDetailView> queryById(@RequestParam(name = "id") Long id) {
        return Result.OK(waveOrderService.getWaveDetail(id));
    }

    /**
     * 自动/指定分波（按承运商+温区分波生成波次单）
     */
    @AutoLog(value = "波次-自动分波")
    @Operation(summary = "波次-自动分波")
    @RequiresPermissions("wms:wms_wave_order:generate")
    @PostMapping(value = "/generate")
    public Result<List<WmsWaveOrder>> generate(@RequestBody WmsWaveGenerateForm form) {
        List<WmsWaveOrder> waves = waveOrderService.generateWaves(form);
        return Result.OK("成功生成 " + waves.size() + " 个波次单", waves);
    }

    /**
     * 开始波次拣货
     */
    @AutoLog(value = "波次-开始拣货")
    @Operation(summary = "波次-开始拣货")
    @RequiresPermissions("wms:wms_wave_order:startPick")
    @PostMapping(value = "/startPick")
    public Result<String> startPick(@RequestParam(name = "id") Long id,
                                    @RequestParam(name = "empId", required = false) Integer empId) {
        waveOrderService.startWavePick(id, empId);
        return Result.OK("波次已开始拣货");
    }

    /**
     * 完成波次分货播种
     */
    @AutoLog(value = "波次-完成分货")
    @Operation(summary = "波次-完成分货播种")
    @RequiresPermissions("wms:wms_wave_order:completeDistribute")
    @PostMapping(value = "/completeDistribute")
    public Result<String> completeDistribute(@RequestParam(name = "id") Long id) {
        waveOrderService.completeWaveDistribute(id);
        return Result.OK("波次分货播种完成，关联出库单已流转至待复核");
    }

    /**
     * 取消波次
     */
    @AutoLog(value = "波次-取消")
    @Operation(summary = "波次-取消波次")
    @RequiresPermissions("wms:wms_wave_order:cancel")
    @PostMapping(value = "/cancel")
    public Result<String> cancel(@RequestParam(name = "id") Long id) {
        waveOrderService.cancelWave(id);
        return Result.OK("波次已取消，出库单已释放");
    }
}
