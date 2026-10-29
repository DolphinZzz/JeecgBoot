import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/wms/wmsWarehouseOperations/list',
  generateReplenish = '/wms/wmsWarehouseOperations/generateReplenish',
  execute = '/wms/wmsWarehouseOperations/execute',
  cancel = '/wms/wmsWarehouseOperations/cancel',
  queryById = '/wms/wmsWarehouseOperations/queryById',
  queryDetails = '/wms/wmsWarehouseOperations/queryDetails',
}

/**
 * 补货作业单列表（operationType=2 补货）
 */
export const list = (params) => defHttp.get({ url: Api.list, params: { ...params, operationType: 2 } });

/**
 * 按拣货区上下限自动生成补货单
 * 后端 @RequestParam 接收，必须拼到 query string；generateReplenish 若无可补货 SKU 会抛异常提示。
 */
export const generateReplenish = (params) =>
  defHttp.post({ url: Api.generateReplenish, params }, { joinParamsToUrl: true, successMessageMode: 'none' });

/**
 * 执行作业单
 */
export const executeOperation = (id) => defHttp.post({ url: Api.execute, params: { id } }, { joinParamsToUrl: true });

/**
 * 取消作业单
 */
export const cancelOperation = (id) => defHttp.post({ url: Api.cancel, params: { id } }, { joinParamsToUrl: true });

/**
 * 补货单详情
 */
export const queryById = (id) => defHttp.get({ url: Api.queryById, params: { id } });

/**
 * 补货单明细（分页返回，字典字段已被后端翻译成 _dictText）
 */
export const queryDetails = (id) => defHttp.get({ url: Api.queryDetails, params: { id } });
