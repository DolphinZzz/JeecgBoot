import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/wms/wmsWarehouseOperations/list',
  add = '/wms/wmsWarehouseOperations/add',
  execute = '/wms/wmsWarehouseOperations/execute',
  cancel = '/wms/wmsWarehouseOperations/cancel',
  queryById = '/wms/wmsWarehouseOperations/queryById',
  queryDetails = '/wms/wmsWarehouseOperations/queryDetails',
}

/**
 * 移库作业单列表（operationType=1 移库移位）
 */
export const list = (params) => defHttp.get({ url: Api.list, params: { ...params, operationType: 1 } });

/**
 * 新建移库单
 */
export const createMove = (data) => defHttp.post({ url: Api.add, data }, { successMessageMode: 'none' });

/**
 * 执行作业单（后端 @RequestParam 接收，需拼到 query string）
 */
export const executeOperation = (id) => defHttp.post({ url: Api.execute, params: { id } }, { joinParamsToUrl: true });

/**
 * 取消作业单
 */
export const cancelOperation = (id) => defHttp.post({ url: Api.cancel, params: { id } }, { joinParamsToUrl: true });

/**
 * 作业单详情（含明细）
 */
export const queryById = (id) => defHttp.get({ url: Api.queryById, params: { id } });

/**
 * 作业单明细（分页返回，字典字段已被后端翻译成 _dictText）
 */
export const queryDetails = (id) => defHttp.get({ url: Api.queryDetails, params: { id } });
