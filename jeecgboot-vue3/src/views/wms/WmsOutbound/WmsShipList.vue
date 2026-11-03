<template>
  <div>
    <a-tabs v-model:activeKey="activeTabKey" @change="handleTabChange" style="background: #fff; padding: 12px 16px 0">
      <a-tab-pane key="70" tab="待发货 (70)" />
      <a-tab-pane key="80" tab="已发货 (80)" />
      <a-tab-pane key="ALL" tab="全部单据" />
    </a-tabs>

    <BasicTable @register="registerTable">
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
    </BasicTable>

    <!-- 弹窗 -->
    <WmsShipConfirmModal @register="registerShipModal" @success="reload" />
    <WmsOutboundDetailDrawer @register="registerDetailDrawer" />
  </div>
</template>

<script lang="ts" name="wms-wmsShip" setup>
  import { ref } from 'vue';
  import { BasicTable, TableAction, ActionItem } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useDrawer } from '/@/components/Drawer';
  import { useListPage } from '/@/hooks/system/useListPage';
  import WmsShipConfirmModal from './components/WmsShipConfirmModal.vue';
  import WmsOutboundDetailDrawer from './components/WmsOutboundDetailDrawer.vue';
  import { columns, searchFormSchema } from './WmsOutbound.data';
  import { list } from './WmsOutbound.api';

  const activeTabKey = ref('70');

  const [registerShipModal, { openModal: openShipModal }] = useModal();
  const [registerDetailDrawer, { openDrawer: openDetailDrawer }] = useDrawer();

  const { tableContext } = useListPage({
    tableProps: {
      title: '物流发运出库工作台',
      api: list,
      columns,
      canResize: true,
      formConfig: {
        schemas: searchFormSchema,
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
      beforeFetch: (params) => {
        const p = Object.assign({}, params);
        if (activeTabKey.value !== 'ALL') {
          p.status = Number(activeTabKey.value);
        } else {
          delete p.status;
        }
        return p;
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  function handleTabChange() {
    reload();
  }

  function handleShip(record: Recordable) {
    openShipModal(true, { record });
  }

  function handleViewDetail(record: Recordable) {
    openDetailDrawer(true, { id: record.id });
  }

  function getTableAction(record: Recordable): ActionItem[] {
    const actions: ActionItem[] = [];
    if (record.status === 70) {
      actions.push({
        label: '发运确认',
        onClick: handleShip.bind(null, record),
        auth: 'wms:wms_outbound_order:shipConfirm',
      });
    }
    actions.push({
      label: record.status >= 80 ? '发运详情' : '详情',
      onClick: handleViewDetail.bind(null, record),
    });
    return actions;
  }
</script>
