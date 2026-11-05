<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    :title="modalTitle"
    :width="650"
    :showOkBtn="false"
    :showCancelBtn="false"
  >
    <div class="step-op-form-wrap">
      <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ style: { width: '95px' } }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="顺序编号" name="stepOrder">
              <a-input-number v-model:value="formState.stepOrder" :min="1" :step="1" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="辅助设备" name="auxiliaryEquipment">
              <a-input v-model:value="formState.auxiliaryEquipment" placeholder="请输入辅助设备" allowClear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="辅助材料" name="auxiliaryMaterials">
              <a-input v-model:value="formState.auxiliaryMaterials" placeholder="请输入辅助材料" allowClear />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="步骤说明" name="stepDescription">
              <a-textarea v-model:value="formState.stepDescription" :rows="3" placeholder="请输入步骤说明" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="formState.remark" :rows="2" placeholder="请输入内容" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <div class="modal-footer-btns">
        <a-button type="primary" :loading="submitting" @click="handleOk">确 定</a-button>
        <a-button @click="closeModal">取 消</a-button>
      </div>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, reactive, computed } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { saveOrUpdateStepOperation } from '../MesProcessStep.api';
  import { useMessage } from '/@/hooks/web/useMessage';

  const emit = defineEmits(['register', 'success']);
  const { createMessage } = useMessage();

  const formRef = ref();
  const isUpdate = ref(false);
  const submitting = ref(false);

  const formState = reactive<any>({
    id: undefined,
    processStepId: undefined,
    stepOrder: 1,
    stepDescription: '',
    auxiliaryEquipment: '',
    auxiliaryMaterials: '',
    remark: '',
  });

  const formRules = {
    stepOrder: [{ required: true, message: '请输入顺序编号', trigger: 'change' }],
    stepDescription: [{ required: true, message: '请输入步骤说明', trigger: 'blur' }],
  };

  const modalTitle = computed(() => (isUpdate.value ? '修改操作步骤' : '添加操作步骤'));

  const [registerModal, { closeModal }] = useModalInner(async (data) => {
    isUpdate.value = !!data?.isUpdate;
    formState.processStepId = data?.processStepId;
    if (isUpdate.value && data?.record) {
      formState.id = data.record.id;
      formState.stepOrder = data.record.stepOrder ?? 1;
      formState.stepDescription = data.record.stepDescription || '';
      formState.auxiliaryEquipment = data.record.auxiliaryEquipment || '';
      formState.auxiliaryMaterials = data.record.auxiliaryMaterials || '';
      formState.remark = data.record.remark || '';
    } else {
      formState.id = undefined;
      formState.stepOrder = data?.nextStepOrder || 1;
      formState.stepDescription = '';
      formState.auxiliaryEquipment = '';
      formState.auxiliaryMaterials = '';
      formState.remark = '';
    }
    formRef.value?.clearValidate();
  });

  async function handleOk() {
    try {
      await formRef.value.validate();
      submitting.value = true;
      await saveOrUpdateStepOperation(
        {
          id: formState.id,
          processStepId: formState.processStepId,
          stepOrder: formState.stepOrder,
          stepDescription: formState.stepDescription,
          auxiliaryEquipment: formState.auxiliaryEquipment,
          auxiliaryMaterials: formState.auxiliaryMaterials,
          remark: formState.remark,
        },
        isUpdate.value
      );
      createMessage.success(isUpdate.value ? '修改操作步骤成功' : '添加操作步骤成功');
      closeModal();
      emit('success');
    } catch (e) {
      // ignore validation error
    } finally {
      submitting.value = false;
    }
  }
</script>

<style lang="less" scoped>
  .step-op-form-wrap {
    padding: 16px 24px 8px;
  }

  .modal-footer-btns {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    padding-top: 12px;
  }
</style>
