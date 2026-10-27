package org.jeecg.modules.wms.vo;

import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.entity.WmsItemSpu;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecgframework.poi.excel.annotation.ExcelCollection;

import java.util.List;

/**
 * @Description: 商品管理
 * @Author: jeecg-boot
 * @Date: 2026-10-27
 * @Version: V1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WmsItemSpuPage extends WmsItemSpu {

    @ExcelCollection(name = "SKU信息")
    private List<WmsItemSku> wmsItemSkuList;
}
