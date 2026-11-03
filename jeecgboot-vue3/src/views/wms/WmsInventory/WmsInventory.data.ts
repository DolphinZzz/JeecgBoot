import { h } from 'vue';
import { Tag, Progress } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

/** 效期状态：1正常 2临期 3过期 4无有效期 */
const expireStatusColor: Record<number, string> = { 1: 'green', 2: 'orange', 3: 'red', 4: 'default' };

/** 库位状态颜色：1空闲(绿色) 2占用(蓝色) 3盘点锁定(紫色) 4禁用(灰色) 5满仓(橙色) */
const locaStatusColorMap: Record<number, string> = {
  1: 'success',
  2: 'processing',
  3: 'purple',
  4: 'default',
  5: 'warning',
};

// ==================== Tab 1: 库位库存（一库位一SKU一批次实际占用） ====================
export const locaColumns: BasicColumn[] = [
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
    title: '库位编码',
    align: 'center',
    width: 120,
    dataIndex: 'locaCode',
    customRender: ({ text }) => h('span', { style: { fontWeight: 600 } }, text),
  },
  {
    title: '货架物理坐标',
    align: 'center',
    width: 140,
    dataIndex: 'positionText',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '库位状态',
    align: 'center',
    width: 90,
    dataIndex: 'locaStatus',
    customRender: ({ record }) => {
      const status = record.locaStatus;
      const text = record.locaStatusText || '未知';
      return h(Tag, { color: locaStatusColorMap[status] || 'default' }, () => text);
    },
  },
  {
    title: '绑定规格(SKU)',
    align: 'left',
    width: 220,
    dataIndex: 'boundSkuId',
    slots: { customRender: 'boundSku' },
  },
  {
    title: '当前在库批次',
    align: 'center',
    width: 150,
    dataIndex: 'batchNumber',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '在库件数',
    align: 'center',
    width: 100,
    dataIndex: 'totalCount',
    customRender: ({ text }) =>
      h(
        'span',
        { style: { fontWeight: 'bold', fontSize: '14px', color: Number(text) > 0 ? '#1677ff' : '#8c8c8c' } },
        `${text ?? 0} 件`
      ),
  },
  {
    title: '可用件数',
    align: 'center',
    width: 90,
    dataIndex: 'availableCount',
    customRender: ({ text }) =>
      h('span', { style: { color: '#52c41a', fontWeight: 600 } }, `${text ?? 0}`),
  },
  {
    title: '锁定件数',
    align: 'center',
    width: 90,
    dataIndex: 'lockedCount',
    customRender: ({ text }) =>
      h('span', { style: { color: Number(text) > 0 ? '#fa8c16' : undefined } }, `${text ?? 0}`),
  },
  {
    title: '冻结件数',
    align: 'center',
    width: 90,
    dataIndex: 'frozenCount',
    customRender: ({ text }) =>
      h('span', { style: { color: Number(text) > 0 ? '#f5222d' : undefined } }, `${text ?? 0}`),
  },
];

export const locaSearchFormSchema: FormSchema[] = [
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
    label: '库位编码',
    field: 'locaCode',
    component: 'JInput',
    componentProps: { placeholder: '支持模糊查询' },
    colProps: { span: 6 },
  },
  {
    label: '库位状态',
    field: 'locaStatus',
    component: 'Select',
    componentProps: {
      placeholder: '全部状态',
      options: [
        { label: '空闲', value: 1 },
        { label: '占用', value: 2 },
        { label: '盘点锁定', value: 3 },
        { label: '禁用', value: 4 },
      ],
    },
    colProps: { span: 6 },
  },
  {
    label: '占用状态',
    field: 'isOccupied',
    component: 'Select',
    componentProps: {
      placeholder: '全部',
      options: [
        { label: '空闲未占用', value: 0 },
        { label: '在库已占用', value: 1 },
      ],
    },
    colProps: { span: 6 },
  },
  {
    label: '商品/规格',
    field: 'skuKeyword',
    component: 'JInput',
    componentProps: { placeholder: '输入商品名称/规格编码' },
    colProps: { span: 6 },
  },
];

