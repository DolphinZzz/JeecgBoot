<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    title="自动分波生成"
    :width="600"
    okText="开始自动分波"
    @ok="handleSubmit"
  >
    <a-alert
      type="info"
      show-icon
      message="波次智能聚合规则"
      description="系统将扫描指定仓库内所有【已分配锁库】且拣货模式为【波次播种法】的待出库单，严格按照「物流承运商 + 存储温区」双维度智能聚合分组生成波次拣货任务，避免冷热混装与物流交接拆单。"
      style="margin-bottom: 20px"
    />

    <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="目标仓库" required>
        <j-search-select
          v-model:value="warehouseId"
          dict="wms_warehouse,name,id"
          placeholder="请选择需要分波的仓库"
        />
      </a-form-item>
    </a-form>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import JSearchSelect from '/@/components/Form/src/jeecg/components/JSearchSelect.vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { generateWaves } from '../WmsWave.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);
  const warehouseId = ref<number | undefined>(undefined);

  const [registerModal, { setModalProps, closeModal }] = useModalInner(() => {
    warehouseId.value = undefined;
    setModalProps({ confirmLoading: false });
  });

  async function handleSubmit() {
    if (!warehouseId.value) {
      createMessage.warning('请选择目标仓库！');
      return;
    }

    setModalProps({ confirmLoading: true });
    try {
      const res = await generateWaves({ warehouseId: warehouseId.value });
      const count = Array.isArray(res) ? res.length : 0;
      createMessage.success(`自动分波完成！成功生成 ${count} 个波次任务单`);
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
