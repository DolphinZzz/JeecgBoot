import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';
import { JVxeTypes, JVxeColumn } from '/@/components/jeecg/JVxeTable/types';

/** 上架状态：1未上架 2已上架 */
const statusColor: Record<number, string> = { 1: 'orange', 2: 'green' };
const statusText: Record<number, string> = { 1: '未上架', 2: '已上架' };

/** 上架策略 */
export const strategyText: Record<string, string> = {
  FIXED: '固定库位',
  ABC: 'ABC 分级',
  NEAREST: '就近上架',
};

/** 分区类型 */
export const zoneText: Record<string, string> = {
  GENERAL: '通用区',
  SMALL: '小件区',
  LARGE: '大件区',
  VALUABLE: '贵重区',
};

export const columns: BasicColumn[] = [
  {
    title: '入库单号',
    align: 'center',
    width: 190,
    dataIndex: 'entryCode',
  },
  {
    title: 'SKU编码',
    align: 'center',
    width: 130,
    dataIndex: 'skuCode',
    customRender: ({ text, record }) => text || record.skuId_dictText || '-',
  },
  {
    title: '商品名称',
    align: 'left',
    width: 180,
    ellipsis: true,
    dataIndex: 'spuTitle',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '批次号',
    align: 'center',
    width: 120,
    dataIndex: 'batchNumber',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '仓库',
    align: 'center',
    width: 110,
    dataIndex: 'warehouseName',
    customRender: ({ text, record }) => text || record.warehouseId_dictText || '-',
  },
  {
    title: '库区',
    align: 'center',
    width: 110,
    dataIndex: 'areaName',
    customRender: ({ text, record }) => text || record.warehouseAreaId_dictText || '-',
  },
  {
    title: '上架库位',
    align: 'center',
    width: 130,
    dataIndex: 'locaCode',
    customRender: ({ text, record }) => text || record.warehouseAreaLocaId_dictText || '-',
  },
  {
    title: '推荐库位',
    align: 'center',
    width: 130,
    dataIndex: 'recommendLocaCode',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '上架数量',
    align: 'center',
    width: 100,
    dataIndex: 'count',
  },
  {
    title: '策略',
    align: 'center',
    width: 110,
    dataIndex: 'strategyUsed',
    customRender: ({ record }) => {
      const label = record.strategyUsed_dictText || strategyText[record.strategyUsed] || '-';
      return label;
    },
  },
  {
    title: '偏离推荐',
    align: 'center',
    width: 100,
    dataIndex: 'isDeviate',
    customRender: ({ record }) => {
      if (record.isDeviate !== 1) return h('span', { style: { color: '#8c8c8c' } }, '否');
      return h(Tag, { color: 'orange' }, () => '偏离');
    },
  },
  {
    title: '偏离原因',
    align: 'left',
    width: 200,
    ellipsis: true,
    dataIndex: 'deviateReason',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '上架状态',
    align: 'center',
    width: 100,
    dataIndex: 'status',
    customRender: ({ record }) => {
      const label = record.status_dictText || statusText[record.status] || '-';
      return h(Tag, { color: statusColor[record.status] || 'default' }, () => label);
    },
  },
  {
    title: '操作人',
    align: 'center',
    width: 110,
    dataIndex: 'createBy',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '上架时间',
    align: 'center',
    width: 165,
    dataIndex: 'createTime',
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    label: '入库单号',
    field: 'entryCode',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: '仓库',
    field: 'warehouseId',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_warehouse,name,id',
      stringToNumber: true,
      placeholder: '请选择',
    },
    colProps: { span: 6 },
  },
  {
    label: '上架库位',
    field: 'warehouseAreaLocaId',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_warehouse_area_loca,loca_code,id',
      stringToNumber: true,
      placeholder: '请选择',
    },
    colProps: { span: 6 },
  },
  {
    label: '上架状态',
    field: 'status',
    component: 'Select',
    componentProps: {
      placeholder: '请选择',
      options: [
        { label: '未上架', value: 1 },
        { label: '已上架', value: 2 },
      ],
    },
    colProps: { span: 6 },
  },
  {
    label: '只看偏离',
    field: 'onlyDeviate',
    component: 'Switch',
    componentProps: { checkedChildren: '是', unCheckedChildren: '否' },
    colProps: { span: 6 },
  },
];

/**
 * 确认上架的落点列。
 *
 * 行数据来自上架推荐（每条 = 一个「放哪个库位、放几件」），
 * 作业员可以改目标库位与数量，只要过了后端第 1 层硬过滤就允许，
 * 与推荐不一致时后端会记 is_deviate 并保留偏离原因。
 */
export const confirmColumns: JVxeColumn[] = [
  {
    title: '商品SKU',
    key: 'skuText',
    type: JVxeTypes.normal,
    width: 150,
    disabled: true,
  },
  {
    title: '推荐库位',
    key: 'recommendLocaCode',
    type: JVxeTypes.normal,
    width: 130,
    disabled: true,
  },
  {
    title: '库位剩余可放',
    key: 'capacity',
    type: JVxeTypes.normal,
    width: 120,
    disabled: true,
  },
  {
    title: '上架库位',
    key: 'locaId',
    // 下拉选项由弹窗按本仓库位动态注入（按库区分组），不用表字典：
    // 表字典会把所有仓库的库位混在一起，也看不出属于哪个库区
    type: JVxeTypes.select,
    width: 220,
    allowSearch: true,
    placeholder: '选择本仓库位',
    validateRules: [{ required: true, message: '请选择上架库位' }],
  },
  {
    title: '上架数量',
    key: 'count',
    type: JVxeTypes.inputNumber,
    width: 110,
    props: { min: 1, precision: 0 },
    validateRules: [
      { required: true, message: '请填写上架数量' },
      { pattern: '^[1-9]\\d*$', message: '上架数量必须是大于0的整数' },
    ],
  },
  {
    title: '强制上架',
    key: 'force',
    type: JVxeTypes.checkbox,
    width: 100,
    defaultChecked: false,
  },
  {
    title: '偏离/强制原因',
    key: 'deviateReason',
    type: JVxeTypes.input,
    width: 240,
    placeholder: '改库位或强制指定时必填',
  },
];
