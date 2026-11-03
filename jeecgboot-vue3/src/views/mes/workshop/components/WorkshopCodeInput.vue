<template>
  <div class="workshop-code-input-wrapper">
    <a-input
      :value="innerValue"
      :placeholder="isAuto ? '正在自动生成编码...' : (placeholder || '请输入车间编码')"
      :disabled="disabled || isAuto"
      allowClear
      @change="handleInputChange"
    >
      <template #prefix v-if="loading">
        <LoadingOutlined />
      </template>
    </a-input>
    <div class="switch-box" v-if="!isUpdate">
      <a-switch
        v-model:checked="isAuto"
        :loading="loading"
        :disabled="disabled"
        checked-children="开"
        un-checked-children="关"
        @change="handleSwitchChange"
      />
      <span class="switch-label">自动生成编码</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import { LoadingOutlined } from '@ant-design/icons-vue';
  import { getNextWorkshopCode } from '../MesWorkshop.api';
  import { useMessage } from '/@/hooks/web/useMessage';

  const props = defineProps({
    value: {
      type: String,
      default: '',
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    isUpdate: {
      type: Boolean,
      default: false,
    },
    placeholder: {
      type: String,
      default: '请输入车间编码',
    },
  });

  const emit = defineEmits(['update:value', 'change']);
  const { createMessage } = useMessage();

  const innerValue = ref<string>(props.value || '');
  const isAuto = ref<boolean>(false);
  const loading = ref<boolean>(false);
  const manualDraft = ref<string>('');

  watch(
    () => props.value,
    (val) => {
      innerValue.value = val || '';
      if (!isAuto.value && val) {
        manualDraft.value = val;
      }
    },
    { immediate: true }
  );

  watch(
    () => props.isUpdate,
    (val) => {
      if (val) {
        isAuto.value = false;
      }
    }
  );

  function handleInputChange(e: any) {
    const val = e.target.value;
    innerValue.value = val;
    manualDraft.value = val;
    emit('update:value', val);
    emit('change', val);
  }

  async function handleSwitchChange(checked: boolean) {
    if (checked) {
      manualDraft.value = innerValue.value;
      loading.value = true;
      try {
        const nextCode = await getNextWorkshopCode();
        if (nextCode) {
          innerValue.value = nextCode;
          emit('update:value', nextCode);
          emit('change', nextCode);
        }
      } catch (err) {
        createMessage.error('获取自动车间编码失败，请手动输入');
        isAuto.value = false;
      } finally {
        loading.value = false;
      }
    } else {
      const restored = manualDraft.value || '';
      innerValue.value = restored;
      emit('update:value', restored);
      emit('change', restored);
    }
  }
</script>

<style lang="less" scoped>
  .workshop-code-input-wrapper {
    display: flex;
    align-items: center;
    width: 100%;
    gap: 12px;

    :deep(.ant-input-affix-wrapper),
    :deep(.ant-input) {
      flex: 1;
    }

    .switch-box {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-shrink: 0;
      user-select: none;

      .switch-label {
        font-size: 13px;
        color: #555;
      }
    }
  }
</style>
