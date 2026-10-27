<template>
  <div>
    <BasicTable @register="registerTable" :rowSelection="rowSelection" :expandedRowKeys="expandedRowKeys" @expand="handleExpand" @fetch-success="onFetchSuccess">
      <template #tableTitle>
        <a-button type="primary" v-auth="'wms:wms_item_category:add'" @click="handleCreate" preIcon="ant-design:plus-outlined"> 新增</a-button>
        <a-button type="primary" v-auth="'wms:wms_item_category:exportXls'" preIcon="ant-design:export-outlined" @click="onExportXls"> 导出</a-button>
        <j-upload-button type="primary" v-auth="'wms:wms_item_category:importExcel'" preIcon="ant-design:import-outlined" @click="onImportXls">导入</j-upload-button>
        <a-dropdown v-if="selectedRowKeys.length > 0">
          <template #overlay>
            <a-menu>
              <a-menu-item key="1" @click="batchHandleDelete">
                <Icon icon="ant-design:delete-outlined" />
                删除
              </a-menu-item>
            </a-menu>
          </template>
          <a-button v-auth="'wms:wms_item_category:deleteBatch'">批量操作
            <Icon icon="mdi:chevron-down" />
          </a-button>
        </a-dropdown>
        <super-query :config="superQueryConfig" @search="handleSuperQuery" />
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" :dropDownActions="getDropDownAction(record)" />
      </template>
      <template v-slot:bodyCell="{ column, record, index, text }">
      </template>
    </BasicTable>
    <WmsItemCategoryModal @register="registerModal" @success="handleSuccess" />
  </div>
</template>

