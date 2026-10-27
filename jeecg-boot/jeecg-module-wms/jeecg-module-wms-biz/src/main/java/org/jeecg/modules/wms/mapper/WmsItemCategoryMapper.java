package org.jeecg.modules.wms.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.wms.entity.WmsItemCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @Description: 商品类别
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
public interface WmsItemCategoryMapper extends BaseMapper<WmsItemCategory> {

    /**
     * 按父节点查询子节点。parentId 为空时查询顶级（parent_id 为空或 0）。
     */
    List<SelectTreeModel> queryListByPid(@Param("parentId") Integer parentId);
}
