<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="生成二维码" :width="980" destroyOnClose :showOkBtn="false" :showCancelBtn="false">
    <div id="wms-item-qrcode-print" class="code-sheet" :style="sheetStyle">
      <div v-for="(item, index) in displayItems" :key="`${item.text}-${index}`" class="code-card" :style="cardStyle">
        <img v-if="images[index]" :src="images[index]" alt="" />
        <div class="code-text">
          <div v-for="line in item.lines" :key="line">{{ line }}</div>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="print-footer">
        <div class="print-settings">
          <span>每行放置数量</span>
          <a-input-number v-model:value="perRow" :min="1" :precision="0" />
          <span>行间距(px)</span>
          <a-input-number v-model:value="rowGap" :min="0" :precision="0" />
          <span>个数</span>
          <a-input-number v-model:value="copies" :min="1" :precision="0" />
        </div>
        <div>
          <a-button @click="closeModal">关闭</a-button>
          <a-button type="primary" class="ml-2" @click="handlePrint">打印</a-button>
        </div>
      </div>
    </template>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { computed, ref, watch } from 'vue';
  import { toDataURL } from 'qrcode';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { printJS } from '/@/hooks/web/usePrintJS';
  import { buildQrLines, buildQrText, ItemCodeDetail } from '../itemCode';

  const details = ref<ItemCodeDetail[]>([]);
  const perRow = ref(3);
  const rowGap = ref(16);
  const copies = ref(1);
  const images = ref<string[]>([]);

  const [registerModal, { closeModal }] = useModalInner((data) => {
    details.value = data?.details || [];
    perRow.value = 3;
    rowGap.value = 16;
    copies.value = 1;
  });

  const displayItems = computed(() => {
    const repeat = normalizeCount(copies.value, 1);
    const items: { text: string; lines: string[] }[] = [];
    details.value.forEach((detail) => {
      const item = { text: buildQrText(detail), lines: buildQrLines(detail) };
      for (let index = 0; index < repeat; index += 1) {
        items.push(item);
      }
    });
    return items;
  });

  const sheetStyle = computed(() => ({
    display: 'flex',
    flexWrap: 'wrap',
    alignItems: 'flex-start',
    columnGap: '12px',
    rowGap: `${normalizeCount(rowGap.value, 0)}px`,
  }));

  const cardStyle = computed(() => {
    const count = normalizeCount(perRow.value, 1);
    return {
      width: `calc((100% - ${(count - 1) * 12}px) / ${count})`,
    };
  });

  let imageToken = 0;
  watch(
    displayItems,
    async (items) => {
      const token = ++imageToken;
      const next = await Promise.all(items.map((item) => toDataURL(item.text, { margin: 1, width: 180, errorCorrectionLevel: 'M' })));
      if (token === imageToken) {
        images.value = next;
      }
    },
    { immediate: true },
  );

  watch(copies, (value) => {
    if (normalizeCount(value, 0) < 1) {
      copies.value = 1;
    }
  });

  watch(perRow, (value) => {
    if (normalizeCount(value, 0) < 1) {
      perRow.value = 1;
    }
  });

  watch(rowGap, (value) => {
    if (normalizeCount(value, -1) < 0) {
      rowGap.value = 0;
    }
  });

  function normalizeCount(value, fallback) {
    const number = Number(value);
    if (!Number.isFinite(number)) {
      return fallback;
    }
    return Math.floor(number);
  }

  function handlePrint() {
    if (normalizeCount(copies.value, 0) < 1) {
      copies.value = 1;
      return;
    }
    printJS({ printable: 'wms-item-qrcode-print', type: 'html' });
  }
</script>

<style lang="less" scoped>
  .code-card {
    box-sizing: border-box;
    padding: 8px;
    text-align: center;

    img {
      width: 160px;
      height: 160px;
    }
  }

  .code-text {
    margin-top: 8px;
    font-size: 12px;
    line-height: 1.5;
    text-align: left;
    word-break: break-all;
  }

  .print-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    gap: 12px;
  }

  .print-settings {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }
</style>
