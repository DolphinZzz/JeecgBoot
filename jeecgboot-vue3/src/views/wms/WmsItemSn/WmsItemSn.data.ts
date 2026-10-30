import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

// 列表列定义
export const columns: BasicColumn[] = [
  {
    title: 'SN序列号',
    align: 'center',
    width: 200,
    dataIndex: 'snCode',
  },
  {
    title: '商品信息',
    align: 'left',
    width: 250,
    ellipsis: true,
    dataIndex: 'skuCode',
    customRender: ({ text, record }) => `${text || record.skuId_dictText || '-'} ${record.spuTitle || ''}`.trim(),
  },
  {
    title: '批次号',
    align: 'center',
    width: 130,
    dataIndex: 'batchNumber',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '仓库',
    align: 'center',
    width: 120,
    dataIndex: 'warehouseName',
    customRender: ({ text, record }) => text || record.warehouseId_dictText || '-',
  },
  {
    title: '库位',
    align: 'center',
    width: 120,
    dataIndex: 'locaCode',
    customRender: ({ text, record }) => text || record.warehouseAreaLocaId_dictText || '-',
  },
  {
    title: '质量状态',
    align: 'center',
    width: 100,
    dataIndex: 'qualityStatus_dictText',
    customRender: ({ text }) => text || '-',
  },
  {
    title: 'SN状态',
    align: 'center',
    width: 110,
    dataIndex: 'status_dictText',
    customRender: ({ text, record }) => {
      const colorMap: Record<number, string> = { 1: 'gold', 2: 'green', 3: 'red', 4: 'blue', 5: 'default', 6: 'default' };
      const label = text || record.status;
      return label ? h(Tag, { color: colorMap[record.status] || 'default' }, () => label) : '-';
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
    title: '入库单ID',
    align: 'center',
    width: 120,
    dataIndex: 'entryOrderId',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '出库单ID',
    align: 'center',
    width: 120,
    dataIndex: 'outboundOrderId',
    customRender: ({ text }) => text || '-',
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
    label: 'SN序列号',
    field: 'snCode',
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
    label: 'SKU编码',
    field: 'skuId',
    component: 'JSearchSelect',
    componentProps: {
      dict: 'wms_item_sku_v,text,id',
      placeholder: '请选择SKU',
    },
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
    label: 'MES工单号',
    field: 'mesWorkOrderNo',
    component: 'JInput',
    componentProps: { placeholder: '精确匹配' },
    colProps: { span: 6 },
  },
  {
    label: 'SN状态',
    field: 'status',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_sn_status',
      stringToNumber: true,
      placeholder: '请选择SN状态',
    },
    colProps: { span: 6 },
  },
];
