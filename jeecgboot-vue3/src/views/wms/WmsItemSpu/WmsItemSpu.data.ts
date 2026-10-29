import { reactive } from 'vue';
import { BasicColumn, FormSchema } from '/@/components/Table';
import { JVxeColumn, JVxeTypes } from '/@/components/jeecg/JVxeTable/types';
import { list as supplierList } from '../WmsSupplier/WmsSupplier.api';
import { columns as supplierColumns } from '../WmsSupplier/WmsSupplier.data';

const supplierSelectColumns = supplierColumns.filter((col) => col.dataIndex !== 'createBy' && col.dataIndex !== 'createTime');

export const measureUnitState = reactive({
  lengthUnit: '',
  volumeUnit: '',
  weightUnit: '',
});

const lengthUnitOptions = [
  { label: 'mm', value: 'mm' },
  { label: 'cm', value: 'cm' },
  { label: 'dm', value: 'dm' },
  { label: 'm', value: 'm' },
];
const volumeUnitOptions = [
  { label: 'cm³', value: 'cm³' },
  { label: 'dm³', value: 'dm³' },
  { label: 'm³', value: 'm³' },
];
const weightUnitOptions = [
  { label: 'mg', value: 'mg' },
  { label: 'g', value: 'g' },
  { label: 'kg', value: 'kg' },
];

function measureUnitField(label, field, options): FormSchema {
  return {
    label,
    field,
    component: 'Select',
    componentProps: {
      options,
      placeholder: `请选择${label}`,
      getPopupContainer: () => document.body,
      onChange: (value) => {
        measureUnitState[field] = value || '';
      },
    },
    dynamicRules: () => [{ required: true, message: `请选择${label}!` }],
    colProps: { span: 8 },
  };
}

export function syncMeasureUnits(record?) {
  measureUnitState.lengthUnit = record?.lengthUnit || '';
  measureUnitState.volumeUnit = record?.volumeUnit || '';
  measureUnitState.weightUnit = record?.weightUnit || '';
}

const measureFieldUnit = {
  productWeight: 'weightUnit',
  productLength: 'lengthUnit',
  productWidth: 'lengthUnit',
  productHeight: 'lengthUnit',
  productVolume: 'volumeUnit',
};

export function formatMeasure(value, unit) {
  if (value === undefined || value === null || value === '') {
    return '';
  }
  return unit ? `${value} ${unit}` : String(value);
}

export function withMeasureUnit(columns, units) {
  return columns.map((col) => {
    const unit = units?.[measureFieldUnit[col.key]] || '';
    if (!unit) {
      return col;
    }
    return {
      ...col,
      props: { ...(col.props || {}), addonAfter: unit },
      formatter: ({ cellValue }) => formatMeasure(cellValue, unit),
    };
  });
}

function categoryText(record) {
  return record?.categoryId3_dictText || record?.categoryId2_dictText || record?.categoryId1_dictText || '';
}

// 列表列定义。商品编码在商品行取 subTitle，在规格行取规格编码。
export const columns: BasicColumn[] = [
  { title: '序号', align: 'center', dataIndex: 'serialNo', width: 100 },
  { title: '商品编码', align: 'center', dataIndex: 'productCode', width: 140 },
  { title: '商品名称', align: 'center', dataIndex: 'productName', width: 160 },
  { title: '商品类别', align: 'center', dataIndex: 'categoryName', width: 120 },
  { title: '商品描述', align: 'center', dataIndex: 'description', width: 160 },
  { title: '供应商名称', align: 'center', dataIndex: 'supplierId_dictText', width: 140 },
  { title: '品牌', align: 'center', dataIndex: 'brandId_dictText', width: 120 },
  { title: '商品条码', align: 'center', dataIndex: 'code', width: 120 },
  { title: '商品重量', align: 'center', dataIndex: 'productWeight', width: 100 },
  { title: '商品长度', align: 'center', dataIndex: 'productLength', width: 100 },
  { title: '商品宽度', align: 'center', dataIndex: 'productWidth', width: 100 },
  { title: '商品高度', align: 'center', dataIndex: 'productHeight', width: 100 },
  { title: '商品体积', align: 'center', dataIndex: 'productVolume', width: 100 },
  { title: '商品成本', align: 'center', dataIndex: 'realPrice', width: 100 },
  { title: '商品价格', align: 'center', dataIndex: 'price', width: 100 },
  { title: 'ABC分类', align: 'center', dataIndex: 'abcClass', width: 90 },
  { title: '存储条件', align: 'center', dataIndex: 'storageCondition', width: 100 },
  { title: '质检类型', align: 'center', dataIndex: 'qcType', width: 90 },
  { title: '上架策略', align: 'center', dataIndex: 'putawayStrategy', width: 100 },
];

