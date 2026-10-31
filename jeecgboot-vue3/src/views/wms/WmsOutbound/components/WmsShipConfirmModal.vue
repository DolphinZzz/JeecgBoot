<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    title="物流发货出库确认"
    :width="600"
    okText="确认出库发货"
    @ok="handleSubmit"
  >
    <a-alert
      type="info"
      show-icon
      message="发运出库将真实扣减仓库物理库存与锁定库存，核销相关SN编码为已出库，操作后不可直接逆向取消！"
      style="margin-bottom: 16px"
    />

    <a-descriptions bordered size="small" :column="2" style="margin-bottom: 16px">
      <a-descriptions-item label="出库单号">{{ orderInfo?.outboundCode }}</a-descriptions-item>
      <a-descriptions-item label="仓库">{{ orderInfo?.warehouseName }}</a-descriptions-item>
      <a-descriptions-item label="客户">{{ orderInfo?.customerName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="出库件数">{{ orderInfo?.pickedTotal ?? orderInfo?.pickedCount ?? orderInfo?.totalCount ?? 0 }}</a-descriptions-item>
    </a-descriptions>

    <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="物流承运商" required>
        <a-select
          v-model:value="formData.logisticsCompanyId"
          placeholder="请选择承运商"
          :options="logisticsOptions"
        />
      </a-form-item>

      <a-form-item label="物流运单号" required>
        <a-input
          v-model:value="formData.logisticsNumber"
          placeholder="请输入快递单号/运单号"
          maxlength="64"
        />
      </a-form-item>

      <a-form-item label="运费 (元)">
        <a-input-number
          v-model:value="formData.freightFee"
          :min="0"
          :precision="2"
          style="width: 100%"
          placeholder="选填"
        />
      </a-form-item>
    </a-form>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { confirmShip } from '../WmsOutbound.api';
  import { queryAllLogistics } from '../../WmsLogistics/WmsLogistics.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);
  const orderInfo = ref<any>(null);
  const logisticsOptions = ref<{ label: string; value: number }[]>([]);

  const formData = reactive({
    logisticsCompanyId: undefined as number | undefined,
    logisticsNumber: '',
    freightFee: 0,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    orderInfo.value = data?.record || {};
    formData.logisticsCompanyId = orderInfo.value.logisticsCompanyId || undefined;
    formData.logisticsNumber = orderInfo.value.logisticsNumber || '';
    formData.freightFee = 0;
    setModalProps({ confirmLoading: false });
    await loadLogisticsOptions();
  });

  async function loadLogisticsOptions() {
    try {
      const res = await queryAllLogistics();
      if (Array.isArray(res)) {
        logisticsOptions.value = res.map((item: any) => ({
          label: item.companyName,
          value: item.id,
        }));
      }
    } catch (e) {
      logisticsOptions.value = [];
    }
  }

  async function handleSubmit() {
    if (!formData.logisticsCompanyId) {
      createMessage.warning('请选择物流承运商！');
      return;
    }
    if (!formData.logisticsNumber.trim()) {
      createMessage.warning('请输入物流运单号！');
      return;
    }

    setModalProps({ confirmLoading: true });
    try {
      await confirmShip({
        outboundOrderId: orderInfo.value.id,
        logisticsCompanyId: formData.logisticsCompanyId,
        logisticsNumber: formData.logisticsNumber.trim(),
        freightFee: formData.freightFee || 0,
      });
      createMessage.success('发货成功！库存扣减与SN核销已完成');
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
