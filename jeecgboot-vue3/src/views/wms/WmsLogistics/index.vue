<template>
  <div>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button type="primary" v-auth="'wms:logistics:add'" @click="handleAdd" preIcon="ant-design:plus-outlined">
          新增承运商
        </a-button>
        <a-dropdown v-if="selectedRowKeys.length > 0">
          <template #overlay>
            <a-menu>
              <a-menu-item key="1" @click="batchHandleDelete">
                <Icon icon="ant-design:delete-outlined" />
                批量删除
              </a-menu-item>
            </a-menu>
          </template>
          <a-button v-auth="'wms:logistics:delete'">
            批量操作
            <Icon icon="mdi:chevron-down" />
          </a-button>
        </a-dropdown>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
    </BasicTable>
    <WmsLogisticsModal @register="registerModal" @success="reload" />
  </div>
</template>

<script lang="ts" name="wms-WmsLogistics" setup>
  import { reactive } from 'vue';
  import { BasicTable, TableAction, ActionItem } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useListPage } from '/@/hooks/system/useListPage';
  import Icon from '/@/components/Icon/src/Icon.vue';
  import WmsLogisticsModal from './components/WmsLogisticsModal.vue';
  import { columns, searchFormSchema } from './WmsLogistics.data';
  import { list, deleteOne, batchDelete } from './WmsLogistics.api';

  const queryParam = reactive<any>({});
  const [registerModal, { openModal }] = useModal();

  const { tableContext } = useListPage({
    tableProps: {
      title: '物流承运商管理',
      api: list,
      columns,
      canResize: true,
      formConfig: {
        schemas: searchFormSchema,
        autoSubmitOnEnter: true,
        showAdvancedButton: false,
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

  const [registerTable, { reload }, { rowSelection, selectedRowKeys }] = tableContext;

  function handleAdd() {
    openModal(true, {
      isUpdate: false,
    });
  }

  function handleEdit(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
    });
  }

  function handleDelete(record: Recordable) {
    deleteOne({ id: record.id }, reload);
  }

  function batchHandleDelete() {
    batchDelete({ ids: selectedRowKeys.value.join(',') }, reload);
  }

  function getTableAction(record: Recordable): ActionItem[] {
    return [
      {
        label: '编辑',
        onClick: handleEdit.bind(null, record),
        auth: 'wms:logistics:edit',
      },
      {
        label: '删除',
        color: 'error',
        auth: 'wms:logistics:delete',
        popConfirm: {
          title: '是否确认删除该物流承运商？',
          confirm: handleDelete.bind(null, record),
        },
      },
    ];
  }
</script>
