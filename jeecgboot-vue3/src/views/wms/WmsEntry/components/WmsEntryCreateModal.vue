<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose title="新建入库单" :width="1150" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
    <a-divider orientation="center" class="detail-divider">入库明细（一行为一个 SKU 的计划数量）</a-divider>
    <JVxeTable
      keep-source
      resizable
      ref="detailTableRef"
      :columns="createDetailColumns"
      :dataSource="tableData.dataSource"
      :height="300"
      :rowNumber="true"
      :toolbar="true"
    />
    <a-alert type="info" show-icon style="margin-top: 12px">
      <template #message>
        建单后单据状态为「待到货」；工业制造仓的「MES 生产入库」必须填写 MES 工单号。批次号留空时后端统一生成，整单共用一个批次。
      </template>
    </a-alert>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { JVxeTable } from '/@/components/jeecg/JVxeTable';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { createSchema, createDetailColumns } from '../WmsEntry.data';
  import { addEntry } from '../WmsEntry.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);

  const detailTableRef = ref();
  const tableData = reactive({ dataSource: [] as any[] });

  const [registerForm, { resetFields, validate }] = useForm({
    schemas: createSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 12 },
    labelWidth: 110,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async () => {
    await resetFields();
    tableData.dataSource = [];
    setModalProps({ confirmLoading: false });
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      const rows: any[] = detailTableRef.value?.getTableData() ?? [];
      const details = rows
        .filter((row) => row.skuId && Number(row.count) > 0)
        .map((row) => ({
          skuId: Number(row.skuId),
          count: Number(row.count),
          price: row.price === undefined || row.price === null || row.price === '' ? null : Number(row.price),
          supplierId: row.supplierId ? Number(row.supplierId) : null,
          prodDate: row.prodDate || null,
          expireDate: row.expireDate || null,
        }));
      if (details.length === 0) {
        createMessage.warning('请至少填写一行入库明细，商品与计划数量不能为空');
        return;
      }
      if (!values.mesWorkOrderNo && Number(values.entryType) === 2) {
        createMessage.warning('MES 生产入库必须填写 MES 工单号');
        return;
      }
      setModalProps({ confirmLoading: true });
      await addEntry({
        entryType: values.entryType ? Number(values.entryType) : 1,
        warehouseId: Number(values.warehouseId),
        consignorId: Number(values.consignorId),
        batchNumber: values.batchNumber || null,
        erpPoNo: values.erpPoNo || null,
        mesWorkOrderNo: values.mesWorkOrderNo || null,
        predictedArrivalTime: values.predictedArrivalTime || null,
        details,
      });
      createMessage.success('入库单已创建');
      closeModal();
      emit('success');
    } catch (e: any) {
      if (e?.errorFields) return;
      createMessage.error(e?.message || '新建入库单失败');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<style lang="less" scoped>
  .detail-divider {
    margin: 4px 0 16px;
    font-weight: 500;
  }
</style>
