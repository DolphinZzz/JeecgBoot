import { BasicColumn, FormSchema } from '/@/components/Table';

/**
 * 工序设置台账列表列定义
 */
export const columns: BasicColumn[] = [
  {
    title: '工序编码',
    align: 'center',
    dataIndex: 'processCode',
    width: 150,
  },
  {
    title: '工序名称',
    align: 'center',
    dataIndex: 'processName',
    width: 180,
  },
  {
    title: '工序说明',
    align: 'center',
    dataIndex: 'description',
  },
  {
    title: '是否启用',
    align: 'center',
    dataIndex: 'isEnabled',
    width: 110,
  },
  {
    title: '备注',
    align: 'center',
    dataIndex: 'remark',
  },
];

/**
 * 工序设置台账顶部搜索表单
 */
export const searchFormSchema: FormSchema[] = [
  {
    label: '工序编码',
    field: 'processCode',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: '工序名称',
    field: 'processName',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: '是否启用',
    field: 'isEnabled',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: {
      placeholder: '请选择是否启用',
      options: [
        { label: '是', value: 1 },
        { label: '否', value: 2 },
      ],
    },
  },
];

/**
 * 工序操作步骤明细子表列定义
 */
export const operationColumns = [
  {
    title: '顺序编号',
    dataIndex: 'stepOrder',
    key: 'stepOrder',
    align: 'center',
    width: 90,
  },
  {
    title: '步骤说明',
    dataIndex: 'stepDescription',
    key: 'stepDescription',
    align: 'center',
  },
  {
    title: '辅助设备',
    dataIndex: 'auxiliaryEquipment',
    key: 'auxiliaryEquipment',
    align: 'center',
    width: 160,
  },
  {
    title: '辅助材料',
    dataIndex: 'auxiliaryMaterials',
    key: 'auxiliaryMaterials',
    align: 'center',
    width: 160,
  },
  {
    title: '备注',
    dataIndex: 'remark',
    key: 'remark',
    align: 'center',
    width: 160,
  },
  {
    title: '操作',
    key: 'action',
    align: 'center',
    width: 130,
  },
];
