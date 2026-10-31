<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    title="新建出库单"
    :width="1150"
    @ok="handleSubmit"
  >
    <BasicForm @register="registerForm" />
    <a-divider orientation="center" class="detail-divider">出库明细</a-divider>
    <JVxeTable
      keep-source
      resizable
      ref="detailTableRef"
      :columns="detailColumns"
      :dataSource="tableData.dataSource"
      :height="300"
      :rowNumber="true"
      :toolbar="true"
    >
      <template #skuSlot="{ row, triggerChange }">
        <div
          class="sku-cell"
          :title="pickedSku[row.id] ? `${pickedSku[row.id].skuCode} - ${pickedSku[row.id].spuName}` : '点击选择商品信息'"
          @click="openSkuPicker(row, triggerChange)"
        >
          <template v-if="pickedSku[row.id]">
            <a-tag class="sku-cell__tag" color="blue">{{ pickedSku[row.id].skuCode }}</a-tag>
            <span class="sku-cell__title">{{ pickedSku[row.id].spuName }}</span>
            <span class="sku-cell__action">更换</span>
          </template>
          <span v-else class="sku-cell__placeholder">
            <Icon icon="ant-design:plus-circle-outlined" /> 点击选择商品信息
          </span>
        </div>
      </template>
    </JVxeTable>

    <!-- 商品信息选择器弹窗 -->
    <WmsItemSkuPickerModal @register="registerSkuPicker" @selected="onSkuSelected" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import { BasicModal, useModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { JVxeTable } from '/@/components/jeecg/JVxeTable';
  import { JVxeTypes, JVxeColumn } from '/@/components/jeecg/JVxeTable/types';
  import { FormSchema } from '/@/components/Table';
  import { Icon } from '/@/components/Icon';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { createOutboundOrder } from '../WmsOutbound.api';
  import WmsItemSkuPickerModal from '../../components/WmsItemSkuPickerModal.vue';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);

  const detailTableRef = ref();
  const tableData = reactive({ dataSource: [] as any[] });

  /** row.id -> 已选商品详情 (WmsItemSkuSelectVo) */
  const pickedSku = ref<Record<string, any>>({});
  /** 单行点击选择时暂存上下文 */
  const pickerTarget = ref<{ row: any; triggerChange: (v: any) => void } | null>(null);

  const [registerSkuPicker, { openModal: openSkuPickerModal }] = useModal();

  const createFormSchema: FormSchema[] = [
    {
      label: '出库仓库',
      field: 'warehouseId',
      component: 'JSearchSelect',
      required: true,
      componentProps: {
        dict: 'wms_warehouse,name,id',
        placeholder: '请选择仓库',
      },
      colProps: { span: 12 },
    },
    {
      label: '出库类型',
      field: 'outboundType',
      component: 'JDictSelectTag',
      defaultValue: 1,
      required: true,
      componentProps: {
        dictCode: 'wms_outbound_type',
        stringToNumber: true,
        placeholder: '请选择出库类型',
      },
      colProps: { span: 12 },
    },
    {
      label: '货主',
      field: 'consignorId',
      component: 'JSearchSelect',
      required: true,
      componentProps: {
        dict: 'wms_consignor,consignor_name,id',
        placeholder: '请选择货主',
      },
      colProps: { span: 12 },
    },
    {
      label: '客户',
      field: 'customerId',
      component: 'JSearchSelect',
      componentProps: {
        dict: 'wms_customer,customer_name,id',
        placeholder: '销售出库时必选',
      },
      colProps: { span: 12 },
      dynamicRules: ({ values }) => {
        return values.outboundType === 1 ? [{ required: true, message: '销售出库必须选择客户!' }] : [];
      },
    },
    {
      label: 'MES工单号',
      field: 'mesWorkOrderNo',
      component: 'Input',
      componentProps: {
        placeholder: '工业仓工单领料必填',
        maxlength: 64,
      },
      colProps: { span: 12 },
      dynamicRules: ({ values }) => {
        return values.outboundType === 2
          ? [{ required: true, message: 'MES工单领料必须录入生产工单号!' }]
          : [];
      },
    },
    {
      label: '订单日期',
      field: 'orderDate',
      component: 'DatePicker',
      componentProps: {
        valueFormat: 'YYYY-MM-DD',
        style: { width: '100%' },
      },
      colProps: { span: 12 },
    },
    {
      label: '备注',
      field: 'remark',
      component: 'InputTextArea',
      componentProps: {
        rows: 2,
        maxlength: 255,
        placeholder: '选填',
      },
      colProps: { span: 24 },
    },
  ];

  const detailColumns: JVxeColumn[] = [
    {
      title: '商品信息',
      key: 'skuId',
      type: JVxeTypes.slot,
      slotName: 'skuSlot',
      width: '280px',
      validateRules: [{ required: true, message: '请选择出库商品信息' }],
    },
    {
      title: '计划出库数',
      key: 'outCount',
      type: JVxeTypes.inputNumber,
      width: '120px',
      defaultValue: 1,
      validateRules: [
        { required: true, message: '计划出库数不能为空' },
        { pattern: /^[1-9]\d*$/, message: '必须大于 0' },
      ],
    },
    {
      title: '指定批次号',
      key: 'batchNumber',
      type: JVxeTypes.input,
      width: '160px',
      placeholder: '选填/MES指定',
    },
    {
      title: '缺货策略',
      key: 'shortageHandle',
      type: JVxeTypes.select,
      width: '140px',
      defaultValue: 1,
      options: [
        { value: 1, text: '剔除缺货行' },
        { value: 2, text: '整单挂起' },
      ],
    },
  ];

  const [registerForm, { resetFields, validate }] = useForm({
    schemas: createFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 12 },
    labelWidth: 110,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async () => {
    await resetFields();
    tableData.dataSource = [];
    pickedSku.value = {};
    pickerTarget.value = null;
    setModalProps({ confirmLoading: false });
  });

  function openSkuPicker(row: any, triggerChange: (v: any) => void) {
    pickerTarget.value = { row, triggerChange };
    openSkuPickerModal(true, {
      multiple: false,
      skuId: row.skuId,
      title: '选择出库商品信息',
    });
  }

  function onSkuSelected(data: any) {
    const target = pickerTarget.value;
    if (!target) return;
    const row = target.row;
    const selectedItem = Array.isArray(data) ? data[0] : data;
    if (!selectedItem) return;

    pickedSku.value[row.id] = selectedItem;
    target.triggerChange(selectedItem.id);
  }

  async function handleSubmit() {
    try {
      const formValues = await validate();
      const err = await detailTableRef.value.validateTable();
      if (err) {
        return;
      }
      const tableRows = detailTableRef.value.getTableData();
      if (!tableRows || tableRows.length === 0) {
        createMessage.warning('请至少添加一行出库明细！');
        return;
      }

      const details = tableRows.map((r: any) => ({
        skuId: r.skuId,
        outCount: Number(r.outCount),
        batchNumber: r.batchNumber || null,
        shortageHandle: Number(r.shortageHandle || 1),
      }));

      const payload = {
        ...formValues,
        details,
      };

      setModalProps({ confirmLoading: true });
      await createOutboundOrder(payload);
      createMessage.success('出库单创建成功！');
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<style lang="less" scoped>
  .detail-divider {
    margin: 12px 0 16px;
    font-size: 14px;
    font-weight: 500;
  }

  .sku-cell {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    min-height: 28px;
    padding: 2px 6px;
    border-radius: 4px;
    transition: background-color 0.2s;

    &:hover {
      background-color: #f0f5ff;
    }

    &__tag {
      margin-right: 0;
      flex-shrink: 0;
    }

    &__title {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 13px;
      color: #333;
    }

    &__action {
      flex-shrink: 0;
      font-size: 12px;
      color: #1890ff;
    }

    &__placeholder {
      font-size: 13px;
      color: #999;
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
</style>
