import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/wms/wmsInventory/list',
  queryById = '/wms/wmsInventory/queryById',
  sumTotal = '/wms/wmsInventory/sumTotal',
  logList = '/wms/wmsInventoryLog/list',
  locaPageList = '/wms/wmsInventory/locaPageList',
  ledgerPageList = '/wms/wmsInventory/ledgerPageList',
  capacityListByLocaId = '/wms/wmsLocaItemCapacity/listByLocaId',
  capacitySaveBatch = '/wms/wmsLocaItemCapacity/saveBatch',
  spuOptions = '/wms/wmsLocaItemCapacity/spuOptions',
}

/**
 * 多维库存列表
 */
export const list = (params) => defHttp.get({ url: Api.list, params });
export const queryInventoryList = list;

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

/**
 * Tab 1: 库位列表（含商品规划与容量占用）
 */
export const queryLocaPageList = (params) => defHttp.get({ url: Api.locaPageList, params });

/**
 * Tab 2: 全周期库存台账
 */
export const queryLedgerPageList = (params) => defHttp.get({ url: Api.ledgerPageList, params });

/**
 * 查询指定库位的商品规划与容量列表
 */
export const queryCapacityListByLocaId = (locaId: number) =>
  defHttp.get({ url: Api.capacityListByLocaId, params: { locaId } });

/**
 * 保存库位商品规划配置（单库位最多3个SPU）
 */
export const saveLocaCapacityBatch = (data: any) =>
  defHttp.post({ url: Api.capacitySaveBatch, data });

/**
 * 获取SPU商品下拉选项列表
 */
export const querySpuOptions = (params?: any) =>
  defHttp.get({ url: Api.spuOptions, params });

