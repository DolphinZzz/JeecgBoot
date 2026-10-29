<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose title="上架作业" :width="1200" :okText="'确认上架'" @ok="handleSubmit">
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
          <span v-if="plan.remainCount > 0" style="color: #f5222d; margin-left: 8px">
            （还有 {{ plan.remainCount }} 件没有可用库位，需人工指定库位分批上架）
          </span>
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
      />
    </a-spin>

    <a-alert type="warning" show-icon style="margin-top: 12px">
      <template #message>
        改库位会重新走一遍硬过滤（库位状态、分区匹配、存放条件、混放规则、容积承重），不通过会被拦下。与推荐不一致时记偏离并留痕。
      </template>
    </a-alert>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { JVxeTable } from '/@/components/jeecg/JVxeTable';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { confirmColumns, strategyText, zoneText } from '../WmsPutaway.data';
  import { recommend, confirmPutaway } from '../WmsPutaway.api';
  import { queryDetails } from '../../WmsEntry/WmsEntry.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);

  const putawayTableRef = ref();
  const tableData = reactive({ dataSource: [] as any[] });
  const entry = ref<Recordable>({});
  const plan = ref<Recordable>({});
  const loading = ref(false);

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false });
    const record = data?.record || {};
    const entryId = data?.id || record.id;
    entry.value = record;
    tableData.dataSource = [];
    loading.value = true;
    try {
      // 推荐结果里只有 detailId，商品编码要靠入库明细映射回来
      const [planRes, detailRes]: any = await Promise.all([recommend(entryId), queryDetails(entryId)]);
      plan.value = planRes || {};
      const detailMap = new Map<number, any>();
      (detailRes || []).forEach((d) => detailMap.set(Number(d.id), d));
      tableData.dataSource = (plan.value.lines || []).map((line, index) => {
        const detail = detailMap.get(Number(line.detailId)) || {};
        return {
          // JVxeTable 以 id 作为 rowKey，回填的数据必须自带唯一 id，否则行复用会串数据
          id: `plan-${index}`,
          detailId: line.detailId ?? null,
          skuText: `${detail.skuCode || detail.skuId || '-'} ${detail.spuTitle || ''}`.trim(),
          recommendLocaCode: line.locaCode || '-',
          capacity: line.capacity ?? '-',
          locaId: line.locaId ?? null,
          count: line.count ?? 0,
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
          deviateReason: row.deviateReason || null,
        }));
      if (details.length === 0) {
        createMessage.warning('没有可提交的上架行（请先选择库位并填写数量）');
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
