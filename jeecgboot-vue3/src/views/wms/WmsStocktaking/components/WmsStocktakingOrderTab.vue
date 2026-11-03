<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleCreateOrder">
          新增盘库单
        </a-button>
        <span class="tip">
          流程：新增盘库单 → 选择仓库并录入实盘数据 → 完成盘点（锁定实盘与盈亏） → 台账中按需【生成盘盈入库】/【生成盘亏出库】调账闭环。
        </span>
      </template>

      <!-- 盘库调整：生成盘盈入库 / 盘亏出库操作与已生成单号Tag展示 -->
      <template #adjust="{ record }">
        <div class="adjust-cell">
          <span v-if="record.status === 0" class="empty-text">-</span>
          <template v-else>
            <span
              v-if="(!record.profitCount || record.profitCount <= 0) && (!record.lossCount || record.lossCount <= 0)"
              class="no-diff-text"
            >
              无差异
            </span>
            <div v-else class="adjust-actions">
              <!-- 盘盈调整 -->
              <div v-if="record.profitCount > 0" class="adjust-item">
                <a-tag v-if="record.adjustInStatus === 1" color="green" class="adjust-tag">
                  入库: {{ record.adjustInOrderNo || '已生成' }}
                </a-tag>
                <a-popconfirm
                  v-else
                  title="确认针对盘盈明细生成正式入库单并执行调账增库吗？"
                  ok-text="确认生成"
                  cancel-text="取消"
                  @confirm="handleCreateProfit(record)"
                >
                  <a-button
                    type="primary"
                    size="small"
                    ghost
                    :loading="loadingMap['profit_' + record.id]"
                  >
                    生成盘盈入库
                  </a-button>
                </a-popconfirm>
              </div>

              <!-- 盘亏调整 -->
              <div v-if="record.lossCount > 0" class="adjust-item">
                <a-tag v-if="record.adjustOutStatus === 1" color="red" class="adjust-tag">
                  出库: {{ record.adjustOutOrderNo || '已生成' }}
                </a-tag>
                <a-popconfirm
                  v-else
                  title="确认针对盘亏明细生成正式出库单并执行调账扣减吗？"
                  ok-text="确认生成"
                  cancel-text="取消"
                  @confirm="handleCreateLoss(record)"
                >
                  <a-button
                    type="primary"
                    danger
                    size="small"
                    ghost
                    :loading="loadingMap['loss_' + record.id]"
                  >
                    生成盘亏出库
                  </a-button>
                </a-popconfirm>
              </div>
            </div>
          </template>
        </div>
      </template>

      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
    </BasicTable>

    <!-- 新增/编辑/详情盘库单弹窗（图2） -->
    <WmsStocktakingOrderModal @register="registerOrderModal" @success="handleSuccess" />
  </div>
</template>

<script lang="ts" setup>
  import { reactive } from 'vue';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import WmsStocktakingOrderModal from './WmsStocktakingOrderModal.vue';
  import { orderColumns, orderSearchFormSchema } from '../WmsStocktaking.data';
  import {
    getStocktakingOrderList,
    deleteStocktakingOrder,
    createProfitEntry,
    createLossOutbound,
  } from '../WmsStocktaking.api';

  const { createMessage } = useMessage();
  const queryParam = reactive<any>({});
  const loadingMap = reactive<Record<string, boolean>>({});
  const [registerOrderModal, { openModal: openOrderModal }] = useModal();

  const { tableContext } = useListPage({
    tableProps: {
      title: '盘库单管理',
      api: getStocktakingOrderList,
      columns: orderColumns,
      canResize: true,
      showIndexColumn: true,
      formConfig: {
        schemas: orderSearchFormSchema,
        autoSubmitOnEnter: true,
        showAdvancedButton: true,
      },
      actionColumn: {
        title: '操作',
        dataIndex: 'action',
        width: 170,
        fixed: 'right',
        slots: { customRender: 'action' },
      },
      beforeFetch: (params) => {
        return Object.assign(params, queryParam);
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  function handleCreateOrder() {
    openOrderModal(true, { isDetail: false });
  }

  function handleSuccess() {
    reload();
  }

  function handleDetail(record: any) {
    openOrderModal(true, { id: record.id, isDetail: true });
  }

  function handleEdit(record: any) {
    openOrderModal(true, { id: record.id, isDetail: false });
  }

  async function handleDelete(record: any) {
    try {
      await deleteStocktakingOrder(record.id);
      createMessage.success('盘库单已删除');
      reload();
    } catch (e: any) {
      createMessage.error(e?.message || '删除失败');
    }
  }

  async function handleCreateProfit(record: any) {
    const key = 'profit_' + record.id;
    loadingMap[key] = true;
    try {
      const res = await createProfitEntry(record.id);
      createMessage.success(`盘盈入库单生成成功，单号: ${res || ''}`);
      reload();
    } catch (e: any) {
      createMessage.error(e?.message || '生成盘盈入库单失败');
    } finally {
      loadingMap[key] = false;
    }
  }

  async function handleCreateLoss(record: any) {
    const key = 'loss_' + record.id;
    loadingMap[key] = true;
    try {
      const res = await createLossOutbound(record.id);
      createMessage.success(`盘亏出库单生成成功，单号: ${res || ''}`);
      reload();
    } catch (e: any) {
      createMessage.error(e?.message || '生成盘亏出库单失败');
    } finally {
      loadingMap[key] = false;
    }
  }

  function getTableAction(record: any) {
    const actions: any[] = [
      {
        label: '详情',
        onClick: handleDetail.bind(null, record),
      },
    ];

    // 仅草稿状态支持编辑与删除
    if (record.status === 0) {
      actions.push({
        label: '继续编辑',
        onClick: handleEdit.bind(null, record),
      });
      actions.push({
        label: '删除',
        color: 'error',
        popConfirm: {
          title: '确定删除该草稿盘库单吗？',
          confirm: handleDelete.bind(null, record),
          placement: 'topLeft',
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
  .adjust-cell {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 28px;
    .empty-text {
      color: #bfbfbf;
    }
    .no-diff-text {
      color: #8c8c8c;
      font-size: 13px;
    }
    .adjust-actions {
      display: flex;
      flex-direction: column;
      gap: 6px;
      align-items: center;
      width: 100%;
      padding: 4px 0;
    }
    .adjust-item {
      display: inline-flex;
      align-items: center;
    }
    .adjust-tag {
      font-size: 12px;
      padding: 1px 8px;
      line-height: 20px;
    }
  }
</style>
