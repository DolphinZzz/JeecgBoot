<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" v-auth="'wms:wms_warehousing_entry:add'" preIcon="ant-design:plus-outlined" @click="handleAdd"> 新建入库单</a-button>
        <span class="tip">流程：建单（待到货）→ 收货清点 → 提交质检 → 质检合格 → 上架落库 → 已完成入库。任何一步都不能跳。</span>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" :dropDownActions="getDropDownAction(record)" />
      </template>
    </BasicTable>

    <WmsEntryCreateModal @register="registerCreateModal" @success="handleSuccess" />
    <WmsEntryReceiveModal @register="registerReceiveModal" @success="handleSuccess" />
    <WmsPutawayConfirmModal @register="registerPutawayModal" @success="handleSuccess" />

    <!-- 明细 / 上架记录抽屉 -->
    <a-drawer v-model:visible="detailVisible" :title="detailTitle" width="1080" destroyOnClose>
      <a-descriptions :column="3" size="small" bordered style="margin-bottom: 16px">
        <a-descriptions-item label="入库单号">{{ current.entryCode || '-' }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{ current.status_dictText || '-' }}</a-descriptions-item>
        <a-descriptions-item label="入库类型">{{ current.entryType_dictText || '-' }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ current.warehouseName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="货主">{{ current.consignorName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="批次号">{{ current.batchNumber || '-' }}</a-descriptions-item>
        <a-descriptions-item label="计划数量">{{ current.totalCount }}</a-descriptions-item>
        <a-descriptions-item label="实收合计">{{ current.receivedTotal ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="已上架合计">{{ current.putawayTotal ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="MES工单号">{{ current.mesWorkOrderNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="ERP采购单号">{{ current.erpPoNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ current.createTime || '-' }}</a-descriptions-item>
      </a-descriptions>

      <a-tabs v-model:activeKey="detailTab">
        <a-tab-pane key="details" tab="入库明细">
          <BasicTable @register="registerDetailTable" :canResize="false" :pagination="false" />
        </a-tab-pane>
        <a-tab-pane key="putaway" tab="上架记录">
          <BasicTable @register="registerRecordTable" :canResize="false" :pagination="false" />
        </a-tab-pane>
      </a-tabs>
    </a-drawer>

    <!-- 超收放行 -->
    <a-modal v-model:visible="auditVisible" title="超收放行审批" :confirm-loading="auditLoading" @ok="handleAuditSubmit">
      <a-descriptions :column="2" size="small" bordered style="margin-bottom: 16px">
        <a-descriptions-item label="入库单号">{{ current.entryCode || '-' }}</a-descriptions-item>
        <a-descriptions-item label="仓库类型">{{ current.warehouseType === 'INDUSTRIAL' ? '工业制造仓' : '普通仓' }}</a-descriptions-item>
        <a-descriptions-item label="超收数量">
          <span style="color: #fa8c16; font-weight: 500">{{ current.surpassCount || 0 }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="少收数量">{{ current.lackCount || 0 }}</a-descriptions-item>
      </a-descriptions>
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="审批结果" required>
          <a-radio-group v-model:value="auditPass">
            <a-radio :value="true">放行</a-radio>
            <a-radio :value="false">驳回</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审批意见">
          <a-textarea v-model:value="auditReason" :rows="3" placeholder="放行可留空；驳回建议填写原因" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" name="wms-wmsEntry" setup>
  import { nextTick, reactive, ref } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import WmsEntryCreateModal from './components/WmsEntryCreateModal.vue';
  import WmsEntryReceiveModal from './components/WmsEntryReceiveModal.vue';
  import WmsPutawayConfirmModal from '../WmsPutaway/components/WmsPutawayConfirmModal.vue';
  import { columns, searchFormSchema, detailViewColumns, putawayRecordColumns } from './WmsEntry.data';
  import { list, cancelEntry, submitToQc, auditOverReceive, queryDetails, queryPutawayRecords } from './WmsEntry.api';

  const { createMessage } = useMessage();
  const queryParam = reactive<any>({});
  const [registerCreateModal, { openModal: openCreateModal }] = useModal();
  const [registerReceiveModal, { openModal: openReceiveModal }] = useModal();
  const [registerPutawayModal, { openModal: openPutawayModal }] = useModal();

  const { tableContext } = useListPage({
    tableProps: {
      title: '入库管理',
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
        width: 230,
        fixed: 'right',
        slots: { customRender: 'action' },
      },
      beforeFetch: (params) => {
        return Object.assign(params, queryParam);
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  function handleAdd() {
    openCreateModal(true, {});
  }

  function handleSuccess() {
    reload();
  }

  /**
   * 明细抽屉
   */
  const detailVisible = ref(false);
  const detailTitle = ref('');
  const detailTab = ref('details');
  const current = ref<Recordable>({});
  // 抽屉里的表格挂载时还没有单据ID，关掉自动加载，等打开抽屉时再手动 reload
  const [registerDetailTable, { reload: reloadDetails }] = useTable({
    api: (params) => queryDetails(current.value.id),
    columns: detailViewColumns,
    showIndexColumn: false,
    showTableSetting: false,
    useSearchForm: false,
    pagination: false,
    immediate: false,
  });
  const [registerRecordTable, { reload: reloadRecords }] = useTable({
    api: (params) => queryPutawayRecords(current.value.id),
    columns: putawayRecordColumns,
    showIndexColumn: false,
    showTableSetting: false,
    useSearchForm: false,
    pagination: false,
    immediate: false,
  });

  async function handleDetail(record) {
    current.value = record;
    detailTitle.value = `入库单 ${record.entryCode || ''}`;
    detailTab.value = 'details';
    detailVisible.value = true;
    // 抽屉内容是打开后才挂载的，等一帧再触发加载，否则表格实例还没注册
    await nextTick();
    reloadDetails();
    reloadRecords();
  }

  function handleReceive(record) {
    openReceiveModal(true, { record });
  }

  function handlePutaway(record) {
    openPutawayModal(true, { record });
  }

  /**
   * 提交质检：由后端按商品默认质检方式生成质检单，免检商品直接放行
   */
  async function handleSubmitToQc(record) {
    try {
      const res: any = await submitToQc(record.id);
      createMessage.success(res?.message || '已提交质检');
      reload();
    } catch (e: any) {
      createMessage.error(e?.message || '提交质检失败');
    }
  }

  async function handleCancel(record) {
    try {
      await cancelEntry(record.id);
      createMessage.success('入库单已取消');
      reload();
    } catch (e: any) {
      createMessage.error(e?.message || '取消失败');
    }
  }

  /**
   * 超收放行
   */
  const auditVisible = ref(false);
  const auditLoading = ref(false);
  const auditPass = ref(true);
  const auditReason = ref('');

  function handleOpenAudit(record) {
    current.value = record;
    auditPass.value = true;
    auditReason.value = '';
    auditVisible.value = true;
  }

  async function handleAuditSubmit() {
    if (!auditPass.value && !auditReason.value?.trim()) {
      createMessage.warning('驳回时请填写审批意见');
      return;
    }
    try {
      auditLoading.value = true;
      await auditOverReceive({
        id: current.value.id,
        pass: auditPass.value,
        reason: auditReason.value?.trim() || '',
      });
      createMessage.success(auditPass.value ? '超收已放行，可继续提交质检' : '超收已驳回');
      auditVisible.value = false;
      reload();
    } catch (e: any) {
      createMessage.error(e?.message || '审批失败');
    } finally {
      auditLoading.value = false;
    }
  }

  function getTableAction(record) {
    const actions: any[] = [{ label: '明细', onClick: handleDetail.bind(null, record) }];
    const status = Number(record.status);
    // IN-01 禁止跳步：只有待到货 / 收货清点中 / 异常挂起（超收放行后）能收货
    if (status === 10 || status === 20 || status === 90) {
      actions.unshift({
        label: '收货清点',
        auth: 'wms:wms_warehousing_entry:receive',
        onClick: handleReceive.bind(null, record),
      });
    }
    // 质检完成（已出结论）才能上架
    if (status === 40 || status === 50) {
      actions.unshift({
        label: '上架作业',
        auth: 'wms:wms_warehousing_entry_up:confirm',
        onClick: handlePutaway.bind(null, record),
      });
    }
    return actions;
  }

  function getDropDownAction(record) {
    const status = Number(record.status);
    return [
      {
        label: '提交质检',
        disabled: status !== 20,
        auth: 'wms:wms_warehousing_entry:receive',
        popConfirm: {
          title: '提交后将按商品质检方式生成质检单（免检商品直接放行），是否继续？',
          confirm: handleSubmitToQc.bind(null, record),
          placement: 'topLeft',
        },
      },
      {
        label: '超收放行',
        disabled: !(status === 90 && record.overReceiveAuditStatus === 1),
        auth: 'wms:wms_warehousing_entry:overReceiveAudit',
        onClick: handleOpenAudit.bind(null, record),
      },
      {
        label: '取消单据',
        disabled: status !== 10,
        auth: 'wms:wms_warehousing_entry:cancel',
        popConfirm: {
          title: '确认取消该入库单？只有「待到货」的单据可以取消。',
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
