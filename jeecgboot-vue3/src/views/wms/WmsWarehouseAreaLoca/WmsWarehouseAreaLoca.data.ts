import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';

// 列表列定义
export const columns: BasicColumn[] = [
  {
    title: '所属仓库',
    align: 'center',
    width: 130,
    dataIndex: 'warehouseId_dictText',
  },
  {
    title: '所属库区',
    align: 'center',
    width: 140,
    dataIndex: 'warehouseAreaId_dictText',
  },
  {
    title: '库位编号',
    align: 'center',
    width: 140,
    dataIndex: 'locaCode',
    customRender: ({ text }) => h('span', { style: { fontWeight: 'bold', color: '#1890ff' } }, text),
  },
  {
    title: '库位状态',
    align: 'center',
    width: 100,
    dataIndex: 'locaStatus',
    customRender: ({ record }) => {
      const s = record.locaStatus;
      if (s === 1) return h(Tag, { color: 'green' }, () => '空闲');
      if (s === 2) return h(Tag, { color: 'orange' }, () => '占用');
      if (s === 3) return h(Tag, { color: 'red' }, () => '盘点锁定');
      if (s === 4) return h(Tag, { color: 'default' }, () => '禁用');
      return record.locaStatus_dictText || '-';
    },
  },
  {
    title: '固定SKU',
    align: 'center',
    width: 130,
    dataIndex: 'fixedSkuId_dictText',
    customRender: ({ text }) => text || '非固定',
  },
  {
    title: '巷道/货架/层/位',
    align: 'center',
    width: 150,
    dataIndex: 'tunnelNumber',
    customRender: ({ record }) => {
      const parts = [record.tunnelNumber, record.shelfNumber, record.layerNumber, record.positionNumber];
      if (parts.every((p) => p === null || p === undefined || p === '')) return '-';
      return parts.map((p) => (p === null || p === undefined || p === '' ? '—' : p)).join(' / ');
    },
  },
  {
    title: '存储条件',
    align: 'center',
    width: 100,
    dataIndex: 'storageCondition_dictText',
    customRender: ({ text }) => text || '常温',
  },
  {
    title: '长×宽×高(mm)',
    align: 'center',
    width: 150,
    dataIndex: 'locaLen',
    customRender: ({ record }) => {
      const { locaLen, locaWidth, locaHeight } = record;
      if (!locaLen && !locaWidth && !locaHeight) return '-';
      return `${locaLen ?? '-'} × ${locaWidth ?? '-'} × ${locaHeight ?? '-'}`;
    },
  },
  {
    title: 'ABC分类',
    align: 'center',
    width: 90,
    dataIndex: 'abcClass',
    customRender: ({ text }) => {
      if (!text) return '-';
      const colorMap: Record<string, string> = { A: 'green', B: 'blue', C: 'orange' };
      return h(Tag, { color: colorMap[text] || 'default' }, () => text);
    },
  },
  {
    title: '容积(cm³)',
    align: 'center',
    dataIndex: 'localVolume',
  },
  {
    title: '承重(g)',
    align: 'center',
    dataIndex: 'localBearingCapacity',
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
    label: '所属库区',
    field: 'warehouseAreaId',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_warehouse_area,name,id',
      stringToNumber: true,
      placeholder: '请选择库区',
    },
    colProps: { span: 6 },
  },
  {
    label: '库位编号',
    field: 'locaCode',
    component: 'JInput',
    colProps: { span: 6 },
  },
  {
    label: 'ABC分类',
    field: 'abcClass',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_abc_class',
      placeholder: '请选择等级',
    },
    colProps: { span: 6 },
  },
  {
    label: '库位状态',
    field: 'locaStatus',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_loca_status',
      stringToNumber: true,
      placeholder: '请选择状态',
    },
    colProps: { span: 6 },
  },
  {
    label: '存储条件',
    field: 'storageCondition',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_storage_condition',
      placeholder: '请选择存储条件',
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
    colProps: { span: 12 },
  },
  {
    label: '所属库区',
    field: 'warehouseAreaId',
    component: 'JDictSelectTag',
    required: true,
    componentProps: {
      dictCode: 'wms_warehouse_area,name,id',
      stringToNumber: true,
      placeholder: '请选择所属库区',
    },
    colProps: { span: 12 },
  },
  {
    label: '库位编号',
    field: 'locaCode',
    component: 'Input',
    required: true,
    componentProps: {
      placeholder: '请输入库位编号，如 01-A-03-02',
      maxlength: 100,
    },
    colProps: { span: 12 },
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
    colProps: { span: 12 },
  },
  {
    label: '巷道号',
    field: 'tunnelNumber',
    component: 'Input',
    componentProps: { placeholder: '如 01' },
    colProps: { span: 8 },
  },
  {
    label: '货架号',
    field: 'shelfNumber',
    component: 'Input',
    componentProps: { placeholder: '如 A' },
    colProps: { span: 8 },
  },
  {
    label: '层号',
    field: 'layerNumber',
    component: 'Input',
    componentProps: { placeholder: '如 03' },
    colProps: { span: 8 },
  },
  {
    label: '位号',
    field: 'positionNumber',
    component: 'Input',
    componentProps: { placeholder: '如 02' },
    colProps: { span: 8 },
  },
  {
    label: '长(mm)',
    field: 'locaLen',
    component: 'InputNumber',
    componentProps: { min: 0, placeholder: '长(mm)' },
    colProps: { span: 8 },
  },
  {
    label: '宽(mm)',
    field: 'locaWidth',
    component: 'InputNumber',
    componentProps: { min: 0, placeholder: '宽(mm)' },
    colProps: { span: 8 },
  },
  {
    label: '高(mm)',
    field: 'locaHeight',
    component: 'InputNumber',
    componentProps: { min: 0, placeholder: '高(mm)' },
    colProps: { span: 8 },
  },
  {
    label: '容积(cm³)',
    field: 'localVolume',
    component: 'InputNumber',
    helpMessage: '留空将由长宽高自动计算',
    componentProps: { min: 0, placeholder: '容积(cm³)' },
    colProps: { span: 12 },
  },
  {
    label: '承重(g)',
    field: 'localBearingCapacity',
    component: 'InputNumber',
    componentProps: { min: 0, placeholder: '承重(g)' },
    colProps: { span: 12 },
  },
  {
    label: '拣货距离(m)',
    field: 'pickDistance',
    component: 'InputNumber',
    helpMessage: '到拣货/复核区路径距离(米)，参与ABC库位得分计算',
    componentProps: { min: 0, placeholder: '路径距离(米)' },
    colProps: { span: 12 },
  },
  {
    label: '收货距离(m)',
    field: 'receiveDistance',
    component: 'InputNumber',
    helpMessage: '到收货区路径距离(米)',
    componentProps: { min: 0, placeholder: '路径距离(米)' },
    colProps: { span: 12 },
  },
  {
    label: '人体工学层级',
    field: 'ergonomicLevel',
    component: 'JDictSelectTag',
    defaultValue: 2,
    helpMessage: '1黄金层(0.8-1.5m), 2次优层(0.3-0.8m或1.5-2m), 3不便层(地面或高位)',
    componentProps: {
      dictCode: 'wms_ergonomic_level',
      stringToNumber: true,
      placeholder: '请选择层级',
    },
    colProps: { span: 12 },
  },
  {
    label: '库位状态',
    field: 'locaStatus',
    component: 'JDictSelectTag',
    defaultValue: 1,
    required: true,
    componentProps: {
      dictCode: 'wms_loca_status',
      stringToNumber: true,
      placeholder: '请选择状态',
    },
    colProps: { span: 12 },
  },
  {
    label: '固定SKU',
    field: 'fixedSkuId',
    component: 'JDictSelectTag',
    helpMessage: '绑定固定SKU后，仅可存放该SKU',
    componentProps: {
      dictCode: 'wms_item_sku_v,text,id',
      stringToNumber: true,
      placeholder: '选择绑定的SKU(选填)',
    },
    colProps: { span: 12 },
  },
  {
    label: 'ABC等级',
    field: 'abcClass',
    component: 'Input',
    dynamicDisabled: true,
    helpMessage: '由库位等级计算服务自动维护，也可手动指定',
    colProps: { span: 6 },
  },
  {
    label: '库位得分',
    field: 'locaScore',
    component: 'InputNumber',
    dynamicDisabled: true,
    helpMessage: '依据距离与人体工学计算生成的分值',
    colProps: { span: 6 },
  },
];

export const superQuerySchema = {
  warehouseId: { title: '所属仓库', order: 1, view: 'list', type: 'number', dictTable: 'wms_warehouse', dicText: 'name', dicCode: 'id' },
  warehouseAreaId: { title: '所属库区', order: 2, view: 'list', type: 'number', dictTable: 'wms_warehouse_area', dicText: 'name', dicCode: 'id' },
  locaCode: { title: '库位编号', order: 3, view: 'text', type: 'string' },
  abcClass: { title: 'ABC分类', order: 4, view: 'list', type: 'string', dictCode: 'wms_abc_class' },
  locaStatus: { title: '库位状态', order: 5, view: 'list', type: 'number', dictCode: 'wms_loca_status' },
  ergonomicLevel: { title: '人体工学', order: 6, view: 'list', type: 'number', dictCode: 'wms_ergonomic_level' },
};
