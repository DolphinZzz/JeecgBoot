<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose title="新建移库单" :width="1300" @ok="handleSubmit">
    <!-- 顶部双仓库与备注配置 -->
    <BasicForm @register="registerForm" ref="formRef" />

    <!-- 移库流向指示与工具栏 -->
    <div class="move-toolbar">
      <div class="toolbar-title">
        <span style="font-weight: bold; color: #1890ff">📦 移库流向：</span>
        <span style="color: #333">{{ moveDirectionText }}</span>
      </div>
      <div class="toolbar-actions">
        <a-button type="primary" preIcon="ant-design:database-outlined" size="small" @click="handleOpenInvPicker">
          从调出仓选货移出
        </a-button>
        <a-button
          v-if="tableData.dataSource.length > 0"
          type="default"
          preIcon="ant-design:aim-outlined"
          size="small"
          @click="handlePickFirstUnmatched"
        >
          挑选调入仓合规库位
        </a-button>
        <a-button
          v-if="tableData.dataSource.length > 0"
          danger
          type="text"
          size="small"
          @click="handleClearAll"
        >
          清空明细
        </a-button>
      </div>
    </div>

    <!-- 移库明细表格：彻底禁止下拉乱选，全部由在库数据真实带出与智能挑选器选择 -->
    <a-table
      size="small"
      bordered
      rowKey="key"
      :columns="tableColumns"
      :dataSource="tableData.dataSource"
      :pagination="false"
      :scroll="{ y: 360 }"
    >
      <template #bodyCell="{ column, record, index }">
        <!-- 序号 -->
        <template v-if="column.key === 'index'">
          {{ index + 1 }}
        </template>

        <!-- 商品信息 -->
        <template v-else-if="column.key === 'skuText'">
          <div style="font-weight: 500; color: #1890ff">{{ record.spuTitle || '未命名商品' }}</div>
          <div style="font-size: 12px; color: #888">规格: {{ record.skuCode || record.skuId }}</div>
        </template>

        <!-- 批次号 -->
        <template v-else-if="column.key === 'batchNumber'">
          <a-tag v-if="record.batchNumber && record.batchNumber !== '-'" color="blue">{{ record.batchNumber }}</a-tag>
          <span v-else style="color: #bbb">-</span>
        </template>

        <!-- 货主 -->
        <template v-else-if="column.key === 'consignorName'">
          <span>{{ record.consignorName || '自有货主' }}</span>
        </template>

        <!-- 质量状态 -->
        <template v-else-if="column.key === 'qualityStatus'">
          <a-tag :color="record.qualityStatus === 1 ? 'green' : record.qualityStatus === 2 ? 'orange' : 'red'">
            {{ record.qualityStatus === 1 ? '良品' : record.qualityStatus === 2 ? '待检' : '不良品' }}
          </a-tag>
        </template>

        <!-- 移出库区/库位 (只读真实带出) -->
        <template v-else-if="column.key === 'sourceLoca'">
          <div>
            <span style="color: #666">{{ record.sourceWarehouseAreaName || '库区' }} / </span>
            <a-tag color="cyan" style="font-weight: bold">{{ record.sourceLocaCode }}</a-tag>
          </div>
        </template>

        <!-- 调入目标库位 (必须通过智能挑选器合规选定，禁止下拉直接修改) -->
        <template v-else-if="column.key === 'targetLoca'">
          <div v-if="record.targetWarehouseAreaLocaId" style="display: flex; align-items: center; justify-content: center; gap: 6px">
            <a-tag color="green" style="font-weight: bold">
              {{ record.targetWarehouseAreaName ? `${record.targetWarehouseAreaName} / ` : '' }}{{ record.targetLocaCode }}
            </a-tag>
            <a-button type="link" size="small" @click="handleOpenLocaPickerForRow(index)">
              更换
            </a-button>
          </div>
          <div v-else style="display: flex; align-items: center; justify-content: center; gap: 6px">
            <a-tag color="orange">未指定调入库位</a-tag>
            <a-button type="primary" size="small" ghost @click="handleOpenLocaPickerForRow(index)">
              智能选位
            </a-button>
          </div>
        </template>

        <!-- 移出数量 (可调，最大不超过在库可用件数) -->
        <template v-else-if="column.key === 'count'">
          <div style="display: flex; flex-direction: column; align-items: center">
            <a-input-number
              v-model:value="record.count"
              :min="1"
              :max="record.availableCount"
              :precision="0"
              size="small"
              style="width: 100px; text-align: center"
            />
            <span style="font-size: 11px; color: #999; margin-top: 2px">可用: {{ record.availableCount }}</span>
          </div>
        </template>

        <!-- 操作 -->
        <template v-else-if="column.key === 'action'">
          <a-space size="small">
            <a-button type="link" size="small" @click="handleOpenLocaPickerForRow(index)">
              {{ record.targetWarehouseAreaLocaId ? '换位' : '选位' }}
            </a-button>
            <a-popconfirm title="确定移除此行货物吗？" @confirm="handleRemoveRow(index)">
              <a-button type="link" danger size="small">移除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>

      <!-- 空数据友好提示 -->
      <template #emptyText>
        <div style="padding: 24px 0; color: #999">
          <div>暂无移库货物明细</div>
          <div style="margin-top: 6px; font-size: 12px">
            请先在上方确定【调出仓库】，然后点击
            <a-button type="link" size="small" @click="handleOpenInvPicker">「从调出仓选货移出」</a-button>
            批量勾选在库库存
          </div>
        </div>
      </template>
    </a-table>

    <!-- 从调出仓选货移出弹窗 (支持多选批量导入) -->
    <BasicModal
      v-model:visible="invModalVisible"
      title="从调出仓选择在库货物移出"
      :width="1100"
      @ok="handleConfirmInvSelect"
      :okButtonProps="{ disabled: selectedInvKeys.length === 0 }"
      :okText="`添加选中的 ${selectedInvKeys.length} 项货物`"
    >
      <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center">
        <a-input
          v-model:value="invSearchKeyword"
          placeholder="搜索库位编码 / 商品名称 / 规格编码 / 批次号 / 库区"
          style="width: 380px"
          allowClear
        />
        <span style="font-size: 13px; color: #666">
          已勾选 <span style="font-weight: bold; color: #1890ff">{{ selectedInvKeys.length }}</span> 项货物
        </span>
      </div>
      <a-table
        size="small"
        bordered
        rowKey="id"
        :loading="invLoading"
        :columns="invTableColumns"
        :dataSource="filteredInvList"
        :pagination="{ pageSize: 8, showTotal: (t) => `共 ${t} 条在库货物` }"
        :rowSelection="{ type: 'checkbox', selectedRowKeys: selectedInvKeys, onChange: onInvSelectChange }"
        :customRow="customInvRow"
      />
    </BasicModal>

    <!-- 移入库位智能选择器（统一落库硬过滤，校验目标仓温区、分区与混放策略） -->
    <WmsLocaPickerModal @register="registerLocaPicker" @selected="onTargetLocaSelected" />
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, reactive, computed, h } from 'vue';
  import { Tag } from 'ant-design-vue';
  import { BasicModal, useModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { formSchema } from '../WmsMove.data';
  import { createMove } from '../WmsMove.api';
  import { queryInventoryList } from '../../WmsInventory/WmsInventory.api';
  import WmsLocaPickerModal from '../../components/WmsLocaPickerModal.vue';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);

  const formRef = ref();
  const tableData = reactive({ dataSource: [] as any[] });

  // 表头表单配置
  const [registerForm, { resetFields, validate, setProps, getFieldsValue, setFieldsValue }] = useForm({
    schemas: formSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 8 },
    labelWidth: 100,
  });

  const [registerLocaPicker, { openModal: openLocaPicker }] = useModal();

  // 移库流向提示文案
  const moveDirectionText = computed(() => {
    const values = getFieldsValue();
    if (!values?.warehouseId) {
      return '请在表头选择「调出仓库」与「调入仓库」';
    }
    const srcId = values.warehouseId;
    const tgtId = values.targetWarehouseId || srcId;
    if (srcId === tgtId) {
      return '【仓内移位整理】同仓库内库位搬移';
    }
    return `【跨仓库存调拨】从调出仓库(ID: ${srcId}) 调拨至 调入仓库(ID: ${tgtId})`;
  });

  // 主明细表格列定义 (只读展示，严禁单元格下拉任意篡改库位与批次)
  const tableColumns = [
    { title: '#', key: 'index', width: 50, align: 'center' },
    { title: '商品信息', key: 'skuText', align: 'left', minWidth: 200, ellipsis: true },
    { title: '批次号', key: 'batchNumber', width: 130, align: 'center' },
    { title: '货主', key: 'consignorName', width: 130, align: 'center', ellipsis: true },
    { title: '质量状态', key: 'qualityStatus', width: 90, align: 'center' },
    { title: '移出库位', key: 'sourceLoca', width: 170, align: 'center' },
    { title: '调入目标库位', key: 'targetLoca', width: 220, align: 'center' },
    { title: '移出数量', key: 'count', width: 120, align: 'center' },
    { title: '操作', key: 'action', width: 110, align: 'center' },
  ];

  // 弹窗打开初始化
  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    tableData.dataSource = [];
    setProps({ disabled: false });
    setModalProps({ confirmLoading: false });

    // 从库存列表穿透发起移库预填
    if (data?.prefill) {
      const p = data.prefill;
      if (p.warehouseId) {
        await setFieldsValue({
          warehouseId: p.warehouseId,
          targetWarehouseId: p.warehouseId,
        });
      }
      tableData.dataSource = [
        {
          key: `prefill_${Date.now()}`,
          invId: p.id,
          skuId: p.skuId,
          spuTitle: p.spuTitle || '未命名商品',
          skuCode: p.skuCode || String(p.skuId),
          skuText: `${p.spuTitle || '商品'} [${p.skuCode || p.skuId}]`,
          batchNumber: p.batchNumber || '-',
          consignorId: p.consignorId || 0,
          consignorName: p.consignorName || '自有货主',
          qualityStatus: p.qualityStatus || 1,
          sourceWarehouseAreaId: p.warehouseAreaId,
          sourceWarehouseAreaName: p.warehouseAreaName || '源库区',
          sourceWarehouseAreaLocaId: p.sourceWarehouseAreaLocaId || p.warehouseAreaLocaId,
          sourceLocaCode: p.locaCode || '源库位',
          availableCount: p.availableCount && p.availableCount > 0 ? p.availableCount : 1,
          count: p.availableCount && p.availableCount > 0 ? p.availableCount : 1,
          targetWarehouseAreaLocaId: undefined,
          targetLocaCode: '',
          targetWarehouseAreaName: '',
        },
      ];
    }
  });

  // ==============================
  // 从调出仓选货移出弹窗 (多选批量导入)
  // ==============================
  const invModalVisible = ref(false);
  const invLoading = ref(false);
  const invList = ref<any[]>([]);
  const invSearchKeyword = ref('');
  const selectedInvKeys = ref<(string | number)[]>([]);
  const selectedInvRows = ref<any[]>([]);

  const invTableColumns = [
    { title: '移出库位', dataIndex: 'locaCode', align: 'center', width: 130 },
    { title: '库区', dataIndex: 'areaName', align: 'center', width: 120 },
    { title: '商品名称', dataIndex: 'spuTitle', align: 'left', ellipsis: true },
    { title: '规格编码', dataIndex: 'skuCode', align: 'center', width: 130 },
    { title: '批次号', dataIndex: 'batchNumber', align: 'center', width: 130, customRender: ({ text }) => text || '-' },
    { title: '货主', dataIndex: 'consignorName', align: 'center', width: 120, customRender: ({ text }) => text || '自有货主' },
    {
      title: '质量状态',
      dataIndex: 'qualityStatus',
      align: 'center',
      width: 85,
      customRender: ({ text }) => {
        const q = Number(text);
        return h(Tag, { color: q === 1 ? 'green' : q === 2 ? 'orange' : 'red' }, () => (q === 1 ? '良品' : q === 2 ? '待检' : '不良品'));
      },
    },
    {
      title: '可用件数',
      dataIndex: 'availableCount',
      align: 'center',
      width: 95,
      customRender: ({ text }) => h('span', { style: { color: '#52c41a', fontWeight: 'bold' } }, `${text ?? 0}`),
    },
  ];

  const filteredInvList = computed(() => {
    const kw = invSearchKeyword.value.trim().toLowerCase();
    if (!kw) return invList.value;
    return invList.value.filter((item) => {
      const haystack = [item.locaCode, item.spuTitle, item.skuCode, item.batchNumber, item.areaName, item.consignorName]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return haystack.includes(kw);
    });
  });

  async function handleOpenInvPicker() {
    const values = getFieldsValue();
    if (!values?.warehouseId) {
      createMessage.warning('请先在表头选择「调出仓库」');
      return;
    }
    // 默认调入仓库等于调出仓库
    if (!values.targetWarehouseId) {
      await setFieldsValue({ targetWarehouseId: values.warehouseId });
    }

    invModalVisible.value = true;
    invLoading.value = true;
    selectedInvKeys.value = [];
    selectedInvRows.value = [];
    try {
      const res: any = await queryInventoryList({
        warehouseId: values.warehouseId,
        onlyAvailable: true,
        pageSize: 500,
      });
      const list = (res?.records || res || []) as any[];
      invList.value = list.filter((item: any) => (item.availableCount || 0) > 0);
    } finally {
      invLoading.value = false;
    }
  }

  function onInvSelectChange(keys: (string | number)[], rows: any[]) {
    selectedInvKeys.value = keys;
    selectedInvRows.value = rows;
  }

  function customInvRow(record: any) {
    return {
      onClick: () => {
        const idx = selectedInvKeys.value.indexOf(record.id);
        if (idx >= 0) {
          selectedInvKeys.value.splice(idx, 1);
          selectedInvRows.value = selectedInvRows.value.filter((r) => r.id !== record.id);
        } else {
          selectedInvKeys.value.push(record.id);
          selectedInvRows.value.push(record);
        }
      },
      style: { cursor: 'pointer' },
    };
  }

  function handleConfirmInvSelect() {
    if (selectedInvRows.value.length === 0) return;

    let addedCount = 0;
    const existingInvIds = new Set(tableData.dataSource.map((r) => r.invId));

    for (const inv of selectedInvRows.value) {
      // 避免重复添加完全一致的在库库存行
      if (existingInvIds.has(inv.id)) {
        continue;
      }
      const newRow = {
        key: `row_${inv.id}_${Date.now()}_${Math.random()}`,
        invId: inv.id,
        warehouseId: inv.warehouseId || values.warehouseId,
        skuId: inv.skuId,
        spuTitle: inv.spuTitle || '未命名商品',
        skuCode: inv.skuCode || String(inv.skuId),
        skuText: `${inv.spuTitle || '商品'} [${inv.skuCode || inv.skuId}]`,
        batchNumber: inv.batchNumber || '-',
        consignorId: inv.consignorId || 0,
        consignorName: inv.consignorName || '自有货主',
        qualityStatus: inv.qualityStatus || 1,
        sourceWarehouseAreaId: inv.warehouseAreaId,
        sourceWarehouseAreaName: inv.areaName || '源库区',
        sourceWarehouseAreaLocaId: inv.warehouseAreaLocaId,
        sourceLocaCode: inv.locaCode,
        availableCount: inv.availableCount && inv.availableCount > 0 ? inv.availableCount : 1,
        count: inv.availableCount && inv.availableCount > 0 ? inv.availableCount : 1,
        targetWarehouseAreaLocaId: undefined,
        targetLocaCode: '',
        targetWarehouseAreaName: '',
      };
      tableData.dataSource.push(newRow);
      existingInvIds.add(inv.id);
      addedCount++;
    }

    invModalVisible.value = false;
    createMessage.success(`成功导入 ${addedCount} 条货物，请为每条明细挑选调入库位`);
  }

  // ==============================
  // 移入库位智能选择器逻辑 (支持行内独立选位与全局快捷选位)
  // ==============================
  let targetLocaRowIndex = -1;

  function handleOpenLocaPickerForRow(index: number) {
    const values = getFieldsValue();
    const targetWhId = values?.targetWarehouseId || values?.warehouseId;
    if (!targetWhId) {
      createMessage.warning('请先在表头选择「调入仓库」');
      return;
    }
    const row = tableData.dataSource[index];
    if (!row) return;

    targetLocaRowIndex = index;
    openLocaPicker(true, {
      title: `为第 ${index + 1} 行商品【${row.spuTitle}】挑选调入库位（校验目标仓合规策略）`,
      warehouseId: Number(targetWhId),
      skuId: Number(row.skuId),
      batchNumber: row.batchNumber === '-' ? '' : row.batchNumber,
      qualityStatus: Number(row.qualityStatus) || 1,
      count: Number(row.count) || 1,
      locaId: row.targetWarehouseAreaLocaId ? Number(row.targetWarehouseAreaLocaId) : undefined,
    });
  }

  function handlePickFirstUnmatched() {
    const firstUnmatchedIdx = tableData.dataSource.findIndex((r) => !r.targetWarehouseAreaLocaId);
    if (firstUnmatchedIdx >= 0) {
      handleOpenLocaPickerForRow(firstUnmatchedIdx);
    } else if (tableData.dataSource.length > 0) {
      handleOpenLocaPickerForRow(0);
    } else {
      createMessage.info('暂无货物明细，请先从调出仓选货移出');
    }
  }

  function onTargetLocaSelected(loca: any) {
    if (!loca?.locaId || targetLocaRowIndex < 0 || targetLocaRowIndex >= tableData.dataSource.length) return;
    const row = tableData.dataSource[targetLocaRowIndex];
    row.targetWarehouseAreaLocaId = Number(loca.locaId);
    row.targetLocaCode = loca.locaCode;
    row.targetWarehouseAreaName = loca.areaName || '';
    createMessage.success(`已为第 ${targetLocaRowIndex + 1} 行设置调入库位[${loca.locaCode}]`);
  }

  function handleRemoveRow(index: number) {
    tableData.dataSource.splice(index, 1);
  }

  function handleClearAll() {
    tableData.dataSource = [];
  }

  // ==============================
  // 提交逻辑
  // ==============================
  async function handleSubmit() {
    try {
      const formValues = await validate();
      const targetWarehouseId = formValues.targetWarehouseId || formValues.warehouseId;
      const isSameWarehouse = formValues.warehouseId === targetWarehouseId;

      const rows = tableData.dataSource;
      if (rows.length === 0) {
        createMessage.warning('请先点击「从调出仓选货移出」添加需要搬移的货物');
        return;
      }

      // 严格检查货物归属与目标库位
      for (let i = 0; i < rows.length; i++) {
        const r = rows[i];
        if (r.warehouseId && r.warehouseId !== formValues.warehouseId) {
          createMessage.error(`第 ${i + 1} 行货物属于原调出仓库，与当前表头调出仓库不一致，请重新选货！`);
          return;
        }
        if (!r.targetWarehouseAreaLocaId) {
          createMessage.error(`第 ${i + 1} 行商品【${r.spuTitle}】尚未挑选调入库位，请点击「智能选位」`);
          return;
        }
        if (isSameWarehouse && r.sourceWarehouseAreaLocaId === r.targetWarehouseAreaLocaId) {
          createMessage.error(`第 ${i + 1} 行移出库位与调入库位相同[${r.sourceLocaCode}]，仓内移位必须移至不同库位！`);
          return;
        }
        if (!r.count || r.count <= 0) {
          createMessage.error(`第 ${i + 1} 行移出数量必须大于0`);
          return;
        }
        if (r.count > r.availableCount) {
          createMessage.error(`第 ${i + 1} 行移出数量(${r.count})超过最大可用库存(${r.availableCount})！`);
          return;
        }
      }

      const details = rows.map((r) => ({
        skuId: r.skuId,
        batchNumber: r.batchNumber === '-' ? '' : r.batchNumber,
        consignorId: r.consignorId || 0,
        qualityStatus: r.qualityStatus || 1,
        sourceWarehouseAreaLocaId: r.sourceWarehouseAreaLocaId,
        targetWarehouseId: targetWarehouseId,
        targetWarehouseAreaLocaId: r.targetWarehouseAreaLocaId,
        count: Number(r.count),
      }));

      setModalProps({ confirmLoading: true });
      const res: any = await createMove({
        operationType: 1,
        warehouseId: formValues.warehouseId,
        targetWarehouseId,
        remark: formValues.remark,
        details,
      });
      createMessage.success(res?.message || (isSameWarehouse ? '移库单已生成' : '跨仓调拨移库单已生成'));
      closeModal();
      emit('success');
    } catch (e: any) {
      if (e?.errorFields) return;
      createMessage.error(e?.message || '新建移库单失败');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<style lang="less" scoped>
  .move-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin: 8px 0 12px;
    padding: 6px 10px;
    background: #f7f9fc;
    border: 1px dashed #d9d9d9;
    border-radius: 4px;

    .toolbar-title {
      font-size: 13px;
    }

    .toolbar-actions {
      display: flex;
      gap: 8px;
    }
  }
</style>