// ==================== Tab 2: 全周期库存台账 (11个关键指标) ====================
export const ledgerColumns: BasicColumn[] = [
  {
    title: '商品编码',
    align: 'center',
    width: 130,
    dataIndex: 'spuCode',
    fixed: 'left',
  },
  {
    title: '商品名称',
    align: 'left',
    width: 180,
    ellipsis: true,
    dataIndex: 'spuName',
  },
  {
    title: '规格编码',
    align: 'center',
    width: 140,
    dataIndex: 'skuCode',
  },
  {
    title: '所属仓库',
    align: 'center',
    width: 120,
    dataIndex: 'warehouseName',
    customRender: ({ text }) => text || '全仓汇总',
  },
  {
    title: '在库库位数',
    align: 'center',
    width: 100,
    dataIndex: 'inStockLocaCount',
    customRender: ({ text }) =>
      h('span', { style: { fontWeight: 600, color: Number(text) > 0 ? '#13c2c2' : undefined } }, `${text ?? 0} 个`),
  },
  {
    title: '库存数量',
    align: 'center',
    width: 100,
    dataIndex: 'totalCount',
    customRender: ({ text }) =>
      h('span', { style: { fontWeight: 'bold', fontSize: '14px' } }, text ?? 0),
  },
  {
    title: '可用数量',
    align: 'center',
    width: 100,
    dataIndex: 'availableCount',
    customRender: ({ text }) =>
      h('span', { style: { color: '#1677ff', fontWeight: 600 } }, text ?? 0),
  },
  {
    title: '锁定数量',
    align: 'center',
    width: 100,
    dataIndex: 'lockedCount',
    customRender: ({ text }) =>
      h('span', { style: { color: Number(text) > 0 ? '#fa8c16' : undefined } }, text ?? 0),
  },
  {
    title: '冻结数量',
    align: 'center',
    width: 100,
    dataIndex: 'frozenCount',
    customRender: ({ text }) =>
      h('span', { style: { color: Number(text) > 0 ? '#f5222d' : undefined } }, text ?? 0),
  },
  {
    title: '待卸货数量',
    align: 'center',
    width: 110,
    dataIndex: 'pendingUnloadCount',
    customRender: ({ text }) =>
      h('span', { style: { color: Number(text) > 0 ? '#722ed1' : undefined } }, text ?? 0),
  },
  {
    title: '待分拣数量',
    align: 'center',
    width: 110,
    dataIndex: 'pendingSortCount',
    customRender: ({ text }) =>
      h('span', { style: { color: Number(text) > 0 ? '#13c2c2' : undefined } }, text ?? 0),
  },
  {
    title: '已分拣数量',
    align: 'center',
    width: 110,
    dataIndex: 'sortedCount',
    customRender: ({ text }) =>
      h('span', { style: { color: Number(text) > 0 ? '#52c41a' : undefined } }, text ?? 0),
  },
  {
    title: '欠货数量',
    align: 'center',
    width: 110,
    dataIndex: 'shortageCount',
    customRender: ({ text }) => {
      const num = Number(text) || 0;
      if (num > 0) {
        return h(Tag, { color: 'red' }, () => `欠货 ${num}`);
      }
      return '0';
    },
  },
];

export const ledgerSearchFormSchema: FormSchema[] = [
  {
    label: '仓库',
    field: 'warehouseId',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_warehouse,name,id',
      stringToNumber: true,
      placeholder: '全仓汇总（可选择具体仓库）',
    },
    colProps: { span: 8 },
  },
  {
    label: '商品信息',
    field: 'spuKeyword',
    component: 'JInput',
    componentProps: { placeholder: '支持商品编码/名称模糊查询' },
    colProps: { span: 8 },
  },
  {
    label: '规格编码',
    field: 'skuCode',
    component: 'JInput',
    componentProps: { placeholder: '规格编码精确或模糊' },
    colProps: { span: 8 },
  },
];

