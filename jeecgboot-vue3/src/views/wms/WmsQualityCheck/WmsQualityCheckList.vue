<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <span class="tip">质检单由「入库单-提交质检」自动生成；收货清点检出破损时也会在这里生成来源为「收货破损」的退货记录。不合格品 / 破损件需放置于仓库退货货架区（RETURN）并联络供应商退还，合格品走正常上架。</span>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" :dropDownActions="getDropDownAction(record)" />
      </template>
    </BasicTable>

    <WmsQualityCheckSubmitModal @register="registerSubmitModal" @success="handleSuccess" />
    <WmsQualityCheckReturnModal @register="registerReturnModal" @success="handleSuccess" />
    <WmsQualityCheckShipModal @register="registerShipModal" @success="handleSuccess" />
  </div>
</template>

<script lang="ts" name="wms-wmsQualityCheck" setup>
  import { reactive } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useListPage } from '/@/hooks/system/useListPage';
  import WmsQualityCheckSubmitModal from './components/WmsQualityCheckSubmitModal.vue';
  import WmsQualityCheckReturnModal from './components/WmsQualityCheckReturnModal.vue';
  import WmsQualityCheckShipModal from './components/WmsQualityCheckShipModal.vue';
  import { columns, searchFormSchema, isDecided } from './WmsQualityCheck.data';
  import { list } from './WmsQualityCheck.api';

  const queryParam = reactive<any>({});
  const [registerSubmitModal, { openModal: openSubmitModal }] = useModal();
  const [registerReturnModal, { openModal: openReturnModal }] = useModal();
  const [registerShipModal, { openModal: openShipModal }] = useModal();

  const { tableContext } = useListPage({
    tableProps: {
      title: '质检单',
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
        width: 200,
        fixed: 'right',
        slots: { customRender: 'action' },
      },
      beforeFetch: (params) => {
        return Object.assign(params, queryParam);
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  function handleSuccess() {
    reload();
  }

  function handleSubmit(record) {
    openSubmitModal(true, { record });
  }

  function handleReturnModal(record) {
    openReturnModal(true, { record });
  }

  function handleShipModal(record) {
    openShipModal(true, { record });
  }

  function getTableAction(record) {
    const actions: any[] = [];

    // 1. 录入结论（未出结论时可用）
    if (!isDecided(Number(record.status))) {
      actions.push({
        label: '录入结论',
        auth: 'wms:wms_quality_check:submit',
        onClick: handleSubmit.bind(null, record),
      });
    }

    // 2. 退货处置（有不合格品且待退货安置）
    if (Number(record.failCount ?? 0) > 0 && Number(record.returnStatus ?? 0) === 1) {
      actions.push({
        label: '退货处置',
        color: 'warning',
        auth: 'wms:wms_quality_check:return',
        onClick: handleReturnModal.bind(null, record),
      });
    }

    // 3. 确认退还供应商（已安置退货区待提货发运）
    if (Number(record.returnStatus ?? 0) === 2) {
      actions.push({
        label: '退还供应商',
        color: 'success',
        auth: 'wms:wms_quality_check:shipReturn',
        onClick: handleShipModal.bind(null, record),
      });
    }

    return actions;
  }

  function getDropDownAction(record) {
    const dropDowns: any[] = [];
    // 已入退货区时，可变更退货货架库位
    if (Number(record.failCount ?? 0) > 0 && Number(record.returnStatus ?? 0) === 2) {
      dropDowns.push({
        label: '变更退货库位',
        auth: 'wms:wms_quality_check:return',
        onClick: handleReturnModal.bind(null, record),
      });
    }
    return dropDowns;
  }
</script>

<style lang="less" scoped>
  .tip {
    color: #8c8c8c;
    font-size: 13px;
  }
</style>

