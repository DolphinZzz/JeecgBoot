package org.jeecg.modules.wms.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Description: 上架弹窗里「选择上架库位」的选项（库位选择器的数据行）
 *
 * 只列本仓库的库位，带上库区、坐标、尺寸容积与剩余可放件数，
 * 作业员不必记住库位编码也能按仓库/库区/巷道筛出来。
 *
 * 这里的选项不做分区/温区硬过滤 —— 强制指定是留给人来判断的（见 confirmPutaway 的
 * force 分支），硬过滤的结论挂在 allowed / rejectReason 上，提交时才校验。
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

    /**库位编码*/
    private String locaCode;

    /**分组名（库区名称），下拉按它分组*/
    private String group;

    /**仓库ID*/
    private Integer warehouseId;

    /**仓库名称*/
    private String warehouseName;

    /**库区ID*/
    private Integer areaId;

    /**库区名称*/
    private String areaName;

    /**库区类型(STORAGE/PICKING/RECEIVING/QC/RETURN/SHIPPING)，用于筛选*/
    private String areaCategory;

    /**库区类型中文（收货暂存/待检/存储/拣货/退货不良/集货待发）*/
    private String areaCategoryText;

    /**库区分区(GENERAL/SMALL/LARGE/VALUABLE)*/
    private String zoneType;

    /**库区分区中文*/
    private String zoneTypeText;

    /**生效存储条件（库位优先，其次库区）*/
    private String storageCondition;

    /**库位状态(1空闲 2占用 3盘点锁定 4禁用)*/
    private Integer locaStatus;

    /**库位状态中文*/
    private String locaStatusText;

    /**长度(mm)*/
    private Integer locaLen;

    /**宽度(mm)*/
    private Integer locaWidth;

    /**高度(mm)*/
    private Integer locaHeight;

    /**容积(cm³)，0 表示未配置*/
    private Integer localVolume;

    /**承重(g)，0 表示未配置*/
    private Integer localBearingCapacity;

    /**巷道号*/
    private String tunnelNumber;

    /**货架号*/
    private String shelfNumber;

    /**层号*/
    private String layerNumber;

    /**位号*/
    private String positionNumber;

    /**剩余容积(cm³)，未配置容积时为 null 表示不限*/
    private Integer remainVolume;

    /**剩余承重(g)，未配置承重时为 null 表示不限*/
    private Integer remainWeight;

    /**
     * 剩余可上架件数：min(⌊剩余容积/单件体积⌋, ⌊剩余承重/单件重量⌋)。
     * 未指定商品或商品未维护体积重量时为 null 表示无法估算。
     */
    private Integer remainCount;

    /**固定库位绑定的SKU，为空表示非固定库位*/
    private Long fixedSkuId;

    /**是否通过硬过滤：false 时仍可选（强制上架要填原因），但页面上标红提示*/
    private boolean allowed;

    /**未通过硬过滤的具体原因，allowed=true 时为空*/
    private String rejectReason;
}
