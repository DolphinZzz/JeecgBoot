import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';

enum Api {
  list = '/sys/autoCodeRule/list',
  save = '/sys/autoCodeRule/add',
  edit = '/sys/autoCodeRule/edit',
  delete = '/sys/autoCodeRule/delete',
  deleteBatch = '/sys/autoCodeRule/deleteBatch',
  queryById = '/sys/autoCodeRule/queryById',
  exportXls = '/sys/autoCodeRule/exportXls',
  importExcel = '/sys/autoCodeRule/importExcel',
}

export const exportUrl = Api.exportXls;
export const importUrl = Api.importExcel;

/**
 * 列表查询
 * @param params
 */
export const getAutoCodeRuleList = (params) => {
  return defHttp.get({ url: Api.list, params });
};

/**
 * 保存
 * @param params
 */
export const saveAutoCodeRule = (params) => {
  return defHttp.post({ url: Api.save, params });
};

/**
 * 更新
 * @param params
 */
export const updateAutoCodeRule = (params) => {
  return defHttp.put({ url: Api.edit, params });
};

/**
 * 删除
 * @param params
 * @param handleSuccess
 */
export const deleteAutoCodeRule = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.delete, params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};

/**
 * 批量删除
 * @param params
 * @param handleSuccess
 */
export const batchDeleteAutoCodeRule = (params, handleSuccess) => {
  Modal.confirm({
    title: '确认删除',
    content: '是否删除选中数据？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      return defHttp.delete({ url: Api.deleteBatch, params }, { joinParamsToUrl: true }).then(() => {
        handleSuccess();
      });
    },
  });
};
