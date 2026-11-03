<template>
  <div class="stock-ledger-container">
    <!-- 顶部业务视角切换与实时统计栏 -->
    <div class="view-header">
      <div class="view-switcher">
        <span class="switcher-label">统计视角：</span>
        <a-radio-group
          v-model:value="currentDimension"
          button-style="solid"
          size="middle"
          @change="handleDimensionChange"
        >
          <a-radio-button value="warehouse">
            <ShopOutlined style="margin-right: 4px" />仓库库位分布
          </a-radio-button>
          <a-radio-button value="sku">
            <AppstoreOutlined style="margin-right: 4px" />商品规格总账
          </a-radio-button>
        </a-radio-group>
      </div>

      <!-- 右侧业务状态与统计指标（替代生硬的长文本） -->
      <div class="stat-indicators">
        <template v-if="currentDimension === 'warehouse'">
          <div class="stat-pill">
            <span class="stat-label">在库总计</span>
            <span class="stat-number text-primary">{{ warehouseTotalCount }}</span>
            <span class="stat-unit">件</span>
          </div>
          <div class="stat-pill">
            <span class="stat-label">可用库存</span>
            <span class="stat-number text-success">{{ warehouseAvailableCount }}</span>
            <span class="stat-unit">件</span>
          </div>
          <div class="stat-pill">
            <span class="stat-label">占用库位</span>
            <span class="stat-number text-warning">{{ warehouseLocaCount }}</span>
            <span class="stat-unit">个</span>
          </div>
          <a-button type="default" size="small" preIcon="ant-design:sync-outlined" @click="handleRefreshWarehouse">
            刷新
          </a-button>
        </template>

        <template v-else>
          <div class="stat-pill">
            <span class="stat-label">物理在库</span>
            <span class="stat-number text-primary">{{ ledgerTotalCount }}</span>
            <span class="stat-unit">件</span>
          </div>
          <a-button
            type="primary"
            size="small"
            v-auth="'wms:wms_inventory:verify'"
            preIcon="ant-design:check-circle-outlined"
            @click="handleVerify"
            :loading="verifyLoading"
          >
            库存总量核对
          </a-button>
          <a-tag v-if="verifyResult" color="green" style="margin-left: 6px">
            {{ verifyResult }}
          </a-tag>
        </template>
      </div>
    </div>

    <!-- 视图一：仓库库位分布（从仓库层级查看具体库位上存放的货品） -->
    <div v-show="currentDimension === 'warehouse'" class="table-wrapper">
      <BasicTable @register="registerWarehouseTable">
        <template #action="{ record }">
          <TableAction :actions="getWarehouseTableAction(record)" />
        </template>
      </BasicTable>
    </div>

    <!-- 视图二：商品规格总账（从商品层级查看全仓全周期各状态与可用量） -->
    <div v-show="currentDimension === 'sku'" class="table-wrapper">
      <BasicTable @register="registerSkuTable">
        <template #action="{ record }">
          <TableAction :actions="getSkuTableAction(record)" />
        </template>
      </BasicTable>
    </div>

    <!-- 库位分布抽屉（商品维度穿透） -->
    <a-drawer v-model:visible="locaDrawerVisible" :title="locaDrawerTitle" width="960" destroyOnClose>
      <BasicTable @register="registerLocaTable" :canResize="false" :pagination="{ pageSize: 10 }" />
    </a-drawer>

    <!-- 库存流水抽屉 -->
    <a-drawer v-model:visible="logDrawerVisible" :title="logDrawerTitle" width="920" destroyOnClose>
      <BasicTable @register="registerLogTable" :canResize="false" :pagination="{ pageSize: 10 }" />
    </a-drawer>

    <!-- 移库作业弹窗（支持从仓库维度直接发起在库商品移位） -->
    <WmsMoveModal @register="registerMoveModal" @success="handleMoveSuccess" />
  </div>
</template>

