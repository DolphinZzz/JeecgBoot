import { h } from 'vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

// 列表列定义
export const columns: BasicColumn[] = [
  {
    title: '变动时间',
    align: 'center',
    width: 165,
    dataIndex: 'createTime',
    sorter: true,
  },
  {
    title: '业务类型',
    align: 'center',
    width: 110,
    dataIndex: 'bizType_dictText',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '关联单号',
    align: 'center',
    width: 175,
    dataIndex: 'bizNo',
    customRender: ({ text }) => text || '-',
  },
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
    width: 120,
    dataIndex: 'batchNumber',
    customRender: ({ text }) => text || '-',
  },
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
  {
    title: '变动前可用',
    align: 'center',
    width: 110,
    dataIndex: 'beforeCount',
  },
  {
    title: '变动后可用',
    align: 'center',
    width: 110,
    dataIndex: 'afterCount',
  },
  {
    title: '货主',
    align: 'center',
    width: 130,
    dataIndex: 'consignorName',
    customRender: ({ text }) => text || '自有',
  },
  {
    title: '操作人',
    align: 'center',
    width: 110,
    dataIndex: 'createBy',
  },
  {
    title: '备注',
    align: 'left',
    width: 200,
    ellipsis: true,
    dataIndex: 'remark',
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
    label: '业务类型',
    field: 'bizType',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_inventory_biz_type',
      stringToNumber: true,
      placeholder: '请选择业务类型',
    },
    colProps: { span: 6 },
  },
  {
    label: '关联单号',
    field: 'bizNo',
    component: 'JInput',
    componentProps: { placeholder: '支持模糊' },
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
    label: '变动时间',
    field: 'changeTime',
    component: 'RangePicker',
    componentProps: {
      showTime: true,
      valueFormat: 'YYYY-MM-DD HH:mm:ss',
      placeholder: ['开始时间', '结束时间'],
    },
    colProps: { span: 12 },
  },
];
