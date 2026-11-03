<template>
  <div>
    <a-tabs v-model:activeKey="activeTabKey" @change="handleTabChange" style="background: #fff; padding: 12px 16px 0">
      <a-tab-pane key="50" tab="待出库复核 (50)" />
      <a-tab-pane key="60" tab="待打包称重 (60)" />
      <a-tab-pane key="ALL" tab="全部单据" />
    </a-tabs>

    <BasicTable @register="registerTable">
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
    </BasicTable>

    <!-- 弹窗 -->
    <WmsReviewConfirmModal @register="registerReviewModal" @success="reload" />
    <WmsPackageWeightModal @register="registerPackageModal" @success="reload" />
    <WmsOutboundDetailDrawer @register="registerDetailDrawer" />
  </div>
</template>

<script lang="ts" name="wms-wmsReviewPackage" setup>
  import { ref } from 'vue';
  import { BasicTable, TableAction, ActionItem } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useDrawer } from '/@/components/Drawer';
  import { useListPage } from '/@/hooks/system/useListPage';
  import WmsReviewConfirmModal from './components/WmsReviewConfirmModal.vue';
  import WmsPackageWeightModal from './components/WmsPackageWeightModal.vue';
  import WmsOutboundDetailDrawer from './components/WmsOutboundDetailDrawer.vue';
  import { columns, searchFormSchema } from './WmsOutbound.data';
  import { list } from './WmsOutbound.api';

  const activeTabKey = ref('50');

  const [registerReviewModal, { openModal: openReviewModal }] = useModal();
  const [registerPackageModal, { openModal: openPackageModal }] = useModal();
  const [registerDetailDrawer, { openDrawer: openDetailDrawer }] = useDrawer();

  const { tableContext } = useListPage({
    tableProps: {
      title: '出库复核与打包称重工作台',
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
        width: 150,
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

  function handleReview(record: Recordable) {
    openReviewModal(true, { record });
  }

  function handlePackage(record: Recordable) {
    openPackageModal(true, { record });
  }

  function handleViewDetail(record: Recordable) {
    openDetailDrawer(true, { id: record.id });
  }

  function getTableAction(record: Recordable): ActionItem[] {
    const actions: ActionItem[] = [];

    if (record.status === 50) {
      actions.push({
        label: '出库复核',
        onClick: handleReview.bind(null, record),
        auth: 'wms:wms_outbound_order:reviewConfirm',
      });
    } else if (record.status === 60) {
      actions.push({
        label: '打包称重',
        onClick: handlePackage.bind(null, record),
        auth: 'wms:wms_outbound_order:packageWeight',
      });
    }

    actions.push({
      label: '详情',
      onClick: handleViewDetail.bind(null, record),
    });

    return actions;
  }
</script>
