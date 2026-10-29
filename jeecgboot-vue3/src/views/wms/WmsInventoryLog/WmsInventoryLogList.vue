<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <span class="tip">库存流水只由库存服务写入，页面仅提供查询，用于「库存数 = 流水累计净额」的追溯核对。</span>
      </template>
    </BasicTable>
  </div>
</template>

<script lang="ts" name="wms-wmsInventoryLog" setup>
  import { reactive } from 'vue';
  import { BasicTable } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { columns, searchFormSchema } from './WmsInventoryLog.data';
  import { list } from './WmsInventoryLog.api';

  const queryParam = reactive<any>({});

  const { tableContext } = useListPage({
    tableProps: {
      title: '库存流水',
      api: list,
      columns,
      canResize: true,
      showIndexColumn: false,
      formConfig: {
        schemas: searchFormSchema,
        autoSubmitOnEnter: true,
        showAdvancedButton: true,
        // 区间时间拆成后端认识的 beginTime / endTime
        fieldMapToTime: [['changeTime', ['beginTime', 'endTime'], 'YYYY-MM-DD HH:mm:ss']],
      },
      beforeFetch: (params) => {
        return Object.assign(params, queryParam);
      },
    },
  });

  const [registerTable] = tableContext;
</script>

<style lang="less" scoped>
  .tip {
    color: #8c8c8c;
    font-size: 13px;
  }
</style>
