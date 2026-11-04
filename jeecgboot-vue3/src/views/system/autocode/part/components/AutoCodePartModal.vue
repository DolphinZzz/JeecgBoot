<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="title"
    :width="800"
    :minHeight="420"
    wrapClassName="auto-code-part-modal"
    destroyOnClose
    @ok="handleSubmit"
  >
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formSchema } from '../autoCodePart.data';
  import { saveAutoCodePart, updateAutoCodePart } from '../autoCodePart.api';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(false);
  const currentRuleId = ref<string | number>('');

  const title = computed(() => (!unref(isUpdate) ? '添加规则组成' : '编辑规则组成'));

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: formSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 12 },
    labelWidth: 100,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;
    currentRuleId.value = data?.ruleId || '';

    if (unref(isUpdate)) {
      await setFieldsValue({
        ...data.record,
      });
    } else {
      await setFieldsValue({
        ruleId: unref(currentRuleId),
        partIndex: 0,
        partLength: 0,
        cycleFlag: 'N',
      });
    }
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });
      values.ruleId = values.ruleId || unref(currentRuleId);

      // 根据分段类型进行字段清洗
      const pType = values.partType;
      if (pType !== 'INPUTCHAR') {
        values.inputCharacter = null;
      }
      if (pType !== 'NOWDATE') {
        values.dateFormat = null;
      }
      if (pType !== 'FIXCHAR') {
        values.fixCharacter = null;
      }
      if (pType !== 'SERIALNO') {
        values.serialStartNo = null;
        values.serialStep = null;
        values.cycleFlag = 'N';
        values.cycleMethod = null;
      } else {
        if (values.cycleFlag !== 'Y') {
          values.cycleMethod = null;
        }
      }

      if (unref(isUpdate)) {
        await updateAutoCodePart(values);
      } else {
        await saveAutoCodePart(values);
      }
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<style lang="less">
  .auto-code-part-modal {
    .ant-modal-body {
      padding: 24px 32px;
      min-height: 380px;
    }
    .ant-select {
      width: 100% !important;
    }
    .ant-input-number {
      width: 100% !important;
    }
  }
</style>
