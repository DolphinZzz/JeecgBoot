<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose title="新建出库单" :width="1180" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
    <a-divider orientation="center" class="detail-divider">出库明细清单</a-divider>
    <JVxeTable
      keep-source
      resizable
      ref="detailTableRef"
      :columns="detailColumns"
      :dataSource="tableData.dataSource"
      :height="320"
      :rowNumber="true"
      :toolbar="true"
    >
      <!-- 商品信息选择插槽 -->
      <template #skuSlot="{ row, triggerChange }">
        <div
          class="picker-cell"
          :title="pickedSku[row.id] ? `${pickedSku[row.id].skuCode} - ${pickedSku[row.id].spuName}` : '点击选择商品信息'"
          @click="openSkuPicker(row, triggerChange)"
        >
          <template v-if="pickedSku[row.id]">
            <a-tag class="picker-cell__tag" color="blue">{{ pickedSku[row.id].skuCode }}</a-tag>
            <span class="picker-cell__title">{{ pickedSku[row.id].spuName }}</span>
            <span class="picker-cell__action">更换</span>
          </template>
          <span v-else class="picker-cell__placeholder"> <Icon icon="ant-design:plus-circle-outlined" /> 点击选择商品 </span>
        </div>
      </template>

      <!-- 出库库位选择插槽 -->
      <template #locaSlot="{ row, triggerChange }">
        <div
          class="picker-cell"
          :title="row.warehouseAreaLocaId ? `库位: ${row.locaCode || row.warehouseAreaLocaId}` : '点击选择在库库位'"
          @click="openLocaPicker(row, triggerChange)"
        >
          <template v-if="row.warehouseAreaLocaId">
            <a-tag class="picker-cell__tag" color="cyan">{{ row.locaCode || `库位${row.warehouseAreaLocaId}` }}</a-tag>
            <span v-if="row.batchNumber" class="picker-cell__batch">[{{ row.batchNumber }}]</span>
            <span class="picker-cell__action">更换</span>
          </template>
          <span v-else class="picker-cell__placeholder picker-cell__placeholder--loca">
            <Icon icon="ant-design:environment-outlined" /> 点击选择商品所在库位
          </span>
        </div>
      </template>

      <!-- 可用库存插槽 -->
      <template #availableCountSlot="{ row }">
        <div v-if="row.availableCount !== undefined && row.availableCount !== null" style="text-align: center">
          <a-tag color="green" style="font-weight: bold; font-size: 13px"> {{ row.availableCount }} 件 </a-tag>
        </div>
        <div v-else style="text-align: center; color: #bbb">-</div>
      </template>
    </JVxeTable>

    <!-- 商品信息选择器弹窗 -->
    <WmsItemSkuPickerModal @register="registerSkuPicker" @selected="onSkuSelected" />

    <!-- 商品在库库位选择器弹窗 -->
    <WmsInventoryLocaPickerModal @register="registerLocaPicker" @selected="onLocaSelected" />
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
  import WmsInventoryLocaPickerModal from '../../components/WmsInventoryLocaPickerModal.vue';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);

  const detailTableRef = ref();
  const tableData = reactive({ dataSource: [] as any[] });

  /** row.id -> 已选商品详情 (WmsItemSkuSelectVo) */
  const pickedSku = ref<Record<string, any>>({});
  /** 单行点击选择SKU时暂存上下文 */
  const skuPickerTarget = ref<{ row: any; triggerChange: (v: any) => void } | null>(null);
  /** 单行点击选择库位时暂存上下文 */
  const locaPickerTarget = ref<{ row: any; triggerChange: (v: any) => void } | null>(null);

  const [registerSkuPicker, { openModal: openSkuPickerModal }] = useModal();
  const [registerLocaPicker, { openModal: openLocaPickerModal }] = useModal();

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
      component: 'Select',
      defaultValue: 1,
      required: true,
      componentProps: {
        options: [
          { label: '销售出库', value: 1 },
          { label: 'MES工单领料出库', value: 2 },
          { label: '采购退货', value: 3 },
        ],
        placeholder: '请选择出库类型',
      },
      colProps: { span: 12 },
    },
    {
      label: 'MES工单号',
      field: 'mesWorkOrderNo',
      component: 'Input',
      componentProps: {
        placeholder: 'MES工单领料出库必填',
        maxlength: 64,
      },
      colProps: { span: 12 },
      ifShow: ({ values }) => values.outboundType === 2,
      dynamicRules: ({ values }) => {
        return values.outboundType === 2 ? [{ required: true, message: 'MES工单领料出库必须录入MES生产工单号!' }] : [];
      },
    },
    {
      label: '客户',
      field: 'customerId',
      component: 'JSearchSelect',
      componentProps: {
        dict: 'wms_customer,customer_name,id',
        placeholder: '请选择客户',
      },
      colProps: { span: 12 },
      ifShow: ({ values }) => values.outboundType === 1,
      dynamicRules: ({ values }) => {
        return values.outboundType === 1 ? [{ required: true, message: '销售出库必须选择客户!' }] : [];
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
      width: '260px',
      validateRules: [{ required: true, message: '请选择出库商品信息' }],
    },
    {
      title: '出库库位',
      key: 'warehouseAreaLocaId',
      type: JVxeTypes.slot,
      slotName: 'locaSlot',
      width: '260px',
      validateRules: [{ required: true, message: '请选择出库库位' }],
    },
    {
      title: '可用库存',
      key: 'availableCount',
      type: JVxeTypes.slot,
      slotName: 'availableCountSlot',
      width: '120px',
      align: 'center',
    },
    {
      title: '计划出库数',
      key: 'outCount',
      type: JVxeTypes.inputNumber,
      width: '130px',
      defaultValue: 1,
      validateRules: [
        { required: true, message: '计划出库数不能为空' },
        { pattern: /^[1-9]\d*$/, message: '必须大于 0' },
      ],
    },
    {
      title: '在库批次号',
      key: 'batchNumber',
      type: JVxeTypes.normal,
      width: '140px',
      align: 'center',
    },
    {
      title: '缺货策略',
      key: 'shortageHandle',
      type: JVxeTypes.select,
      width: '130px',
      defaultValue: 1,
      options: [
        { value: 1, text: '剔除缺货行' },
        { value: 2, text: '整单挂起' },
      ],
    },
  ];

  const [registerForm, { resetFields, validate, getFieldsValue }] = useForm({
    schemas: createFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 12 },
    labelWidth: 110,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async () => {
    await resetFields();
    tableData.dataSource = [];
    pickedSku.value = {};
    skuPickerTarget.value = null;
    locaPickerTarget.value = null;
    setModalProps({ confirmLoading: false });
  });

  function openSkuPicker(row: any, triggerChange: (v: any) => void) {
    skuPickerTarget.value = { row, triggerChange };
    openSkuPickerModal(true, {
      multiple: false,
      skuId: row.skuId,
      title: '选择出库商品信息',
    });
  }

  function onSkuSelected(data: any) {
    const target = skuPickerTarget.value;
    if (!target) return;
    const row = target.row;
    const selectedItem = Array.isArray(data) ? data[0] : data;
    if (!selectedItem) return;

    pickedSku.value[row.id] = selectedItem;
    target.triggerChange(selectedItem.id);

    // 重新选定商品后，重置库位与库存数据
    row.warehouseAreaLocaId = null;
    row.warehouseAreaId = null;
    row.locaCode = null;
    row.batchNumber = null;
    row.availableCount = null;
    row.outCount = 1;
  }

  function openLocaPicker(row: any, triggerChange: (v: any) => void) {
    const values = getFieldsValue();
    if (!values?.warehouseId) {
      createMessage.warning('请先在上方表单中选择出库仓库！');
      return;
    }
    if (!row.skuId) {
      createMessage.warning('请先选择该明细行的出库商品！');
      return;
    }

    locaPickerTarget.value = { row, triggerChange };
    openLocaPickerModal(true, {
      warehouseId: values.warehouseId,
      skuId: row.skuId,
      sku: pickedSku.value[row.id],
      currentLocaId: row.warehouseAreaLocaId,
    });
  }

  function onLocaSelected(locaInv: any) {
    const target = locaPickerTarget.value;
    if (!target) return;
    const row = target.row;
    row.warehouseAreaLocaId = locaInv.warehouseAreaLocaId;
    row.warehouseAreaId = locaInv.warehouseAreaId;
    row.locaCode = locaInv.locaCode;
    row.batchNumber = locaInv.batchNumber || '';
    row.availableCount = locaInv.availableCount || 0;
    row.consignorId = locaInv.consignorId;

    if (!row.outCount || Number(row.outCount) <= 0) {
      row.outCount = 1;
    }

    target.triggerChange(locaInv.warehouseAreaLocaId);
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

      // 明细与库位库存硬校验
      for (let i = 0; i < tableRows.length; i++) {
        const r = tableRows[i];
        const lineNo = i + 1;
        if (!r.skuId) {
          createMessage.warning(`第 ${lineNo} 行未选择商品信息！`);
          return;
        }
        if (!r.warehouseAreaLocaId) {
          const skuLabel = pickedSku.value[r.id]?.skuCode || r.skuId;
          createMessage.warning(`第 ${lineNo} 行商品 [${skuLabel}] 未选择出库库位！`);
          return;
        }
        const outCount = Number(r.outCount);
        if (!outCount || outCount <= 0) {
          createMessage.warning(`第 ${lineNo} 行计划出库数必须大于 0！`);
          return;
        }
        const availableCount = Number(r.availableCount || 0);
        if (outCount > availableCount) {
          createMessage.error(
            `第 ${lineNo} 行商品在库位 [${r.locaCode || r.warehouseAreaLocaId}] 的计划出库数 (${outCount}) 超出库位可用库存 (${availableCount})，不允许出库！`
          );
          return;
        }
      }

      const details = tableRows.map((r: any) => ({
        skuId: r.skuId,
        warehouseAreaLocaId: r.warehouseAreaLocaId,
        warehouseAreaId: r.warehouseAreaId,
        batchNumber: r.batchNumber || null,
        outCount: Number(r.outCount),
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

  .picker-cell {
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

    &__batch {
      font-size: 12px;
      color: #8c8c8c;
      flex-shrink: 0;
    }

    &__action {
      margin-left: auto;
      font-size: 12px;
      color: #1890ff;
      flex-shrink: 0;
    }

    &__placeholder {
      font-size: 13px;
      color: #8c8c8c;
      display: flex;
      align-items: center;
      gap: 4px;

      &--loca {
        color: #fa8c16;
      }
    }
  }
</style>
