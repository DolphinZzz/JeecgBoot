<template>
  <div>
    <!--引用表格-->
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <!--插槽:table标题-->
      <template #tableTitle>
        <a-button type="primary" v-auth="'wms:wms_warehouse_area_loca:add'" @click="handleAdd" preIcon="ant-design:plus-outlined"> 新增</a-button>
        <a-button
          type="primary"
          v-auth="'wms:wms_warehouse_area_loca:recalcLocaScore'"
          preIcon="ant-design:calculator-outlined"
          @click="handleOpenRecalcModal"
          style="background-color: #52c41a; border-color: #52c41a"
        >
          重算库位等级</a-button
        >
        <a-button type="primary" v-auth="'wms:wms_warehouse_area_loca:exportXls'" preIcon="ant-design:export-outlined" @click="onExportXls">
          导出</a-button
        >
        <j-upload-button type="primary" v-auth="'wms:wms_warehouse_area_loca:importExcel'" preIcon="ant-design:import-outlined" @click="onImportXls"
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
          <a-button v-auth="'wms:wms_warehouse_area_loca:deleteBatch'"
            >批量操作
            <Icon icon="mdi:chevron-down" />
          </a-button>
        </a-dropdown>
        <!-- 高级查询 -->
        <super-query :config="superQueryConfig" @search="handleSuperQuery" />
      </template>
      <!--操作栏-->
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" :dropDownActions="getDropDownAction(record)" />
      </template>
    </BasicTable>

    <!-- 重算库位等级弹窗 -->
    <a-modal v-model:visible="recalcModalVisible" title="重算库位等级 (ABC分类)" :confirm-loading="recalcLoading" @ok="handleDoRecalc">
      <div style="margin-bottom: 16px; color: #555; line-height: 1.6">
        <p>系统将依据公式计算存储区与拣货区库位分值并回写 ABC 分类：</p>
        <p style="font-family: monospace; background: #f5f5f5; padding: 6px 10px; border-radius: 4px"> S_i = w_d × (1 - d_i / d_max) + w_e × E_i </p>
        <p>包含距离因子与人体工学系数（黄金层1.0，次优层0.6，不便层0.2）。</p>
      </div>
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="目标仓库" required>
          <j-dict-select-tag
            v-model:value="recalcWarehouseId"
            dictCode="wms_warehouse,name,id"
            :stringToNumber="true"
            placeholder="请选择要计算的仓库"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 表单区域 -->
    <WmsWarehouseAreaLocaModal @register="registerModal" @success="handleSuccess" />
  </div>
</template>

<script lang="ts" name="wms-wmsWarehouseAreaLoca" setup>
  import { ref, reactive } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import JDictSelectTag from '/@/components/Form/src/jeecg/components/JDictSelectTag.vue';
  import WmsWarehouseAreaLocaModal from './components/WmsWarehouseAreaLocaModal.vue';
  import { columns, searchFormSchema, superQuerySchema } from './WmsWarehouseAreaLoca.data';
  import { list, deleteOne, batchDelete, getImportUrl, getExportUrl, recalcLocaScore } from './WmsWarehouseAreaLoca.api';

  const { createMessage } = useMessage();
  const queryParam = reactive<any>({});
  const recalcModalVisible = ref(false);
  const recalcLoading = ref(false);
  const recalcWarehouseId = ref<number | undefined>(undefined);

  // 注册 modal
  const [registerModal, { openModal }] = useModal();
  // 注册 table
  const { tableContext, onExportXls, onImportXls } = useListPage({
    tableProps: {
      title: '库位管理',
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
      name: '库位管理',
      url: getExportUrl,
      params: queryParam,
    },
    importConfig: {
      url: getImportUrl,
      success: handleSuccess,
    },
  });

  const [registerTable, { reload }, { rowSelection, selectedRowKeys }] = tableContext;

  // 高级查询配置
  const superQueryConfig = reactive(superQuerySchema);

  /**
   * 打开重算弹窗
   */
  function handleOpenRecalcModal() {
    recalcWarehouseId.value = queryParam.warehouseId || undefined;
    recalcModalVisible.value = true;
  }

  /**
   * 执行重算
   */
  async function handleDoRecalc() {
    if (!recalcWarehouseId.value) {
      createMessage.warning('请选择目标仓库！');
      return;
    }
    try {
      recalcLoading.value = true;
      const res = await recalcLocaScore(recalcWarehouseId.value);
      createMessage.success(res?.message || '重算库位等级成功！');
      recalcModalVisible.value = false;
      reload();
    } catch (e: any) {
      createMessage.error(e?.message || '重算失败');
    } finally {
      recalcLoading.value = false;
    }
  }

  /**
   * 高级查询事件
   */
  function handleSuperQuery(params) {
    Object.keys(params).map((k) => {
      queryParam[k] = params[k];
    });
    reload();
  }

  /**
   * 新增事件
   */
  function handleAdd() {
    openModal(true, {
      isUpdate: false,
      showFooter: true,
    });
  }

  /**
   * 编辑事件
   */
  function handleEdit(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
      showFooter: true,
    });
  }

  /**
   * 详情
   */
  function handleDetail(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
      showFooter: false,
    });
  }

  /**
   * 删除事件
   */
  async function handleDelete(record) {
    await deleteOne({ id: record.id }, handleSuccess);
  }

  /**
   * 批量删除事件
   */
  async function batchHandleDelete() {
    await batchDelete({ ids: selectedRowKeys.value }, handleSuccess);
  }

  /**
   * 成功回调
   */
  function handleSuccess() {
    (selectedRowKeys.value = []) && reload();
  }

  /**
   * 操作栏
   */
  function getTableAction(record) {
    return [
      {
        label: '编辑',
        onClick: handleEdit.bind(null, record),
        auth: 'wms:wms_warehouse_area_loca:edit',
      },
    ];
  }

  /**
   * 下拉操作栏
   */
  function getDropDownAction(record) {
    return [
      {
        label: '详情',
        onClick: handleDetail.bind(null, record),
      },
      {
        label: '删除',
        popConfirm: {
          title: '是否确认删除该库位？',
          confirm: handleDelete.bind(null, record),
          placement: 'topLeft',
        },
        auth: 'wms:wms_warehouse_area_loca:delete',
      },
    ];
  }
</script>

<style lang="less" scoped>
  :deep(.ant-picker-range) {
    width: 100%;
  }
</style>
