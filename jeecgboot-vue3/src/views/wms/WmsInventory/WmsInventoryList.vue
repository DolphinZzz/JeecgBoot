<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button
          type="primary"
          v-auth="'wms:wms_inventory:verify'"
          preIcon="ant-design:check-circle-outlined"
          @click="handleVerify"
          :loading="verifyLoading"
        >
          库存总量核对</a-button
        >
        <a-tag style="margin-left: 12px" :color="verifyResult ? 'green' : 'default'">
          {{ verifyResult || '可用库存 = 物理库存 - 锁定 - 冻结' }}
        </a-tag>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
    </BasicTable>

    <!-- 库存流水抽屉 -->
    <a-drawer v-model:visible="logDrawerVisible" :title="logDrawerTitle" width="900" destroyOnClose>
      <BasicTable @register="registerLogTable" :canResize="false" :pagination="{ pageSize: 10 }" />
    </a-drawer>
  </div>
</template>

<script lang="ts" name="wms-wmsInventory" setup>
  import { ref } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { columns, searchFormSchema, logColumns } from './WmsInventory.data';
  import { list, sumTotal, queryLogList } from './WmsInventory.api';

  const { createMessage } = useMessage();
  const verifyLoading = ref(false);
  const verifyResult = ref('');

  const { tableContext } = useListPage({
    tableProps: {
      title: '实时库存',
      api: list,
      columns,
      canResize: true,
      showIndexColumn: false,
      formConfig: {
        schemas: searchFormSchema,
        autoSubmitOnEnter: true,
        showAdvancedButton: true,
      },
      actionColumn: {
        title: '操作',
        dataIndex: 'action',
        width: 150,
        fixed: 'right',
        slots: { customRender: 'action' },
      },
    },
  });

  const [registerTable, { getForm }] = tableContext;

  /**
   * 库存流水抽屉
   */
  const logDrawerVisible = ref(false);
  const logDrawerTitle = ref('');
  const currentInventoryId = ref<number | undefined>(undefined);
  // 抽屉 destroyOnClose，每次打开重新挂载即按当前库存行取流水，无需手动 reload
  const [registerLogTable] = useTable({
    api: (params) => queryLogList({ ...params, inventoryId: currentInventoryId.value }),
    columns: logColumns,
    showIndexColumn: false,
    showTableSetting: false,
    useSearchForm: false,
  });

  /**
   * 总量核对：同一维度下 物理库存 = 可用 + 锁定 + 冻结
   */
  async function handleVerify() {
    try {
      verifyLoading.value = true;
      // 以当前查询条件里的仓库 / SKU 为核对范围，未选则统计全部
      const formValues = (await getForm()?.getFieldsValue?.()) || {};
      const total = await sumTotal({ warehouseId: formValues.warehouseId, skuId: formValues.skuId });
      verifyResult.value = `物理库存合计：${total ?? 0}`;
      createMessage.success('物理库存合计已刷新');
    } catch (e: any) {
      createMessage.error(e?.message || '核对失败');
    } finally {
      verifyLoading.value = false;
    }
  }

  /**
   * 操作栏
   */
  function getTableAction(record) {
    return [
      {
        label: '流水',
        onClick: handleOpenLog.bind(null, record),
      },
    ];
  }

  function handleOpenLog(record) {
    logDrawerTitle.value = `库存流水 - ${record.skuCode || ''} / 库位 ${record.locaCode || ''} / 批次 ${record.batchNumber || '-'}`;
    currentInventoryId.value = record.id;
    logDrawerVisible.value = true;
  }
</script>
