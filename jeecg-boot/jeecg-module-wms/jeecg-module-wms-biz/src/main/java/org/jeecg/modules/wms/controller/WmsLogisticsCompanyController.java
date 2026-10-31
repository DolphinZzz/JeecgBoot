package org.jeecg.modules.wms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.modules.wms.entity.WmsLogisticsCompany;
import org.jeecg.modules.wms.service.IWmsLogisticsCompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * @Description: 物流公司基础数据控制器
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Tag(name = "物流公司管理")
@RestController
@RequestMapping("/wms/wmsLogisticsCompany")
@Slf4j
public class WmsLogisticsCompanyController {

    @Autowired
    private IWmsLogisticsCompanyService logisticsCompanyService;

    /**
     * 物流公司分页查询
     */
    @Operation(summary = "物流公司-分页列表查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsLogisticsCompany>> queryPageList(WmsLogisticsCompany company,
                                                            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        LambdaQueryWrapper<WmsLogisticsCompany> qw = new LambdaQueryWrapper<WmsLogisticsCompany>()
                .eq(WmsLogisticsCompany::getIsDel, 1);

        if (company.getCompanyName() != null && !company.getCompanyName().trim().isEmpty()) {
            qw.like(WmsLogisticsCompany::getCompanyName, company.getCompanyName().trim());
        }
        if (company.getLinkUser() != null && !company.getLinkUser().trim().isEmpty()) {
            qw.like(WmsLogisticsCompany::getLinkUser, company.getLinkUser().trim());
        }
        qw.orderByAsc(WmsLogisticsCompany::getSort).orderByAsc(WmsLogisticsCompany::getId);

        Page<WmsLogisticsCompany> page = new Page<>(pageNo, pageSize);
        return Result.OK(logisticsCompanyService.page(page, qw));
    }

    /**
     * 下拉选择列表查询全部启用的物流公司
     */
    @Operation(summary = "物流公司-查询全部启用")
    @GetMapping(value = "/queryAll")
    public Result<List<WmsLogisticsCompany>> queryAll() {
        return Result.OK(logisticsCompanyService.listActiveCompanies());
    }

    /**
     * 新增物流公司
     */
    @AutoLog(value = "物流公司-新增")
    @Operation(summary = "物流公司-新增")
    @PostMapping(value = "/add")
    public Result<String> add(@RequestBody WmsLogisticsCompany company) {
        if (company.getCompanyName() == null || company.getCompanyName().trim().isEmpty()) {
            return Result.error("物流公司名称不能为空");
        }
        Long count = logisticsCompanyService.count(new LambdaQueryWrapper<WmsLogisticsCompany>()
                .eq(WmsLogisticsCompany::getCompanyName, company.getCompanyName().trim())
                .eq(WmsLogisticsCompany::getIsDel, 1));
        if (count > 0) {
            return Result.error("物流公司名称[" + company.getCompanyName() + "]已存在");
        }
        company.setCreateTime(new Date());
        company.setIsDel(1);
        logisticsCompanyService.save(company);
        return Result.OK("添加成功");
    }

    /**
     * 编辑物流公司
     */
    @AutoLog(value = "物流公司-编辑")
    @Operation(summary = "物流公司-编辑")
    @PutMapping(value = "/edit")
    public Result<String> edit(@RequestBody WmsLogisticsCompany company) {
        WmsLogisticsCompany exist = logisticsCompanyService.getById(company.getId());
        if (exist == null) {
            return Result.error("物流公司不存在");
        }
        company.setUpdateTime(new Date());
        logisticsCompanyService.updateById(company);
        return Result.OK("编辑成功");
    }

    /**
     * 软删除物流公司
     */
    @AutoLog(value = "物流公司-删除")
    @Operation(summary = "物流公司-删除")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam(name = "id") Integer id) {
        WmsLogisticsCompany company = logisticsCompanyService.getById(id);
        if (company == null) {
            return Result.error("未找到对应数据");
        }
        company.setIsDel(2);
        company.setUpdateTime(new Date());
        logisticsCompanyService.updateById(company);
        return Result.OK("删除成功");
    }
}
