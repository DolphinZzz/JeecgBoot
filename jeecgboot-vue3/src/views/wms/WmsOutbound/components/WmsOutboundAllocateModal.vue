<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    title="库存分配结果"
    :width="850"
    :showCancelBtn="false"
    okText="确定"
    @ok="handleOk"
  >
    <div v-loading="loading">
      <a-alert
        v-if="allocateResult"
        :type="alertType"
        show-icon
        style="margin-bottom: 16px"
      >
        <template #message>
          <span style="font-weight: bold; font-size: 14px">{{ alertTitle }}</span>
        </template>
        <template #description>
          <div>出库单号：<b>{{ allocateResult.outboundCode }}</b></div>
          <div>当前单据状态：<a-tag :color="statusColorMap[allocateResult.status] || 'default'">{{ statusTextMap[allocateResult.status] || '-' }}</a-tag></div>
          <div v-if="allocateResult.status === 30" style="color: #faad14; margin-top: 4px">
            提示：本单存在缺货行且策略为整单挂起，已自动回滚全部锁定库存，请安排仓库补货后重新分配。
          </div>
        </template>
      </a-alert>

      <a-table
        v-if="allocateResult"
        size="small"
        bordered
        :pagination="false"
        :columns="resultColumns"
        :data-source="allocateResult.lineResults || []"
        row-key="detailId"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'isShortage'">
            <a-tag :color="record.isShortage ? 'error' : 'success'">
              {{ record.isShortage ? '缺货' : '充足' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'shortageCount'">
            <span :style="{ color: record.shortageCount > 0 ? '#ff4d4f' : 'inherit', fontWeight: record.shortageCount > 0 ? 'bold' : 'normal' }">
              {{ record.shortageCount || 0 }}
            </span>
          </template>
        </template>
      </a-table>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { allocateInventory } from '../WmsOutbound.api';
  import { statusColorMap, statusTextMap } from '../WmsOutbound.data';

  const emit = defineEmits(['register', 'success']);
  const loading = ref(false);
  const allocateResult = ref<any>(null);

  const [registerModal, { closeModal }] = useModalInner(async (data) => {
    allocateResult.value = null;
    if (data?.id) {
      await doAllocate(data.id);
    }
  });

  const resultColumns = [
    { title: '明细ID', dataIndex: 'detailId', width: 90, align: 'center' },
    { title: '计划数量', dataIndex: 'planCount', width: 100, align: 'center' },
    { title: '成功分配数', dataIndex: 'allocatedCount', width: 110, align: 'center' },
    { title: '库存状态', key: 'isShortage', width: 100, align: 'center' },
    { title: '缺货件数', key: 'shortageCount', width: 100, align: 'center' },
  ];

  const alertType = computed(() => {
    if (!allocateResult.value) return 'info';
    return allocateResult.value.status === 30 ? 'warning' : 'success';
  });

  const alertTitle = computed(() => {
    if (!allocateResult.value) return '';
    return allocateResult.value.status === 30 ? '分配完成：存在缺货并挂起' : '分配锁库成功';
  });

  async function doAllocate(id: string | number) {
    loading.value = true;
    try {
      const res = await allocateInventory(id);
      allocateResult.value = res;
      emit('success');
    } finally {
      loading.value = false;
    }
  }

  function handleOk() {
    closeModal();
  }
</script>
