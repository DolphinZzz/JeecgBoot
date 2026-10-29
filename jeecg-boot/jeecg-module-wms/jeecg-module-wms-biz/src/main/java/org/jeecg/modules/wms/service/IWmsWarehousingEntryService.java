package org.jeecg.modules.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import java.util.Map;
import org.jeecg.modules.wms.entity.WmsWarehousingEntry;
import org.jeecg.modules.wms.entity.WmsWarehousingEntryUp;
import org.jeecg.modules.wms.vo.WmsEntryCreateForm;
import org.jeecg.modules.wms.vo.WmsEntryDetailView;
import org.jeecg.modules.wms.vo.WmsEntryPage;
import org.jeecg.modules.wms.vo.WmsEntryQuery;
import org.jeecg.modules.wms.vo.WmsPutawayConfirmForm;
import org.jeecg.modules.wms.vo.WmsPutawayPlan;
import org.jeecg.modules.wms.vo.WmsQcSubmitForm;
import org.jeecg.modules.wms.vo.WmsReceiveForm;

/**
 * @Description: 入库闭环（建单 → 收货清点 → 质检 → 上架落库）
 *
 * 状态机：10待到货 → 20收货清点中 → 30待质检 → 40质检完成待上架
 *        → 50上架中 → 60已完成；超收未放行 / SN 不符 → 90异常挂起。
 *
 * 硬性规则（方案 3.2.3）：
 * IN-01 严格按 收货 → 质检 → 上架 推进，禁止跳步；
 * IN-02 工业制造仓的生产入库质检必须绑定 MES 工单；
 * IN-03 不合格品只能上架到 RETURN 库区；
 * IN-04 超收按仓型分级：工业仓硬拦截（主管放行后置 2），普通仓挂 90 待审批；
 * IN-05/IN-06 容积承重与存储条件由上架策略引擎的统一硬过滤把关；
 * IN-07 SN 唯一且不重复入库；
 * IN-08 少收自动登记待补料。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
public interface IWmsWarehousingEntryService extends IService<WmsWarehousingEntry> {

    /** 入库单分页查询 */
    IPage<WmsEntryPage> queryEntryPage(WmsEntryQuery query, Integer pageNo, Integer pageSize);

    /** 新建入库单（含明细），状态置 10 待到货 */
    WmsWarehousingEntry createEntry(WmsEntryCreateForm form);

    /** 查明细（带商品编码 / 名称 / 默认质检方式 / 已上架数） */
    List<WmsEntryDetailView> listDetails(Long entryId);

    /**
     * 收货清点：登记实收 / 破损 / SN，算超收与少收，登记待补料，
     * 按仓型判定超收放行，状态 10/20 → 20 或 90。
     */
    Map<String, Object> receive(WmsReceiveForm form);

    /**
     * 收货完成 → 生成质检单，状态 20 → 30。
     * 免检明细直接生成「免检放行」质检记录，其余生成待质检记录。
     */
    Map<String, Object> submitToQc(Long entryId);

    /**
     * 提交质检结果：落质检结论，明细回写合格 / 不合格数，
     * 全部明细出结论后状态 30 → 40。
     */
    Map<String, Object> submitQc(WmsQcSubmitForm form);

    /**
     * 超收放行 / 驳回（主管操作）。放行后状态 90 → 30，驳回保持 90。
     */
    void auditOverReceive(Long entryId, boolean pass, String reason);

    /** 计算上架推荐（不落库），供页面展示 */
    WmsPutawayPlan recommendPutaway(Long entryId);

    /**
     * 确认上架并落库：写库存 + 库存流水 + SN 在库 + 库位占用，
     * 写 wms_warehousing_entry_up，状态 40 → 50 → 60。
     */
    Map<String, Object> confirmPutaway(WmsPutawayConfirmForm form);

    /** 查上架记录 */
    List<WmsWarehousingEntryUp> listPutawayRecords(Long entryId);

    /** 取消入库单（只有未开始收货的单据可取消） */
    void cancel(Long entryId);
}
