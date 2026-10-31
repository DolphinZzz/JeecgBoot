<template>
  <div>
    <BasicTable @register="registerTable">
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
    </BasicTable>

    <!-- 弹窗 -->
    <WmsPickConfirmModal @register="registerPickModal" @success="reload" />
    <WmsOutboundDetailDrawer @register="registerDetailDrawer" />
  </div>
</template>

<script lang="ts" name="wms-wmsPick" setup>
  import { reactive } from 'vue';
  import { BasicTable, TableAction, ActionItem } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useDrawer } from '/@/components/Drawer';
  import { useListPage } from '/@/hooks/system/useListPage';
  import WmsPickConfirmModal from './components/WmsPickConfirmModal.vue';
  import WmsOutboundDetailDrawer from './components/WmsOutboundDetailDrawer.vue';
  import { columns, searchFormSchema } from './WmsOutbound.data';
  import { list } from './WmsOutbound.api';

  const queryParam = reactive<any>({
    // 默认展示已分配(20)和拣货中(40)的单据
    status: 20,
  });

  const [registerPickModal, { openModal: openPickModal }] = useModal();
  const [registerDetailDrawer, { openDrawer: openDetailDrawer }] = useDrawer();

  const { tableContext } = useListPage({
    tableProps: {
      title: '出库拣货任务工作台',
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

  function handlePick(record: Recordable) {
    openPickModal(true, { id: record.id });
  }

  function handleViewDetail(record: Recordable) {
    openDetailDrawer(true, { id: record.id });
  }

  function getTableAction(record: Recordable): ActionItem[] {
    return [
      {
        label: '实拣确认',
        onClick: handlePick.bind(null, record),
        auth: 'wms:wms_outbound_order:pickConfirm',
      },
      {
        label: '详情',
        onClick: handleViewDetail.bind(null, record),
      },
    ];
  }
</script>
