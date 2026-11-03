<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    :title="'入库货物收货单 (Goods Receipt Note)'"
    :width="1000"
    :footer="null"
  >
    <div class="receipt-modal-wrapper">
      <!-- 顶栏操作区（打印时自动隐藏） -->
      <div class="receipt-actions no-print">
        <a-space>
          <a-button type="primary" preIcon="ant-design:printer-outlined" @click="handlePrint">
            一键打印收货单
          </a-button>
          <a-button @click="loadData" :loading="loading" preIcon="ant-design:reload-outlined">
            刷新
          </a-button>
        </a-space>
        <span class="print-tip">提示：点击「一键打印」可直接调起浏览器打印，支持导出 PDF 或连接热敏/针式/激光打印机。</span>
      </div>

      <a-spin :spinning="loading">
        <!-- 打印实体单据区域 -->
        <div id="receipt-print-area" class="receipt-paper">
          <!-- 单据大标题 -->
          <div class="receipt-header">
            <div class="receipt-title">{{ entry.warehouseName || 'WMS' }} 入库货物收货单</div>
            <div class="receipt-subtitle">GOODS RECEIPT NOTE (GRN)</div>
            <div class="receipt-stamp-placeholder">
              <div class="stamp-circle">收货讫</div>
            </div>
          </div>

          <!-- 单头基础属性 -->
          <div class="receipt-meta-grid">
            <div class="meta-item"><span class="meta-label">入库单号：</span><span class="meta-value font-bold">{{ entry.entryCode || '-' }}</span></div>
            <div class="meta-item"><span class="meta-label">收货日期：</span><span class="meta-value">{{ entry.updateTime || entry.createTime || '-' }}</span></div>
            <div class="meta-item"><span class="meta-label">货主/客户：</span><span class="meta-value">{{ entry.consignorName || '-' }}</span></div>
            <div class="meta-item"><span class="meta-label">批次号：</span><span class="meta-value">{{ entry.batchNumber || '-' }}</span></div>
            <div class="meta-item"><span class="meta-label">入库仓库：</span><span class="meta-value">{{ entry.warehouseName || '-' }}</span></div>
            <div class="meta-item"><span class="meta-label">入库类型：</span><span class="meta-value">{{ entry.entryType_dictText || '采购入库' }}</span></div>
            <div class="meta-item"><span class="meta-label">ERP采购单：</span><span class="meta-value">{{ entry.erpPoNo || '-' }}</span></div>
            <div class="meta-item"><span class="meta-label">MES工单号：</span><span class="meta-value">{{ entry.mesWorkOrderNo || '-' }}</span></div>
          </div>

          <!-- 收货明细与库位落位表格 -->
          <table class="receipt-table">
            <thead>
              <tr>
                <th style="width: 40px; text-align: center">No</th>
                <th style="width: 130px">商品编码</th>
                <th>商品名称 / 规格描述</th>
                <th style="width: 75px; text-align: right">计划数</th>
                <th style="width: 75px; text-align: right">实收数</th>
                <th style="width: 75px; text-align: right">合格数</th>
                <th style="width: 75px; text-align: right">不良/破损</th>
                <th style="width: 170px">上架落位库位</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, idx) in details" :key="item.id || idx">
                <td style="text-align: center">{{ idx + 1 }}</td>
                <td class="font-mono">{{ item.skuCode || item.skuId || '-' }}</td>
                <td>
                  <div class="font-bold">{{ item.spuTitle || item.spuName || '-' }}</div>
                  <div v-if="item.skuName" class="sku-sub">{{ item.skuName }}</div>
                </td>
                <td style="text-align: right">{{ item.count ?? item.planCount ?? 0 }}</td>
                <td style="text-align: right" class="font-bold">{{ item.receivedCount ?? 0 }}</td>
                <td style="text-align: right; color: #389e0d">{{ item.qcPassCount ?? item.receivedCount ?? 0 }}</td>
                <td style="text-align: right; color: #cf1322">{{ (item.qcFailCount || 0) + (item.damageCount || 0) }}</td>
                <td>
                  <span v-if="item.locaSummary" class="loca-badge">{{ item.locaSummary }}</span>
                  <span v-else class="text-muted">待落位</span>
                </td>
              </tr>
              <tr v-if="details.length === 0">
                <td colspan="8" style="text-align: center; color: #999; padding: 20px">暂无收货明细数据</td>
              </tr>
            </tbody>
            <!-- 合计行 -->
            <tfoot>
              <tr class="total-row">
                <td colspan="3" style="text-align: center; font-weight: bold">合 计</td>
                <td style="text-align: right; font-weight: bold">{{ totalPlan }}</td>
                <td style="text-align: right; font-weight: bold">{{ totalReceived }}</td>
                <td style="text-align: right; font-weight: bold; color: #389e0d">{{ totalPass }}</td>
                <td style="text-align: right; font-weight: bold; color: #cf1322">{{ totalFail }}</td>
                <td style="font-weight: bold">共 {{ totalPutaway }} 件已上架</td>
              </tr>
            </tfoot>
          </table>

          <!-- 签章备查栏 -->
          <div class="receipt-footer">
            <div class="sign-block">
              <span class="sign-label">送货单位（承运人签字）：</span>
              <span class="sign-line"></span>
            </div>
            <div class="sign-block">
              <span class="sign-label">收货清点人：</span>
              <span class="sign-line"></span>
            </div>
            <div class="sign-block">
              <span class="sign-label">质量检验人（IQC）：</span>
              <span class="sign-line"></span>
            </div>
            <div class="sign-block">
              <span class="sign-label">库房复核/保管人：</span>
              <span class="sign-line"></span>
            </div>
          </div>

          <div class="receipt-bottom-note">
            <span>白联：财务记账联&nbsp;&nbsp;&nbsp;&nbsp;红联：仓库留存联&nbsp;&nbsp;&nbsp;&nbsp;黄联：货主/供应商备查联</span>
            <span class="print-time">打印时间：{{ printTime }}</span>
          </div>
        </div>
      </a-spin>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { queryDetails, queryPutawayRecords } from '../WmsEntry.api';

  const { createMessage } = useMessage();

  const loading = ref(false);
  const entry = ref<Recordable>({});
  const details = ref<any[]>([]);
  const putawayRecords = ref<any[]>([]);
  const printTime = ref('');

  const [registerModal] = useModalInner(async (data) => {
    entry.value = data?.record || {};
    printTime.value = formatNow();
    await loadData();
  });

  function formatNow() {
    const d = new Date();
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  }

  async function loadData() {
    if (!entry.value.id) return;
    try {
      loading.value = true;
      const [detailRes, recordRes]: any = await Promise.all([
        queryDetails(entry.value.id),
        queryPutawayRecords(entry.value.id),
      ]);
      const rawDetails = Array.isArray(detailRes) ? detailRes : detailRes?.records || [];
      const records = Array.isArray(recordRes) ? recordRes : recordRes?.records || [];
      putawayRecords.value = records;

      // 按 SKU 汇总结算每个明细落位在哪些库位
      const locaMap = new Map<number, string[]>();
      records.forEach((r: any) => {
        const skuId = Number(r.skuId);
        if (!locaMap.has(skuId)) locaMap.set(skuId, []);
        const tag = `${r.locaCode || '库位'}(${r.count}件)`;
        locaMap.get(skuId)!.push(tag);
      });

      details.value = rawDetails.map((d: any) => {
        const skuId = Number(d.skuId);
        const locas = locaMap.get(skuId) || [];
        return {
          ...d,
          locaSummary: locas.length > 0 ? locas.join('、') : (d.putawayCount ? `已上架 ${d.putawayCount} 件` : ''),
        };
      });
    } catch (e: any) {
      createMessage.error(e?.message || '加载收货单明细失败');
    } finally {
      loading.value = false;
    }
  }

  const totalPlan = computed(() => details.value.reduce((sum, d) => sum + Number(d.count ?? d.planCount ?? 0), 0));
  const totalReceived = computed(() => details.value.reduce((sum, d) => sum + Number(d.receivedCount ?? 0), 0));
  const totalPass = computed(() => details.value.reduce((sum, d) => sum + Number(d.qcPassCount ?? d.receivedCount ?? 0), 0));
  const totalFail = computed(() => details.value.reduce((sum, d) => sum + Number((d.qcFailCount || 0) + (d.damageCount || 0)), 0));
  const totalPutaway = computed(() => putawayRecords.value.reduce((sum, r) => sum + Number(r.count ?? 0), 0));

  function handlePrint() {
    printTime.value = formatNow();
    window.print();
  }
