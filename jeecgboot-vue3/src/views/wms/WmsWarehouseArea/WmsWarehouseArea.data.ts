import { BasicColumn, FormSchema } from '/@/components/Table';

// 列表列定义
export const columns: BasicColumn[] = [
  {
    title: '所属仓库',
    align: 'center',
    dataIndex: 'warehouseId_dictText',
  },
  {
    title: '库区名称',
    align: 'center',
    dataIndex: 'name',
  },
  {
    title: '库区类别',
    align: 'center',
    dataIndex: 'category_dictText',
  },
  {
    title: '存储条件',
    align: 'center',
    dataIndex: 'storageCondition_dictText',
    customRender: ({ text }) => text || '常温',
  },
  {
    title: 'ABC分类',
    align: 'center',
    dataIndex: 'abcClass_dictText',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '分区类型',
    align: 'center',
    dataIndex: 'zoneType_dictText',
    customRender: ({ text }) => text || '通用区',
  },
  {
    title: '创建时间',
    align: 'center',
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
  {
    label: '分区类型',
    field: 'zoneType',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_zone_type',
      placeholder: '请选择分区',
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
  {
    label: '分区类型',
    field: 'zoneType',
    component: 'JDictSelectTag',
    defaultValue: 'GENERAL',
    componentProps: {
      dictCode: 'wms_zone_type',
      placeholder: '请选择分区类型',
    },
  },
];

export const superQuerySchema = {
  warehouseId: { title: '所属仓库', order: 1, view: 'list', type: 'number', dictTable: 'wms_warehouse', dicText: 'name', dicCode: 'id' },
  name: { title: '库区名称', order: 2, view: 'text', type: 'string' },
  category: { title: '库区类别', order: 3, view: 'list', type: 'string', dictCode: 'wms_warehouse_area_category' },
  zoneType: { title: '分区类型', order: 4, view: 'list', type: 'string', dictCode: 'wms_zone_type' },
};
