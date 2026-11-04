import { BasicColumn, FormSchema } from '/@/components/Table';
import { render } from '/@/utils/common/renderUtils';

/**
 * 规则组成列表列配置
 */
export const columns: BasicColumn[] = [
  {
    title: '组成编码',
    dataIndex: 'partCode',
    width: 140,
    align: 'center',
  },
  {
    title: '组成名称',
    dataIndex: 'partName',
    width: 160,
    align: 'center',
  },
  {
    title: '分段序号',
    dataIndex: 'partIndex',
    width: 90,
    align: 'center',
  },
  {
    title: '分段类型',
    dataIndex: 'partType_dictText',
    width: 130,
    align: 'center',
    customRender: ({ text, record }) => {
      if (text) {
        return text;
      }
      const typeMap: Record<string, string> = {
        INPUTCHAR: '输入字符',
        NOWDATE: '当前日期时间',
        FIXCHAR: '固定字符',
        SERIALNO: '流水号',
      };
      return typeMap[record.partType] || record.partType || '-';
    },
  },
  {
    title: '分段长度',
    dataIndex: 'partLength',
    width: 90,
    align: 'center',
    customRender: ({ text }) => (text !== null && text !== undefined ? text : '-'),
  },
  {
    title: '备注',
    dataIndex: 'remark',
    width: 200,
    align: 'center',
    customRender: ({ text }) => render.renderTip(text, 25),
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
    align: 'center',
  },
];

/**
 * 列表搜索表单 Schema
 */
export const searchFormSchema: FormSchema[] = [
  {
    field: 'partCode',
    label: '组成编码',
    component: 'JInput',
    colProps: { span: 6 },
    componentProps: {
      placeholder: '请输入组成编码',
    },
  },
  {
    field: 'partName',
    label: '组成名称',
    component: 'JInput',
    colProps: { span: 6 },
    componentProps: {
      placeholder: '请输入组成名称',
    },
  },
];

/**
 * 添加/编辑规则组成模态框表单 Schema
 */
export const formSchema: FormSchema[] = [
  {
    field: 'id',
    label: '',
    component: 'Input',
    show: false,
  },
  {
    field: 'ruleId',
    label: '',
    component: 'Input',
    show: false,
  },
  // 第 1 行：分段编码、分段名称
  {
    field: 'partCode',
    label: '分段编码',
    component: 'Input',
    required: true,
    colProps: { span: 12 },
    componentProps: {
      placeholder: '请输入分段编码',
    },
  },
  {
    field: 'partName',
    label: '分段名称',
    component: 'Input',
    required: true,
    colProps: { span: 12 },
    componentProps: {
      placeholder: '请输入分段名称',
    },
  },
  // 第 2 行：分段序号、分段长度
  {
    field: 'partIndex',
    label: '分段序号',
    component: 'InputNumber',
    required: true,
    defaultValue: 0,
    colProps: { span: 12 },
    componentProps: {
      min: 0,
      precision: 0,
      style: { width: '100%' },
      placeholder: '请输入分段序号',
    },
  },
  {
    field: 'partLength',
    label: '分段长度',
    component: 'InputNumber',
    defaultValue: 0,
    colProps: { span: 12 },
    componentProps: {
      min: 0,
      precision: 0,
      style: { width: '100%' },
      placeholder: '请输入分段长度',
    },
  },
  // 第 3 行：分段类型
  {
    field: 'partType',
    label: '分段类型',
    component: 'JDictSelectTag',
    required: true,
    colProps: { span: 12 },
    componentProps: {
      dictCode: 'sys_autocode_parttype',
      placeholder: '请选择',
      showChooseOption: false,
      getPopupContainer: () => document.body,
      placement: 'bottomLeft',
      style: { width: '100%' },
      stringToNumber: false,
    },
  },
  // 联动项 1：【图3】分段类型为输入字符时展示“输入字符”
  {
    field: 'inputCharacter',
    label: '输入字符',
    component: 'Input',
    colProps: { span: 12 },
    componentProps: {
      placeholder: '请填写输入字符',
      style: { width: '100%' },
    },
    ifShow: ({ values }) => values.partType === 'INPUTCHAR',
  },
  // 联动项 2：【图4】分段类型为当前日期时间时展示“日期时间格式”
  {
    field: 'dateFormat',
    label: '日期时间格式',
    component: 'Input',
    colProps: { span: 12 },
    componentProps: {
      placeholder: '请输入日期时间格式',
      style: { width: '100%' },
    },
    ifShow: ({ values }) => values.partType === 'NOWDATE',
  },
  // 联动项 3：【图5】分段类型为固定字符时展示“固定字符”
  {
    field: 'fixCharacter',
    label: '固定字符',
    component: 'Input',
    colProps: { span: 12 },
    componentProps: {
      placeholder: '请填写固定字符',
      style: { width: '100%' },
    },
    ifShow: ({ values }) => values.partType === 'FIXCHAR',
  },
  // 联动项 4：【图6】分段类型为流水号时展示“起始流水号”
  {
    field: 'serialStartNo',
    label: '起始流水号',
    component: 'InputNumber',
    colProps: { span: 12 },
    componentProps: {
      min: 0,
      precision: 0,
      style: { width: '100%' },
      placeholder: '请填写起始流水号',
    },
    ifShow: ({ values }) => values.partType === 'SERIALNO',
  },
  // 联动项 5：【图6】流水号步长
  {
    field: 'serialStep',
    label: '流水号步长',
    component: 'InputNumber',
    defaultValue: 1,
    colProps: { span: 12 },
    componentProps: {
      min: 1,
      precision: 0,
      style: { width: '100%' },
      placeholder: '请填写流水号步长',
    },
    ifShow: ({ values }) => values.partType === 'SERIALNO',
  },
  // 联动项 6：【图6】是否循环（单选：是/否，默认否）
  {
    field: 'cycleFlag',
    label: '是否循环',
    component: 'RadioGroup',
    defaultValue: 'N',
    colProps: { span: 12 },
    componentProps: {
      options: [
        { label: '是', value: 'Y' },
        { label: '否', value: 'N' },
      ],
    },
    ifShow: ({ values }) => values.partType === 'SERIALNO',
  },
  // 联动项 7：【图7】当是否循环为“是”时，展示“循环方式”
  {
    field: 'cycleMethod',
    label: '循环方式',
    component: 'JDictSelectTag',
    colProps: { span: 12 },
    componentProps: {
      dictCode: 'sys_autocode_cyclemethod',
      placeholder: '请选择',
      showChooseOption: false,
      getPopupContainer: () => document.body,
      placement: 'bottomLeft',
      style: { width: '100%' },
      stringToNumber: false,
    },
    ifShow: ({ values }) => values.partType === 'SERIALNO' && values.cycleFlag === 'Y',
  },
  // 备注：单列排版
  {
    field: 'remark',
    label: '备注',
    component: 'InputTextArea',
    colProps: { span: 24 },
    componentProps: {
      rows: 3,
      placeholder: '请输入内容',
    },
  },
];
