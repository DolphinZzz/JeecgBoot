<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    :title="title"
    :width="1180"
    :okText="'选择该库位'"
    :okButtonProps="{ disabled: !currentLocaId }"
    @ok="handleOk"
  >
    <div class="loca-picker">
      <div class="loca-picker__filter">
        <a-input v-model:value="keyword" allowClear placeholder="搜索库位编码 / 巷道号 / 货架号" style="width: 260px">
          <template #prefix><Icon icon="ant-design:search-outlined" /></template>
        </a-input>
        <a-select v-model:value="category" allowClear placeholder="库区类型" style="width: 150px" :options="categoryOptions" />
        <a-select v-model:value="areaId" allowClear placeholder="库区" style="width: 190px" :options="areaOptions" />
        <a-checkbox v-model:checked="onlyAllowed">只看本商品可放的库位</a-checkbox>
        <span class="loca-picker__summary">
          共 {{ filteredData.length }} 个库位<span v-if="skuId">，其中可放 {{ allowedCount }} 个</span>
        </span>
      </div>

      <a-alert v-if="skuText" type="info" show-icon style="margin-bottom: 8px">
        <template #message>
          正在为 <b>{{ skuText }}</b> 选库位，带 <b>⚠</b> 的库位未通过分区/温区/混放/容积校验，需勾选「强制上架」才能提交。
        </template>
      </a-alert>

      <a-table
        size="small"
        rowKey="locaId"
        bordered
        :loading="loading"
        :columns="pickerColumns"
        :data-source="pagedData"
        :pagination="pagination"
        :row-selection="{ type: 'radio', selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
        :customRow="customRow"
        :scroll="{ x: 1400 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'locaCode'">
            <a-tag :color="record.allowed ? 'green' : 'orange'">{{ record.locaCode }}</a-tag>
          </template>
          <template v-else-if="column.key === 'position'">
            {{ coordText(record) }}
          </template>
          <template v-else-if="column.key === 'size'">
            {{ sizeText(record) }}
          </template>
          <template v-else-if="column.key === 'remainCount'">
            <span v-if="record.remainCount === null || record.remainCount === undefined" class="text-muted">不限/未知</span>
            <b v-else :class="{ 'text-danger': record.remainCount <= 0 }">{{ record.remainCount }}</b>
          </template>
          <template v-else-if="column.key === 'allowed'">
            <a-tooltip v-if="!record.allowed" :title="record.rejectReason || '未通过硬过滤'">
              <a-tag color="orange">⚠ 不可放</a-tag>
            </a-tooltip>
            <a-tag v-else color="green">可放</a-tag>
          </template>
        </template>
      </a-table>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { Icon } from '/@/components/Icon';
  import { listLocaOptions } from '../WmsPutaway/WmsPutaway.api';

  /** 库区类型：与字典 wms_warehouse_area_category 一致 */
  const AREA_CATEGORIES = [
    { label: '存储区', value: 'STORAGE' },
    { label: '拣货区', value: 'PICKING' },
    { label: '收货暂存', value: 'RECEIVING' },
    { label: '待检区', value: 'QC' },
    { label: '退货不良', value: 'RETURN' },
    { label: '集货待发', value: 'SHIPPING' },
  ];

  const emit = defineEmits(['register', 'selected']);

  const loading = ref(false);
  const options = ref<any[]>([]);
  const keyword = ref('');
  const category = ref<string | undefined>();
  const areaId = ref<number | undefined>();
  const onlyAllowed = ref(false);
  const selectedRowKeys = ref<(string | number)[]>([]);
  const skuText = ref('');
  const skuId = ref<number | undefined>();
  /** 打开时传入的已选库位，回显选中行 */
  const initLocaId = ref<number | undefined>();
  const title = ref('选择库位');

  const categoryOptions = AREA_CATEGORIES;

  const areaOptions = computed(() => {
    const map = new Map<number, string>();
    options.value.forEach((o) => {
      if (o.areaId !== null && o.areaId !== undefined) map.set(Number(o.areaId), o.areaName || `库区${o.areaId}`);
    });
    return Array.from(map.entries()).map(([value, label]) => ({ value, label }));
  });

  const filteredData = computed(() => {
    const kw = keyword.value.trim().toLowerCase();
    return options.value.filter((o) => {
      if (onlyAllowed.value && !o.allowed) return false;
      if (category.value && o.areaCategory !== category.value) return false;
      if (areaId.value !== undefined && areaId.value !== null && Number(o.areaId) !== Number(areaId.value)) return false;
      if (!kw) return true;
      // 库位编码之外，巷道/货架/层/位和库区名也进搜索，作业员记不住编码也能找
      const haystack = [o.locaCode, o.tunnelNumber, o.shelfNumber, o.layerNumber, o.positionNumber, o.areaName]
        .filter((v) => v !== null && v !== undefined)
        .join(' ')
        .toLowerCase();
      return haystack.includes(kw);
    });
  });

  const allowedCount = computed(() => filteredData.value.filter((o) => o.allowed).length);

  const current = ref(1);
  const pageSize = ref(10);

  const pagination = computed(() => ({
    current: current.value,
    pageSize: pageSize.value,
    total: filteredData.value.length,
    showSizeChanger: true,
    pageSizeOptions: ['10', '20', '50'],
    showTotal: (t: number) => `共 ${t} 条`,
  }));

  const pagedData = computed(() => {
    const start = (current.value - 1) * pageSize.value;
    return filteredData.value.slice(start, start + pageSize.value);
  });

  const currentLocaId = computed(() => (selectedRowKeys.value.length > 0 ? Number(selectedRowKeys.value[0]) : undefined));

  const pickerColumns = [
    { title: '库位编码', key: 'locaCode', dataIndex: 'locaCode', width: 130, fixed: 'left' },
    { title: '仓库', key: 'warehouseName', dataIndex: 'warehouseName', width: 120 },
    { title: '库区', key: 'areaName', dataIndex: 'areaName', width: 140, ellipsis: true },
    { title: '库区类型', key: 'areaCategoryText', dataIndex: 'areaCategoryText', width: 100 },
    { title: '分区', key: 'zoneTypeText', dataIndex: 'zoneTypeText', width: 80 },
    { title: '巷道/货架/层/位', key: 'position', width: 150 },
    { title: '长×宽×高(mm)', key: 'size', width: 160 },
    { title: '容积(cm³)', key: 'localVolume', dataIndex: 'localVolume', width: 110 },
    { title: '剩余上架件数', key: 'remainCount', width: 120 },
    { title: '状态', key: 'locaStatusText', dataIndex: 'locaStatusText', width: 90 },
    { title: '可用性', key: 'allowed', width: 100 },
  ];

  function coordText(record: any) {
    const parts = [record.tunnelNumber, record.shelfNumber, record.layerNumber, record.positionNumber];
    if (parts.every((p) => p === null || p === undefined || p === '')) return '-';
    return parts.map((p) => (p === null || p === undefined || p === '' ? '—' : p)).join(' / ');
  }

  function sizeText(record: any) {
    const { locaLen, locaWidth, locaHeight } = record;
    if (!locaLen && !locaWidth && !locaHeight) return '-';
    return `${locaLen ?? '-'} × ${locaWidth ?? '-'} × ${locaHeight ?? '-'}`;
  }

  function onSelectChange(keys: (string | number)[]) {
    selectedRowKeys.value = keys;
  }

  /** 整行可点：作业员在一屏里点到哪一行就是选哪一行 */
  function customRow(record: any) {
    return {
      onClick: () => {
        selectedRowKeys.value = [record.locaId];
      },
      style: { cursor: 'pointer' },
    };
  }

  function handleTableChange(pag: any) {
    current.value = pag.current;
    pageSize.value = pag.pageSize;
  }

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false });
    keyword.value = '';
    category.value = undefined;
    areaId.value = undefined;
    onlyAllowed.value = false;
    options.value = [];
    selectedRowKeys.value = [];
    current.value = 1;
    skuId.value = data?.skuId ? Number(data.skuId) : undefined;
    skuText.value = data?.skuText || '';
    initLocaId.value = data?.locaId ? Number(data.locaId) : undefined;
    title.value = data?.title || '选择库位';
    loading.value = true;
    try {
      const list: any[] = ((await listLocaOptions({
        id: data?.entryId,
        warehouseId: data?.warehouseId,
        skuId: skuId.value,
        count: data?.count || 1,
        batchNumber: data?.batchNumber,
      })) || []) as any[];
      options.value = list;
      if (initLocaId.value) {
        selectedRowKeys.value = [initLocaId.value];
        const index = list.findIndex((o) => Number(o.locaId) === initLocaId.value);
        if (index >= 0) current.value = Math.floor(index / pageSize.value) + 1;
      }
    } finally {
      loading.value = false;
    }
  });

  function handleOk() {
    const record = options.value.find((o) => Number(o.locaId) === currentLocaId.value);
    if (!record) return;
    emit('selected', record);
    // BasicModal 的 ok 只是把事件抛出去，窗口不会自己关，这里显式关掉
    closeModal();
  }
</script>

<style lang="less" scoped>
  .loca-picker__filter {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
  }

  .loca-picker__summary {
    margin-left: auto;
    color: #8c8c8c;
  }

  .text-muted {
    color: #8c8c8c;
  }

  .text-danger {
    color: #cf1322;
  }
</style>
