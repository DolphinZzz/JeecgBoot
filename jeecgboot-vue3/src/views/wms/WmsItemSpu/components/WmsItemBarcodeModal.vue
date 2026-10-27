<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="打印条码" :width="960" destroyOnClose :showOkBtn="false" :showCancelBtn="false">
    <div id="wms-item-barcode-print" class="code-sheet" :style="sheetStyle">
      <div v-for="(item, index) in barcodes" :key="index" class="code-card">
        <img :src="item.image" alt="" />
        <div class="code-text">{{ item.barcode }}</div>
      </div>
    </div>
    <template #footer>
      <div class="print-footer">
        <span class="print-tip">条码只包含商品信息详情中的条码编号</span>
        <div>
          <a-button @click="closeModal">关闭</a-button>
          <a-button type="primary" class="ml-2" @click="handlePrint">打印</a-button>
        </div>
      </div>
    </template>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { printJS } from '/@/hooks/web/usePrintJS';
  import { code128DataUrl } from '../code128';

  const barcodes = ref<{ barcode: string; image: string }[]>([]);

  const [registerModal, { closeModal }] = useModalInner((data) => {
    barcodes.value = (data?.barcodes || [])
      .map((barcode) => String(barcode || '').trim())
      .filter((barcode) => barcode !== '')
      .map((barcode) => ({ barcode, image: code128DataUrl(barcode) }));
  });

  const sheetStyle = computed(() => ({
    display: 'flex',
    flexWrap: 'wrap',
    alignItems: 'flex-start',
    columnGap: '16px',
    rowGap: '16px',
  }));

  function handlePrint() {
    printJS({ printable: 'wms-item-barcode-print', type: 'html' });
  }
</script>

<style lang="less" scoped>
  .code-card {
    width: 220px;
    padding: 8px;
    text-align: center;

    img {
      max-width: 100%;
      height: 72px;
    }
  }

  .code-text {
    margin-top: 6px;
    font-size: 13px;
    word-break: break-all;
  }

  .print-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
  }

  .print-tip {
    color: rgba(0, 0, 0, 0.45);
  }
</style>
