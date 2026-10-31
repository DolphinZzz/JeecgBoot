<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    title="出库打包与实测称重校验"
    :width="850"
    okText="完成打包称重"
    @ok="handleSubmit"
  >
    <a-descriptions bordered size="small" :column="2" style="margin-bottom: 16px">
      <a-descriptions-item label="出库单号">{{ orderInfo?.outboundCode }}</a-descriptions-item>
      <a-descriptions-item label="仓库">{{ orderInfo?.warehouseName }}</a-descriptions-item>
      <a-descriptions-item label="客户">{{ orderInfo?.customerName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="实拣总件数">{{ orderInfo?.pickedTotal ?? orderInfo?.pickedCount ?? 0 }}</a-descriptions-item>
    </a-descriptions>

    <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center">
      <span style="font-weight: bold; font-size: 14px">包装箱单与称重清单</span>
      <a-button type="primary" size="small" @click="handleAddPackage">
        <Icon icon="ant-design:plus-outlined" /> 添加箱号
      </a-button>
    </div>

    <a-table
      size="small"
      bordered
      :pagination="false"
      :columns="packageColumns"
      :data-source="packageList"
      row-key="id"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'packageCode'">
          <a-input v-model:value="record.packageCode" placeholder="箱号" />
        </template>
        <template v-else-if="column.key === 'packageType'">
          <a-select v-model:value="record.packageType" style="width: 100%">
            <a-select-option value="纸箱">纸箱</a-select-option>
            <a-select-option value="木箱">木箱</a-select-option>
            <a-select-option value="托盘">托盘</a-select-option>
            <a-select-option value="气泡袋">气泡袋</a-select-option>
          </a-select>
        </template>
        <template v-else-if="column.key === 'actualWeight'">
          <a-input-number
            v-model:value="record.actualWeight"
            :min="0.01"
            :precision="3"
            style="width: 100%"
            placeholder="实测kg"
            @change="calcDiff(record)"
          />
        </template>
        <template v-else-if="column.key === 'diffWeight'">
          <span :style="{ color: Math.abs(record.diffWeight || 0) > 0.5 ? '#ff4d4f' : '#52c41a', fontWeight: 'bold' }">
            {{ record.diffWeight !== undefined && record.diffWeight !== null ? `${record.diffWeight} kg` : '-' }}
          </span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" danger size="small" :disabled="packageList.length <= 1" @click="handleRemovePackage(index)">
            删除
          </a-button>
        </template>
      </template>
    </a-table>

    <div v-if="hasLargeDiff" style="margin-top: 12px">
      <a-alert
        type="warning"
        show-icon
        message="称重偏差预警"
        description="部分箱件实测重量与理论重量偏差较大，请仔细复核箱内物料，防范漏发、重发风险！"
      />
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { Icon } from '/@/components/Icon';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { packageAndWeight } from '../WmsOutbound.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);
  const orderInfo = ref<any>(null);
  const packageList = ref<any[]>([]);

  const packageColumns = [
    { title: '箱号/包条码', key: 'packageCode', width: 180 },
    { title: '包装类型', key: 'packageType', width: 140 },
    { title: '实测重量 (kg)', key: 'actualWeight', width: 150 },
    { title: '理论重量 (kg)', dataIndex: 'theoreticalWeight', width: 120, align: 'center' },
    { title: '重量偏差', key: 'diffWeight', width: 130, align: 'center' },
    { title: '操作', key: 'action', width: 80, align: 'center' },
  ];

  const [registerModal, { setModalProps, closeModal }] = useModalInner((data) => {
    orderInfo.value = data?.record || {};
    const defaultTheoretical = 1.0;
    packageList.value = [
      {
        id: '1',
        packageCode: `${orderInfo.value.outboundCode || 'OB'}-BOX01`,
        packageType: '纸箱',
        actualWeight: defaultTheoretical,
        theoreticalWeight: defaultTheoretical,
        diffWeight: 0,
      },
    ];
    setModalProps({ confirmLoading: false });
  });

  function handleAddPackage() {
    const seq = packageList.value.length + 1;
    const seqStr = seq < 10 ? `0${seq}` : `${seq}`;
    packageList.value.push({
      id: String(Date.now()),
      packageCode: `${orderInfo.value.outboundCode || 'OB'}-BOX${seqStr}`,
      packageType: '纸箱',
      actualWeight: 1.0,
      theoreticalWeight: 1.0,
      diffWeight: 0,
    });
  }

  function handleRemovePackage(index: number) {
    packageList.value.splice(index, 1);
  }

  function calcDiff(record: any) {
    if (record.actualWeight !== undefined && record.theoreticalWeight !== undefined) {
      record.diffWeight = Number((record.actualWeight - record.theoreticalWeight).toFixed(3));
    }
  }

  const hasLargeDiff = computed(() => {
    return packageList.value.some((p) => Math.abs(p.diffWeight || 0) > 0.5);
  });

  async function handleSubmit() {
    for (const p of packageList.value) {
      if (!p.packageCode || !p.packageCode.trim()) {
        createMessage.warning('箱号不能为空！');
        return;
      }
      if (!p.actualWeight || p.actualWeight <= 0) {
        createMessage.warning('实测重量必须大于 0！');
        return;
      }
    }

    const packages = packageList.value.map((p) => ({
      packageCode: p.packageCode.trim(),
      packageType: p.packageType,
      skuIds: [],
    }));

    const weights = packageList.value.map((p) => ({
      packageCode: p.packageCode.trim(),
      actualWeight: Number(p.actualWeight),
      theoreticalWeight: Number(p.theoreticalWeight || 0),
      diffWeight: Number(p.diffWeight || 0),
    }));

    setModalProps({ confirmLoading: true });
    try {
      await packageAndWeight({
        outboundOrderId: orderInfo.value.id,
        packages,
        weights,
      });
      createMessage.success('打包与实测称重成功！单据进入待发货状态');
      closeModal();
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
