import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

/** 效期状态：1正常 2临期 3过期 4无有效期 */
const expireStatusColor: Record<number, string> = { 1: 'green', 2: 'orange', 3: 'red', 4: 'default' };

// 列表列定义
export const columns: BasicColumn[] = [
  {
    title: '仓库',
    align: 'center',
    width: 110,
    dataIndex: 'warehouseName',
  },
  {
    title: '库区',
    align: 'center',
    width: 100,
    dataIndex: 'areaName',
  },
  {
    title: '库位',
    align: 'center',
    width: 120,
    dataIndex: 'locaCode',
  },
  {
    title: 'SKU编码',
    align: 'center',
    width: 130,
    dataIndex: 'skuCode',
  },
  {
    title: '商品名称',
    align: 'left',
    width: 180,
    ellipsis: true,
    dataIndex: 'spuTitle',
  },
  {
    title: '批次号',
    align: 'center',
    width: 120,
    dataIndex: 'batchNumber',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '生产日期',
    align: 'center',
    width: 110,
    dataIndex: 'prodDate',
    customRender: ({ text }) => (text ? String(text).substring(0, 10) : '-'),
  },
  {
    title: '有效期至',
    align: 'center',
    width: 110,
    dataIndex: 'expireDate',
    customRender: ({ text }) => (text ? String(text).substring(0, 10) : '-'),
  },
  {
    title: '效期状态',
    align: 'center',
    width: 100,
    dataIndex: 'expireStatus',
    customRender: ({ text, record }) => {
      const value = text ?? record.expireStatus;
      const labelMap: Record<number, string> = { 1: '正常', 2: '临期', 3: '过期', 4: '无有效期' };
      if (value === null || value === undefined) return '-';
      return h(Tag, { color: expireStatusColor[value] || 'default' }, () => labelMap[value] || value);
    },
  },
  {
    title: '质量状态',
    align: 'center',
    width: 90,
    dataIndex: 'qualityStatus_dictText',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '物理库存',
    align: 'center',
    width: 100,
    dataIndex: 'totalCount',
  },
  {
    title: '可用库存',
    align: 'center',
    width: 100,
    dataIndex: 'availableCount',
    customRender: ({ text }) => h('span', { style: { color: '#1677ff', fontWeight: 500 } }, text),
  },
  {
    title: '锁定库存',
    align: 'center',
    width: 100,
    dataIndex: 'lockedCount',
    customRender: ({ text }) => h('span', { style: { color: Number(text) > 0 ? '#fa8c16' : undefined } }, text),
  },
  {
    title: '冻结库存',
    align: 'center',
    width: 100,
    dataIndex: 'frozenCount',
    customRender: ({ text }) => h('span', { style: { color: Number(text) > 0 ? '#f5222d' : undefined } }, text),
  },
  {
    title: '货主',
    align: 'center',
    width: 140,
    dataIndex: 'consignorName',
    customRender: ({ text }) => text || '自有',
  },
  {
    title: '更新时间',
    align: 'center',
    width: 160,
    dataIndex: 'updateTime',
  },
];

// 查询表单 Schema
export const searchFormSchema: FormSchema[] = [
  {
    label: '仓库',
    field: 'warehouseId',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_warehouse,name,id',
      stringToNumber: true,
      placeholder: '请选择仓库',
    },
    colProps: { span: 6 },
  },
  {
    label: '库区',
    field: 'warehouseAreaId',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_warehouse_area,name,id',
      stringToNumber: true,
      placeholder: '请选择库区',
    },
    colProps: { span: 6 },
  },
  {
    label: '库位',
    field: 'locaCode',
    component: 'JInput',
    componentProps: { placeholder: '库位编号，支持模糊' },
    colProps: { span: 6 },
  },
  {
    label: 'SKU编码',
    field: 'skuCode',
    component: 'JInput',
    componentProps: { placeholder: '支持模糊' },
    colProps: { span: 6 },
  },
  {
    label: '商品名称',
    field: 'spuTitle',
    component: 'JInput',
    componentProps: { placeholder: '支持模糊' },
    colProps: { span: 6 },
  },
  {
    label: '批次号',
    field: 'batchNumber',
    component: 'JInput',
    componentProps: { placeholder: '精确匹配' },
    colProps: { span: 6 },
  },
  {
    label: '质量状态',
    field: 'qualityStatus',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_quality_status',
      stringToNumber: true,
      placeholder: '请选择质量状态',
    },
    colProps: { span: 6 },
  },
  {
    label: '效期状态',
    field: 'expireStatus',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_expire_status',
      stringToNumber: true,
      placeholder: '请选择效期状态',
    },
    colProps: { span: 6 },
  },
  {
    label: 'SN序列号',
    field: 'snCode',
    component: 'JInput',
    componentProps: { placeholder: '支持模糊，用于单件追溯' },
    colProps: { span: 6 },
  },
  {
    label: '仅看有货',
    field: 'onlyAvailable',
    component: 'Switch',
    defaultValue: true,
    helpMessage: '关闭后可查看数量已归零的库存行',
    colProps: { span: 6 },
  },
];

// 库存流水抽屉列
export const logColumns: BasicColumn[] = [
  { title: '变动时间', align: 'center', width: 160, dataIndex: 'createTime' },
  { title: '业务类型', align: 'center', width: 110, dataIndex: 'bizType_dictText' },
  { title: '关联单号', align: 'center', width: 170, dataIndex: 'bizNo' },
  {
    title: '变动数量',
    align: 'center',
    width: 100,
    dataIndex: 'changeCount',
    customRender: ({ text }) => {
      const num = Number(text);
      return h('span', { style: { color: num >= 0 ? '#52c41a' : '#f5222d', fontWeight: 500 } }, num > 0 ? `+${num}` : `${num}`);
    },
  },
  { title: '变动前可用', align: 'center', width: 110, dataIndex: 'beforeCount' },
  { title: '变动后可用', align: 'center', width: 110, dataIndex: 'afterCount' },
  { title: '操作人', align: 'center', width: 110, dataIndex: 'createBy' },
  { title: '备注', align: 'left', dataIndex: 'remark' },
];
