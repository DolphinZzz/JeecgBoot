<template>
  <div class="wms-entry-page">
    <!-- 顶层流程步骤 Tab 切换 -->
    <div class="entry-tab-card">
      <a-tabs v-model:activeKey="activeTabKey" type="card" @change="handleTabChange" class="entry-step-tabs">
        <a-tab-pane key="arriving">
          <template #tab>
            <span>
              <Icon icon="ant-design:car-outlined" /> 待到货
              <a-badge v-if="tabCounts.arriving > 0" :count="tabCounts.arriving" :number-style="{ backgroundColor: '#fa8c16' }" class="tab-badge" />
            </span>
          </template>
        </a-tab-pane>

        <a-tab-pane key="receiving">
          <template #tab>
            <span>
              <Icon icon="ant-design:inbox-outlined" /> 待卸货
              <a-badge v-if="tabCounts.receiving > 0" :count="tabCounts.receiving" :number-style="{ backgroundColor: '#1890ff' }" class="tab-badge" />
            </span>
          </template>
        </a-tab-pane>

        <a-tab-pane key="sorting">
          <template #tab>
            <span>
              <Icon icon="ant-design:partition-outlined" /> 待分拣(待质检)
              <a-badge v-if="tabCounts.sorting > 0" :count="tabCounts.sorting" :number-style="{ backgroundColor: '#722ed1' }" class="tab-badge" />
            </span>
          </template>
        </a-tab-pane>

        <a-tab-pane key="qcDecided">
          <template #tab>
            <span>
              <Icon icon="ant-design:check-circle-outlined" /> 质检完成
              <a-badge v-if="tabCounts.qcDecided > 0" :count="tabCounts.qcDecided" :number-style="{ backgroundColor: '#13c2c2' }" class="tab-badge" />
            </span>
          </template>
        </a-tab-pane>

        <a-tab-pane key="putting">
          <template #tab>
            <span>
              <Icon icon="ant-design:arrow-up-outlined" /> 待上架
              <a-badge v-if="tabCounts.putting > 0" :count="tabCounts.putting" :number-style="{ backgroundColor: '#52c41a' }" class="tab-badge" />
            </span>
          </template>
        </a-tab-pane>

        <a-tab-pane key="completed">
          <template #tab>
            <span>
              <Icon icon="ant-design:file-done-outlined" /> 收货明细
            </span>
          </template>
        </a-tab-pane>

        <a-tab-pane key="all">
          <template #tab>
            <span>全部单据</span>
          </template>
        </a-tab-pane>
      </a-tabs>
    </div>

    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" v-auth="'wms:wms_warehousing_entry:add'" preIcon="ant-design:plus-outlined" @click="handleAdd">
          新建入库单
        </a-button>
        <span class="tip">
          <template v-if="activeTabKey === 'arriving'">待到货阶段：供货商已发运但货物尚未到仓，支持建单、取消与到货卸货清点。</template>
          <template v-else-if="activeTabKey === 'receiving'">待卸货阶段：货已到仓正在月台卸货，请点击【收货清点】录入实收数量、分离破损件并登记SN。</template>
          <template v-else-if="activeTabKey === 'sorting'">待分拣阶段：收货清点完毕，点击【提交质检】按商品明细生成质检单并流转至质检模块。</template>
          <template v-else-if="activeTabKey === 'qcDecided'">质检完成阶段：质检结论已出具，合格品可直接点击【开始上架】，不合格品可操作【退货处置】。</template>
          <template v-else-if="activeTabKey === 'putting'">待上架作业：已内聚上架作业能力，直接点击【上架作业】按规划容量推荐落位并确认上架落库。</template>
          <template v-else-if="activeTabKey === 'completed'">收货明细终态：单据已全量落库入库完成，可点击【开具货物收货单】生成标准A4凭证并打印。</template>
          <template v-else>全流程流转：建单（待到货）→ 卸货清点 → 提交质检 → 质检合格 → 上架落库 → 收货明细（开具收货单）。</template>
        </span>
      </template>

      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" :dropDownActions="getDropDownAction(record)" />
      </template>
    </BasicTable>

    <!-- 弹窗组件 -->
    <WmsEntryCreateModal @register="registerCreateModal" @success="handleSuccess" />
    <WmsEntryReceiveModal @register="registerReceiveModal" @success="handleSuccess" />
    <WmsPutawayConfirmModal @register="registerPutawayModal" @success="handleSuccess" />
    <WmsQualityCheckReturnModal @register="registerReturnModal" @success="handleSuccess" />
    <WmsGoodsReceiptModal @register="registerReceiptModal" />

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
        <a-descriptions-item label="质检合格合计">
          <span style="color: #52c41a; font-weight: 500">{{ current.qcPassTotal ?? 0 }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="质检不合格合计">
          <span style="color: #f5222d; font-weight: 500">{{ current.qcFailTotal ?? 0 }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="MES工单号">{{ current.mesWorkOrderNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="ERP采购单号">{{ current.erpPoNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间" :span="2">{{ current.createTime || '-' }}</a-descriptions-item>
      </a-descriptions>

      <div style="margin-bottom: 12px; display: flex; justify-content: flex-end">
        <a-button type="primary" preIcon="ant-design:printer-outlined" @click="handleOpenReceipt(current)">
          开具货物收货单
        </a-button>
      </div>

      <a-tabs v-model:activeKey="detailTab">
        <a-tab-pane key="details" tab="入库明细">
          <BasicTable @register="registerDetailTable" :canResize="false" :pagination="false" />
        </a-tab-pane>
        <a-tab-pane key="putaway" tab="上架记录">
          <BasicTable @register="registerRecordTable" :canResize="false" :pagination="false" />
        </a-tab-pane>
      </a-tabs>
    </a-drawer>

    <!-- 超收放行审批 -->
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
  import { nextTick, onMounted, reactive, ref } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { Icon } from '/@/components/Icon';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import WmsEntryCreateModal from './components/WmsEntryCreateModal.vue';
  import WmsEntryReceiveModal from './components/WmsEntryReceiveModal.vue';
  import WmsPutawayConfirmModal from '../WmsPutaway/components/WmsPutawayConfirmModal.vue';
  import WmsQualityCheckReturnModal from '../WmsQualityCheck/components/WmsQualityCheckReturnModal.vue';
  import WmsGoodsReceiptModal from './components/WmsGoodsReceiptModal.vue';
  import { columns, searchFormSchema, detailViewColumns, putawayRecordColumns } from './WmsEntry.data';
  import { list, cancelEntry, submitToQc, auditOverReceive, queryDetails, queryPutawayRecords } from './WmsEntry.api';
  import { list as listQc } from '../WmsQualityCheck/WmsQualityCheck.api';

  const { createMessage } = useMessage();
  const queryParam = reactive<any>({});

  // 流程步骤 Tab 状态定义
  const activeTabKey = ref('arriving');
  const tabCounts = reactive({
    arriving: 0,
    receiving: 0,
    sorting: 0,
    qcDecided: 0,
    putting: 0,
    completed: 0,
  });

  const [registerCreateModal, { openModal: openCreateModal }] = useModal();
  const [registerReceiveModal, { openModal: openReceiveModal }] = useModal();
  const [registerPutawayModal, { openModal: openPutawayModal }] = useModal();
  const [registerReturnModal, { openModal: openReturnModal }] = useModal();
  const [registerReceiptModal, { openModal: openReceiptModal }] = useModal();

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
        width: 250,
        fixed: 'right',
        slots: { customRender: 'action' },
      },
      beforeFetch: (params) => {
        applyTabFilter(params);
        return Object.assign(params, queryParam);
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  /**
   * 将当前 activeTabKey 映射为后端 status 过滤条件
   */
  function applyTabFilter(params: any) {
    // 若用户在高级搜索里手动选了 status，以用户为准
    if (queryParam.status !== undefined && queryParam.status !== null && queryParam.status !== '') {
      return;
    }
    switch (activeTabKey.value) {
      case 'arriving':
        params.status = '10';
        break;
      case 'receiving':
        // 待卸货清点：单据已生成，等待月台卸货与实收清点
        params.status = '10';
        break;
      case 'sorting':
        // 待分拣（待质检）：20 已收货清点待提交质检，30 已送检等待质检结论
        params.status = '20,30';
        break;
      case 'qcDecided':
        params.status = '40';
        break;
      case 'putting':
        // 40 质检完成待上架，50 上架中
        params.status = '40,50';
        break;
      case 'completed':
        params.status = '60';
        break;
      case 'all':
      default:
        delete params.status;
        break;
    }
  }

  function handleTabChange(key: string) {
    activeTabKey.value = key;
    reload();
    fetchTabCounts();
  }

  /**
   * 异步统计各流程 Tab 的待办数量
   */
  async function fetchTabCounts() {
    try {
      const [arrRes, recRes, sortRes, qcRes, putRes]: any = await Promise.all([
        list({ status: '10', pageNo: 1, pageSize: 1 }),
        list({ status: '10', pageNo: 1, pageSize: 1 }),
        list({ status: '20,30', pageNo: 1, pageSize: 1 }),
        list({ status: '40', pageNo: 1, pageSize: 1 }),
        list({ status: '40,50', pageNo: 1, pageSize: 1 }),
      ]);
      tabCounts.arriving = arrRes?.total || 0;
      tabCounts.receiving = recRes?.total || 0;
      tabCounts.sorting = sortRes?.total || 0;
      tabCounts.qcDecided = qcRes?.total || 0;
      tabCounts.putting = putRes?.total || 0;
    } catch (e) {
      // 容错不阻断主表格渲染
    }
  }

  onMounted(() => {
    fetchTabCounts();
  });

  function handleAdd() {
    openCreateModal(true, {});
  }

  function handleSuccess() {
    reload();
    fetchTabCounts();
  }

  /**
   * 明细抽屉
   */
  const detailVisible = ref(false);
  const detailTitle = ref('');
  const detailTab = ref('details');
  const current = ref<Recordable>({});

  const [registerDetailTable, { reload: reloadDetails }] = useTable({
    api: async () => {
      if (!current.value.id) return [];
      const res: any = await queryDetails(current.value.id);
      const list = Array.isArray(res) ? res : res?.records || [];
      if (list.length > 0) {
        const putawaySum = list.reduce((sum, d) => sum + Number(d.putawayCount ?? 0), 0);
        const receivedSum = list.reduce((sum, d) => sum + Number(d.receivedCount ?? 0), 0);
        const qcPassSum = list.reduce((sum, d) => sum + Number(d.qcPassCount ?? 0), 0);
        const qcFailSum = list.reduce((sum, d) => sum + Number(d.qcFailCount ?? 0), 0);
        current.value = {
          ...current.value,
          putawayTotal: putawaySum,
          receivedTotal: receivedSum,
          qcPassTotal: qcPassSum,
          qcFailTotal: qcFailSum,
        };
      }
      return list;
    },
    columns: detailViewColumns,
    canResize: false,
    showIndexColumn: true,
    pagination: false,
    immediate: false,
  });

  const [registerRecordTable, { reload: reloadRecords }] = useTable({
    api: async () => {
      if (!current.value.id) return [];
      const res: any = await queryPutawayRecords(current.value.id);
      return Array.isArray(res) ? res : res?.records || [];
    },
    columns: putawayRecordColumns,
    canResize: false,
    showIndexColumn: true,
    pagination: false,
    immediate: false,
  });

  function handleDetail(record: Recordable) {
    current.value = { ...record };
    detailTitle.value = `入库单 ${record.entryCode} 明细`;
    detailTab.value = 'details';
    detailVisible.value = true;
    nextTick(() => {
      reloadDetails();
      reloadRecords();
    });
  }

  function handleReceive(record: Recordable) {
    openReceiveModal(true, { record });
  }

  function handlePutaway(record: Recordable) {
    openPutawayModal(true, { record });
  }

  function handleOpenReceipt(record: Recordable) {
    openReceiptModal(true, { record });
  }

  /**
   * 提交质检
   */
  async function handleSubmitToQc(record) {
    try {
      const res: any = await submitToQc(record.id);
      createMessage.success(res?.message || '已提交质检');
      reload();
      fetchTabCounts();
    } catch (e: any) {
      createMessage.error(e?.message || '提交质检失败');
    }
  }

  async function handleCancel(record) {
    try {
      await cancelEntry(record.id);
      createMessage.success('入库单已取消');
      reload();
      fetchTabCounts();
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
      fetchTabCounts();
    } catch (e: any) {
      createMessage.error(e?.message || '审批失败');
    } finally {
      auditLoading.value = false;
    }
  }

  async function handleQcReturnFromEntry(record) {
    try {
      const res: any = await listQc({ entryCode: record.entryCode });
      const records = res?.records || res || [];
      let target = records.find((r: any) => Number(r.failCount ?? 0) > 0 && Number(r.returnStatus ?? 0) === 1);
      if (!target) {
        target = records.find((r: any) => Number(r.failCount ?? 0) > 0);
      }
      if (target) {
        openReturnModal(true, { record: target });
      } else {
        createMessage.info('未找到该单对应的不合格品质检记录，请前往「质检单」页面查看');
      }
    } catch (e: any) {
      createMessage.error(e?.message || '获取质检退货数据失败');
    }
  }

  /**
   * 根据当前激活 Tab 与单据状态动态生成最佳操作项
   */
  function getTableAction(record: Recordable) {
    const actions: any[] = [];
    const status = Number(record.status);
    const putawayTotal = Number(record.putawayTotal ?? 0);
    const qcPassTotal = record.qcPassTotal !== undefined && record.qcPassTotal !== null ? Number(record.qcPassTotal) : undefined;
    const hasRemainGood = qcPassTotal === undefined ? true : qcPassTotal > putawayTotal;

    // 1. 【待到货】Tab: 优先到货卸货与清点
    if (activeTabKey.value === 'arriving') {
      actions.push({
        label: '到货卸货',
        auth: 'wms:wms_warehousing_entry:receive',
        onClick: handleReceive.bind(null, record),
      });
    }

    // 2. 【待卸货】Tab: 收货清点
    else if (activeTabKey.value === 'receiving') {
      actions.push({
        label: '收货清点',
        auth: 'wms:wms_warehousing_entry:receive',
        onClick: handleReceive.bind(null, record),
      });
    }

    // 3. 【待分拣（待质检）】Tab: 操作为提交质检（文字形式），质检中显示状态提示
    else if (activeTabKey.value === 'sorting') {
      if (status === 20) {
        actions.push({
          label: '提交质检',
          auth: 'wms:wms_warehousing_entry:receive',
          popConfirm: {
            title: '提交后将按商品质检方式生成质检单（免检商品直接放行），是否继续？',
            confirm: handleSubmitToQc.bind(null, record),
            placement: 'topLeft',
          },
        });
      } else if (status === 30) {
        actions.push({
          label: '质检中',
          disabled: true,
        });
      }
    }

    // 4. 【质检完成】Tab: 合格品启动上架，不合格品退货处置
    else if (activeTabKey.value === 'qcDecided') {
      if (hasRemainGood) {
        actions.push({
          label: '开始上架',
          auth: 'wms:wms_warehousing_entry_up:confirm',
          onClick: handlePutaway.bind(null, record),
        });
      }
      if (Number(record.qcFailTotal ?? 0) > 0 || Number(record.pendingReturnRows ?? 0) > 0) {
        actions.push({
          label: '退货处置',
          auth: 'wms:wms_quality_check:return',
          onClick: handleQcReturnFromEntry.bind(null, record),
        });
      }
    }

    // 5. 【待上架】Tab: 内聚核心上架作业！
    else if (activeTabKey.value === 'putting') {
      if (hasRemainGood) {
        actions.push({
          label: '上架作业',
          auth: 'wms:wms_warehousing_entry_up:confirm',
          onClick: handlePutaway.bind(null, record),
        });
      } else {
        actions.push({
          label: '已全部上架',
          disabled: true,
        });
      }
    }

    // 6. 【收货明细】Tab: 重点开具货物收货单
    else if (activeTabKey.value === 'completed') {
      actions.push({
        label: '开具收货单',
        onClick: handleOpenReceipt.bind(null, record),
      });
    }

    // 7. 【全部单据】Tab (兜底视图)
    else {
      if (status === 10) {
        actions.push({
          label: '收货清点',
          auth: 'wms:wms_warehousing_entry:receive',
          onClick: handleReceive.bind(null, record),
        });
      } else if (status === 20) {
        actions.push({
          label: '提交质检',
          auth: 'wms:wms_warehousing_entry:receive',
          popConfirm: {
            title: '提交后将按商品质检方式生成质检单（免检商品直接放行），是否继续？',
            confirm: handleSubmitToQc.bind(null, record),
            placement: 'topLeft',
          },
        });
      } else if (status === 30) {
        actions.push({
          label: '质检中',
          disabled: true,
        });
      } else if (status === 40 || status === 50) {
        if (hasRemainGood) {
          actions.push({
            label: '上架作业',
            auth: 'wms:wms_warehousing_entry_up:confirm',
            onClick: handlePutaway.bind(null, record),
          });
        }
      } else if (status === 60) {
        actions.push({
          label: '开具收货单',
          onClick: handleOpenReceipt.bind(null, record),
        });
      }
    }

    // 常驻操作：查看单据明细
    actions.push({
      label: '明细',
      onClick: handleDetail.bind(null, record),
    });

    return actions;
  }

  function getDropDownAction(record: Recordable) {
    const status = Number(record.status);
    const dropDowns: any[] = [];

    // 退货处置
    if ((Number(record.qcFailTotal ?? 0) > 0 || Number(record.pendingReturnRows ?? 0) > 0) && activeTabKey.value !== 'qcDecided') {
      dropDowns.push({
        label: '退货处置',
        auth: 'wms:wms_quality_check:return',
        onClick: handleQcReturnFromEntry.bind(null, record),
      });
    }

    // 超收放行
    if (status === 90 && record.overReceiveAuditStatus === 1) {
      dropDowns.push({
        label: '超收放行',
        auth: 'wms:wms_warehousing_entry:overReceiveAudit',
        onClick: handleOpenAudit.bind(null, record),
      });
    }

    // 开具货物收货单
    if (status === 60 && activeTabKey.value !== 'completed') {
      dropDowns.push({
        label: '开具收货单',
        onClick: handleOpenReceipt.bind(null, record),
      });
    }

    // 取消单据（仅待到货单据可用）
    if (status === 10) {
      dropDowns.push({
        label: '取消单据',
        auth: 'wms:wms_warehousing_entry:cancel',
        popConfirm: {
          title: '确认取消该入库单？只有「待到货」的单据可以取消。',
          confirm: handleCancel.bind(null, record),
          placement: 'topLeft',
        },
      });
    }

    return dropDowns;
  }
</script>

<style lang="less" scoped>
  .wms-entry-page {
    .entry-tab-card {
      background: #ffffff;
      padding: 12px 12px 0 12px;
      margin-bottom: -6px;
      border-radius: 4px 4px 0 0;
    }

    .entry-step-tabs {
      :deep(.ant-tabs-nav) {
        margin-bottom: 0;
      }

      :deep(.ant-tabs-tab) {
        padding: 8px 16px;
        font-size: 13px;
        transition: all 0.2s ease;
      }

      :deep(.ant-tabs-tab-active) {
        font-weight: 600;
      }
    }

    .tab-badge {
      margin-left: 6px;
    }

    .tip {
      margin-left: 12px;
      color: #8c8c8c;
      font-size: 13px;
    }
  }
</style>
