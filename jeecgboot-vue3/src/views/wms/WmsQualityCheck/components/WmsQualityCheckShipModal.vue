<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    title="确认退还供应商出库"
    :width="640"
    @ok="handleSubmit"
  >
    <a-alert
      type="warning"
      show-icon
      message="确认出库提醒"
      description="供应商提货或物流快递取件发货后执行此操作。系统将自动从退货货架区核减不良品库存，并将关联的商品序列码（SN）标记为已退货。"
      style="margin-bottom: 12px"
    />

    <a-descriptions :column="2" size="small" bordered style="margin-bottom: 12px">
      <a-descriptions-item label="退货单号">
        <span style="color: #1890ff; font-weight: bold; font-family: monospace">{{ record.returnOrderNo || '-' }}</span>
      </a-descriptions-item>
      <a-descriptions-item label="质检单号">{{ record.qcCode || '-' }}</a-descriptions-item>
      <a-descriptions-item label="商品信息" :span="2">
        {{ `${record.skuCode || record.skuId_dictText || '-'} ${record.spuTitle || ''}`.trim() }}
      </a-descriptions-item>
      <a-descriptions-item label="退货数量">
        <span style="color: #f5222d; font-weight: bold; font-size: 15px">{{ record.failCount ?? 0 }}</span> 件
      </a-descriptions-item>
      <a-descriptions-item label="当前退货库位">
        {{ record.returnAreaName ? `${record.returnAreaName} / ` : '' }}{{ record.returnLocaCode || '-' }}
      </a-descriptions-item>
      <a-descriptions-item label="退还供应商" :span="2">
        <span style="font-weight: 600">{{ record.supplierName || '未关联供应商' }}</span>
        <span v-if="record.supplierLinkPhone" style="color: #8c8c8c; margin-left: 8px">
          ({{ [record.supplierLinkName, record.supplierLinkPhone].filter(Boolean).join(' ') }})
        </span>
      </a-descriptions-item>
      <a-descriptions-item v-if="record.returnRemark" label="前期退货备注" :span="2">
        {{ record.returnRemark }}
      </a-descriptions-item>
    </a-descriptions>

    <a-form layout="vertical">
      <a-form-item label="出库发运/交接签收说明" required help="请录入物流快递单号、承运车牌或供应商提货人员信息">
        <a-textarea
          v-model:value="shipRemark"
          placeholder="例如：顺丰快递单号 SF9876543210，运费到付；或供应商司机张师傅已当面清点提走"
          :rows="3"
          maxlength="200"
        />
      </a-form-item>
    </a-form>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { shipReturn } from '../WmsQualityCheck.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);

  const record = ref<Recordable>({});
  const shipRemark = ref<string>('');

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false });
    const row = data?.record || {};
    record.value = row;
    shipRemark.value = '';
  });

  async function handleSubmit() {
    if (!shipRemark.value || !shipRemark.value.trim()) {
      createMessage.warning('请填写出库发运/交接签收说明');
      return;
    }
    try {
      setModalProps({ confirmLoading: true });
      const res: any = await shipReturn({
        qcId: record.value.id,
        shipRemark: shipRemark.value.trim(),
      });
      const data = res?.result || res || {};
      createMessage.success(data.message || res?.message || '已确认退还供应商出库');
      closeModal();
      emit('success', data);
    } catch (e: any) {
      createMessage.error(e?.message || '确认退还出库失败');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
