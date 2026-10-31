<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    title="不合格品退货处置（放置退货区）"
    :width="680"
    @ok="handleSubmit"
  >
    <a-descriptions :column="2" size="small" bordered style="margin-bottom: 12px">
      <a-descriptions-item label="质检单号">{{ record.qcCode || '-' }}</a-descriptions-item>
      <a-descriptions-item label="入库单号">{{ record.entryCode || '-' }}</a-descriptions-item>
      <a-descriptions-item label="商品信息" :span="2">
        {{ `${record.skuCode || record.skuId_dictText || '-'} ${record.spuTitle || ''}`.trim() }}
      </a-descriptions-item>
      <a-descriptions-item label="批次号">{{ record.batchNumber || '-' }}</a-descriptions-item>
      <a-descriptions-item label="不合格数量">
        <span style="color: #f5222d; font-weight: bold; font-size: 15px">{{ record.failCount ?? 0 }}</span> 件
      </a-descriptions-item>
      <a-descriptions-item label="不合格原因" :span="2">{{ record.failReason || '未填写' }}</a-descriptions-item>
    </a-descriptions>

    <!-- 供应商联络卡片 -->
    <a-card size="small" title="供应商联系信息" style="margin-bottom: 12px; background-color: #fafafa">
      <a-descriptions :column="2" size="small" bordered>
        <a-descriptions-item label="供应商名称">
          <span style="font-weight: 600">{{ record.supplierName || '未关联供应商' }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="联系人及电话">
          {{ [record.supplierLinkName, record.supplierLinkPhone].filter(Boolean).join(' / ') || '暂无电话' }}
        </a-descriptions-item>
        <a-descriptions-item label="退货地址" :span="2">
          {{ record.supplierAddress || '暂无地址' }}
        </a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-form layout="vertical">
      <a-form-item
        label="退货货架库位（不良品放置区）"
        required
        help="不合格货物必须严格放置到退货不良区（RETURN），系统将落库并生成不良品库存留痕"
      >
        <a-select
          v-model:value="returnLocaId"
          placeholder="请选择退货货架库位"
          style="width: 100%"
          :loading="loadingLocas"
        >
          <a-select-option v-for="item in returnLocaList" :key="item.id" :value="item.id">
            {{ item.locaCode }} [{{ item.shelfNumber ? `货架${item.shelfNumber}` : '' }} {{ item.layerNumber ? `第${item.layerNumber}层` : '' }} 承重{{ item.localBearingCapacity || 0 }}g]
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="退货联系/处置沟通说明">
        <a-textarea
          v-model:value="returnRemark"
          placeholder="例如：已电话联系供应商李经理确认退货返厂，顺丰快递单号 SF1234567890；或供应商将于明天下午派车提走"
          :rows="3"
          maxlength="300"
        />
      </a-form-item>
    </a-form>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { handleReturn, listReturnLocaOptions } from '../WmsQualityCheck.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);

  const record = ref<Recordable>({});
  const returnLocaId = ref<number | undefined>(undefined);
  const returnRemark = ref<string>('');
  const returnLocaList = ref<any[]>([]);
  const loadingLocas = ref(false);

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false });
    const row = data?.record || {};
    record.value = row;
    returnLocaId.value = row.returnLocaId || undefined;
    returnRemark.value = row.returnRemark || '';

    // 加载退货货架库位
    if (row.id) {
      loadingLocas.value = true;
      try {
        const res: any = await listReturnLocaOptions({ qcId: row.id, warehouseId: row.warehouseId });
        returnLocaList.value = Array.isArray(res) ? res : res?.result || [];
      } catch (e) {
        returnLocaList.value = [];
      } finally {
        loadingLocas.value = false;
      }
    }
  });

  async function handleSubmit() {
    if (!returnLocaId.value) {
      createMessage.warning('请选择退货货架库位');
      return;
    }
    try {
      setModalProps({ confirmLoading: true });
      const res: any = await handleReturn({
        qcId: record.value.id,
        returnLocaId: returnLocaId.value,
        returnRemark: returnRemark.value ? returnRemark.value.trim() : undefined,
      });
      const data = res?.result || res || {};
      createMessage.success(data.message || res?.message || '退货处置成功');
      closeModal();
      emit('success', data);
    } catch (e: any) {
      createMessage.error(e?.message || '退货处置失败');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
