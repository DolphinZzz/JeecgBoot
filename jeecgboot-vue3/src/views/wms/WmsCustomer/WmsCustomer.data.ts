import { BasicColumn, FormSchema } from '/@/components/Table';

/** 直辖市：北京、上海、天津、重庆。这些记录在 base_province 中，下级是区，不能再选城市。 */
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
        formActionType.setFieldsValue({ cityCode: undefined });
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
    title: '客户名称',
    align: 'center',
    dataIndex: 'customerName',
  },
  {
    title: '负责人',
    align: 'center',
    dataIndex: 'linkName',
  },
  {
    title: '联系电话',
    align: 'center',
    dataIndex: 'linkPhone',
  },
  {
    title: '所在省份',
    align: 'center',
    dataIndex: 'provinceId_dictText',
  },
  {
    title: '所在城市',
    align: 'center',
    dataIndex: 'cityCode_dictText',
  },
  {
    title: '详情地址',
    align: 'center',
    dataIndex: 'address',
  },
  {
    title: '创建人',
    align: 'center',
    dataIndex: 'createBy',
  },
  {
    title: '创建日期',
    align: 'center',
    dataIndex: 'createTime',
  },
];

// 查询表单 Schema
export const searchFormSchema: FormSchema[] = [
  {
    label: '客户名称',
    field: 'customerName',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: '负责人',
    field: 'linkName',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: '联系电话',
    field: 'linkPhone',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: '所在省份',
    field: 'provinceId',
    component: 'JDictSelectTag',
    componentProps: provinceComponentProps,
    colProps: { span: 6 },
  },
  {
    label: '所在城市',
    field: 'cityCode',
    component: 'JDictSelectTag',
    componentProps: cityComponentProps,
    dynamicDisabled: cityDisabled,
    colProps: { span: 6 },
  },
  {
    label: '详情地址',
    field: 'address',
    component: 'JInput',
    colProps: { span: 6 },
  },
];

// 编辑表单 Schema - FormSchema 必含首位隐藏 id 字段！
export const formSchema: FormSchema[] = [
  {
    label: '',
    field: 'id',
    component: 'Input',
    show: false,
  },
  {
    label: '客户名称',
    field: 'customerName',
    component: 'Input',
    componentProps: { placeholder: '请输入客户名称' },
    dynamicRules: () => {
      return [{ required: true, message: '请输入客户名称!' }];
    },
  },
  {
    label: '负责人',
    field: 'linkName',
    component: 'Input',
    componentProps: { placeholder: '请输入负责人' },
  },
  {
    label: '联系电话',
    field: 'linkPhone',
    component: 'Input',
    componentProps: { placeholder: '请输入联系电话' },
    dynamicRules: () => {
      return [
        {
          validator: async (_rule, value) => {
            if (value === undefined || value === null || String(value).trim() === '') {
              return Promise.resolve();
            }
            if (!/^1[3456789]\d{9}$/.test(String(value))) {
              return Promise.reject('请输入正确的手机号码!');
            }
            return Promise.resolve();
          },
        },
      ];
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
    field: 'cityCode',
    component: 'JDictSelectTag',
    componentProps: cityComponentProps,
    dynamicDisabled: cityDisabled,
  },
  {
    label: '详情地址',
    field: 'address',
    component: 'InputTextArea',
    componentProps: { placeholder: '请输入详情地址', rows: 4 },
  },
];

// 高级查询配置
export const superQuerySchema = {
  customerName: { title: '客户名称', order: 0, view: 'text', type: 'string' },
  linkName: { title: '负责人', order: 1, view: 'text', type: 'string' },
  linkPhone: { title: '联系电话', order: 2, view: 'text', type: 'string' },
  provinceId: { title: '所在省份', order: 3, view: 'sel_search', type: 'number', dictTable: 'base_province', dictCode: 'id', dictText: 'name' },
  cityCode: { title: '所在城市', order: 4, view: 'sel_search', type: 'number', dictTable: 'base_city', dictCode: 'id', dictText: 'name' },
  address: { title: '详情地址', order: 5, view: 'text', type: 'string' },
  createBy: { title: '创建人', order: 6, view: 'text', type: 'string' },
  createTime: { title: '创建日期', order: 7, view: 'datetime', type: 'string' },
};
