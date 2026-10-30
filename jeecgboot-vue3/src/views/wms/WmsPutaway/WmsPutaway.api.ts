import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/wms/wmsPutaway/list',
  recommend = '/wms/wmsPutaway/recommend',
  locaOptions = '/wms/wmsPutaway/locaOptions',
  confirm = '/wms/wmsPutaway/confirm',
}

/**
 * 上架记录列表
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

/**
 * 上架推荐（不落库，仅返回落点方案）
 */
export const recommend = (id) => defHttp.get({ url: Api.recommend, params: { id } });

/**
 * 人工指定库位的下拉选项：只列本仓库位，按库区分组，并带上每个库位的硬过滤结论
 */
export const listLocaOptions = (params) => defHttp.get({ url: Api.locaOptions, params });

/**
 * 确认上架落库
 */
export const confirmPutaway = (data) => defHttp.post({ url: Api.confirm, data }, { successMessageMode: 'none' });