</script>

<style lang="less" scoped>
  .receipt-modal-wrapper {
    background: #f0f2f5;
    padding: 12px;
  }

  .receipt-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    padding: 8px 12px;
    background: #ffffff;
    border-radius: 4px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

    .print-tip {
      font-size: 12px;
      color: #8c8c8c;
    }
  }

  .receipt-paper {
    background: #ffffff;
    padding: 24px 30px;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    color: #1f1f1f;
    font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  }

  .receipt-header {
    position: relative;
    text-align: center;
    border-bottom: 2px solid #000;
    padding-bottom: 12px;
    margin-bottom: 16px;

    .receipt-title {
      font-size: 22px;
      font-weight: bold;
      letter-spacing: 2px;
      color: #000;
    }

    .receipt-subtitle {
      font-size: 13px;
      font-weight: 500;
      color: #555;
      letter-spacing: 1px;
      margin-top: 2px;
    }

    .receipt-stamp-placeholder {
      position: absolute;
      right: 20px;
      top: 0;

      .stamp-circle {
        width: 60px;
        height: 60px;
        border: 2px dashed #cf1322;
        border-radius: 50%;
        color: #cf1322;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;
        font-weight: bold;
        transform: rotate(-15deg);
        opacity: 0.85;
      }
    }
  }

  .receipt-meta-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 8px 12px;
    font-size: 12px;
    margin-bottom: 16px;
    background: #fafafa;
    padding: 10px 14px;
    border: 1px solid #e8e8e8;
    border-radius: 4px;

    .meta-item {
      display: flex;
      white-space: nowrap;
      overflow: hidden;

      .meta-label {
        color: #595959;
        flex-shrink: 0;
      }

      .meta-value {
        color: #000;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }

  .receipt-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 12px;
    margin-bottom: 20px;

    th, td {
      border: 1px solid #333;
      padding: 6px 8px;
      line-height: 1.4;
    }

    thead th {
      background: #f0f0f0;
      font-weight: bold;
      color: #000;
      text-align: left;
    }

    .total-row td {
      background: #fafafa;
      border-top: 2px solid #000;
    }

    .sku-sub {
      font-size: 11px;
      color: #595959;
      font-weight: normal;
    }

    .loca-badge {
      background: #e6f7ff;
      border: 1px solid #91d5ff;
      padding: 1px 6px;
      border-radius: 2px;
      color: #096dd9;
      display: inline-block;
      font-size: 11px;
    }
  }

  .receipt-footer {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-top: 24px;
    padding-top: 12px;

    .sign-block {
      font-size: 12px;

      .sign-label {
        color: #333;
        display: block;
        margin-bottom: 24px;
      }

      .sign-line {
        display: block;
        border-bottom: 1px solid #000;
        width: 90%;
      }
    }
  }

  .receipt-bottom-note {
    display: flex;
    justify-content: space-between;
    font-size: 11px;
    color: #8c8c8c;
    margin-top: 20px;
    border-top: 1px dashed #d9d9d9;
    padding-top: 8px;
  }

  .font-mono {
    font-family: monospace;
  }

  .font-bold {
    font-weight: bold;
  }

  .text-muted {
    color: #8c8c8c;
  }

  /* 打印样式：打印时屏蔽外部 modal 的一切外壳，只打印收货单主体 */
  @media print {
    body * {
      visibility: hidden;
    }

    .no-print {
      display: none !important;
    }

    #receipt-print-area,
    #receipt-print-area * {
      visibility: visible;
    }

    #receipt-print-area {
      position: absolute;
      left: 0;
      top: 0;
      width: 100%;
      margin: 0;
      padding: 10mm 15mm;
      box-shadow: none;
      border-radius: 0;
    }

    @page {
      size: A4 portrait;
      margin: 10mm;
    }
  }
</style>
