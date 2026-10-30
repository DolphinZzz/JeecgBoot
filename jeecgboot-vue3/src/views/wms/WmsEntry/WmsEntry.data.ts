import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';
import { JVxeTypes, JVxeColumn } from '/@/components/jeecg/JVxeTable/types';

/** 入库单状态（与 WmsEntryStatusEnum 一致） */
const statusColor: Record<number, string> = {
  10: 'default',
  20: 'processing',
  30: 'orange',
  40: 'blue',
  50: 'cyan',
  60: 'green',
  90: 'red',
  99: 'default',
};
const statusText: Record<number, string> = {
  10: '待到货',
  20: '收货清点中',
  30: '待质检',
  40: '质检完成待上架',
  50: '上架中',
  60: '已完成入库',
  90: '异常挂起',
  99: '已取消',
};

/** 超收放行状态 */
const overReceiveColor: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'red' };
const overReceiveText: Record<number, string> = { 0: '无需放行', 1: '待主管确认', 2: '已放行', 3: '已驳回' };

export const columns: BasicColumn[] = [
  {
    title: '入库单号',
    align: 'center',
    width: 190,
    dataIndex: 'entryCode',
  },
  {
    title: '入库类型',
    align: 'center',
    width: 120,
    dataIndex: 'entryType_dictText',
  },
  {
    title: '状态',
    align: 'center',
    width: 130,
    dataIndex: 'status',
    customRender: ({ record }) => {
      const label = record.status_dictText || statusText[record.status] || '-';
      return h(Tag, { color: statusColor[record.status] || 'default' }, () => label);
    },
  },
  {
    title: '仓库',
    align: 'center',
    width: 120,
    dataIndex: 'warehouseName',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '货主',
    align: 'center',
    width: 140,
    dataIndex: 'consignorName',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '批次号',
    align: 'center',
    width: 130,
    dataIndex: 'batchNumber',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '计划数量',
    align: 'center',
    width: 100,
    dataIndex: 'totalCount',
  },
  {
    title: '实收',
    align: 'center',
    width: 90,
    dataIndex: 'receivedTotal',
    customRender: ({ text }) => text ?? 0,
  },
  {
    title: '已上架',
    align: 'center',
    width: 90,
    dataIndex: 'putawayTotal',
    customRender: ({ text }) => text ?? 0,
  },
  {
    title: '超收',
    align: 'center',
    width: 80,
    dataIndex: 'surpassCount',
    customRender: ({ text }) => {
      const num = Number(text ?? 0);
      return num > 0 ? h('span', { style: { color: '#fa8c16', fontWeight: 500 } }, num) : 0;
    },
  },
  {
    title: '少收(待补料)',
    align: 'center',
    width: 110,
    dataIndex: 'lackCount',
    customRender: ({ text }) => {
      const num = Number(text ?? 0);
      if (num <= 0) return 0;
      return h('span', { style: { color: '#f5222d', fontWeight: 500 } }, `${num}(待补料)`);
    },
  },
  {
    title: '超收放行',
    align: 'center',
    width: 110,
    dataIndex: 'overReceiveAuditStatus',
    customRender: ({ record }) => {
      const label = record.overReceiveAuditStatus_dictText || overReceiveText[record.overReceiveAuditStatus] || '-';
      return h(Tag, { color: overReceiveColor[record.overReceiveAuditStatus] || 'default' }, () => label);
    },
  },
  {
    title: 'MES工单号',
    align: 'center',
    width: 150,
    dataIndex: 'mesWorkOrderNo',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '创建时间',
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
    label: '入库类型',
    field: 'entryType',
    component: 'JDictSelectTag',
    componentProps: { dictCode: 'wms_entry_type', placeholder: '请选择' },
    colProps: { span: 6 },
  },
  {
    label: '状态',
    field: 'status',
    component: 'JDictSelectTag',
    componentProps: { dictCode: 'wms_entry_status', placeholder: '请选择' },
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
];

/** 新建入库单表头 */
export const createSchema: FormSchema[] = [
  {
    label: '入库类型',
    field: 'entryType',
    component: 'JDictSelectTag',
    required: true,
    defaultValue: 1,
    componentProps: { dictCode: 'wms_entry_type', stringToNumber: true },
    colProps: { span: 12 },
  },
  {
    label: '入库仓库',
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
    label: '货主',
    field: 'consignorId',
    component: 'JDictSelectTag',
    required: true,
    componentProps: {
      dictCode: 'wms_consignor,consignor_name,id,enable_status=1',
      stringToNumber: true,
      placeholder: '请选择货主',
    },
    colProps: { span: 12 },
  },
  {
    label: '批次号',
    field: 'batchNumber',
    component: 'Input',
    helpMessage: '留空自动生成 B+日期+随机数',
    colProps: { span: 12 },
  },
  {
    label: 'ERP采购单号',
    field: 'erpPoNo',
    component: 'Input',
    colProps: { span: 12 },
  },
  {
    label: 'MES工单号',
    field: 'mesWorkOrderNo',
    component: 'Input',
    helpMessage: '生产入库必填；工业制造仓在质检环节强制校验',
    colProps: { span: 12 },
  },
  {
    label: '预测到达时间',
    field: 'predictedArrivalTime',
    component: 'DatePicker',
    componentProps: {
      showTime: true,
      valueFormat: 'YYYY-MM-DD HH:mm:ss',
      style: { width: '100%' },
    },
    colProps: { span: 12 },
  },
];

/** 新建入库单的明细行（一行为一个 SKU 的计划数量） */
export const createDetailColumns: JVxeColumn[] = [
  {
    title: '商品SKU',
    key: 'skuId',
    type: JVxeTypes.select,
    width: 220,
    dictCode: 'wms_item_sku,code,id',
    allowSearch: true,
    placeholder: '请选择SKU',
    validateRules: [{ required: true, message: '请选择商品SKU' }],
  },
  {
    title: '计划数量',
    key: 'count',
    type: JVxeTypes.inputNumber,
    width: 120,
    defaultValue: 1,
    props: { min: 1, precision: 0 },
    validateRules: [
      { required: true, message: '请填写计划数量' },
      { pattern: '^[1-9]\\d*$', message: '数量必须是大于0的整数' },
    ],
  },
  {
    title: '单价(元)',
    key: 'price',
    type: JVxeTypes.inputNumber,
    width: 120,
    props: { min: 0, precision: 2 },
    placeholder: '留空取商品档案价',
  },
  {
    title: '供应商',
    key: 'supplierId',
    type: JVxeTypes.select,
    width: 180,
    dictCode: 'wms_supplier,supplier_name,id',
    allowSearch: true,
    placeholder: '留空取商品默认供应商',
  },
  {
    title: '生产日期',
    key: 'prodDate',
    type: JVxeTypes.date,
    width: 150,
    placeholder: '可留空',
  },
  {
    title: '有效期至',
    key: 'expireDate',
    type: JVxeTypes.date,
    width: 150,
    placeholder: '留空按保质期推算',
  },
];

/** 收货清点行的只读展示列 */
export const receiveViewColumns: BasicColumn[] = [
  { title: 'SKU编码', align: 'center', width: 130, dataIndex: 'skuCode' },
  { title: '商品名称', align: 'left', width: 180, ellipsis: true, dataIndex: 'spuTitle' },
  { title: '批次号', align: 'center', width: 120, dataIndex: 'batchNumber' },
  { title: '计划数量', align: 'center', width: 100, dataIndex: 'count' },
  {
    title: '已实收',
    align: 'center',
    width: 90,
    dataIndex: 'receivedCount',
    customRender: ({ text }) => text ?? 0,
  },
  {
    title: 'SN管理',
    align: 'center',
    width: 90,
    dataIndex: 'isSnManaged',
    customRender: ({ text }) => (text === 1 ? '是' : '否'),
  },
  {
    title: '质检状态',
    align: 'center',
    width: 100,
    dataIndex: 'qcStatus_dictText',
    customRender: ({ text }) => text || '-',
  },
];

/** 收货清点录入列（实收 / 破损 / SN 由作业员填） */
export const receiveEditColumns: JVxeColumn[] = [
  {
    title: '商品SKU',
    key: 'skuCode',
    type: JVxeTypes.normal,
    width: 140,
    disabled: true,
  },
  {
    title: '商品名称',
    key: 'spuTitle',
    type: JVxeTypes.normal,
    width: 170,
    disabled: true,
  },
  {
    title: '计划数量',
    key: 'count',
    type: JVxeTypes.normal,
    width: 100,
    disabled: true,
  },
  {
    title: '实收数量',
    key: 'receivedCount',
    type: JVxeTypes.inputNumber,
    width: 120,
    props: { min: 0, precision: 0 },
    validateRules: [
      { required: true, message: '请填写实收数量' },
      { pattern: '^\\d+$', message: '实收数量必须是非负整数' },
    ],
  },
  {
    title: '破损数量',
    key: 'damageCount',
    type: JVxeTypes.inputNumber,
    width: 110,
    defaultValue: 0,
    props: { min: 0, precision: 0 },
    placeholder: '默认0',
  },
  {
    title: 'SN列表',
    key: 'snCodesText',
    type: JVxeTypes.input,
    width: 240,
    placeholder: 'SN管理商品必填，英文逗号分隔',
  },
];

/** 入库明细查看列 */
export const detailViewColumns: BasicColumn[] = [
  { title: 'SKU编码', align: 'center', width: 130, dataIndex: 'skuCode' },
  { title: '商品名称', align: 'left', width: 180, ellipsis: true, dataIndex: 'spuTitle' },
  { title: '批次号', align: 'center', width: 120, dataIndex: 'batchNumber' },
  { title: '计划数量', align: 'center', width: 100, dataIndex: 'count' },
  {
    title: '实收数量',
    align: 'center',
    width: 100,
    dataIndex: 'receivedCount',
    customRender: ({ text }) => text ?? 0,
  },
  { title: '质检状态', align: 'center', width: 100, dataIndex: 'qcStatus_dictText', customRender: ({ text }) => text || '-' },
  { title: '合格数', align: 'center', width: 90, dataIndex: 'qcPassCount', customRender: ({ text }) => text ?? 0 },
  { title: '不合格数', align: 'center', width: 90, dataIndex: 'qcFailCount', customRender: ({ text }) => text ?? 0 },
  { title: '已上架', align: 'center', width: 90, dataIndex: 'putawayCount', customRender: ({ text }) => text ?? 0 },
  {
    title: '默认质检方式',
    align: 'center',
    width: 110,
    dataIndex: 'defaultQcType',
    customRender: ({ text }) => (({ 1: '免检', 2: '抽检', 3: '全检' }) as Record<number, string>)[text] || '-',
  },
];

/** 上架记录列 */
export const putawayRecordColumns: BasicColumn[] = [
  { title: 'SKU编码', align: 'center', width: 130, dataIndex: 'skuId_dictText' },
  { title: '批次号', align: 'center', width: 120, dataIndex: 'batchNumber' },
  { title: '上架库位', align: 'center', width: 130, dataIndex: 'warehouseAreaLocaId_dictText' },
  { title: '上架数量', align: 'center', width: 100, dataIndex: 'count' },
  { title: '策略', align: 'center', width: 100, dataIndex: 'strategyUsed', customRender: ({ text }) => text || '-' },
  {
    title: '偏离推荐',
    align: 'center',
    width: 100,
    dataIndex: 'isDeviate',
    customRender: ({ record }) => {
      if (record.isDeviate !== 1) return '-';
      return h(Tag, { color: 'orange' }, () => '偏离');
    },
  },
  { title: '偏离原因', align: 'left', width: 200, ellipsis: true, dataIndex: 'deviateReason', customRender: ({ text }) => text || '-' },
  { title: '创建人', align: 'center', width: 110, dataIndex: 'createBy' },
  { title: '上架时间', align: 'center', width: 165, dataIndex: 'createTime' },
];
