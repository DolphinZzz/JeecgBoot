<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    title="波次单详情"
    :width="960"
    :showFooter="false"
  >
    <div v-loading="loading">
      <a-descriptions bordered size="small" :column="3" style="margin-bottom: 16px">
        <a-descriptions-item label="波次编号">
          <b>{{ waveInfo?.waveCode }}</b>
        </a-descriptions-item>
        <a-descriptions-item label="波次状态">
          <a-tag :color="waveStatusColorMap[waveInfo?.status] || 'default'">
            {{ waveStatusTextMap[waveInfo?.status] || '-' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="所属仓库">{{ waveInfo?.warehouseName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="承运商">{{ waveInfo?.logisticsCompanyName || '未指定' }}</a-descriptions-item>
        <a-descriptions-item label="存储温区">
          <a-tag :color="storageConditionColorMap[waveInfo?.storageCondition] || 'default'">
            {{ waveInfo?.storageCondition || '常温' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="包含订单数">{{ waveInfo?.orderCount || 0 }}</a-descriptions-item>
        <a-descriptions-item label="SKU品类数">{{ waveInfo?.skuCount || 0 }}</a-descriptions-item>
        <a-descriptions-item label="商品总件数">
          <span style="color: #1890ff; font-weight: bold">{{ waveInfo?.totalCount || 0 }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ waveInfo?.createTime || '-' }}</a-descriptions-item>
      </a-descriptions>

      <a-tabs default-active-key="1">
        <a-tab-pane key="1" tab="汇总拣货任务清单">
          <a-table
            size="small"
            bordered
            :pagination="false"
            :columns="pickColumns"
            :data-source="pickItems"
            row-key="skuId"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'totalPickCount'">
                <span style="color: #52c41a; font-weight: bold">{{ record.totalPickCount }}</span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="2" tab="包含的出库单清单">
          <a-table
            size="small"
            bordered
            :pagination="false"
            :columns="orderColumns"
            :data-source="outboundOrders"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="statusColorMap[record.status] || 'default'">
                  {{ statusTextMap[record.status] || record.status_dictText || '-' }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </div>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { queryById } from '../WmsWave.api';
  import { waveStatusColorMap, waveStatusTextMap, storageConditionColorMap } from '../WmsWave.data';
  import { statusColorMap, statusTextMap } from '../../WmsOutbound/WmsOutbound.data';

  const loading = ref(false);
  const waveInfo = ref<any>(null);
  const pickItems = ref<any[]>([]);
  const outboundOrders = ref<any[]>([]);

  const pickColumns = [
    { title: '商品编码', dataIndex: 'skuCode', width: 150 },
    { title: '商品名称', dataIndex: 'skuName', width: 220 },
    { title: '推荐拣货库位', dataIndex: 'locaCode', width: 140, align: 'center' },
    { title: '汇总拣货件数', key: 'totalPickCount', width: 120, align: 'center' },
  ];

  const orderColumns = [
    { title: '出库单号', dataIndex: 'outboundCode', width: 170 },
    { title: '客户', dataIndex: 'customerName', width: 150 },
    { title: '单据状态', key: 'status', width: 120, align: 'center' },
    { title: '计划件数', dataIndex: 'totalCount', width: 100, align: 'center' },
    { title: '创建时间', dataIndex: 'createTime', width: 160, align: 'center' },
  ];

  const [registerDrawer] = useDrawerInner(async (data) => {
    waveInfo.value = null;
    pickItems.value = [];
    outboundOrders.value = [];

    if (data?.id) {
      await loadDetail(data.id);
    }
  });

  async function loadDetail(id: number | string) {
    loading.value = true;
    try {
      const res = await queryById(id);
      waveInfo.value = res?.waveOrder || {};
      pickItems.value = res?.pickItems || [];
      outboundOrders.value = res?.outboundOrders || [];
    } finally {
      loading.value = false;
    }
  }
</script>
