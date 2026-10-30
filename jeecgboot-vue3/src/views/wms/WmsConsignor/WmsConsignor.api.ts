import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';

const { createConfirm } = useMessage();

enum Api {
  list = '/wms/wmsConsignor/list',
  save = '/wms/wmsConsignor/add',
  edit = '/wms/wmsConsignor/edit',
  deleteOne = '/wms/wmsConsignor/delete',
  deleteBatch = '/wms/wmsConsignor/deleteBatch',
  importExcel = '/wms/wmsConsignor/importExcel',
  exportXls = '/wms/wmsConsignor/exportXls',
}

/**
 * 导出api
 */
export const getExportUrl = Api.exportXls;

/**
 * 导入api
 */
export const getImportUrl = Api.importExcel;

/**
 * 列表接口
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

/**
 * 删除单个：货主已被业务引用时后端会返回业务异常，这里把消息透传给用户
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
    content: '是否删除选中数据？已被入库单/库存引用的货主不允许删除，只能停用。',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      return defHttp.delete({ url: Api.deleteBatch, data: params }, { joinParamsToUrl: true }).then(() => {
        handleSuccess();
      });
    },
  });
};

/**
 * 保存或者更新
 */
export const saveOrUpdate = (params, isUpdate) => {
  const url = isUpdate ? Api.edit : Api.save;
  return defHttp.post({ url: url, params }, { successMessageMode: 'none' });
};
