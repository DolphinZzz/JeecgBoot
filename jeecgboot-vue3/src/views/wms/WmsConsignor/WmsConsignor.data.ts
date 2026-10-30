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
    title: '货主编码',
    align: 'center',
    width: 120,
    dataIndex: 'consignorCode',
  },
  {
    title: '货主名称',
    align: 'left',
    dataIndex: 'consignorName',
  },
  {
    title: '货主类型',
    align: 'center',
    width: 110,
    dataIndex: 'consignorType_dictText',
  },
  {
    title: '联系人',
    align: 'center',
    width: 100,
    dataIndex: 'linkName',
  },
  {
    title: '联系电话',
    align: 'center',
    width: 130,
    dataIndex: 'linkPhone',
  },
  {
    title: '所在省份',
    align: 'center',
    width: 110,
    dataIndex: 'provinceId_dictText',
  },
  {
    title: '所在城市',
    align: 'center',
    width: 110,
    dataIndex: 'cityCode_dictText',
  },
  {
    title: '详细地址',
    align: 'left',
    dataIndex: 'address',
  },
  {
    title: '启用状态',
    align: 'center',
    width: 100,
    dataIndex: 'enableStatus_dictText',
    customRender: ({ record, text }) => (record.enableStatus === 1 ? '启用' : text || '停用'),
  },
  {
    title: '备注',
    align: 'left',
    dataIndex: 'remark',
  },
  {
    title: '创建人',
    align: 'center',
    width: 100,
    dataIndex: 'createBy',
  },
  {
    title: '创建日期',
    align: 'center',
    width: 160,
    dataIndex: 'createTime',
  },
];

// 查询表单 Schema
export const searchFormSchema: FormSchema[] = [
  {
    label: '货主编码',
    field: 'consignorCode',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: '货主名称',
    field: 'consignorName',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: '货主类型',
    field: 'consignorType',
    component: 'JDictSelectTag',
    componentProps: { dictCode: 'wms_consignor_type', stringToNumber: true, placeholder: '请选择' },
    colProps: { span: 6 },
  },
  {
    label: '联系人',
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
    label: '启用状态',
    field: 'enableStatus',
    component: 'JDictSelectTag',
    componentProps: { dictCode: 'valid_status', stringToNumber: true, placeholder: '请选择' },
    colProps: { span: 6 },
  },
];

// 编辑表单 Schema - 首位放隐藏 id
export const formSchema: FormSchema[] = [
  {
    label: '',
    field: 'id',
    component: 'Input',
    show: false,
  },
  {
    label: '货主名称',
    field: 'consignorName',
    component: 'Input',
    componentProps: { placeholder: '请输入货主名称', maxlength: 100 },
    dynamicRules: () => [{ required: true, message: '请输入货主名称!' }],
  },
  {
    label: '货主类型',
    field: 'consignorType',
    component: 'JDictSelectTag',
    defaultValue: 1,
    required: true,
    componentProps: { dictCode: 'wms_consignor_type', stringToNumber: true },
  },
  {
    label: '联系人',
    field: 'linkName',
    component: 'Input',
    componentProps: { placeholder: '请输入联系人', maxlength: 50 },
  },
  {
    label: '联系电话',
    field: 'linkPhone',
    component: 'Input',
    componentProps: { placeholder: '请输入联系电话', maxlength: 20 },
    dynamicRules: () => [
      {
        validator: async (_rule, value) => {
          if (value === undefined || value === null || String(value).trim() === '') {
            return Promise.resolve();
          }
          // 允许手机号与带区号的座机，两种仓库/货主场景都会用到
          if (!/^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8})$/.test(String(value).trim())) {
            return Promise.reject('请输入正确的手机号或座机号!');
          }
          return Promise.resolve();
        },
      },
    ],
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
    label: '详细地址',
    field: 'address',
    component: 'InputTextArea',
    componentProps: { placeholder: '请输入详细地址', rows: 3, maxlength: 255 },
  },
  {
    label: '启用状态',
    field: 'enableStatus',
    component: 'JDictSelectTag',
    defaultValue: 1,
    required: true,
    componentProps: { dictCode: 'valid_status', stringToNumber: true },
    helpMessage: '停用后不再出现在入库单等货主下拉里，历史单据不受影响',
  },
  {
    label: '备注',
    field: 'remark',
    component: 'InputTextArea',
    componentProps: { placeholder: '选填', rows: 3, maxlength: 500 },
  },
];

// 高级查询配置
export const superQuerySchema = {
  consignorCode: { title: '货主编码', order: 0, view: 'text', type: 'string' },
  consignorName: { title: '货主名称', order: 1, view: 'text', type: 'string' },
  consignorType: { title: '货主类型', order: 2, view: 'list', type: 'int', dictCode: 'wms_consignor_type' },
  linkName: { title: '联系人', order: 3, view: 'text', type: 'string' },
  linkPhone: { title: '联系电话', order: 4, view: 'text', type: 'string' },
  provinceId: {
    title: '所在省份',
    order: 5,
    view: 'sel_search',
    type: 'int',
    dictTable: 'base_province',
    dictCode: 'id',
    dictText: 'name',
  },
  cityCode: {
    title: '所在城市',
    order: 6,
    view: 'sel_search',
    type: 'int',
    dictTable: 'base_city',
    dictCode: 'id',
    dictText: 'name',
  },
  address: { title: '详细地址', order: 7, view: 'text', type: 'string' },
  enableStatus: { title: '启用状态', order: 8, view: 'list', type: 'int', dictCode: 'valid_status' },
  createTime: { title: '创建日期', order: 9, view: 'datetime', type: 'string' },
};
