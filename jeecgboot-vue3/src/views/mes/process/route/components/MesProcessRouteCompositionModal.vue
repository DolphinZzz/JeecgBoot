<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    :title="modalTitle"
    :width="820"
    :showOkBtn="false"
    :showCancelBtn="false"
  >
    <div class="composition-form-wrap">
      <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ style: { width: '130px' } }">
        <a-row :gutter="24" align="middle">
          <!-- 序号（步进器） -->
          <a-col :span="12">
            <a-form-item label="序号" name="sequenceNo">
              <div class="stepper-box">
                <a-button class="stepper-btn" @click="changeStep('sequenceNo', -1, 1)">-</a-button>
                <a-input-number
                  v-model:value="formState.sequenceNo"
                  :min="1"
                  :step="1"
                  :controls="false"
                  class="stepper-input"
                />
                <a-button class="stepper-btn" @click="changeStep('sequenceNo', 1, 1)">+</a-button>
              </div>
            </a-form-item>
          </a-col>

          <!-- 工序（数据来自工序设置） -->
          <a-col :span="12">
            <a-form-item label="工序" name="processStepId">
              <a-select
                v-model:value="formState.processStepId"
                placeholder="请选择工序"
                show-search
                option-filter-prop="label"
                :options="processStepOptions"
                allowClear
              />
            </a-form-item>
          </a-col>

          <!-- 与下一道工序关系 -->
          <a-col :span="12">
            <a-form-item label="与下一道工序关系" name="relationWithNext">
              <a-select
                v-model:value="formState.relationWithNext"
                placeholder="请选择与下一道工序关系"
                :options="nextRelationOptions"
                allowClear
              />
            </a-form-item>
          </a-col>

          <!-- 甘特图显示颜色 -->
          <a-col :span="12">
            <a-form-item label="甘特图显示颜色" name="ganttColor">
              <div class="color-picker-row">
                <label class="color-Trigger">
                  <input type="color" v-model="formState.ganttColor" class="native-color-input" />
                  <span class="color-preview" :style="{ backgroundColor: formState.ganttColor || '#00AEF3' }">
                    <DownOutlined class="color-arrow" />
                  </span>
                </label>
                <a-input v-model:value="formState.ganttColor" placeholder="#00AEF3" class="color-hex-input" />
              </div>
            </a-form-item>
          </a-col>

          <!-- 是否关键工序（整个工艺只允许有一个关键工序） -->
          <a-col :span="12">
            <a-form-item label="是否关键工序" name="isCriticalProcess">
              <a-select v-model:value="formState.isCriticalProcess" placeholder="请选择">
                <a-select-option :value="1">是</a-select-option>
                <a-select-option :value="0">否</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <!-- 是否需要质检确认 -->
          <a-col :span="12">
            <a-form-item label="是否需要质检确认" name="needQcConfirmation">
              <a-select v-model:value="formState.needQcConfirmation" placeholder="请选择">
                <a-select-option :value="1">是</a-select-option>
                <a-select-option :value="0">否</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <!-- 准备时间（时间单位为一小时） -->
          <a-col :span="12">
            <a-form-item label="准备时间" name="setupTime">
              <div class="stepper-box">
                <a-button class="stepper-btn" @click="changeStep('setupTime', -1, 0)">-</a-button>
                <a-input-number
                  v-model:value="formState.setupTime"
                  :min="0"
                  :step="1"
                  :controls="false"
                  class="stepper-input"
                />
                <a-button class="stepper-btn" @click="changeStep('setupTime', 1, 0)">+</a-button>
              </div>
            </a-form-item>
          </a-col>

          <!-- 等待时间（时间单位为一小时） -->
          <a-col :span="12">
            <a-form-item label="等待时间" name="waitTime">
              <div class="stepper-box">
                <a-button class="stepper-btn" @click="changeStep('waitTime', -1, 0)">-</a-button>
                <a-input-number
                  v-model:value="formState.waitTime"
                  :min="0"
                  :step="1"
                  :controls="false"
                  class="stepper-input"
                />
                <a-button class="stepper-btn" @click="changeStep('waitTime', 1, 0)">+</a-button>
              </div>
            </a-form-item>
          </a-col>

          <!-- 备注 -->
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="formState.remark" :rows="3" placeholder="请输入内容" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <!-- 底部按钮 -->
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
  import { DownOutlined } from '@ant-design/icons-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { nextRelationOptions } from '../MesProcessRoute.data';
  import { saveOrUpdateRouteComposition } from '../MesProcessRoute.api';
  import { listEnabledProcessSteps } from '../../step/MesProcessStep.api';

  const emit = defineEmits(['register', 'success']);
  const { createMessage } = useMessage();

  const formRef = ref();
  const isUpdate = ref(false);
  const submitting = ref(false);
  const processStepOptions = ref<{ label: string; value: number }[]>([]);
  const existingCompositions = ref<any[]>([]);

  const formState = reactive<any>({
    id: undefined,
    routeId: undefined,
    sequenceNo: 1,
    processStepId: undefined,
    relationWithNext: undefined,
    ganttColor: '#00AEF3',
    isCriticalProcess: undefined,
    needQcConfirmation: 0,
    setupTime: 0,
    waitTime: 0,
    remark: '',
  });

  const formRules = {
    processStepId: [{ required: true, message: '请选择工序', trigger: 'change' }],
    isCriticalProcess: [{ required: true, message: '请选择是否关键工序', trigger: 'change' }],
    needQcConfirmation: [{ required: true, message: '请选择是否需要质检确认', trigger: 'change' }],
  };

  const modalTitle = computed(() => (isUpdate.value ? '修改工艺组成' : '添加工艺组成'));

  function changeStep(field: 'sequenceNo' | 'setupTime' | 'waitTime', delta: number, min: number) {
    const cur = Number(formState[field]) || 0;
    const next = cur + delta;
    formState[field] = next < min ? min : next;
  }

  async function loadProcessOptions() {
    try {
      const res: any = await listEnabledProcessSteps();
      const list = Array.isArray(res) ? res : res?.result || [];
      processStepOptions.value = list.map((item: any) => ({
        label: item.processName,
        value: item.id,
      }));
    } catch (e) {
      processStepOptions.value = [];
    }
  }

  const [registerModal, { closeModal }] = useModalInner(async (data) => {
    isUpdate.value = !!data?.isUpdate;
    formState.routeId = data?.routeId;
    existingCompositions.value = data?.existingCompositions || [];

    await loadProcessOptions();

    if (isUpdate.value && data?.record) {
      const r = data.record;
      formState.id = r.id;
      formState.sequenceNo = r.sequenceNo ?? 1;
      formState.processStepId = r.processStepId;
      formState.relationWithNext = r.relationWithNext || undefined;
      formState.ganttColor = r.ganttColor || '#00AEF3';
      formState.isCriticalProcess = r.isCriticalProcess ?? 0;
      formState.needQcConfirmation = r.needQcConfirmation ?? 0;
      formState.setupTime = r.setupTime ?? 0;
      formState.waitTime = r.waitTime ?? 0;
      formState.remark = r.remark || '';
    } else {
      formState.id = undefined;
      formState.sequenceNo = data?.nextSequenceNo || 1;
      formState.processStepId = undefined;
      formState.relationWithNext = undefined;
      formState.ganttColor = '#00AEF3';
      formState.isCriticalProcess = undefined;
      formState.needQcConfirmation = 0;
      formState.setupTime = 0;
      formState.waitTime = 0;
      formState.remark = '';
    }
    formRef.value?.clearValidate();
  });

  async function handleOk() {
    try {
      await formRef.value.validate();

      // 前端前置校验：整个工艺只允许有一个关键工序
      if (formState.isCriticalProcess === 1) {
        const conflictKey = existingCompositions.value.find(
          (item) => item.isCriticalProcess === 1 && item.id !== formState.id
        );
        if (conflictKey) {
          createMessage.warning('整个工艺只允许有一个关键工序！');
          return;
        }
      }

      // 校验同一工艺下工序是否重复
      const conflictStep = existingCompositions.value.find(
        (item) => item.processStepId === formState.processStepId && item.id !== formState.id
      );
      if (conflictStep) {
        createMessage.warning('当前工艺路线中已存在该工序，请勿重复添加！');
        return;
      }

      // 校验同一工艺下序号是否重复
      const conflictSeq = existingCompositions.value.find(
        (item) => Number(item.sequenceNo) === Number(formState.sequenceNo) && item.id !== formState.id
      );
      if (conflictSeq) {
        createMessage.warning('当前工艺路线中已存在相同的序号，请调整序号！');
        return;
      }

      submitting.value = true;
      await saveOrUpdateRouteComposition(
        {
          id: formState.id,
          routeId: formState.routeId,
          sequenceNo: formState.sequenceNo,
          processStepId: formState.processStepId,
          relationWithNext: formState.relationWithNext,
          ganttColor: formState.ganttColor || '#00AEF3',
          isCriticalProcess: formState.isCriticalProcess,
          needQcConfirmation: formState.needQcConfirmation,
          setupTime: formState.setupTime ?? 0,
          waitTime: formState.waitTime ?? 0,
          remark: formState.remark,
        },
        isUpdate.value
      );
      createMessage.success(isUpdate.value ? '修改工艺组成成功' : '添加工艺组成成功');
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
  .composition-form-wrap {
    padding: 18px 24px 8px;
  }

  .stepper-box {
    display: inline-flex;
    align-items: center;
    width: 180px;

    .stepper-btn {
      width: 36px;
      height: 32px;
      padding: 0;
      background: #f5f7fa;
      color: #606266;
      border: 1px solid #dcdfe6;
      font-size: 16px;
      line-height: 1;

      &:first-child {
        border-top-right-radius: 0;
        border-bottom-right-radius: 0;
        border-right: none;
      }

      &:last-child {
        border-top-left-radius: 0;
        border-bottom-left-radius: 0;
        border-left: none;
      }
    }

    .stepper-input {
      flex: 1;
      border-radius: 0;

      :deep(.ant-input-number-input) {
        text-align: center;
      }
    }
  }

  .color-picker-row {
    display: flex;
    align-items: center;
    gap: 12px;

    .color-Trigger {
      position: relative;
      display: inline-flex;
      padding: 3px;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      cursor: pointer;

      .native-color-input {
        position: absolute;
        inset: 0;
        opacity: 0;
        width: 100%;
        height: 100%;
        cursor: pointer;
      }

      .color-preview {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 26px;
        height: 24px;
        border-radius: 2px;
        color: #fff;
        font-size: 10px;
      }
    }

    .color-hex-input {
      flex: 1;
    }
  }

  .modal-footer-btns {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    padding-top: 12px;
  }
</style>
