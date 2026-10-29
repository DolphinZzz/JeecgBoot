import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/wms/wmsQualityCheck/list',
  queryById = '/wms/wmsQualityCheck/queryById',
  submit = '/wms/wmsQualityCheck/submit',
}

/**
 * 质检单列表
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

/**
 * 质检单详情
 */
export const queryById = (id) => defHttp.get({ url: Api.queryById, params: { id } });

/**
 * 提交质检结果（可一次提交多张）
 */
export const submitQc = (data) => defHttp.post({ url: Api.submit, data }, { successMessageMode: 'none' });
