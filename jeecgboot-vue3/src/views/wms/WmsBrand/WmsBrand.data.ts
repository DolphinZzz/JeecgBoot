import { BasicColumn, FormSchema } from '/@/components/Table';

// 列表列定义
export const columns: BasicColumn[] = [
  {
    title: '品牌名字',
    align: 'center',
    dataIndex: 'name',
  },
  {
    title: '排序',
    align: 'center',
    dataIndex: 'sort',
  },
  {
    title: '创建人',
    align: 'center',
    dataIndex: 'createBy',
  },
  {
    title: '创建日期',
    align: 'center',
    dataIndex: 'createTime',
  },
];

// 查询表单 Schema
export const searchFormSchema: FormSchema[] = [
  {
    label: '品牌名字',
    field: 'name',
    component: 'JInput',
    colProps: { span: 6 },
  },
];

// 编辑表单 Schema
export const formSchema: FormSchema[] = [
  {
    label: '',
    field: 'id',
    component: 'Input',
    show: false,
  },
  {
    label: '品牌名字',
    field: 'name',
    component: 'Input',
    componentProps: { placeholder: '请输入品牌名字', maxlength: 100 },
    dynamicRules: () => {
      return [
        { required: true, message: '请输入品牌名字!' },
        { max: 100, message: '品牌名字不能超过100个字符' },
      ];
    },
  },
  {
    label: '排序',
    field: 'sort',
    component: 'InputNumber',
    componentProps: { placeholder: '请输入排序', style: 'width: 100%' },
  },
];

// 高级查询配置
export const superQuerySchema = {
  name: { title: '品牌名字', order: 0, view: 'text', type: 'string' },
  sort: { title: '排序', order: 1, view: 'number', type: 'number' },
  createBy: { title: '创建人', order: 2, view: 'text', type: 'string' },
  createTime: { title: '创建日期', order: 3, view: 'datetime', type: 'string' },
};
