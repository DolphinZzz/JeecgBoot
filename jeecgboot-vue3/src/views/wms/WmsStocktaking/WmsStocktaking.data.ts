import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

/** 盘点状态：1待盘点 2已盘待审核 3已确认 */
const statusColor: Record<number, string> = { 1: 'orange', 2: 'blue', 3: 'green' };
const statusText: Record<number, string> = { 1: '待盘点', 2: '已盘待审核', 3: '已确认' };

/** 差异审批：0无差异 1待审批 2已通过 3已驳回 */
const auditColor: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'red' };
const auditText: Record<number, string> = { 0: '无差异', 1: '待审批', 2: '已通过', 3: '已驳回' };

/** 盘库单状态：0草稿 1已盘点 2已调账 */
const orderStatusColor: Record<number, string> = { 0: 'orange', 1: 'blue', 2: 'green' };
const orderStatusText: Record<number, string> = { 0: '草稿', 1: '已盘点', 2: '已调账' };

/**
 * 格式化规格名（将 JSON 字符串 {"specCode":..., "specName":"..."} 解析为人类可读的规格名）
 */
export function formatSkuSpecName(val: any): string {
  if (!val) return '-';
  if (typeof val !== 'string') {
    val = JSON.stringify(val);
  }
  const trimmed = val.trim();
  if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) {
    return trimmed;
  }
  try {
    const parsed = JSON.parse(trimmed);
    if (parsed && typeof parsed === 'object') {
      if (parsed.specName) {
        return parsed.specName;
      }
      const validValues = Object.entries(parsed)
        .filter(([k, v]) => !['specCode', 'unit', 'code', 'spuId', 'id'].includes(k) && v !== null && v !== undefined && v !== '')
        .map(([, v]) => String(v));
      if (validValues.length > 0) {
        return validValues.join(' ');
      }
    }
  } catch {
    // 忽略异常
  }
  return trimmed;
}

// ==================== 1. 盘库单主台账列定义 ====================
export const orderColumns: BasicColumn[] = [
  {
    title: '盘库单号',
    align: 'center',
    width: 175,
    dataIndex: 'orderNo',
    fixed: 'left',
  },
  {
    title: '盘库状态',
    align: 'center',
    width: 105,
    dataIndex: 'status',
    customRender: ({ record }) => {
      const label = orderStatusText[record.status] || (record.status === 1 ? '已盘点' : record.status === 2 ? '已调账' : '草稿');
      return h(Tag, { color: orderStatusColor[record.status] || 'orange' }, () => label);
    },
  },
  {
    title: '仓库',
    align: 'center',
    width: 130,
    dataIndex: 'warehouseId_dictText',
  },
  {
    title: '盘盈',
    align: 'center',
    width: 150,
    dataIndex: 'profit',
    customRender: ({ record }) => {
      const count = record.profitCount || 0;
      const amt = Number(record.profitAmount || 0).toFixed(2);
      if (count > 0) {
        return h('span', { style: { color: '#52c41a', fontWeight: 'bold' } }, `+${count} (￥${amt})`);
      }
      return h('span', { style: { color: '#bfbfbf' } }, '-');
    },
  },
  {
    title: '盘亏',
    align: 'center',
    width: 150,
    dataIndex: 'loss',
    customRender: ({ record }) => {
      const count = record.lossCount || 0;
      const amt = Number(record.lossAmount || 0).toFixed(2);
      if (count > 0) {
        return h('span', { style: { color: '#f5222d', fontWeight: 'bold' } }, `-${count} (￥${amt})`);
      }
      return h('span', { style: { color: '#bfbfbf' } }, '-');
    },
  },
  {
    title: '盘库调整',
    align: 'center',
    width: 230,
    dataIndex: 'adjust',
    slots: { customRender: 'adjust' },
  },
  {
    title: '实际金额',
    align: 'right',
    width: 125,
    dataIndex: 'totalActualAmount',
    customRender: ({ text }) => (text !== null && text !== undefined ? `￥${Number(text).toFixed(2)}` : '￥0.00'),
  },
  {
    title: '总金额',
    align: 'right',
    width: 125,
    dataIndex: 'totalBookAmount',
    customRender: ({ text }) => (text !== null && text !== undefined ? `￥${Number(text).toFixed(2)}` : '￥0.00'),
  },
];

// 盘库单搜索 Schema
export const orderSearchFormSchema: FormSchema[] = [
  {
    label: '盘库单号',
    field: 'orderNo',
    component: 'JInput',
    componentProps: { placeholder: '输入盘库单号' },
    colProps: { span: 6 },
  },
  {
    label: '仓库',
    field: 'warehouseId',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_warehouse,name,id',
      stringToNumber: true,
      placeholder: '选择仓库',
    },
    colProps: { span: 6 },
  },
  {
    label: '盘库状态',
    field: 'status',
    component: 'Select',
    componentProps: {
      options: [
        { label: '草稿', value: 0 },
        { label: '已盘点', value: 1 },
        { label: '已调账', value: 2 },
      ],
      placeholder: '选择状态',
    },
    colProps: { span: 6 },
  },
];

