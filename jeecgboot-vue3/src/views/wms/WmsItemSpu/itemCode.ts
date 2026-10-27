export interface ItemCodeDetail {
  productCode: string;
  productName: string;
  specCode: string;
  specName: string;
  specUnit: string;
  barcode: string;
}

function textOf(value) {
  return value === undefined || value === null ? '' : String(value);
}

/** 每个商品信息详情对应一条码内容。商品编码、名称取商品行，规格和条码取详情行。 */
export function collectItemDetails(spuList): ItemCodeDetail[] {
  const details: ItemCodeDetail[] = [];
  (spuList || []).forEach((spu) => {
    (spu.children || []).forEach((sku) => {
      details.push({
        productCode: textOf(spu.productCode || spu.subTitle),
        productName: textOf(spu.productName || spu.title),
        specCode: textOf(sku.productCode),
        specName: textOf(sku.productName),
        specUnit: textOf(sku.categoryName),
        barcode: textOf(sku.code).trim(),
      });
    });
  });
  return details;
}

export function buildQrLines(detail: ItemCodeDetail) {
  return [
    `商品编码：${detail.productCode}`,
    `商品名称：${detail.productName}`,
    `规格编码：${detail.specCode}`,
    `规格名称：${detail.specName}`,
    `商品单位：${detail.specUnit}`,
  ];
}

/** 扫码内容。键与库表一致：sub_title、title 为商品表字段，spec 为规格列里保存的 JSON。 */
export function buildQrPayload(detail: ItemCodeDetail) {
  return {
    sub_title: detail.productCode,
    title: detail.productName,
    spec: {
      specCode: detail.specCode,
      specName: detail.specName,
      unit: detail.specUnit,
    },
  };
}

export function buildQrText(detail: ItemCodeDetail) {
  return JSON.stringify(buildQrPayload(detail));
}
