<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button
          type="primary"
          v-auth="'wms:wms_warehouse_operations:generateReplenish'"
          preIcon="ant-design:retweet-outlined"
          @click="handleOpenGenerate"
        >
          生成补货单</a-button
        >
        <span class="tip">拣货区可用库存低于 SKU 补货下限时，按「上限 - 拣货区库存」补货，并从存储区按 FEFO 取货。</span>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" :dropDownActions="getDropDownAction(record)" />
      </template>
    </BasicTable>

    <!-- 生成补货单 -->
    <a-modal v-model:visible="generateVisible" title="生成补货单" :confirm-loading="generateLoading" @ok="handleGenerate">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="目标仓库" required>
          <j-dict-select-tag
            v-model:value="generateWarehouseId"
            dictCode="wms_warehouse,name,id"
            :stringToNumber="true"
            placeholder="请选择需要补货的仓库"
          />
        </a-form-item>
        <a-form-item label="指定SKU">
          <j-search-select
            v-model:value="generateSkuIds"
            dict="wms_item_sku,code,id"
            :multiple="true"
            placeholder="留空则扫描全部低于补货下限的SKU"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 作业单明细 -->
    <a-drawer v-model:visible="detailVisible" :title="detailTitle" width="1000" destroyOnClose>
      <a-descriptions :column="3" size="small" bordered style="margin-bottom: 16px">
        <a-descriptions-item label="补货单号">{{ detailData.jobCode || '-' }}</a-descriptions-item>
        <a-descriptions-item label="所属仓库">{{ detailData.warehouseId_dictText || '-' }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{ detailData.status_dictText || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建人">{{ detailData.createBy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ detailData.createTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="备注">{{ detailData.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
      <BasicTable @register="registerDetailTable" :canResize="false" :pagination="false" />
    </a-drawer>
  </div>
</template>

<script lang="ts" name="wms-wmsReplenish" setup>
  import { ref, reactive } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import JDictSelectTag from '/@/components/Form/src/jeecg/components/JDictSelectTag.vue';
  import JSearchSelect from '/@/components/Form/src/jeecg/components/JSearchSelect.vue';
  import { columns, searchFormSchema, detailViewColumns } from './WmsReplenish.data';
  import { list, generateReplenish, executeOperation, cancelOperation, queryDetails } from './WmsReplenish.api';

  const { createMessage } = useMessage();
  const queryParam = reactive<any>({});

  const { tableContext } = useListPage({
    tableProps: {
      title: '补货作业',
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

  /**
   * 生成补货单
   */
  const generateVisible = ref(false);
  const generateLoading = ref(false);
  const generateWarehouseId = ref<number | undefined>(undefined);
  const generateSkuIds = ref<any[]>([]);

  function handleOpenGenerate() {
    generateWarehouseId.value = queryParam.warehouseId || undefined;
    generateSkuIds.value = [];
    generateVisible.value = true;
  }

  async function handleGenerate() {
    if (!generateWarehouseId.value) {
      createMessage.warning('请选择目标仓库');
      return;
    }
    try {
      generateLoading.value = true;
      const skuIds = (generateSkuIds.value || []).filter((v) => v !== undefined && v !== null && v !== '').join(',');
      const res: any = await generateReplenish({
        warehouseId: generateWarehouseId.value,
        ...(skuIds ? { skuIds } : {}),
      });
      createMessage.success(res?.message || '补货单已生成');
      generateVisible.value = false;
      reload();
    } catch (e: any) {
      createMessage.error(e?.message || '生成补货单失败');
    } finally {
      generateLoading.value = false;
    }
  }

  /**
   * 明细抽屉
   */
  const detailVisible = ref(false);
  const detailTitle = ref('');
  const detailData = ref<Recordable>({});
  const currentOperationId = ref<number | undefined>(undefined);
  const [registerDetailTable, { reload: reloadDetails }] = useTable({
    api: (params) => queryDetails({ ...params, id: currentOperationId.value }),
    columns: detailViewColumns,
    showIndexColumn: false,
    showTableSetting: false,
    useSearchForm: false,
    pagination: false,
  });

  function handleDetail(record) {
    // 列表行已带字典翻译，直接用于表头信息
    detailData.value = record;
    detailTitle.value = `补货单 ${record.jobCode || ''}`;
    currentOperationId.value = record.id;
    detailVisible.value = true;
    reloadDetails();
  }

  async function handleExecute(record) {
    try {
      const res: any = await executeOperation(record.id);
      createMessage.success(res?.message || '作业单已执行');
      reload();
    } catch (e: any) {
      createMessage.error(e?.message || '执行失败');
    }
  }

  async function handleCancel(record) {
    try {
      await cancelOperation(record.id);
      createMessage.success('已取消');
      reload();
    } catch (e: any) {
      createMessage.error(e?.message || '取消失败');
    }
  }

  function getTableAction(record) {
    const actions: any[] = [
      {
        label: '明细',
        onClick: handleDetail.bind(null, record),
      },
    ];
    if (record.status === 1) {
      actions.unshift({
        label: '执行',
        auth: 'wms:wms_warehouse_operations:execute',
        popConfirm: {
          title: '执行后将把存储区库存移到拣货区，是否继续？',
          confirm: handleExecute.bind(null, record),
          placement: 'topLeft',
          okText: '确认执行',
          cancelText: '再想想',
        },
      });
    }
    return actions;
  }

  function getDropDownAction(record) {
    return [
      {
        label: '取消',
        disabled: record.status !== 1,
        auth: 'wms:wms_warehouse_operations:cancel',
        popConfirm: {
          title: '确认取消该补货单？',
          confirm: handleCancel.bind(null, record),
          placement: 'topLeft',
        },
      },
    ];
  }
</script>

<style lang="less" scoped>
  .tip {
    margin-left: 12px;
    color: #8c8c8c;
    font-size: 13px;
  }
</style>
