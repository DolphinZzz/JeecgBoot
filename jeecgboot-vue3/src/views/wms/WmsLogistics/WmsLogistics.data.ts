import { BasicColumn, FormSchema } from '/@/components/Table';

export const columns: BasicColumn[] = [
  {
    title: '承运商名称',
    align: 'center',
    dataIndex: 'companyName',
    width: 180,
  },
  {
    title: '起步价 (元)',
    align: 'center',
    dataIndex: 'minPrice',
    width: 120,
    customRender: ({ text }) => (text !== undefined && text !== null ? `¥${text}` : '-'),
  },
  {
    title: '首重价 (元)',
    align: 'center',
    dataIndex: 'firstPrice',
    width: 120,
    customRender: ({ text }) => (text !== undefined && text !== null ? `¥${text}` : '-'),
  },
  {
    title: '续重价 (元)',
    align: 'center',
    dataIndex: 'continutePrice',
    width: 120,
    customRender: ({ text }) => (text !== undefined && text !== null ? `¥${text}` : '-'),
  },
  {
    title: '联系人',
    align: 'center',
    dataIndex: 'linkUser',
    width: 140,
    customRender: ({ text }) => text || '-',
  },
  {
    title: '联系电话',
    align: 'center',
    dataIndex: 'linkePhone',
    width: 150,
    customRender: ({ text }) => text || '-',
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
    label: '承运商名称',
    field: 'companyName',
    component: 'JInput',
    colProps: { span: 8 },
  },
  {
    label: '联系人',
    field: 'linkUser',
    component: 'JInput',
    colProps: { span: 8 },
  },
  {
    label: '联系电话',
    field: 'linkePhone',
    component: 'JInput',
    colProps: { span: 8 },
  },
];

export const formSchema: FormSchema[] = [
  {
    label: '',
    field: 'id',
    component: 'Input',
    show: false,
  },
  {
    label: '承运商名称',
    field: 'companyName',
    component: 'Input',
    required: true,
    componentProps: {
      placeholder: '请输入承运商/物流公司名称，如：顺丰速运',
      maxlength: 100,
    },
  },
  {
    label: '起步价 (元)',
    field: 'minPrice',
    component: 'InputNumber',
    defaultValue: 0,
    componentProps: {
      min: 0,
      precision: 2,
      style: { width: '100%' },
      placeholder: '请输入起步价',
    },
  },
  {
    label: '首重价 (元)',
    field: 'firstPrice',
    component: 'InputNumber',
    defaultValue: 0,
    componentProps: {
      min: 0,
      precision: 2,
      style: { width: '100%' },
      placeholder: '请输入首重价',
    },
  },
  {
    label: '续重价 (元)',
    field: 'continutePrice',
    component: 'InputNumber',
    defaultValue: 0,
    componentProps: {
      min: 0,
      precision: 2,
      style: { width: '100%' },
      placeholder: '请输入续重价',
    },
  },
  {
    label: '联系人',
    field: 'linkUser',
    component: 'Input',
    componentProps: {
      placeholder: '请输入对接人姓名',
      maxlength: 50,
    },
  },
  {
    label: '联系电话',
    field: 'linkePhone',
    component: 'Input',
    componentProps: {
      placeholder: '请输入联系电话',
      maxlength: 30,
    },
  },
];