<script lang="ts" setup>
  import { ref, h, nextTick } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { ShopOutlined, AppstoreOutlined } from '@ant-design/icons-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    warehouseColumns,
    warehouseSearchFormSchema,
    ledgerColumns,
    ledgerSearchFormSchema,
    logColumns,
    locaDistributionColumns,
  } from '../WmsInventory.data';
  import { queryLedgerPageList, sumTotal, queryLogList, queryInventoryList } from '../WmsInventory.api';
  import WmsMoveModal from '../../WmsMove/components/WmsMoveModal.vue';

  const { createMessage } = useMessage();

  // 当前视角：默认从仓库库位分布切入
  const currentDimension = ref<'warehouse' | 'sku'>('warehouse');

  const [registerMoveModal, { openModal: openMoveModal }] = useModal();

  // 统计指标
  const warehouseTotalCount = ref(0);
  const warehouseAvailableCount = ref(0);
  const warehouseLocaCount = ref(0);
  const ledgerTotalCount = ref(0);

  // ==========================================
  // 视图一：仓库库位分布
  // ==========================================
  const [registerWarehouseTable, { reload: reloadWarehouseTable }] = useTable({
    api: (params) => queryInventoryList({ ...params, onlyAvailable: true }),
    afterFetch: (records: any[]) => {
      if (Array.isArray(records)) {
        warehouseTotalCount.value = records.reduce((sum, r) => sum + (Number(r.totalCount) || 0), 0);
        warehouseAvailableCount.value = records.reduce((sum, r) => sum + (Number(r.availableCount) || 0), 0);
        const locaSet = new Set(records.map((r) => r.warehouseAreaLocaId).filter(Boolean));
        warehouseLocaCount.value = locaSet.size;
      }
      return records;
    },
    columns: warehouseColumns,
    canResize: true,
    showIndexColumn: true,
    useSearchForm: true,
    formConfig: {
      schemas: warehouseSearchFormSchema,
      autoSubmitOnEnter: true,
      showAdvancedButton: true,
    },
    actionColumn: {
      title: '操作',
      dataIndex: 'action',
      width: 130,
      fixed: 'right',
      slots: { customRender: 'action' },
    },
  });

  function getWarehouseTableAction(record: any) {
    const actions: any[] = [];
    if (Number(record.availableCount) > 0) {
      actions.push({
        label: '移库',
        onClick: handleOpenMoveForWarehouseRow.bind(null, record),
      });
    }
    actions.push({
      label: '流水',
      onClick: handleOpenLogForWarehouseRow.bind(null, record),
    });
    return actions;
  }

  function handleOpenMoveForWarehouseRow(record: any) {
    openMoveModal(true, {
      prefill: {
        id: record.id,
        warehouseId: record.warehouseId,
        warehouseAreaId: record.warehouseAreaId,
        warehouseAreaName: record.areaName,
        sourceWarehouseAreaLocaId: record.warehouseAreaLocaId,
        locaCode: record.locaCode,
        skuId: record.skuId,
        skuCode: record.skuCode,
        spuTitle: record.spuTitle,
        batchNumber: record.batchNumber,
        consignorId: record.consignorId,
        consignorName: record.consignorName,
        qualityStatus: record.qualityStatus,
        availableCount: record.availableCount,
      },
    });
  }

  function handleOpenLogForWarehouseRow(record: any) {
    logDrawerTitle.value = `库存变动流水 - ${record.spuTitle || ''} [${record.skuCode || ''}] (${record.warehouseName || ''} / ${record.locaCode || ''})`;
    currentSkuId.value = record.skuId;
    currentWarehouseId.value = record.warehouseId;
    currentLocaId.value = record.warehouseAreaLocaId;
    logDrawerVisible.value = true;
  }

  function handleRefreshWarehouse() {
    reloadWarehouseTable();
    createMessage.success('已刷新仓库库存数据');
  }

  function handleMoveSuccess() {
    reloadWarehouseTable();
    reloadSkuTable();
  }

  // ==========================================
  // 视图二：商品规格总账
  // ==========================================
  const verifyLoading = ref(false);
  const verifyResult = ref('');

  const [registerSkuTable, { reload: reloadSkuTable, getForm: getSkuForm }] = useTable({
    api: queryLedgerPageList,
    afterFetch: (records: any[]) => {
      if (Array.isArray(records)) {
        ledgerTotalCount.value = records.reduce((sum, r) => sum + (Number(r.totalCount) || 0), 0);
      }
      return records;
    },
    columns: ledgerColumns,
    canResize: true,
    showIndexColumn: true,
    useSearchForm: true,
    formConfig: {
      schemas: ledgerSearchFormSchema,
      autoSubmitOnEnter: true,
      showAdvancedButton: true,
    },
    actionColumn: {
      title: '操作',
      dataIndex: 'action',
      width: 140,
      fixed: 'right',
      slots: { customRender: 'action' },
    },
  });

  function getSkuTableAction(record: any) {
    return [
      {
        label: '库位分布',
        onClick: handleOpenLocaDistribution.bind(null, record),
      },
      {
        label: '流水',
        onClick: handleOpenLogForSkuRow.bind(null, record),
      },
    ];
  }

  async function handleVerify() {
    try {
      verifyLoading.value = true;
      const formValues = (await getSkuForm()?.getFieldsValue?.()) || {};
      const total = await sumTotal({
        warehouseId: formValues.warehouseId,
      });
      verifyResult.value = `物理库存合计：${total ?? 0} 件`;
      createMessage.success('物理库存合计已刷新');
    } catch (e: any) {
      createMessage.error(e?.message || '核对失败');
    } finally {
      verifyLoading.value = false;
    }
  }

  function handleDimensionChange() {
    nextTick(() => {
      if (currentDimension.value === 'warehouse') {
        reloadWarehouseTable();
      } else {
        reloadSkuTable();
      }
    });
  }

  // ==========================================
  // 抽屉：库位分布与库存流水
  // ==========================================
  const currentSkuId = ref<number | undefined>(undefined);
  const currentWarehouseId = ref<number | undefined>(undefined);
  const currentLocaId = ref<number | undefined>(undefined);

  const locaDrawerVisible = ref(false);
  const locaDrawerTitle = ref('');

  const [registerLocaTable] = useTable({
    api: (params) =>
      queryInventoryList({
        ...params,
        skuId: currentSkuId.value,
        warehouseId: currentWarehouseId.value,
        onlyAvailable: false,
      }),
    columns: locaDistributionColumns,
    showIndexColumn: true,
    showTableSetting: false,
    useSearchForm: false,
  });

  function handleOpenLocaDistribution(record: any) {
    locaDrawerTitle.value = `库位分布 - ${record.spuName || ''} (${record.skuCode || ''}) - ${record.warehouseName || '全仓'}`;
    currentSkuId.value = record.skuId;
    currentWarehouseId.value = record.warehouseId;
    currentLocaId.value = undefined;
    locaDrawerVisible.value = true;
  }

  const logDrawerVisible = ref(false);
  const logDrawerTitle = ref('');

  const [registerLogTable] = useTable({
    api: (params) =>
      queryLogList({
        ...params,
        skuId: currentSkuId.value,
        warehouseId: currentWarehouseId.value,
        warehouseAreaLocaId: currentLocaId.value,
      }),
    columns: logColumns,
    showIndexColumn: false,
    showTableSetting: false,
    useSearchForm: false,
  });

  function handleOpenLogForSkuRow(record: any) {
    logDrawerTitle.value = `库存变动流水 - ${record.spuName || ''} (${record.skuCode || ''}) - ${record.warehouseName || '全仓'}`;
    currentSkuId.value = record.skuId;
    currentWarehouseId.value = record.warehouseId;
    currentLocaId.value = undefined;
    logDrawerVisible.value = true;
  }
