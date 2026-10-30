import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/wms/wmsWarehousingEntry/list',
  add = '/wms/wmsWarehousingEntry/add',
  queryById = '/wms/wmsWarehousingEntry/queryById',
  queryDetails = '/wms/wmsWarehousingEntry/queryDetails',
  receive = '/wms/wmsWarehousingEntry/receive',
  submitToQc = '/wms/wmsWarehousingEntry/submitToQc',
  auditOverReceive = '/wms/wmsWarehousingEntry/auditOverReceive',
  queryPutawayRecords = '/wms/wmsWarehousingEntry/queryPutawayRecords',
  cancel = '/wms/wmsWarehousingEntry/cancel',
}

/**
 * 入库单列表
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

/**
 * 新建入库单
 */
export const addEntry = (data) => defHttp.post({ url: Api.add, data }, { successMessageMode: 'none' });

/**
 * 入库单详情
 */
export const queryById = (id) => defHttp.get({ url: Api.queryById, params: { id } });

/**
 * 入库明细
 */
export const queryDetails = (id) => defHttp.get({ url: Api.queryDetails, params: { id } });

/**
 * 收货清点
 */
export const receive = (data) => defHttp.post({ url: Api.receive, data }, { successMessageMode: 'none' });

/**
 * 收货完成，提交质检（后端 @RequestParam 接收）
 */
export const submitToQc = (id) => defHttp.post({ url: Api.submitToQc, params: { id } }, { joinParamsToUrl: true, successMessageMode: 'none' });

/**
 * 超收放行 / 驳回（后端 @RequestParam 接收）
 */
export const auditOverReceive = (params) =>
  defHttp.post({ url: Api.auditOverReceive, params }, { joinParamsToUrl: true, successMessageMode: 'none' });

/**
 * 上架记录
 */
export const queryPutawayRecords = (id) => defHttp.get({ url: Api.queryPutawayRecords, params: { id } });

/**
 * 取消入库单
 */
export const cancelEntry = (id) => defHttp.post({ url: Api.cancel, params: { id } }, { joinParamsToUrl: true, successMessageMode: 'none' });

/**
 * 商品选择器分页列表
 */
export const querySkuPageForSelect = (params) => defHttp.get({ url: '/wms/wmsItemSpu/querySkuPageForSelect', params });
