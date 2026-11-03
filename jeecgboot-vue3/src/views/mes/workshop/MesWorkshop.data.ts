import { BasicColumn, FormSchema } from '/@/components/Table';
import { render } from '/@/utils/common/renderUtils';
//列表数据
export const columns: BasicColumn[] = [
   {
    title: '车间编码',
    align:"center",
    dataIndex: 'workshopCode'
   },
   {
    title: '车间名称',
    align:"center",
    dataIndex: 'workshopName'
   },
   {
    title: '面积（平方米）',
    align:"center",
    dataIndex: 'area'
   },
   {
    title: '负责人',
    align:"center",
    dataIndex: 'responsiblePersonId_dictText'
   },
   {
    title: '是否启用',
    align:"center",
    dataIndex: 'isEnabled',
    customRender:({text}) => {
       return  render.renderSwitch(text, [{text:'是',value:'1'},{text:'否',value:'0'}])
     },
   },
   {
    title: '备注',
    align:"center",
    dataIndex: 'remarks'
   },
];
//查询数据
export const searchFormSchema: FormSchema[] = [
  {
    label: "车间编码",
    field: "workshopCode",
    component: 'JInput',
  },
  {
    label: "车间名称",
    field: "workshopName",
    component: 'JInput',
  },
];
//表单数据
export const formSchema: FormSchema[] = [
  {
    label: '车间编码',
    field: 'workshopCode',
    component: 'Input',
    slot: 'workshopCode',
    dynamicRules: () => {
          return [
                 { required: true, message: '请输入车间编码!'},
          ];
     },
  },
  {
    label: '车间名称',
    field: 'workshopName',
    component: 'Input',
    dynamicRules: () => {
          return [
                 { required: true, message: '请输入车间名称!'},
          ];
     },
  },
  {
    label: '面积（平方米）',
    field: 'area',
    component: 'InputNumber',
  },
  {
    label: '负责人',
    field: 'responsiblePersonId',
    component: 'JSelectUserByDept',
    componentProps:{
      showButton: false,
      isRadioSelection: true,
      rowKey: 'id',
      labelKey: 'realname',
      placeholder: '请选择负责人',
    },
  },
  {
    label: '是否启用',
    field: 'isEnabled',
     component: 'JSwitch',
     defaultValue: '1',
     componentProps:{
       options: ['1', '0'],
     },
    dynamicRules: () => {
          return [
                 { required: true, message: '请选择是否启用!'},
          ];
     },
  },
  {
    label: '备注',
    field: 'remarks',
    component: 'InputTextArea',
  },
	// TODO 主键隐藏字段，目前写死为ID
	{
	  label: '',
	  field: 'id',
	  component: 'Input',
	  show: false
	},
];

// 高级查询数据
export const superQuerySchema = {
  workshopCode: {title: '车间编码',order: 0,view: 'text', type: 'string',},
  workshopName: {title: '车间名称',order: 1,view: 'text', type: 'string',},
  area: {title: '面积（平方米）',order: 2,view: 'number', type: 'number',},
  responsiblePersonId: {title: '负责人',order: 3,view: 'text', type: 'string',},
  isEnabled: {title: '是否启用',order: 4,view: 'number', type: 'number',},
  remarks: {title: '备注',order: 5,view: 'textarea', type: 'string',},
};

/**
* 流程表单调用这个方法获取formSchema
* @param param
*/
export function getBpmFormSchema(_formData): FormSchema[]{
  // 默认和原始表单保持一致 如果流程中配置了权限数据，这里需要单独处理formSchema
  return formSchema;
}
