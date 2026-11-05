<template>
  <div>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <!-- 表格顶部工具栏 -->
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd">新增</a-button>
        <a-button type="primary" preIcon="ant-design:export-outlined" @click="onExportXls">导出</a-button>
        <j-upload-button type="primary" preIcon="ant-design:import-outlined" @click="onImportXls">导入</j-upload-button>
        <a-dropdown v-if="selectedRowKeys.length > 0">
          <template #overlay>
            <a-menu>
              <a-menu-item key="1" @click="batchHandleDelete">
                <Icon icon="ant-design:delete-outlined" />
                删除
              </a-menu-item>
            </a-menu>
          </template>
          <a-button>
            批量操作
            <Icon icon="mdi:chevron-down" />
          </a-button>
        </a-dropdown>
      </template>

      <!-- 字段渲染 -->
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'isEnabled'">
          <a-switch
            :checked="record.isEnabled === 1"
            checked-children="是"
            un-checked-children="否"
            @change="(checked) => handleToggleEnable(record, checked)"
          />
        </template>
      </template>

      <!-- 操作栏 -->
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" :dropDownActions="getDropDownAction(record)" />
      </template>
    </BasicTable>

    <!-- 新增/修改工序模态框 -->
    <MesProcessStepModal @register="registerModal" @success="handleSuccess" />
  </div>
</template>

<script lang="ts" name="mes-process-step" setup>
  import { reactive } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { Icon } from '/@/components/Icon';
  import MesProcessStepModal from './components/MesProcessStepModal.vue';
  import { columns, searchFormSchema } from './MesProcessStep.data';
  import { list, deleteOne, batchDelete, saveOrUpdate, getImportUrl, getExportUrl } from './MesProcessStep.api';

  const queryParam = reactive<any>({});
  const [registerModal, { openModal }] = useModal();

  const { tableContext, onExportXls, onImportXls } = useListPage({
    tableProps: {
      title: '工序设置',
      api: list,
      columns,
      canResize: true,
      formConfig: {
        schemas: searchFormSchema,
        autoSubmitOnEnter: true,
        showAdvancedButton: false,
      },
      actionColumn: {
        width: 140,
        fixed: 'right',
      },
      beforeFetch: (params) => {
        return Object.assign(params, queryParam);
      },
    },
    exportConfig: {
      name: '生产工序',
      url: getExportUrl,
      params: queryParam,
    },
    importConfig: {
      url: getImportUrl,
      success: handleSuccess,
    },
  });

  const [registerTable, { reload }, { rowSelection, selectedRowKeys }] = tableContext;

  function handleAdd() {
    openModal(true, {
      isUpdate: false,
      isDetail: false,
    });
  }

  function handleEdit(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
      isDetail: false,
    });
  }

  function handleDetail(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
      isDetail: true,
    });
  }

  async function handleToggleEnable(record: Recordable, checked: boolean) {
    const newStatus = checked ? 1 : 2;
    await saveOrUpdate({ id: record.id, isEnabled: newStatus }, true, true);
    reload();
  }

  async function handleDelete(record: Recordable) {
    await deleteOne({ id: record.id }, handleSuccess);
  }

  async function batchHandleDelete() {
    await batchDelete({ ids: selectedRowKeys.value.join(',') }, handleSuccess);
  }

  function handleSuccess() {
    selectedRowKeys.value = [];
    reload();
  }

  function getTableAction(record: Recordable) {
    return [
      {
        label: '修改',
        onClick: handleEdit.bind(null, record),
      },
    ];
  }

  function getDropDownAction(record: Recordable) {
    return [
      {
        label: '详情',
        onClick: handleDetail.bind(null, record),
      },
      {
        label: '删除',
        popConfirm: {
          title: '是否确认删除该工序？',
          confirm: handleDelete.bind(null, record),
          placement: 'topLeft',
        },
      },
    ];
  }
</script>
