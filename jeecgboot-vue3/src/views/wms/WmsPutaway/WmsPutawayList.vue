<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:import-outlined" @click="handleOpenPutaway">上架作业</a-button>
        <span class="tip">上架记录由「确认上架」写入；同一明细允许分批上架，全部合格品上完后入库单才置「已完成入库」。</span>
      </template>
    </BasicTable>

    <WmsPutawayConfirmModal @register="registerPutawayModal" @success="handleSuccess" />

    <!-- 选入库单开工 -->
    <a-modal v-model:visible="pickerVisible" title="选择待上架的入库单" :confirm-loading="false" @ok="handlePickEntry">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="入库单" required>
          <a-select
            v-model:value="pickedEntryId"
            show-search
            option-filter-prop="label"
            placeholder="只列出质检完成、仍有合格品未上架的单据"
            :loading="pickerLoading"
            :options="entryOptions"
            :field-names="{ label: 'label', value: 'value' }"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
      <a-alert v-if="!pickerLoading && entryOptions.length === 0" type="info" show-icon>
        <template #message>当前没有待上架的入库单。入库单需先完成收货并出具质检结论（状态为「质检完成待上架」）。</template>
      </a-alert>
    </a-modal>
  </div>
</template>

<script lang="ts" name="wms-wmsPutaway" setup>
  import { reactive, ref } from 'vue';
  import { BasicTable } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import WmsPutawayConfirmModal from './components/WmsPutawayConfirmModal.vue';
  import { columns, searchFormSchema } from './WmsPutaway.data';
  import { list } from './WmsPutaway.api';
  import { list as listEntries } from '../WmsEntry/WmsEntry.api';

  const { createMessage } = useMessage();
  const queryParam = reactive<any>({});
  const [registerPutawayModal, { openModal: openPutawayModal }] = useModal();

  const { tableContext } = useListPage({
    tableProps: {
      title: '上架作业',
      api: list,
      columns,
      canResize: true,
      showIndexColumn: false,
      formConfig: {
        schemas: searchFormSchema,
        autoSubmitOnEnter: true,
        showAdvancedButton: true,
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  function handleSuccess() {
    reload();
  }

  /**
   * 选单开工：只列「质检完成待上架 / 上架中」两种状态的单据
   */
  const pickerVisible = ref(false);
  const pickerLoading = ref(false);
  const pickedEntryId = ref<number | undefined>(undefined);
  const entryOptions = ref<any[]>([]);
  const entryCache = new Map<number, any>();

  async function handleOpenPutaway() {
    pickerVisible.value = true;
    pickedEntryId.value = undefined;
    pickerLoading.value = true;
    try {
      const [waiting, putting]: any = await Promise.all([
        listEntries({ status: 40, pageNo: 1, pageSize: 100 }),
        listEntries({ status: 50, pageNo: 1, pageSize: 100 }),
      ]);
      const records = [...(waiting?.records || []), ...(putting?.records || [])];
      entryCache.clear();
      records.forEach((r) => entryCache.set(Number(r.id), r));
      entryOptions.value = records.map((r) => ({
        value: Number(r.id),
        label: `${r.entryCode}（${r.warehouseName || '-'} / 计划 ${r.totalCount ?? 0} 件 / ${r.status_dictText || ''}）`,
      }));
    } catch (e: any) {
      createMessage.error(e?.message || '加载待上架单据失败');
    } finally {
      pickerLoading.value = false;
    }
  }

  function handlePickEntry() {
    if (!pickedEntryId.value) {
      createMessage.warning('请先选择入库单');
      return;
    }
    const record = entryCache.get(Number(pickedEntryId.value)) || { id: pickedEntryId.value };
    pickerVisible.value = false;
    openPutawayModal(true, { record });
  }
</script>

<style lang="less" scoped>
  .tip {
    margin-left: 12px;
    color: #8c8c8c;
    font-size: 13px;
  }
</style>
