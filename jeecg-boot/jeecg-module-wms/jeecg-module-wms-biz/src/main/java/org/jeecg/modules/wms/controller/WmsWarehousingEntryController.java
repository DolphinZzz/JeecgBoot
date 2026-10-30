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
import org.jeecg.modules.wms.dto.ErpWarehousingEntryCreateDto;
import org.jeecg.modules.wms.entity.WmsWarehousingEntry;
import org.jeecg.modules.wms.entity.WmsWarehousingEntryUp;
import org.jeecg.modules.wms.service.IWmsWarehousingEntryService;
import org.jeecg.modules.wms.vo.WmsEntryCreateForm;
import org.jeecg.modules.wms.vo.WmsEntryDetailView;
import org.jeecg.modules.wms.vo.WmsEntryPage;
import org.jeecg.modules.wms.vo.WmsEntryQuery;
import org.jeecg.modules.wms.vo.WmsReceiveForm;
import org.jeecg.modules.wms.vo.WmsWarehousingEntryResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Description: 入库单（建单 / 收货清点 / 超收放行 / 取消）
 *
 * 上架推荐与确认上架放在 WmsPutawayController，两者权限点不同。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Tag(name = "入库单")
@RestController
@RequestMapping("/wms/wmsWarehousingEntry")
@Slf4j
public class WmsWarehousingEntryController {

    @Autowired
    private IWmsWarehousingEntryService entryService;

    /**
     * 入库单分页查询
     */
    @Operation(summary = "入库单-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsEntryPage>> queryPageList(WmsEntryQuery query,
                                                     @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                     @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.OK(entryService.queryEntryPage(query, pageNo, pageSize));
    }

    /**
     * 新建入库单
     */
    @AutoLog(value = "入库单-新建")
    @Operation(summary = "入库单-新建")
    @RequiresPermissions("wms:wms_warehousing_entry:add")
    @PostMapping(value = "/add")
    public Result<WmsWarehousingEntry> add(@RequestBody WmsEntryCreateForm form) {
        WmsWarehousingEntry entry = entryService.createEntry(form);
        return Result.OK("入库单[" + entry.getEntryCode() + "]已创建", entry);
    }

    /**
     * ERP系统通过HTTP协议推送创建入库单（支持FeignClient或直接HTTP调用）
     */
    @AutoLog(value = "入库单-ERP推送创建")
    @Operation(summary = "入库单-ERP推送创建")
    @PostMapping(value = "/createFromErp")
    public Result<WmsWarehousingEntryResultVo> createFromErp(@RequestBody ErpWarehousingEntryCreateDto dto) {
        WmsWarehousingEntryResultVo result = entryService.createEntryFromErp(dto);
        return Result.OK("ERP采购单[" + result.getErpPoNo() + "]对应入库单[" + result.getEntryCode() + "]已创建", result);
    }

    /**
     * 通过 id 查询
     */
    @Operation(summary = "入库单-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsWarehousingEntry> queryById(@RequestParam(name = "id", required = true) Long id) {
        WmsWarehousingEntry entry = entryService.getById(id);
        if (entry == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(entry);
    }

    /**
     * 查询明细
     */
    @Operation(summary = "入库单-查询明细")
    @GetMapping(value = "/queryDetails")
    public Result<List<WmsEntryDetailView>> queryDetails(@RequestParam(name = "id", required = true) Long id) {
        return Result.OK(entryService.listDetails(id));
    }

    /**
     * 收货清点
     */
    @AutoLog(value = "入库单-收货清点")
    @Operation(summary = "入库单-收货清点")
    @RequiresPermissions("wms:wms_warehousing_entry:receive")
    @PostMapping(value = "/receive")
    public Result<Map<String, Object>> receive(@RequestBody WmsReceiveForm form) {
        Map<String, Object> result = entryService.receive(form);
        return Result.OK((String) result.getOrDefault("message", "收货完成"), result);
    }

    /**
     * 收货完成提交质检
     */
    @AutoLog(value = "入库单-提交质检")
    @Operation(summary = "入库单-提交质检")
    @RequiresPermissions("wms:wms_warehousing_entry:receive")
    @PostMapping(value = "/submitToQc")
    public Result<Map<String, Object>> submitToQc(@RequestParam(name = "id", required = true) Long id) {
        Map<String, Object> result = entryService.submitToQc(id);
        return Result.OK((String) result.getOrDefault("message", "已提交质检"), result);
    }

    /**
     * 超收放行 / 驳回
     */
    @AutoLog(value = "入库单-超收放行")
    @Operation(summary = "入库单-超收放行/驳回")
    @RequiresPermissions("wms:wms_warehousing_entry:overReceiveAudit")
    @PostMapping(value = "/auditOverReceive")
    public Result<String> auditOverReceive(@RequestParam(name = "id", required = true) Long id,
                                           @RequestParam(name = "pass", required = true) Boolean pass,
                                           @RequestParam(name = "reason", required = false) String reason) {
        entryService.auditOverReceive(id, pass, reason);
        return Result.OK(pass ? "超收已放行，可继续提交质检" : "超收已驳回");
    }

    /**
     * 查询上架记录
     */
    @Operation(summary = "入库单-查询上架记录")
    @GetMapping(value = "/queryPutawayRecords")
    public Result<List<WmsWarehousingEntryUp>> queryPutawayRecords(@RequestParam(name = "id", required = true) Long id) {
        return Result.OK(entryService.listPutawayRecords(id));
    }

    /**
     * 取消入库单
     */
    @AutoLog(value = "入库单-取消")
    @Operation(summary = "入库单-取消")
    @RequiresPermissions("wms:wms_warehousing_entry:cancel")
    @PostMapping(value = "/cancel")
    public Result<String> cancel(@RequestParam(name = "id", required = true) Long id) {
        entryService.cancel(id);
        return Result.OK("入库单已取消");
    }
}
