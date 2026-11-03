import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

/** 出库单状态颜色与文案 */
export const statusColorMap: Record<number, string> = {
  10: 'default',     // 待分配
  20: 'processing',  // 已分配
  30: 'warning',     // 待补货
  40: 'blue',        // 拣货中
  50: 'cyan',        // 待复核
  60: 'purple',      // 待打包
  70: 'gold',        // 待发货
  80: 'success',     // 已发货
  90: 'success',     // 已签收
  91: 'error',       // 已取消
  92: 'error',       // 异常挂起
};

export const statusTextMap: Record<number, string> = {
  10: '待分配',
  20: '已分配',
  30: '待补货',
  40: '拣货中',
  50: '待复核',
  60: '待打包',
  70: '待发货',
  80: '已发货',
  90: '已签收',
  91: '已取消',
  92: '异常挂起',
};

/** 出库类型文案与徽标颜色 */
export const outboundTypeMap: Record<number, { text: string; color: string }> = {
  1: { text: '销售出库', color: 'blue' },
  2: { text: 'MES工单领料', color: 'purple' },
  3: { text: '采购退货出库', color: 'orange' },
  4: { text: '调拨出库', color: 'cyan' },
};

export const columns: BasicColumn[] = [
  {
    title: '出库单号',
    align: 'center',
    dataIndex: 'outboundCode',
    width: 170,
  },
  {
    title: '出库类型',
    align: 'center',
    dataIndex: 'outboundType',
    width: 130,
    customRender: ({ record, text }) => {
      const typeInfo = outboundTypeMap[record.outboundType];
      if (typeInfo) {
        return h(Tag, { color: typeInfo.color }, () => typeInfo.text);
      }
      return record.outboundType_dictText || text || '-';
    },
  },
  {
    title: '状态',
    align: 'center',
    dataIndex: 'status',
    width: 110,
    customRender: ({ record, text }) => {
      const color = statusColorMap[record.status] || 'default';
      const label = statusTextMap[record.status] || record.status_dictText || text || '-';
      return h(Tag, { color }, () => label);
    },
  },
  {
    title: '仓库',
    align: 'center',
    dataIndex: 'warehouseName',
    width: 130,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '货主',
    align: 'center',
    dataIndex: 'consignorName',
    width: 140,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '客户',
    align: 'center',
    dataIndex: 'customerName',
    width: 140,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '计划总数',
    align: 'center',
    dataIndex: 'totalCount',
    width: 90,
  },
  {
    title: '已分配数',
    align: 'center',
    dataIndex: 'allocatedTotal',
    width: 90,
    customRender: ({ record }) => {
      const count = record.allocatedTotal ?? record.allocatedCount ?? 0;
      return h(
        'span',
        { style: { color: count < (record.totalCount || 0) ? '#faad14' : '#52c41a', fontWeight: 'bold' } },
        String(count),
      );
    },
  },
  {
    title: '实拣数',
    align: 'center',
    dataIndex: 'pickedTotal',
    width: 90,
    customRender: ({ record }) => {
      const count = record.pickedTotal ?? record.pickedCount ?? 0;
      return h('span', { style: { fontWeight: 'bold' } }, String(count));
    },
  },
  {
    title: 'MES工单号',
    align: 'center',
    dataIndex: 'mesWorkOrderNo',
    width: 140,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '承运商',
    align: 'center',
    dataIndex: 'logisticsCompanyName',
    width: 120,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '物流单号',
    align: 'center',
    dataIndex: 'logisticsNumber',
    width: 140,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '运费(元)',
    align: 'center',
    dataIndex: 'freightFee',
    width: 100,
    customRender: ({ text }) => (text != null ? `¥${Number(text).toFixed(2)}` : '-'),
  },
  {
    title: '发运时间',
    align: 'center',
    dataIndex: 'shipTime',
    width: 160,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '订单日期',
    align: 'center',
    dataIndex: 'orderDate',
    width: 120,
    customRender: ({ text }) => (text ? text.substring(0, 10) : '-'),
  },
  {
    title: '创建时间',
    align: 'center',
    dataIndex: 'createTime',
    width: 160,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    label: '出库单号',
    field: 'outboundCode',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: '出库仓库',
    field: 'warehouseId',
    component: 'JSearchSelect',
    componentProps: {
      dict: 'wms_warehouse,name,id',
      placeholder: '请选择仓库',
    },
    colProps: { span: 6 },
  },
  {
    label: '出库类型',
    field: 'outboundType',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_outbound_type',
      stringToNumber: true,
      placeholder: '请选择类型',
    },
    colProps: { span: 6 },
  },
  {
    label: '单据状态',
    field: 'status',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_outbound_status',
      stringToNumber: true,
      placeholder: '请选择状态',
    },
    colProps: { span: 6 },
  },
  {
    label: '货主',
    field: 'consignorId',
    component: 'JSearchSelect',
    componentProps: {
      dict: 'wms_consignor,consignor_name,id',
      placeholder: '请选择货主',
    },
    colProps: { span: 6 },
  },
  {
    label: '客户',
    field: 'customerId',
    component: 'JSearchSelect',
    componentProps: {
      dict: 'wms_customer,customer_name,id',
      placeholder: '请选择客户',
    },
    colProps: { span: 6 },
  },
  {
    label: 'MES工单号',
    field: 'mesWorkOrderNo',
    component: 'JInput',
    colProps: { span: 6 },
  },
];
