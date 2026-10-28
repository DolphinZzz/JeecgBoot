-- WMS 表结构演进 第 9 步（对应方案 4.4 顺序 9）：字典与定时任务初始化。
--
-- 1. 方案 5.1 的 22 个字典。当前库中没有任何 wms_* 字典，全部为新建。
--    全部使用 INSERT ... WHERE NOT EXISTS 固定 ID，重复执行不会重复插入。
-- 2. ABC 月度重算定时任务，注册状态为 -1（停止）。出库流水积累满一个统计窗口后，
--    再到「系统管理 / 定时任务」页面启用，避免上线初期天天跑空。
--
-- 菜单不在本脚本中创建：按方案六的路线图，菜单与各期模块的前端页面同批交付，
-- 与 V20261027_1~6 的既有做法一致。提前建菜单会出现点进去 404 的死链。

-- ----------------------------------------------------------------
-- 1. 字典
-- ----------------------------------------------------------------

-- wms_warehouse_type 仓库类型
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933001000000000000000000000000', '仓库类型', 'wms_warehouse_type', '仓库类型', 0, 'admin', NOW(), 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_warehouse_type');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934001010000000000000000000000', '17933001000000000000000000000000', '普通仓', 'NORMAL', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934001010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934001020000000000000000000000', '17933001000000000000000000000000', '工业制造仓', 'INDUSTRIAL', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934001020000000000000000000000');

-- wms_warehouse_area_category 库区类型
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933002000000000000000000000000', '库区类型', 'wms_warehouse_area_category', '库区类型', 0, 'admin', NOW(), 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_warehouse_area_category');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934002010000000000000000000000', '17933002000000000000000000000000', '收货暂存', 'RECEIVING', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934002010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934002020000000000000000000000', '17933002000000000000000000000000', '待检', 'QC', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934002020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934002030000000000000000000000', '17933002000000000000000000000000', '存储', 'STORAGE', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934002030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934002040000000000000000000000', '17933002000000000000000000000000', '拣货', 'PICKING', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934002040000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934002050000000000000000000000', '17933002000000000000000000000000', '退货不良', 'RETURN', 5, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934002050000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934002060000000000000000000000', '17933002000000000000000000000000', '集货待发', 'SHIPPING', 6, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934002060000000000000000000000');

-- wms_loca_status 库位状态
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933003000000000000000000000000', '库位状态', 'wms_loca_status', '库位状态', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_loca_status');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934003010000000000000000000000', '17933003000000000000000000000000', '空闲', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934003010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934003020000000000000000000000', '17933003000000000000000000000000', '占用', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934003020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934003030000000000000000000000', '17933003000000000000000000000000', '盘点锁定', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934003030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934003040000000000000000000000', '17933003000000000000000000000000', '禁用', '4', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934003040000000000000000000000');

-- wms_abc_class ABC分类
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933004000000000000000000000000', 'ABC分类', 'wms_abc_class', 'ABC分类', 0, 'admin', NOW(), 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_abc_class');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934004010000000000000000000000', '17933004000000000000000000000000', 'A类', 'A', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934004010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934004020000000000000000000000', '17933004000000000000000000000000', 'B类', 'B', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934004020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934004030000000000000000000000', '17933004000000000000000000000000', 'C类', 'C', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934004030000000000000000000000');

-- wms_storage_condition 存储条件
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933005000000000000000000000000', '存储条件', 'wms_storage_condition', '存储条件', 0, 'admin', NOW(), 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_storage_condition');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934005010000000000000000000000', '17933005000000000000000000000000', '常温', 'NORMAL', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934005010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934005020000000000000000000000', '17933005000000000000000000000000', '阴凉', 'COOL', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934005020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934005030000000000000000000000', '17933005000000000000000000000000', '冷藏', 'COLD', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934005030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934005040000000000000000000000', '17933005000000000000000000000000', '冷冻', 'FROZEN', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934005040000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934005050000000000000000000000', '17933005000000000000000000000000', '防静电', 'ESD', 5, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934005050000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934005060000000000000000000000', '17933005000000000000000000000000', '危化', 'HAZARD', 6, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934005060000000000000000000000');

-- wms_qc_type 质检方式
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933006000000000000000000000000', '质检方式', 'wms_qc_type', '质检方式', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_qc_type');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934006010000000000000000000000', '17933006000000000000000000000000', '免检', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934006010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934006020000000000000000000000', '17933006000000000000000000000000', '抽检', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934006020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934006030000000000000000000000', '17933006000000000000000000000000', '全检', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934006030000000000000000000000');

-- wms_qc_status 质检状态
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933007000000000000000000000000', '质检状态', 'wms_qc_status', '质检状态', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_qc_status');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934007010000000000000000000000', '17933007000000000000000000000000', '待质检', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934007010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934007020000000000000000000000', '17933007000000000000000000000000', '质检中', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934007020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934007030000000000000000000000', '17933007000000000000000000000000', '合格', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934007030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934007040000000000000000000000', '17933007000000000000000000000000', '不合格', '4', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934007040000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934007050000000000000000000000', '17933007000000000000000000000000', '部分合格', '5', 5, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934007050000000000000000000000');

