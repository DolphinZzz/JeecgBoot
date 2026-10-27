import { BasicColumn, FormSchema } from '/@/components/Table';

export const columns: BasicColumn[] = [
  {
    title: '分类名字',
    align: 'left',
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

export const searchFormSchema: FormSchema[] = [
  {
    label: '分类名字',
    field: 'name',
    component: 'JInput',
    colProps: { span: 6 },
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
    label: '分类名字',
    field: 'name',
    component: 'Input',
    componentProps: { placeholder: '请输入分类名字' },
    dynamicRules: () => {
      return [
        { required: true, message: '请输入分类名字!' },
        { max: 50, message: '分类名字不能超过50个字符!' },
      ];
    },
  },
  {
    label: '所属类别',
    field: 'parentId',
    component: 'JTreeSelect',
    componentProps: {
      dict: 'wms_item_category,name,id',
      pidField: 'parent_id',
      pidValue: '',
      condition: '{"is_del":"1"}',
      placeholder: '不选则为顶级类别',
    },
  },
  {
    label: '排序',
    field: 'sort',
    component: 'InputNumber',
    componentProps: { placeholder: '请输入排序', precision: 0 },
  },
];

export const superQuerySchema = {
  name: { title: '分类名字', order: 0, view: 'text', type: 'string' },
  sort: { title: '排序', order: 1, view: 'number', type: 'number' },
  createBy: { title: '创建人', order: 2, view: 'text', type: 'string' },
  createTime: { title: '创建日期', order: 3, view: 'datetime', type: 'string' },
};