// 查询表单 Schema
export const searchFormSchema: FormSchema[] = [
  {
    label: '商品名称',
    field: 'title',
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
    label: '商品编码',
    field: 'subTitle',
    component: 'Input',
    componentProps: { placeholder: '请输入商品编码', maxlength: 200 },
    dynamicRules: () => {
      return [
        { required: true, message: '请输入商品编码!' },
        { max: 200, message: '商品编码不能超过200个字符' },
      ];
    },
  },
  {
    label: '商品名称',
    field: 'title',
    component: 'Input',
    componentProps: { placeholder: '请输入商品名称', maxlength: 100 },
    dynamicRules: () => {
      return [
        { required: true, message: '请输入商品名称!' },
        { max: 100, message: '商品名称不能超过100个字符' },
      ];
    },
  },
  {
    label: '商品类别',
    field: 'categoryId1',
    component: 'JTreeSelect',
    componentProps: {
      dict: 'wms_item_category,name,id',
      pidField: 'parent_id',
      pidValue: '',
      hasChildField: '',
      condition: '{"is_del":"1"}',
      placeholder: '请选择商品类别',
      treeDefaultExpandAll: false,
    },
  },
  {
    label: '供应商名称',
    field: 'supplierId',
    component: 'JSelectRecord',
    componentProps: {
      api: supplierList,
      columns: supplierSelectColumns,
      rowKey: 'id',
      valueField: 'id',
      labelField: 'supplierName',
      searchField: 'supplierName',
      searchLabel: '供应商名称',
      searchPlaceholder: '请输入供应商名称',
      modalTitle: '选择供应商',
      modalWidth: 1000,
      placeholder: '请选择供应商名称',
      valueType: 'number',
    },
  },
  {
    label: '品牌',
    field: 'brandId',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_brand,name,id,is_del=1',
      stringToNumber: true,
      placeholder: '请选择品牌',
      getPopupContainer: () => document.body,
    },
  },
  {
    label: '商品描述',
    field: 'description',
    component: 'InputTextArea',
    colProps: { span: 24 },
    componentProps: { placeholder: '请输入商品描述', rows: 4, maxlength: 500 },
    dynamicRules: () => {
      return [{ max: 500, message: '商品描述不能超过500个字符' }];
    },
  },
  measureUnitField('长度单位', 'lengthUnit', lengthUnitOptions),
  measureUnitField('体积单位', 'volumeUnit', volumeUnitOptions),
  measureUnitField('重量单位', 'weightUnit', weightUnitOptions),
  {
    label: 'ABC分类',
    field: 'abcClass',
    component: 'JDictSelectTag',
    componentProps: {
      dictCode: 'wms_abc_class',
      placeholder: '请选择ABC分类',
    },
    colProps: { span: 8 },
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
    colProps: { span: 8 },
  },
  {
    label: '是否贵品',
    field: 'isValuable',
    component: 'RadioGroup',
    defaultValue: 0,
    componentProps: {
      options: [
        { label: '否', value: 0 },
        { label: '是', value: 1 },
      ],
    },
    colProps: { span: 8 },
  },
  {
    label: '保质期管理',
    field: 'isShelfLife',
    component: 'RadioGroup',
    defaultValue: 0,
    componentProps: {
      options: [
        { label: '否', value: 0 },
        { label: '是', value: 1 },
      ],
    },
    colProps: { span: 8 },
  },
  {
    label: '保质期(天)',
    field: 'shelfLifeDays',
    component: 'InputNumber',
    componentProps: { min: 1, placeholder: '天数' },
    ifShow: ({ values }) => values.isShelfLife === 1,
    colProps: { span: 8 },
  },
  {
    label: '临期预警(天)',
    field: 'nearExpireDays',
    component: 'InputNumber',
    componentProps: { min: 1, placeholder: '天数' },
    ifShow: ({ values }) => values.isShelfLife === 1,
    colProps: { span: 8 },
  },
];

