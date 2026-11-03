package org.jeecg.modules.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.wms.dto.ErpOutboundOrderCreateDto;
import org.jeecg.modules.wms.entity.WmsItemSn;
import org.jeecg.modules.wms.entity.WmsOutboundOrder;
import org.jeecg.modules.wms.entity.WmsOutboundOrderPackage;
import org.jeecg.modules.wms.entity.WmsOutboundOrderWeights;
import org.jeecg.modules.wms.vo.*;

import java.util.List;

/**
 * @Description: 出库单服务接口（出库全链路闭环核心）
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
public interface IWmsOutboundOrderService extends IService<WmsOutboundOrder> {

    /**
     * 出库单分页列表
     */
    IPage<WmsOutboundPage> queryOutboundPage(WmsOutboundQuery query, Integer pageNo, Integer pageSize);

    /**
     * 获取出库单详情（包含单头与统计指标）
     */
    WmsOutboundPage getOutboundDetail(Long orderId);

    /**
     * 查询出库单明细视图列表
     */
    List<WmsOutboundDetailView> listDetails(Long orderId);

    /**
     * 新建出库单（支持销售、MES领料、采购退货、调拨）
     */
    WmsOutboundOrder createOutboundOrder(WmsOutboundCreateForm form);

    /**
     * ERP/MES 外部接口推送创建出库单
     */
    WmsOutboundOrderResultVo createFromErp(ErpOutboundOrderCreateDto dto);

    /**
     * 核心步骤 1：库存预检与分配（FEFO近效期优先、临期禁出拦截、MES工单批次强匹配、缺货剔除/挂起、原子锁库）
     */
    WmsOutboundAllocateResultVo allocateInventory(Long orderId);

    /**
     * 核心步骤 2：实拣确认（实拣件数核销、SN序列号强校验与拣货锁定）
     */
    void confirmPick(WmsPickConfirmForm form);

    /**
     * 核心步骤 3：出库复核（核验SKU、批次、数量、SN，通过后推进到待打包）
     */
    void confirmReview(WmsReviewConfirmForm form);

    /**
     * 核心步骤 4：打包与称重（箱单打包、实测称重比对理论重量防漏发，生成面单，推进到待发货）
     */
    void packageAndWeigh(WmsPackageWeightForm form);

    /**
     * 核心步骤 5：发货确认（扣减物理库存与锁定库存、SN置已出库、流转状态至已发货、回传上游）
     */
    void confirmShip(WmsShipConfirmForm form);

    /**
     * 逆向分支：取消出库单（未拣货释放锁库，已拣货未发货生成取消返架作业单并释放SN/锁库，已发货硬拦截）
     */
    void cancelOutboundOrder(Long orderId, String reason);

    /**
     * 查询出库单包装箱单清单
     */
    List<WmsOutboundOrderPackage> listPackages(Long orderId);

    /**
     * 查询出库单称重记录清单
     */
    List<WmsOutboundOrderWeights> listWeights(Long orderId);

    /**
     * 查询出库单关联SN序列号清单
     */
    List<WmsItemSn> listSns(Long orderId);
}
