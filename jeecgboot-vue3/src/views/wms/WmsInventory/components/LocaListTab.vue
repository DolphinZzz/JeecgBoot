<template>
  <div>
    <a-alert type="info" show-icon style="margin-bottom: 12px">
      <template #message>
        <span>
          <b>库位库存视图：</b>展示仓库物理库位的实际在库件数、可用件数、锁定件数与冻结件数。
          存储区与拣货区遵循<b>「一库位一绑定规格(SKU)至多一个在库批次」</b>，库存归零后恢复为空闲；点击操作栏<b>「明细」</b>可穿透查看具体在库记录，或点击<b>「移库」</b>进行库位移位调拨。
        </span>
      </template>
    </a-alert>

    <BasicTable @register="registerTable">
      <!-- 绑定规格插槽 -->
      <template #boundSku="{ record }">
        <template v-if="record.boundSkuId">
          <div style="display: flex; flex-direction: column; gap: 2px">
            <div style="font-weight: 500">
              {{ record.spuName || '未命名商品' }}
            </div>
            <div style="font-size: 12px; color: #8c8c8c">
              规格: <a-tag color="blue">{{ record.skuCode || record.boundSkuId }}</a-tag>
              <span v-if="record.skuName">({{ record.skuName }})</span>
            </div>
            <div v-if="record.isMixed" style="margin-top: 2px">
              <a-tag color="red">存在混放SKU</a-tag>
            </div>
          </div>
        </template>
        <span v-else style="color: #bfbfbf">
          <a-tag v-if="record.fixedSkuId" color="orange">固定未入库</a-tag>
          <a-tag v-else color="default">未占用</a-tag>
        </span>
      </template>

      <!-- 操作栏 -->
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
    </BasicTable>

    <!-- 库位库存明细抽屉 -->
    <a-drawer v-model:visible="detailDrawerVisible" :title="detailDrawerTitle" width="960" destroyOnClose>
      <BasicTable @register="registerDetailTable" :canResize="false" :pagination="{ pageSize: 10 }" />
    </a-drawer>

    <!-- 移库弹窗 -->
    <WmsMoveModal @register="registerMoveModal" @success="handleMoveSuccess" />
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { locaColumns, locaSearchFormSchema, locaDistributionColumns } from '../WmsInventory.data';
  import { queryLocaPageList, queryInventoryList } from '../WmsInventory.api';
  import WmsMoveModal from '../../WmsMove/components/WmsMoveModal.vue';

  const [registerMoveModal, { openModal: openMoveModal }] = useModal();

  const [registerTable, { reload }] = useTable({
    title: '库位库存列表',
    api: queryLocaPageList,
    columns: locaColumns,
    canResize: true,
    showIndexColumn: true,
    useSearchForm: true,
    formConfig: {
      schemas: locaSearchFormSchema,
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

  // 库位库存明细抽屉
  const detailDrawerVisible = ref(false);
  const detailDrawerTitle = ref('');
  const currentLocaId = ref<number | undefined>(undefined);

  const [registerDetailTable] = useTable({
    api: (params) =>
      queryInventoryList({
        ...params,
        warehouseAreaLocaId: currentLocaId.value,
        onlyAvailable: false,
      }),
    columns: locaDistributionColumns,
    showIndexColumn: true,
    showTableSetting: false,
    useSearchForm: false,
  });

  function getTableAction(record: any) {
    const actions: any[] = [
      {
        label: '明细',
        onClick: handleOpenDetail.bind(null, record),
      },
    ];
    if (Number(record.totalCount) > 0) {
      actions.push({
        label: '移库',
        onClick: handleOpenMove.bind(null, record),
      });
    }
    return actions;
  }

  function handleOpenDetail(record: any) {
    currentLocaId.value = record.locaId;
    detailDrawerTitle.value = `库位库存明细 - ${record.locaCode} (${record.warehouseName || ''} / ${record.areaName || ''})`;
    detailDrawerVisible.value = true;
  }

  function handleOpenMove(record: any) {
    openMoveModal(true, {
      prefill: {
        warehouseId: record.warehouseId,
        sourceWarehouseAreaLocaId: record.locaId,
        sourceLocaCode: record.locaCode,
        skuId: record.boundSkuId,
        skuCode: record.skuCode,
        spuName: record.spuName,
        batchNumber: record.batchNumber,
        availableCount: record.availableCount,
      },
    });
  }

  function handleMoveSuccess() {
    reload();
  }
</script>
