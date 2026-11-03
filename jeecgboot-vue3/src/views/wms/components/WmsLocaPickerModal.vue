<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    :title="title"
    :width="1280"
    :okText="'选择该库位'"
    :okButtonProps="{ disabled: !currentLocaId }"
    @ok="handleOk"
  >
    <div class="loca-picker">
      <!-- 顶部规格与库位统计面板 -->
      <div v-if="skuInfo" class="loca-picker__stat-bar">
        <div class="stat-card stat-card--spu">
          <div class="stat-label">目标商品规格(SKU)</div>
          <div class="stat-value text-primary" :title="skuInfo.name">
            {{ skuInfo.name }}
            <span v-if="skuInfo.code" class="stat-sub">({{ skuInfo.code }})</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-label">候选库位数</div>
          <div class="stat-value">{{ statData.totalLoca }} <span class="unit">个</span></div>
        </div>
        <div class="stat-card">
          <div class="stat-label">满足校验可放</div>
          <div class="stat-value text-success font-bold">{{ statData.availLoca }} <span class="unit">个</span></div>
        </div>
        <div class="stat-card">
          <div class="stat-label">空闲库位</div>
          <div class="stat-value text-primary">{{ statData.emptyLoca }} <span class="unit">个</span></div>
        </div>
        <div class="stat-card">
          <div class="stat-label">已占用库位</div>
          <div class="stat-value text-warning">{{ statData.occupiedLoca }} <span class="unit">个</span></div>
        </div>
        <div class="stat-card">
          <div class="stat-label">在库总件数</div>
          <div class="stat-value">{{ statData.totalInStock }} <span class="unit">件</span></div>
        </div>
      </div>

      <div class="loca-picker__filter">
        <a-input v-model:value="keyword" allowClear placeholder="搜索库位编码 / 巷道号 / 货架号" style="width: 260px">
          <template #prefix><Icon icon="ant-design:search-outlined" /></template>
        </a-input>
        <a-select v-model:value="category" allowClear placeholder="库区类型" style="width: 150px" :options="categoryOptions" />
        <a-select v-model:value="areaId" allowClear placeholder="库区" style="width: 190px" :options="areaOptions" />
        <a-checkbox v-model:checked="onlyAllowed">只看本商品可放的库位</a-checkbox>
        <span class="loca-picker__summary">
          共 {{ filteredData.length }} 个库位<span v-if="skuId">，其中满足落库条件 {{ allowedCount }} 个</span>
        </span>
      </div>

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
        :scroll="{ x: 1350 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'locaCode'">
            <a-tag :color="record.allowed ? 'green' : 'orange'">{{ record.locaCode }}</a-tag>
          </template>
          <template v-else-if="column.key === 'position'">
            {{ coordText(record) }}
          </template>
          <template v-else-if="column.key === 'boundSummary'">
            <template v-if="record.skuCode || record.boundSummary">
              <a-tooltip :title="record.skuName || record.boundSummary">
                <a-tag color="blue">{{ record.skuCode || record.boundSummary }}</a-tag>
              </a-tooltip>
              <span v-if="record.skuName" style="font-size: 12px; color: #595959">{{ record.skuName }}</span>
            </template>
            <a-tag v-else color="default">未占用</a-tag>
          </template>
          <template v-else-if="column.key === 'batchNumber'">
            <span>{{ record.batchNumber || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'usedCount'">
            <span style="color: #1890ff; font-weight: 500">{{ record.usedCount ?? 0 }} 件</span>
          </template>
          <template v-else-if="column.key === 'availableCount'">
            <span style="color: #52c41a; font-weight: 600">{{ record.availableCount ?? 0 }} 件</span>
          </template>
          <template v-else-if="column.key === 'allowed'">
            <a-tooltip v-if="!record.allowed" :title="record.rejectReason || '未通过落库硬过滤'">
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
  import { useMessage } from '/@/hooks/web/useMessage';
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

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'selected']);

  const loading = ref(false);
  const options = ref<any[]>([]);
  const keyword = ref('');
  const category = ref<string | undefined>();
  const areaId = ref<number | undefined>();
  const onlyAllowed = ref(false);
  const selectedRowKeys = ref<(string | number)[]>([]);
  const skuId = ref<number | undefined>();
  /** 打开时传入的已选库位，回显选中行 */
  const initLocaId = ref<number | undefined>();
  const title = ref('选择库位');

  const categoryOptions = AREA_CATEGORIES;

  const skuInfo = computed(() => {
    const first = options.value.find((o) => o.skuCode || o.skuName || o.spuName);
    if (!first) return null;
    return {
      name: first.skuName || first.spuName || '指定规格',
      code: first.skuCode || '',
    };
  });

  const statData = computed(() => {
    const totalLoca = options.value.length;
    const availLoca = options.value.filter((o) => o.allowed).length;
    const emptyLoca = options.value.filter((o) => (o.usedCount ?? 0) === 0).length;
    const occupiedLoca = options.value.filter((o) => (o.usedCount ?? 0) > 0).length;
    const totalInStock = options.value.reduce((acc, cur) => acc + (cur.usedCount || 0), 0);
    return {
      totalLoca,
      availLoca,
      emptyLoca,
      occupiedLoca,
      totalInStock,
    };
  });

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
      const haystack = [o.locaCode, o.tunnelNumber, o.shelfNumber, o.layerNumber, o.positionNumber, o.areaName, o.skuCode, o.skuName, o.spuName]
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
    { title: '仓库', key: 'warehouseName', dataIndex: 'warehouseName', width: 110 },
    { title: '库区', key: 'areaName', dataIndex: 'areaName', width: 120, ellipsis: true },
    { title: '库区类型', key: 'areaCategoryText', dataIndex: 'areaCategoryText', width: 95 },
    { title: '货架坐标(巷/架/层/位)', key: 'position', width: 140 },
    { title: '绑定规格(SKU)', key: 'boundSummary', width: 170, ellipsis: true },
    { title: '在库批次', key: 'batchNumber', width: 140 },
    { title: '当前在库', key: 'usedCount', width: 95, align: 'right' },
    { title: '当前可用', key: 'availableCount', width: 95, align: 'right' },
    { title: '状态', key: 'locaStatusText', dataIndex: 'locaStatusText', width: 85 },
    { title: '校验结论', key: 'allowed', width: 95, fixed: 'right' },
  ];

  function coordText(record: any) {
    const parts = [record.tunnelNumber, record.shelfNumber, record.layerNumber, record.positionNumber];
    if (parts.every((p) => !p)) return '-';
    return parts.filter(Boolean).join('-');
  }

  function onSelectChange(keys: (string | number)[]) {
    selectedRowKeys.value = keys;
  }

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
    initLocaId.value = data?.locaId ? Number(data.locaId) : undefined;
    title.value = data?.title || '选择上架库位';
    loading.value = true;
    try {
      const list: any[] = ((await listLocaOptions({
        id: data?.entryId,
        warehouseId: data?.warehouseId,
        skuId: skuId.value,
        count: data?.count || 1,
        batchNumber: data?.batchNumber,
        qualityStatus: data?.qualityStatus || 1,
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
    if (!record.allowed) {
      createMessage.warning(record.rejectReason || '该库位不满足落库策略要求，无法选择！');
      return;
    }
    emit('selected', record);
    closeModal();
  }
</script>

<style lang="less" scoped>
  .loca-picker__stat-bar {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 12px;
    padding: 10px 12px;
    background: #f7f9fa;
    border: 1px solid #e8e8e8;
    border-radius: 6px;

    .stat-card {
      flex: 1;
      min-width: 110px;
      padding: 6px 10px;
      background: #fff;
      border: 1px solid #f0f0f0;
      border-radius: 4px;

      &--spu {
        flex: 2;
        min-width: 200px;
        background: #e6f7ff;
        border-color: #91d5ff;
      }

      .stat-label {
        font-size: 12px;
        color: #8c8c8c;
      }

      .stat-value {
        margin-top: 2px;
        font-size: 16px;
        font-weight: 500;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;

        .unit {
          margin-left: 2px;
          font-size: 12px;
          font-weight: normal;
          color: #8c8c8c;
        }

        .stat-sub {
          margin-left: 4px;
          font-size: 12px;
          color: #595959;
          font-weight: normal;
        }
      }
    }
  }

  .loca-picker__filter {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
    flex-wrap: wrap;

    .loca-picker__summary {
      margin-left: auto;
      font-size: 12px;
      color: #8c8c8c;
    }
  }
</style>
