<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose title="提交质检结果" :width="680" @ok="handleSubmit">
    <a-descriptions :column="3" size="small" bordered style="margin-bottom: 12px">
      <a-descriptions-item label="入库单号">{{ record.entryCode || '-' }}</a-descriptions-item>
      <a-descriptions-item label="仓型">{{ record.warehouseType === 'INDUSTRIAL' ? '工业制造仓' : '普通仓' }}</a-descriptions-item>
      <a-descriptions-item label="实收数量">{{ record.receivedCount ?? '-' }}</a-descriptions-item>
    </a-descriptions>
    <BasicForm @register="registerForm" />
    <a-alert type="info" show-icon style="margin-top: 4px">
      <template #message>
        合格数 + 不合格数不能超过送检数量：全合格判「合格」，全不合格判「不合格」，两者都有判「部分合格」。不合格品只能上架到退货区。
      </template>
    </a-alert>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref, watch } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { submitFormSchema } from '../WmsQualityCheck.data';
  import { submitQc } from '../WmsQualityCheck.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);

  const record = ref<Recordable>({});
  const checkCount = ref(0);
  const qcType = ref(2);
  const sampleCount = ref<number | undefined>(undefined);
  const passCount = ref(0);
  const failCount = ref(0);

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    schemas: submitFormSchema,
    showActionButtonGroup: false,
    baseColProps: { span: 12 },
    labelWidth: 100,
  });

  // 免检直接按送检数全合格，其余方式由作业员填数
  const autoPass = computed(() => qcType.value === 1);
  watch(autoPass, (val) => {
    if (val) {
      passCount.value = checkCount.value;
      failCount.value = 0;
      setFieldsValue({ passCount: checkCount.value, failCount: 0 });
    }
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
    const row = data?.record || {};
    record.value = row;
    checkCount.value = Number(row.checkCount ?? row.receivedCount ?? 0);
    qcType.value = Number(row.qcType ?? 2);
    sampleCount.value = row.sampleCount ?? undefined;
    passCount.value = Number(row.passCount ?? 0) || checkCount.value;
    failCount.value = Number(row.failCount ?? 0);
    await setFieldsValue({
      qcCode: row.qcCode,
      skuText: `${row.skuCode || row.skuId_dictText || row.skuId || '-'} ${row.spuTitle || ''}`.trim(),
      batchNumber: row.batchNumber || '-',
      qcType: qcType.value,
      checkCount: checkCount.value,
      sampleCount: sampleCount.value,
      passCount: passCount.value,
      failCount: failCount.value,
      failReason: row.failReason || '',
    });
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      const pass = Number(values.passCount ?? 0);
      const fail = Number(values.failCount ?? 0);
      if (pass + fail > checkCount.value) {
        createMessage.warning(`合格数 ${pass} + 不合格数 ${fail} 不能超过送检数 ${checkCount.value}`);
        return;
      }
      if (fail > 0 && !values.failReason?.trim()) {
        createMessage.warning('存在不合格品时请填写不合格原因');
        return;
      }
      setModalProps({ confirmLoading: true });
      const res: any = await submitQc({
        qcIds: [record.value.id],
        lines: [
          {
            qcId: record.value.id,
            qcType: Number(values.qcType ?? 2),
            sampleCount: values.sampleCount === undefined || values.sampleCount === null ? null : Number(values.sampleCount),
            passCount: pass,
            failCount: fail,
            failReason: values.failReason?.trim() || null,
          },
        ],
        failReason: values.failReason?.trim() || null,
      });
      const data = res?.result || res || {};
      createMessage.success(data.message || res?.message || '质检已提交');
      closeModal();
      emit('success', data);
    } catch (e: any) {
      if (e?.errorFields) return;
      createMessage.error(e?.message || '提交质检失败');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
