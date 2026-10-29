<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" v-auth="'wms:wms_stocktaking:create'" preIcon="ant-design:plus-outlined" @click="handleCreate">
          生成盘点任务</a-button
        >
        <span class="tip">流程：生成盘点任务（锁定库位）→ 录入实盘数 → 差异审批 → 结案调账（按实盘数调整库存并生成损溢记录）。</span>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
    </BasicTable>

    <WmsStocktakingCreateModal @register="registerCreateModal" @success="handleSuccess" />
    <WmsStocktakingCountModal @register="registerCountModal" @success="handleSuccess" />

    <!-- 差异审批 -->
    <a-modal v-model:visible="auditVisible" title="差异审批" :confirm-loading="auditLoading" @ok="handleAuditSubmit">
      <a-descriptions :column="2" size="small" bordered style="margin-bottom: 16px">
        <a-descriptions-item label="盘点批次">{{ auditRecord.batchNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="SKU">{{ auditRecord.skuId_dictText || '-' }}</a-descriptions-item>
        <a-descriptions-item label="库位">{{ auditRecord.targetWarehouseAreaLocaId_dictText || '-' }}</a-descriptions-item>
        <a-descriptions-item label="批次号">{{ auditRecord.batchNumber || '-' }}</a-descriptions-item>
        <a-descriptions-item label="账面数量">{{ auditRecord.bookCount }}</a-descriptions-item>
        <a-descriptions-item label="实盘数量">{{ auditRecord.correctCount }}</a-descriptions-item>
        <a-descriptions-item label="差异数量" :span="2">
          <span :style="{ color: auditRecord.discrepancyCount > 0 ? '#52c41a' : '#f5222d', fontWeight: 500 }">
            {{ auditRecord.discrepancyCount > 0 ? `盘盈 +${auditRecord.discrepancyCount}` : `盘亏 ${auditRecord.discrepancyCount}` }}
          </span>
        </a-descriptions-item>
      </a-descriptions>
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="审批结果" required>
          <a-radio-group v-model:value="auditPass">
            <a-radio :value="true">通过</a-radio>
            <a-radio :value="false">驳回</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审批意见">
          <a-textarea v-model:value="auditReason" :rows="3" placeholder="通过可留空；驳回建议填写原因" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" name="wms-wmsStocktaking" setup>
  import { ref, reactive } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import WmsStocktakingCreateModal from './components/WmsStocktakingCreateModal.vue';
  import WmsStocktakingCountModal from './components/WmsStocktakingCountModal.vue';
  import { columns, searchFormSchema } from './WmsStocktaking.data';
  import { list, audit, confirm } from './WmsStocktaking.api';

  const { createMessage } = useMessage();
  const queryParam = reactive<any>({});
  const [registerCreateModal, { openModal: openCreateModal }] = useModal();
  const [registerCountModal, { openModal: openCountModal }] = useModal();

  const { tableContext } = useListPage({
    tableProps: {
      title: '盘点管理',
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
        width: 200,
        fixed: 'right',
        slots: { customRender: 'action' },
      },
      beforeFetch: (params) => {
        return Object.assign(params, queryParam);
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  function handleCreate() {
    openCreateModal(true, {});
  }

  function handleSuccess() {
    reload();
  }

  function handleInputCount(record) {
    openCountModal(true, { record });
  }

  /**
   * 差异审批
   */
  const auditVisible = ref(false);
  const auditLoading = ref(false);
  const auditPass = ref(true);
  const auditReason = ref('');
  const auditRecord = ref<Recordable>({});

  function handleOpenAudit(record) {
    auditRecord.value = record;
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
      await audit({ id: auditRecord.value.id, pass: auditPass.value, reason: auditReason.value?.trim() || '' });
      createMessage.success(auditPass.value ? '差异已通过' : '差异已驳回');
      auditVisible.value = false;
      reload();
    } catch (e: any) {
      createMessage.error(e?.message || '审批失败');
    } finally {
      auditLoading.value = false;
    }
  }

  /**
   * 结案调账：无差异或审批通过后，按实盘数调整库存
   */
  async function handleConfirm(record) {
    try {
      const res: any = await confirm(record.id);
      createMessage.success(res?.message || '已结案');
      reload();
    } catch (e: any) {
      createMessage.error(e?.message || '结案失败');
    }
  }

  function getTableAction(record) {
    const actions: any[] = [];
    // 待盘点 / 已盘待审核 都可以重新录入实盘数
    if (record.status !== 3) {
      actions.push({
        label: '录入实盘',
        auth: 'wms:wms_stocktaking:count',
        onClick: handleInputCount.bind(null, record),
      });
    }
    // 有待审批差异时才能审批
    if (record.auditStatus === 1) {
      actions.push({
        label: '差异审批',
        auth: 'wms:wms_stocktaking:audit',
        onClick: handleOpenAudit.bind(null, record),
      });
    }
    // 已录入实盘数且（无差异或差异已通过）才可结案
    if (record.status === 2 && record.auditStatus !== 1 && record.auditStatus !== 3) {
      actions.push({
        label: '结案调账',
        auth: 'wms:wms_stocktaking:confirm',
        popConfirm: {
          title: '结案后库存将按实盘数调整，且不可撤销，是否继续？',
          confirm: handleConfirm.bind(null, record),
          placement: 'topLeft',
          okText: '确认结案',
          cancelText: '再想想',
        },
      });
    }
    return actions;
  }
</script>

<style lang="less" scoped>
  .tip {
    margin-left: 12px;
    color: #8c8c8c;
    font-size: 13px;
  }
</style>
