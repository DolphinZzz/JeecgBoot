<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    destroyOnClose
    :title="`库位商品规划与容量配置 - ${locaInfo.locaCode || ''}`"
    :width="920"
    :okText="'保存规划配置'"
    :confirmLoading="submitting"
    @ok="handleSubmit"
  >
    <a-descriptions :column="4" size="small" bordered style="margin-bottom: 12px">
      <a-descriptions-item label="库位编码">
        <b style="color: #1890ff">{{ locaInfo.locaCode || '-' }}</b>
      </a-descriptions-item>
      <a-descriptions-item label="所属仓库">{{ locaInfo.warehouseName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="所属库区">{{ locaInfo.areaName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="货架坐标">{{ locaInfo.positionText || '-' }}</a-descriptions-item>
    </a-descriptions>

    <a-alert type="info" show-icon style="margin-bottom: 14px">
      <template #message>
        <span>
          <b>业务规划规则：</b>在此配置该库位允许存放的商品（SPU）及最大存放件数。
          如果是混合库位，可设置多个SPU，<b>但单个库位最多不能超过 3 个 SPU</b>。
          配置完成后，收货上架与入库作业将直接以此容量核算剩余空间。
        </span>
      </template>
    </a-alert>

    <div style="margin-bottom: 12px; display: flex; align-items: center; justify-content: space-between">
      <div style="display: flex; align-items: center">
        <span style="font-weight: 600; margin-right: 8px">库位属性：</span>
        <a-radio-group v-model:value="isMixed" button-style="solid" size="small" @change="handleMixedChange">
          <a-radio-button :value="0">单品库位 (最多1个SPU)</a-radio-button>
          <a-radio-button :value="1">混合库位 (最多3个SPU)</a-radio-button>
        </a-radio-group>
        <span style="margin-left: 12px; color: #8c8c8c; font-size: 13px">
          当前已规划: <b :style="{ color: items.length > 3 ? '#f5222d' : '#1890ff' }">{{ items.length }}</b> / {{ maxAllowedSpu }} 个SPU
        </span>
      </div>

      <a-tooltip :title="addDisabledReason">
        <a-button
          type="primary"
          size="small"
          :disabled="isAddDisabled"
          @click="handleAddRow"
        >
          + 添加规划商品
        </a-button>
      </a-tooltip>
    </div>

    <a-table
      size="small"
      bordered
      :dataSource="items"
      :columns="tableColumns"
      :pagination="false"
      rowKey="_tempId"
      :locale="{ emptyText: '暂未配置商品规划，点击上方「+ 添加规划商品」进行设置' }"
    >
      <!-- 商品SPU选择器 -->
      <template #spuSlot="{ record }">
        <a-select
          v-model:value="record.spuId"
          show-search
          placeholder="请选择商品(SPU)"
          style="width: 100%"
          :filter-option="filterSpuOption"
          :options="spuOptions"
          @change="(val, option) => handleSpuChange(record, val, option)"
        />
      </template>

      <!-- 最大件数输入框 -->
      <template #maxCountSlot="{ record }">
        <a-input-number
          v-model:value="record.maxCount"
          :min="1"
          :max="999999"
          :precision="0"
          placeholder="容量上限"
          style="width: 100%"
        />
      </template>

      <!-- 当前在库件数 -->
      <template #currentCountSlot="{ record }">
        <span :style="{ color: record.currentCount > 0 ? '#1890ff' : '#8c8c8c', fontWeight: 600 }">
          {{ record.currentCount ?? 0 }} 件
        </span>
      </template>

      <!-- 剩余可放件数 -->
      <template #remainCountSlot="{ record }">
        <template v-if="record.maxCount">
          <span
            v-if="record.maxCount - (record.currentCount || 0) <= 0"
            style="color: #f5222d; font-weight: 600"
          >
            已放满 (0件)
          </span>
          <span v-else style="color: #52c41a; font-weight: 600">
            {{ record.maxCount - (record.currentCount || 0) }} 件
          </span>
        </template>
        <span v-else style="color: #8c8c8c">-</span>
      </template>

      <!-- 备注 -->
      <template #remarkSlot="{ record }">
        <a-input v-model:value="record.remark" placeholder="规划备注(选填)" :maxlength="100" />
      </template>

      <!-- 操作 -->
      <template #actionSlot="{ index }">
        <a-popconfirm title="确定移除该商品的规划吗？" @confirm="handleRemoveRow(index)">
          <a-button type="link" danger size="small">移除</a-button>
        </a-popconfirm>
      </template>
    </a-table>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { queryCapacityListByLocaId, saveLocaCapacityBatch, querySpuOptions } from '../WmsInventory.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);

  const submitting = ref(false);
  const locaInfo = ref<any>({});
  const isMixed = ref<number>(0);
  const items = ref<any[]>([]);
  const spuOptions = ref<any[]>([]);

  let tempIdSeed = 1;

  const tableColumns = [
    {
      title: '允许存放商品 (SPU)',
      dataIndex: 'spuId',
      width: 250,
      slots: { customRender: 'spuSlot' },
    },
    {
      title: '最大可存放件数',
      dataIndex: 'maxCount',
      width: 140,
      align: 'center',
      slots: { customRender: 'maxCountSlot' },
    },
    {
      title: '当前在库件数',
      dataIndex: 'currentCount',
      width: 110,
      align: 'center',
      slots: { customRender: 'currentCountSlot' },
    },
    {
      title: '剩余可放件数',
      dataIndex: 'remainCount',
      width: 120,
      align: 'center',
      slots: { customRender: 'remainCountSlot' },
    },
    {
      title: '规划备注',
      dataIndex: 'remark',
      slots: { customRender: 'remarkSlot' },
    },
    {
      title: '操作',
      dataIndex: 'action',
      width: 80,
      align: 'center',
      slots: { customRender: 'actionSlot' },
    },
  ];

  const maxAllowedSpu = computed(() => (isMixed.value === 1 ? 3 : 1));

  const isAddDisabled = computed(() => {
    return items.value.length >= maxAllowedSpu.value;
  });

  const addDisabledReason = computed(() => {
    if (items.value.length >= 3) {
      return '单库位最多只能配置 3 个商品(SPU)！';
    }
    if (isMixed.value === 0 && items.value.length >= 1) {
      return '单品库位最多只能配置 1 个商品(SPU)，若需存放多种商品请切换为混合库位';
    }
    return '';
  });

  const [registerModal, { closeModal }] = useModalInner(async (data) => {
    locaInfo.value = data || {};
    isMixed.value = data?.isMixed ?? 0;
    items.value = [];

    // 加载 SPU 选项
    loadSpuOptions();

    // 加载已有配置
    const locaId = data?.locaId || data?.id;
    if (locaId) {
      try {
        const list = await queryCapacityListByLocaId(locaId);
        if (Array.isArray(list) && list.length > 0) {
          items.value = list.map((item) => ({
            _tempId: tempIdSeed++,
            spuId: item.spuId,
            spuCode: item.spuCode,
            spuName: item.spuName,
            maxCount: item.maxCount,
            currentCount: item.currentCount || 0,
            remainCount: item.remainCount,
            remark: item.remark || '',
          }));
          if (items.value.length > 1) {
            isMixed.value = 1;
          }
        }
      } catch (e: any) {
        createMessage.error(e?.message || '加载库位商品规划配置失败');
      }
    }
  });

  async function loadSpuOptions() {
    try {
      const res = await querySpuOptions();
      if (Array.isArray(res)) {
        spuOptions.value = res.map((s) => ({
          label: `[${s.spuCode || '无编码'}] ${s.spuName}`,
          value: s.id,
          spuCode: s.spuCode,
          spuName: s.spuName,
        }));
      }
    } catch (e) {
      console.error('加载商品选项失败', e);
    }
  }

  function filterSpuOption(input: string, option: any) {
    const label = option.label || '';
    return label.toLowerCase().includes(input.toLowerCase());
  }

  function handleSpuChange(record: any, val: any, option: any) {
    if (option) {
      record.spuCode = option.spuCode;
      record.spuName = option.spuName;
    }
  }

  function handleMixedChange(e: any) {
    const val = e.target.value;
    if (val === 0 && items.value.length > 1) {
      createMessage.warning('单品库位最多只能配置 1 个 SPU，请先删除多余商品后再切换');
      isMixed.value = 1;
    }
  }

  function handleAddRow() {
    if (items.value.length >= maxAllowedSpu.value) {
      createMessage.warning(addDisabledReason.value || '已达到最大配置数量限制');
      return;
    }
    items.value.push({
      _tempId: tempIdSeed++,
      spuId: undefined,
      spuCode: '',
      spuName: '',
      maxCount: undefined,
      currentCount: 0,
      remainCount: 0,
      remark: '',
    });
  }

  function handleRemoveRow(index: number) {
    items.value.splice(index, 1);
  }

  async function handleSubmit() {
    const locaId = locaInfo.value.locaId || locaInfo.value.id;
    if (!locaId) {
      createMessage.error('缺少库位ID');
      return;
    }

    // 校验
    if (items.value.length > 3) {
      createMessage.error('单个库位最多不能超过 3 个商品(SPU)！');
      return;
    }
    if (isMixed.value === 0 && items.value.length > 1) {
      createMessage.error('单品库位最多只能配置 1 个商品(SPU)！若需存放多种商品，请选择混合库位');
      return;
    }

    const spuIdSet = new Set<any>();
    for (let i = 0; i < items.value.length; i++) {
      const item = items.value[i];
      if (!item.spuId) {
        createMessage.warning(`第 ${i + 1} 行未选择商品(SPU)！`);
        return;
      }
      if (spuIdSet.has(item.spuId)) {
        createMessage.warning(`商品 [${item.spuName || item.spuId}] 重复配置，同一库位不可重复规划相同商品！`);
        return;
      }
      spuIdSet.add(item.spuId);

      if (!item.maxCount || item.maxCount <= 0) {
        createMessage.warning(`第 ${i + 1} 行商品的最大可存放件数必须大于0！`);
        return;
      }
    }

    try {
      submitting.value = true;
      const payload = {
        locaId,
        items: items.value.map((i) => ({
          spuId: i.spuId,
          maxCount: i.maxCount,
          remark: i.remark,
        })),
      };
      await saveLocaCapacityBatch(payload);
      createMessage.success('库位商品规划配置保存成功！');
      emit('success');
      closeModal();
    } catch (e: any) {
      createMessage.error(e?.message || '保存失败');
    } finally {
      submitting.value = false;
    }
  }
</script>
