<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    :title="modalTitle"
    :width="980"
    :showOkBtn="false"
    :showCancelBtn="false"
  >
    <div class="process-route-modal-body">
      <!-- 顶部主表单：【图1】编号（可自动生成 ROUTE_CODE）、名称、说明、备注 -->
      <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ style: { width: '70px' } }">
        <a-row :gutter="24" align="middle">
          <!-- 编号 + 自动生成开关 -->
          <a-col :span="12">
            <a-form-item label="编号" name="routeCode">
              <div class="code-auto-row">
                <a-input
                  v-model:value="formState.routeCode"
                  :disabled="isDetail || isAutoCode"
                  placeholder="请输入工艺路线编号"
                  allowClear
                />
                <div class="auto-switch-wrap">
                  <a-switch
                    v-model:checked="isAutoCode"
                    :loading="codeLoading"
                    :disabled="isDetail"
                    @change="handleAutoCodeChange"
                  />
                  <span class="auto-label" :class="{ active: isAutoCode }">自动生成</span>
                </div>
              </div>
            </a-form-item>
          </a-col>

          <!-- 名称 -->
          <a-col :span="12">
            <a-form-item label="名称" name="routeName">
              <a-input
                v-model:value="formState.routeName"
                :disabled="isDetail"
                placeholder="请输入工艺路线名称"
                allowClear
              />
            </a-form-item>
          </a-col>

          <!-- 说明 -->
          <a-col :span="24">
            <a-form-item label="说明" name="description">
              <a-textarea v-model:value="formState.description" :disabled="isDetail" :rows="3" placeholder="请输入内容" />
            </a-form-item>
          </a-col>

          <!-- 备注 -->
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="formState.remark" :disabled="isDetail" :rows="3" placeholder="请输入内容" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <!-- 【图2】【图4】点击保存初步保存工艺流程后原地弹出 Tab 栏：【组成工序】与【关联产品】 -->
      <div v-if="showSubTabs && formState.id" class="route-sub-tabs-box">
        <a-tabs v-model:activeKey="activeTabKey" type="card">
          <!-- Tab 1：组成工序 -->
          <a-tab-pane key="composition" tab="组成工序">
            <div class="sub-tab-content">
              <div class="sub-toolbar">
                <a-space>
                  <a-button
                    v-if="!isDetail"
                    class="btn-add-light"
                    size="small"
                    preIcon="ant-design:plus-outlined"
                    @click="handleAddComposition"
                  >
                    新增
                  </a-button>
                  <a-button
                    v-if="!isDetail"
                    class="btn-edit-light"
                    size="small"
                    preIcon="ant-design:edit-outlined"
                    :disabled="selectedCompRowKeys.length !== 1"
                    @click="handleEditSelectedComposition"
                  >
                    修改
                  </a-button>
                  <a-button
                    v-if="!isDetail"
                    class="btn-del-light"
                    size="small"
                    preIcon="ant-design:delete-outlined"
                    :disabled="selectedCompRowKeys.length === 0"
                    @click="handleBatchDeleteComposition"
                  >
                    删除
                  </a-button>
                </a-space>

                <div class="sub-toolbar-right">
                  <a-tooltip title="搜索">
                    <a-button
                      shape="circle"
                      size="small"
                      preIcon="ant-design:search-outlined"
                      @click="loadCompositions"
                    />
                  </a-tooltip>
                  <a-tooltip title="刷新">
                    <a-button
                      shape="circle"
                      size="small"
                      preIcon="ant-design:reload-outlined"
                      @click="loadCompositions"
                    />
                  </a-tooltip>
                </div>
              </div>

              <a-table
                size="small"
                rowKey="id"
                :loading="compLoading"
                :columns="compositionColumns"
                :dataSource="compositionList"
                :pagination="compPagination"
                @change="handleCompTableChange"
                :rowSelection="
                  isDetail
                    ? undefined
                    : {
                        selectedRowKeys: selectedCompRowKeys,
                        onChange: onCompSelectChange,
                      }
                "
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'isCriticalProcess'">
                    <span>{{ record.isCriticalProcess === 1 ? '是' : '否' }}</span>
                  </template>
                  <template v-else-if="column.key === 'relationWithNext'">
                    <span>{{ record.relationWithNext || '-' }}</span>
                  </template>
                  <template v-else-if="column.key === 'nextProcessName'">
                    <span>{{ record.nextProcessName || '-' }}</span>
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <a-space v-if="!isDetail">
                      <a @click="handleEditComposition(record)">修改</a>
                      <a-popconfirm title="是否确认删除该组成工序？" @confirm="handleDeleteComposition(record)">
                        <a style="color: #ff4d4f">删除</a>
                      </a-popconfirm>
                    </a-space>
                    <span v-else>-</span>
                  </template>
                </template>
              </a-table>
            </div>
          </a-tab-pane>

          <!-- Tab 2：关联产品（【图4】及点击新增弹出【图5】添加产品制程模态框） -->
          <a-tab-pane key="product" tab="关联产品">
            <div class="sub-tab-content">
              <!-- 搜索栏：产品物料编码、产品物料名称、搜索、重置 -->
              <div v-show="showProductSearch" class="product-search-bar">
                <div class="search-field">
                  <span class="field-label">产品物料编码</span>
                  <a-input
                    v-model:value="productQuery.productId"
                    placeholder="请输入产品物料编码"
                    allowClear
                    style="width: 190px"
                  />
                </div>
                <div class="search-field">
                  <span class="field-label">产品物料名称</span>
                  <a-input
                    v-model:value="productQuery.productName"
                    placeholder="请输入产品物料名称"
                    allowClear
                    style="width: 190px"
                  />
                </div>
                <a-space>
                  <a-button type="primary" size="small" preIcon="ant-design:search-outlined" @click="handleSearchProduct">
                    搜索
                  </a-button>
                  <a-button size="small" preIcon="ant-design:reload-outlined" @click="handleResetProduct">
                    重置
                  </a-button>
                </a-space>
              </div>

              <!-- 操作按钮栏：+ 新增、修改、删除、导出 -->
              <div class="sub-toolbar">
                <a-space>
                  <a-button
                    v-if="!isDetail"
                    class="btn-add-light"
                    size="small"
                    preIcon="ant-design:plus-outlined"
                    @click="handleAddProduct"
                  >
                    新增
                  </a-button>
                  <a-button
                    v-if="!isDetail"
                    class="btn-edit-light"
                    size="small"
                    preIcon="ant-design:edit-outlined"
                    :disabled="selectedProdRowKeys.length !== 1"
                    @click="handleEditSelectedProduct"
                  >
                    修改
                  </a-button>
                  <a-button
                    v-if="!isDetail"
                    class="btn-del-light"
                    size="small"
                    preIcon="ant-design:delete-outlined"
                    :disabled="selectedProdRowKeys.length === 0"
                    @click="handleBatchDeleteProduct"
                  >
                    删除
                  </a-button>
                  <a-button
                    class="btn-export-light"
                    size="small"
                    preIcon="ant-design:download-outlined"
                    @click="handleExportProduct"
                  >
                    导出
                  </a-button>
                </a-space>

                <div class="sub-toolbar-right">
                  <a-tooltip :title="showProductSearch ? '隐藏搜索' : '显示搜索'">
                    <a-button
                      shape="circle"
                      size="small"
                      preIcon="ant-design:search-outlined"
                      @click="toggleProductSearch"
                    />
                  </a-tooltip>
                  <a-tooltip title="刷新">
                    <a-button
                      shape="circle"
                      size="small"
                      preIcon="ant-design:reload-outlined"
                      @click="handleSearchProduct"
                    />
                  </a-tooltip>
                </div>
              </div>

              <a-table
                size="small"
                rowKey="id"
                :columns="productColumns"
                :dataSource="filteredProductList"
                :pagination="prodPagination"
                @change="handleProdTableChange"
                :rowSelection="
                  isDetail
                    ? undefined
                    : {
                        selectedRowKeys: selectedProdRowKeys,
                        onChange: onProdSelectChange,
                      }
                "
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'action'">
                    <a-space v-if="!isDetail">
                      <a @click="handleEditProduct(record)">修改</a>
                      <a-popconfirm title="是否确认删除该关联产品？" @confirm="handleDeleteProduct(record)">
                        <a style="color: #ff4d4f">删除</a>
                      </a-popconfirm>
                    </a-space>
                    <span v-else>-</span>
                  </template>
                </template>
              </a-table>
            </div>
          </a-tab-pane>
        </a-tabs>
      </div>

      <!-- 底部保存与关闭按钮 -->
      <div class="modal-footer-btns">
        <a-button v-if="!isDetail" type="primary" :loading="saving" @click="handleSave">保 存</a-button>
        <a-button @click="handleClose">关 闭</a-button>
      </div>
    </div>

    <!-- 【图3】添加/修改工艺组成模态框 -->
    <MesProcessRouteCompositionModal @register="registerCompModal" @success="loadCompositions" />

    <!-- 【图5】添加/修改产品制程模态框 -->
    <MesProcessProductModal @register="registerProductModal" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, reactive, computed } from 'vue';
  import { BasicModal, useModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { compositionColumns, productColumns } from '../MesProcessRoute.data';
  import {
    getNextRouteCode,
    saveOrUpdate,
    listRouteCompositions,
    deleteRouteComposition,
    batchDeleteRouteComposition,
  } from '../MesProcessRoute.api';
  import MesProcessRouteCompositionModal from './MesProcessRouteCompositionModal.vue';
  import MesProcessProductModal from './MesProcessProductModal.vue';

  const emit = defineEmits(['register', 'success']);
  const { createMessage, createConfirm } = useMessage();

  const formRef = ref();
  const isUpdate = ref(false);
  const isDetail = ref(false);
  const showSubTabs = ref(false);
  const saving = ref(false);
  const isAutoCode = ref(false);
  const codeLoading = ref(false);
  const manualCodeDraft = ref('');
  const activeTabKey = ref('composition');

  const formState = reactive<any>({
    id: undefined,
    routeCode: '',
    routeName: '',
    description: '',
    remark: '',
    isEnabled: 1,
  });

  const formRules = {
    routeCode: [{ required: true, message: '请输入工艺路线编号', trigger: 'blur' }],
    routeName: [{ required: true, message: '请输入工艺路线名称', trigger: 'blur' }],
  };

  // 【组成工序】状态与分页
  const compLoading = ref(false);
  const compositionList = ref<any[]>([]);
  const selectedCompRowKeys = ref<(string | number)[]>([]);
  const selectedCompRows = ref<any[]>([]);

  const compPagination = reactive({
    current: 1,
    pageSize: 10,
    pageSizeOptions: ['10', '20', '30'],
    showTotal: (total: number, range: [number, number]) => `${range[0]}-${range[1]} 共 ${total} 条`,
    showQuickJumper: true,
    showSizeChanger: true,
    size: 'small',
    onChange: (page: number, pageSize: number) => {
      compPagination.current = page;
      compPagination.pageSize = pageSize;
    },
    onShowSizeChange: (current: number, size: number) => {
      compPagination.current = current;
      compPagination.pageSize = size;
    },
  });

  function handleCompTableChange(pagination: any) {
    compPagination.current = pagination.current;
    compPagination.pageSize = pagination.pageSize;
  }

  // 【关联产品】前端状态、搜索与分页
  const showProductSearch = ref(true);
  const productQuery = reactive({
    productId: '',
    productName: '',
  });
  const productList = ref<any[]>([]);
  const selectedProdRowKeys = ref<(string | number)[]>([]);
  const selectedProdRows = ref<any[]>([]);

  const filteredProductList = computed(() => {
    let list = productList.value || [];
    if (productQuery.productId) {
      list = list.filter((item) => String(item.productId || '').includes(productQuery.productId.trim()));
    }
    if (productQuery.productName) {
      list = list.filter((item) => String(item.productName || '').includes(productQuery.productName.trim()));
    }
    return list;
  });

  const prodPagination = reactive({
    current: 1,
    pageSize: 10,
    pageSizeOptions: ['10', '20', '30'],
    showTotal: (total: number, range: [number, number]) => `${range[0]}-${range[1]} 共 ${total} 条`,
    showQuickJumper: true,
    showSizeChanger: true,
    size: 'small',
    onChange: (page: number, pageSize: number) => {
      prodPagination.current = page;
      prodPagination.pageSize = pageSize;
    },
    onShowSizeChange: (current: number, size: number) => {
      prodPagination.current = current;
      prodPagination.pageSize = size;
    },
  });

  function handleProdTableChange(pagination: any) {
    prodPagination.current = pagination.current;
    prodPagination.pageSize = pagination.pageSize;
  }

  function toggleProductSearch() {
    showProductSearch.value = !showProductSearch.value;
  }

  const [registerCompModal, { openModal: openCompModal }] = useModal();
  const [registerProductModal, { openModal: openProductModal }] = useModal();

  const modalTitle = computed(() => {
    if (isDetail.value) return '查看工艺路线';
    return isUpdate.value ? '修改工艺路线' : '添加工艺路线';
  });

  const [registerModal, { closeModal }] = useModalInner(async (data) => {
    isUpdate.value = !!data?.isUpdate;
    isDetail.value = !!data?.isDetail;
    showSubTabs.value = !!data?.isUpdate;
    isAutoCode.value = false;
    manualCodeDraft.value = '';
    activeTabKey.value = 'composition';

    compPagination.current = 1;
    prodPagination.current = 1;

    compositionList.value = [];
    selectedCompRowKeys.value = [];
    selectedCompRows.value = [];

    productList.value = [];
    selectedProdRowKeys.value = [];
    selectedProdRows.value = [];
    productQuery.productId = '';
    productQuery.productName = '';

    if (isUpdate.value && data?.record) {
      formState.id = data.record.id;
      formState.routeCode = data.record.routeCode || '';
      formState.routeName = data.record.routeName || '';
      formState.description = data.record.description || '';
      formState.remark = data.record.remark || '';
      formState.isEnabled = data.record.isEnabled ?? 1;
      await loadCompositions();
    } else {
      formState.id = undefined;
      formState.routeCode = '';
      formState.routeName = '';
      formState.description = '';
      formState.remark = '';
      formState.isEnabled = 1;
    }
    formRef.value?.clearValidate();
  });

  /**
   * 切换自动生成编号开关：调用 /sys/autocode/get/ROUTE_CODE
   */
  async function handleAutoCodeChange(checked: boolean) {
    if (checked) {
      manualCodeDraft.value = formState.routeCode;
      codeLoading.value = true;
      try {
        const nextCode = await getNextRouteCode();
        if (nextCode) {
          formState.routeCode = nextCode;
          formRef.value?.clearValidate(['routeCode']);
        }
      } catch (e) {
        createMessage.error('自动生成工艺路线编号失败，请手动输入');
        isAutoCode.value = false;
      } finally {
        codeLoading.value = false;
      }
    } else {
      formState.routeCode = manualCodeDraft.value || '';
    }
  }

  /**
   * 点击底部【保存】：
   * - 新增模式下：初步保存工艺流程数据，模态框不消失，刷新展开下方的【组成工序】与【关联产品】Tab栏（如【图2】）
   * - 修改模式下：更新工艺流程数据并保持或关闭
   */
  async function handleSave() {
    try {
      await formRef.value.validate();
      saving.value = true;
      const payload = {
        id: formState.id,
        routeCode: formState.routeCode,
        routeName: formState.routeName,
        description: formState.description,
        remark: formState.remark,
        isEnabled: formState.isEnabled ?? 1,
      };
      const isAlreadySaved = !!formState.id;
      const res: any = await saveOrUpdate(payload, isAlreadySaved, true);
      const savedEntity = res?.result || res;
      if (!showSubTabs.value) {
        if (savedEntity && savedEntity.id) {
          formState.id = savedEntity.id;
        }
        showSubTabs.value = true;
        await loadCompositions();
      }
      emit('success');
    } catch (e) {
      // ignore validation error
    } finally {
      saving.value = false;
    }
  }

  function handleClose() {
    closeModal();
  }

  // ==================== 【图2/图3】组成工序 Tab 逻辑 ====================

  async function loadCompositions() {
    if (!formState.id) return;
    compLoading.value = true;
    try {
      const res: any = await listRouteCompositions(formState.id);
      compositionList.value = Array.isArray(res) ? res : res?.result || [];
      selectedCompRowKeys.value = [];
      selectedCompRows.value = [];
    } finally {
      compLoading.value = false;
    }
  }

  function onCompSelectChange(keys: (string | number)[], rows: any[]) {
    selectedCompRowKeys.value = keys;
    selectedCompRows.value = rows;
  }

  function handleAddComposition() {
    const maxSeq = compositionList.value.reduce((max, item) => Math.max(max, Number(item.sequenceNo) || 0), 0);
    openCompModal(true, {
      isUpdate: false,
      routeId: formState.id,
      nextSequenceNo: maxSeq + 1,
      existingCompositions: compositionList.value,
    });
  }

  function handleEditComposition(record: any) {
    openCompModal(true, {
      isUpdate: true,
      routeId: formState.id,
      record,
      existingCompositions: compositionList.value,
    });
  }

  function handleEditSelectedComposition() {
    if (selectedCompRows.value.length === 1) {
      handleEditComposition(selectedCompRows.value[0]);
    }
  }

  async function handleDeleteComposition(record: any) {
    await deleteRouteComposition(record.id);
    createMessage.success('删除成功');
    await loadCompositions();
  }

  function handleBatchDeleteComposition() {
    if (selectedCompRowKeys.value.length === 0) return;
    createConfirm({
      iconType: 'warning',
      title: '确认删除',
      content: `确定删除选中的 ${selectedCompRowKeys.value.length} 道组成工序吗？`,
      onOk: async () => {
        await batchDeleteRouteComposition(selectedCompRowKeys.value.join(','));
        createMessage.success('批量删除成功');
        await loadCompositions();
      },
    });
  }

  // ==================== 【图4/图5】关联产品 Tab 前端构建逻辑 ====================

  function onProdSelectChange(keys: (string | number)[], rows: any[]) {
    selectedProdRowKeys.value = keys;
    selectedProdRows.value = rows;
  }

  function handleSearchProduct() {
    prodPagination.current = 1;
  }

  function handleResetProduct() {
    productQuery.productId = '';
    productQuery.productName = '';
    prodPagination.current = 1;
  }

  function handleAddProduct() {
    openProductModal(true, {
      isUpdate: false,
      routeId: formState.id,
    });
  }

  function handleEditProduct(record: any) {
    openProductModal(true, {
      isUpdate: true,
      routeId: formState.id,
      record,
    });
  }

  function handleEditSelectedProduct() {
    if (selectedProdRows.value.length === 1) {
      handleEditProduct(selectedProdRows.value[0]);
    }
  }

  function handleDeleteProduct(record: any) {
    const idx = productList.value.findIndex((item) => item.id === record.id);
    if (idx !== -1) {
      productList.value.splice(idx, 1);
      createMessage.success('删除成功');
    }
  }

  function handleBatchDeleteProduct() {
    if (selectedProdRowKeys.value.length === 0) return;
    createConfirm({
      iconType: 'warning',
      title: '确认删除',
      content: `确定删除选中的 ${selectedProdRowKeys.value.length} 项关联产品吗？`,
      onOk: () => {
        productList.value = productList.value.filter((item) => !selectedProdRowKeys.value.includes(item.id));
        selectedProdRowKeys.value = [];
        selectedProdRows.value = [];
        createMessage.success('批量删除成功');
      },
    });
  }

  function handleExportProduct() {
    createMessage.info('关联产品导出功能将在后续需求中进一步完善');
  }
</script>

<style lang="less" scoped>
  .process-route-modal-body {
    padding: 16px 24px 12px;
  }

  .code-auto-row {
    display: flex;
    align-items: center;
    gap: 12px;

    .auto-switch-wrap {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-shrink: 0;

      .auto-label {
        font-size: 13px;
        color: #606266;

        &.active {
          color: #1890ff;
        }
      }
    }
  }

  .route-sub-tabs-box {
    margin-top: 8px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    background: #fff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    overflow: hidden;

    :deep(.ant-tabs-card) {
      > .ant-tabs-nav {
        margin-bottom: 0;
        background: #f5f7fa;
        border-bottom: 1px solid #dcdfe6;

        &::before {
          display: none !important;
        }

        .ant-tabs-nav-wrap {
          margin-bottom: -1px;
        }

        .ant-tabs-nav-list {
          padding-left: 0;
        }

        .ant-tabs-tab {
          margin: 0 !important;
          padding: 8px 18px !important;
          background: #f5f7fa;
          border: 1px solid #dcdfe6 !important;
          border-top: none !important;
          border-left: none !important;
          border-radius: 0 !important;
          transition: all 0.2s;

          + .ant-tabs-tab {
            margin-left: 0 !important;
            border-left: 1px solid #dcdfe6 !important;
          }

          .ant-tabs-tab-btn {
            color: #606266;
            font-size: 14px;
          }

          &:hover {
            .ant-tabs-tab-btn {
              color: #1890ff;
            }
          }

          &.ant-tabs-tab-active {
            background: #fff !important;
            border-bottom: 1px solid #fff !important;
            z-index: 2;

            .ant-tabs-tab-btn {
              color: #1890ff !important;
              font-weight: 600;
            }
          }
        }
      }

      .ant-tabs-content-holder {
        background: #fff;
      }
    }

    .sub-tab-content {
      padding: 16px;
    }
  }

  .product-search-bar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 16px;
    margin-bottom: 14px;

    .search-field {
      display: flex;
      align-items: center;
      gap: 8px;

      .field-label {
        font-weight: 600;
        font-size: 13px;
        color: #606266;
        white-space: nowrap;
      }
    }
  }

  .sub-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .sub-toolbar-right {
      display: flex;
      align-items: center;
      gap: 8px;

      :deep(.ant-btn-circle) {
        display: inline-flex;
        align-items: center;
        justify-content: center;
      }
    }
  }

  .btn-add-light {
    color: #1890ff;
    background: #e6f7ff;
    border-color: #91d5ff;
  }

  .btn-edit-light {
    color: #52c41a;
    background: #f6ffed;
    border-color: #b7eb8f;
  }

  .btn-del-light {
    color: #ff4d4f;
    background: #fff1f0;
    border-color: #ffa39e;
  }

  .btn-export-light {
    color: #faad14;
    background: #fffbe6;
    border-color: #ffe58f;
  }

  .modal-footer-btns {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 20px;
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;
  }
</style>