-- wms_entry_type 入库类型
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933008000000000000000000000000', '入库类型', 'wms_entry_type', '入库类型', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_entry_type');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934008010000000000000000000000', '17933008000000000000000000000000', '采购入库', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934008010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934008020000000000000000000000', '17933008000000000000000000000000', 'MES生产入库', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934008020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934008030000000000000000000000', '17933008000000000000000000000000', '销退入库', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934008030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934008040000000000000000000000', '17933008000000000000000000000000', '调拨入库', '4', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934008040000000000000000000000');

-- wms_outbound_type 出库类型
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933009000000000000000000000000', '出库类型', 'wms_outbound_type', '出库类型', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_outbound_type');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934009010000000000000000000000', '17933009000000000000000000000000', '销售出库', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934009010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934009020000000000000000000000', '17933009000000000000000000000000', 'MES工单领料', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934009020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934009030000000000000000000000', '17933009000000000000000000000000', '采购退货', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934009030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934009040000000000000000000000', '17933009000000000000000000000000', '调拨出库', '4', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934009040000000000000000000000');

-- wms_pick_mode 拣货模式
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933010000000000000000000000000', '拣货模式', 'wms_pick_mode', '拣货模式', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_pick_mode');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934010010000000000000000000000', '17933010000000000000000000000000', '摘果法', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934010010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934010020000000000000000000000', '17933010000000000000000000000000', '波次播种法', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934010020000000000000000000000');

-- wms_operation_type 库内作业子类
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933011000000000000000000000000', '库内作业子类', 'wms_operation_type', '库内作业子类', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_operation_type');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934011010000000000000000000000', '17933011000000000000000000000000', '移库移位', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934011010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934011020000000000000000000000', '17933011000000000000000000000000', '补货', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934011020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934011030000000000000000000000', '17933011000000000000000000000000', '加工', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934011030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934011040000000000000000000000', '17933011000000000000000000000000', '取消返架', '4', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934011040000000000000000000000');

-- wms_quality_status 库存质量状态
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933012000000000000000000000000', '库存质量状态', 'wms_quality_status', '库存质量状态', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_quality_status');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934012010000000000000000000000', '17933012000000000000000000000000', '良品', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934012010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934012020000000000000000000000', '17933012000000000000000000000000', '待检', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934012020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934012030000000000000000000000', '17933012000000000000000000000000', '不良品', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934012030000000000000000000000');

-- wms_sn_status SN状态
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933013000000000000000000000000', 'SN状态', 'wms_sn_status', 'SN状态', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_sn_status');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934013010000000000000000000000', '17933013000000000000000000000000', '收货待检', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934013010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934013020000000000000000000000', '17933013000000000000000000000000', '在库良品', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934013020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934013030000000000000000000000', '17933013000000000000000000000000', '在库不良', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934013030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934013040000000000000000000000', '17933013000000000000000000000000', '拣货锁定', '4', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934013040000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934013050000000000000000000000', '17933013000000000000000000000000', '已出库', '5', 5, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934013050000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934013060000000000000000000000', '17933013000000000000000000000000', '已退货', '6', 6, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934013060000000000000000000000');

-- wms_inventory_biz_type 库存变动类型
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933014000000000000000000000000', '库存变动类型', 'wms_inventory_biz_type', '库存变动类型', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_inventory_biz_type');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934014010000000000000000000000', '17933014000000000000000000000000', '入库上架', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934014010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934014020000000000000000000000', '17933014000000000000000000000000', '出库扣减', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934014020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934014030000000000000000000000', '17933014000000000000000000000000', '移库移出', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934014030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934014040000000000000000000000', '17933014000000000000000000000000', '移库移入', '4', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934014040000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934014050000000000000000000000', '17933014000000000000000000000000', '补货', '5', 5, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934014050000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934014060000000000000000000000', '17933014000000000000000000000000', '盘点调账', '6', 6, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934014060000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934014070000000000000000000000', '17933014000000000000000000000000', '冻结', '7', 7, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934014070000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934014080000000000000000000000', '17933014000000000000000000000000', '解冻', '8', 8, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934014080000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934014090000000000000000000000', '17933014000000000000000000000000', '锁定', '9', 9, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934014090000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934014100000000000000000000000', '17933014000000000000000000000000', '解锁', '10', 10, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934014100000000000000000000000');

-- wms_zone_type 分区类型
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933015000000000000000000000000', '分区类型', 'wms_zone_type', '分区类型', 0, 'admin', NOW(), 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_zone_type');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934015010000000000000000000000', '17933015000000000000000000000000', '通用', 'GENERAL', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934015010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934015020000000000000000000000', '17933015000000000000000000000000', '小件', 'SMALL', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934015020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934015030000000000000000000000', '17933015000000000000000000000000', '大件', 'LARGE', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934015030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934015040000000000000000000000', '17933015000000000000000000000000', '贵品', 'VALUABLE', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934015040000000000000000000000');

