<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    title="取消出库单"
    :width="550"
    okText="确认取消"
    okType="danger"
    @ok="handleSubmit"
  >
    <a-alert
      v-if="isPicked"
      type="warning"
      show-icon
      message="逆向返架提示"
      description="该出库单物料已拣货！取消后系统将自动生成【取消返架作业单】（作业类型=4），并将物料的锁定库存释放、SN序列号恢复为在库良品状态。"
      style="margin-bottom: 16px"
    />
    <a-alert
      v-else
      type="info"
      show-icon
      message="释放锁库提示"
      description="取消后将全额释放该出库单预先锁定的库存可用量。"
      style="margin-bottom: 16px"
    />

    <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
      <a-form-item label="出库单号">
        <b>{{ orderInfo?.outboundCode }}</b>
      </a-form-item>
      <a-form-item label="取消原因" required>
        <a-textarea
          v-model:value="reason"
          :rows="3"
          maxlength="200"
          placeholder="请输入取消原因（如：客户临时取消、工单计划调整等）"
        />
      </a-form-item>
    </a-form>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { cancelOutboundOrder } from '../WmsOutbound.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);
  const orderInfo = ref<any>(null);
  const reason = ref('');

  const [registerModal, { setModalProps, closeModal }] = useModalInner((data) => {
    orderInfo.value = data?.record || {};
    reason.value = '';
    setModalProps({ confirmLoading: false });
  });

  const isPicked = computed(() => {
    const status = orderInfo.value?.status;
    return status >= 40 && status < 80;
  });

  async function handleSubmit() {
    if (!reason.value.trim()) {
      createMessage.warning('请输入取消原因！');
      return;
    }

    setModalProps({ confirmLoading: true });
    try {
      await cancelOutboundOrder(orderInfo.value.id, reason.value.trim());
      createMessage.success('出库单已成功取消！');
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
