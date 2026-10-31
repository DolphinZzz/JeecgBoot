import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/wms/wmsQualityCheck/list',
  queryById = '/wms/wmsQualityCheck/queryById',
  submit = '/wms/wmsQualityCheck/submit',
  returnLocaOptions = '/wms/wmsQualityCheck/returnLocaOptions',
  handleReturn = '/wms/wmsQualityCheck/handleReturn',
  shipReturn = '/wms/wmsQualityCheck/shipReturn',
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

/**
 * 查询退货不良区货架库位选项
 */
export const listReturnLocaOptions = (params: { qcId?: number | string; warehouseId?: number | string }) =>
  defHttp.get({ url: Api.returnLocaOptions, params });

/**
 * 质检不合格品退货处理（分流放置退货货架区并联系供应商）
 */
export const handleReturn = (data: { qcId: number | string; returnLocaId: number | string; returnRemark?: string }) =>
  defHttp.post({ url: Api.handleReturn, data });

/**
 * 确认退还供应商出库
 */
export const shipReturn = (data: { qcId: number | string; shipRemark?: string }) =>
  defHttp.post({ url: Api.shipReturn, data });

