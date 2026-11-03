package org.jeecg.modules.wms.service;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.wms.entity.WmsStocktakingOrder;
import org.jeecg.modules.wms.vo.WmsStocktakingOrderForm;
import org.jeecg.modules.wms.vo.WmsStocktakingSkuSelectVO;

/**
 * @Description: 盘库单服务接口
 * @Author: jeecg-boot
 * @Date: 2026-11-03
 * @Version: V1.0
 */
public interface IWmsStocktakingOrderService extends IService<WmsStocktakingOrder> {

    int STATUS_DRAFT = 0;       // 草稿/暂存
    int STATUS_COUNTED = 1;     // 已盘点(实盘已锁定，未调账)
    int STATUS_ADJUSTED = 2;    // 已调账(盘盈入库/盘亏出库调账完成)

    int ADJUST_STATUS_PENDING = 0; // 0 未生成
    int ADJUST_STATUS_DONE = 1;    // 1 已生成
    int ADJUST_STATUS_NONE = 2;    // 2 无需生成

    /**
     * 分页查询盘库单
     */
    IPage<WmsStocktakingOrder> queryPageList(Page<WmsStocktakingOrder> page,
                                            String orderNo,
                                            Integer warehouseId,
                                            Integer status,
                                            String startDate,
                                            String endDate);

    /**
     * 根据ID查询盘库单及明细
     */
    WmsStocktakingOrderForm queryDetailById(Long id);

    /**
     * 暂存盘库单（草稿模式，不影响物理库存）
     */
    WmsStocktakingOrder saveDraft(WmsStocktakingOrderForm form);

    /**
     * 完成盘点（仅锁定实盘数与盈亏统计，不自动触发库存调账）
     */
    WmsStocktakingOrder complete(WmsStocktakingOrderForm form);

    /**
     * 手动生成盘盈入库单并完成入库调账闭环
     *
     * @param orderId 盘库单ID
     * @return 生成的盘盈入库单号
     */
    String createProfitEntry(Long orderId);

    /**
     * 手动生成盘亏出库单并完成出库调账闭环
     *
     * @param orderId 盘库单ID
     * @return 生成的盘亏出库单号
     */
    String createLossOutbound(Long orderId);

    /**
     * 盘库商品选择器多维检索分页查询
     */
    IPage<WmsStocktakingSkuSelectVO> selectSkuPageForStocktaking(Page<WmsStocktakingSkuSelectVO> page,
                                                               Integer categoryId,
                                                               String skuCode,
                                                               String skuName,
                                                               String spuCode,
                                                               String spuName,
                                                               Integer warehouseId);

    /**
     * 一键导入仓库下所有有库存的商品明细
     */
    List<WmsStocktakingSkuSelectVO> importWarehouseAllSkus(Integer warehouseId);

    /**
     * 获取指定仓库下某SKU当前的账面库存
     */
    Long getSkuBookStock(Integer warehouseId, Long skuId);

    /**
     * 删除盘库单（仅草稿状态允许删除）
     */
    void deleteOrder(Long id);
}
