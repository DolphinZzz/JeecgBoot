<template>
  <div class="p-2">
    <a-card :bordered="false">
      <a-tabs v-model:activeKey="activeTab" type="card">
        <!-- Tab 1: 盘库单管理（主子表单据化作业、实盘录入、自动盘盈盘亏调账闭环） -->
        <a-tab-pane key="order" tab="盘库单管理">
          <WmsStocktakingOrderTab />
        </a-tab-pane>

        <!-- Tab 2: 库位盘点流水（保留库位级盘点流水记录） -->
        <a-tab-pane key="batch" tab="库位盘点流水">
          <WmsStocktakingBatchTab />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script lang="ts" name="wms-wmsStocktaking" setup>
  import { ref } from 'vue';
  import WmsStocktakingOrderTab from './components/WmsStocktakingOrderTab.vue';
  import WmsStocktakingBatchTab from './components/WmsStocktakingBatchTab.vue';

  // 默认直接进入「盘库单管理」单据化工作台
  const activeTab = ref<string>('order');
</script>

<style lang="less" scoped>
  :deep(.ant-tabs-nav) {
    margin-bottom: 8px;
  }
</style>
