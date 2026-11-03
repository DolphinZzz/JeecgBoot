<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    title="实拣确认与SN追溯登记"
    :width="900"
    okText="确认实拣"
    @ok="handleSubmit"
  >
    <div v-loading="loading">
      <a-descriptions bordered size="small" :column="2" style="margin-bottom: 16px">
        <a-descriptions-item label="出库单号">{{ orderInfo?.outboundCode }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ orderInfo?.warehouseName }}</a-descriptions-item>
      </a-descriptions>

      <a-table
        size="small"
        bordered
        :pagination="false"
        :columns="pickColumns"
        :data-source="pickLines"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'pickedCount'">
            <a-input-number
              v-model:value="record.pickedCount"
              :min="1"
              :max="record.allocatedCount"
              style="width: 100%"
              placeholder="实拣件数"
            />
          </template>
          <template v-else-if="column.key === 'snCodes'">
            <div>
              <a-textarea
                v-model:value="record.snInput"
                :rows="2"
                placeholder="若含SN码，按回车或逗号分隔录入"
                @change="onSnInputChange(record)"
              />
              <div v-if="record.parsedSnList && record.parsedSnList.length > 0" style="margin-top: 4px; font-size: 12px">
                已识别 SN 数：<b>{{ record.parsedSnList.length }}</b> / 实拣：<b>{{ record.pickedCount || 0 }}</b>
                <span v-if="record.parsedSnList.length !== record.pickedCount" style="color: #ff4d4f; margin-left: 6px">
                  (数量不符)
                </span>
                <span v-else style="color: #52c41a; margin-left: 6px">
                  (校验通过)
                </span>
              </div>
            </div>
          </template>
        </template>
      </a-table>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { queryById, queryDetails, confirmPick } from '../WmsOutbound.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);
  const loading = ref(false);
  const orderInfo = ref<any>(null);
  const pickLines = ref<any[]>([]);

  const pickColumns = [
    { title: '商品名称', dataIndex: 'skuName', width: 160 },
    { title: '批次号', dataIndex: 'batchNumber', width: 130, align: 'center' },
    { title: '推荐库位', dataIndex: 'locaCode', width: 120, align: 'center' },
    { title: '已分配数', dataIndex: 'allocatedCount', width: 90, align: 'center' },
    { title: '实拣数量', key: 'pickedCount', width: 120, align: 'center' },
    { title: 'SN序列号(选填/SN管理品必填)', key: 'snCodes', width: 260 },
  ];

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    orderInfo.value = null;
    pickLines.value = [];
    if (data?.id) {
      await loadDetail(data.id);
    }
  });

  async function loadDetail(id: number | string) {
    loading.value = true;
    try {
      const [orderRes, detailsRes]: any = await Promise.all([queryById(id), queryDetails(id)]);
      orderInfo.value = orderRes || {};
      const rawDetails = detailsRes || [];
      pickLines.value = rawDetails.map((d: any) => ({
        ...d,
        skuName: `${d.skuCode || d.skuId || '-'} ${d.spuTitle || ''}`.trim(),
        pickedCount: d.pickedCount && d.pickedCount > 0 ? d.pickedCount : d.allocatedCount || 1,
        snInput: '',
        parsedSnList: [] as string[],
      }));
    } finally {
      loading.value = false;
    }
  }

  function onSnInputChange(record: any) {
    if (!record.snInput) {
      record.parsedSnList = [];
      return;
    }
    const list = record.snInput
      .split(/[\n,，\s]+/)
      .map((s: string) => s.trim())
      .filter((s: string) => s.length > 0);
    record.parsedSnList = list;
  }

  async function handleSubmit() {
    if (!pickLines.value || pickLines.value.length === 0) {
      createMessage.warning('出库明细为空，无法拣货');
      return;
    }

    const payloadLines: any[] = [];
    for (const line of pickLines.value) {
      const count = Number(line.pickedCount);
      if (!count || count <= 0) {
        createMessage.warning(`请录入有效的实拣数量`);
        return;
      }
      if (line.parsedSnList && line.parsedSnList.length > 0) {
        if (line.parsedSnList.length !== count) {
          createMessage.error(`商品 [${line.skuName || line.skuId}] 录入的 SN 数量 (${line.parsedSnList.length}) 与实拣数量 (${count}) 不一致！`);
          return;
        }
      }

      payloadLines.push({
        detailId: line.id,
        pickedCount: count,
        snCodes: line.parsedSnList || [],
      });
    }

    setModalProps({ confirmLoading: true });
    try {
      await confirmPick({
        outboundOrderId: orderInfo.value.id,
        pickLines: payloadLines,
      });
      createMessage.success('实拣确认成功！单据已推进至待复核');
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
