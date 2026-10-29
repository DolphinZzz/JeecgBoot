import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/wms/wmsInventoryLog/list',
}

/**
 * 库存流水列表（只读，流水只由库存服务写入）
 */
export const list = (params) => defHttp.get({ url: Api.list, params });
