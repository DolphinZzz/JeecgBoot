import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

export const waveStatusColorMap: Record<number, string> = {
  1: 'default',     // 待拣货
  2: 'processing',  // 拣货中
  3: 'blue',        // 已拣货
  4: 'orange',      // 分货中
  5: 'success',     // 已完成
  9: 'error',       // 已取消
};

export const waveStatusTextMap: Record<number, string> = {
  1: '待拣货',
  2: '拣货中',
  3: '已拣货',
  4: '分货中',
  5: '已完成',
  9: '已取消',
};

export const storageConditionColorMap: Record<string, string> = {
  常温: 'default',
  冷藏: 'cyan',
  冷冻: 'blue',
  阴凉: 'green',
  恒温恒湿: 'purple',
};

export const columns: BasicColumn[] = [
  {
    title: '波次编号',
    align: 'center',
    dataIndex: 'waveCode',
    width: 170,
  },
  {
    title: '仓库',
    align: 'center',
    dataIndex: 'warehouseName',
    width: 130,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '物流承运商',
    align: 'center',
    dataIndex: 'logisticsCompanyName',
    width: 140,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '存储温区',
    align: 'center',
    dataIndex: 'storageCondition',
    width: 110,
    customRender: ({ text }) => {
      const color = storageConditionColorMap[text] || 'default';
      return h(Tag, { color }, () => text || '常温');
    },
  },
  {
    title: '包含订单数',
    align: 'center',
    dataIndex: 'orderCount',
    width: 110,
    customRender: ({ text }) => h('b', String(text || 0)),
  },
  {
    title: 'SKU品类数',
    align: 'center',
    dataIndex: 'skuCount',
    width: 100,
    customRender: ({ text }) => String(text || 0),
  },
  {
    title: '商品总件数',
    align: 'center',
    dataIndex: 'totalCount',
    width: 110,
    customRender: ({ text }) => h('span', { style: { color: '#1890ff', fontWeight: 'bold' } }, String(text || 0)),
  },
  {
    title: '状态',
    align: 'center',
    dataIndex: 'status',
    width: 110,
    customRender: ({ record, text }) => {
      const color = waveStatusColorMap[record.status] || 'default';
      const label = waveStatusTextMap[record.status] || record.status_dictText || text || '-';
      return h(Tag, { color }, () => label);
    },
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
    label: '波次编号',
    field: 'waveCode',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: '所属仓库',
    field: 'warehouseId',
    component: 'JSearchSelect',
    componentProps: {
      dict: 'wms_warehouse,name,id',
      placeholder: '请选择仓库',
    },
    colProps: { span: 6 },
  },
  {
    label: '存储温区',
    field: 'storageCondition',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_storage_condition',
      placeholder: '请选择温区',
    },
    colProps: { span: 6 },
  },
  {
    label: '波次状态',
    field: 'status',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_wave_status',
      stringToNumber: true,
      placeholder: '请选择状态',
    },
    colProps: { span: 6 },
  },
];
