<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose title="收货清点" :width="1200" @ok="handleSubmit">
    <a-descriptions :column="4" size="small" bordered style="margin-bottom: 12px">
      <a-descriptions-item label="入库单号">{{ entry.entryCode || '-' }}</a-descriptions-item>
      <a-descriptions-item label="仓库">{{ entry.warehouseName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="货主">{{ entry.consignorName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="批次号">{{ entry.batchNumber || '-' }}</a-descriptions-item>
    </a-descriptions>

    <JVxeTable
      keep-source
      resizable
      ref="receiveTableRef"
      :columns="receiveEditColumns"
      :dataSource="tableData.dataSource"
      :height="330"
      :rowNumber="true"
      :toolbar="false"
    />

    <a-alert type="info" show-icon style="margin-top: 12px">
      <template #message>
        实收数量含破损件；破损数量直接计入退货区，不参与上架。超收与少收由后端按「实收 − 计划」自动计算：超收需主管放行，少收自动登记待补料。
      </template>
    </a-alert>
    <a-alert v-if="snWarning" type="warning" show-icon style="margin-top: 8px">
      <template #message>SN 管理商品的 SN 数量必须等于「实收 − 破损」，请补扫后再提交。</template>
    </a-alert>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { JVxeTable } from '/@/components/jeecg/JVxeTable';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { receiveEditColumns } from '../WmsEntry.data';
  import { queryById, queryDetails, receive } from '../WmsEntry.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);

  const receiveTableRef = ref();
  const tableData = reactive({ dataSource: [] as any[] });
  const entry = ref<Recordable>({});
  const snWarning = ref(false);

  const details = ref<any[]>([]);

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false });
    snWarning.value = false;
    const record = data?.record || {};
    entry.value = record;
    tableData.dataSource = [];
    const [head, list]: any = await Promise.all([queryById(record.id), queryDetails(record.id)]);
    entry.value = { ...record, ...(head || {}) };
    details.value = list || [];
    tableData.dataSource = details.value.map((d) => ({
      // 以明细ID 作为行 key，JVxeTable 默认用 id 作 rowKey
      id: d.id,
      detailId: d.id,
      skuCode: d.skuCode || d.skuId_dictText || '-',
      spuTitle: d.spuTitle || '-',
      count: d.count ?? 0,
      receivedCount: d.receivedCount ?? d.count ?? 0,
      damageCount: 0,
      snCodesText: '',
      isSnManaged: d.isSnManaged,
    }));
  });

  function parseSnCodes(text: string): string[] {
    if (!text) return [];
    return String(text)
      .split(/[,，\s]+/)
      .map((s) => s.trim())
      .filter((s) => s.length > 0);
  }

  async function handleSubmit() {
    try {
      const rows: any[] = receiveTableRef.value?.getTableData() ?? tableData.dataSource;
      const lines = rows.map((row) => ({
        detailId: Number(row.detailId),
        receivedCount: Number(row.receivedCount ?? 0),
        damageCount: Number(row.damageCount ?? 0),
        snCodes: parseSnCodes(row.snCodesText),
      }));
      const invalid = lines.find((l) => l.damageCount > l.receivedCount);
      if (invalid) {
        createMessage.warning('破损数量不能大于实收数量');
        return;
      }
      snWarning.value = false;
      setModalProps({ confirmLoading: true });
      const res: any = await receive({ id: entry.value.id, details: lines });
      const data = res?.result || res || {};
      createMessage.success(data.message || res?.message || '收货完成');
      snWarning.value = false;
      closeModal();
      emit('success', data);
    } catch (e: any) {
      if (e?.errorFields) return;
      const msg = e?.message || '收货失败';
      snWarning.value = msg.includes('SN');
      createMessage.error(msg);
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