// ==================== 2. 底层单行盘点记录（保留兼容） ====================
export const columns: BasicColumn[] = [
  {
    title: '盘点批次',
    align: 'center',
    width: 180,
    dataIndex: 'batchNo',
  },
  {
    title: '状态',
    align: 'center',
    width: 120,
    dataIndex: 'status',
    customRender: ({ record }) => {
      const label = record.status_dictText || statusText[record.status] || '-';
      return h(Tag, { color: statusColor[record.status] || 'default' }, () => label);
    },
  },
  {
    title: '盘点类型',
    align: 'center',
    width: 100,
    dataIndex: 'category_dictText',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '仓库',
    align: 'center',
    width: 110,
    dataIndex: 'targetWarehouseId_dictText',
  },
  {
    title: '库区',
    align: 'center',
    width: 100,
    dataIndex: 'targetWarehouseAreaId_dictText',
  },
  {
    title: '库位',
    align: 'center',
    width: 120,
    dataIndex: 'targetWarehouseAreaLocaId_dictText',
  },
  {
    title: 'SKU编码',
    align: 'center',
    width: 130,
    dataIndex: 'skuId_dictText',
  },
  {
    title: '批次号',
    align: 'center',
    width: 120,
    dataIndex: 'batchNumber',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '账面数量',
    align: 'center',
    width: 100,
    dataIndex: 'bookCount',
  },
  {
    title: '实盘数量',
    align: 'center',
    width: 100,
    dataIndex: 'correctCount',
    customRender: ({ text }) => (text === null || text === undefined ? '-' : text),
  },
  {
    title: '差异数量',
    align: 'center',
    width: 100,
    dataIndex: 'discrepancyCount',
    customRender: ({ text }) => {
      if (text === null || text === undefined) return '-';
      const num = Number(text);
      if (num === 0) return h('span', { style: { color: '#8c8c8c' } }, '0');
      return h('span', { style: { color: num > 0 ? '#52c41a' : '#f5222d', fontWeight: 500 } }, num > 0 ? `盘盈 +${num}` : `盘亏 ${num}`);
    },
  },
  {
    title: '差异审批',
    align: 'center',
    width: 110,
    dataIndex: 'auditStatus',
    customRender: ({ record }) => {
      const label = record.auditStatus_dictText || auditText[record.auditStatus] || '-';
      return h(Tag, { color: auditColor[record.auditStatus] || 'default' }, () => label);
    },
  },
  {
    title: '审批人',
    align: 'center',
    width: 110,
    dataIndex: 'auditBy',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '生成时间',
    align: 'center',
    width: 165,
    dataIndex: 'createTime',
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    label: '盘点批次',
    field: 'batchNo',
    component: 'JInput',
    componentProps: { placeholder: '精确匹配批次号' },
    colProps: { span: 6 },
  },
  {
    label: '盘点状态',
    field: 'status',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_stocktaking_status',
      placeholder: '请选择状态',
    },
    colProps: { span: 6 },
  },
];

export const createFormSchema: FormSchema[] = [
  {
    label: '盘点仓库',
    field: 'warehouseId',
    component: 'JDictSelectTag',
    required: true,
    componentProps: {
      dictCode: 'wms_warehouse,name,id',
      stringToNumber: true,
      placeholder: '请选择仓库',
    },
    colProps: { span: 12 },
  },
  {
    label: '盘点库区',
    field: 'warehouseAreaId',
    component: 'JDictSelectTag',
    helpMessage: '留空表示对整仓有库存的库位盘点',
    componentProps: {
      dictCode: 'wms_warehouse_area,name,id',
      stringToNumber: true,
      placeholder: '留空为全仓',
    },
    colProps: { span: 12 },
  },
  {
    label: '盘点类型',
    field: 'category',
    component: 'JDictSelectTag',
    defaultValue: 1,
    componentProps: {
      dictCode: 'wms_stocktaking_category',
      stringToNumber: true,
    },
    colProps: { span: 12 },
  },
];

export const countFormSchema: FormSchema[] = [
  {
    label: '商品SKU',
    field: 'skuIdText',
    component: 'Input',
    dynamicDisabled: true,
    colProps: { span: 24 },
  },
  {
    label: '盘点库位',
    field: 'locaText',
    component: 'Input',
    dynamicDisabled: true,
    colProps: { span: 24 },
  },
  {
    label: '账面数量',
    field: 'bookCount',
    component: 'InputNumber',
    dynamicDisabled: true,
    colProps: { span: 12 },
  },
  {
    label: '实盘数量',
    field: 'correctCount',
    component: 'InputNumber',
    required: true,
    componentProps: {
      min: 0,
      precision: 0,
      placeholder: '填写清点件数',
    },
    colProps: { span: 12 },
  },
];
