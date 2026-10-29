package org.jeecg.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.entity.WmsQualityCheck;
import org.jeecg.modules.wms.vo.WmsQualityCheckPage;
import org.jeecg.modules.wms.vo.WmsQualityCheckQuery;

/**
 * @Description: 质检单
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
public interface WmsQualityCheckMapper extends BaseMapper<WmsQualityCheck> {

    /**
     * 质检单分页查询：带出入库单号、仓库、商品编码与名称，
     * 让质检页面不必再逐行查入库单。
     */
    IPage<WmsQualityCheckPage> selectQualityCheckPage(Page<WmsQualityCheckPage> page,
                                                      @Param("q") WmsQualityCheckQuery query);
}
