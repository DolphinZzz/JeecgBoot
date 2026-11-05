import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { getAutoCode } from '/@/views/system/autocode/autoCode.api';

const { createConfirm } = useMessage();

enum Api {
  list = '/mes/mesProcessRoute/list',
  save = '/mes/mesProcessRoute/add',
  edit = '/mes/mesProcessRoute/edit',
  deleteOne = '/mes/mesProcessRoute/delete',
  deleteBatch = '/mes/mesProcessRoute/deleteBatch',
  queryById = '/mes/mesProcessRoute/queryById',
  importExcel = '/mes/mesProcessRoute/importExcel',
  exportXls = '/mes/mesProcessRoute/exportXls',
  // 组成工序子表
  compositionList = '/mes/mesProcessRoute/composition/list',
  compositionAdd = '/mes/mesProcessRoute/composition/add',
  compositionEdit = '/mes/mesProcessRoute/composition/edit',
  compositionDelete = '/mes/mesProcessRoute/composition/delete',
  compositionDeleteBatch = '/mes/mesProcessRoute/composition/deleteBatch',
}

/**
 * 获取下一个工艺路线编号（调用 /sys/autocode/get 接口，工艺编码规则为 ROUTE_CODE）
 */
export const getNextRouteCode = () => getAutoCode('ROUTE_CODE');

export const getExportUrl = Api.exportXls;
export const getImportUrl = Api.importExcel;

/**
 * 工艺路线分页列表
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

/**
 * 保存或更新工艺路线
 */
export const saveOrUpdate = (params, isUpdate, showTip = true) => {
  const url = isUpdate ? Api.edit : Api.save;
  return defHttp.post({ url, params }, { successMessageMode: showTip ? 'success' : 'none' });
};

/**
 * 删除单条工艺路线
 */
export const deleteOne = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.deleteOne, params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess && handleSuccess();
  });
};

/**
 * 批量删除工艺路线
 */
export const batchDelete = (params, handleSuccess) => {
  createConfirm({
    iconType: 'warning',
    title: '确认删除',
    content: '是否删除选中数据',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      return defHttp.delete({ url: Api.deleteBatch, data: params }, { joinParamsToUrl: true }).then(() => {
        handleSuccess && handleSuccess();
      });
    },
  });
};

// ==================== 组成工序子表 API ====================

/**
 * 查询工艺路线下的组成工序列表
 */
export const listRouteCompositions = (routeId: string | number) =>
  defHttp.get({ url: Api.compositionList, params: { routeId } });

/**
 * 新增或修改工艺组成工序
 */
export const saveOrUpdateRouteComposition = (params, isUpdate) => {
  const url = isUpdate ? Api.compositionEdit : Api.compositionAdd;
  return defHttp.post({ url, params });
};

/**
 * 删除单条工艺组成工序
 */
export const deleteRouteComposition = (id: string | number) =>
  defHttp.delete({ url: Api.compositionDelete, params: { id } }, { joinParamsToUrl: true });

/**
 * 批量删除工艺组成工序
 */
export const batchDeleteRouteComposition = (ids: string) =>
  defHttp.delete({ url: Api.compositionDeleteBatch, params: { ids } }, { joinParamsToUrl: true });
