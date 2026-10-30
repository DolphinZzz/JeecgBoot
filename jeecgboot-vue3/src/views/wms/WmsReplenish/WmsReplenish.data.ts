import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

// 列表列定义
export const columns: BasicColumn[] = [
  {
    title: '补货单号',
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
    width: 120,
    dataIndex: 'sourceType_dictText',
    customRender: ({ text }) => text || '系统补货',
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
    label: '补货单号',
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

// 作业明细展示列
export const detailViewColumns: BasicColumn[] = [
  { title: '商品信息', align: 'center', width: 140, dataIndex: 'skuId_dictText' },
  { title: '批次号', align: 'center', width: 130, dataIndex: 'batchNumber' },
  { title: '货主', align: 'center', width: 140, dataIndex: 'consignorId_dictText' },
  { title: '质量状态', align: 'center', width: 100, dataIndex: 'qualityStatus_dictText' },
  { title: '移出库区（存储区）', align: 'center', width: 150, dataIndex: 'sourceWarehouseAreaId_dictText' },
  { title: '移出库位', align: 'center', width: 130, dataIndex: 'sourceWarehouseAreaLocaId_dictText' },
  { title: '移入库区（拣货区）', align: 'center', width: 150, dataIndex: 'targetWarehouseAreaId_dictText' },
  { title: '移入库位', align: 'center', width: 130, dataIndex: 'targetWarehouseAreaLocaId_dictText' },
  { title: '补货数量', align: 'center', width: 100, dataIndex: 'sourceCount' },
];
