<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" v-auth="'wms:wms_warehouse_operations:add'" preIcon="ant-design:plus-outlined" @click="handleAdd">
          新建移库单</a-button
        >
        <span class="tip">移库移位只在仓库内部搬移，全仓物理库存总量不变。</span>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" :dropDownActions="getDropDownAction(record)" />
      </template>
    </BasicTable>

    <WmsMoveModal @register="registerModal" @success="handleSuccess" />

    <!-- 作业单明细 -->
    <a-drawer v-model:visible="detailVisible" :title="detailTitle" width="1000" destroyOnClose>
      <a-descriptions :column="3" size="small" bordered style="margin-bottom: 16px">
        <a-descriptions-item label="作业单号">{{ detailData.jobCode || '-' }}</a-descriptions-item>
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

<script lang="ts" name="wms-wmsMove" setup>
  import { ref, reactive } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import WmsMoveModal from './components/WmsMoveModal.vue';
  import { columns, searchFormSchema, detailViewColumns } from './WmsMove.data';
  import { list, executeOperation, cancelOperation, queryDetails } from './WmsMove.api';

  const { createMessage } = useMessage();
  const queryParam = reactive<any>({});
  const [registerModal, { openModal }] = useModal();

  const { tableContext } = useListPage({
    tableProps: {
      title: '移库移位',
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
   * 作业明细抽屉
   */
  const detailVisible = ref(false);
  const detailTitle = ref('');
  const detailData = ref<Recordable>({});
  const currentOperationId = ref<number | undefined>(undefined);
  // 明细接口以 IPage 返回，@Dict 字段由后端翻译，这里直接当表格数据源
  const [registerDetailTable, { reload: reloadDetails }] = useTable({
    api: (params) => queryDetails({ ...params, id: currentOperationId.value }),
    columns: detailViewColumns,
    showIndexColumn: false,
    showTableSetting: false,
    useSearchForm: false,
    pagination: false,
  });

  function handleAdd() {
    openModal(true, {});
  }

  function handleSuccess() {
    reload();
  }

  /**
   * 执行：把移出库位库存扣到移入库位
   */
  async function handleExecute(record) {
    try {
      const res: any = await executeOperation(record.id);
      createMessage.success(res?.message || '作业单已执行');
      reload();
    } catch (e: any) {
      createMessage.error(e?.message || '执行失败');
    }
  }

  /**
   * 取消：只有未确认的作业单可取消
   */
  async function handleCancel(record) {
    try {
      await cancelOperation(record.id);
      createMessage.success('已取消');
      reload();
    } catch (e: any) {
      createMessage.error(e?.message || '取消失败');
    }
  }

  function handleDetail(record) {
    // 列表行已带字典翻译，直接用于表头信息，避免详情接口不经过字典切面而丢 _dictText
    detailData.value = record;
    detailTitle.value = `移库作业单 ${record.jobCode || ''}`;
    currentOperationId.value = record.id;
    detailVisible.value = true;
    reloadDetails();
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
          title: '执行后将立即扣减移出库位库存并增加到移入库位，是否继续？',
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
          title: '确认取消该作业单？',
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
