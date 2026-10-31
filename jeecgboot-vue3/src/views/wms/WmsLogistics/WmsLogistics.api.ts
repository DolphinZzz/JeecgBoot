import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';

const { createConfirm } = useMessage();

enum Api {
  list = '/wms/wmsLogisticsCompany/list',
  all = '/wms/wmsLogisticsCompany/queryAll',
  add = '/wms/wmsLogisticsCompany/add',
  edit = '/wms/wmsLogisticsCompany/edit',
  deleteOne = '/wms/wmsLogisticsCompany/delete',
  deleteBatch = '/wms/wmsLogisticsCompany/deleteBatch',
}

/**
 * 分页查询物流承运商
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

/**
 * 查询所有有效承运商下拉列表
 */
export const queryAllLogistics = () => defHttp.get({ url: Api.all });

/**
 * 保存或更新承运商
 */
export const saveOrUpdate = (params, isUpdate) => {
  const url = isUpdate ? Api.edit : Api.add;
  return isUpdate
    ? defHttp.put({ url, params }, { successMessageMode: 'none' })
    : defHttp.post({ url, params }, { successMessageMode: 'none' });
};

/**
 * 删除单个
 */
export const deleteOne = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.deleteOne, params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};

/**
 * 批量删除
 */
export const batchDelete = (params, handleSuccess) => {
  createConfirm({
    iconType: 'warning',
    title: '确认删除',
    content: '是否删除选中的物流承运商？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      return defHttp.delete({ url: Api.deleteBatch, params }, { joinParamsToUrl: true }).then(() => {
        handleSuccess();
      });
    },
  });
};
