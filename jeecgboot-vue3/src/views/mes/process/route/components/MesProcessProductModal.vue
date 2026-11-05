<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    :title="modalTitle"
    :width="860"
    :showOkBtn="false"
    :showCancelBtn="false"
  >
    <div class="product-process-form-wrap">
      <a-form ref="formRef" :model="formState" :label-col="{ style: { width: '105px' } }">
        <a-row :gutter="16" align="middle">
          <!-- 产品物料编码 + 放大镜选择按钮 -->
          <a-col :span="9">
            <a-form-item label="产品物料编码" name="productId">
              <a-input-group compact class="product-code-group">
                <a-input
                  v-model:value="formState.productId"
                  placeholder="请选择产品"
                  readonly
                  class="product-code-input"
                />
                <a-button class="product-search-btn" preIcon="ant-design:search-outlined" @click="handleSelectProduct" />
              </a-input-group>
            </a-form-item>
          </a-col>

          <!-- 产品物料名称 -->
          <a-col :span="9">
            <a-form-item label="产品物料名称" name="productName">
              <a-input v-model:value="formState.productName" placeholder="请选择产品" disabled />
            </a-form-item>
          </a-col>

          <!-- 单位 -->
          <a-col :span="6">
            <a-form-item label="单位" name="unit" :label-col="{ style: { width: '55px' } }">
              <a-input v-model:value="formState.unit" placeholder="请选择产品" disabled />
            </a-form-item>
          </a-col>

          <!-- 规格型号 -->
          <a-col :span="24">
            <a-form-item label="规格型号" name="specification">
              <a-textarea v-model:value="formState.specification" :rows="3" placeholder="请选择产品" disabled />
            </a-form-item>
          </a-col>

          <!-- 备注 -->
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="formState.remark" :rows="3" placeholder="请输入内容" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <!-- 底部按钮 -->
      <div class="modal-footer-btns">
        <a-button type="primary" @click="handleOk">确 定</a-button>
        <a-button @click="closeModal">取 消</a-button>
      </div>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, reactive, computed } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';

  const emit = defineEmits(['register', 'success']);
  const { createMessage } = useMessage();

  const formRef = ref();
  const isUpdate = ref(false);

  const formState = reactive<any>({
    id: undefined,
    productId: '',
    productName: '',
    unit: '',
    specification: '',
    remark: '',
  });

  const modalTitle = computed(() => (isUpdate.value ? '修改产品制程' : '添加产品制程'));

  const [registerModal, { closeModal }] = useModalInner(async (data) => {
    isUpdate.value = !!data?.isUpdate;
    if (isUpdate.value && data?.record) {
      formState.id = data.record.id;
      formState.productId = data.record.productId || '';
      formState.productName = data.record.productName || '';
      formState.unit = data.record.unit || '';
      formState.specification = data.record.specification || '';
      formState.remark = data.record.remark || '';
    } else {
      formState.id = undefined;
      formState.productId = '';
      formState.productName = '';
      formState.unit = '';
      formState.specification = '';
      formState.remark = '';
    }
  });

  function handleSelectProduct() {
    createMessage.info('产品物料选择功能将在后续需求中进一步完善');
  }

  function handleOk() {
    closeModal();
    emit('success', { ...formState });
  }
</script>

<style lang="less" scoped>
  .product-process-form-wrap {
    padding: 18px 24px 8px;
  }

  .product-code-group {
    display: flex !important;
    width: 100%;

    .product-code-input {
      flex: 1;
      border-top-right-radius: 0 !important;
      border-bottom-right-radius: 0 !important;
    }

    .product-search-btn {
      width: 44px;
      background: #f5f7fa;
      color: #909399;
      border-top-left-radius: 0 !important;
      border-bottom-left-radius: 0 !important;
    }
  }

  .modal-footer-btns {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    padding-top: 12px;
  }
</style>