</script>

<style lang="less" scoped>
  .stock-ledger-container {
    .view-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      padding-bottom: 10px;
      border-bottom: 1px solid #f0f0f0;

      .view-switcher {
        display: flex;
        align-items: center;

        .switcher-label {
          font-size: 14px;
          font-weight: 600;
          color: #334155;
          margin-right: 8px;
        }

        :deep(.ant-radio-button-wrapper) {
          display: inline-flex;
          align-items: center;
          font-weight: 500;
          padding: 0 16px;
          height: 34px;
          line-height: 32px;
          border-color: #cbd5e1;
        }

        :deep(.ant-radio-button-wrapper-checked:not(.ant-radio-button-wrapper-disabled)) {
          background-color: #1890ff;
          border-color: #1890ff;
          color: #ffffff;
          box-shadow: 0 2px 6px rgba(24, 144, 255, 0.3);
        }
      }

      .stat-indicators {
        display: flex;
        align-items: center;
        gap: 10px;

        .stat-pill {
          display: inline-flex;
          align-items: center;
          padding: 3px 10px;
          background: #f8fafc;
          border: 1px solid #e2e8f0;
          border-radius: 4px;
          font-size: 13px;

          .stat-label {
            color: #64748b;
            margin-right: 6px;
          }

          .stat-number {
            font-weight: 700;
            font-size: 14px;
            margin-right: 2px;
          }

          .stat-unit {
            color: #94a3b8;
            font-size: 11px;
          }

          .text-primary {
            color: #1890ff;
          }

          .text-success {
            color: #52c41a;
          }

          .text-warning {
            color: #fa8c16;
          }
        }
      }
    }
  }
</style>
