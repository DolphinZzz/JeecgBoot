-- WMS 货主信息：建表、存量数据迁移、字典、菜单权限、表字典白名单。
--
-- 背景：入库单的「货主」原先直接读 wms_customer（客户表）。客户是销售侧的往来单位，
-- 货主是库存归属的主体，两者后续会分叉（一个客户可能对应多个货主，自有仓还有不对外销售的货主），
-- 因此拆出独立的 wms_consignor，入库 / 库存 / 流水 / 库内作业 / 盘点的 consignor_id 统一指向它。
-- 现有 36 条客户数据按原 id 迁移进货主表：id 保持不变，历史单据里的 consignor_id 不需要改写。
--
-- 菜单挂在「基础数据 /wms/base」下，与供应商、客户并列。
-- 全部语句可重复执行：建表 IF NOT EXISTS，插入一律 WHERE NOT EXISTS。
--
-- ID 规则（需与既有 ID 段错开）：
--   字典：17933032000000000000000000000000
--   字典项：17934032 + 两位项序号
--   菜单：179350 + 9 位流水；角色授权：179450 + 同一条流水

-- ========== 1. 建表 ==========
CREATE TABLE IF NOT EXISTS `wms_consignor` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `consignor_code` varchar(32) NOT NULL COMMENT '货主编码',
  `consignor_name` varchar(100) NOT NULL COMMENT '货主名称',
  `consignor_type` int NOT NULL DEFAULT '1' COMMENT '货主类型(1自有货主 2第三方货主)',
  `link_name` varchar(50) DEFAULT NULL COMMENT '联系人',
  `link_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `province_id` int DEFAULT NULL COMMENT '所在省份',
  `city_code` int DEFAULT NULL COMMENT '所在城市',
  `address` varchar(255) DEFAULT NULL COMMENT '详细地址',
  `enable_status` int NOT NULL DEFAULT '1' COMMENT '启用状态(1启用 0停用)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `saas_id` varchar(32) DEFAULT NULL COMMENT 'SaaS标识',
  `create_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `is_del` tinyint NOT NULL DEFAULT '1' COMMENT '是否删除 1未删除 2已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consignor_code` (`consignor_code`),
  KEY `idx_consignor_name` (`consignor_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货主信息';

-- ========== 2. 存量迁移：客户表的数据按原 id 复制为初始货主 ==========
-- 只迁一次：目标表已有同 id 记录就跳过。编码按 id 补足 6 位，保证可读且唯一。
INSERT INTO `wms_consignor`
  (`id`, `consignor_code`, `consignor_name`, `consignor_type`, `link_name`, `link_phone`,
   `province_id`, `city_code`, `address`, `enable_status`, `create_by`, `create_time`, `is_del`)
SELECT c.`id`,
       CONCAT('HZ', LPAD(c.`id`, 6, '0')),
       c.`customer_name`,
       1,
       c.`link_name`,
       c.`link_phone`,
       c.`province_id`,
       c.`city_code`,
       c.`address`,
       1,
       'admin',
       NOW(),
       1
FROM `wms_customer` c
WHERE c.`is_del` = 1
  AND NOT EXISTS (SELECT 1 FROM `wms_consignor` t WHERE t.`id` = c.`id`);

-- ========== 3. 字典：货主类型 ==========
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933032000000000000000000000000', '货主类型', 'wms_consignor_type', '货主类型', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_consignor_type');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934032010000000000000000000000', '17933032000000000000000000000000', '自有货主', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934032010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934032020000000000000000000000', '17933032000000000000000000000000', '第三方货主', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934032020000000000000000000000');

-- ========== 4. 菜单权限 ==========
-- 货主信息（挂在「基础数据」下，sort 6 排在客户信息之后；有按钮子级，is_leaf=0）
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179350000000001', '179307527735921', '货主信息', '/wms/base/wmsConsignorList', 'wms/WmsConsignor/WmsConsignorList', 'wms-wmsConsignor', NULL, 1, NULL, '0', 6.00, 0, 'ant-design:idcard-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179350000000001');

-- 新增
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179350000000002', '179350000000001', '添加货主信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_consignor:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179350000000002');

-- 编辑
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179350000000003', '179350000000001', '编辑货主信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_consignor:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179350000000003');

-- 删除
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179350000000004', '179350000000001', '删除货主信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_consignor:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179350000000004');

-- 批量删除
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179350000000005', '179350000000001', '批量删除货主信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_consignor:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179350000000005');

-- 导出excel
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179350000000006', '179350000000001', '导出excel_货主信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_consignor:exportXls', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179350000000006');

-- 导入excel
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179350000000007', '179350000000001', '导入excel_货主信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_consignor:importExcel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179350000000007');

-- ========== 5. 角色授权（管理员角色） ==========
INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date, operate_ip)
SELECT REPLACE(p.id, '17935', '17945'), 'f6817f48af4fb3af11b9e8bf182f618b', p.id, NOW(), '127.0.0.1'
FROM sys_permission p
WHERE p.id IN (
    '179350000000001','179350000000002','179350000000003','179350000000004',
    '179350000000005','179350000000006','179350000000007'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.id = REPLACE(p.id, '17935', '17945')
);

-- ========== 6. 表字典白名单 ==========
-- 入库单 / 移库的货主下拉走表字典 wms_consignor,consignor_name,id,enable_status=1，
-- 白名单要同时放行显示字段、取值字段和过滤条件字段。
INSERT INTO sys_table_white_list(id, table_name, field_name, status, create_by, create_time)
SELECT '179350000000008', 'wms_consignor', 'id,consignor_name,consignor_code,enable_status', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_table_white_list WHERE table_name = 'wms_consignor');
