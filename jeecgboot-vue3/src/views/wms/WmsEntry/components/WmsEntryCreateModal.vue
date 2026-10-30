<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose title="新建入库单" :width="1150" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
    <a-divider orientation="center" class="detail-divider">入库明细</a-divider>
    <JVxeTable
      keep-source
      resizable
      ref="detailTableRef"
      :columns="createDetailColumns"
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
  import { Icon } from '/@/components/Icon';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { createSchema, createDetailColumns } from '../WmsEntry.data';
  import { addEntry } from '../WmsEntry.api';
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

  const [registerForm, { resetFields, validate }] = useForm({
    schemas: createSchema,
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

  /**
   * 单行选择商品
   */
  function openSkuPicker(row: any, triggerChange: (v: any) => void) {
    pickerTarget.value = { row, triggerChange };
    openSkuPickerModal(true, {
      multiple: false,
      skuId: row.skuId,
      title: '选择入库商品信息',
    });
  }

  /**
   * 商品选择回调
   */
  function onSkuSelected(data: any) {
    const target = pickerTarget.value;
    if (!target) return;
    const row = target.row;
    const selectedItem = Array.isArray(data) ? data[0] : data;
    if (!selectedItem) return;

    pickedSku.value[row.id] = selectedItem;
    target.triggerChange(selectedItem.id);

    // 自动带出商品档案价与供应商
    const updateValues: Recordable = { skuId: selectedItem.id };
    if (selectedItem.price !== null && selectedItem.price !== undefined) {
      updateValues.price = selectedItem.price;
    }
    if (selectedItem.supplierId) {
      updateValues.supplierId = String(selectedItem.supplierId);
    }
    detailTableRef.value?.setValues([{ rowKey: row.id, values: updateValues }]);
    pickerTarget.value = null;
  }

  async function handleSubmit() {
    try {
      const values = await validate();
      const errMap = await detailTableRef.value?.validateTable();
      if (errMap) {
        createMessage.warning('入库明细校验未通过，请检查必选商品与计划数量');
        return;
      }
      const rows: any[] = detailTableRef.value?.getTableData() ?? [];
      const details = rows
        .filter((row) => row.skuId && Number(row.count) > 0)
        .map((row) => ({
          skuId: Number(row.skuId),
          count: Number(row.count),
          price: row.price === undefined || row.price === null || row.price === '' ? null : Number(row.price),
          supplierId: row.supplierId ? Number(row.supplierId) : null,
          prodDate: row.prodDate || null,
          expireDate: row.expireDate || null,
        }));
      if (details.length === 0) {
        createMessage.warning('请至少填写一行入库明细，商品与计划数量不能为空');
        return;
      }
      setModalProps({ confirmLoading: true });
      await addEntry({
        entryType: values.entryType ? Number(values.entryType) : 1,
        warehouseId: Number(values.warehouseId),
        consignorId: Number(values.consignorId),
        predictedArrivalTime: values.predictedArrivalTime || null,
        details,
      });
      createMessage.success('入库单已创建');
      closeModal();
      emit('success');
    } catch (e: any) {
      if (e?.errorFields) return;
      createMessage.error(e?.message || '新建入库单失败');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<style lang="less" scoped>
  .detail-divider {
    margin: 4px 0 16px;
    font-weight: 500;
  }

  .sku-cell {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
    height: 100%;
    cursor: pointer;

    &__tag {
      flex: none;
      margin-inline-end: 0;
    }

    &__title {
      flex: 1 1 auto;
      min-width: 0;
      overflow: hidden;
      font-size: 13px;
      color: #262626;
      white-space: nowrap;
      text-overflow: ellipsis;
    }

    &__action {
      flex: none;
      color: @primary-color;
      font-size: 12px;
    }

    &__placeholder {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      color: #8c8c8c;
      font-size: 13px;

      &:hover {
        color: @primary-color;
      }
    }
  }
</style>
