import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

// 列表列定义
export const columns: BasicColumn[] = [
  {
    title: '所属仓库',
    align: 'center',
    width: 140,
    dataIndex: 'warehouseId_dictText',
  },
  {
    title: '库区名称',
    align: 'left',
    width: 160,
    dataIndex: 'name',
    customRender: ({ text }) => h('span', { style: { fontWeight: 'bold', color: '#1890ff' } }, text),
  },
  {
    title: '库区类别',
    align: 'center',
    width: 120,
    dataIndex: 'category_dictText',
  },
  {
    title: '存储条件',
    align: 'center',
    width: 100,
    dataIndex: 'storageCondition_dictText',
    customRender: ({ text }) => text || '常温',
  },
  {
    title: '总库位数',
    align: 'center',
    width: 95,
    dataIndex: 'totalLocaCount',
    customRender: ({ text }) => h('span', { style: { fontWeight: 'bold' } }, `${text ?? 0}`),
  },
  {
    title: '空闲库位',
    align: 'center',
    width: 110,
    dataIndex: 'emptyLocaCount',
    customRender: ({ text }) => h(Tag, { color: 'green' }, () => `${text ?? 0} 空闲`),
  },
  {
    title: '占用库位',
    align: 'center',
    width: 110,
    dataIndex: 'occupiedLocaCount',
    customRender: ({ text }) => h(Tag, { color: (text && text > 0) ? 'orange' : 'default' }, () => `${text ?? 0} 占用`),
  },
  {
    title: '在库总件数',
    align: 'center',
    width: 110,
    dataIndex: 'totalItemCount',
    customRender: ({ text }) => h('span', { style: { fontWeight: 'bold', color: '#1890ff' } }, `${text ?? 0} 件`),
  },
  {
    title: 'ABC分类',
    align: 'center',
    width: 90,
    dataIndex: 'abcClass_dictText',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '创建时间',
    align: 'center',
    width: 160,
    dataIndex: 'createTime',
  },
];

// 查询表单 Schema
export const searchFormSchema: FormSchema[] = [
  {
    label: '所属仓库',
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
    label: '库区名称',
    field: 'name',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: '库区类别',
    field: 'category',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_warehouse_area_category',
      placeholder: '请选择类别',
    },
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
    label: '所属仓库',
    field: 'warehouseId',
    component: 'JDictSelectTag',
    required: true,
    componentProps: {
      dictCode: 'wms_warehouse,name,id',
      stringToNumber: true,
      placeholder: '请选择所属仓库',
    },
  },
  {
    label: '库区名称',
    field: 'name',
    component: 'Input',
    required: true,
    componentProps: {
      placeholder: '请输入库区名称',
      maxlength: 200,
    },
  },
  {
    label: '库区类别',
    field: 'category',
    component: 'JDictSelectTag',
    defaultValue: 'STORAGE',
    required: true,
    componentProps: {
      dictCode: 'wms_warehouse_area_category',
      placeholder: '请选择库区类别',
    },
  },
  {
    label: '存储条件',
    field: 'storageCondition',
    component: 'JDictSelectTag',
    defaultValue: 'NORMAL',
    componentProps: {
      dictCode: 'wms_storage_condition',
      placeholder: '请选择存储条件',
    },
  },
  {
    label: 'ABC分类',
    field: 'abcClass',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_abc_class',
      placeholder: '可选ABC分类',
    },
  },
];

export const superQuerySchema = {
  warehouseId: { title: '所属仓库', order: 1, view: 'list', type: 'number', dictTable: 'wms_warehouse', dicText: 'name', dicCode: 'id' },
  name: { title: '库区名称', order: 2, view: 'text', type: 'string' },
  category: { title: '库区类别', order: 3, view: 'list', type: 'string', dictCode: 'wms_warehouse_area_category' },
};
