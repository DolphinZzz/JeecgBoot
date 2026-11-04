<template>
  <div :class="prefixCls">
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <!-- 插槽: table标题操作按钮 -->
      <template #tableTitle>
        <a-button preIcon="ant-design:plus-outlined" type="primary" @click="handleAdd">新增</a-button>
        <a-button type="primary" preIcon="ant-design:export-outlined" @click="onExportXls">导出</a-button>
        <j-upload-button type="primary" preIcon="ant-design:import-outlined" @click="onImportXls">导入</j-upload-button>
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
      </template>

      <!-- 规则编码点击进入规则组成 -->
      <template #ruleCode="{ text, record }">
        <a @click="handleGoPart(record)" style="color: #1890ff; cursor: pointer;">
          {{ text }}
        </a>
      </template>

      <!-- 操作栏 -->
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
    </BasicTable>
    <AutoCodeRuleModal @register="registerModal" @success="reload" />
  </div>
</template>

<script name="system-autocode" lang="ts" setup>
  import { useRouter } from 'vue-router';
  import { BasicTable, TableAction, ActionItem } from '/@/components/Table';
  import { useListPage } from '/@/hooks/system/useListPage';
  import { useModal } from '/@/components/Modal';
  import Icon from '/@/components/Icon';
  import AutoCodeRuleModal from './components/AutoCodeRuleModal.vue';
  import {
    getAutoCodeRuleList,
    deleteAutoCodeRule,
    batchDeleteAutoCodeRule,
    exportUrl,
    importUrl,
  } from './autoCode.api';
  import { columns, searchFormSchema } from './autoCode.data';

  const [registerModal, { openModal }] = useModal();
  const router = useRouter();

  /**
   * 跳转到规则组成页面
   */
  function handleGoPart(record) {
    if (record?.id) {
      router.push({ path: `/autocodePart/index/${record.id}` });
    }
  }

  // 列表页面公共参数、方法
  const { prefixCls, tableContext, onExportXls, onImportXls } = useListPage({
    designScope: 'auto-code-rule',
    tableProps: {
      title: '编码规则列表',
      api: getAutoCodeRuleList,
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
    exportConfig: {
      url: exportUrl,
      name: '系统编码规则列表',
    },
    importConfig: {
      url: importUrl,
      success: () => reload(),
    },
  });

  const [registerTable, { reload }, { rowSelection, selectedRowKeys }] = tableContext;

  /**
   * 新增事件
   */
  function handleAdd() {
    openModal(true, {
      isUpdate: false,
    });
  }

  /**
   * 编辑事件
   */
  function handleEdit(record) {
    openModal(true, {
      record,
      isUpdate: true,
    });
  }

  /**
   * 删除事件
   */
  async function handleDelete(record) {
    await deleteAutoCodeRule({ id: record.id }, reload);
  }

  /**
   * 批量删除事件
   */
  async function batchHandleDelete() {
    await batchDeleteAutoCodeRule({ ids: selectedRowKeys.value.join(',') }, () => {
      selectedRowKeys.value = [];
      reload();
    });
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
