package org.jeecg.modules.wms.service;

import java.util.List;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.modules.wms.entity.WmsItemCategory;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @Description: 商品类别
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
public interface IWmsItemCategoryService extends IService<WmsItemCategory> {

    /**新增节点。未选所属类别时作为顶级。*/
    void addWmsItemCategory(WmsItemCategory entity);

    /**修改节点*/
    void updateWmsItemCategory(WmsItemCategory entity);

    /**删除节点，并删除其全部下级*/
    void deleteWmsItemCategory(Integer id) throws JeecgBootException;

    /**查询全部匹配数据后，归并到各自的顶级节点（搜索用）*/
    List<WmsItemCategory> queryTreeListNoPage(QueryWrapper<WmsItemCategory> queryWrapper);

    /**
     * 按父节点加载树。parentId 为空时加载顶级。
     */
    List<SelectTreeModel> queryListByPid(Integer parentId);

    /**按是否存在子记录回填 hasChild，不写库。*/
    void fillHasChild(List<WmsItemCategory> records);
}
