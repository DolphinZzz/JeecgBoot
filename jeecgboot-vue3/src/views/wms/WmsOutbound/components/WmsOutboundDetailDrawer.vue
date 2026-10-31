<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    title="出库单详情"
    :width="960"
    :showFooter="false"
  >
    <div v-loading="loading">
      <a-descriptions bordered size="small" :column="3" style="margin-bottom: 16px">
        <a-descriptions-item label="出库单号">
          <b>{{ orderInfo?.outboundCode }}</b>
        </a-descriptions-item>
        <a-descriptions-item label="出库类型">
          <a-tag :color="outboundTypeMap[orderInfo?.outboundType]?.color || 'blue'">
            {{ outboundTypeMap[orderInfo?.outboundType]?.text || orderInfo?.outboundType_dictText || '-' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="单据状态">
          <a-tag :color="statusColorMap[orderInfo?.status] || 'default'">
            {{ statusTextMap[orderInfo?.status] || orderInfo?.status_dictText || '-' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="仓库">{{ orderInfo?.warehouseName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="货主">{{ orderInfo?.consignorName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ orderInfo?.customerName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="计划总数">{{ orderInfo?.totalCount || 0 }}</a-descriptions-item>
        <a-descriptions-item label="分配数">{{ orderInfo?.allocatedTotal ?? orderInfo?.allocatedCount ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="实拣数">{{ orderInfo?.pickedTotal ?? orderInfo?.pickedCount ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="MES工单号">{{ orderInfo?.mesWorkOrderNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="物流公司">{{ orderInfo?.logisticsCompanyName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="物流单号">{{ orderInfo?.logisticsNumber || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间" :span="2">{{ orderInfo?.createTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="订单备注" :span="3">{{ orderInfo?.remark || '-' }}</a-descriptions-item>
      </a-descriptions>

      <a-tabs default-active-key="1">
        <a-tab-pane key="1" tab="出库商品明细">
          <a-table
            size="small"
            bordered
            :pagination="false"
            :columns="detailColumns"
            :data-source="detailList"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'pickStatus'">
                <a-tag :color="record.pickStatus === 2 ? 'success' : record.pickStatus === 1 ? 'processing' : 'default'">
                  {{ record.pickStatus === 2 ? '已拣货' : record.pickStatus === 1 ? '部分拣货' : '未拣货' }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="2" tab="包装箱单">
          <a-table
            size="small"
            bordered
            :pagination="false"
            :columns="packageColumns"
            :data-source="packageList"
            row-key="id"
          />
        </a-tab-pane>

        <a-tab-pane key="3" tab="称重记录">
          <a-table
            size="small"
            bordered
            :pagination="false"
            :columns="weightColumns"
            :data-source="weightList"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'diffWeight'">
                <span :style="{ color: Math.abs(record.diffWeight || 0) > 0.5 ? '#ff4d4f' : '#52c41a', fontWeight: 'bold' }">
                  {{ record.diffWeight !== undefined && record.diffWeight !== null ? `${record.diffWeight} kg` : '-' }}
                </span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="4" tab="SN追溯明细">
          <a-table
            size="small"
            bordered
            :pagination="false"
            :columns="snColumns"
            :data-source="snList"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 5 ? 'purple' : record.status === 4 ? 'orange' : 'green'">
                  {{ record.status === 5 ? '已出库' : record.status === 4 ? '拣货锁定' : '在库良品' }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </div>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { queryById } from '../WmsOutbound.api';
  import { statusColorMap, statusTextMap, outboundTypeMap } from '../WmsOutbound.data';

  const loading = ref(false);
  const orderInfo = ref<any>(null);
  const detailList = ref<any[]>([]);
  const packageList = ref<any[]>([]);
  const weightList = ref<any[]>([]);
  const snList = ref<any[]>([]);

  const detailColumns = [
    { title: '商品编码', dataIndex: 'skuCode', width: 140 },
    { title: '商品名称', dataIndex: 'skuName', width: 180 },
    { title: '计划件数', dataIndex: 'outCount', width: 90, align: 'center' },
    { title: '分配件数', dataIndex: 'allocatedCount', width: 90, align: 'center' },
    { title: '实拣件数', dataIndex: 'pickedCount', width: 90, align: 'center' },
    { title: '推荐库位', dataIndex: 'locaCode', width: 120, align: 'center' },
    { title: '分配批次', dataIndex: 'batchNumber', width: 140, align: 'center' },
    { title: '拣货状态', key: 'pickStatus', width: 100, align: 'center' },
  ];

  const packageColumns = [
    { title: '箱号', dataIndex: 'packageCode', width: 180 },
    { title: '包装类型', dataIndex: 'packageType', width: 120, align: 'center' },
    { title: '创建时间', dataIndex: 'createTime', width: 160, align: 'center' },
  ];

  const weightColumns = [
    { title: '箱号', dataIndex: 'packageCode', width: 180 },
    { title: '实测重量 (kg)', dataIndex: 'actualWeight', width: 120, align: 'center' },
    { title: '理论重量 (kg)', dataIndex: 'theoreticalWeight', width: 120, align: 'center' },
    { title: '重量偏差', key: 'diffWeight', width: 120, align: 'center' },
    { title: '称重时间', dataIndex: 'createTime', width: 160, align: 'center' },
  ];

  const snColumns = [
    { title: 'SN序列号', dataIndex: 'snCode', width: 200 },
    { title: '商品SKU', dataIndex: 'skuId', width: 120, align: 'center' },
    { title: '状态', key: 'status', width: 120, align: 'center' },
    { title: '更新时间', dataIndex: 'updateTime', width: 160, align: 'center' },
  ];

  const [registerDrawer] = useDrawerInner(async (data) => {
    orderInfo.value = null;
    detailList.value = [];
    packageList.value = [];
    weightList.value = [];
    snList.value = [];

    if (data?.id) {
      await loadDetail(data.id);
    }
  });

  async function loadDetail(id: number | string) {
    loading.value = true;
    try {
      const res = await queryById(id);
      orderInfo.value = res?.outboundOrder || {};
      detailList.value = res?.details || [];
      packageList.value = res?.packages || [];
      weightList.value = res?.weights || [];
      snList.value = res?.snList || [];
    } finally {
      loading.value = false;
    }
  }
</script>
