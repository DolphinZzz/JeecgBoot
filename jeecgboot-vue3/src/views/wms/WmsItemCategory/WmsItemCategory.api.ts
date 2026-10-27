import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';

const { createConfirm } = useMessage();

enum Api {
  rootList = '/wms/wmsItemCategory/rootList',
  childList = '/wms/wmsItemCategory/childList',
  getChildListBatch = '/wms/wmsItemCategory/getChildListBatch',
  save = '/wms/wmsItemCategory/add',
  edit = '/wms/wmsItemCategory/edit',
  deleteOne = '/wms/wmsItemCategory/delete',
  deleteBatch = '/wms/wmsItemCategory/deleteBatch',
  importExcel = '/wms/wmsItemCategory/importExcel',
  exportXls = '/wms/wmsItemCategory/exportXls',
  loadTreeData = '/wms/wmsItemCategory/loadTreeRoot',
}

export const getExportUrl = Api.exportXls;
export const getImportUrl = Api.importExcel;
export const list = (params) => defHttp.get({ url: Api.rootList, params });
export const getChildList = (params) => defHttp.get({ url: Api.childList, params });
export const getChildListBatch = (params) => defHttp.get({ url: Api.getChildListBatch, params }, { isTransformResponse: false });
export const deleteOne = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.deleteOne, params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};
export const batchDelete = (params, handleSuccess) => {
  const ids = Array.isArray(params?.ids) ? params.ids.join(',') : params?.ids;
  createConfirm({
    iconType: 'warning',
    title: '确认删除',
    content: '是否删除选中数据',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      return defHttp.delete({ url: Api.deleteBatch, data: { ids } }, { joinParamsToUrl: true }).then(() => {
        handleSuccess();
      });
    },
  });
};
export const saveOrUpdate = (params, isUpdate) => {
  const url = isUpdate ? Api.edit : Api.save;
  return defHttp.post({ url: url, params });
};
export const loadTreeData = (params) => defHttp.get({ url: Api.loadTreeData, params });
