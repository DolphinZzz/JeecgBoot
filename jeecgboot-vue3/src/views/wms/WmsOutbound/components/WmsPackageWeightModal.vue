<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    title="出库打包与实测称重校验"
    :width="920"
    okText="完成打包称重"
    @ok="handleSubmit"
  >
    <div v-loading="loading">
      <a-descriptions bordered size="small" :column="2" style="margin-bottom: 14px">
        <a-descriptions-item label="出库单号">{{ orderInfo?.outboundCode }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ orderInfo?.warehouseName }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ orderInfo?.customerName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="实拣总件数">
          <span style="font-size: 15px; font-weight: bold; color: #1890ff"> {{ totalPickedCount }} 件 </span>
        </a-descriptions-item>
        <a-descriptions-item label="商品理论净重">
          <span style="font-weight: bold; color: #52c41a"> {{ totalNetWeightKg.toFixed(3) }} kg </span>
          <span style="color: #8c8c8c; margin-left: 6px; font-size: 12px"> (单件加权均重: {{ avgUnitWeightKg.toFixed(3) }} kg/件) </span>
        </a-descriptions-item>
        <a-descriptions-item label="包装皮重合计">
          <span>{{ totalTareWeightKg.toFixed(3) }} kg</span>
          <span style="color: #8c8c8c; margin-left: 6px; font-size: 12px">
            (整单理论总毛重: {{ (totalNetWeightKg + totalTareWeightKg).toFixed(3) }} kg)
          </span>
        </a-descriptions-item>
      </a-descriptions>

      <!-- 理论重量计算公式提示 -->
      <a-alert
        type="info"
        show-icon
        style="margin-bottom: 12px"
        message="理论重量计算公式"
        :description="`本箱理论重量 (kg) = (装箱件数 × 商品均重 ${avgUnitWeightKg.toFixed(3)} kg) + 包装箱皮重。实拣总数 ${totalPickedCount} 件已按商品单件规格精准核算。`"
      />

      <div style="margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center">
        <span style="font-weight: bold; font-size: 14px">包装箱单与称重清单</span>
        <a-button type="primary" size="small" @click="handleAddPackage"> <Icon icon="ant-design:plus-outlined" /> 添加箱号 </a-button>
      </div>

      <a-table size="small" bordered :pagination="false" :columns="packageColumns" :data-source="packageList" row-key="id">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'packageCode'">
            <a-input v-model:value="record.packageCode" placeholder="箱号" />
          </template>

          <template v-else-if="column.key === 'packageType'">
            <a-select v-model:value="record.packageType" style="width: 100%" @change="onPackageTypeChange(record)">
              <a-select-option value="纸箱">纸箱 (0.3kg)</a-select-option>
              <a-select-option value="气泡袋">气泡袋 (0.05kg)</a-select-option>
              <a-select-option value="木箱">木箱 (2.0kg)</a-select-option>
              <a-select-option value="托盘">托盘 (15.0kg)</a-select-option>
              <a-select-option value="无包装">无包装 (0kg)</a-select-option>
            </a-select>
          </template>

          <template v-else-if="column.key === 'packageCount'">
            <a-input-number
              v-model:value="record.packageCount"
              :min="1"
              :max="totalPickedCount || 99999"
              :precision="0"
              style="width: 100%"
              placeholder="装箱件数"
              @change="recalcBoxWeight(record)"
            />
          </template>

          <template v-else-if="column.key === 'tareWeight'">
            <a-input-number
              v-model:value="record.tareWeight"
              :min="0"
              :precision="3"
              :step="0.05"
              style="width: 100%"
              placeholder="皮重kg"
              @change="recalcBoxWeight(record)"
            />
          </template>

          <template v-else-if="column.key === 'theoreticalWeight'">
            <span style="font-weight: bold; color: #1890ff">
              {{ record.theoreticalWeight !== undefined && record.theoreticalWeight !== null ? Number(record.theoreticalWeight).toFixed(3) : '-' }}
            </span>
          </template>

          <template v-else-if="column.key === 'actualWeight'">
            <a-input-number
              v-model:value="record.actualWeight"
              :min="0.001"
              :precision="3"
              :step="0.1"
              style="width: 100%"
              placeholder="实测kg"
              @change="calcDiff(record)"
            />
          </template>

          <template v-else-if="column.key === 'diffWeight'">
            <span :style="{ color: isOverTolerance(record.diffWeight) ? '#ff4d4f' : '#52c41a', fontWeight: 'bold' }">
              {{ formatDiff(record.diffWeight) }}
            </span>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" :disabled="packageList.length <= 1" @click="handleRemovePackage(index)"> 删除 </a-button>
          </template>
        </template>
      </a-table>

      <div v-if="hasLargeDiff" style="margin-top: 12px">
        <a-alert
          type="warning"
          show-icon
          message="称重偏差预警（超差防漏发）"
          description="部分箱件实测重量与理论重量偏差超过允许阈值（±0.5kg 或 ±5%），请仔细复核箱内物料数量及包装，防范漏发、多发风险！"
        />
      </div>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { Icon } from '/@/components/Icon';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { packageAndWeight, queryDetails } from '../WmsOutbound.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);
  const loading = ref(false);
  const orderInfo = ref<any>(null);
  const detailList = ref<any[]>([]);
  const packageList = ref<any[]>([]);

  // 包装类型默认皮重 (kg)
  const TARE_WEIGHT_MAP: Record<string, number> = {
    纸箱: 0.3,
    气泡袋: 0.05,
    木箱: 2.0,
    托盘: 15.0,
    无包装: 0.0,
  };

  const packageColumns = [
    { title: '箱号/包条码', key: 'packageCode', width: 160 },
    { title: '包装类型', key: 'packageType', width: 130 },
    { title: '装箱件数', key: 'packageCount', width: 95, align: 'center' },
    { title: '箱皮重(kg)', key: 'tareWeight', width: 95, align: 'center' },
    { title: '理论重量(kg)', key: 'theoreticalWeight', width: 110, align: 'center' },
    { title: '实测重量(kg)', key: 'actualWeight', width: 115, align: 'center' },
    { title: '重量偏差', key: 'diffWeight', width: 100, align: 'center' },
    { title: '操作', key: 'action', width: 65, align: 'center' },
  ];

  /** 实拣总件数 */
  const totalPickedCount = computed(() => {
    if (detailList.value && detailList.value.length > 0) {
      return detailList.value.reduce((sum, d) => sum + Number(d.pickedCount ?? d.allocatedCount ?? d.outCount ?? 0), 0);
    }
    return Number(orderInfo.value?.pickedTotal ?? orderInfo.value?.pickedCount ?? orderInfo.value?.totalCount ?? 0);
  });

  /** 商品理论净重合计 (kg) */
  const totalNetWeightKg = computed(() => {
    if (!detailList.value || detailList.value.length === 0) {
      return 0;
    }
    return detailList.value.reduce((sum, d) => {
      const count = Number(d.pickedCount ?? d.allocatedCount ?? d.outCount ?? 0);
      let unitKg = Number(d.unitWeightKg || 0);
      if (unitKg <= 0 && d.productWeight) {
        const unit = (d.weightUnit || 'g').toLowerCase().trim();
        if (unit === 'kg') {
          unitKg = Number(d.productWeight);
        } else if (unit === 'mg') {
          unitKg = Number(d.productWeight) / 1000000;
        } else {
          unitKg = Number(d.productWeight) / 1000;
        }
      }
      return sum + count * unitKg;
    }, 0);
  });

  /** 单件商品加权均重 (kg/件) */
  const avgUnitWeightKg = computed(() => {
    const totalCount = totalPickedCount.value;
    if (totalCount > 0 && totalNetWeightKg.value > 0) {
      return totalNetWeightKg.value / totalCount;
    }
    return 0;
  });

  /** 包装箱皮重合计 (kg) */
  const totalTareWeightKg = computed(() => {
    return packageList.value.reduce((sum, p) => sum + Number(p.tareWeight || 0), 0);
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    orderInfo.value = data?.record || {};
    detailList.value = [];
    packageList.value = [];
    setModalProps({ confirmLoading: false });

    if (orderInfo.value?.id) {
      loading.value = true;
      try {
        const res: any = await queryDetails(orderInfo.value.id);
        detailList.value = res || [];
      } catch (e) {
        console.error('获取出库单明细失败', e);
      } finally {
        loading.value = false;
      }
    }

    // 初始化默认单箱：包含全部实拣件数
    initDefaultPackage();
  });

  function initDefaultPackage() {
    const totalCount = totalPickedCount.value || 1;
    const defaultTare = TARE_WEIGHT_MAP['纸箱'] || 0.3;
    const netWeight = totalNetWeightKg.value > 0 ? totalNetWeightKg.value : totalCount * avgUnitWeightKg.value;
    const theoretical = Number((netWeight + defaultTare).toFixed(3));

    packageList.value = [
      {
        id: '1',
        packageCode: `${orderInfo.value?.outboundCode || 'OB'}-BOX01`,
        packageType: '纸箱',
        packageCount: totalCount,
        tareWeight: defaultTare,
        theoreticalWeight: theoretical,
        actualWeight: theoretical,
        diffWeight: 0,
      },
    ];
  }

  function onPackageTypeChange(record: any) {
    const defaultTare = TARE_WEIGHT_MAP[record.packageType] ?? 0.3;
    record.tareWeight = defaultTare;
    recalcBoxWeight(record);
  }

  function recalcBoxWeight(record: any) {
    const count = Number(record.packageCount || 0);
    const tare = Number(record.tareWeight || 0);
    const unitKg = avgUnitWeightKg.value;
    const theoretical = Number((count * unitKg + tare).toFixed(3));
    record.theoreticalWeight = theoretical;

    // 若实测重量未录入或等于原理论重量，则自动跟随
    if (!record.actualWeight || record.diffWeight === 0) {
      record.actualWeight = theoretical;
      record.diffWeight = 0;
    } else {
      calcDiff(record);
    }
  }

  function calcDiff(record: any) {
    if (record.actualWeight !== undefined && record.actualWeight !== null && record.theoreticalWeight !== undefined) {
      record.diffWeight = Number((Number(record.actualWeight) - Number(record.theoreticalWeight)).toFixed(3));
    }
  }

  function formatDiff(diff: number | undefined | null) {
    if (diff === undefined || diff === null) return '-';
    if (diff > 0) return `+${diff.toFixed(3)} kg`;
    return `${diff.toFixed(3)} kg`;
  }

  function isOverTolerance(diff: number | undefined | null) {
    return Math.abs(diff || 0) > 0.5;
  }

  const hasLargeDiff = computed(() => {
    return packageList.value.some((p) => isOverTolerance(p.diffWeight));
  });

  function handleAddPackage() {
    const seq = packageList.value.length + 1;
    const seqStr = seq < 10 ? `0${seq}` : `${seq}`;
    const defaultTare = TARE_WEIGHT_MAP['纸箱'] || 0.3;

    // 默认新箱分得件数
    let assignedCount = 1;
    const currentAssigned = packageList.value.reduce((sum, p) => sum + Number(p.packageCount || 0), 0);
    const remain = totalPickedCount.value - currentAssigned;
    if (remain > 0) {
      assignedCount = remain;
    }

    const theoretical = Number((assignedCount * avgUnitWeightKg.value + defaultTare).toFixed(3));
    packageList.value.push({
      id: String(Date.now()),
      packageCode: `${orderInfo.value?.outboundCode || 'OB'}-BOX${seqStr}`,
      packageType: '纸箱',
      packageCount: assignedCount,
      tareWeight: defaultTare,
      theoreticalWeight: theoretical,
      actualWeight: theoretical,
      diffWeight: 0,
    });
  }

  function handleRemovePackage(index: number) {
    packageList.value.splice(index, 1);
  }

  async function handleSubmit() {
    if (!packageList.value || packageList.value.length === 0) {
      createMessage.warning('包装箱单清单不能为空！');
      return;
    }

    for (let i = 0; i < packageList.value.length; i++) {
      const p = packageList.value[i];
      const boxNo = i + 1;
      if (!p.packageCode || !p.packageCode.trim()) {
        createMessage.warning(`第 ${boxNo} 箱的箱号不能为空！`);
        return;
      }
      if (!p.packageCount || p.packageCount <= 0) {
        createMessage.warning(`第 ${boxNo} 箱装箱件数必须大于 0！`);
        return;
      }
      if (!p.actualWeight || p.actualWeight <= 0) {
        createMessage.warning(`第 ${boxNo} 箱实测重量必须大于 0！`);
        return;
      }
    }

    const totalActualWeightKg = packageList.value.reduce((sum, p) => sum + Number(p.actualWeight || 0), 0);

    const packages = packageList.value.map((p) => ({
      packageCode: p.packageCode.trim(),
      packageType: p.packageType,
      packageCount: Number(p.packageCount || 0),
      skuId: detailList.value.length > 0 ? detailList.value[0].skuId : null,
      detailId: detailList.value.length > 0 ? detailList.value[0].id : null,
    }));

    const weights = packageList.value.map((p) => ({
      packageCode: p.packageCode.trim(),
      packageType: p.packageType,
      packageCount: Number(p.packageCount || 0),
      actualWeight: Number(p.actualWeight),
      theoreticalWeight: Number(p.theoreticalWeight || 0),
      diffWeight: Number(p.diffWeight || 0),
    }));

    setModalProps({ confirmLoading: true });
    try {
      await packageAndWeight({
        outboundOrderId: orderInfo.value.id,
        actualWeight: Math.round(totalActualWeightKg * 1000), // 克
        actualWeightKg: Number(totalActualWeightKg.toFixed(3)), // 千克
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
