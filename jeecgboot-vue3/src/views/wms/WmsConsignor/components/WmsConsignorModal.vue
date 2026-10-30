<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose :title="title" :width="800" @ok="handleSubmit">
    <BasicForm @register="registerForm" name="WmsConsignorForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { formSchema, isMunicipalityProvince } from '../WmsConsignor.data';
  import { saveOrUpdate } from '../WmsConsignor.api';

  const { createMessage } = useMessage();

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(true);
  const isDetail = ref(false);

  const [registerForm, { setProps, resetFields, setFieldsValue, validate, scrollToField }] = useForm({
    labelWidth: 140,
    schemas: formSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 12 },
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false, showCancelBtn: !!data?.showFooter, showOkBtn: !!data?.showFooter });
    isUpdate.value = !!data?.isUpdate;
    isDetail.value = !!data?.showFooter;
    if (unref(isUpdate)) {
      const record = { ...data.record };
      // 直辖市不走城市，回填时清掉，避免把上一次选的旧城市带进来
      if (isMunicipalityProvince(record.provinceId)) {
        record.cityCode = undefined;
      }
      await setFieldsValue(record);
    }
    setProps({ disabled: !data?.showFooter });
  });

  const title = computed(() => (!unref(isUpdate) ? '新增' : !unref(isDetail) ? '详情' : '编辑'));

  async function handleSubmit() {
    try {
      const values = await validate();
      if (isMunicipalityProvince(values.provinceId)) {
        values.cityCode = null;
      }
      setModalProps({ confirmLoading: true });
      await saveOrUpdate(values, isUpdate.value);
      createMessage.success(unref(isUpdate) ? '编辑成功!' : '添加成功！');
      closeModal();
      emit('success');
    } catch ({ errorFields }) {
      if (errorFields) {
        const firstField = errorFields[0];
        if (firstField) {
          scrollToField(firstField.name, { behavior: 'smooth', block: 'center' });
        }
      }
      return Promise.reject(errorFields);
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
</style>
