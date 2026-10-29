<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose title="生成盘点任务" :width="700" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
    <a-alert type="info" show-icon style="margin-top: 8px">
      <template #message>
        盘点范围取「该仓库下物理库存大于 0」的库位与 SKU，生成时会把账面数快照进盘点记录；生成后相关库位将被锁定，避免盘点期间被移入移出。
      </template>
    </a-alert>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { createFormSchema } from '../WmsStocktaking.data';
  import { createStocktaking } from '../WmsStocktaking.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);

  const [registerForm, { resetFields, validate }] = useForm({
    schemas: createFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 12 },
    labelWidth: 110,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async () => {
    await resetFields();
    setModalProps({ confirmLoading: false });
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });
      const res: any = await createStocktaking({
        warehouseId: values.warehouseId,
        warehouseAreaId: values.warehouseAreaId ?? null,
        category: values.category ?? 1,
      });
      createMessage.success(res?.message || '盘点任务已生成');
      closeModal();
      emit('success');
    } catch (e: any) {
      if (e?.errorFields) return;
      createMessage.error(e?.message || '生成盘点任务失败');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
