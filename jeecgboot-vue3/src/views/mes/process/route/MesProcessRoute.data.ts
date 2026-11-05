import { BasicColumn, FormSchema } from '/@/components/Table';

/**
 * 与下一道工序关系规则选项定义
 */
export const nextRelationOptions = [
  {
    label: 'S-to-S（当前工序开始生产，下一道工序才可开始生产）',
    shortLabel: 'S-to-S',
    value: 'S-to-S',
  },
  {
    label: 'F-to-F（当前工序结束生产，下一道工序才可结束生产）',
    shortLabel: 'F-to-F',
    value: 'F-to-F',
  },
  {
    label: 'S-to-F（当前工序开始生产，下一道工序才可结束生产）',
    shortLabel: 'S-to-F',
    value: 'S-to-F',
  },
  {
    label: 'F-to-S（当前工序结束生产，下一道工序才可开始生产）',
    shortLabel: 'F-to-S',
    value: 'F-to-S',
  },
];

/**
 * 工艺流程台账列表显示字段：
 * 工艺路线编号、工艺路线名称、工艺路线说明、是否启用、备注、操作
 */
export const columns: BasicColumn[] = [
  {
    title: '工艺路线编号',
    align: 'center',
    dataIndex: 'routeCode',
    width: 160,
  },
  {
    title: '工艺路线名称',
    align: 'center',
    dataIndex: 'routeName',
    width: 200,
  },
  {
    title: '工艺路线说明',
    align: 'center',
    dataIndex: 'description',
  },
  {
    title: '是否启用',
    align: 'center',
    dataIndex: 'isEnabled',
    width: 120,
  },
  {
    title: '备注',
    align: 'center',
    dataIndex: 'remark',
  },
];

/**
 * 工艺流程台账顶部搜索框：
 * 工艺路线编号、工艺路线名称、是否启用
 */
export const searchFormSchema: FormSchema[] = [
  {
    label: '工艺路线编号',
    field: 'routeCode',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: '工艺路线名称',
    field: 'routeName',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: '是否启用',
    field: 'isEnabled',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: {
      placeholder: '请选择是否启用',
      options: [
        { label: '是', value: 1 },
        { label: '否', value: 2 },
      ],
    },
  },
];

/**
 * 【图2】组成工序 Tab 表格列定义：
 * 序号、工序编码、工序名称、下一道工序、与下一道工序关系、关键工序、操作
 */
export const compositionColumns = [
  {
    title: '序号',
    dataIndex: 'sequenceNo',
    key: 'sequenceNo',
    align: 'center',
    width: 80,
  },
  {
    title: '工序编码',
    dataIndex: 'processCode',
    key: 'processCode',
    align: 'center',
    width: 140,
  },
  {
    title: '工序名称',
    dataIndex: 'processName',
    key: 'processName',
    align: 'center',
    width: 150,
  },
  {
    title: '下一道工序',
    dataIndex: 'nextProcessName',
    key: 'nextProcessName',
    align: 'center',
    width: 150,
  },
  {
    title: '与下一道工序关系',
    dataIndex: 'relationWithNext',
    key: 'relationWithNext',
    align: 'center',
    width: 160,
  },
  {
    title: '关键工序',
    dataIndex: 'isCriticalProcess',
    key: 'isCriticalProcess',
    align: 'center',
    width: 100,
  },
  {
    title: '操作',
    key: 'action',
    align: 'center',
    width: 130,
  },
];

/**
 * 【图4】关联产品 Tab 表格列定义：
 * 产品物料编码、产品物料名称、规格型号、单位、操作
 */
export const productColumns = [
  {
    title: '产品物料编码',
    dataIndex: 'productId',
    key: 'productId',
    align: 'center',
    width: 160,
  },
  {
    title: '产品物料名称',
    dataIndex: 'productName',
    key: 'productName',
    align: 'center',
    width: 180,
  },
  {
    title: '规格型号',
    dataIndex: 'specification',
    key: 'specification',
    align: 'center',
  },
  {
    title: '单位',
    dataIndex: 'unit',
    key: 'unit',
    align: 'center',
    width: 100,
  },
  {
    title: '操作',
    key: 'action',
    align: 'center',
    width: 130,
  },
];
