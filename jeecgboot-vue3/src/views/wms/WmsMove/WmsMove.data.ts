import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';
import { JVxeTypes, JVxeColumn } from '/@/components/jeecg/JVxeTable/types';

// 列表列定义
export const columns: BasicColumn[] = [
  {
    title: '作业单号',
    align: 'center',
    width: 190,
    dataIndex: 'jobCode',
  },
  {
    title: '所属仓库',
    align: 'center',
    width: 120,
    dataIndex: 'warehouseId_dictText',
  },
  {
    title: '作业来源',
    align: 'center',
    width: 110,
    dataIndex: 'sourceType_dictText',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '状态',
    align: 'center',
    width: 100,
    dataIndex: 'status',
    customRender: ({ record }) => {
      const colorMap: Record<number, string> = { 1: 'orange', 2: 'green', 3: 'default' };
      const labelMap: Record<number, string> = { 1: '未确认', 2: '已执行', 3: '已取消' };
      const label = record.status_dictText || labelMap[record.status] || '-';
      return h(Tag, { color: colorMap[record.status] || 'default' }, () => label);
    },
  },
  {
    title: '备注',
    align: 'left',
    dataIndex: 'remark',
    ellipsis: true,
  },
  {
    title: '创建人',
    align: 'center',
    width: 110,
    dataIndex: 'createBy',
  },
  {
    title: '创建时间',
    align: 'center',
    width: 165,
    dataIndex: 'createTime',
  },
];

// 查询表单 Schema
export const searchFormSchema: FormSchema[] = [
  {
    label: '作业单号',
    field: 'jobCode',
    component: 'JInput',
    componentProps: { placeholder: '支持模糊' },
    colProps: { span: 6 },
  },
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
    label: '状态',
    field: 'status',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_operation_status',
      stringToNumber: true,
      placeholder: '请选择状态',
    },
    colProps: { span: 6 },
  },
];

// 新建移库单的表头
export const formSchema: FormSchema[] = [
  {
    label: '所属仓库',
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
    label: '备注',
    field: 'remark',
    component: 'Input',
    componentProps: { placeholder: '如：整箱移位、库位整理', maxlength: 200 },
    colProps: { span: 12 },
  },
];

// 移库明细行（一行为一次「从某库位某批次移到另一库位」）
export const detailColumns: JVxeColumn[] = [
  {
    title: '商品信息',
    key: 'skuId',
    type: JVxeTypes.select,
    width: 200,
    dictCode: 'wms_item_sku_v,text,id',
    allowSearch: true,
    placeholder: '请选择商品信息',
    validateRules: [{ required: true, message: '请选择商品信息' }],
  },
  {
    title: '批次号',
    key: 'batchNumber',
    type: JVxeTypes.input,
    width: 140,
    placeholder: '无批次可留空',
  },
  {
    title: '货主',
    key: 'consignorId',
    type: JVxeTypes.select,
    width: 150,
    dictCode: 'wms_consignor,consignor_name,id,enable_status=1',
    allowSearch: true,
    placeholder: '自有货主可留空',
  },
  {
    title: '质量状态',
    key: 'qualityStatus',
    type: JVxeTypes.select,
    width: 110,
    dictCode: 'wms_quality_status',
    defaultValue: '1',
    placeholder: '默认良品',
  },
  {
    title: '移出库位',
    key: 'sourceWarehouseAreaLocaId',
    type: JVxeTypes.select,
    width: 180,
    dictCode: 'wms_warehouse_area_loca,loca_code,id',
    allowSearch: true,
    placeholder: '请选择移出库位',
    validateRules: [{ required: true, message: '请选择移出库位' }],
  },
  {
    title: '移入库位',
    key: 'targetWarehouseAreaLocaId',
    type: JVxeTypes.select,
    width: 180,
    dictCode: 'wms_warehouse_area_loca,loca_code,id',
    allowSearch: true,
    placeholder: '请选择移入库位',
    validateRules: [{ required: true, message: '请选择移入库位' }],
  },
  {
    title: '数量',
    key: 'count',
    type: JVxeTypes.inputNumber,
    width: 110,
    defaultValue: 1,
    props: { min: 1, precision: 0 },
    validateRules: [
      { required: true, message: '请填写数量' },
      { pattern: '^[1-9]\\d*$', message: '数量必须是大于0的整数' },
    ],
  },
];

// 作业明细展示列（详情弹窗）
export const detailViewColumns: BasicColumn[] = [
  { title: '商品信息', align: 'left', width: 250, ellipsis: true, dataIndex: 'skuId_dictText' },
  { title: '批次号', align: 'center', width: 130, dataIndex: 'batchNumber' },
  { title: '货主', align: 'center', width: 140, dataIndex: 'consignorId_dictText' },
  { title: '质量状态', align: 'center', width: 100, dataIndex: 'qualityStatus_dictText' },
  { title: '移出库区', align: 'center', width: 130, dataIndex: 'sourceWarehouseAreaId_dictText' },
  { title: '移出库位', align: 'center', width: 130, dataIndex: 'sourceWarehouseAreaLocaId_dictText' },
  { title: '移入库区', align: 'center', width: 130, dataIndex: 'targetWarehouseAreaId_dictText' },
  { title: '移入库位', align: 'center', width: 130, dataIndex: 'targetWarehouseAreaLocaId_dictText' },
  { title: '数量', align: 'center', width: 90, dataIndex: 'sourceCount' },
];
