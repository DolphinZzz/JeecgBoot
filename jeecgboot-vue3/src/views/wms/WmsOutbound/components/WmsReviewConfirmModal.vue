<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose title="出库复核确认" :width="760" okText="提交复核" @ok="handleSubmit">
    <div v-loading="loading">
      <a-descriptions bordered size="small" :column="2" style="margin-bottom: 16px">
        <a-descriptions-item label="出库单号">{{ orderInfo?.outboundCode }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ orderInfo?.warehouseName }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ orderInfo?.customerName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="拣货总件数">{{ orderInfo?.pickedTotal ?? orderInfo?.pickedCount ?? 0 }}</a-descriptions-item>
      </a-descriptions>

      <!-- 复核明细清单 -->
      <a-table
        v-if="detailList.length > 0"
        size="small"
        bordered
        :pagination="false"
        :columns="reviewColumns"
        :data-source="detailList"
        row-key="id"
        style="margin-bottom: 16px"
      />

      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
        <a-form-item label="复核结果" required>
          <a-radio-group v-model:value="formData.reviewStatus">
            <a-radio :value="1">
              <span style="color: #52c41a; font-weight: bold">复核通过</span>
            </a-radio>
            <a-radio :value="2">
              <span style="color: #ff4d4f; font-weight: bold">复核异常</span>
            </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="复核意见/备注">
          <a-textarea v-model:value="formData.reviewRemark" :rows="3" maxlength="255" placeholder="请输入复核说明（异常时必填）" />
        </a-form-item>
      </a-form>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { confirmReview, queryDetails } from '../WmsOutbound.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);
  const loading = ref(false);
  const orderInfo = ref<any>(null);
  const detailList = ref<any[]>([]);

  const reviewColumns = [
    { title: '商品编码', dataIndex: 'skuCode', width: 140 },
    { title: '商品名称', dataIndex: 'skuName', width: 180 },
    { title: '出库库位', dataIndex: 'locaCode', width: 120, align: 'center' },
    { title: '批次号', dataIndex: 'batchNumber', width: 140, align: 'center' },
    { title: '实拣数量', dataIndex: 'pickedCount', width: 90, align: 'center' },
  ];

  const formData = reactive({
    reviewStatus: 1,
    reviewRemark: '',
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    orderInfo.value = data?.record || {};
    formData.reviewStatus = 1;
    formData.reviewRemark = '';
    detailList.value = [];
    setModalProps({ confirmLoading: false });

    if (orderInfo.value?.id) {
      loading.value = true;
      try {
        const detailsRes: any = await queryDetails(orderInfo.value.id);
        detailList.value = detailsRes || [];
      } finally {
        loading.value = false;
      }
    }
  });

  async function handleSubmit() {
    if (formData.reviewStatus === 2 && !formData.reviewRemark.trim()) {
      createMessage.warning('复核异常时必须录入复核意见！');
      return;
    }

    const reviewLines = (detailList.value || []).map((d: any) => ({
      detailId: d.id,
      reviewCount: formData.reviewStatus === 1 ? d.pickedCount || d.allocatedCount || 0 : 0,
      reviewStatus: formData.reviewStatus,
    }));

    setModalProps({ confirmLoading: true });
    try {
      await confirmReview({
        outboundOrderId: orderInfo.value.id,
        reviewStatus: formData.reviewStatus,
        reviewRemark: formData.reviewRemark,
        reviewLines,
      });
      createMessage.success('复核操作完成！');
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
