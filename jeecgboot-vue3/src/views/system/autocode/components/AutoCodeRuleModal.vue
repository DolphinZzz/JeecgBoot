<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="title"
    :width="700"
    wrapClassName="auto-code-rule-modal"
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
  import { formSchema } from '../autoCode.data';
  import { saveAutoCodeRule, updateAutoCodeRule } from '../autoCode.api';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(false);

  const title = computed(() => (!unref(isUpdate) ? '添加编码规则' : '编辑编码规则'));

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: formSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
    labelWidth: 100,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;

    if (unref(isUpdate)) {
      await setFieldsValue({
        ...data.record,
      });
    } else {
      await setFieldsValue({
        isPadded: 'Y',
        paddedMethod: 'L',
        enableFlag: 1,
      });
    }
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });
      // 如果未启用补齐，清空补齐字符与补齐方式
      if (values.isPadded === 'N') {
        values.paddedChar = '';
        values.paddedMethod = '';
      }
      if (unref(isUpdate)) {
        await updateAutoCodeRule(values);
      } else {
        await saveAutoCodeRule(values);
      }
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<style lang="less">
  .auto-code-rule-modal {
    .ant-modal-body {
      padding: 24px;
    }
  }
</style>
