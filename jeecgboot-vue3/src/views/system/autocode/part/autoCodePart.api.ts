import { defHttp } from '/@/utils/http/axios';
import { Modal } from 'ant-design-vue';

enum Api {
  list = '/sys/autoCodePart/list',
  save = '/sys/autoCodePart/add',
  edit = '/sys/autoCodePart/edit',
  delete = '/sys/autoCodePart/delete',
  deleteBatch = '/sys/autoCodePart/deleteBatch',
  queryById = '/sys/autoCodePart/queryById',
  queryRuleById = '/sys/autoCodeRule/queryById',
}

/**
 * 规则组成列表查询
 * @param params
 */
export const getAutoCodePartList = (params) => {
  return defHttp.get({ url: Api.list, params });
};

/**
 * 规则详情查询
 * @param id
 */
export const getAutoCodeRuleById = (id: string | number) => {
  return defHttp.get({ url: Api.queryRuleById, params: { id } });
};

/**
 * 保存规则组成
 * @param params
 */
export const saveAutoCodePart = (params) => {
  return defHttp.post({ url: Api.save, params });
};

/**
 * 更新规则组成
 * @param params
 */
export const updateAutoCodePart = (params) => {
  return defHttp.put({ url: Api.edit, params });
};

/**
 * 删除规则组成
 * @param params
 * @param handleSuccess
 */
export const deleteAutoCodePart = (params, handleSuccess) => {
  return defHttp.delete({ url: Api.delete, params }, { joinParamsToUrl: true }).then(() => {
    handleSuccess();
  });
};

/**
 * 批量删除规则组成
 * @param params
 * @param handleSuccess
 */
export const batchDeleteAutoCodePart = (params, handleSuccess) => {
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
