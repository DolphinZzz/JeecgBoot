package org.jeecg.modules.wms.mapper;

import org.jeecg.modules.wms.entity.WmsConsignor;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description: 货主信息
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
public interface WmsConsignorMapper extends BaseMapper<WmsConsignor> {

    /**
     * 取当前最大货主编码，用于生成下一个编码。
     * 编码统一是 HZ + 6 位数字，按字符串排序即可拿到最大序号；表为空返回 null。
     */
    String selectMaxConsignorCode(@Param("prefix") String prefix);

    /**
     * 统计该货主被业务数据引用的次数：入库单、库存、库存流水、库内作业、盘点。
     * 大于 0 说明货主已经进入业务，不能再删。
     */
    long countReferences(@Param("consignorId") Long consignorId);

    /**
     * 货主编码占用数。已逻辑删除的记录也算占用，
     * 与表上 consignor_code 的唯一索引口径保持一致。
     */
    long countByCode(@Param("consignorCode") String consignorCode, @Param("excludeId") Integer excludeId);
}
