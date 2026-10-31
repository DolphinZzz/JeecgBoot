<template>
  <div>
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
  import { reactive } from 'vue';
  import { BasicTable, TableAction, ActionItem } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useDrawer } from '/@/components/Drawer';
  import { useListPage } from '/@/hooks/system/useListPage';
  import WmsShipConfirmModal from './components/WmsShipConfirmModal.vue';
  import WmsOutboundDetailDrawer from './components/WmsOutboundDetailDrawer.vue';
  import { columns, searchFormSchema } from './WmsOutbound.data';
  import { list } from './WmsOutbound.api';

  const queryParam = reactive<any>({
    status: 70, // 默认展示已打包待发货
  });

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
        return Object.assign(params, queryParam);
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  function handleShip(record: Recordable) {
    openShipModal(true, { record });
  }

  function handleViewDetail(record: Recordable) {
    openDetailDrawer(true, { id: record.id });
  }

  function getTableAction(record: Recordable): ActionItem[] {
    return [
      {
        label: '发运确认',
        onClick: handleShip.bind(null, record),
        auth: 'wms:wms_outbound_order:shipConfirm',
      },
      {
        label: '详情',
        onClick: handleViewDetail.bind(null, record),
      },
    ];
  }
</script>
