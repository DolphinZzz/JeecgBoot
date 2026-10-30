<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose title="上架作业" :width="1250" :okText="'确认上架'" @ok="handleSubmit">
    <a-descriptions :column="4" size="small" bordered style="margin-bottom: 12px">
      <a-descriptions-item label="入库单号">{{ entry.entryCode || '-' }}</a-descriptions-item>
      <a-descriptions-item label="仓库">{{ entry.warehouseName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="货主">{{ entry.consignorName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="批次号">{{ entry.batchNumber || '-' }}</a-descriptions-item>
    </a-descriptions>

    <a-spin :spinning="loading">
      <a-alert v-if="plan.strategyUsed" type="info" show-icon style="margin-bottom: 12px">
        <template #message>
          策略：<b>{{ strategyText[plan.strategyUsed] || plan.strategyUsed }}</b> ，商品分区：<b>{{
            zoneText[plan.zoneType] || plan.zoneType || '-'
          }}</b>
          ，待上架 <b>{{ plan.needCount ?? 0 }}</b> 件
        </template>
      </a-alert>

      <!-- 推荐为空 / 排不完时把真实原因摆出来：不给原因作业员只能猜 -->
      <a-alert v-if="plan.remainCount > 0" type="warning" show-icon style="margin-bottom: 12px">
        <template #message>
          还有 <b>{{ plan.remainCount }}</b> 件没有合规库位，需人工指定库位上架。
          <span v-if="plan.emptyReason">原因：{{ plan.emptyReason }}</span>
        </template>
        <template #description>
          点「选择库位」可以按库位编码、巷道/货架/层/位筛选本仓库位，逐个看剩余可放件数。
          人工指定会跳过分区、温区、混放、容积承重校验（跨仓与禁用/盘点锁定库位仍然拦截）， 必须勾选「强制上架」并填写原因，记录会标记为强制上架留痕。
        </template>
      </a-alert>

      <JVxeTable
        keep-source
        resizable
        ref="putawayTableRef"
        :columns="confirmColumns"
        :dataSource="tableData.dataSource"
        :height="340"
        :rowNumber="true"
        :toolbar="false"
      >
        <template #locaSlot="{ row, triggerChange }">
          <div class="loca-cell">
            <template v-if="picked[row.id]">
              <div class="loca-cell__code">
                <a-tag :color="picked[row.id].allowed ? 'green' : 'orange'">{{ picked[row.id].locaCode }}</a-tag>
                <a v-if="picked[row.id].allowed === false" class="loca-cell__warn" title="未通过硬过滤，提交时需勾选强制上架">⚠</a>
              </div>
              <div class="loca-cell__meta">{{ coordText(picked[row.id]) }}</div>
            </template>
            <a-button size="small" @click="openPicker(row, triggerChange)">
              {{ picked[row.id] ? '更换' : '选择库位' }}
            </a-button>
          </div>
        </template>
      </JVxeTable>
    </a-spin>

    <a-alert type="warning" show-icon style="margin-top: 12px">
      <template #message>
        不勾选「强制上架」时，改库位会重新走一遍硬过滤（库位状态、分区匹配、存放条件、混放规则、容积承重），不通过会被拦下。
      </template>
    </a-alert>

    <WmsLocaPickerModal @register="registerPicker" @selected="onLocaSelected" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import { BasicModal, useModal, useModalInner } from '/@/components/Modal';
  import { JVxeTable } from '/@/components/jeecg/JVxeTable';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { confirmColumns, strategyText, zoneText } from '../WmsPutaway.data';
  import { recommend, confirmPutaway, listLocaOptions } from '../WmsPutaway.api';
  import { queryDetails } from '../../WmsEntry/WmsEntry.api';
  import WmsLocaPickerModal from '../../components/WmsLocaPickerModal.vue';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);

  const putawayTableRef = ref();
  const tableData = reactive({ dataSource: [] as any[] });
  const entry = ref<Recordable>({});
  const plan = ref<Recordable>({});
  const loading = ref(false);
  /** 行ID → 已选库位（库位选择器返回的整行，含坐标、可用性结论） */
  const picked = ref<Record<string, any>>({});

  /** 打开库位选择器时暂存上下文，选完才知道该回填到哪一行 */
  const pickerTarget = ref<{ row: any; triggerChange: (v: any) => void } | null>(null);

  const [registerPicker, { openModal: openPickerModal }] = useModal();

  /**
   * 每行都带着自己的 SKU 打开选择器：硬过滤结论与剩余可放件数都是
   * 「某个商品放到某个库位」才有意义的结果，不能拿整单混算。
   */
  function openPicker(row: any, triggerChange: (v: any) => void) {
    pickerTarget.value = { row, triggerChange };
    openPickerModal(true, {
      entryId: entry.value.id,
      skuId: row.skuId,
      skuText: row.skuText,
      locaId: row.locaId,
      count: row.count,
      batchNumber: entry.value.batchNumber,
      title: '选择上架库位',
    });
  }

  function onLocaSelected(record: any) {
    const target = pickerTarget.value;
    if (!target) return;
    picked.value = { ...picked.value, [target.row.id]: record };
    target.triggerChange(record.locaId);
    pickerTarget.value = null;
  }

  function coordText(record: any) {
    const parts = [record.tunnelNumber, record.shelfNumber, record.layerNumber, record.positionNumber];
    if (parts.every((p) => p === null || p === undefined || p === '')) return record.areaName || '-';
    return `${record.areaName || ''} ${parts.map((p) => (p === null || p === undefined || p === '' ? '—' : p)).join('-')}`.trim();
  }

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false });
    const record = data?.record || {};
    const entryId = data?.id || record.id;
    entry.value = record;
    tableData.dataSource = [];
    picked.value = {};
    pickerTarget.value = null;
    loading.value = true;
    try {
      // 推荐结果里只有 detailId，商品编码/名称要靠入库明细映射回来
      const [planRes, detailRes]: any = await Promise.all([recommend(entryId), queryDetails(entryId)]);
      plan.value = planRes || {};
      const detailMap = new Map<number, any>();
      (detailRes || []).forEach((d) => detailMap.set(Number(d.id), d));

      // 推荐结果只带库位编码，坐标/尺寸/可放件数要回本仓库位清单里取，
      // 否则推荐行显示得比手选的还单薄
      const locaMap = new Map<number, any>();
      const skuIds = Array.from(new Set((detailRes || []).map((d) => Number(d.skuId)).filter((id) => !isNaN(id))));
      try {
        const lists: any[] = await Promise.all(skuIds.map((skuId) => listLocaOptions({ id: entryId, skuId, count: 1 }).catch(() => [])));
        lists.forEach((list: any[]) => (list || []).forEach((o) => locaMap.set(Number(o.locaId), o)));
      } catch (e) {
        // 拉不到坐标不影响推荐本身，降级成只显示编码
      }

      tableData.dataSource = (plan.value.lines || []).map((line, index) => {
        const detail = detailMap.get(Number(line.detailId)) || {};
        return {
          // JVxeTable 以 id 作为 rowKey，回填的数据必须自带唯一 id，否则行复用会串数据
          id: `plan-${index}`,
          detailId: line.detailId ?? null,
          skuId: detail.skuId ?? null,
          skuText: `${detail.skuCode || detail.skuId || '-'} ${detail.spuTitle || ''}`.trim(),
          recommendLocaCode: line.locaCode || (line.needManual ? '系统未找到' : '-'),
          capacity: line.needManual ? '需人工指定' : (line.capacity ?? '-'),
          // 有推荐的先填推荐库位；待人工指定的行留空强制作业员自己选
          locaId: line.locaId ?? null,
          count: line.count ?? 0,
          force: false,
          deviateReason: '',
        };
      });

      // 推荐库位也算「已选」，只是没走选择器，补一条展示数据
      const initial: Record<string, any> = {};
      tableData.dataSource.forEach((row) => {
        const line = (plan.value.lines || []).find((l) => Number(l.detailId) === Number(row.detailId) && l.locaId === row.locaId);
        if (line && line.locaId) {
          const known = locaMap.get(Number(line.locaId)) || {};
          initial[row.id] = { ...known, locaId: line.locaId, locaCode: line.locaCode, allowed: true };
        }
      });
      picked.value = initial;
    } catch (e: any) {
      createMessage.error(e?.message || '上架推荐失败');
    } finally {
      loading.value = false;
    }
  });

  async function handleSubmit() {
    try {
      const rows: any[] = putawayTableRef.value?.getTableData() ?? tableData.dataSource;
      const details = rows
        .filter((row) => row.detailId && row.locaId && Number(row.count) > 0)
        .map((row) => ({
          detailId: Number(row.detailId),
          count: Number(row.count),
          locaId: Number(row.locaId),
          force: row.force ? true : false,
          deviateReason: row.deviateReason || null,
        }));
      if (details.length === 0) {
        createMessage.warning('没有可提交的上架行（请先选择库位并填写数量）');
        return;
      }
      const unplanned = (rows || []).filter((row) => row.detailId && !row.locaId);
      if (unplanned.length > 0) {
        createMessage.warning(`还有 ${unplanned.length} 行没有选择库位，请补全后再提交（可分批上架）`);
        return;
      }
      // 前端兜底：选了硬过滤不通过的库位却没勾强制上架，先提示，省得后端驳回
      const needForceRow = details.find((d) => {
        if (d.force) return false;
        const row = rows.find((r) => Number(r.detailId) === d.detailId && Number(r.locaId) === d.locaId);
        const record = row ? picked.value[row.id] : null;
        return record && record.allowed === false;
      });
      if (needForceRow) {
        const row = rows.find((r) => Number(r.detailId) === needForceRow.detailId);
        const record = picked.value[row.id];
        createMessage.warning(`库位[${record.locaCode}] 未通过硬过滤：${record.rejectReason || '未知原因'}，请勾选「强制上架」并填写原因`);
        return;
      }
      const missingReason = details.find((d) => d.force && !d.deviateReason);
      if (missingReason) {
        createMessage.warning('勾选了「强制上架」的行必须填写强制上架原因');
        return;
      }
      setModalProps({ confirmLoading: true });
      const res: any = await confirmPutaway({ id: entry.value.id, details });
      const data = res?.result || res || {};
      createMessage.success(data.message || res?.message || '上架完成');
      closeModal();
      emit('success', data);
    } catch (e: any) {
      if (e?.errorFields) return;
      createMessage.error(e?.message || '确认上架失败');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<style lang="less" scoped>
  .loca-cell {
    line-height: 1.3;
    padding: 2px 0;
  }

  .loca-cell__code {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .loca-cell__warn {
    color: #d48806;
  }

  .loca-cell__meta {
    color: #8c8c8c;
    font-size: 12px;
    margin-bottom: 2px;
  }
</style>