-- wms_putaway_strategy 上架策略
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933016000000000000000000000000', '上架策略', 'wms_putaway_strategy', '上架策略', 0, 'admin', NOW(), 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_putaway_strategy');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934016010000000000000000000000', '17933016000000000000000000000000', '固定库位', 'FIXED', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934016010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934016020000000000000000000000', '17933016000000000000000000000000', 'ABC驱动', 'ABC', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934016020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934016030000000000000000000000', '17933016000000000000000000000000', '就近上架', 'NEAREST', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934016030000000000000000000000');

-- wms_ergonomic_level 人体工学层级
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933017000000000000000000000000', '人体工学层级', 'wms_ergonomic_level', '人体工学层级', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_ergonomic_level');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934017010000000000000000000000', '17933017000000000000000000000000', '黄金层', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934017010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934017020000000000000000000000', '17933017000000000000000000000000', '次优层', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934017020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934017030000000000000000000000', '17933017000000000000000000000000', '不便层', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934017030000000000000000000000');

-- wms_abc_calc_mode ABC计算模式
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933018000000000000000000000000', 'ABC计算模式', 'wms_abc_calc_mode', 'ABC计算模式', 0, 'admin', NOW(), 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_abc_calc_mode');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934018010000000000000000000000', '17933018000000000000000000000000', '按频次', 'FREQ', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934018010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934018020000000000000000000000', '17933018000000000000000000000000', '按金额', 'AMOUNT', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934018020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934018030000000000000000000000', '17933018000000000000000000000000', '综合', 'COMPOSITE', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934018030000000000000000000000');

-- wms_abc_batch_status ABC批次状态
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933019000000000000000000000000', 'ABC批次状态', 'wms_abc_batch_status', 'ABC批次状态', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_abc_batch_status');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934019010000000000000000000000', '17933019000000000000000000000000', '计算中', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934019010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934019020000000000000000000000', '17933019000000000000000000000000', '待确认', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934019020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934019030000000000000000000000', '17933019000000000000000000000000', '已生效', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934019030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934019040000000000000000000000', '17933019000000000000000000000000', '已作废', '4', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934019040000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934019050000000000000000000000', '17933019000000000000000000000000', '已跳过', '5', 5, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934019050000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934019060000000000000000000000', '17933019000000000000000000000000', '计算失败', '6', 6, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934019060000000000000000000000');

-- wms_abc_trigger_type ABC触发方式
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933020000000000000000000000000', 'ABC触发方式', 'wms_abc_trigger_type', 'ABC触发方式', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_abc_trigger_type');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934020010000000000000000000000', '17933020000000000000000000000000', '定时', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934020010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934020020000000000000000000000', '17933020000000000000000000000000', '手工', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934020020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934020030000000000000000000000', '17933020000000000000000000000000', '导入', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934020030000000000000000000000');

-- wms_abc_adjust_reason ABC调整原因
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933021000000000000000000000000', 'ABC调整原因', 'wms_abc_adjust_reason', 'ABC调整原因', 0, 'admin', NOW(), 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_abc_adjust_reason');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934021010000000000000000000000', '17933021000000000000000000000000', '人工锁定', 'MANUAL_LOCK', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934021010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934021020000000000000000000000', '17933021000000000000000000000000', '新品保护', 'NEW_ITEM', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934021020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934021030000000000000000000000', '17933021000000000000000000000000', 'A类封顶', 'A_CAP', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934021030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934021040000000000000000000000', '17933021000000000000000000000000', '降级缓冲', 'DOWN_BUFFER', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934021040000000000000000000000');

-- wms_operation_source 库内作业来源
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933022000000000000000000000000', '库内作业来源', 'wms_operation_source', '库内作业来源', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_operation_source');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934022010000000000000000000000', '17933022000000000000000000000000', '人工创建', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934022010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934022020000000000000000000000', '17933022000000000000000000000000', 'ABC优化建议', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934022020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934022030000000000000000000000', '17933022000000000000000000000000', '系统补货', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934022030000000000000000000000');

-- ----------------------------------------------------------------
-- 2. ABC 月度重算定时任务（状态 -1 停止）
-- ----------------------------------------------------------------
INSERT INTO sys_quartz_job (id, create_by, create_time, del_flag, job_class_name, cron_expression, parameter, description, status)
SELECT '179320000000001', 'admin', NOW(), 0,
       'org.jeecg.modules.wms.job.WmsAbcCalcJob', '0 0 2 1 * ? *', NULL,
       'WMS 商品ABC分类月度重算（参数为仓库ID，为空则全部仓库）', -1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_quartz_job WHERE job_class_name = 'org.jeecg.modules.wms.job.WmsAbcCalcJob');
