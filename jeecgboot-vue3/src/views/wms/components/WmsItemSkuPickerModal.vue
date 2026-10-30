<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    :title="title"
    :width="1100"
    :okText="okText"
    :okButtonProps="{ disabled: selectedRowKeys.length === 0 }"
    @ok="handleOk"
  >
    <div class="sku-picker">
      <!-- 顶部搜索栏：商品名称、商品编码、商品类别 -->
      <div class="sku-picker__filter">
        <a-input
          v-model:value="queryParam.spuName"
          allowClear
          placeholder="搜索商品名称"
          style="width: 190px"
          @pressEnter="handleSearch"
        >
          <template #prefix><Icon icon="ant-design:search-outlined" /></template>
        </a-input>

        <a-input
          v-model:value="queryParam.skuCode"
          allowClear
          placeholder="搜索商品编码 / SPU编码"
          style="width: 210px"
          @pressEnter="handleSearch"
        >
          <template #prefix><Icon icon="ant-design:barcode-outlined" /></template>
        </a-input>

        <div style="width: 200px">
          <JTreeSelect
            v-model:value="queryParam.categoryId"
            dict="wms_item_category,name,id"
            pid-field="parent_id"
            condition='{"is_del":"1"}'
            placeholder="搜索商品类别"
            allowClear
          />
        </div>

        <a-space>
          <a-button type="primary" preIcon="ant-design:search-outlined" @click="handleSearch">搜索</a-button>
          <a-button preIcon="ant-design:reload-outlined" @click="handleReset">重置</a-button>
        </a-space>

        <span class="sku-picker__summary">
          共 {{ total }} 条数据<span v-if="selectedRowKeys.length">，已选中 {{ selectedRowKeys.length }} 项</span>
        </span>
      </div>

      <!-- 下方商品数据列表 -->
      <a-table
        size="small"
        rowKey="id"
        bordered
        :loading="loading"
        :columns="pickerColumns"
        :data-source="dataSource"
        :pagination="pagination"
        :row-selection="{ type: multiple ? 'checkbox' : 'radio', selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
        :customRow="customRow"
        :scroll="{ x: 1000, y: 380 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'skuCode'">
            <a-tag color="blue">{{ record.skuCode }}</a-tag>
          </template>
          <template v-else-if="column.key === 'categoryName'">
            <a-tag v-if="record.categoryName" color="cyan">{{ record.categoryName }}</a-tag>
            <span v-else class="text-muted">-</span>
          </template>
          <template v-else-if="column.key === 'price'">
            <span v-if="record.price !== null && record.price !== undefined">
              ￥{{ Number(record.price).toFixed(2) }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
          <template v-else-if="column.key === 'isSnManaged'">
            <a-tag :color="record.isSnManaged === 1 ? 'purple' : 'default'">
              {{ record.isSnManaged === 1 ? '是' : '否' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, reactive, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { Icon } from '/@/components/Icon';
  import { JTreeSelect } from '/@/components/Form';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { querySkuPageForSelect } from '../WmsEntry/WmsEntry.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'selected']);

  const loading = ref(false);
  const dataSource = ref<any[]>([]);
  const selectedRowKeys = ref<(string | number)[]>([]);
  const selectedRecordsMap = ref<Map<string | number, any>>(new Map());

  const multiple = ref(false);
  const title = ref('选择商品信息');
  const okText = computed(() => (multiple.value ? `确认选择 (${selectedRowKeys.value.length})` : '确认选择'));

  const queryParam = reactive({
    spuName: '',
    skuCode: '',
    categoryId: undefined as string | number | undefined,
  });

  const current = ref(1);
  const pageSize = ref(10);
  const total = ref(0);

  const pickerColumns = [
    { title: '商品编码', key: 'skuCode', dataIndex: 'skuCode', width: 140, fixed: 'left' },
    { title: '商品名称', key: 'spuName', dataIndex: 'spuName', width: 200, ellipsis: true },
    { title: 'SPU编码', key: 'spuCode', dataIndex: 'spuCode', width: 130 },
    { title: '商品类别', key: 'categoryName', dataIndex: 'categoryName', width: 120 },
    { title: '单价(元)', key: 'price', dataIndex: 'price', width: 100, align: 'right' },
    { title: '供应商', key: 'supplierName', dataIndex: 'supplierName', width: 160, ellipsis: true },
    { title: 'SN管理', key: 'isSnManaged', dataIndex: 'isSnManaged', width: 90, align: 'center' },
  ];

  const pagination = computed(() => ({
    current: current.value,
    pageSize: pageSize.value,
    total: total.value,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ['10', '20', '50'],
    showTotal: (t: number) => `共 ${t} 条`,
  }));

  async function loadData() {
    loading.value = true;
    try {
      const params: any = {
        pageNo: current.value,
        pageSize: pageSize.value,
      };
      if (queryParam.spuName?.trim()) params.spuName = queryParam.spuName.trim();
      if (queryParam.skuCode?.trim()) params.skuCode = queryParam.skuCode.trim();
      if (queryParam.categoryId) params.categoryId = Number(queryParam.categoryId);

      const res: any = await querySkuPageForSelect(params);
      const records = res?.records || res?.result?.records || [];
      dataSource.value = records;
      total.value = res?.total ?? res?.result?.total ?? records.length;

      // 缓存当前页记录，供分页跨页勾选识别
      records.forEach((r: any) => {
        selectedRecordsMap.value.set(r.id, r);
      });
    } catch (e: any) {
      createMessage.error(e?.message || '加载商品列表失败');
    } finally {
      loading.value = false;
    }
  }

  function handleSearch() {
    current.value = 1;
    loadData();
  }

  function handleReset() {
    queryParam.spuName = '';
    queryParam.skuCode = '';
    queryParam.categoryId = undefined;
    current.value = 1;
    loadData();
  }

  function handleTableChange(pag: any) {
    current.value = pag.current;
    pageSize.value = pag.pageSize;
    loadData();
  }

  function onSelectChange(keys: (string | number)[], rows: any[]) {
    selectedRowKeys.value = keys;
    rows.forEach((r) => {
      if (r && r.id) selectedRecordsMap.value.set(r.id, r);
    });
  }

  function customRow(record: any) {
    return {
      onClick: () => {
        if (multiple.value) {
          const index = selectedRowKeys.value.indexOf(record.id);
          if (index >= 0) {
            selectedRowKeys.value.splice(index, 1);
          } else {
            selectedRowKeys.value.push(record.id);
            selectedRecordsMap.value.set(record.id, record);
          }
        } else {
          selectedRowKeys.value = [record.id];
          selectedRecordsMap.value.set(record.id, record);
        }
      },
      onDblclick: () => {
        if (!multiple.value) {
          selectedRowKeys.value = [record.id];
          selectedRecordsMap.value.set(record.id, record);
          handleOk();
        }
      },
      style: { cursor: 'pointer' },
    };
  }

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false });
    queryParam.spuName = '';
    queryParam.skuCode = '';
    queryParam.categoryId = undefined;
    current.value = 1;
    multiple.value = !!data?.multiple;
    title.value = data?.title || '选择商品信息';
    selectedRowKeys.value = [];
    selectedRecordsMap.value.clear();

    if (data?.skuId) {
      selectedRowKeys.value = [Number(data.skuId)];
    } else if (Array.isArray(data?.skuIds)) {
      selectedRowKeys.value = data.skuIds.map(Number);
    }

    await loadData();
  });

  function handleOk() {
    if (selectedRowKeys.value.length === 0) {
      createMessage.warning('请选择商品信息');
      return;
    }
    const selectedList = selectedRowKeys.value
      .map((key) => selectedRecordsMap.value.get(key) || dataSource.value.find((d) => d.id === key))
      .filter(Boolean);

    if (selectedList.length === 0) return;
    emit('selected', multiple.value ? selectedList : selectedList[0]);
    closeModal();
  }
</script>

<style lang="less" scoped>
  .sku-picker {
    &__filter {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 12px;
      margin-bottom: 14px;
    }

    &__summary {
      margin-left: auto;
      font-size: 13px;
      color: #8c8c8c;
    }
  }

  .text-muted {
    color: #bfbfbf;
  }
</style>
