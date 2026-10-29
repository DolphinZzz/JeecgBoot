import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/wms/wmsInventory/list',
  queryById = '/wms/wmsInventory/queryById',
  sumTotal = '/wms/wmsInventory/sumTotal',
  logList = '/wms/wmsInventoryLog/list',
}

/**
 * 多维库存列表
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

/**
 * 通过id查询库存行
 */
export const queryById = (id) => defHttp.get({ url: Api.queryById, params: { id } });

/**
 * 库存总量核对：库存数 = 流水的累计净额
 */
export const sumTotal = (params) => defHttp.get({ url: Api.sumTotal, params });

/**
 * 某一库存行的变动流水（库存中心穿透）
 */
export const queryLogList = (params) => defHttp.get({ url: Api.logList, params });
