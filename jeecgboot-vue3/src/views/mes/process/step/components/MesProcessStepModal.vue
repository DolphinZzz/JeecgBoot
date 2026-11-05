<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    :title="modalTitle"
    :width="920"
    :showOkBtn="false"
    :showCancelBtn="false"
  >
    <div class="process-step-modal-body">
      <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ style: { width: '80px' } }">
        <a-row :gutter="24" align="middle">
          <!-- 工序编码 + 自动生成开关 -->
          <a-col :span="12">
            <a-form-item label="工序编码" name="processCode">
              <div class="code-auto-row">
                <a-input
                  v-model:value="formState.processCode"
                  :disabled="isDetail || isAutoCode"
                  placeholder="请输入工序编码"
                  allowClear
                />
                <div class="auto-switch-wrap">
                  <a-switch
                    v-model:checked="isAutoCode"
                    :loading="codeLoading"
                    :disabled="isDetail || isUpdate"
                    @change="handleAutoCodeChange"
                  />
                  <span class="auto-label" :class="{ active: isAutoCode }">自动生成</span>
                </div>
              </div>
            </a-form-item>
          </a-col>

          <!-- 工序名称 -->
          <a-col :span="12">
            <a-form-item label="工序名称" name="processName">
              <a-input v-model:value="formState.processName" :disabled="isDetail" placeholder="请输入工序名称" allowClear />
            </a-form-item>
          </a-col>

          <!-- 是否启用 -->
          <a-col :span="12">
            <a-form-item label="是否启用" name="isEnabled">
              <a-radio-group v-model:value="formState.isEnabled" :disabled="isDetail">
                <a-radio :value="1">是</a-radio>
                <a-radio :value="2">否</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>

          <!-- 工序说明 -->
          <a-col :span="24">
            <a-form-item label="工序说明" name="description">
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

      <!-- 保存后原地展开【操作步骤】Tab -->
      <div v-if="isUpdate && formState.id" class="sub-tabs-container">
        <a-tabs v-model:activeKey="activeTabKey" type="card">
          <a-tab-pane key="operation" tab="操作步骤">
            <div class="sub-table-toolbar" v-if="!isDetail">
              <a-space>
                <a-button
                  class="btn-add-light"
                  size="small"
                  preIcon="ant-design:plus-outlined"
                  @click="handleAddOperation"
                >
                  新增
                </a-button>
                <a-button
                  class="btn-edit-light"
                  size="small"
                  preIcon="ant-design:edit-outlined"
                  :disabled="selectedOpRowKeys.length !== 1"
                  @click="handleEditSelectedOperation"
                >
                  修改
                </a-button>
                <a-button
                  class="btn-del-light"
                  size="small"
                  preIcon="ant-design:delete-outlined"
                  :disabled="selectedOpRowKeys.length === 0"
                  @click="handleBatchDeleteOperation"
                >
                  删除
                </a-button>
              </a-space>
              <div class="toolbar-right">
                <a-tooltip title="刷新">
                  <a-button
                    shape="circle"
                    size="small"
                    preIcon="ant-design:reload-outlined"
                    @click="loadOperations"
                  />
                </a-tooltip>
              </div>
            </div>

            <a-table
              size="small"
              rowKey="id"
              :loading="opLoading"
              :columns="operationColumns"
              :dataSource="operationList"
              :pagination="opPagination"
              @change="handleOpTableChange"
              :rowSelection="
                isDetail
                  ? undefined
                  : {
                      selectedRowKeys: selectedOpRowKeys,
                      onChange: onOpSelectChange,
                    }
              "
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'action'">
                  <a-space v-if="!isDetail">
                    <a @click="handleEditOperation(record)">修改</a>
                    <a-popconfirm title="是否确认删除该操作步骤？" @confirm="handleDeleteOperation(record)">
                      <a style="color: #ff4d4f">删除</a>
                    </a-popconfirm>
                  </a-space>
                  <span v-else>-</span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </div>

      <!-- 底部按钮 -->
      <div class="modal-footer-btns">
        <a-button v-if="!isDetail" type="primary" :loading="saving" @click="handleSave">保 存</a-button>
        <a-button @click="handleClose">关 闭</a-button>
      </div>
    </div>

    <!-- 添加/修改操作步骤子模态框 -->
    <MesProcessStepOperationModal @register="registerOpModal" @success="loadOperations" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, reactive, computed } from 'vue';
  import { BasicModal, useModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { operationColumns } from '../MesProcessStep.data';
  import {
    getNextProcessCode,
    saveOrUpdate,
    listStepOperations,
    deleteStepOperation,
    batchDeleteStepOperation,
  } from '../MesProcessStep.api';
  import MesProcessStepOperationModal from './MesProcessStepOperationModal.vue';

  const emit = defineEmits(['register', 'success']);
  const { createMessage, createConfirm } = useMessage();

  const formRef = ref();
  const isUpdate = ref(false);
  const isDetail = ref(false);
  const saving = ref(false);
  const isAutoCode = ref(false);
  const codeLoading = ref(false);
  const manualCodeDraft = ref('');
  const activeTabKey = ref('operation');

  const formState = reactive<any>({
    id: undefined,
    processCode: '',
    processName: '',
    isEnabled: 1,
    description: '',
    remark: '',
  });

  const formRules = {
    processCode: [{ required: true, message: '请输入工序编码', trigger: 'blur' }],
    processName: [{ required: true, message: '请输入工序名称', trigger: 'blur' }],
  };

  // 操作步骤明细列表状态与分页
  const opLoading = ref(false);
  const operationList = ref<any[]>([]);
  const selectedOpRowKeys = ref<(string | number)[]>([]);
  const selectedOpRows = ref<any[]>([]);

  const opPagination = reactive({
    current: 1,
    pageSize: 10,
    pageSizeOptions: ['10', '20', '30'],
    showTotal: (total: number, range: [number, number]) => `${range[0]}-${range[1]} 共 ${total} 条`,
    showQuickJumper: true,
    showSizeChanger: true,
    size: 'small',
    onChange: (page: number, pageSize: number) => {
      opPagination.current = page;
      opPagination.pageSize = pageSize;
    },
    onShowSizeChange: (current: number, size: number) => {
      opPagination.current = current;
      opPagination.pageSize = size;
    },
  });

  function handleOpTableChange(pagination: any) {
    opPagination.current = pagination.current;
    opPagination.pageSize = pagination.pageSize;
  }

  const [registerOpModal, { openModal: openOpModal }] = useModal();

  const modalTitle = computed(() => {
    if (isDetail.value) return '查看生产工序';
    return isUpdate.value ? '修改生产工序' : '添加生产工序';
  });

  const [registerModal, { closeModal }] = useModalInner(async (data) => {
    isUpdate.value = !!data?.isUpdate;
    isDetail.value = !!data?.isDetail;
    isAutoCode.value = false;
    manualCodeDraft.value = '';
    activeTabKey.value = 'operation';
    opPagination.current = 1;
    selectedOpRowKeys.value = [];
    selectedOpRows.value = [];
    operationList.value = [];

    if (isUpdate.value && data?.record) {
      formState.id = data.record.id;
      formState.processCode = data.record.processCode || '';
      formState.processName = data.record.processName || '';
      formState.isEnabled = data.record.isEnabled ?? 1;
      formState.description = data.record.description || '';
      formState.remark = data.record.remark || '';
      await loadOperations();
    } else {
      formState.id = undefined;
      formState.processCode = '';
      formState.processName = '';
      formState.isEnabled = 1;
      formState.description = '';
      formState.remark = '';
    }
    formRef.value?.clearValidate();
  });

  async function handleAutoCodeChange(checked: boolean) {
    if (checked) {
      manualCodeDraft.value = formState.processCode;
      codeLoading.value = true;
      try {
        const nextCode = await getNextProcessCode();
        if (nextCode) {
          formState.processCode = nextCode;
          formRef.value?.clearValidate(['processCode']);
        }
      } catch (e) {
        createMessage.error('自动生成工序编码失败，请手动输入');
        isAutoCode.value = false;
      } finally {
        codeLoading.value = false;
      }
    } else {
      formState.processCode = manualCodeDraft.value || '';
    }
  }

  /**
   * 保存工序主表：
   * - 新增首次保存后，不关闭弹窗，切换为修改模式并展示下方的【操作步骤】Tab
   * - 已经在修改模式时再次点击保存，更新主表并提示成功
   */
  async function handleSave() {
    try {
      await formRef.value.validate();
      saving.value = true;
      const payload = {
        id: formState.id,
        processCode: formState.processCode,
        processName: formState.processName,
        isEnabled: formState.isEnabled,
        description: formState.description,
        remark: formState.remark,
      };
      const res: any = await saveOrUpdate(payload, isUpdate.value, true);
      const savedEntity = res?.result || res;
      if (!isUpdate.value) {
        if (savedEntity && savedEntity.id) {
          formState.id = savedEntity.id;
        }
        isUpdate.value = true;
        await loadOperations();
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

  // ==================== 操作步骤子表操作 ====================

  async function loadOperations() {
    if (!formState.id) return;
    opLoading.value = true;
    try {
      const res: any = await listStepOperations(formState.id);
      operationList.value = Array.isArray(res) ? res : res?.result || [];
      selectedOpRowKeys.value = [];
      selectedOpRows.value = [];
    } finally {
      opLoading.value = false;
    }
  }

  function onOpSelectChange(keys: (string | number)[], rows: any[]) {
    selectedOpRowKeys.value = keys;
    selectedOpRows.value = rows;
  }

  function handleAddOperation() {
    const maxOrder = operationList.value.reduce((max, item) => Math.max(max, Number(item.stepOrder) || 0), 0);
    openOpModal(true, {
      isUpdate: false,
      processStepId: formState.id,
      nextStepOrder: maxOrder + 1,
    });
  }

  function handleEditOperation(record: any) {
    openOpModal(true, {
      isUpdate: true,
      processStepId: formState.id,
      record,
    });
  }

  function handleEditSelectedOperation() {
    if (selectedOpRows.value.length === 1) {
      handleEditOperation(selectedOpRows.value[0]);
    }
  }

  async function handleDeleteOperation(record: any) {
    await deleteStepOperation(record.id);
    createMessage.success('删除成功');
    await loadOperations();
  }

  function handleBatchDeleteOperation() {
    if (selectedOpRowKeys.value.length === 0) return;
    createConfirm({
      iconType: 'warning',
      title: '确认删除',
      content: `确定删除选中的 ${selectedOpRowKeys.value.length} 条操作步骤吗？`,
      onOk: async () => {
        await batchDeleteStepOperation(selectedOpRowKeys.value.join(','));
        createMessage.success('批量删除成功');
        await loadOperations();
      },
    });
  }
</script>

<style lang="less" scoped>
  .process-step-modal-body {
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

  .sub-tabs-container {
    margin-top: 8px;
    border: 1px solid #e8e8e8;
    border-radius: 4px;
    padding: 12px 16px 16px;
    background: #fff;
  }

  .sub-table-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
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

  .modal-footer-btns {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 20px;
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;
  }
</style>
