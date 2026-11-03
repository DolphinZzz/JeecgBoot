package org.jeecg.modules.wms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.wms.entity.WmsItemSpu;
import org.jeecg.modules.wms.mapper.WmsItemSpuMapper;
import org.jeecg.modules.wms.service.IWmsLocaItemCapacityService;
import org.jeecg.modules.wms.vo.WmsLocaCapacityItemVO;
import org.jeecg.modules.wms.vo.WmsLocaCapacitySaveDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description: 库位商品规划与容量设置
 * @Author: jeecg-boot
 * @Date: 2026-11-01
 * @Version: V1.0
 */
@Tag(name = "库位商品规划")
@RestController
@RequestMapping("/wms/wmsLocaItemCapacity")
@Slf4j
public class WmsLocaItemCapacityController {

    @Autowired
    private IWmsLocaItemCapacityService capacityService;

    @Autowired
    private WmsItemSpuMapper spuMapper;

    /**
     * 查询指定库位已配置的商品列表及当前在库/剩余容量
     */
    @Operation(summary = "库位商品规划-查询库位规划列表")
    @GetMapping(value = "/listByLocaId")
    public Result<List<WmsLocaCapacityItemVO>> listByLocaId(@RequestParam(name = "locaId", required = true) Integer locaId) {
        return Result.OK(capacityService.listCapacityByLocaId(locaId));
    }

    /**
     * 保存库位商品规划（单库位最多3个SPU）
     */
    @AutoLog(value = "库位商品规划-保存规划配置")
    @Operation(summary = "库位商品规划-保存规划配置")
    @RequiresPermissions("wms:wms_inventory:capacity_edit")
    @PostMapping(value = "/saveBatch")
    public Result<String> saveBatch(@RequestBody WmsLocaCapacitySaveDTO dto) {
        capacityService.saveLocaCapacity(dto);
        return Result.OK("保存库位商品规划成功！");
    }

    /**
     * 获取可选商品SPU下拉选项（用于配置商品弹窗）
     */
    @Operation(summary = "库位商品规划-可选商品SPU列表")
    @GetMapping(value = "/spuOptions")
    public Result<List<Map<String, Object>>> spuOptions(@RequestParam(name = "keyword", required = false) String keyword) {
        LambdaQueryWrapper<WmsItemSpu> wrapper = new LambdaQueryWrapper<WmsItemSpu>()
                .select(WmsItemSpu::getId, WmsItemSpu::getTitle, WmsItemSpu::getSubTitle)
                .orderByDesc(WmsItemSpu::getId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(WmsItemSpu::getTitle, keyword.trim())
                    .or().like(WmsItemSpu::getSubTitle, keyword.trim()));
        }
        wrapper.last("LIMIT 100");
        List<WmsItemSpu> list = spuMapper.selectList(wrapper);
        List<Map<String, Object>> result = list.stream().map(spu -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", spu.getId());
            map.put("spuCode", spu.getSubTitle());
            map.put("spuName", spu.getTitle());
            map.put("label", (spu.getSubTitle() != null ? spu.getSubTitle() + " - " : "") + spu.getTitle());
            map.put("value", spu.getId());
            return map;
        }).collect(Collectors.toList());
        return Result.OK(result);
    }
}
