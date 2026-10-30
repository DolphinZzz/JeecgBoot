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
          人工指定会跳过分区、温区、混放、容积承重校验（跨仓与禁用/盘点锁定库位仍然拦截）， 必须勾选「强制上架」并填写原因，记录会标记为强制上架留痕。
        </template>
      </a-alert>

      <a-alert v-if="rejectedOptions.length > 0" type="info" show-icon style="margin-bottom: 12px">
        <template #message>
          下拉里带 <b>⚠</b> 的库位未通过本商品的硬过滤（共 {{ rejectedOptions.length }} 个），选中它们需要勾选「强制上架」。
        </template>
        <template #description>
          <div v-for="(item, index) of rejectedOptions.slice(0, 3)" :key="index"> {{ item.locaCode }}：{{ item.rejectReason }} </div>
          <div v-if="rejectedOptions.length > 3">…… 其余 {{ rejectedOptions.length - 3 }} 个见下拉提示</div>
        </template>
      </a-alert>

      <JVxeTable
        keep-source
        resizable
        ref="putawayTableRef"
        :columns="putawayColumns"
        :dataSource="tableData.dataSource"
        :height="340"
        :rowNumber="true"
        :toolbar="false"
      />
    </a-spin>

    <a-alert type="warning" show-icon style="margin-top: 12px">
      <template #message>
        不勾选「强制上架」时，改库位会重新走一遍硬过滤（库位状态、分区匹配、存放条件、混放规则、容积承重），不通过会被拦下。
      </template>
    </a-alert>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, reactive, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { JVxeTable } from '/@/components/jeecg/JVxeTable';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { confirmColumns, strategyText, zoneText } from '../WmsPutaway.data';
  import { recommend, confirmPutaway, listLocaOptions } from '../WmsPutaway.api';
  import { queryDetails } from '../../WmsEntry/WmsEntry.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);

  const putawayTableRef = ref();
  const tableData = reactive({ dataSource: [] as any[] });
  const entry = ref<Recordable>({});
  const plan = ref<Recordable>({});
  const loading = ref(false);
  /** 库位下拉选项：按库区分组，带每个库位对本商品的硬过滤结论 */
  const locaOptions = ref<any[]>([]);
  /** 库位ID → 拒绝原因（仅本单涉及的商品），提交前做一次前端兜底提示 */
  const rejectMap = ref<Map<number, string>>(new Map());

  /** 未通过硬过滤的选项，单独列出来告诉作业员为什么 */
  const rejectedOptions = computed(() => locaOptions.value.filter((o) => !o.allowed));

  /**
   * 同一张单可能有多行不同 SKU，而 JVxe 的下拉选项是列级的，
   * 所以按本单涉及的每个 SKU 各取一次，用并集做选项：任一 SKU 放不下就标 ⚠，
   * 原因里带上库位编码以外的说明即可。
   */
  async function loadLocaOptions(entryId: number, skuIds: number[]) {
    const results = await Promise.all(skuIds.map((skuId) => listLocaOptions({ id: entryId, skuId, count: 1 }).catch(() => [])));
    const merged = new Map<number, any>();
    const reasons = new Map<number, string>();
    results.forEach((list: any[]) => {
      (list || []).forEach((opt) => {
        const key = Number(opt.locaId);
        const existed = merged.get(key);
        if (!existed) {
          merged.set(key, { ...opt });
        }
        if (!opt.allowed) {
          const prev = reasons.get(key);
          const reason = opt.rejectReason || '未通过硬过滤';
          if (!prev) {
            reasons.set(key, reason);
          } else if (!prev.includes(reason)) {
            reasons.set(key, `${prev}；${reason}`);
          }
        }
      });
    });
    rejectMap.value = reasons;
    locaOptions.value = Array.from(merged.values())
      .map((opt) => {
        const reason = reasons.get(Number(opt.locaId));
        const capacity = opt.remainVolume === null && opt.remainWeight === null ? '不限' : `余${opt.remainVolume ?? '∞'}cm³`;
        return {
          // JVxe 下拉按 group 字段分组渲染，文本里带库位编码 + 状态，方便在长列表里定位
          value: opt.locaId,
          text: `${reason ? '⚠ ' : ''}${opt.locaCode}（${capacity}）`,
          title: reason ? `${opt.locaCode}：${reason}` : opt.locaCode,
          group: opt.group,
          disabled: false,
        };
      })
      .sort((a, b) => String(a.group).localeCompare(String(b.group)) || String(a.text).localeCompare(String(b.text)));
  }

  const putawayColumns = computed(() => confirmColumns.map((col) => (col.key === 'locaId' ? { ...col, options: locaOptions.value } : col)));

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false });
    const record = data?.record || {};
    const entryId = data?.id || record.id;
    entry.value = record;
    tableData.dataSource = [];
    locaOptions.value = [];
    rejectMap.value = new Map();
    loading.value = true;
    try {
      // 推荐结果里只有 detailId，商品编码要靠入库明细映射回来
      const [planRes, detailRes]: any = await Promise.all([recommend(entryId), queryDetails(entryId)]);
      plan.value = planRes || {};
      const detailMap = new Map<number, any>();
      (detailRes || []).forEach((d) => detailMap.set(Number(d.id), d));

      const skuIds = Array.from(
        new Set(
          (plan.value.lines || [])
            .map((line) => detailMap.get(Number(line.detailId))?.skuId)
            .filter((id) => id !== undefined && id !== null)
            .map(Number)
        )
      ) as number[];
      if (skuIds.length > 0) {
        await loadLocaOptions(entryId, skuIds);
      }

      tableData.dataSource = (plan.value.lines || []).map((line, index) => {
        const detail = detailMap.get(Number(line.detailId)) || {};
        return {
          // JVxeTable 以 id 作为 rowKey，回填的数据必须自带唯一 id，否则行复用会串数据
          id: `plan-${index}`,
          detailId: line.detailId ?? null,
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
      const needForce = details.find((d) => !d.force && rejectMap.value.has(d.locaId));
      if (needForce) {
        createMessage.warning(`库位[${needForce.locaId}] 未通过硬过滤，请勾选「强制上架」并填写原因`);
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
  .detail-divider {
    margin: 4px 0 16px;
    font-weight: 500;
  }
</style>