export function parseItemSpec(spec) {
  const empty = { specCode: '', specName: '', specUnit: '' };
  if (spec === undefined || spec === null || spec === '') {
    return empty;
  }
  try {
    const data = typeof spec === 'string' ? JSON.parse(spec) : spec;
    return {
      specCode: data?.specCode ?? '',
      specName: data?.specName ?? '',
      specUnit: data?.unit ?? '',
    };
  } catch (_error) {
    return empty;
  }
}

export function expandItemSpec(row) {
  return { ...row, ...parseItemSpec(row?.spec) };
}

export function packItemSpec(row) {
  const specCode = row?.specCode == null ? '' : String(row.specCode).trim();
  const specName = row?.specName == null ? '' : String(row.specName).trim();
  const specUnit = row?.specUnit == null ? '' : String(row.specUnit).trim();
  const { specCode: _specCode, specName: _specName, specUnit: _specUnit, ...rest } = row;
  return {
    ...rest,
    spec: JSON.stringify({ specCode, specName, unit: specUnit }),
  };
}

export function toItemTreeRecords(records, pageNo, pageSize, skuMap) {
  const start = (Number(pageNo) - 1) * Number(pageSize);
  return (records || []).map((spu, index) => {
    const serialNo = start + index + 1;
    const children = (skuMap[spu.id] || []).map((sku, skuIndex) => {
      const spec = parseItemSpec(sku.spec);
      return {
        id: `sku-${sku.id}`,
        rowType: 'sku',
        serialNo: `${serialNo}.${skuIndex + 1}`,
        productCode: spec.specCode,
        productName: spec.specName,
        categoryName: spec.specUnit,
        description: '',
        supplierId_dictText: '',
        brandId_dictText: '',
        code: sku.code,
        productWeight: formatMeasure(sku.productWeight, spu.weightUnit),
        productLength: formatMeasure(sku.productLength, spu.lengthUnit),
        productWidth: formatMeasure(sku.productWidth, spu.lengthUnit),
        productHeight: formatMeasure(sku.productHeight, spu.lengthUnit),
        productVolume: formatMeasure(sku.productVolume, spu.volumeUnit),
        realPrice: sku.realPrice,
        price: sku.price,
        abcClass: '-',
        storageCondition: '-',
        qcType: sku.qcType === 1 ? '免检' : sku.qcType === 2 ? '全检' : sku.qcType === 3 ? '抽检' : '-',
        putawayStrategy:
          sku.putawayStrategy === 'ABC'
            ? 'ABC驱动'
            : sku.putawayStrategy === 'FIXED'
              ? '固定库位'
              : sku.putawayStrategy === 'NEAREST'
                ? '就近上架'
                : sku.putawayStrategy || 'ABC驱动',
      };
    });
    const row = {
      ...spu,
      rowType: 'spu',
      serialNo,
      productCode: spu.subTitle || '',
      productName: spu.title,
      categoryName: categoryText(spu),
      abcClass: spu.abcClass || '-',
      storageCondition: spu.storageCondition_dictText || spu.storageCondition || '常温',
      qcType: '-',
      putawayStrategy: '-',
    };
    if (children.length) {
      row.children = children;
    }
    return row;
  });
}

const specTextRule = [{ pattern: '^.{0,100}$', message: '${title}不能超过100个字符' }];

