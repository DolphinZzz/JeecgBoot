package org.jeecg.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.entity.WmsItemSn;
import org.jeecg.modules.wms.vo.WmsItemSnPage;
import org.jeecg.modules.wms.vo.WmsItemSnQuery;

/**
 * @Description: SN序列号追踪
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
public interface WmsItemSnMapper extends BaseMapper<WmsItemSn> {

    /**
     * SN 追溯查询：带出所在仓库与库位编码。
     */
    IPage<WmsItemSnPage> selectSnPage(Page<WmsItemSnPage> page, @Param("q") WmsItemSnQuery query);
}
