<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    :title="title"
    :width="960"
    okText="确定选择该库位"
    :okButtonProps="{ disabled: !selectedRecord }"
    @ok="handleOk"
  >
    <div class="inv-loca-picker">
      <!-- 顶部信息摘要 -->
      <div v-if="targetSku" class="inv-loca-picker__stat-bar">
        <div class="stat-card stat-card--sku">
          <div class="stat-label">出库目标商品</div>
          <div class="stat-value text-primary">
            {{ targetSku.spuName || targetSku.skuName || '商品规格' }}
            <span v-if="targetSku.skuCode" class="stat-sub">({{ targetSku.skuCode }})</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-label">在库库位数</div>
          <div class="stat-value">{{ invList.length }} <span class="unit">个</span></div>
        </div>
        <div class="stat-card">
          <div class="stat-label">总可用库存</div>
          <div class="stat-value text-success font-bold">{{ totalAvailableCount }} <span class="unit">件</span></div>
        </div>
      </div>

      <!-- 搜索过滤栏 -->
      <div class="inv-loca-picker__filter">
        <a-input v-model:value="searchKeyword" allowClear placeholder="搜索库位编码 / 库区名称 / 批次号" style="width: 300px">
          <template #prefix><Icon icon="ant-design:search-outlined" /></template>
        </a-input>
        <span class="inv-loca-picker__summary">
          共找到 <b>{{ filteredList.length }}</b> 个可用库位
        </span>
      </div>

      <!-- 库位库存列表 -->
      <a-table
        size="small"
        bordered
        rowKey="warehouseAreaLocaId"
        :loading="loading"
        :columns="columns"
        :dataSource="filteredList"
        :pagination="{ pageSize: 8, showTotal: (t) => `共 ${t} 个库位` }"
        :row-selection="{
          type: 'radio',
          selectedRowKeys: selectedRowKeys,
          onChange: onSelectChange,
        }"
        :customRow="customRow"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'locaCode'">
            <a-tag color="blue" style="font-weight: bold">{{ record.locaCode }}</a-tag>
          </template>
          <template v-else-if="column.key === 'availableCount'">
            <a-tag color="green" style="font-size: 13px; font-weight: bold"> {{ record.availableCount ?? 0 }} 件 </a-tag>
          </template>
          <template v-else-if="column.key === 'batchNumber'">
            <span v-if="record.batchNumber">{{ record.batchNumber }}</span>
            <span v-else style="color: #999">-</span>
          </template>
          <template v-else-if="column.key === 'qualityStatus'">
            <a-tag color="success">良品</a-tag>
          </template>
        </template>
      </a-table>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { Icon } from '/@/components/Icon';
  import { queryInventoryList } from '../WmsInventory/WmsInventory.api';

  const emit = defineEmits(['register', 'selected']);

  const loading = ref(false);
  const title = ref('选择商品所在库位');
  const targetSku = ref<any>(null);
  const invList = ref<any[]>([]);
  const searchKeyword = ref('');
  const selectedRowKeys = ref<(string | number)[]>([]);
  const selectedRecord = ref<any>(null);

  const columns = [
    { title: '库位编码', key: 'locaCode', dataIndex: 'locaCode', width: 140 },
    { title: '所属库区', key: 'areaName', dataIndex: 'areaName', width: 130 },
    { title: '存储条件', key: 'storageCondition', dataIndex: 'storageCondition', width: 100 },
    { title: '批次号', key: 'batchNumber', width: 150 },
    { title: '有效期至', key: 'expireDate', dataIndex: 'expireDate', width: 130 },
    { title: '质量状态', key: 'qualityStatus', width: 90, align: 'center' },
    { title: '当前可用库存', key: 'availableCount', width: 130, align: 'right' },
    { title: '在库总数', key: 'totalCount', dataIndex: 'totalCount', width: 100, align: 'right' },
    { title: '货主', key: 'consignorName', dataIndex: 'consignorName', width: 120 },
  ];

  const totalAvailableCount = computed(() => invList.value.reduce((acc, cur) => acc + (cur.availableCount || 0), 0));

  const filteredList = computed(() => {
    const kw = searchKeyword.value.trim().toLowerCase();
    if (!kw) return invList.value;
    return invList.value.filter((item) => {
      const haystack = [item.locaCode, item.areaName, item.batchNumber, item.consignorName].filter(Boolean).join(' ').toLowerCase();
      return haystack.includes(kw);
    });
  });

  const [registerModal, { closeModal }] = useModalInner(async (data) => {
    targetSku.value = null;
    invList.value = [];
    searchKeyword.value = '';
    selectedRowKeys.value = [];
    selectedRecord.value = null;

    if (data?.sku) {
      targetSku.value = data.sku;
      title.value = `选择商品库位 - ${data.sku.spuName || data.sku.skuCode || ''}`;
    }

    if (data?.currentLocaId) {
      selectedRowKeys.value = [data.currentLocaId];
    }

    if (data?.warehouseId && data?.skuId) {
      await loadLocaInventories(data.warehouseId, data.skuId, data.currentLocaId);
    }
  });

  async function loadLocaInventories(warehouseId: number, skuId: number, currentLocaId?: number) {
    loading.value = true;
    try {
      const res: any = await queryInventoryList({
        warehouseId,
        skuId,
        qualityStatus: 1, // 仅良品
        onlyAvailable: true, // 只看有库存
        pageSize: 100,
      });

      const records = res?.records || res || [];
      // 过滤出可用库存 > 0 的库位
      invList.value = records.filter((r: any) => (r.availableCount || 0) > 0);

      // 若有已选库位，回显
      if (currentLocaId) {
        const found = invList.value.find((r: any) => r.warehouseAreaLocaId === currentLocaId);
        if (found) {
          selectedRecord.value = found;
          selectedRowKeys.value = [found.warehouseAreaLocaId];
        }
      }
    } finally {
      loading.value = false;
    }
  }

  function onSelectChange(keys: (string | number)[], rows: any[]) {
    selectedRowKeys.value = keys;
    selectedRecord.value = rows[0] || null;
  }

  function customRow(record: any) {
    return {
      onClick: () => {
        selectedRowKeys.value = [record.warehouseAreaLocaId];
        selectedRecord.value = record;
      },
    };
  }

  function handleOk() {
    if (!selectedRecord.value) return;
    emit('selected', selectedRecord.value);
    closeModal();
  }
</script>

<style lang="less" scoped>
  .inv-loca-picker {
    &__stat-bar {
      display: flex;
      gap: 12px;
      margin-bottom: 14px;
      flex-wrap: wrap;

      .stat-card {
        flex: 1;
        min-width: 140px;
        background: #f8fafd;
        border: 1px solid #e6f0fc;
        border-radius: 6px;
        padding: 8px 12px;

        &--sku {
          flex: 2;
          background: #f0f7ff;
          border-color: #d6e8ff;
        }

        .stat-label {
          font-size: 12px;
          color: #8c8c8c;
          margin-bottom: 2px;
        }

        .stat-value {
          font-size: 15px;
          font-weight: 500;
          color: #262626;

          &.text-primary {
            color: #1890ff;
          }

          &.text-success {
            color: #52c41a;
          }

          .stat-sub {
            font-size: 12px;
            color: #595959;
            font-weight: normal;
            margin-left: 4px;
          }

          .unit {
            font-size: 12px;
            font-weight: normal;
            color: #8c8c8c;
          }
        }
      }
    }

    &__filter {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }

    &__summary {
      font-size: 13px;
      color: #666;
    }
  }
</style>
