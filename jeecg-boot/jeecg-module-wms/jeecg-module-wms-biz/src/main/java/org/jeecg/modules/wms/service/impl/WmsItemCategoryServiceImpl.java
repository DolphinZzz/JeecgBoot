package org.jeecg.modules.wms.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.SelectTreeModel;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsItemCategory;
import org.jeecg.modules.wms.mapper.WmsItemCategoryMapper;
import org.jeecg.modules.wms.service.IWmsItemCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: 商品类别
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
@Service
public class WmsItemCategoryServiceImpl extends ServiceImpl<WmsItemCategoryMapper, WmsItemCategory> implements IWmsItemCategoryService {

    private static final int NAME_MAX_LENGTH = 50;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addWmsItemCategory(WmsItemCategory entity) {
        save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWmsItemCategory(WmsItemCategory entity) {
        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WmsItemCategory entity) {
        prepareInsert(entity);
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<WmsItemCategory> entityList, int batchSize) {
        for (WmsItemCategory entity : entityList) {
            prepareInsert(entity);
        }
        return super.saveBatch(entityList, batchSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(WmsItemCategory entity) {
        if (entity == null || entity.getId() == null) {
            throw new JeecgBootException("未找到对应数据");
        }
        WmsItemCategory old = getById(entity.getId());
        if (old == null) {
            throw new JeecgBootException("未找到对应实体");
        }
        normalize(entity);
        assertParentValid(entity.getId(), entity.getParentId());
        return super.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWmsItemCategory(Integer id) throws JeecgBootException {
        if (id == null) {
            throw new JeecgBootException("未找到对应实体");
        }
        WmsItemCategory item = getById(id);
        if (item == null) {
            return;
        }
        List<WmsItemCategory> children = list(new QueryWrapper<WmsItemCategory>().eq("parent_id", id));
        for (WmsItemCategory child : children) {
            deleteWmsItemCategory(child.getId());
        }
        removeById(id);
    }

    @Override
    public List<WmsItemCategory> queryTreeListNoPage(QueryWrapper<WmsItemCategory> queryWrapper) {
        List<WmsItemCategory> dataList = baseMapper.selectList(queryWrapper);
        List<WmsItemCategory> mapList = new ArrayList<>();
        for (WmsItemCategory data : dataList) {
            Integer parentId = data.getParentId();
            if (!isRoot(parentId)) {
                WmsItemCategory root = getTreeRoot(parentId);
                if (root != null && !containsId(mapList, root.getId())) {
                    mapList.add(root);
                }
            } else if (!containsId(mapList, data.getId())) {
                mapList.add(data);
            }
        }
        mapList.sort(Comparator.comparing(WmsItemCategory::getSort, Comparator.nullsLast(Integer::compareTo)).thenComparing(WmsItemCategory::getId, Comparator.nullsLast(Integer::compareTo)));
        fillHasChild(mapList);
        return mapList;
    }

    @Override
    public List<SelectTreeModel> queryListByPid(Integer parentId) {
        Integer pid = isRoot(parentId) ? null : parentId;
        return baseMapper.queryListByPid(pid);
    }

    @Override
    public void fillHasChild(List<WmsItemCategory> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Integer> ids = records.stream().map(WmsItemCategory::getId).filter(Objects::nonNull).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        QueryWrapper<WmsItemCategory> wrapper = new QueryWrapper<>();
        wrapper.select("parent_id").in("parent_id", ids).groupBy("parent_id");
        Set<Integer> parentIds = new HashSet<>();
        for (WmsItemCategory child : list(wrapper)) {
            if (child.getParentId() != null) {
                parentIds.add(child.getParentId());
            }
        }
        for (WmsItemCategory item : records) {
            item.setHasChild(parentIds.contains(item.getId()) ? "1" : "0");
        }
    }

    /**
     * 补齐新增必填项。id 留给数据库自增，is_del 默认 1。
     */
    private void prepareInsert(WmsItemCategory entity) {
        entity.setId(null);
        normalize(entity);
        assertParentValid(null, entity.getParentId());
        if (entity.getIsDel() == null) {
            entity.setIsDel(1);
        }
    }

    private void normalize(WmsItemCategory entity) {
        if (entity == null) {
            throw new JeecgBootException("请输入分类名字");
        }
        String name = entity.getName() == null ? "" : entity.getName().trim();
        if (oConvertUtils.isEmpty(name)) {
            throw new JeecgBootException("请输入分类名字");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new JeecgBootException("分类名字不能超过50个字符");
        }
        entity.setName(name);
        if (isRoot(entity.getParentId())) {
            entity.setParentId(null);
        }
    }

    /**
     * 所属类别必须存在，且不能是自己或自己的下级。
     */
    private void assertParentValid(Integer id, Integer parentId) {
        if (isRoot(parentId)) {
            return;
        }
        if (id != null && id.equals(parentId)) {
            throw new JeecgBootException("所属类别不能选择自己");
        }
        Integer cursor = parentId;
        int guard = 0;
        while (cursor != null && cursor != 0 && guard++ < 64) {
            if (id != null && id.equals(cursor)) {
                throw new JeecgBootException("所属类别不能选择当前类别的下级");
            }
            WmsItemCategory parent = getById(cursor);
            if (parent == null) {
                throw new JeecgBootException("所属类别不存在");
            }
            cursor = parent.getParentId();
        }
        if (guard >= 64) {
            throw new JeecgBootException("分类层级异常");
        }
    }

    private boolean isRoot(Integer parentId) {
        return parentId == null || parentId == 0;
    }

    private boolean containsId(List<WmsItemCategory> list, Integer id) {
        for (WmsItemCategory item : list) {
            if (item.getId() != null && item.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private WmsItemCategory getTreeRoot(Integer parentId) {
        if (isRoot(parentId)) {
            return null;
        }
        WmsItemCategory data = baseMapper.selectById(parentId);
        if (data == null) {
            return null;
        }
        if (isRoot(data.getParentId()) || data.getParentId().equals(data.getId())) {
            return data;
        }
        return getTreeRoot(data.getParentId());
    }
}