<script lang="ts" name="wms-wmsItemCategory" setup>
  import { ref, reactive, unref } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useListPage } from '/@/hooks/system/useListPage';
  import WmsItemCategoryModal from './components/WmsItemCategoryModal.vue';
  import { columns, searchFormSchema, superQuerySchema } from './WmsItemCategory.data';
  import { list, getChildList, getChildListBatch, deleteOne, batchDelete, getExportUrl, getImportUrl } from './WmsItemCategory.api';

  const queryParam = reactive<any>({});
  const expandedRowKeys = ref<Array<string | number>>([]);
  const [registerModal, { openModal }] = useModal();

  const { tableContext, onExportXls, onImportXls } = useListPage({
    tableProps: {
      title: '商品类别',
      api: list,
      columns,
      canResize: true,
      isTreeTable: true,
      formConfig: {
        schemas: searchFormSchema,
        autoSubmitOnEnter: true,
        showAdvancedButton: true,
        fieldMapToNumber: [],
        fieldMapToTime: [],
      },
      actionColumn: {
        width: 240,
        fixed: 'right',
      },
      beforeFetch: (params) => {
        params['hasQuery'] = 'true';
        return Object.assign(params, queryParam);
      },
    },
    exportConfig: {
      name: '商品类别',
      url: getExportUrl,
      params: queryParam,
    },
    importConfig: {
      url: getImportUrl,
      success: importSuccess,
    },
  });

  const [registerTable, { reload, updateTableDataRecord, findTableDataRecord, getDataSource }, { rowSelection, selectedRowKeys }] = tableContext;
  const superQueryConfig = reactive(superQuerySchema);

  function onFetchSuccess(result) {
    getDataByResult(result.items) && loadDataByExpandedRows();
  }

  async function loadDataByExpandedRows() {
    if (unref(expandedRowKeys).length > 0) {
      const res = await getChildListBatch({ parentIds: unref(expandedRowKeys).join(',') });
      if (res.success && res.result.records.length > 0) {
        const records = res.result.records;
        const listMap = new Map();
        for (let item of records) {
          const parentId = item['parentId'];
          if (unref(expandedRowKeys).map(String).includes(String(parentId))) {
            let mapList = listMap.get(parentId);
            if (mapList == null) {
              mapList = [];
            }
            mapList.push(item);
            listMap.set(parentId, mapList);
          }
        }
        const fn = (list) => {
          if (list) {
            list.forEach((data) => {
              if (unref(expandedRowKeys).map(String).includes(String(data.id))) {
                data.children = getDataByResult(listMap.get(data.id));
                fn(data.children);
              }
            });
          }
        };
        fn(getDataSource());
      }
    }
  }

  function getDataByResult(result) {
    if (result && result.length > 0) {
      return result.map((item) => {
        if (item['hasChild'] == '1') {
          const loadChild = { id: item.id + '_loadChild', name: 'loading...', isLoading: true };
          item.children = [loadChild];
        }
        return item;
      });
    }
  }

  async function handleExpand(expanded, record) {
    if (expanded) {
      expandedRowKeys.value.push(record.id);
      if (record.children && record.children.length > 0 && !!record.children[0].isLoading) {
        let result = await getChildList({ parentId: record.id });
        result = result.records ? result.records : result;
        if (result && result.length > 0) {
          record.children = getDataByResult(result);
        } else {
          record.children = null;
          record.hasChild = '0';
        }
      }
    } else {
      const keyIndex = expandedRowKeys.value.findIndex((item) => String(item) === String(record.id));
      if (keyIndex >= 0) {
        expandedRowKeys.value.splice(keyIndex, 1);
      }
    }
  }

  async function expandTreeNode(key) {
    const rowKey = Number(key);
    const record: any = findTableDataRecord(rowKey);
    if (!record) {
      return;
    }
    expandedRowKeys.value.push(rowKey);
    let result = await getChildList({ parentId: rowKey });
    result = result && result.records ? result.records : result;
    if (result && result.length > 0) {
      record.children = getDataByResult(result);
    } else {
      record.children = null;
      record.hasChild = '0';
    }
    updateTableDataRecord(rowKey, record);
  }

  function handleSuperQuery(params) {
    Object.keys(params).map((k) => {
      queryParam[k] = params[k];
    });
    reload();
  }

  function handleCreate() {
    openModal(true, { isUpdate: false });
  }

  function handleAddChild(record) {
    openModal(true, { record: { parentId: record.id }, isUpdate: false });
  }

  function handleEdit(record) {
    openModal(true, { record, isUpdate: true });
  }

  function handleDetail(record) {
    openModal(true, { record, isUpdate: true, hideFooter: true });
  }

  async function handleDelete(record) {
    await deleteOne({ id: record.id }, importSuccess);
  }

  async function batchHandleDelete() {
    const ids = selectedRowKeys.value.filter((item) => !String(item).includes('loadChild'));
    await batchDelete({ ids }, importSuccess);
  }

  function importSuccess() {
    (selectedRowKeys.value = []) && reload();
  }

  async function handleSuccess({ isUpdate, values, expandedArr, changeParent }) {
    if (isUpdate) {
      if (changeParent) {
        reload();
      } else {
        const params: Recordable = { id: values.id, pageSize: 1, pageNo: 1 };
        if (values['parentId']) {
          params.parentId = values['parentId'];
        }
        const data = await list(params);
        if (data && data.records && data.records.length > 0) {
          const row = data.records[0];
          getDataByResult([row]);
          updateTableDataRecord(values.id, row);
        } else {
          updateTableDataRecord(values.id, values);
        }
      }
    } else {
      if (!values['id'] || !values['parentId']) {
        reload();
      } else {
        expandedRowKeys.value = [];
        for (const key of unref(expandedArr)) {
          await expandTreeNode(key);
        }
      }
    }
  }

  function getTableAction(record) {
    return [
      { label: '编辑', onClick: handleEdit.bind(null, record), auth: 'wms:wms_item_category:edit' },
      { label: '添加下级', onClick: handleAddChild.bind(null, record), auth: 'wms:wms_item_category:add' },
    ];
  }

  function getDropDownAction(record) {
    return [
      { label: '详情', onClick: handleDetail.bind(null, record) },
      {
        label: '删除',
        popConfirm: { title: '确定删除吗?', confirm: handleDelete.bind(null, record), placement: 'topLeft' },
        auth: 'wms:wms_item_category:delete',
      },
    ];
  }
</script>

<style lang="less" scoped>
  :deep(.ant-picker),
  :deep(.ant-input-number) {
    width: 100%;
  }
</style>
