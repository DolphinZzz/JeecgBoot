import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/wms/wmsItemSn/list',
  queryBySn = '/wms/wmsItemSn/queryBySn',
}

/**
 * SN 追溯列表（只读，SN 由收货/复核环节写入）
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

/**
 * 按 SN 精确追溯单件
 */
export const queryBySn = (snCode: string) => defHttp.get({ url: Api.queryBySn, params: { snCode } });