// ==================== 维度一：按仓库维度（仓库/库区/库位存放商品分布） ====================
export const warehouseColumns: BasicColumn[] = [
  {
    title: '仓库',
    align: 'center',
    width: 120,
    dataIndex: 'warehouseName',
    fixed: 'left',
  },
  {
    title: '库区',
    align: 'center',
    width: 110,
    dataIndex: 'areaName',
  },
  {
    title: '库位',
    align: 'center',
    width: 130,
    dataIndex: 'locaCode',
    customRender: ({ text }) => h(Tag, { color: 'cyan', style: { fontWeight: 'bold' } }, () => text || '-'),
  },
  {
    title: '商品编码',
    align: 'center',
    width: 140,
    dataIndex: 'spuCode',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '商品名称',
    align: 'left',
    width: 180,
    ellipsis: true,
    dataIndex: 'spuTitle',
    customRender: ({ text }) => h('span', { style: { fontWeight: 500 } }, text || '-'),
  },
  {
    title: '规格编码',
    align: 'center',
    width: 140,
    dataIndex: 'skuCode',
    customRender: ({ text }) => (text ? h(Tag, { color: 'blue' }, () => text) : '-'),
  },
  {
    title: '批次号',
    align: 'center',
    width: 130,
    dataIndex: 'batchNumber',
    customRender: ({ text }) => (text && text !== '-' ? h(Tag, { color: 'blue' }, () => text) : h('span', { style: { color: '#bfbfbf' } }, '-')),
  },
  {
    title: '货主',
    align: 'center',
    width: 120,
    dataIndex: 'consignorName',
    customRender: ({ text }) => text || '自有货主',
  },
  {
    title: '质量状态',
    align: 'center',
    width: 90,
    dataIndex: 'qualityStatus_dictText',
    customRender: ({ record }) => {
      const q = record.qualityStatus;
      const text = record.qualityStatus_dictText || (q === 1 ? '良品' : q === 2 ? '待检' : q === 3 ? '不良品' : '-');
      const color = q === 1 ? 'green' : q === 2 ? 'orange' : q === 3 ? 'red' : 'default';
      return h(Tag, { color }, () => text);
    },
  },
  {
    title: '在库库存',
    align: 'center',
    width: 95,
    dataIndex: 'totalCount',
    customRender: ({ text }) => h('span', { style: { fontWeight: 'bold', fontSize: '14px', color: Number(text) > 0 ? '#1890ff' : '#8c8c8c' } }, `${text ?? 0}`),
  },
  {
    title: '可用数量',
    align: 'center',
    width: 95,
    dataIndex: 'availableCount',
    customRender: ({ text }) => h('span', { style: { color: '#52c41a', fontWeight: 'bold' } }, `${text ?? 0}`),
  },
  {
    title: '锁定数量',
    align: 'center',
    width: 90,
    dataIndex: 'lockedCount',
    customRender: ({ text }) => h('span', { style: { color: Number(text) > 0 ? '#fa8c16' : undefined } }, `${text ?? 0}`),
  },
  {
    title: '冻结数量',
    align: 'center',
    width: 90,
    dataIndex: 'frozenCount',
    customRender: ({ text }) => h('span', { style: { color: Number(text) > 0 ? '#f5222d' : undefined } }, `${text ?? 0}`),
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
    width: 95,
    dataIndex: 'expireStatus',
    customRender: ({ text, record }) => {
      const value = text ?? record.expireStatus;
      const labelMap: Record<number, string> = { 1: '正常', 2: '临期', 3: '过期', 4: '无有效期' };
      if (value === null || value === undefined) return '-';
      return h(Tag, { color: expireStatusColor[value] || 'default' }, () => labelMap[value] || value);
    },
  },
];

export const columns = warehouseColumns;

// 仓库维度查询表单 Schema
export const warehouseSearchFormSchema: FormSchema[] = [
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
    label: '商品编码',
    field: 'spuCode',
    component: 'JInput',
    componentProps: { placeholder: '商品编码，支持模糊' },
    colProps: { span: 6 },
  },
  {
    label: '商品名称',
    field: 'spuTitle',
    component: 'JInput',
    componentProps: { placeholder: '商品名称，支持模糊' },
    colProps: { span: 6 },
  },
  {
    label: '规格编码',
    field: 'skuCode',
    component: 'JInput',
    componentProps: { placeholder: '规格编码，支持模糊' },
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
];

export const searchFormSchema = warehouseSearchFormSchema;

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

/** Tab 2 库位分布抽屉表格列 */
export const locaDistributionColumns: BasicColumn[] = [
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
    dataIndex: 'warehouseAreaName',
  },
  {
    title: '库位编码',
    align: 'center',
    width: 120,
    dataIndex: 'locaCode',
    customRender: ({ text }) => h('span', { style: { fontWeight: 600 } }, text),
  },
  {
    title: '批次号',
    align: 'center',
    width: 150,
    dataIndex: 'batchNumber',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '质量状态',
    align: 'center',
    width: 90,
    dataIndex: 'qualityStatus',
    customRender: ({ text }) => {
      const q = Number(text);
      return h(
        Tag,
        { color: q === 1 ? 'green' : q === 2 ? 'orange' : 'red' },
        () => (q === 1 ? '良品' : q === 2 ? '待检' : '不良品')
      );
    },
  },
  {
    title: '在库数量',
    align: 'center',
    width: 100,
    dataIndex: 'totalCount',
    customRender: ({ text }) => h('span', { style: { fontWeight: 'bold' } }, `${text ?? 0} 件`),
  },
  {
    title: '可用数量',
    align: 'center',
    width: 90,
    dataIndex: 'availableCount',
    customRender: ({ text }) => h('span', { style: { color: '#52c41a', fontWeight: 600 } }, `${text ?? 0}`),
  },
  {
    title: '锁定数量',
    align: 'center',
    width: 90,
    dataIndex: 'lockedCount',
    customRender: ({ text }) => h('span', { style: { color: Number(text) > 0 ? '#fa8c16' : undefined } }, `${text ?? 0}`),
  },
  {
    title: '冻结数量',
    align: 'center',
    width: 90,
    dataIndex: 'frozenCount',
    customRender: ({ text }) => h('span', { style: { color: Number(text) > 0 ? '#f5222d' : undefined } }, `${text ?? 0}`),
  },
];
