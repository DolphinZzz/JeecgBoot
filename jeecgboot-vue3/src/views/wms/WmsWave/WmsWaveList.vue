<template>
  <div>
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button
          type="primary"
          v-auth="'wms:wms_wave_order:generate'"
          preIcon="ant-design:branches-outlined"
          @click="handleGenerate"
        >
          自动分波
        </a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" :dropDownActions="getDropDownAction(record)" />
      </template>
    </BasicTable>

    <!-- 弹窗与抽屉 -->
    <WmsWaveGenerateModal @register="registerGenerateModal" @success="reload" />
    <WmsWaveDetailDrawer @register="registerDetailDrawer" />
  </div>
</template>

<script lang="ts" name="wms-wmsWave" setup>
  import { reactive } from 'vue';
  import { BasicTable, TableAction, ActionItem } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { useDrawer } from '/@/components/Drawer';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useMessage } from '/@/hooks/web/useMessage';
  import WmsWaveGenerateModal from './components/WmsWaveGenerateModal.vue';
  import WmsWaveDetailDrawer from './components/WmsWaveDetailDrawer.vue';
  import { columns, searchFormSchema } from './WmsWave.data';
  import { list, startWavePick, completeWaveDistribute, cancelWave } from './WmsWave.api';

  const { createMessage, createConfirm } = useMessage();
  const queryParam = reactive<any>({});

  const [registerGenerateModal, { openModal: openGenerateModal }] = useModal();
  const [registerDetailDrawer, { openDrawer: openDetailDrawer }] = useDrawer();

  const { tableContext } = useListPage({
    tableProps: {
      title: '波次拣货与播种管理',
      api: list,
      columns,
      canResize: true,
      formConfig: {
        schemas: searchFormSchema,
        autoSubmitOnEnter: true,
        showAdvancedButton: true,
      },
      actionColumn: {
        title: '操作',
        dataIndex: 'action',
        width: 170,
        fixed: 'right',
        slots: { customRender: 'action' },
      },
      beforeFetch: (params) => {
        return Object.assign(params, queryParam);
      },
    },
  });

  const [registerTable, { reload }] = tableContext;

  function handleGenerate() {
    openGenerateModal(true);
  }

  function handleViewDetail(record: Recordable) {
    openDetailDrawer(true, { id: record.id });
  }

  function handleStartPick(record: Recordable) {
    createConfirm({
      iconType: 'info',
      title: '开始波次拣货',
      content: `是否确认开始波次 [${record.waveCode}] 的集中拣货作业？关联的所有出库单将同步进入拣货中。`,
      okText: '确认开始',
      onOk: async () => {
        await startWavePick(record.id, undefined);
        createMessage.success('波次拣货已启动！');
        reload();
      },
    });
  }

  function handleCompleteDistribute(record: Recordable) {
    createConfirm({
      iconType: 'info',
      title: '完成播种分货',
      content: `确认已按播种货架完成分货？系统将把波次标记为已完成，并将包含的 ${record.orderCount || 0} 笔出库单自动回填实拣数量并推进至【待复核】状态。`,
      okText: '确认完成',
      onOk: async () => {
        await completeWaveDistribute(record.id);
        createMessage.success('播种分货完成！关联出库单已全部进入待复核状态。');
        reload();
      },
    });
  }

  function handleCancelWave(record: Recordable) {
    createConfirm({
      iconType: 'warning',
      title: '取消波次',
      content: `是否取消波次 [${record.waveCode}]？取消后关联出库单将解除绑定并恢复为独立待拣货状态。`,
      okText: '确认取消',
      onOk: async () => {
        await cancelWave(record.id);
        createMessage.success('波次已取消，出库单已释放！');
        reload();
      },
    });
  }

  function getTableAction(record: Recordable): ActionItem[] {
    const actions: ActionItem[] = [];

    if (record.status === 1) {
      actions.push({
        label: '开始拣货',
        onClick: handleStartPick.bind(null, record),
        auth: 'wms:wms_wave_order:startPick',
      });
    } else if (record.status === 2 || record.status === 4) {
      actions.push({
        label: '完成分货播种',
        onClick: handleCompleteDistribute.bind(null, record),
        auth: 'wms:wms_wave_order:completeDistribute',
      });
    }

    actions.push({
      label: '详情',
      onClick: handleViewDetail.bind(null, record),
    });

    return actions;
  }

  function getDropDownAction(record: Recordable): ActionItem[] {
    const drops: ActionItem[] = [];

    if (record.status < 5 && record.status !== 9) {
      drops.push({
        label: '取消波次',
        color: 'error',
        onClick: handleCancelWave.bind(null, record),
        auth: 'wms:wms_wave_order:cancel',
      });
    }

    return drops;
  }
</script>
