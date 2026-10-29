<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose title="新建移库单" :width="1200" @ok="handleSubmit">
    <BasicForm @register="registerForm" ref="formRef" />
    <a-divider orientation="center" class="detail-divider">移库明细（一行为一次「移出库位 → 移入库位」）</a-divider>
    <JVxeTable
      keep-source
      resizable
      ref="detailTableRef"
      :columns="detailColumns"
      :dataSource="tableData.dataSource"
      :height="320"
      :rowNumber="true"
      :toolbar="true"
    />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, reactive } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { formSchema, detailColumns } from '../WmsMove.data';
  import { createMove } from '../WmsMove.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);

  const detailTableRef = ref();
  const formRef = ref();
  const tableData = reactive({ dataSource: [] as any[] });

  const [registerForm, { resetFields, validate, setProps }] = useForm({
    schemas: formSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 12 },
    labelWidth: 110,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async () => {
    await resetFields();
    tableData.dataSource = [];
    setProps({ disabled: false });
    setModalProps({ confirmLoading: false });
  });

  async function handleSubmit() {
    try {
      const formValues = await validate();
      const rows: any[] = detailTableRef.value?.getTableData() ?? [];
      const details = rows
        .filter((row) => row.skuId && row.sourceWarehouseAreaLocaId && row.targetWarehouseAreaLocaId && row.count > 0)
        .map((row) => ({
          skuId: toLong(row.skuId),
          batchNumber: row.batchNumber || '',
          consignorId: toLong(row.consignorId),
          qualityStatus: row.qualityStatus ? Number(row.qualityStatus) : 1,
          sourceWarehouseAreaLocaId: toLong(row.sourceWarehouseAreaLocaId),
          targetWarehouseAreaLocaId: toLong(row.targetWarehouseAreaLocaId),
          count: Number(row.count),
        }));
      if (details.length === 0) {
        createMessage.warning('请至少填写一行移库明细，且移出/移入库位与数量不能为空');
        return;
      }
      setModalProps({ confirmLoading: true });
      const res: any = await createMove({
        operationType: 1,
        warehouseId: formValues.warehouseId,
        remark: formValues.remark,
        details,
      });
      createMessage.success(res?.message || '移库单已生成');
      closeModal();
      emit('success');
    } catch (e: any) {
      // 表单校验失败会抛 errorFields，不重复提示
      if (e?.errorFields) return;
      createMessage.error(e?.message || '新建移库单失败');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }

  function toLong(value) {
    if (value === undefined || value === null || value === '') return null;
    const num = Number(value);
    return Number.isNaN(num) ? null : num;
  }
</script>

<style lang="less" scoped>
  .detail-divider {
    margin: 4px 0 16px;
    font-weight: 500;
  }
</style>
