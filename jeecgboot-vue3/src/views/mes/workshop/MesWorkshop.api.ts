import {defHttp} from '/@/utils/http/axios';
import { useMessage } from "/@/hooks/web/useMessage";

const { createConfirm } = useMessage();

enum Api {
  list = '/mes/mesWorkshop/list',
  save = '/mes/mesWorkshop/add',
  edit = '/mes/mesWorkshop/edit',
  deleteOne = '/mes/mesWorkshop/delete',
  deleteBatch = '/mes/mesWorkshop/deleteBatch',
  importExcel = '/mes/mesWorkshop/importExcel',
  exportXls = '/mes/mesWorkshop/exportXls',
  getNextCode = '/mes/mesWorkshop/getNextWorkshopCode',
}
/**
 * 获取下一个车间编码 (WSxxxx, xxxx从1001开始)
 */
export const getNextWorkshopCode = () =>
  defHttp.get<string>({ url: Api.getNextCode });
/**
 * 导出api
 * @param params
 */
export const getExportUrl = Api.exportXls;
/**
 * 导入api
 */
export const getImportUrl = Api.importExcel;
/**
 * 列表接口
 * @param params
 */
export const list = (params) =>
  defHttp.get({url: Api.list, params});

/**
 * 删除单个
 */
export const deleteOne = (params,handleSuccess) => {
  return defHttp.delete({url: Api.deleteOne, params}, {joinParamsToUrl: true}).then(() => {
    handleSuccess();
  });
}
/**
 * 批量删除
 * @param params
 */
export const batchDelete = (params, handleSuccess) => {
  createConfirm({
    iconType: 'warning',
    title: '确认删除',
    content: '是否删除选中数据',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      return defHttp.delete({url: Api.deleteBatch, data: params}, {joinParamsToUrl: true}).then(() => {
        handleSuccess();
      });
    }
  });
}
/**
 * 保存或者更新
 * @param params
 */
export const saveOrUpdate = (params, isUpdate,showTip = true) => {
  const url = isUpdate ? Api.edit : Api.save;
  return defHttp.post({url: url, params},{successMessageMode:showTip?'success':'none'});
}
