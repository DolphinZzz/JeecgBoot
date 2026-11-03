<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    title="商品选择"
    :width="1180"
    :showFooter="true"
    :okText="okText"
    :okButtonProps="{ disabled: selectedRowKeys.length === 0 }"
    @ok="handleConfirm"
  >
    <div class="sku-select-drawer">
      <!-- 顶部筛选栏（与图1原型完全一致） -->
      <a-form layout="inline" class="search-form">
        <a-form-item label="商品分类">
          <div style="width: 170px">
            <JTreeSelect
              v-model:value="searchForm.categoryId"
              dict="wms_item_category,name,id"
              pid-field="parent_id"
              condition='{"is_del":"1"}'
              placeholder="请选择商品分类"
              allowClear
              @change="handleSearch"
            />
          </div>
        </a-form-item>

        <a-form-item label="规格编号">
          <a-input
            v-model:value="searchForm.skuCode"
            placeholder="规格编号"
            allowClear
            style="width: 150px"
            @pressEnter="handleSearch"
          />
        </a-form-item>

        <a-form-item label="规格名称">
          <a-input
            v-model:value="searchForm.skuName"
            placeholder="规格名称"
            allowClear
            style="width: 150px"
            @pressEnter="handleSearch"
          />
        </a-form-item>

        <a-form-item label="商品编号">
          <a-input
            v-model:value="searchForm.spuCode"
            placeholder="商品编号"
            allowClear
            style="width: 150px"
            @pressEnter="handleSearch"
          />
        </a-form-item>

        <a-form-item label="商品名称">
          <a-input
            v-model:value="searchForm.spuName"
            placeholder="商品名称"
            allowClear
            style="width: 150px"
            @pressEnter="handleSearch"
          />
        </a-form-item>

        <a-form-item>
          <a-space>
            <a-button type="primary" preIcon="ant-design:search-outlined" @click="handleSearch">查询</a-button>
            <a-button preIcon="ant-design:reload-outlined" @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <!-- 表格工具栏 -->
      <div class="table-toolbar">
        <span class="tip-text">
          已选择 <b style="color: #1890ff">{{ selectedRowKeys.length }}</b> 项商品
        </span>
      </div>

      <!-- 商品规格多选表格 -->
      <a-table
        size="middle"
        rowKey="id"
        bordered
        :loading="loading"
        :columns="columns"
        :data-source="dataSource"
        :pagination="pagination"
        :row-selection="{
          type: 'checkbox',
          selectedRowKeys: selectedRowKeys,
          onChange: onSelectChange,
        }"
        :scroll="{ y: 460 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'index'">
            {{ (pagination.current - 1) * pagination.pageSize + index + 1 }}
          </template>
          <!-- 规格名：人类可读规格名 -->
          <template v-else-if="column.key === 'skuName'">
            <span :title="formatSkuSpecName(record.skuName)">
              {{ formatSkuSpecName(record.skuName) }}
            </span>
          </template>
          <template v-else-if="column.key === 'inPrice'">
            <span v-if="record.inPrice !== null && record.inPrice !== undefined">
              {{ Number(record.inPrice).toFixed(2) }}
            </span>
            <span v-else>-</span>
          </template>
        </template>
      </a-table>
    </div>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { computed, reactive, ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { JTreeSelect } from '/@/components/Form';
  import { Icon } from '/@/components/Icon';
  import { formatSkuSpecName } from '../WmsStocktaking.data';
  import { selectSkuPageForStocktaking } from '../WmsStocktaking.api';

  const emit = defineEmits(['register', 'selected']);

  const currentWarehouseId = ref<number | undefined>(undefined);
  const loading = ref(false);
  const dataSource = ref<any[]>([]);
  const selectedRowKeys = ref<(string | number)[]>([]);
  const selectedRowsMap = ref<Map<string | number, any>>(new Map());

  const searchForm = reactive({
    categoryId: undefined as number | undefined,
    skuCode: '',
    skuName: '',
    spuCode: '',
    spuName: '',
  });

  const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ['10', '20', '50'],
    showTotal: (total: number) => `共 ${total} 条数据`,
  });

  const okText = computed(() => {
    return selectedRowKeys.value.length > 0
      ? `确定选择 (${selectedRowKeys.value.length})`
      : '确定选择';
  });

  const columns = [
    { title: '#', key: 'index', width: 60, align: 'center' },
    { title: '规格编号', dataIndex: 'skuCode', key: 'skuCode', width: 150 },
    { title: '规格名', dataIndex: 'skuName', key: 'skuName', width: 180, ellipsis: true },
    { title: '商品名', dataIndex: 'spuName', key: 'spuName', width: 220, ellipsis: true },
    { title: '单位', dataIndex: 'unit', key: 'unit', width: 90, align: 'center' },
    { title: '入库价', dataIndex: 'inPrice', key: 'inPrice', width: 130, align: 'right' },
  ];

  const [registerDrawer, { closeDrawer }] = useDrawerInner(async (data) => {
    currentWarehouseId.value = data?.warehouseId;
    selectedRowKeys.value = [];
    selectedRowsMap.value.clear();
    handleReset();
  });

  async function loadData() {
    loading.value = true;
    try {
      const params: any = {
        pageNo: pagination.current,
        pageSize: pagination.pageSize,
        warehouseId: currentWarehouseId.value,
      };
      if (searchForm.categoryId) params.categoryId = searchForm.categoryId;
      if (searchForm.skuCode?.trim()) params.skuCode = searchForm.skuCode.trim();
      if (searchForm.skuName?.trim()) params.skuName = searchForm.skuName.trim();
      if (searchForm.spuCode?.trim()) params.spuCode = searchForm.spuCode.trim();
      if (searchForm.spuName?.trim()) params.spuName = searchForm.spuName.trim();

      const res: any = await selectSkuPageForStocktaking(params);
      const records = (res?.records || res?.result?.records || []).map((r: any) => ({
        ...r,
        skuName: formatSkuSpecName(r.skuName),
      }));
      dataSource.value = records;
      pagination.total = res?.total ?? res?.result?.total ?? records.length;
    } finally {
      loading.value = false;
    }
  }

  function handleSearch() {
    pagination.current = 1;
    loadData();
  }

  function handleReset() {
    searchForm.categoryId = undefined;
    searchForm.skuCode = '';
    searchForm.skuName = '';
    searchForm.spuCode = '';
    searchForm.spuName = '';
    pagination.current = 1;
    loadData();
  }

  function handleTableChange(pag: any) {
    pagination.current = pag.current;
    pagination.pageSize = pag.pageSize;
    loadData();
  }

  function onSelectChange(keys: (string | number)[], selectedRows: any[]) {
    selectedRowKeys.value = keys;
    // 跨页保留选中的对象
    selectedRows.forEach((row) => {
      if (row && row.id) {
        selectedRowsMap.value.set(row.id, row);
      }
    });
    // 移除未选中的
    const keySet = new Set(keys);
    Array.from(selectedRowsMap.value.keys()).forEach((id) => {
      if (!keySet.has(id)) {
        selectedRowsMap.value.delete(id);
      }
    });
  }

  function handleConfirm() {
    const list: any[] = [];
    selectedRowKeys.value.forEach((id) => {
      const item = selectedRowsMap.value.get(id);
      if (item) {
        list.push(item);
      } else {
        const found = dataSource.value.find((d) => d.id === id);
        if (found) list.push(found);
      }
    });
    emit('selected', list);
    closeDrawer();
  }
</script>

<style lang="less" scoped>
  .sku-select-drawer {
    .search-form {
      background-color: #fafafa;
      padding: 12px 16px;
      border-radius: 4px;
      margin-bottom: 12px;
      gap: 8px;
    }
    .table-toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
      .tip-text {
        font-size: 13px;
        color: #555;
      }
    }
  }
</style>
