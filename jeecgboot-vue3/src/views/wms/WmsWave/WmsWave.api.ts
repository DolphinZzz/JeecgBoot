import { defHttp } from '/@/utils/http/axios';

enum Api {
  list = '/wms/wmsWaveOrder/list',
  queryById = '/wms/wmsWaveOrder/queryById',
  generate = '/wms/wmsWaveOrder/generate',
  startPick = '/wms/wmsWaveOrder/startPick',
  completeDistribute = '/wms/wmsWaveOrder/completeDistribute',
  cancel = '/wms/wmsWaveOrder/cancel',
}

/**
 * 分页查询波次列表
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

/**
 * 获取波次全量详情（单头、关联出库单、汇总拣货行）
 */
export const queryById = (id) => defHttp.get({ url: Api.queryById, params: { id } });

/**
 * 自动分波生成
 */
export const generateWaves = (params) =>
  defHttp.post({ url: Api.generate, params }, { successMessageMode: 'none' });

/**
 * 开始波次拣货
 */
export const startWavePick = (id, empId) =>
  defHttp.post(
    { url: Api.startPick, params: { id, empId } },
    { joinParamsToUrl: true, successMessageMode: 'none' },
  );

/**
 * 完成播种分货（出库单批量流转至 50待复核）
 */
export const completeWaveDistribute = (id) =>
  defHttp.post(
    { url: Api.completeDistribute, params: { id } },
    { joinParamsToUrl: true, successMessageMode: 'none' },
  );

/**
 * 取消波次
 */
export const cancelWave = (id) =>
  defHttp.post(
    { url: Api.cancel, params: { id } },
    { joinParamsToUrl: true, successMessageMode: 'none' },
  );
