import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/wms/wmsOutboundOrder/list',
  queryById = '/wms/wmsOutboundOrder/queryById',
  queryDetails = '/wms/wmsOutboundOrder/queryDetails',
  add = '/wms/wmsOutboundOrder/add',
  allocate = '/wms/wmsOutboundOrder/allocate',
  confirmPick = '/wms/wmsOutboundOrder/confirmPick',
  confirmReview = '/wms/wmsOutboundOrder/confirmReview',
  packageAndWeigh = '/wms/wmsOutboundOrder/packageAndWeigh',
  confirmShip = '/wms/wmsOutboundOrder/confirmShip',
  cancel = '/wms/wmsOutboundOrder/cancel',
  queryPackages = '/wms/wmsOutboundOrder/queryPackages',
  queryWeights = '/wms/wmsOutboundOrder/queryWeights',
  querySns = '/wms/wmsOutboundOrder/querySns',
}

/**
 * 分页查询出库单
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

/**
 * 获取出库单主单详情
 */
export const queryById = (id) => defHttp.get({ url: Api.queryById, params: { id } });

/**
 * 获取出库单明细列表
 */
export const queryDetails = (id) => defHttp.get({ url: Api.queryDetails, params: { id } });

/**
 * 手工新建出库单
 */
export const createOutboundOrder = (params) =>
  defHttp.post({ url: Api.add, params }, { successMessageMode: 'none' });

/**
 * 手动触发库存 FEFO 分配锁库
 */
export const allocateInventory = (id) =>
  defHttp.post({ url: `${Api.allocate}?id=${id}` }, { successMessageMode: 'none' });

/**
 * 实拣确认与 SN 追溯绑定
 */
export const confirmPick = (params) =>
  defHttp.post({ url: Api.confirmPick, params }, { successMessageMode: 'none' });

/**
 * 出库复核
 */
export const confirmReview = (params) =>
  defHttp.post({ url: Api.confirmReview, params }, { successMessageMode: 'none' });

/**
 * 打包与称重校验
 */
export const packageAndWeight = (params) =>
  defHttp.post({ url: Api.packageAndWeigh, params }, { successMessageMode: 'none' });

/**
 * 物流发货（扣减库存）
 */
export const confirmShip = (params) =>
  defHttp.post({ url: Api.confirmShip, params }, { successMessageMode: 'none' });

/**
 * 逆向取消出库单
 */
export const cancelOutboundOrder = (id, reason) =>
  defHttp.post(
    { url: Api.cancel, params: { id, reason } },
    { joinParamsToUrl: true, successMessageMode: 'none' },
  );

/**
 * 获取包装箱单列表
 */
export const queryPackages = (id) => defHttp.get({ url: Api.queryPackages, params: { id } });

/**
 * 获取称重记录列表
 */
export const queryWeights = (id) => defHttp.get({ url: Api.queryWeights, params: { id } });

/**
 * 获取SN追溯列表
 */
export const querySns = (id) => defHttp.get({ url: Api.querySns, params: { id } });
