package org.jeecg.modules.wms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.wms.entity.WmsItemSn;
import org.jeecg.modules.wms.mapper.WmsItemSnMapper;
import org.jeecg.modules.wms.vo.WmsItemSnPage;
import org.jeecg.modules.wms.vo.WmsItemSnQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description: SN 追溯
 * SN 由收货 / 复核环节写入，页面只读。
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Tag(name = "SN追溯")
@RestController
@RequestMapping("/wms/wmsItemSn")
@Slf4j
public class WmsItemSnController {

    @Autowired
    private WmsItemSnMapper wmsItemSnMapper;

    /**
     * SN 分页查询
     */
    @Operation(summary = "SN追溯-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsItemSnPage>> queryPageList(WmsItemSnQuery query,
                                                      @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(wmsItemSnMapper.selectSnPage(new Page<>(pageNo, pageSize), query));
    }

    /**
     * 按 SN 精确追溯单件
     */
    @Operation(summary = "SN追溯-按SN精确查询")
    @GetMapping(value = "/queryBySn")
    public Result<WmsItemSn> queryBySn(@RequestParam(name = "snCode", required = true) String snCode) {
        WmsItemSn sn = wmsItemSnMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WmsItemSn>()
                .eq(WmsItemSn::getSnCode, snCode.trim())
                .last("LIMIT 1"), false);
        if (sn == null) {
            return Result.error("未找到该SN的追溯记录");
        }
        return Result.OK(sn);
    }
}
