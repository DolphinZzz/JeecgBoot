<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose :maxHeight="500" :width="800" :title="title" okText="提交" @ok="handleSubmit">
    <BasicForm @register="registerForm" name="WmsItemCategoryForm" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { formSchema } from '../WmsItemCategory.data';
  import { saveOrUpdate, loadTreeData } from '../WmsItemCategory.api';

  const emit = defineEmits(['register', 'success']);
  const isUpdate = ref(true);
  const isDetail = ref(false);
  const expandedRowKeys = ref<any[]>([]);
  const treeData = ref<any[]>([]);
  let model: Nullable<Recordable> = null;

  const [registerForm, { setProps, resetFields, setFieldsValue, validate, updateSchema, scrollToField }] = useForm({
    schemas: formSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
    labelCol: { xs: { span: 24 }, sm: { span: 4 } },
    wrapperCol: { xs: { span: 24 }, sm: { span: 18 } },
    baseRowStyle: { padding: '0 20px' },
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    expandedRowKeys.value = [];
    setModalProps({ confirmLoading: false, minHeight: 80, showOkBtn: !data?.hideFooter });
    isUpdate.value = !!data?.isUpdate;
    isDetail.value = !data?.hideFooter;
    if (data?.record) {
      model = { ...data.record };
      const record = { ...data.record };
      const parentId = normalizeParentId(record.parentId);
      record.parentId = parentId == null ? undefined : String(parentId);
      await setFieldsValue(record);
      updateSchema([{ field: 'parentId', componentProps: { hiddenNodeKey: data.record.id ? String(data.record.id) : '' } }]);
    } else {
      model = null;
      updateSchema([{ field: 'parentId', componentProps: { hiddenNodeKey: '' } }]);
    }
    treeData.value = await loadTreeData({ async: false, pcode: '' });
    setProps({ disabled: !!data?.hideFooter });
  });

  const title = computed(() => (!unref(isUpdate) ? '新增' : !unref(isDetail) ? '详情' : '编辑'));

  function normalizeParentId(value) {
    if (value === undefined || value === null || value === '' || value === 0 || value === '0') {
      return null;
    }
    const parsed = Number(value);
    return Number.isInteger(parsed) ? parsed : null;
  }

  function getExpandKeysByPid(pid, arr) {
    if (pid && arr && arr.length > 0) {
      for (let i = 0; i < arr.length; i++) {
        if (String(arr[i].key) === String(pid) && unref(expandedRowKeys).map(String).indexOf(String(pid)) < 0) {
          expandedRowKeys.value.push(arr[i].key);
          getExpandKeysByPid(arr[i]['parentId'], unref(treeData));
        } else {
          getExpandKeysByPid(pid, arr[i].children);
        }
      }
    }
  }

  async function handleSubmit() {
    try {
      const values = await validate();
      values.parentId = normalizeParentId(values.parentId);
      if (values.sort === '' || values.sort === undefined) {
        values.sort = null;
      }
      setModalProps({ confirmLoading: true });
      const saved = await saveOrUpdate(values, isUpdate.value);
      if (saved && saved.id) {
        values.id = saved.id;
      }
      closeModal();
      await getExpandKeysByPid(values['parentId'], unref(treeData));
      emit('success', {
        isUpdate: unref(isUpdate),
        values: { ...values },
        expandedArr: unref(expandedRowKeys).reverse(),
        changeParent: model != null && normalizeParentId(model['parentId']) !== normalizeParentId(values['parentId']),
      });
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
