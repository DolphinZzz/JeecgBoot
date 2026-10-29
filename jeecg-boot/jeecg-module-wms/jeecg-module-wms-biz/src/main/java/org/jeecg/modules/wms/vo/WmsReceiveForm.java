package org.jeecg.modules.wms.vo;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description: 收货清点入参（一次提交整单的实收 / 破损 / SN）
 *
 * 超收与少收都由后端按「实收 - 计划」算出来，前端只报实收数和破损数，
 * 避免前端算错导致 surpass_count / lack_count 与实收数对不上。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@Schema(description = "收货清点")
public class WmsReceiveForm {

    /**入库单ID*/
    @Schema(description = "入库单ID")
    private Long id;

    /**卸货员工ID*/
    @Schema(description = "卸货员工ID")
    private Integer unloadEmpId;

    /**明细收货行*/
    @Schema(description = "明细收货行")
    private List<WmsReceiveLine> details;

    @Data
    @Schema(description = "明细收货行")
    public static class WmsReceiveLine {

        /**入库明细ID*/
        @Schema(description = "入库明细ID")
        private Long detailId;

        /**实收数量（含破损件）*/
        @Schema(description = "实收数量")
        private Integer receivedCount;

        /**破损数量（直入退货区，不参与上架）*/
        @Schema(description = "破损数量")
        private Integer damageCount;

        /**扫描到的 SN 列表（该 SKU 启用 SN 管理时必填，件数需与实收 - 破损一致）*/
        @Schema(description = "SN 列表")
        private List<String> snCodes;
    }
}
