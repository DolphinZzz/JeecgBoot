import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

/** 盘点状态：1待盘点 2已盘待审核 3已确认 */
const statusColor: Record<number, string> = { 1: 'orange', 2: 'blue', 3: 'green' };
const statusText: Record<number, string> = { 1: '待盘点', 2: '已盘待审核', 3: '已确认' };

/** 差异审批：0无差异 1待审批 2已通过 3已驳回 */
const auditColor: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'red' };
const auditText: Record<number, string> = { 0: '无差异', 1: '待审批', 2: '已通过', 3: '已驳回' };

// 列表列定义
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

// 查询表单 Schema（后端只接受 batchNo / status 两个条件）
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

// 生成盘点任务的表单
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
      placeholder: '请选择盘点类型',
    },
    colProps: { span: 12 },
  },
];

// 录入实盘数
export const countFormSchema: FormSchema[] = [
  {
    label: '商品信息',
    field: 'skuIdText',
    component: 'Input',
    dynamicDisabled: true,
    colProps: { span: 12 },
  },
  {
    label: '库位',
    field: 'locaText',
    component: 'Input',
    dynamicDisabled: true,
    colProps: { span: 12 },
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
    componentProps: { min: 0, precision: 0, placeholder: '请录入实盘数量' },
    colProps: { span: 12 },
  },
];
