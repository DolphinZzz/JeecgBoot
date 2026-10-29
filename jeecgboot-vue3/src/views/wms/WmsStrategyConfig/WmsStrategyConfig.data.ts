import { BasicColumn, FormSchema } from '/@/components/Table';

// 列表列定义
export const columns: BasicColumn[] = [
  {
    title: '适用范围',
    align: 'center',
    dataIndex: 'warehouseId',
    customRender: ({ text, record }) => {
      if (text === 0 || text === '0' || text === null || text === undefined) {
        return '全局默认';
      }
      return record.warehouseId_dictText || `仓库 #${text}`;
    },
  },
  {
    title: '参数分组',
    align: 'center',
    dataIndex: 'configGroup',
    customRender: ({ text }) => {
      const map: Record<string, string> = {
        LOCA: '库位等级 (LOCA)',
        PUTAWAY: '上架策略 (PUTAWAY)',
        ABC: '商品ABC (ABC)',
        ZONE: '库区分区 (ZONE)',
        GENERAL: '通用配置 (GENERAL)',
      };
      return map[text] || text || '-';
    },
  },
  {
    title: '参数键 (Key)',
    align: 'center',
    dataIndex: 'configKey',
  },
  {
    title: '参数值 (Value)',
    align: 'center',
    dataIndex: 'configValue',
  },
  {
    title: '参数说明',
    align: 'left',
    dataIndex: 'remark',
  },
  {
    title: '更新时间',
    align: 'center',
    dataIndex: 'updateTime',
  },
];

// 查询表单 Schema
export const searchFormSchema: FormSchema[] = [
  {
    label: '所属仓库',
    field: 'warehouseId',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_warehouse,name,id',
      stringToNumber: true,
      placeholder: '选择仓库(空查全部)',
    },
    colProps: { span: 6 },
  },
  {
    label: '参数分组',
    field: 'configGroup',
    component: 'Select',
    componentProps: {
      options: [
        { label: '库位等级 (LOCA)', value: 'LOCA' },
        { label: '上架策略 (PUTAWAY)', value: 'PUTAWAY' },
        { label: '商品ABC (ABC)', value: 'ABC' },
        { label: '库区分区 (ZONE)', value: 'ZONE' },
        { label: '通用配置 (GENERAL)', value: 'GENERAL' },
      ],
      placeholder: '请选择分组',
    },
    colProps: { span: 6 },
  },
  {
    label: '参数键',
    field: 'configKey',
    component: 'JInput',
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
    label: '所属仓库',
    field: 'warehouseId',
    component: 'JDictSelectTag',
    defaultValue: 0,
    helpMessage: '0为全局默认配置；指定仓库后可覆盖全局参数',
    componentProps: {
      dictCode: 'wms_warehouse,name,id',
      stringToNumber: true,
      placeholder: '请选择仓库（留空/0为全局默认）',
    },
  },
  {
    label: '参数分组',
    field: 'configGroup',
    component: 'Select',
    defaultValue: 'LOCA',
    required: true,
    componentProps: {
      options: [
        { label: '库位等级策略 (LOCA)', value: 'LOCA' },
        { label: '上架推荐策略 (PUTAWAY)', value: 'PUTAWAY' },
        { label: '商品ABC策略 (ABC)', value: 'ABC' },
        { label: '库区分区策略 (ZONE)', value: 'ZONE' },
        { label: '通用参数配置 (GENERAL)', value: 'GENERAL' },
      ],
    },
  },
  {
    label: '参数键 (Key)',
    field: 'configKey',
    component: 'Input',
    required: true,
    helpMessage: '常用: loca_weight_distance, loca_weight_ergonomic, loca_a_ratio, loca_b_ratio, putaway_default_strategy 等',
    componentProps: {
      placeholder: '请输入参数键',
      maxlength: 100,
    },
  },
  {
    label: '参数值 (Value)',
    field: 'configValue',
    component: 'Input',
    required: true,
    componentProps: {
      placeholder: '请输入参数值',
      maxlength: 255,
    },
  },
  {
    label: '参数说明',
    field: 'remark',
    component: 'InputTextArea',
    componentProps: {
      placeholder: '请输入参数使用场景与业务含义说明',
      rows: 3,
      maxlength: 255,
    },
  },
];

export const superQuerySchema = {
  configGroup: { title: '参数分组', order: 1, view: 'list', type: 'string' },
  configKey: { title: '参数键', order: 2, view: 'text', type: 'string' },
  configValue: { title: '参数值', order: 3, view: 'text', type: 'string' },
};
