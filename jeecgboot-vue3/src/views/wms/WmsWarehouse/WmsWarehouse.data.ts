import { BasicColumn, FormSchema } from '/@/components/Table';

const MUNICIPALITY_PROVINCE_IDS = new Set([1, 2, 3, 4]);

export function isMunicipalityProvince(provinceId) {
  const id = Number(provinceId);
  return Number.isInteger(id) && MUNICIPALITY_PROVINCE_IDS.has(id);
}

function buildCityDictCode(provinceId) {
  if (isMunicipalityProvince(provinceId)) {
    return 'base_city,name,id,pid=0';
  }
  const pid = provinceId === undefined || provinceId === null || provinceId === '' ? '' : String(provinceId);
  if (!/^\d+$/.test(pid)) {
    return 'base_city,name,id,pid=0';
  }
  return `base_city,name,id,pid=${pid}`;
}

function provinceComponentProps({ formActionType, formModel }) {
  return {
    dictCode: 'base_province,name,id',
    stringToNumber: true,
    placeholder: '请选择省/直辖市',
    getPopupContainer: () => document.body,
    onChange: (value) => {
      const current = formModel.provinceId;
      if (current !== value && String(current ?? '') !== String(value ?? '')) {
        formActionType.setFieldsValue({ cityId: undefined });
      }
    },
  };
}

function cityComponentProps({ formModel }) {
  const municipality = isMunicipalityProvince(formModel.provinceId);
  return {
    dictCode: buildCityDictCode(formModel.provinceId),
    stringToNumber: true,
    placeholder: municipality ? '直辖市无需选择城市' : '请选择城市',
    getPopupContainer: () => document.body,
  };
}

const cityDisabled = ({ values }) => {
  if (values.provinceId === undefined || values.provinceId === null || values.provinceId === '') {
    return true;
  }
  return isMunicipalityProvince(values.provinceId);
};

// 列表列定义
export const columns: BasicColumn[] = [
  {
    title: '仓库名称',
    align: 'center',
    dataIndex: 'name',
  },
  {
    title: '仓库类型',
    align: 'center',
    dataIndex: 'warehouseType_dictText',
    customRender: ({ text }) => text || '普通仓',
  },
  {
    title: '负责人',
    align: 'center',
    dataIndex: 'personChargeId_dictText',
  },
  {
    title: '省份',
    align: 'center',
    dataIndex: 'provinceId_dictText',
  },
  {
    title: '城市',
    align: 'center',
    dataIndex: 'cityId_dictText',
  },
  {
    title: '详细地址',
    align: 'center',
    dataIndex: 'address',
  },
  {
    title: '状态',
    align: 'center',
    dataIndex: 'enableStatus_dictText',
    customRender: ({ text, record }) => {
      const isNormal = record.enableStatus === 1;
      return isNormal ? '正常' : text || '停用';
    },
  },
  {
    title: '排序',
    align: 'center',
    dataIndex: 'sort',
  },
  {
    title: '创建时间',
    align: 'center',
    dataIndex: 'createTime',
  },
];

// 查询表单 Schema
export const searchFormSchema: FormSchema[] = [
  {
    label: '仓库名称',
    field: 'name',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: '仓库类型',
    field: 'warehouseType',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_warehouse_type',
      placeholder: '请选择仓库类型',
    },
    colProps: { span: 6 },
  },
  {
    label: '启用状态',
    field: 'enableStatus',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'valid_status',
      stringToNumber: true,
      placeholder: '请选择状态',
    },
    colProps: { span: 6 },
  },
];

// 编辑表单 Schema
export const formSchema: FormSchema[] = [
  {
    label: '',
    field: 'id',
    component: 'Input',
    show: false,
  },
  {
    label: '仓库名称',
    field: 'name',
    component: 'Input',
    required: true,
    componentProps: {
      placeholder: '请输入仓库名称',
      maxlength: 200,
    },
  },
  {
    label: '仓库类型',
    field: 'warehouseType',
    component: 'JDictSelectTag',
    defaultValue: 'NORMAL',
    required: true,
    componentProps: {
      dictCode: 'wms_warehouse_type',
      placeholder: '请选择仓库类型',
    },
  },
  {
    label: '负责人',
    field: 'personChargeId',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'sys_user,realname,id',
      placeholder: '请选择负责人',
    },
  },
  {
    label: '所在省份',
    field: 'provinceId',
    component: 'JDictSelectTag',
    componentProps: provinceComponentProps,
  },
  {
    label: '所在城市',
    field: 'cityId',
    component: 'JDictSelectTag',
    dynamicDisabled: cityDisabled,
    componentProps: cityComponentProps,
  },
  {
    label: '详细地址',
    field: 'address',
    component: 'Input',
    componentProps: {
      placeholder: '请输入详细地址',
      maxlength: 255,
    },
  },
  {
    label: '排序',
    field: 'sort',
    component: 'InputNumber',
    defaultValue: 1,
    componentProps: {
      placeholder: '请输入排序值',
      min: 1,
    },
  },
  {
    label: '启用状态',
    field: 'enableStatus',
    component: 'JDictSelectTag',
    defaultValue: 1,
    required: true,
    componentProps: {
      dictCode: 'valid_status',
      stringToNumber: true,
      placeholder: '请选择状态',
    },
  },
];

export const superQuerySchema = {
  name: { title: '仓库名称', order: 1, view: 'text', type: 'string' },
  warehouseType: { title: '仓库类型', order: 2, view: 'list', type: 'string', dictCode: 'wms_warehouse_type' },
  enableStatus: { title: '状态', order: 3, view: 'number', type: 'number', dictCode: 'valid_status' },
};
