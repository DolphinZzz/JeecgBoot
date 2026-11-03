<template>
  <div class="p-2">
    <a-card :bordered="false">
      <a-tabs v-model:activeKey="activeTab" type="card">
        <!-- Tab 1: 商品库存（支持仓库库位分布与商品规格总账双重视角） -->
        <a-tab-pane key="ledger" tab="商品库存（多维台账）">
          <StockLedgerTab />
        </a-tab-pane>

        <!-- Tab 2: 库位库存（库位物理占用视角） -->
        <a-tab-pane key="loca" tab="库位监控（空间占用）">
          <LocaListTab />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script lang="ts" name="wms-wmsInventory" setup>
  import { ref } from 'vue';
  import LocaListTab from './components/LocaListTab.vue';
  import StockLedgerTab from './components/StockLedgerTab.vue';

  // 默认进入商品库存多维台账页，直达仓库库位分布与商品总账
  const activeTab = ref<string>('ledger');
</script>

<style lang="less" scoped>
  :deep(.ant-tabs-nav) {
    margin-bottom: 8px;
  }
</style>
