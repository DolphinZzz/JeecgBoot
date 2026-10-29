import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';

const { createConfirm } = useMessage();

enum Api {
  list = '/wms/wmsWarehouseAreaLoca/list',
  save = '/wms/wmsWarehouseAreaLoca/add',
  edit = '/wms/wmsWarehouseAreaLoca/edit',
  deleteOne = '/wms/wmsWarehouseAreaLoca/delete',
  deleteBatch = '/wms/wmsWarehouseAreaLoca/deleteBatch',
  importExcel = '/wms/wmsWarehouseAreaLoca/importExcel',
  exportXls = '/wms/wmsWarehouseAreaLoca/exportXls',
  recalcLocaScore = '/wms/wmsWarehouseAreaLoca/recalcLocaScore',
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
 * @param params
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

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
    content: '是否删除选中库位数据？',
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
 * @param params
 * @param isUpdate
 */
export const saveOrUpdate = (params, isUpdate) => {
  const url = isUpdate ? Api.edit : Api.save;
  return defHttp.post({ url: url, params }, { successMessageMode: 'none' });
};

/**
 * 重算库位 ABC 等级与得分
 * 后端用 @RequestParam 接收，必须走 query string；POST 默认会把 params 当 body 发送，
 * 因此显式开启 joinParamsToUrl 把参数拼到 URL 上。
 * @param warehouseId
 */
export const recalcLocaScore = (warehouseId: number) => {
  return defHttp.post({ url: Api.recalcLocaScore, params: { warehouseId } }, { joinParamsToUrl: true });
};
