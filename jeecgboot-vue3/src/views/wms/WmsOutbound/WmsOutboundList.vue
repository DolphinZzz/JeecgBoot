<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button
          type="primary"
          v-auth="'wms:wms_outbound_order:add'"
          preIcon="ant-design:plus-outlined"
          @click="handleCreate"
        >
          新建出库单
        </a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" :dropDownActions="getDropDownAction(record)" />
      </template>
    </BasicTable>

    <!-- 弹窗与抽屉 -->
    <WmsOutboundCreateModal @register="registerCreateModal" @success="reload" />
    <WmsOutboundAllocateModal @register="registerAllocateModal" @success="reload" />
    <WmsOutboundCancelModal @register="registerCancelModal" @success="reload" />
    <WmsOutboundDetailDrawer @register="registerDetailDrawer" />
    <WmsPickConfirmModal @register="registerPickModal" @success="reload" />
    <WmsReviewConfirmModal @register="registerReviewModal" @success="reload" />
    <WmsPackageWeightModal @register="registerPackageModal" @success="reload" />
    <WmsShipConfirmModal @register="registerShipModal" @success="reload" />
  </div>
</template>

<script lang="ts" name="wms-wmsOutbound" setup>
  import { reactive } from 'vue';
  import { BasicTable, TableAction, ActionItem } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useDrawer } from '/@/components/Drawer';
  import { useListPage } from '/@/hooks/system/useListPage';
  import WmsOutboundCreateModal from './components/WmsOutboundCreateModal.vue';
  import WmsOutboundAllocateModal from './components/WmsOutboundAllocateModal.vue';
  import WmsOutboundCancelModal from './components/WmsOutboundCancelModal.vue';
  import WmsOutboundDetailDrawer from './components/WmsOutboundDetailDrawer.vue';
  import WmsPickConfirmModal from './components/WmsPickConfirmModal.vue';
  import WmsReviewConfirmModal from './components/WmsReviewConfirmModal.vue';
  import WmsPackageWeightModal from './components/WmsPackageWeightModal.vue';
  import WmsShipConfirmModal from './components/WmsShipConfirmModal.vue';
  import { columns, searchFormSchema } from './WmsOutbound.data';
  import { list } from './WmsOutbound.api';

  const queryParam = reactive<any>({});

  const [registerCreateModal, { openModal: openCreateModal }] = useModal();
  const [registerAllocateModal, { openModal: openAllocateModal }] = useModal();
  const [registerCancelModal, { openModal: openCancelModal }] = useModal();
  const [registerPickModal, { openModal: openPickModal }] = useModal();
  const [registerReviewModal, { openModal: openReviewModal }] = useModal();
  const [registerPackageModal, { openModal: openPackageModal }] = useModal();
  const [registerShipModal, { openModal: openShipModal }] = useModal();
  const [registerDetailDrawer, { openDrawer: openDetailDrawer }] = useDrawer();

  const { tableContext } = useListPage({
    tableProps: {
      title: '出库单管理',
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
        width: 180,
        fixed: 'right',
        slots: { customRender: 'action' },
      },
      beforeFetch: (params) => {
        return Object.assign(params, queryParam);
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  function handleCreate() {
    openCreateModal(true);
  }

  function handleViewDetail(record: Recordable) {
    openDetailDrawer(true, { id: record.id });
  }

  function handleAllocate(record: Recordable) {
    openAllocateModal(true, { id: record.id });
  }

  function handlePick(record: Recordable) {
    openPickModal(true, { id: record.id });
  }

  function handleReview(record: Recordable) {
    openReviewModal(true, { record });
  }

  function handlePackage(record: Recordable) {
    openPackageModal(true, { record });
  }

  function handleShip(record: Recordable) {
    openShipModal(true, { record });
  }

  function handleCancel(record: Recordable) {
    openCancelModal(true, { record });
  }

  function getTableAction(record: Recordable): ActionItem[] {
    const actions: ActionItem[] = [
      {
        label: '详情',
        onClick: handleViewDetail.bind(null, record),
      },
    ];

    // 状态流转主操作
    if (record.status === 10 || record.status === 30) {
      actions.push({
        label: '分配库存',
        onClick: handleAllocate.bind(null, record),
        auth: 'wms:wms_outbound_order:allocate',
      });
    } else if (record.status === 20 || record.status === 40) {
      actions.push({
        label: '实拣确认',
        onClick: handlePick.bind(null, record),
        auth: 'wms:wms_outbound_order:pickConfirm',
      });
    } else if (record.status === 50) {
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
    } else if (record.status === 70) {
      actions.push({
        label: '发运确认',
        onClick: handleShip.bind(null, record),
        auth: 'wms:wms_outbound_order:shipConfirm',
      });
    }

    return actions;
  }

  function getDropDownAction(record: Recordable): ActionItem[] {
    const drops: ActionItem[] = [];

    // 未发货且未取消前允许取消
    if (record.status < 80 && record.status !== 91) {
      drops.push({
        label: '取消出库',
        color: 'error',
        onClick: handleCancel.bind(null, record),
        auth: 'wms:wms_outbound_order:cancel',
      });
    }

    return drops;
  }
</script>
