package org.jeecg.modules.mes.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.query.QueryRuleEnum;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.mes.entity.MesWorkshop;
import org.jeecg.modules.mes.service.IMesWorkshopService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.jeecg.common.system.base.controller.JeecgController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.apache.shiro.authz.annotation.RequiresPermissions;
 /**
 * @Description: mes_workshop
 * @Author: jeecg-boot
 * @Date:   2026-11-03
 * @Version: V1.0
 */
@Tag(name="mes_workshop")
@RestController
@RequestMapping("/mes/mesWorkshop")
@Slf4j
public class MesWorkshopController extends JeecgController<MesWorkshop, IMesWorkshopService> {
	@Autowired
	private IMesWorkshopService mesWorkshopService;
	
	/**
	 * 分页列表查询
	 *
	 * @param mesWorkshop
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "mes_workshop-分页列表查询")
	@Operation(summary="mes_workshop-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<MesWorkshop>> queryPageList(MesWorkshop mesWorkshop,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {


		QueryWrapper<MesWorkshop> queryWrapper = QueryGenerator.initQueryWrapper(mesWorkshop, req.getParameterMap());
		Page<MesWorkshop> page = new Page<MesWorkshop>(pageNo, pageSize);
		IPage<MesWorkshop> pageList = mesWorkshopService.page(page, queryWrapper);
		return Result.OK(pageList);
	}

	/**
	 *   添加
	 *
	 * @param mesWorkshop
	 * @return
	 */
	@AutoLog(value = "mes_workshop-添加")
	@Operation(summary="mes_workshop-添加")
	@RequiresPermissions("mes:mes_workshop:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody MesWorkshop mesWorkshop) {
		mesWorkshopService.save(mesWorkshop);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param mesWorkshop
	 * @return
	 */
	@AutoLog(value = "mes_workshop-编辑")
	@Operation(summary="mes_workshop-编辑")
	@RequiresPermissions("mes:mes_workshop:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody MesWorkshop mesWorkshop) {
		mesWorkshopService.updateById(mesWorkshop);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "mes_workshop-通过id删除")
	@Operation(summary="mes_workshop-通过id删除")
	@RequiresPermissions("mes:mes_workshop:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		mesWorkshopService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "mes_workshop-批量删除")
	@Operation(summary="mes_workshop-批量删除")
	@RequiresPermissions("mes:mes_workshop:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.mesWorkshopService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "mes_workshop-通过id查询")
	@Operation(summary="mes_workshop-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<MesWorkshop> queryById(@RequestParam(name="id",required=true) String id) {
		MesWorkshop mesWorkshop = mesWorkshopService.getById(id);
		if(mesWorkshop==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(mesWorkshop);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param mesWorkshop
    */
    @RequiresPermissions("mes:mes_workshop:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, MesWorkshop mesWorkshop) {
        return super.exportXls(request, mesWorkshop, MesWorkshop.class, "mes_workshop");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    @RequiresPermissions("mes:mes_workshop:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, MesWorkshop.class);
    }

}
