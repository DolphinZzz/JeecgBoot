import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { BasicColumn, FormSchema } from '/@/components/Table';
import { render } from '/@/utils/common/renderUtils';
import { duplicateCheckDelay } from '/@/views/system/user/user.api';

/**
 * 列表列配置
 */
export const columns: BasicColumn[] = [
  {
    title: '规则编码',
    dataIndex: 'ruleCode',
    width: 160,
    align: 'center',
    slots: { customRender: 'ruleCode' },
  },
  {
    title: '规则名称',
    dataIndex: 'ruleName',
    width: 180,
    align: 'center',
  },
  {
    title: '最大长度',
    dataIndex: 'maxLength',
    width: 100,
    align: 'center',
    customRender: ({ text }) => text || '-',
  },
  {
    title: '是否补齐',
    dataIndex: 'isPadded',
    width: 100,
    align: 'center',
    customRender: ({ text }) => {
      const isY = text === 'Y';
      return h(Tag, { color: isY ? 'blue' : 'default' }, () => (isY ? '是' : '否'));
    },
  },
  {
    title: '是否可用',
    dataIndex: 'enableFlag',
    width: 100,
    align: 'center',
    customRender: ({ text }) => {
      const isEnable = text === 1 || text === '1';
      return h(Tag, { color: isEnable ? 'green' : 'red' }, () => (isEnable ? '是' : '否'));
    },
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
 * 列表搜索表单
 */
export const searchFormSchema: FormSchema[] = [
  {
    field: 'ruleCode',
    label: '规则编码',
    component: 'JInput',
    colProps: { span: 6 },
    componentProps: {
      placeholder: '请输入规则编码',
    },
  },
  {
    field: 'ruleName',
    label: '规则名称',
    component: 'JInput',
    colProps: { span: 6 },
    componentProps: {
      placeholder: '请输入规则名称',
    },
  },
  {
    field: 'enableFlag',
    label: '启用状态',
    component: 'Select',
    colProps: { span: 6 },
    componentProps: {
      placeholder: '请选择启用状态',
      options: [
        { label: '是', value: 1 },
        { label: '否', value: 0 },
      ],
      allowClear: true,
    },
  },
];

/**
 * 新增/编辑模态框表单 Schema
 */
export const formSchema: FormSchema[] = [
  {
    field: 'id',
    label: '',
    component: 'Input',
    show: false,
  },
  {
    field: 'ruleCode',
    label: '规则编码',
    component: 'Input',
    colProps: { span: 24 },
    dynamicDisabled: ({ values }) => !!values.id,
    dynamicRules: ({ model }) => [
      {
        required: true,
        validator: (_, value) => {
          return new Promise<void>((resolve, reject) => {
            if (!value) {
              return reject('请输入规则编码！');
            }
            const params = {
              tableName: 'sys_auto_code_rule',
              fieldName: 'rule_code',
              fieldVal: value,
              dataId: model.id,
            };
            duplicateCheckDelay(params)
              .then((res: any) => {
                res.success ? resolve() : reject('规则编码已存在！');
              })
              .catch((err) => {
                reject(err.message || '校验失败');
              });
          });
        },
      },
    ],
    componentProps: {
      placeholder: '请输入规则编码',
    },
  },
  {
    field: 'ruleName',
    label: '规则名称',
    component: 'Input',
    required: true,
    colProps: { span: 24 },
    componentProps: {
      placeholder: '请输入规则名称',
    },
  },
  {
    field: 'ruleDesc',
    label: '描述',
    component: 'Input',
    colProps: { span: 24 },
    componentProps: {
      placeholder: '请输入描述信息',
    },
  },
  {
    field: 'maxLength',
    label: '最大长度',
    component: 'InputNumber',
    colProps: { span: 24 },
    componentProps: {
      min: 1,
      precision: 0,
      placeholder: '请输入最大长度',
      style: { width: '100%' },
    },
  },
  {
    field: 'isPadded',
    label: '是否补齐',
    component: 'RadioGroup',
    defaultValue: 'Y',
    colProps: { span: 24 },
    componentProps: {
      options: [
        { label: '是', value: 'Y' },
        { label: '否', value: 'N' },
      ],
    },
  },
  {
    field: 'paddedChar',
    label: '补齐字符',
    component: 'Input',
    colProps: { span: 24 },
    componentProps: {
      placeholder: '请输入补齐字符',
      maxlength: 10,
    },
    // 联动逻辑：当“是否补齐”为否('N')时动态隐藏（对应图2）
    ifShow: ({ values }) => values.isPadded === 'Y',
  },
  {
    field: 'paddedMethod',
    label: '补齐方式',
    component: 'RadioGroup',
    defaultValue: 'L',
    colProps: { span: 24 },
    componentProps: {
      options: [
        { label: '左补齐', value: 'L' },
        { label: '右补齐', value: 'R' },
      ],
    },
    // 联动逻辑：当“是否补齐”为否('N')时动态隐藏（对应图2）
    ifShow: ({ values }) => values.isPadded === 'Y',
  },
  {
    field: 'enableFlag',
    label: '是否启用',
    component: 'RadioGroup',
    defaultValue: 1,
    colProps: { span: 24 },
    componentProps: {
      options: [
        { label: '是', value: 1 },
        { label: '否', value: 0 },
      ],
    },
  },
  {
    field: 'remark',
    label: '备注',
    component: 'InputTextArea',
    colProps: { span: 24 },
    componentProps: {
      rows: 4,
      placeholder: '请输入内容',
    },
  },
];
