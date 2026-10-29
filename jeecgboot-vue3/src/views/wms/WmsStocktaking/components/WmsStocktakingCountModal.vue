<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose title="录入实盘数" :width="640" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { countFormSchema } from '../WmsStocktaking.data';
  import { submitCount } from '../WmsStocktaking.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: countFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 12 },
    labelWidth: 110,
  });

  let currentId: number | undefined = undefined;

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
    const record = data?.record || {};
    currentId = record.id;
    await setFieldsValue({
      skuIdText: record.skuId_dictText || record.skuId,
      locaText: record.targetWarehouseAreaLocaId_dictText || record.targetWarehouseAreaLocaId,
      bookCount: record.bookCount,
      correctCount: record.correctCount,
    });
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });
      await submitCount({ id: currentId, correctCount: values.correctCount });
      createMessage.success('实盘数已录入');
      closeModal();
      emit('success');
    } catch (e: any) {
      if (e?.errorFields) return;
      createMessage.error(e?.message || '录入失败');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
