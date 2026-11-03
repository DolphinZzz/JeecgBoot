import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/wms/wmsWarehouseOperations/list',
  add = '/wms/wmsWarehouseOperations/add',
  execute = '/wms/wmsWarehouseOperations/execute',
  cancel = '/wms/wmsWarehouseOperations/cancel',
  queryById = '/wms/wmsWarehouseOperations/queryById',
  queryDetails = '/wms/wmsWarehouseOperations/queryDetails',
  previewAreaMigration = '/wms/wmsWarehouseOperations/previewAreaMigration',
  executeAreaMigration = '/wms/wmsWarehouseOperations/executeAreaMigration',
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
export const queryById = (params) => {
  const p = typeof params === 'object' && params !== null ? params : { id: params };
  return defHttp.get({ url: Api.queryById, params: p });
};

/**
 * 作业单明细（分页返回，字典字段已被后端翻译成 _dictText）
 */
export const queryDetails = (params) => {
  const p = typeof params === 'object' && params !== null ? params : { id: params };
  return defHttp.get({ url: Api.queryDetails, params: p });
};

/**
 * 整库区迁移预检与智能空库位配对
 */
export const previewAreaMigration = (params: {
  sourceWarehouseId: number;
  sourceWarehouseAreaId: number;
  targetWarehouseId: number;
  targetWarehouseAreaId: number;
}) => defHttp.get({ url: Api.previewAreaMigration, params });

/**
 * 执行整库区批量迁移
 */
export const executeAreaMigration = (data: any) =>
  defHttp.post({ url: Api.executeAreaMigration, data }, { successMessageMode: 'none' });
