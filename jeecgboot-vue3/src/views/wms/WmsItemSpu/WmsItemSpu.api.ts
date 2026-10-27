import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';

const { createConfirm } = useMessage();

enum Api {
  list = '/wms/wmsItemSpu/list',
  save = '/wms/wmsItemSpu/add',
  edit = '/wms/wmsItemSpu/edit',
  deleteOne = '/wms/wmsItemSpu/delete',
  deleteBatch = '/wms/wmsItemSpu/deleteBatch',
  importExcel = '/wms/wmsItemSpu/importExcel',
  exportXls = '/wms/wmsItemSpu/exportXls',
  wmsItemSkuList = '/wms/wmsItemSpu/queryWmsItemSkuByMainId',
  deleteSkuBatch = '/wms/wmsItemSpu/deleteSkuBatch',
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
  let url = isUpdate ? Api.edit : Api.save;
  return defHttp.post({ url: url, params });
};

/**
 * SKU 查询地址，供弹窗加载子表
 */
export const queryWmsItemSkuByMainId = Api.wmsItemSkuList;

/**
 * SKU 查询函数，供展开行读取完整响应
 */
export const wmsItemSkuListApi = (params) => defHttp.get({ url: Api.wmsItemSkuList, params }, { isTransformResponse: false });

/**
 * 删除某个商品下选中的 SKU
 */
export const deleteSkuBatch = (params) => defHttp.delete({ url: Api.deleteSkuBatch, params }, { joinParamsToUrl: true });
