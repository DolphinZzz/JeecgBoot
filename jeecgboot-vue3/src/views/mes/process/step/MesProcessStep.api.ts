import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { getAutoCode } from '/@/views/system/autocode/autoCode.api';

const { createConfirm } = useMessage();

enum Api {
  list = '/mes/mesProcessStep/list',
  listEnabled = '/mes/mesProcessStep/listEnabled',
  save = '/mes/mesProcessStep/add',
  edit = '/mes/mesProcessStep/edit',
  deleteOne = '/mes/mesProcessStep/delete',
  deleteBatch = '/mes/mesProcessStep/deleteBatch',
  queryById = '/mes/mesProcessStep/queryById',
  importExcel = '/mes/mesProcessStep/importExcel',
  exportXls = '/mes/mesProcessStep/exportXls',
  // 操作步骤子表
  operationList = '/mes/mesProcessStep/operation/list',
  operationAdd = '/mes/mesProcessStep/operation/add',
  operationEdit = '/mes/mesProcessStep/operation/edit',
  operationDelete = '/mes/mesProcessStep/operation/delete',
  operationDeleteBatch = '/mes/mesProcessStep/operation/deleteBatch',
}

/**
 * 获取下一个生产工序编码（调用 /sys/autocode/get 接口，规则编码为 PROCESS_CODE）
 */
export const getNextProcessCode = () => getAutoCode('PROCESS_CODE');

export const getExportUrl = Api.exportXls;
export const getImportUrl = Api.importExcel;

/**
 * 生产工序分页列表
 */
export const list = (params) => defHttp.get({ url: Api.list, params });

/**
 * 查询全部启用的生产工序（供工艺路线-添加工艺组成下拉选择）
 */
export const listEnabledProcessSteps = () => defHttp.get({ url: Api.listEnabled });

/**
 * 保存或更新生产工序
 */
export const saveOrUpdate = (params, isUpdate, showTip = true) => {
  const url = isUpdate ? Api.edit : Api.save;
  return defHttp.post({ url, params }, { successMessageMode: showTip ? 'success' : 'none' });
};

/**
 * 删除单个生产工序
 */
export const deleteOne = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.deleteOne, params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess && handleSuccess();
  });
};

/**
 * 批量删除生产工序
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

// ==================== 操作步骤明细子表 API ====================

/**
 * 查询工序下的操作步骤列表
 */
export const listStepOperations = (processStepId: string | number) =>
  defHttp.get({ url: Api.operationList, params: { processStepId } });

/**
 * 保存或修改操作步骤
 */
export const saveOrUpdateStepOperation = (params, isUpdate) => {
  const url = isUpdate ? Api.operationEdit : Api.operationAdd;
  return defHttp.post({ url, params });
};

/**
 * 删除单条操作步骤
 */
export const deleteStepOperation = (id: string | number) =>
  defHttp.delete({ url: Api.operationDelete, params: { id } }, { joinParamsToUrl: true });

/**
 * 批量删除操作步骤
 */
export const batchDeleteStepOperation = (ids: string) =>
  defHttp.delete({ url: Api.operationDeleteBatch, params: { ids } }, { joinParamsToUrl: true });
