import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

/** 质检状态：1待质检 2质检中 3合格 4不合格 5部分合格 */
const statusColor: Record<number, string> = { 1: 'orange', 2: 'processing', 3: 'green', 4: 'red', 5: 'gold' };
const statusText: Record<number, string> = { 1: '待质检', 2: '质检中', 3: '合格', 4: '不合格', 5: '部分合格' };

/** 质检方式：1免检 2抽检 3全检 */
const qcTypeText: Record<number, string> = { 1: '免检', 2: '抽检', 3: '全检' };

/** 已出结论的状态（1/2 仍需作业） */
export function isDecided(status: number): boolean {
  return status === 3 || status === 4 || status === 5;
}

export const columns: BasicColumn[] = [
  {
    title: '质检单号',
    align: 'center',
    width: 190,
    dataIndex: 'qcCode',
  },
  {
    title: '入库单号',
    align: 'center',
    width: 190,
    dataIndex: 'entryCode',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '状态',
    align: 'center',
    width: 110,
    dataIndex: 'status',
    customRender: ({ record }) => {
      const label = record.status_dictText || statusText[record.status] || '-';
      return h(Tag, { color: statusColor[record.status] || 'default' }, () => label);
    },
  },
  {
    title: '仓库',
    align: 'center',
    width: 110,
    dataIndex: 'warehouseName',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '仓型',
    align: 'center',
    width: 110,
    dataIndex: 'warehouseType',
    customRender: ({ text }) => (text === 'INDUSTRIAL' ? '工业制造仓' : text ? '普通仓' : '-'),
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
    title: 'MES工单号',
    align: 'center',
    width: 150,
    dataIndex: 'mesWorkOrderNo',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '质检方式',
    align: 'center',
    width: 100,
    dataIndex: 'qcType',
    customRender: ({ text, record }) => record.qcType_dictText || qcTypeText[text] || '-',
  },
  {
    title: '送检数量',
    align: 'center',
    width: 100,
    dataIndex: 'checkCount',
  },
  {
    title: '合格数',
    align: 'center',
    width: 90,
    dataIndex: 'passCount',
    customRender: ({ text }) => {
      const num = Number(text ?? 0);
      return num > 0 ? h('span', { style: { color: '#52c41a', fontWeight: 500 } }, num) : 0;
    },
  },
  {
    title: '不合格数',
    align: 'center',
    width: 100,
    dataIndex: 'failCount',
    customRender: ({ text }) => {
      const num = Number(text ?? 0);
      return num > 0 ? h('span', { style: { color: '#f5222d', fontWeight: 500 } }, num) : 0;
    },
  },
  {
    title: '不合格原因',
    align: 'left',
    width: 180,
    ellipsis: true,
    dataIndex: 'failReason',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '质检时间',
    align: 'center',
    width: 165,
    dataIndex: 'qcTime',
    customRender: ({ text }) => text || '-',
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    label: '质检单号',
    field: 'qcCode',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: '入库单号',
    field: 'entryCode',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: '状态',
    field: 'status',
    component: 'Select',
    componentProps: {
      placeholder: '请选择',
      options: [
        { label: '待质检', value: 1 },
        { label: '质检中', value: 2 },
        { label: '合格', value: 3 },
        { label: '不合格', value: 4 },
        { label: '部分合格', value: 5 },
      ],
    },
    colProps: { span: 6 },
  },
  {
    label: '只看未出结论',
    field: 'onlyPending',
    component: 'Switch',
    componentProps: { checkedChildren: '是', unCheckedChildren: '否' },
    colProps: { span: 6 },
  },
];

/**
 * 提交质检结果的表单。
 *
 * 合格数 + 不合格数 必须等于送检数量（checkCount），
 * 全部合格 → 状态 3、全部不合格 → 状态 4、两者都有 → 状态 5，
 * 结论由后端按 ofCounts 判定，前端只负责报数。
 */
export const submitFormSchema: FormSchema[] = [
  {
    label: '质检单号',
    field: 'qcCode',
    component: 'Input',
    dynamicDisabled: true,
    colProps: { span: 12 },
  },
  {
    label: '商品SKU',
    field: 'skuText',
    component: 'Input',
    dynamicDisabled: true,
    colProps: { span: 12 },
  },
  {
    label: '批次号',
    field: 'batchNumber',
    component: 'Input',
    dynamicDisabled: true,
    colProps: { span: 12 },
  },
  {
    label: '质检方式',
    field: 'qcType',
    component: 'Select',
    required: true,
    componentProps: {
      placeholder: '请选择',
      options: [
        { label: '免检（全部合格）', value: 1 },
        { label: '抽检', value: 2 },
        { label: '全检', value: 3 },
      ],
    },
    colProps: { span: 12 },
  },
  {
    label: '送检数量',
    field: 'checkCount',
    component: 'InputNumber',
    dynamicDisabled: true,
    colProps: { span: 12 },
  },
  {
    label: '抽检数量',
    field: 'sampleCount',
    component: 'InputNumber',
    helpMessage: '留空按质检方式默认值（抽检取 10%、全检取全部）',
    componentProps: { min: 0, precision: 0, style: { width: '100%' } },
    colProps: { span: 12 },
  },
  {
    label: '合格数量',
    field: 'passCount',
    component: 'InputNumber',
    required: true,
    componentProps: { min: 0, precision: 0, style: { width: '100%' } },
    colProps: { span: 12 },
  },
  {
    label: '不合格数量',
    field: 'failCount',
    component: 'InputNumber',
    required: true,
    defaultValue: 0,
    componentProps: { min: 0, precision: 0, style: { width: '100%' } },
    colProps: { span: 12 },
  },
  {
    label: '不合格原因',
    field: 'failReason',
    component: 'InputTextArea',
    helpMessage: '存在不合格品时建议填写',
    componentProps: { rows: 3, maxlength: 200 },
    colProps: { span: 24 },
  },
];
