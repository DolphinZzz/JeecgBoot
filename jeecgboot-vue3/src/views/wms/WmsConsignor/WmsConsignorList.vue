<template>
  <div>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button type="primary" v-auth="'wms:wms_consignor:add'" @click="handleAdd" preIcon="ant-design:plus-outlined"> 新增</a-button>
        <a-button type="primary" v-auth="'wms:wms_consignor:exportXls'" preIcon="ant-design:export-outlined" @click="onExportXls"> 导出</a-button>
        <j-upload-button type="primary" v-auth="'wms:wms_consignor:importExcel'" preIcon="ant-design:import-outlined" @click="onImportXls"
          >导入</j-upload-button
        >
        <a-dropdown v-if="selectedRowKeys.length > 0">
          <template #overlay>
            <a-menu>
              <a-menu-item key="1" @click="batchHandleDelete">
                <Icon icon="ant-design:delete-outlined" />
                删除
              </a-menu-item>
            </a-menu>
          </template>
          <a-button v-auth="'wms:wms_consignor:deleteBatch'"
            >批量操作
            <Icon icon="mdi:chevron-down" />
          </a-button>
        </a-dropdown>
        <super-query :config="superQueryConfig" @search="handleSuperQuery" />
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" :dropDownActions="getDropDownAction(record)" />
      </template>
    </BasicTable>
    <WmsConsignorModal @register="registerModal" @success="handleSuccess" />
  </div>
</template>

<script lang="ts" name="wms-wmsConsignor" setup>
  import { reactive } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import WmsConsignorModal from './components/WmsConsignorModal.vue';
  import { columns, searchFormSchema, superQuerySchema } from './WmsConsignor.data';
  import { list, deleteOne, batchDelete, getImportUrl, getExportUrl } from './WmsConsignor.api';

  const { createMessage } = useMessage();
  const queryParam = reactive<any>({});
  const [registerModal, { openModal }] = useModal();

  const { tableContext, onExportXls, onImportXls } = useListPage({
    tableProps: {
      title: '货主信息',
      api: list,
      columns,
      canResize: true,
      formConfig: {
        schemas: searchFormSchema,
        autoSubmitOnEnter: true,
        showAdvancedButton: true,
        fieldMapToTime: [],
        fieldMapToNumber: [],
      },
      actionColumn: {
        title: '操作',
        dataIndex: 'action',
        width: 120,
        fixed: 'right',
        slots: { customRender: 'action' },
      },
      beforeFetch: (params) => {
        return Object.assign(params, queryParam);
      },
    },
    exportConfig: {
      name: '货主信息',
      url: getExportUrl,
      params: queryParam,
    },
    importConfig: {
      url: getImportUrl,
      success: handleSuccess,
    },
  });

  const [registerTable, { reload }, { rowSelection, selectedRowKeys }] = tableContext;

  const superQueryConfig = reactive(superQuerySchema);

  function handleSuperQuery(params) {
    Object.keys(params).map((k) => {
      queryParam[k] = params[k];
    });
    reload();
  }

  function handleAdd() {
    openModal(true, {
      isUpdate: false,
      showFooter: true,
    });
  }

  function handleEdit(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
      showFooter: true,
    });
  }

  function handleDetail(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
      showFooter: false,
    });
  }

  async function handleDelete(record) {
    try {
      await deleteOne({ id: record.id }, handleSuccess);
      createMessage.success('删除成功');
    } catch (e: any) {
      createMessage.error(e?.message || '删除失败');
    }
  }

  async function batchHandleDelete() {
    try {
      await batchDelete({ ids: selectedRowKeys.value }, handleSuccess);
    } catch (e: any) {
      createMessage.error(e?.message || '批量删除失败');
    }
  }

  function handleSuccess() {
    (selectedRowKeys.value = []) && reload();
  }

  function getTableAction(record) {
    return [
      {
        label: '编辑',
        onClick: handleEdit.bind(null, record),
        auth: 'wms:wms_consignor:edit',
      },
    ];
  }

  function getDropDownAction(record) {
    return [
      {
        label: '详情',
        onClick: handleDetail.bind(null, record),
      },
      {
        label: '删除',
        popConfirm: {
          title: '是否确认删除？已被业务数据引用的货主不能删除，只能停用。',
          confirm: handleDelete.bind(null, record),
          placement: 'topLeft',
        },
        auth: 'wms:wms_consignor:delete',
      },
    ];
  }
</script>

<style lang="less" scoped>
  :deep(.ant-picker-range) {
    width: 100%;
  }
</style>
