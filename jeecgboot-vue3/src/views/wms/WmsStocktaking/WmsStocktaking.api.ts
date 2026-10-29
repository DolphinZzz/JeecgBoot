import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/wms/wmsStocktaking/list',
  create = '/wms/wmsStocktaking/create',
  submitCount = '/wms/wmsStocktaking/submitCount',
  audit = '/wms/wmsStocktaking/audit',
  confirm = '/wms/wmsStocktaking/confirm',
  queryById = '/wms/wmsStocktaking/queryById',
}

/**
 * 盘点记录列表
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

/**
 * 生成盘点批次
 */
export const createStocktaking = (data) => defHttp.post({ url: Api.create, data }, { successMessageMode: 'none' });

/**
 * 录入实盘数
 */
export const submitCount = (data) => defHttp.post({ url: Api.submitCount, data }, { successMessageMode: 'none' });

/**
 * 差异审批（后端 @RequestParam 接收，拼到 query string）
 */
export const audit = (params) => defHttp.post({ url: Api.audit, params }, { joinParamsToUrl: true, successMessageMode: 'none' });

/**
 * 结案调账
 */
export const confirm = (id) => defHttp.post({ url: Api.confirm, params: { id } }, { joinParamsToUrl: true, successMessageMode: 'none' });

/**
 * 盘点记录详情
 */
export const queryById = (id) => defHttp.get({ url: Api.queryById, params: { id } });
