package org.jeecg.modules.wms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import java.util.Map;
import org.jeecg.modules.wms.entity.WmsWarehouseOperations;
import org.jeecg.modules.wms.entity.WmsWarehouseOperationsDetails;
import org.jeecg.modules.wms.vo.WmsOperationForm;

/**
 * @Description: 库内作业（移库移位 / 补货）
 *
 * 约定：
 * 1. 作业单只「生成」，不自动执行；执行由人在 PDA/页面确认后调用 {@link #execute}；
 * 2. 执行在一个事务内原子完成扣增，全仓 total_count 严格不变；
 * 3. 目标库位必须通过四重兼容性校验：未禁用/未盘点锁、温区匹配、混放策略允许、容积承重充足。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
public interface IWmsWarehouseOperationsService extends IService<WmsWarehouseOperations> {

    /**
     * 新建移库 / 补货作业单（状态=未确认）。
     */
    WmsWarehouseOperations createOperation(WmsOperationForm form);

    /**
     * 执行作业单：逐行扣减源库位、增加目标库位，全部成功才提交。
     */
    Map<String, Object> execute(Long id);

    /**
     * 取消未执行的作业单。
     */
    void cancel(Long id);

    /**
     * 查询作业单明细。
     */
    List<WmsWarehouseOperationsDetails> listDetails(Long operationId);

    /**
     * 按拣货区补货上下限自动生成补货建议（FEFO：效期最近、批次最小优先），返回新建的作业单。
     *
     * @param warehouseId 仓库ID
     * @param skuIds      指定 SKU；为空表示扫描该仓库全部启用补货维度的 SKU
     */
    WmsWarehouseOperations generateReplenish(Integer warehouseId, List<Long> skuIds);
}
