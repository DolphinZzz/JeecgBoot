<template>
  <BasicModal v-bind="$attrs" @register="registerModal" destroyOnClose title="提交质检结果" :width="720" @ok="handleSubmit">
    <a-descriptions :column="3" size="small" bordered style="margin-bottom: 12px">
      <a-descriptions-item label="入库单号">{{ record.entryCode || '-' }}</a-descriptions-item>
      <a-descriptions-item label="仓型">{{ record.warehouseType === 'INDUSTRIAL' ? '工业制造仓' : '普通仓' }}</a-descriptions-item>
      <a-descriptions-item label="实收数量">{{ record.receivedCount ?? '-' }}</a-descriptions-item>
    </a-descriptions>

    <BasicForm @register="registerForm" @field-value-change="handleFieldChange" />

    <!-- 不合格品退货与供应商联络区域 -->
    <a-card
      v-if="failCount > 0"
      size="small"
      title="不合格品退货安置与供应商联络"
      style="margin-top: 10px; border-color: #ffccc7; background-color: #fff1f0"
    >
      <template #extra>
        <a-tag color="error">待退货安置 {{ failCount }} 件</a-tag>
      </template>

      <!-- 供应商联系卡片 -->
      <a-descriptions :column="2" size="small" bordered style="background: #fff; margin-bottom: 12px">
        <a-descriptions-item label="对应供应商">
          <span style="font-weight: 600; color: #cf1322">{{ record.supplierName || '未关联供应商' }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="联系人及电话">
          {{ [record.supplierLinkName, record.supplierLinkPhone].filter(Boolean).join(' / ') || '暂无电话' }}
        </a-descriptions-item>
        <a-descriptions-item label="供应商地址" :span="2">
          {{ record.supplierAddress || '暂无详细地址' }}
        </a-descriptions-item>
      </a-descriptions>

      <a-form layout="vertical">
        <a-form-item
          label="退货货架库位（不良品放置区）"
          help="不合格货物必须放置于退货货架区（RETURN）。可现在直接指定落库，也可稍后在列表中单独处置。"
        >
          <a-select
            v-model:value="returnLocaId"
            placeholder="请选择退货区货架库位（可留空稍后单独处置）"
            allowClear
            style="width: 100%"
          >
            <a-select-option v-for="item in returnLocaList" :key="item.id" :value="item.id">
              {{ item.locaCode }} [{{ item.shelfNumber ? `货架${item.shelfNumber}` : '' }} {{ item.layerNumber ? `第${item.layerNumber}层` : '' }} 承重{{ item.localBearingCapacity || 0 }}g]
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="退货处置/联系供应商沟通备注">
          <a-textarea
            v-model:value="returnRemark"
            placeholder="例如：已电话联系供应商李经理、协商下周顺丰退回或供应商上门提货"
            :rows="2"
            maxlength="200"
          />
        </a-form-item>
      </a-form>
    </a-card>

    <a-alert type="info" show-icon style="margin-top: 10px">
      <template #message>
        合格数 + 不合格数不能超过送检数量：全合格判「合格」，全不合格判「不合格」，两者都有判「部分合格」。合格品走正常上架，不合格品必须安置到退货货架区。
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
  import { submitQc, listReturnLocaOptions } from '../WmsQualityCheck.api';

  const { createMessage } = useMessage();
  const emit = defineEmits(['register', 'success']);

  const record = ref<Recordable>({});
  const checkCount = ref(0);
  const qcType = ref(2);
  const sampleCount = ref<number | undefined>(undefined);
  const passCount = ref(0);
  const failCount = ref(0);

  const returnLocaList = ref<any[]>([]);
  const returnLocaId = ref<number | undefined>(undefined);
  const returnRemark = ref<string>('');

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

  function handleFieldChange(key: string, val: any) {
    if (key === 'passCount') {
      const p = Number(val ?? 0);
      const f = Math.max(0, checkCount.value - p);
      passCount.value = p;
      failCount.value = f;
      setFieldsValue({ failCount: f });
    } else if (key === 'failCount') {
      const f = Number(val ?? 0);
      const p = Math.max(0, checkCount.value - f);
      failCount.value = f;
      passCount.value = p;
      setFieldsValue({ passCount: p });
    } else if (key === 'qcType') {
      qcType.value = Number(val);
      if (qcType.value === 1) {
        passCount.value = checkCount.value;
        failCount.value = 0;
        setFieldsValue({ passCount: checkCount.value, failCount: 0 });
      }
    }
  }

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
    returnLocaId.value = undefined;
    returnRemark.value = '';

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

    // 加载当前仓库退货货架区库位列表
    if (row.id) {
      try {
        const res: any = await listReturnLocaOptions({ qcId: row.id, warehouseId: row.warehouseId });
        returnLocaList.value = Array.isArray(res) ? res : res?.result || [];
      } catch (e) {
        returnLocaList.value = [];
      }
    }
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
        returnLocaId: returnLocaId.value || null,
        returnRemark: returnRemark.value ? returnRemark.value.trim() : null,
        lines: [
          {
            qcId: record.value.id,
            qcType: Number(values.qcType ?? 2),
            sampleCount: values.sampleCount === undefined || values.sampleCount === null ? null : Number(values.sampleCount),
            passCount: pass,
            failCount: fail,
            failReason: values.failReason?.trim() || null,
            returnLocaId: returnLocaId.value || null,
            returnRemark: returnRemark.value ? returnRemark.value.trim() : null,
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
