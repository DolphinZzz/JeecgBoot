<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <span class="tip">质检单由「入库单-提交质检」自动生成，本页只做查询与录入结论；全部明细出结论后入库单自动流转到「质检完成待上架」。</span>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
    </BasicTable>

    <WmsQualityCheckSubmitModal @register="registerSubmitModal" @success="handleSuccess" />
  </div>
</template>

<script lang="ts" name="wms-wmsQualityCheck" setup>
  import { reactive } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useListPage } from '/@/hooks/system/useListPage';
  import WmsQualityCheckSubmitModal from './components/WmsQualityCheckSubmitModal.vue';
  import { columns, searchFormSchema, isDecided } from './WmsQualityCheck.data';
  import { list } from './WmsQualityCheck.api';

  const queryParam = reactive<any>({});
  const [registerSubmitModal, { openModal: openSubmitModal }] = useModal();

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
        width: 130,
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

  function getTableAction(record) {
    // 已出结论（合格 / 不合格 / 部分合格）不能重复提交
    return [
      {
        label: '录入结论',
        auth: 'wms:wms_quality_check:submit',
        disabled: isDecided(Number(record.status)),
        onClick: handleSubmit.bind(null, record),
      },
    ];
  }
</script>

<style lang="less" scoped>
  .tip {
    color: #8c8c8c;
    font-size: 13px;
  }
</style>
