<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    title="整库区批量迁移（清仓腾挪）"
    :width="1160"
    @ok="handleSubmit"
    :okButtonProps="{ disabled: !previewData?.canMigrate || migrationDetails.length === 0 }"
  >
    <!-- 顶部操作指引与仓库库区选择栏 -->
    <a-card size="small" :bordered="false" style="background: #f7f9fc; margin-bottom: 12px; border-radius: 6px">
      <div style="margin-bottom: 10px; color: #606266; font-size: 13px">
        <span style="font-weight: bold; color: #1890ff">💡 业务说明：</span>
        选择源库区与目标库区，系统将自动把源库区内所有有货库位的在库商品一对一智能映射并移入目标库区空闲库位。移库成功后，源库区库位将全部清空腾空，恢复自由开放状态。
      </div>

      <a-row :gutter="16" align="middle">
        <!-- 迁出源端 -->
        <a-col :span="10">
          <a-card size="small" title="📦 迁出源库区" style="border: 1px dashed #ffa39e; border-radius: 4px">
            <a-form layout="vertical">
              <a-form-item label="源仓库" required style="margin-bottom: 8px">
                <a-select
                  v-model:value="sourceWarehouseId"
                  placeholder="请选择源仓库"
                  :options="warehouseOptions"
                  @change="handleSourceWarehouseChange"
                  allowClear
                />
              </a-form-item>
              <a-form-item label="源库区" required style="margin-bottom: 0">
                <a-select
                  v-model:value="sourceWarehouseAreaId"
                  placeholder="请选择需要腾空的源库区"
                  :options="sourceAreaOptions"
                  @change="handleAreaChange"
                  :disabled="!sourceWarehouseId"
                  allowClear
                />
              </a-form-item>
            </a-form>
          </a-card>
        </a-col>

        <!-- 中间流向箭头与预检触发 -->
        <a-col :span="4" style="text-align: center">
          <div style="font-size: 24px; color: #1890ff; margin-bottom: 8px">➔</div>
          <a-button
            type="primary"
            preIcon="ant-design:scan-outlined"
            :loading="scanning"
            :disabled="!canScan"
            @click="handleScanAndPreview"
          >
            智能预检配对
          </a-button>
        </a-col>

        <!-- 迁入目标端 -->
        <a-col :span="10">
          <a-card size="small" title="🎯 迁入目标库区" style="border: 1px dashed #91d5ff; border-radius: 4px">
            <a-form layout="vertical">
              <a-form-item label="目标仓库" required style="margin-bottom: 8px">
                <a-select
                  v-model:value="targetWarehouseId"
                  placeholder="请选择目标仓库"
                  :options="warehouseOptions"
                  @change="handleTargetWarehouseChange"
                  allowClear
                />
              </a-form-item>
              <a-form-item label="目标库区" required style="margin-bottom: 0">
                <a-select
                  v-model:value="targetWarehouseAreaId"
                  placeholder="请选择接收货物的目标库区"
                  :options="targetAreaOptions"
                  @change="handleAreaChange"
                  :disabled="!targetWarehouseId"
                  allowClear
                />
              </a-form-item>
            </a-form>
          </a-card>
        </a-col>
      </a-row>
    </a-card>

    <!-- 预检与匹配概览提示横幅（含缺口库位高亮展示） -->
    <a-alert
      v-if="previewData"
      :type="previewData.canMigrate ? 'success' : 'warning'"
      showIcon
      style="margin-bottom: 12px"
    >
      <template #message>
        <span style="font-weight: bold; font-size: 14px">{{ previewData.message }}</span>
      </template>
      <template #description>
        <div style="margin-top: 6px; display: flex; flex-direction: column; gap: 6px; font-size: 13px">
          <div style="display: flex; gap: 24px; align-items: center">
            <span>需腾迁源库位：<b style="color: #fa541c">{{ previewData.sourceOccupiedLocaCount ?? 0 }}</b> 个</span>
            <span>在库总件数：<b style="color: #1890ff">{{ previewData.sourceTotalItemCount ?? 0 }}</b> 件</span>
            <span>目标可用空库位：<b style="color: #52c41a">{{ previewData.targetEmptyLocaCount ?? 0 }}</b> 个</span>
            <span>
              配对结论：
              <a-tag :color="previewData.canMigrate ? 'green' : 'red'">
                {{ previewData.canMigrate ? '✓ 空位充足，已全部自动配对' : '✕ 目标库区空位不足，无法直接全迁' }}
              </a-tag>
            </span>
          </div>

          <!-- 缺口库位直观清单：清晰指出缺少哪些库位 -->
          <div
            v-if="previewData.missingLocaCount && previewData.missingLocaCount > 0"
            style="background: #fff1f0; border: 1px solid #ffccc7; padding: 6px 10px; border-radius: 4px; margin-top: 4px"
          >
            <div style="font-weight: bold; color: #cf1322; margin-bottom: 4px">
              ⚠️ 缺少目标库位缺口（共缺少 {{ previewData.missingLocaCount }} 个空位，以下源库位未能匹配到目标空位）：
            </div>
            <div style="display: flex; flex-wrap: wrap; gap: 6px">
              <a-tag
                v-for="code in previewData.missingSourceLocas"
                :key="code"
                color="error"
                style="font-weight: bold; font-size: 12px"
              >
                🔴 缺目标库位：{{ code }}
              </a-tag>
            </div>
          </div>
        </div>
      </template>
    </a-alert>

    <!-- 迁移映射明细预览表头与快速状态过滤栏 -->
    <div style="margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center">
      <div style="font-weight: bold; color: #303133; font-size: 14px">
        📋 库区一对一迁移映射明细（确认目标分配位置）：
      </div>
      <div v-if="migrationDetails.length > 0">
        <a-radio-group v-model:value="filterStatus" size="small" buttonStyle="solid">
          <a-radio-button value="ALL">全部库位 ({{ migrationDetails.length }})</a-radio-button>
          <a-radio-button value="MATCHED">
            <span style="color: #52c41a">✓ 已匹配 ({{ matchedCount }})</span>
          </a-radio-button>
          <a-radio-button value="MISSING" :disabled="missingCount === 0">
            <span :style="{ color: missingCount > 0 ? '#ff4d4f' : 'inherit', fontWeight: missingCount > 0 ? 'bold' : 'normal' }">
              🔴 缺少目标库位 ({{ missingCount }})
            </span>
          </a-radio-button>
        </a-radio-group>
      </div>
    </div>

    <!-- 迁移映射明细预览表 -->
    <a-table
      size="small"
      bordered
      rowKey="sourceWarehouseAreaLocaId"
      :columns="tableColumns"
      :dataSource="displayedDetails"
      :pagination="{ pageSize: 5 }"
      :scroll="{ y: 260 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'direction'">
          <span style="color: #1890ff; font-weight: bold">➔ 移入</span>
        </template>
        <template v-else-if="column.key === 'targetLoca'">
          <div v-if="record.matchStatus === 'MATCHED'">
            <a-select
              v-model:value="record.targetWarehouseAreaLocaId"
              placeholder="选择目标库位"
              style="width: 180px"
              size="small"
              :options="record.candidateTargetLocas ? record.candidateTargetLocas.map((c) => ({ label: c.locaCode, value: c.locaId })) : []"
            />
          </div>
          <div v-else>
            <a-tag color="error" style="font-weight: bold; padding: 2px 8px">
              🔴 缺少可用目标库位
            </a-tag>
          </div>
        </template>
      </template>
    </a-table>

    <!-- 底部执行选项 -->
    <div style="margin-top: 14px; display: flex; justify-content: space-between; align-items: center">
      <div style="display: flex; align-items: center; gap: 8px">
        <span style="font-weight: bold; color: #606266">迁移备注：</span>
        <a-input v-model:value="migrationRemark" placeholder="如：1号库区翻新腾空迁移" style="width: 320px" allowClear />
      </div>
      <div>
        <a-checkbox v-model:checked="autoExecute">
          <span style="font-weight: bold; color: #1890ff">迁移后立即执行生效（源库位立刻释放为 0，目标库位立即占用）</span>
        </a-checkbox>
      </div>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed, h } from 'vue';
  import { Tag } from 'ant-design-vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { list as listWarehouse } from '../../WmsWarehouse/WmsWarehouse.api';
  import { list as listArea } from '../../WmsWarehouseArea/WmsWarehouseArea.api';
  import { previewAreaMigration, executeAreaMigration } from '../WmsMove.api';

  const emit = defineEmits(['register', 'success']);
  const { createMessage, createConfirm } = useMessage();

  const sourceWarehouseId = ref<number | undefined>(undefined);
  const sourceWarehouseAreaId = ref<number | undefined>(undefined);
  const targetWarehouseId = ref<number | undefined>(undefined);
  const targetWarehouseAreaId = ref<number | undefined>(undefined);
  const autoExecute = ref(true);
  const migrationRemark = ref('');

  const scanning = ref(false);
  const previewData = ref<any>(null);
  const migrationDetails = ref<any[]>([]);
  const filterStatus = ref<'ALL' | 'MATCHED' | 'MISSING'>('ALL');

  const warehouseOptions = ref<{ label: string; value: number }[]>([]);
  const sourceAreaOptions = ref<{ label: string; value: number }[]>([]);
  const targetAreaOptions = ref<{ label: string; value: number }[]>([]);

  const matchedCount = computed(() => migrationDetails.value.filter((d) => d.matchStatus === 'MATCHED').length);
  const missingCount = computed(() => migrationDetails.value.filter((d) => d.matchStatus === 'MISSING').length);

  const displayedDetails = computed(() => {
    if (filterStatus.value === 'MATCHED') {
      return migrationDetails.value.filter((d) => d.matchStatus === 'MATCHED');
    }
    if (filterStatus.value === 'MISSING') {
      return migrationDetails.value.filter((d) => d.matchStatus === 'MISSING');
    }
    return migrationDetails.value;
  });

  const canScan = computed(() => {
    return (
      !!sourceWarehouseId.value &&
      !!sourceWarehouseAreaId.value &&
      !!targetWarehouseId.value &&
      !!targetWarehouseAreaId.value &&
      sourceWarehouseAreaId.value !== targetWarehouseAreaId.value
    );
  });

  const tableColumns = [
    {
      title: '源库位编码',
      dataIndex: 'sourceLocaCode',
      align: 'center',
      width: 140,
      customRender: ({ text, record }) => {
        const isMissing = record.matchStatus === 'MISSING';
        return h('span', {
          style: {
            fontWeight: 'bold',
            color: isMissing ? '#f5222d' : '#fa541c',
          },
        }, isMissing ? `🔴 ${text}` : text);
      },
    },
    {
      title: '在库商品名称',
      dataIndex: 'spuTitle',
      align: 'left',
      ellipsis: true,
    },
    {
      title: '规格编码',
      dataIndex: 'skuCode',
      align: 'center',
      width: 130,
    },
    {
      title: '批次号',
      dataIndex: 'batchNumber',
      align: 'center',
      width: 140,
      customRender: ({ text }) => (text ? h(Tag, { color: 'blue' }, () => text) : '-'),
    },
    {
      title: '质量',
      dataIndex: 'qualityStatus',
      align: 'center',
      width: 75,
      customRender: ({ text }) => {
        const q = Number(text);
        return h(Tag, { color: q === 1 ? 'green' : q === 2 ? 'orange' : 'red' }, () => (q === 1 ? '良品' : q === 2 ? '待检' : '不良品'));
      },
    },
    {
      title: '在库件数',
      dataIndex: 'count',
      align: 'center',
      width: 90,
      customRender: ({ text }) => h('span', { style: { fontWeight: 'bold', color: '#52c41a' } }, `${text ?? 0}`),
    },
    {
      title: '流向',
      key: 'direction',
      align: 'center',
      width: 70,
    },
    {
      title: '映射目标库位(空闲)',
      key: 'targetLoca',
      align: 'center',
      width: 200,
    },
  ];

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async () => {
    resetState();
    setModalProps({ confirmLoading: false });
    await loadWarehouses();
  });

  function resetState() {
    sourceWarehouseId.value = undefined;
    sourceWarehouseAreaId.value = undefined;
    targetWarehouseId.value = undefined;
    targetWarehouseAreaId.value = undefined;
    autoExecute.value = true;
    migrationRemark.value = '';
    previewData.value = null;
    migrationDetails.value = [];
    filterStatus.value = 'ALL';
    sourceAreaOptions.value = [];
    targetAreaOptions.value = [];
  }

  async function loadWarehouses() {
    try {
      const res: any = await listWarehouse({ pageSize: 100 });
      const records = res?.records || res || [];
      warehouseOptions.value = records.map((w) => ({ label: w.name, value: w.id }));
    } catch (e) {
      console.error('加载仓库列表失败', e);
    }
  }

  function formatAreaOptionLabel(a: any) {
    const emptyCount = a.emptyLocaCount ?? 0;
    const totalCount = a.totalLocaCount ?? 0;
    return `${a.name} [空闲: ${emptyCount} / 总计: ${totalCount}]`;
  }

  async function handleSourceWarehouseChange(val: number) {
    sourceWarehouseAreaId.value = undefined;
    sourceAreaOptions.value = [];
    previewData.value = null;
    migrationDetails.value = [];
    if (!val) return;
    try {
      const res: any = await listArea({ warehouseId: val, pageSize: 200 });
      const records = res?.records || res || [];
      sourceAreaOptions.value = records.map((a) => ({
        label: formatAreaOptionLabel(a),
        value: a.id,
      }));
    } catch (e) {
      console.error('加载源库区失败', e);
    }
  }

  async function handleTargetWarehouseChange(val: number) {
    targetWarehouseAreaId.value = undefined;
    targetAreaOptions.value = [];
    previewData.value = null;
    migrationDetails.value = [];
    if (!val) return;
    try {
      const res: any = await listArea({ warehouseId: val, pageSize: 200 });
      const records = res?.records || res || [];
      targetAreaOptions.value = records.map((a) => ({
        label: formatAreaOptionLabel(a),
        value: a.id,
      }));
    } catch (e) {
      console.error('加载目标库区失败', e);
    }
  }

  function handleAreaChange() {
    previewData.value = null;
    migrationDetails.value = [];
    filterStatus.value = 'ALL';
  }

  async function handleScanAndPreview() {
    if (!canScan.value) {
      createMessage.warning('请选择源仓库、源库区、目标仓库与目标库区，且源库区与目标库区不能相同');
      return;
    }
    scanning.value = true;
    previewData.value = null;
    migrationDetails.value = [];
    filterStatus.value = 'ALL';
    try {
      const res: any = await previewAreaMigration({
        sourceWarehouseId: sourceWarehouseId.value!,
        sourceWarehouseAreaId: sourceWarehouseAreaId.value!,
        targetWarehouseId: targetWarehouseId.value!,
        targetWarehouseAreaId: targetWarehouseAreaId.value!,
      });
      previewData.value = res;
      migrationDetails.value = (res?.details || []).map((d) => ({ ...d }));
      if (res?.canMigrate) {
        createMessage.success(res.message || '智能匹配完成');
      } else {
        createMessage.warning(res.message || '预检发现问题，请检查库位容量');
      }
    } catch (e: any) {
      createMessage.error(e?.message || '预检配对失败');
    } finally {
      scanning.value = false;
    }
  }

  async function handleSubmit() {
    if (!previewData.value?.canMigrate) {
      createMessage.error('当前预检未通过（存在目标库位缺口），不可执行整区迁移');
      return;
    }
    if (migrationDetails.value.length === 0) {
      createMessage.warning('源库区当前没有任何在库货物需要迁移');
      return;
    }
    // 校验每个商品是否都有目标库位
    for (let i = 0; i < migrationDetails.value.length; i++) {
      const row = migrationDetails.value[i];
      if (!row.targetWarehouseAreaLocaId) {
        createMessage.error(`第 ${i + 1} 行[${row.sourceLocaCode}]未分配到目标库位！`);
        return;
      }
    }
    // 校验目标库位不能重复
    const targetSet = new Set<number>();
    for (const row of migrationDetails.value) {
      if (targetSet.has(row.targetWarehouseAreaLocaId)) {
        createMessage.error('目标库区内存在被重复指定的目标库位，请为每个货物分配不同的空库位！');
        return;
      }
      targetSet.add(row.targetWarehouseAreaLocaId);
    }

    createConfirm({
      iconType: 'warning',
      title: '确认执行整库区迁移？',
      content: `即将把源库区【${previewData.value.sourceAreaName}】的 ${migrationDetails.value.length} 个库位（共 ${previewData.value.sourceTotalItemCount} 件货物）整体迁入【${previewData.value.targetAreaName}】。搬迁后源库区将全部腾空为空库位。`,
      onOk: async () => {
        setModalProps({ confirmLoading: true });
        try {
          const payload = {
            sourceWarehouseId: sourceWarehouseId.value,
            sourceWarehouseAreaId: sourceWarehouseAreaId.value,
            targetWarehouseId: targetWarehouseId.value,
            targetWarehouseAreaId: targetWarehouseAreaId.value,
            autoExecute: autoExecute.value,
            remark: migrationRemark.value,
            details: migrationDetails.value.map((d) => ({
              skuId: d.skuId,
              batchNumber: d.batchNumber || '',
              consignorId: d.consignorId || 0,
              qualityStatus: d.qualityStatus || 1,
              sourceWarehouseAreaLocaId: d.sourceWarehouseAreaLocaId,
              targetWarehouseAreaLocaId: d.targetWarehouseAreaLocaId,
              count: d.count,
            })),
          };
          const res: any = await executeAreaMigration(payload);
          createMessage.success(res?.message || '整库区迁移成功！');
          closeModal();
          emit('success');
        } catch (e: any) {
          createMessage.error(e?.message || '执行整库区迁移失败');
        } finally {
          setModalProps({ confirmLoading: false });
        }
      },
    });
  }
</script>

<style scoped>
  :deep(.ant-card-head) {
    min-height: 36px;
    padding: 0 10px;
    font-size: 13px;
  }
</style>
