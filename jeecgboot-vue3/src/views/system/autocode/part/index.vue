<template>
  <div :class="prefixCls">
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <!-- 插槽: table标题操作按钮 -->
      <template #tableTitle>
        <a-button preIcon="ant-design:plus-outlined" type="primary" @click="handleAdd">新增</a-button>
        <a-button
          preIcon="ant-design:edit-outlined"
          type="primary"
          :disabled="selectedRowKeys.length !== 1"
          @click="handleSingleEdit"
        >
          修改
        </a-button>
        <a-dropdown v-if="selectedRowKeys.length > 0">
          <template #overlay>
            <a-menu>
              <a-menu-item key="1" @click="batchHandleDelete">
                <Icon icon="ant-design:delete-outlined" />
                <span>批量删除</span>
              </a-menu-item>
            </a-menu>
          </template>
          <a-button>
            <span>批量操作</span>
            <Icon icon="mdi:chevron-down" />
          </a-button>
        </a-dropdown>
        <a-button preIcon="ant-design:close-outlined" @click="handleClose">关闭</a-button>
      </template>

      <!-- 操作栏 -->
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
    </BasicTable>

    <AutoCodePartModal @register="registerModal" @success="reload" />
  </div>
</template>

<script name="autocodePart-index" lang="ts" setup>
  import { ref, computed, unref, onMounted } from 'vue';
  import { useRoute } from 'vue-router';
  import { BasicTable, TableAction, ActionItem } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useModal } from '/@/components/Modal';
  import { useTabs } from '/@/hooks/web/useTabs';
  import Icon from '/@/components/Icon';
  import AutoCodePartModal from './components/AutoCodePartModal.vue';
  import {
    getAutoCodePartList,
    deleteAutoCodePart,
    batchDeleteAutoCodePart,
    getAutoCodeRuleById,
  } from './autoCodePart.api';
  import { columns, searchFormSchema } from './autoCodePart.data';

  const route = useRoute();
  const { closeCurrent } = useTabs();
  const ruleId = computed(() => (route.params?.id as string) || '');
  const ruleInfo = ref<any>({});

  const [registerModal, { openModal }] = useModal();

  // 列表页面公共参数、方法
  const { prefixCls, tableContext } = useListPage({
    designScope: 'auto-code-part',
    tableProps: {
      title: () => {
        const info = unref(ruleInfo);
        if (info.ruleCode) {
          return `规则组成列表（规则：${info.ruleCode} - ${info.ruleName || ''}）`;
        }
        return '规则组成列表';
      },
      api: (params) => {
        return getAutoCodePartList({ ...params, ruleId: unref(ruleId) });
      },
      columns,
      showIndexColumn: true,
      formConfig: {
        schemas: searchFormSchema,
      },
      actionColumn: {
        width: 140,
        title: '操作',
        dataIndex: 'action',
        slots: { customRender: 'action' },
      },
    },
  });

  const [registerTable, { reload, getSelectRows }, { rowSelection, selectedRowKeys }] = tableContext;

  onMounted(async () => {
    if (unref(ruleId)) {
      try {
        const res = await getAutoCodeRuleById(unref(ruleId));
        if (res) {
          ruleInfo.value = res;
        }
      } catch (e) {
        // 忽略非关键异常
      }
    }
  });

  /**
   * 新增事件
   */
  function handleAdd() {
    openModal(true, {
      isUpdate: false,
      ruleId: unref(ruleId),
    });
  }

  /**
   * 顶部单个修改按钮
   */
  function handleSingleEdit() {
    const rows = getSelectRows();
    if (rows && rows.length === 1) {
      handleEdit(rows[0]);
    }
  }

  /**
   * 编辑事件
   */
  function handleEdit(record) {
    openModal(true, {
      record,
      isUpdate: true,
      ruleId: unref(ruleId),
    });
  }

  /**
   * 删除事件
   */
  async function handleDelete(record) {
    await deleteAutoCodePart({ id: record.id }, reload);
  }

  /**
   * 批量删除事件
   */
  async function batchHandleDelete() {
    await batchDeleteAutoCodePart({ ids: selectedRowKeys.value.join(',') }, () => {
      selectedRowKeys.value = [];
      reload();
    });
  }

  /**
   * 关闭页面返回
   */
  function handleClose() {
    closeCurrent();
  }

  /**
   * 行操作按钮
   */
  function getTableAction(record): ActionItem[] {
    return [
      {
        label: '编辑',
        onClick: handleEdit.bind(null, record),
      },
      {
        label: '删除',
        color: 'error',
        popConfirm: {
          title: '确认要删除吗？',
          confirm: handleDelete.bind(null, record),
        },
      },
    ];
  }
</script>
