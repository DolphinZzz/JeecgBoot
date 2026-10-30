package org.jeecg.modules.wms.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Description: 上架弹窗里「人工指定库位」的下拉选项
 *
 * 只列本仓库的库位，按库区分组展示，作业员能一眼看出库位属于哪个库区、
 * 还剩多少容积承重。这里的选项不做分区/温区硬过滤 —— 强制指定是留给人来
 * 判断的（见 confirmPutaway 的 force 分支），硬过滤的结论在提交时才校验。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
public class WmsPutawayLocaOption {

    /**库位ID，下拉的 value*/
    private Integer locaId;

    /**库位编码，下拉展示的文本*/
    private String locaCode;

    /**分组名（库区名称），下拉按它分组*/
    private String group;

    /**库区ID*/
    private Integer areaId;

    /**库区类型(STORAGE/PICKING/RECEIVING/QC/RETURN/SHIPPING)*/
    private String areaCategory;

    /**库区分区(GENERAL/SMALL/LARGE/VALUABLE)*/
    private String zoneType;

    /**生效存储条件（库位优先，其次库区）*/
    private String storageCondition;

    /**库位状态(1空闲 2占用 3盘点锁定 4禁用)*/
    private Integer locaStatus;

    /**剩余容积(cm³)，未配置容积时为 null 表示不限*/
    private Integer remainVolume;

    /**剩余承重(g)，未配置承重时为 null 表示不限*/
    private Integer remainWeight;

    /**固定库位绑定的SKU，为空表示非固定库位*/
    private Long fixedSkuId;

    /**是否通过硬过滤：false 时下拉仍可选（强制上架要填原因），但会标红提示*/
    private boolean allowed;

    /**未通过硬过滤的具体原因，allowed=true 时为空*/
    private String rejectReason;
}
