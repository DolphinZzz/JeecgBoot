<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    :title="isDetail ? '盘库单详情' : (formState.id ? '编辑盘库单' : '新增盘库单')"
    :width="1380"
    :footer="null"
  >
    <div class="stocktaking-order-modal">
      <!-- 1. 盘库单基本信息区域（参考图2原型） -->
      <a-card title="盘库单基本信息" :bordered="false" size="small" class="section-card">
        <a-form layout="vertical" :model="formState">
          <a-row :gutter="24">
            <a-col :span="8">
              <a-form-item label="盘库单号">
                <a-input v-model:value="formState.orderNo" disabled placeholder="自动生成" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="仓库" required>
                <JSearchSelect
                  v-model:value="formState.warehouseId"
                  dict="wms_warehouse,name,id"
                  placeholder="请选择仓库"
                  :disabled="isDetail || (detailList.length > 0 && !!formState.id)"
                  @change="handleWarehouseChange"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="实际金额">
                <a-input-number
                  v-model:value="formState.totalActualAmount"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                  placeholder="请填写实际金额"
                  :disabled="isDetail"
                >
                  <template #addonAfter>元</template>
                </a-input-number>
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="24">
            <a-col :span="16">
              <a-form-item label="备注">
                <a-textarea
                  v-model:value="formState.remark"
                  :maxlength="100"
                  show-count
                  :rows="2"
                  placeholder="备注...100个字符以内"
                  :disabled="isDetail"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="单据日期" required>
                <a-date-picker
                  v-model:value="formState.orderDate"
                  valueFormat="YYYY-MM-DD"
                  style="width: 100%"
                  placeholder="请选择单据日期"
                  :disabled="isDetail"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-card>

      <!-- 2. 商品明细区域（参考图2原型） -->
      <a-card title="商品明细" :bordered="false" size="small" class="section-card detail-card">
        <!-- 明细操作栏：已去除审批开关组与表格设置无用按钮 -->
        <div class="detail-toolbar" v-if="!isDetail">
          <div class="action-buttons">
            <a-button size="small" preIcon="ant-design:upload-outlined" @click="handleBatchImport">批量导入</a-button>
            <a-button
              size="small"
              type="primary"
              ghost
              preIcon="ant-design:cloud-download-outlined"
              :loading="importLoading"
              @click="handleImportAllWarehouseSkus"
            >
              一键导入仓库所有商品
            </a-button>
            <a-button
              size="small"
              type="primary"
              preIcon="ant-design:plus-outlined"
              @click="openAddSkuDrawer"
            >
              新增库存
            </a-button>
          </div>
        </div>

        <!-- 明细表格：自适应宽度与充裕列间距 -->
        <a-table
          size="middle"
          rowKey="skuId"
          bordered
          :columns="tableColumns"
          :data-source="detailList"
          :pagination="false"
          :scroll="{ y: 400, x: 1320 }"
        >
          <!-- 序号 -->
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'index'">
              {{ index + 1 }}
            </template>

            <!-- 规格名：显示人类可读规格名称，杜绝展示原始 JSON 字符串 -->
            <template v-else-if="column.key === 'skuName'">
              <span :title="formatSkuSpecName(record.skuName)">
                {{ formatSkuSpecName(record.skuName) }}
              </span>
            </template>

            <!-- 单价编辑：自适应留白呼吸感 -->
            <template v-else-if="column.key === 'unitPrice'">
              <div class="cell-input-wrapper">
                <a-input-number
                  v-if="!isDetail"
                  v-model:value="record.unitPrice"
                  :min="0"
                  :precision="2"
                  class="cell-input"
                  placeholder="单价"
                  @change="() => recalcRow(record)"
                >
                  <template #addonAfter>元</template>
                </a-input-number>
                <span v-else>{{ record.unitPrice ? record.unitPrice.toFixed(2) + ' 元' : '-' }}</span>
              </div>
            </template>

            <!-- 实际库存录入：自适应留白呼吸感 -->
            <template v-else-if="column.key === 'actualCount'">
              <div class="cell-input-wrapper">
                <a-input-number
                  v-if="!isDetail"
                  v-model:value="record.actualCount"
                  :min="0"
                  :precision="0"
                  class="cell-input"
                  placeholder="实际库存"
                  @change="() => recalcRow(record)"
                >
                  <template #addonAfter>{{ record.unit || '件' }}</template>
                </a-input-number>
                <span v-else>{{ record.actualCount !== null && record.actualCount !== undefined ? record.actualCount + ' ' + (record.unit || '件') : '-' }}</span>
              </div>
            </template>

            <!-- 实际金额：自适应留白呼吸感 -->
            <template v-else-if="column.key === 'actualAmount'">
              <div class="cell-input-wrapper">
                <a-input-number
                  v-if="!isDetail"
                  v-model:value="record.actualAmount"
                  :min="0"
                  :precision="2"
                  class="cell-input"
                  placeholder="金额"
                  @change="recalcTotals"
                >
                  <template #addonAfter>元</template>
                </a-input-number>
                <span v-else>{{ record.actualAmount ? record.actualAmount.toFixed(2) + ' 元' : '0.00 元' }}</span>
              </div>
            </template>

            <!-- 盈亏数 (实际库存 - 账面库存) -->
            <template v-else-if="column.key === 'discrepancyCount'">
              <span v-if="record.actualCount === null || record.actualCount === undefined" style="color: #bfbfbf">-</span>
              <span v-else-if="record.discrepancyCount > 0" style="color: #52c41a; font-weight: bold">
                +{{ record.discrepancyCount }}
              </span>
              <span v-else-if="record.discrepancyCount < 0" style="color: #f5222d; font-weight: bold">
                {{ record.discrepancyCount }}
              </span>
              <span v-else style="color: #8c8c8c">0</span>
            </template>

            <!-- 实际盈亏金额 -->
            <template v-else-if="column.key === 'discrepancyAmount'">
              <span v-if="record.actualCount === null || record.actualCount === undefined" style="color: #bfbfbf">-</span>
              <span v-else-if="record.discrepancyAmount > 0" style="color: #52c41a; font-weight: 500">
                +{{ Number(record.discrepancyAmount).toFixed(2) }}
              </span>
              <span v-else-if="record.discrepancyAmount < 0" style="color: #f5222d; font-weight: 500">
                {{ Number(record.discrepancyAmount).toFixed(2) }}
              </span>
              <span v-else style="color: #8c8c8c">0.00</span>
            </template>

            <!-- 操作 -->
            <template v-else-if="column.key === 'action'">
              <a-popconfirm
                v-if="!isDetail"
                title="确定移除该明细吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleRemoveRow(index)"
              >
                <a-button type="link" danger size="small">
                  <Icon icon="ant-design:delete-outlined" />
                </a-button>
              </a-popconfirm>
              <span v-else>-</span>
            </template>
          </template>

          <!-- 表尾合计行（与图2原型完全一致） -->
          <template #summary>
            <a-table-summary fixed>
              <a-table-summary-row style="background-color: #fafafa; font-weight: 500">
                <a-table-summary-cell :index="0" align="center">合计</a-table-summary-cell>
                <a-table-summary-cell :index="1" align="center">-</a-table-summary-cell>
                <a-table-summary-cell :index="2" align="center">-</a-table-summary-cell>
                <a-table-summary-cell :index="3" align="center">-</a-table-summary-cell>
                <a-table-summary-cell :index="4" align="center">
                  <span style="color: #1890ff; font-weight: bold">{{ totals.bookCount }}</span>
                </a-table-summary-cell>
                <a-table-summary-cell :index="5" align="center">-</a-table-summary-cell>
                <a-table-summary-cell :index="6" align="center">
                  <span style="color: #722ed1; font-weight: bold">{{ totals.actualCount }}</span>
                </a-table-summary-cell>
                <a-table-summary-cell :index="7" align="center">
                  <span style="color: #1890ff; font-weight: bold">￥{{ totals.actualAmount.toFixed(2) }}</span>
                </a-table-summary-cell>
                <a-table-summary-cell :index="8" align="center">
                  <span :style="{ color: totals.discrepancyCount > 0 ? '#52c41a' : (totals.discrepancyCount < 0 ? '#f5222d' : '#8c8c8c'), fontWeight: 'bold' }">
                    {{ totals.discrepancyCount > 0 ? '+' + totals.discrepancyCount : totals.discrepancyCount }}
                  </span>
                </a-table-summary-cell>
                <a-table-summary-cell :index="9" align="right">
                  <span :style="{ color: totals.discrepancyAmount > 0 ? '#52c41a' : (totals.discrepancyAmount < 0 ? '#f5222d' : '#8c8c8c'), fontWeight: 'bold' }">
                    ￥{{ totals.discrepancyAmount.toFixed(2) }}
                  </span>
                </a-table-summary-cell>
                <a-table-summary-cell :index="10" align="center">-</a-table-summary-cell>
              </a-table-summary-row>
            </a-table-summary>
          </template>
        </a-table>
      </a-card>

      <!-- 3. 底部操作按钮栏 -->
      <div class="modal-footer">
        <a-space>
          <a-button @click="handleCancel">取消</a-button>
          <template v-if="!isDetail">
            <a-button :loading="saveLoading" @click="handleSaveDraft">暂存</a-button>
            <a-button type="primary" :loading="completeLoading" @click="handleComplete">完成盘点</a-button>
          </template>
        </a-space>
      </div>
    </div>

    <!-- 商品选择抽屉组件（图1） -->
    <WmsStocktakingSkuDrawer @register="registerSkuDrawer" @selected="onSkuSelected" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, reactive, ref } from 'vue';
  import dayjs from 'dayjs';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useDrawer } from '/@/components/Drawer';
  import { JSearchSelect } from '/@/components/Form';
  import { Icon } from '/@/components/Icon';
  import { useMessage } from '/@/hooks/web/useMessage';
  import WmsStocktakingSkuDrawer from './WmsStocktakingSkuDrawer.vue';
  import { formatSkuSpecName } from '../WmsStocktaking.data';
  import {
    getStocktakingOrderDetail,
    saveDraftStocktakingOrder,
    completeStocktakingOrder,
    importWarehouseAllSkus,
    getSkuBookStock,
  } from '../WmsStocktaking.api';

  const emit = defineEmits(['register', 'success']);
  const { createMessage, createConfirm } = useMessage();

  const isDetail = ref(false);
  const saveLoading = ref(false);
  const completeLoading = ref(false);
  const importLoading = ref(false);
  const approvalSwitchChecked = ref(false);

  const formState = reactive<any>({
    id: undefined,
    orderNo: '自动生成',
    warehouseId: undefined,
    orderDate: dayjs().format('YYYY-MM-DD'),
    totalActualAmount: undefined,
    remark: '',
  });

  const detailList = ref<any[]>([]);

  const [registerSkuDrawer, { openDrawer: openSkuDrawer }] = useDrawer();

  // 表格列配置：列宽充裕分配，保证单价/实际库存/实际金额输入框有自适应呼吸间隔，不再拥挤
  const tableColumns = [
    { title: '#', key: 'index', width: 50, align: 'center', fixed: 'left' },
    { title: '规格编号', dataIndex: 'skuCode', key: 'skuCode', width: 140, fixed: 'left' },
    { title: '规格名', dataIndex: 'skuName', key: 'skuName', minWidth: 160, ellipsis: true },
    { title: '商品名', dataIndex: 'spuName', key: 'spuName', minWidth: 160, ellipsis: true },
    { title: '账面库存', dataIndex: 'bookCount', key: 'bookCount', width: 95, align: 'center' },
    { title: '单价', key: 'unitPrice', width: 155, align: 'center' },
    { title: '实际库存', key: 'actualCount', width: 165, align: 'center' },
    { title: '实际金额', key: 'actualAmount', width: 155, align: 'center' },
    { title: '盈亏数', key: 'discrepancyCount', width: 95, align: 'center' },
    { title: '实际盈亏金额', key: 'discrepancyAmount', width: 125, align: 'right' },
    { title: '操作', key: 'action', width: 60, align: 'center', fixed: 'right' },
  ];

  // 合计统计
  const totals = computed(() => {
    let book = 0;
    let actual = 0;
    let actAmt = 0;
    let disc = 0;
    let discAmt = 0;

    detailList.value.forEach((d) => {
      book += d.bookCount || 0;
      if (d.actualCount !== null && d.actualCount !== undefined) {
        actual += Number(d.actualCount) || 0;
        actAmt += Number(d.actualAmount) || 0;
        disc += Number(d.discrepancyCount) || 0;
        discAmt += Number(d.discrepancyAmount) || 0;
      }
    });

    return {
      bookCount: book,
      actualCount: actual,
      actualAmount: actAmt,
      discrepancyCount: disc,
      discrepancyAmount: discAmt,
    };
  });

  const [registerModal, { closeModal }] = useModalInner(async (data) => {
    isDetail.value = !!data?.isDetail;
    detailList.value = [];
    approvalSwitchChecked.value = false;

    if (data?.id) {
      // 编辑或查看详情
      try {
        const res: any = await getStocktakingOrderDetail(data.id);
        const order = res?.result || res || {};
        formState.id = order.id;
        formState.orderNo = order.orderNo;
        formState.warehouseId = order.warehouseId;
        formState.orderDate = order.orderDate;
        formState.totalActualAmount = order.totalActualAmount;
        formState.remark = order.remark || '';
        approvalSwitchChecked.value = order.approvalSwitch === 1;
        detailList.value = (order.details || []).map((d: any) => ({
          ...d,
          skuName: formatSkuSpecName(d.skuName || ''),
          unitPrice: Number(d.unitPrice) || 0,
          actualCount: d.actualCount !== null && d.actualCount !== undefined ? Number(d.actualCount) : null,
          actualAmount: Number(d.actualAmount) || 0,
          discrepancyCount: Number(d.discrepancyCount) || 0,
          discrepancyAmount: Number(d.discrepancyAmount) || 0,
        }));
      } catch (err: any) {
        createMessage.error(err?.message || '获取盘库单详情失败');
      }
    } else {
      // 新建
      formState.id = undefined;
      formState.orderNo = '自动生成';
      formState.warehouseId = undefined;
      formState.orderDate = dayjs().format('YYYY-MM-DD');
      formState.totalActualAmount = undefined;
      formState.remark = '';
    }
  });

  function handleWarehouseChange() {
    if (detailList.value.length > 0) {
      createConfirm({
        iconType: 'warning',
        title: '提示',
        content: '切换仓库将清空已选商品明细，是否确认切换？',
        onOk: () => {
          detailList.value = [];
        },
      });
    }
  }

  function openAddSkuDrawer() {
    if (!formState.warehouseId) {
      createMessage.warning('请先选择需要盘库的仓库！');
      return;
    }
    openSkuDrawer(true, { warehouseId: formState.warehouseId });
  }

  async function onSkuSelected(selectedSkus: any[]) {
    if (!selectedSkus || selectedSkus.length === 0) return;

    for (const sku of selectedSkus) {
      const exists = detailList.value.some((d) => d.skuId === sku.id);
      if (exists) continue;

      let bookStock = sku.bookCount;
      if (bookStock === undefined || bookStock === null) {
        try {
          const res: any = await getSkuBookStock(formState.warehouseId, sku.id);
          bookStock = Number(res?.result ?? res ?? 0);
        } catch {
          bookStock = 0;
        }
      }

      const defaultPrice = Number(sku.inPrice ?? sku.price ?? 0);

      detailList.value.push({
        skuId: sku.id,
        skuCode: sku.skuCode || '',
        skuName: formatSkuSpecName(sku.skuName || ''),
        spuName: sku.spuName || '',
        unit: sku.unit || '件',
        bookCount: bookStock,
        unitPrice: defaultPrice,
        actualCount: null,
        actualAmount: 0.0,
        discrepancyCount: 0,
        discrepancyAmount: 0.0,
      });
    }
    recalcTotals();
  }

  async function handleImportAllWarehouseSkus() {
    if (!formState.warehouseId) {
      createMessage.warning('请先选择需要盘库的仓库！');
      return;
    }

    importLoading.value = true;
    try {
      const res: any = await importWarehouseAllSkus(formState.warehouseId);
      const skus = res?.result || res || [];
      if (skus.length === 0) {
        createMessage.info('该仓库当前没有库存商品！');
        return;
      }

      detailList.value = skus.map((sku: any) => ({
        skuId: sku.id,
        skuCode: sku.skuCode || '',
        skuName: formatSkuSpecName(sku.skuName || ''),
        spuName: sku.spuName || '',
        unit: sku.unit || '件',
        bookCount: Number(sku.bookCount) || 0,
        unitPrice: Number(sku.inPrice ?? sku.price ?? 0),
        actualCount: null,
        actualAmount: 0.0,
        discrepancyCount: 0,
        discrepancyAmount: 0.0,
      }));
      recalcTotals();
      createMessage.success(`成功导入 ${skus.length} 款在库商品！`);
    } catch (err: any) {
      createMessage.error(err?.message || '一键导入失败');
    } finally {
      importLoading.value = false;
    }
  }

  function handleBatchImport() {
    createMessage.info('支持线下盘点后通过 Excel 模板批量导入实盘数');
  }

  function handleRemoveRow(index: number) {
    detailList.value.splice(index, 1);
    recalcTotals();
  }

  function recalcRow(row: any) {
    const price = Number(row.unitPrice) || 0;
    const book = Number(row.bookCount) || 0;

    if (row.actualCount !== null && row.actualCount !== undefined) {
      const actual = Number(row.actualCount) || 0;
      row.actualAmount = Number((actual * price).toFixed(2));
      row.discrepancyCount = actual - book;
      row.discrepancyAmount = Number((row.discrepancyCount * price).toFixed(2));
    } else {
      row.actualAmount = 0.0;
      row.discrepancyCount = 0;
      row.discrepancyAmount = 0.0;
    }
    recalcTotals();
  }

  function recalcTotals() {
    if (!formState.totalActualAmount || formState.totalActualAmount === 0) {
      formState.totalActualAmount = totals.value.actualAmount;
    }
  }

  function buildPayload() {
    return {
      id: formState.id,
      orderNo: formState.orderNo === '自动生成' ? undefined : formState.orderNo,
      warehouseId: formState.warehouseId,
      orderDate: formState.orderDate,
      totalActualAmount: formState.totalActualAmount ?? totals.value.actualAmount,
      totalBookCount: totals.value.bookCount,
      totalActualCount: totals.value.actualCount,
      totalDiscrepancyCount: totals.value.discrepancyCount,
      totalDiscrepancyAmount: totals.value.discrepancyAmount,
      remark: formState.remark,
      approvalSwitch: approvalSwitchChecked.value ? 1 : 0,
      details: detailList.value.map((d) => ({
        ...d,
        skuId: d.skuId,
        bookCount: Number(d.bookCount) || 0,
        unitPrice: Number(d.unitPrice) || 0,
        actualCount: d.actualCount !== null && d.actualCount !== undefined ? Number(d.actualCount) : null,
        actualAmount: Number(d.actualAmount) || 0,
        discrepancyCount: Number(d.discrepancyCount) || 0,
        discrepancyAmount: Number(d.discrepancyAmount) || 0,
      })),
    };
  }

  async function handleSaveDraft() {
    if (!formState.warehouseId) {
      createMessage.warning('请选择盘库仓库！');
      return;
    }
    if (!formState.orderDate) {
      createMessage.warning('请选择单据日期！');
      return;
    }

    saveLoading.value = true;
    try {
      await saveDraftStocktakingOrder(buildPayload());
      createMessage.success('盘库单已暂存！');
      closeModal();
      emit('success');
    } catch (err: any) {
      createMessage.error(err?.message || '暂存失败');
    } finally {
      saveLoading.value = false;
    }
  }

  async function handleComplete() {
    if (!formState.warehouseId) {
      createMessage.warning('请选择盘库仓库！');
      return;
    }
    if (!formState.orderDate) {
      createMessage.warning('请选择单据日期！');
      return;
    }
    if (detailList.value.length === 0) {
      createMessage.warning('请至少添加一条盘库商品明细！');
      return;
    }
    for (let i = 0; i < detailList.value.length; i++) {
      const d = detailList.value[i];
      if (d.actualCount === null || d.actualCount === undefined || d.actualCount < 0) {
        createMessage.warning(`第 ${i + 1} 行商品【${d.skuCode}】未录入实际库存，无法完成盘点！`);
        return;
      }
    }

    const discCount = totals.value.discrepancyCount;
    const discAmount = totals.value.discrepancyAmount;
    let confirmMsg = '确认完成本次盘点吗？完成盘点后实盘数据将锁定。';
    if (discCount !== 0) {
      const typeStr = discCount > 0 ? '盘盈' : '盘亏';
      confirmMsg = `本次盘点结果为【${typeStr} ${Math.abs(discCount)} 件】，总盈亏金额 ￥${discAmount.toFixed(2)}。完成盘点后实盘数据将锁定，系统不会自动调整库存，可在台账中按需生成盘盈入库或盘亏出库。是否确认完成？`;
    }

    createConfirm({
      iconType: 'warning',
      title: '完成盘点确认',
      content: confirmMsg,
      onOk: async () => {
        completeLoading.value = true;
        try {
          await completeStocktakingOrder(buildPayload());
          createMessage.success('盘点已完成，实盘数据已锁定！可在台账中生成出入库单调账。');
          closeModal();
          emit('success');
        } catch (err: any) {
          createMessage.error(err?.message || '完成盘点失败');
        } finally {
          completeLoading.value = false;
        }
      },
    });
  }

  function handleCancel() {
    closeModal();
  }
</script>

<style lang="less" scoped>
  .stocktaking-order-modal {
    .section-card {
      margin-bottom: 12px;
      :deep(.ant-card-head) {
        min-height: 38px;
        font-weight: bold;
        background-color: #fafafa;
      }
    }
    .detail-card {
      .detail-toolbar {
        display: flex;
        justify-content: flex-end;
        align-items: center;
        margin-bottom: 12px;
        .action-buttons {
          display: flex;
          gap: 10px;
        }
      }
      :deep(.ant-table) {
        .ant-table-cell {
          padding: 10px 8px !important;
        }
      }
      .cell-input-wrapper {
        display: flex;
        justify-content: center;
        align-items: center;
        padding: 0 4px;
        .cell-input {
          width: 100%;
          max-width: 135px;
          :deep(.ant-input-number-group-addon) {
            padding: 0 8px;
            font-size: 12px;
            color: #8c8c8c;
          }
        }
      }
    }
    .modal-footer {
      display: flex;
      justify-content: flex-end;
      padding: 12px 16px 0 0;
      border-top: 1px solid #f0f0f0;
    }
  }
</style>
