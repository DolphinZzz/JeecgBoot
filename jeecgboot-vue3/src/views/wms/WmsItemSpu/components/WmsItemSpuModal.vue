<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose :title="title" :width="1200" @ok="handleSubmit">
    <BasicForm @register="registerForm" ref="formRef" name="WmsItemSpuForm" />
    <a-divider orientation="center" class="sku-divider">商品信息详情</a-divider>
    <div class="sku-table">
      <JVxeTable
        keep-source
        resizable
        ref="wmsItemSku"
        :loading="wmsItemSkuTable.loading"
        :columns="skuColumns"
        :dataSource="wmsItemSkuTable.dataSource"
        :height="340"
        :disabled="formDisabled"
        :rowNumber="true"
        :rowSelection="true"
        :toolbar="true"
      />
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed, unref, reactive } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { useJvxeMethod } from '/@/hooks/system/useJvxeMethods';
  import { expandItemSpec, formSchema, measureUnitState, packItemSpec, syncMeasureUnits, withMeasureUnit, wmsItemSkuJVxeColumns } from '../WmsItemSpu.data';
  import { saveOrUpdate, queryWmsItemSkuByMainId } from '../WmsItemSpu.api';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(true);
  const formDisabled = ref(false);
  const refKeys = ref(['wmsItemSku']);
  const activeKey = ref('wmsItemSku');
  const wmsItemSku = ref();
  const tableRefs = { wmsItemSku };
  const wmsItemSkuTable = reactive({
    loading: false,
    dataSource: [],
  });
  const skuColumns = computed(() => withMeasureUnit(wmsItemSkuJVxeColumns, measureUnitState));
  const [registerForm, { setProps, resetFields, setFieldsValue }] = useForm({
    schemas: formSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 12 },
    labelWidth: 110,
  });
  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await reset();
    setModalProps({ confirmLoading: false, showCancelBtn: data?.showFooter, showOkBtn: data?.showFooter });
    isUpdate.value = !!data?.isUpdate;
    formDisabled.value = !data?.showFooter;
    if (unref(isUpdate)) {
      const record = { ...data.record };
      record.categoryId1 = record.categoryId3 ?? record.categoryId2 ?? record.categoryId1;
      syncMeasureUnits(record);
      await setFieldsValue(record);
      requestSubTableData(queryWmsItemSkuByMainId, { id: data?.record?.id }, wmsItemSkuTable, () => {
        wmsItemSkuTable.dataSource = (wmsItemSkuTable.dataSource || []).map((row) => expandItemSpec(row));
      });
    }
    setProps({ disabled: !data?.showFooter });
  });
  const [, handleSubmit, requestSubTableData, formRef] = useJvxeMethod(
    requestAddOrEdit,
    classifyIntoFormData,
    tableRefs,
    activeKey,
    refKeys,
  );

  const title = computed(() => (!unref(isUpdate) ? '新增' : !unref(formDisabled) ? '编辑' : '详情'));

  async function reset() {
    await resetFields();
    syncMeasureUnits();
    activeKey.value = 'wmsItemSku';
    wmsItemSkuTable.dataSource = [];
  }

  function toCategoryId(value) {
    if (value === undefined || value === null || value === '') {
      return null;
    }
    const id = Number(value);
    return Number.isNaN(id) ? null : id;
  }

  function classifyIntoFormData(allValues) {
    let main = Object.assign({}, allValues.formValue);
    const categoryId = toCategoryId(main.categoryId1);
    return {
      ...main,
      categoryId1: categoryId,
      categoryId2: null,
      categoryId3: null,
      wmsItemSkuList: allValues.tablesValue[0].tableData.map((row) => packItemSpec(row)),
    };
  }

  async function requestAddOrEdit(values) {
    try {
      setModalProps({ confirmLoading: true });
      await saveOrUpdate(values, isUpdate.value);
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<style lang="less" scoped>
  :deep(.ant-input-number) {
    width: 100%;
  }
  :deep(.ant-calendar-picker) {
    width: 100%;
  }

  .sku-divider {
    margin: 4px 0 16px;
    font-weight: 500;
  }

  .sku-table {
    width: 100%;

    :deep(.vxe-grid),
    :deep(.vxe-table) {
      width: 100%;
    }
  }
</style>
