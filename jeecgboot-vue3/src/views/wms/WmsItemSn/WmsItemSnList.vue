<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-input-search
          v-model:value="snKeyword"
          placeholder="扫描或输入完整 SN 追溯单件"
          enter-button="追溯"
          style="width: 340px"
          :loading="traceLoading"
          @search="handleTrace"
        />
      </template>
    </BasicTable>
  </div>
</template>

<script lang="ts" name="wms-wmsItemSn" setup>
  import { ref, reactive } from 'vue';
  import { BasicTable } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { columns, searchFormSchema } from './WmsItemSn.data';
  import { list, queryBySn } from './WmsItemSn.api';

  const { createMessage } = useMessage();
  const queryParam = reactive<any>({});
  const snKeyword = ref('');
  const traceLoading = ref(false);

  const { tableContext } = useListPage({
    tableProps: {
      title: 'SN追溯',
      api: list,
      columns,
      canResize: true,
      showIndexColumn: false,
      formConfig: {
        schemas: searchFormSchema,
        autoSubmitOnEnter: true,
        showAdvancedButton: true,
      },
      beforeFetch: (params) => {
        return Object.assign(params, queryParam);
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  /**
   * 按 SN 精确追溯：命中的单件放在列表首位，其余按当前筛选条件回填
   */
  async function handleTrace() {
    const code = snKeyword.value?.trim();
    if (!code) {
      createMessage.warning('请输入要追溯的 SN 序列号');
      return;
    }
    try {
      traceLoading.value = true;
      const sn: any = await queryBySn(code);
      if (!sn) {
        createMessage.warning('未找到该 SN 的追溯记录');
        return;
      }
      const rowNo = (sn as any).snCode || code;
      createMessage.success(`已定位 SN：${rowNo}`);
      // 用 SN 精确条件刷新列表，避免与其它筛选条件冲突
      queryParam.snCode = rowNo;
      reload();
    } catch (e: any) {
      createMessage.error(e?.message || '追溯失败');
    } finally {
      traceLoading.value = false;
    }
  }
</script>
