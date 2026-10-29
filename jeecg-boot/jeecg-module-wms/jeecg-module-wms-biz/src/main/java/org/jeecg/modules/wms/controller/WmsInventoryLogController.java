package org.jeecg.modules.wms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.wms.mapper.WmsInventoryLogMapper;
import org.jeecg.modules.wms.vo.WmsInventoryLogPage;
import org.jeecg.modules.wms.vo.WmsInventoryLogQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description: 库存变动流水
 * 流水只由库存服务写入，页面只读。
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Tag(name = "库存流水")
@RestController
@RequestMapping("/wms/wmsInventoryLog")
@Slf4j
public class WmsInventoryLogController {

    @Autowired
    private WmsInventoryLogMapper wmsInventoryLogMapper;

    /**
     * 分页列表查询
     */
    @Operation(summary = "库存流水-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsInventoryLogPage>> queryPageList(WmsInventoryLogQuery query,
                                                            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(wmsInventoryLogMapper.selectLogPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNo, pageSize), query));
    }
}
