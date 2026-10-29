package org.jeecg.modules.wms.controller;

import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.wms.entity.WmsQualityCheck;
import org.jeecg.modules.wms.mapper.WmsQualityCheckMapper;
import org.jeecg.modules.wms.service.IWmsWarehousingEntryService;
import org.jeecg.modules.wms.vo.WmsQcSubmitForm;
import org.jeecg.modules.wms.vo.WmsQualityCheckPage;
import org.jeecg.modules.wms.vo.WmsQualityCheckQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Description: 质检单（IQC 查询 / 提交结论）
 * 质检单由收货提交时自动生成，页面不做新增与删除。
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Tag(name = "质检单")
@RestController
@RequestMapping("/wms/wmsQualityCheck")
@Slf4j
public class WmsQualityCheckController {

    @Autowired
    private WmsQualityCheckMapper qualityCheckMapper;

    @Autowired
    private IWmsWarehousingEntryService entryService;

    /**
     * 质检单分页查询
     */
    @Operation(summary = "质检单-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsQualityCheckPage>> queryPageList(WmsQualityCheckQuery query,
                                                            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(qualityCheckMapper.selectQualityCheckPage(new Page<>(pageNo, pageSize), query));
    }

    /**
     * 通过 id 查询
     */
    @Operation(summary = "质检单-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsQualityCheck> queryById(@RequestParam(name = "id", required = true) Long id) {
        WmsQualityCheck record = qualityCheckMapper.selectById(id);
        if (record == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(record);
    }

    /**
     * 提交质检结果
     */
    @AutoLog(value = "质检单-提交质检结果")
    @Operation(summary = "质检单-提交质检结果")
    @RequiresPermissions("wms:wms_quality_check:submit")
    @PostMapping(value = "/submit")
    public Result<Map<String, Object>> submit(@RequestBody WmsQcSubmitForm form) {
        Map<String, Object> result = entryService.submitQc(form);
        return Result.OK((String) result.getOrDefault("message", "质检已提交"), result);
    }
}