// 弹窗里可编辑的 SKU 列
export const wmsItemSkuJVxeColumns: JVxeColumn[] = [
  {
    title: '规格编码',
    key: 'specCode',
    type: JVxeTypes.input,
    minWidth: 110,
    placeholder: '请输入${title}',
    defaultValue: '',
    validateRules: specTextRule,
  },
  {
    title: '规格名称',
    key: 'specName',
    type: JVxeTypes.input,
    minWidth: 110,
    placeholder: '请输入${title}',
    defaultValue: '',
    validateRules: specTextRule,
  },
  {
    title: '商品单位',
    key: 'specUnit',
    type: JVxeTypes.input,
    minWidth: 90,
    placeholder: '请输入${title}',
    defaultValue: '',
    validateRules: specTextRule,
  },
  {
    title: '商品条码',
    key: 'code',
    type: JVxeTypes.input,
    minWidth: 120,
    placeholder: '请输入${title}',
    defaultValue: '',
    validateRules: [
      { required: true, message: '${title}不能为空' },
      { pattern: 'only', message: '${title}不能重复' },
      { pattern: '^.{1,100}$', message: '${title}不能超过100个字符' },
    ],
  },
  {
    title: '商品重量',
    key: 'productWeight',
    type: JVxeTypes.inputNumber,
    minWidth: 110,
    placeholder: '请输入${title}',
    props: { min: 0, precision: 0 },
  },
  {
    title: '商品长度',
    key: 'productLength',
    type: JVxeTypes.inputNumber,
    minWidth: 110,
    placeholder: '请输入${title}',
    props: { min: 0, precision: 0 },
  },
  {
    title: '商品宽度',
    key: 'productWidth',
    type: JVxeTypes.inputNumber,
    minWidth: 110,
    placeholder: '请输入${title}',
    props: { min: 0, precision: 0 },
  },
  {
    title: '商品高度',
    key: 'productHeight',
    type: JVxeTypes.inputNumber,
    minWidth: 110,
    placeholder: '请输入${title}',
    props: { min: 0, precision: 0 },
  },
  {
    title: '商品体积',
    key: 'productVolume',
    type: JVxeTypes.inputNumber,
    minWidth: 110,
    placeholder: '请输入${title}',
    props: { min: 0, precision: 0 },
  },
  {
    title: '商品成本',
    key: 'realPrice',
    type: JVxeTypes.inputNumber,
    minWidth: 110,
    placeholder: '请输入${title}',
    props: { min: 0, precision: 2 },
  },
  {
    title: '商品价格',
    key: 'price',
    type: JVxeTypes.inputNumber,
    minWidth: 110,
    placeholder: '请输入${title}',
    props: { min: 0, precision: 2 },
  },
  {
    title: '质检类型',
    key: 'qcType',
    type: JVxeTypes.select,
    minWidth: 100,
    options: [
      { text: '免检', value: 1 },
      { text: '全检', value: 2 },
      { text: '抽检', value: 3 },
    ],
    defaultValue: 1,
  },
  {
    title: '一物一码(SN)',
    key: 'isSnManaged',
    type: JVxeTypes.select,
    minWidth: 110,
    options: [
      { text: '否', value: 0 },
      { text: '是', value: 1 },
    ],
    defaultValue: 0,
  },
  {
    title: '安全库存下限',
    key: 'minStock',
    type: JVxeTypes.inputNumber,
    minWidth: 110,
    placeholder: '安全库存',
    props: { min: 0, precision: 0 },
    defaultValue: 0,
  },
  {
    title: '补货库存上限',
    key: 'maxStock',
    type: JVxeTypes.inputNumber,
    minWidth: 110,
    placeholder: '补货上限',
    props: { min: 0, precision: 0 },
    defaultValue: 0,
  },
  {
    title: '推荐上架策略',
    key: 'putawayStrategy',
    type: JVxeTypes.select,
    minWidth: 120,
    options: [
      { text: 'ABC驱动', value: 'ABC' },
      { text: '固定库位', value: 'FIXED' },
      { text: '就近上架', value: 'NEAREST' },
    ],
    defaultValue: 'ABC',
  },
];

// 高级查询配置
export const superQuerySchema = {
  title: { title: '商品名称', order: 0, view: 'text', type: 'string' },
  categoryId1: { title: '商品类别', order: 1, view: 'sel_search', type: 'number', dictTable: 'wms_item_category', dictCode: 'id', dictText: 'name' },
  description: { title: '商品描述', order: 2, view: 'text', type: 'string' },
  supplierId: {
    title: '供应商名称',
    order: 3,
    view: 'sel_search',
    type: 'number',
    dictTable: 'wms_supplier',
    dictCode: 'id',
    dictText: 'supplier_name',
  },
  brandId: { title: '品牌', order: 4, view: 'sel_search', type: 'number', dictTable: 'wms_brand', dictCode: 'id', dictText: 'name' },
};
