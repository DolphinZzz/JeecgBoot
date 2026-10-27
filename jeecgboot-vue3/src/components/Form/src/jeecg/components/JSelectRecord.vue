<template>
  <div class="j-select-record">
    <a-input
      :value="showText"
      readonly
      :disabled="disabled"
      :placeholder="placeholder"
      :allow-clear="!disabled"
      @click="openModal"
      @update:value="onInputValue"
    >
      <template #suffix>
        <Icon icon="ant-design:search-outlined" />
      </template>
    </a-input>
    <a-modal
      v-model:open="visible"
      :title="modalTitle"
      :width="modalWidth"
      :footer="null"
      :z-index="2000"
      destroy-on-close
      :get-container="getContainer"
    >
      <div class="j-select-record-search">
        <span class="j-select-record-search-label">{{ searchLabel }}</span>
        <a-input-search
          v-model:value="keyword"
          :placeholder="searchPlaceholder"
          enter-button="查询"
          allow-clear
          @search="handleSearch"
        />
      </div>
      <BasicTable v-if="visible" @register="registerTable">
        <template #action="{ record }">
          <a-button type="link" @click="choose(record)">选择</a-button>
        </template>
      </BasicTable>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import { BasicTable, useTable, BasicColumn } from '/@/components/Table';
  import { Icon } from '/@/components/Icon';

  defineOptions({ name: 'JSelectRecord', inheritAttrs: false });

  const props = withDefaults(
    defineProps<{
      value?: string | number | null;
      disabled?: boolean;
      placeholder?: string;
      modalTitle?: string;
      modalWidth?: number | string;
      api: (params: Recordable) => Promise<any>;
      columns: BasicColumn[];
      rowKey?: string;
      valueField?: string;
      labelField?: string;
      searchField?: string;
      searchLabel?: string;
      searchPlaceholder?: string;
      valueType?: 'string' | 'number';
    }>(),
    {
      disabled: false,
      placeholder: '请选择',
      modalTitle: '请选择',
      modalWidth: 1000,
      rowKey: 'id',
      valueField: 'id',
      labelField: 'name',
      searchField: 'name',
      searchLabel: '名称',
      searchPlaceholder: '请输入名称',
      valueType: 'string',
    },
  );

  const emit = defineEmits(['change', 'update:value']);

  const visible = ref(false);
  const keyword = ref('');
  const showText = ref('');
  const knownValue = ref<any>(undefined);

  const [registerTable, { reload }] = useTable({
    api: (params) => props.api(params),
    columns: props.columns,
    rowKey: props.rowKey,
    useSearchForm: false,
    showTableSetting: false,
    showIndexColumn: false,
    immediate: false,
    bordered: true,
    canResize: false,
    scroll: { y: 360 },
    actionColumn: {
      width: 80,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
    },
    beforeFetch: (params) => {
      const text = keyword.value.trim();
      if (text && props.searchField) {
        params[props.searchField] = `*${text}*`;
      }
      return params;
    },
  });

  function getContainer() {
    return document.body;
  }

  function openModal(event: MouseEvent) {
    if (props.disabled) {
      return;
    }
    const target = event.target as HTMLElement | null;
    if (target?.closest('.ant-input-clear-icon, .anticon-close-circle')) {
      return;
    }
    keyword.value = '';
    visible.value = true;
    reload({ page: 1 });
  }

  function handleSearch() {
    reload({ page: 1 });
  }

  function onInputValue(val: string) {
    if (val) {
      return;
    }
    showText.value = '';
    knownValue.value = null;
    emit('change', null);
    emit('update:value', null);
  }

  function toValue(raw: any) {
    if (raw === undefined || raw === null || raw === '') {
      return null;
    }
    if (props.valueType === 'number') {
      const num = Number(raw);
      return Number.isNaN(num) ? null : num;
    }
    return raw;
  }

  function choose(record: Recordable) {
    const next = toValue(record[props.valueField]);
    knownValue.value = next;
    showText.value = record[props.labelField] ?? '';
    emit('change', next);
    emit('update:value', next);
    visible.value = false;
  }

  watch(
    () => props.value,
    async (val) => {
      if (val === undefined || val === null || val === '') {
        showText.value = '';
        knownValue.value = val;
        return;
      }
      if (String(knownValue.value) === String(val) && showText.value) {
        return;
      }
      knownValue.value = val;
      try {
        const page = await props.api({ [props.valueField]: val, pageNo: 1, pageSize: 1 });
        const records = page?.records || [];
        const row = records.find((item) => String(item[props.valueField]) === String(val)) || records[0];
        if (String(props.value) !== String(val)) {
          return;
        }
        showText.value = row ? (row[props.labelField] ?? '') : '';
      } catch {
        showText.value = '';
      }
    },
    { immediate: true },
  );
</script>

<style lang="less" scoped>
  .j-select-record {
    width: 100%;

    :deep(.ant-input) {
      cursor: pointer;
    }
  }

  .j-select-record-search {
    display: flex;
    align-items: center;
    margin-top: 16px;
    margin-bottom: 16px;
    padding: 0 8px;
  }

  .j-select-record-search-label {
    flex: none;
    margin-right: 12px;
  }

  .j-select-record-search :deep(.ant-input-search) {
    max-width: 360px;
  }
</style>
