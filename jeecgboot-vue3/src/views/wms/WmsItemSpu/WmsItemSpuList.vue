<template>
  <div>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <!--插槽:table标题-->
      <template #tableTitle>
        <a-button type="primary" v-auth="'wms:wms_item_spu:add'" @click="handleAdd" preIcon="ant-design:plus-outlined"> 新增</a-button>
        <a-button type="primary" preIcon="ant-design:barcode-outlined" @click="handlePrintBarcode()"> 打印条码</a-button>
        <a-button type="primary" preIcon="ant-design:qrcode-outlined" @click="handlePrintQrcode()"> 生成二维码</a-button>
        <a-button type="primary" v-auth="'wms:wms_item_spu:exportXls'" preIcon="ant-design:export-outlined" @click="onExportXls"> 导出</a-button>
        <j-upload-button type="primary" v-auth="'wms:wms_item_spu:importExcel'" preIcon="ant-design:import-outlined" @click="onImportXls">导入</j-upload-button>
        <a-dropdown v-if="selectedRowKeys.length > 0">
          <template #overlay>
            <a-menu>
              <a-menu-item key="1" @click="batchHandleDelete">
                <Icon icon="ant-design:delete-outlined" />
                删除
              </a-menu-item>
            </a-menu>
          </template>
          <a-button v-auth="'wms:wms_item_spu:deleteBatch'">批量操作
            <Icon icon="mdi:chevron-down" />
          </a-button>
        </a-dropdown>
        <!-- 高级查询 -->
        <super-query :config="superQueryConfig" @search="handleSuperQuery" />
      </template>
      <!--操作栏-->
      <template #action="{ record }">
        <TableAction v-if="record.rowType !== 'sku'" :actions="getTableAction(record)" :dropDownActions="getDropDownAction(record)" />
      </template>
    </BasicTable>
    <!-- 表单区域 -->
    <WmsItemSpuModal @register="registerModal" @success="handleSuccess" />
    <WmsItemBarcodeModal @register="registerBarcodeModal" />
    <WmsItemQrcodeModal @register="registerQrcodeModal" />
  </div>
</template>

<script lang="ts" name="wms-wmsItemSpu" setup>
  import { reactive } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import WmsItemSpuModal from './components/WmsItemSpuModal.vue';
  import WmsItemBarcodeModal from './components/WmsItemBarcodeModal.vue';
  import WmsItemQrcodeModal from './components/WmsItemQrcodeModal.vue';
  import { columns, searchFormSchema, superQuerySchema, toItemTreeRecords } from './WmsItemSpu.data';
  import { collectItemDetails } from './itemCode';
  import { isCode128Text } from './code128';
  import { list, deleteOne, batchDelete, getImportUrl, getExportUrl, wmsItemSkuListApi } from './WmsItemSpu.api';

  const { createMessage } = useMessage();

  const queryParam = reactive<any>({});
  const pageState = { pageNo: 1, pageSize: 10 };
  const [registerModal, { openModal }] = useModal();
  const [registerBarcodeModal, { openModal: openBarcodeModal }] = useModal();
  const [registerQrcodeModal, { openModal: openQrcodeModal }] = useModal();
  const { tableContext, onExportXls, onImportXls } = useListPage({
    tableProps: {
      title: '商品管理',
      api: list,
      columns,
      canResize: true,
      isTreeTable: true,
      showIndexColumn: false,
      rowSelection: {
        getCheckboxProps: (record) => ({
          disabled: record.rowType === 'sku',
          style: record.rowType === 'sku' ? { display: 'none' } : {},
        }),
      },
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
        pageState.pageNo = Number(params.pageNo) || 1;
        pageState.pageSize = Number(params.pageSize) || 10;
        return Object.assign(params, queryParam);
      },
      afterFetch: async (records) => {
        const skuMap = {};
        await Promise.all(
          (records || []).map(async (spu) => {
            const res = await wmsItemSkuListApi({ id: spu.id });
            skuMap[spu.id] = res?.success ? res.result?.records || [] : [];
          })
        );
        return toItemTreeRecords(records, pageState.pageNo, pageState.pageSize, skuMap);
      },
    },
    exportConfig: {
      name: '商品管理',
      url: getExportUrl,
      params: queryParam,
    },
    importConfig: {
      url: getImportUrl,
      success: handleSuccess,
    },
  });

  const [registerTable, { reload, getDataSource }, { rowSelection, selectedRowKeys }] = tableContext;
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
    await deleteOne({ id: record.id }, handleSuccess);
  }

  async function batchHandleDelete() {
    const ids = selectedRowKeys.value.filter((id) => String(id).indexOf('sku-') !== 0);
    await batchDelete({ ids }, handleSuccess);
  }

  function handleSuccess() {
    selectedRowKeys.value = [];
    reload();
  }

  function resolveSpuRecords(record?) {
    if (record) {
      return [record];
    }
    const keys = new Set((selectedRowKeys.value || []).map((id) => String(id)));
    return (getDataSource() || []).filter((row) => row.rowType !== 'sku' && keys.has(String(row.id)));
  }

  function handlePrintBarcode(record?) {
    const details = collectItemDetails(resolveSpuRecords(record));
    if (!details.length) {
      createMessage.warning(record || selectedRowKeys.value.length ? '选中的商品没有商品信息详情' : '请选择商品');
      return;
    }
    const barcodes = details.map((item) => item.barcode).filter((code) => code !== '');
    if (!barcodes.length) {
      createMessage.warning('商品信息详情中没有条码编号');
      return;
    }
    const unsupported = barcodes.filter((code) => !isCode128Text(code));
    if (unsupported.length) {
      createMessage.warning('条码编号只支持数字、字母和常见符号');
      return;
    }
    openBarcodeModal(true, { barcodes });
  }

  function handlePrintQrcode(record?) {
    const details = collectItemDetails(resolveSpuRecords(record));
    if (!details.length) {
      createMessage.warning(record || selectedRowKeys.value.length ? '选中的商品没有商品信息详情' : '请选择商品');
      return;
    }
    openQrcodeModal(true, { details });
  }

  function getTableAction(record) {
    return [
      {
        label: '编辑',
        onClick: handleEdit.bind(null, record),
        auth: 'wms:wms_item_spu:edit',
      },
    ];
  }

  function getDropDownAction(record) {
    return [
      {
        label: '打印条码',
        onClick: handlePrintBarcode.bind(null, record),
      },
      {
        label: '生成二维码',
        onClick: handlePrintQrcode.bind(null, record),
      },
      {
        label: '详情',
        onClick: handleDetail.bind(null, record),
      },
      {
        label: '删除',
        popConfirm: {
          title: '是否确认删除',
          confirm: handleDelete.bind(null, record),
          placement: 'topLeft',
        },
        auth: 'wms:wms_item_spu:delete',
      },
    ];
  }
</script>

<style lang="less" scoped>
  :deep(.ant-picker-range) {
    width: 100%;
  }
</style>
