import { defHttp } from '/@/utils/http/axios';

enum Api {
  // 原有底层接口保留兼容
  list = '/wms/wmsStocktaking/list',
  create = '/wms/wmsStocktaking/create',
  submitCount = '/wms/wmsStocktaking/submitCount',
  audit = '/wms/wmsStocktaking/audit',
  confirm = '/wms/wmsStocktaking/confirm',
  queryById = '/wms/wmsStocktaking/queryById',

  // 盘库单单据化新接口
  orderList = '/wms/stocktakingOrder/list',
  orderQueryById = '/wms/stocktakingOrder/queryById',
  orderSaveDraft = '/wms/stocktakingOrder/saveDraft',
  orderComplete = '/wms/stocktakingOrder/complete',
  selectSkuPage = '/wms/stocktakingOrder/selectSkuPage',
  importWarehouseAllSkus = '/wms/stocktakingOrder/importWarehouseAllSkus',
  getSkuBookStock = '/wms/stocktakingOrder/getSkuBookStock',
  orderDelete = '/wms/stocktakingOrder/delete',
  orderCreateProfitEntry = '/wms/stocktakingOrder/createProfitEntry',
  orderCreateLossOutbound = '/wms/stocktakingOrder/createLossOutbound',
}

/** 盘点记录列表（旧） */
export const list = (params) => defHttp.get({ url: Api.list, params });
export const createStocktaking = (data) => defHttp.post({ url: Api.create, data }, { successMessageMode: 'none' });
export const submitCount = (data) => defHttp.post({ url: Api.submitCount, data }, { successMessageMode: 'none' });
export const audit = (params) => defHttp.post({ url: Api.audit, params }, { joinParamsToUrl: true, successMessageMode: 'none' });
export const confirm = (id) => defHttp.post({ url: Api.confirm, params: { id } }, { joinParamsToUrl: true, successMessageMode: 'none' });
export const queryById = (id) => defHttp.get({ url: Api.queryById, params: { id } });

/** 盘库单列表分页查询 */
export const getStocktakingOrderList = (params) => defHttp.get({ url: Api.orderList, params });

/** 查询盘库单详情（含主表及子表明细） */
export const getStocktakingOrderDetail = (id: number | string) => defHttp.get({ url: Api.orderQueryById, params: { id } });

/** 暂存盘库单草稿 */
export const saveDraftStocktakingOrder = (data: any) =>
  defHttp.post({ url: Api.orderSaveDraft, data }, { successMessageMode: 'none' });

/** 完成盘点结案（触发库存调账闭环） */
export const completeStocktakingOrder = (data: any) =>
  defHttp.post({ url: Api.orderComplete, data }, { successMessageMode: 'none' });

/** 盘库商品选择器多维检索分页 */
export const selectSkuPageForStocktaking = (params: any) =>
  defHttp.get({ url: Api.selectSkuPage, params });

/** 一键导入仓库下所有有库存商品 */
export const importWarehouseAllSkus = (warehouseId: number | string) =>
  defHttp.get({ url: Api.importWarehouseAllSkus, params: { warehouseId } });

/** 获取指定SKU在指定仓库的账面库存 */
export const getSkuBookStock = (warehouseId: number | string, skuId: number | string) =>
  defHttp.get({ url: Api.getSkuBookStock, params: { warehouseId, skuId } });

/** 删除草稿盘库单 */
export const deleteStocktakingOrder = (id: number | string) =>
  defHttp.delete({ url: Api.orderDelete, params: { id } }, { joinParamsToUrl: true });

/** 生成盘盈入库单 */
export const createProfitEntry = (orderId: number | string) =>
  defHttp.post({ url: Api.orderCreateProfitEntry, params: { orderId } }, { joinParamsToUrl: true });

/** 生成盘亏出库单 */
export const createLossOutbound = (orderId: number | string) =>
  defHttp.post({ url: Api.orderCreateLossOutbound, params: { orderId } }, { joinParamsToUrl: true });
