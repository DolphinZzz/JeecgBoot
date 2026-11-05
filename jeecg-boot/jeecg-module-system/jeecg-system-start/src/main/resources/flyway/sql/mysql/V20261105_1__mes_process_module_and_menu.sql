-- ================================================================
-- MES 工艺管理模块：表结构完善 + 菜单权限配置（工艺管理 -> 工序设置 / 工艺流程）
-- ================================================================

-- 1. 确保三张工艺路线相关表存在（若本地已创建则跳过）并补齐 mes_process_route.remark 字段
CREATE TABLE IF NOT EXISTS `mes_process_route` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `route_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工艺路线编码，唯一标识',
  `route_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工艺路线名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '工艺路线说明',
  `version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'v1.0' COMMENT '工艺路线版本号',
  `is_enabled` int NOT NULL DEFAULT '1' COMMENT '是否启用：1=是，2=否',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人账号',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `update_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '修改人账号',
  `is_del` int DEFAULT '1' COMMENT '是否删除：1=未删除，2=已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工艺路线表';

CREATE TABLE IF NOT EXISTS `mes_process_route_composition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `route_id` bigint NOT NULL COMMENT '所属工艺路线ID（外键，关联 tb_process_route.id）',
  `process_step_id` bigint NOT NULL COMMENT '工序ID（外键，关联 tb_process_step.id）',
  `sequence_no` int NOT NULL DEFAULT '1' COMMENT '在工艺路线中的序号（1,2,3...）',
  `relation_with_next` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '与下一道工序的关系：S-to-S / F-to-F / S-to-F / F-to-S',
  `gantt_color` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '#00AEF3' COMMENT '甘特图显示颜色，如 #00AEF3',
  `is_critical_process` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为关键工序：1=是，0=否',
  `need_qc_confirmation` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要质检确认：1=是，0=否',
  `setup_time` int unsigned DEFAULT '0' COMMENT '准备时间（小时）',
  `wait_time` int unsigned DEFAULT '0' COMMENT '等待时间（小时）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人账号',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `update_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '修改人账号',
  `is_del` int DEFAULT '1' COMMENT '是否删除：1=未删除，2=已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='MES-工艺工序组成表';

CREATE TABLE IF NOT EXISTS `mes_process_product_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品物料编码，如 IF20250312005',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '产品物料名称，如 螺丝刀刀柄【黑色】',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '单位，如 个、套、千克等',
  `specification` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '规格型号，如 “长15cm, 黑色”',
  `route_id` bigint NOT NULL COMMENT '所属工艺路线ID（外键，关联 tb_process_route.id）',
  `route_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '工艺路线编码，便于展示',
  `route_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '工艺路线名称，便于展示',
  `version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'v1.0' COMMENT '该映射的版本号',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为默认工艺：1=是，0=否',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注，如特殊要求',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人账号',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `update_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '修改人账号',
  `is_del` int DEFAULT '1' COMMENT '是否删除：1=未删除，2=已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工艺关联产品表';

-- 为已存在的 mes_process_route 补充 remark 列（幂等处理）
SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'mes_process_route'
    AND COLUMN_NAME = 'remark'
);
SET @sql_stmt := IF(
  @col_exists = 0,
  'ALTER TABLE `mes_process_route` ADD COLUMN `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT ''备注'' AFTER `is_enabled`',
  'SELECT 1'
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 在 mes 一级菜单 (2117605103480979458) 下创建二级菜单目录「工艺管理」（menu_type = 1，与「基础数据」同级）
INSERT INTO `sys_permission`(
  id, parent_id, name, url, component, component_name, redirect, menu_type,
  perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive,
  hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time
) VALUES (
  '2117700000000000001', '2117605103480979458', '工艺管理', '/mes/process', 'layouts/default/index', 'mes-process', NULL, 1,
  NULL, '0', 2.00, 0, 'ant-design:deployment-unit-outlined', 1, 0, 0,
  0, 0, 'MES工艺管理模块', '1', 0, 0, 'admin', NOW()
) ON DUPLICATE KEY UPDATE
  parent_id = '2117605103480979458',
  name = '工艺管理',
  url = '/mes/process',
  component = 'layouts/default/index',
  component_name = 'mes-process',
  redirect = NULL,
  menu_type = 1,
  sort_no = 2.00,
  icon = 'ant-design:deployment-unit-outlined',
  is_route = 1,
  is_leaf = 0;

-- 3. 三级菜单：「工序设置」
INSERT INTO `sys_permission`(
  id, parent_id, name, url, component, component_name, redirect, menu_type,
  perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive,
  hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time
) VALUES (
  '2117700000000000010', '2117700000000000001', '工序设置', '/mes/process/step', 'mes/process/step/MesProcessStepList', 'mes-process-step', NULL, 1,
  NULL, '0', 1.00, 0, 'ant-design:setting-outlined', 1, 0, 0,
  0, 0, 'MES工序设置', '1', 0, 0, 'admin', NOW()
) ON DUPLICATE KEY UPDATE
  parent_id = '2117700000000000001',
  menu_type = 1,
  is_leaf = 0;

-- 工序设置按钮权限
INSERT IGNORE INTO `sys_permission`(id, parent_id, name, menu_type, perms, perms_type, sort_no, is_route, is_leaf, status, del_flag, create_by, create_time)
VALUES
  ('2117700000000000011', '2117700000000000010', '添加工序', 2, 'mes:mes_process_step:add', '1', 1.00, 0, 1, '1', 0, 'admin', NOW()),
  ('2117700000000000012', '2117700000000000010', '编辑工序', 2, 'mes:mes_process_step:edit', '1', 2.00, 0, 1, '1', 0, 'admin', NOW()),
  ('2117700000000000013', '2117700000000000010', '删除工序', 2, 'mes:mes_process_step:delete', '1', 3.00, 0, 1, '1', 0, 'admin', NOW()),
  ('2117700000000000014', '2117700000000000010', '批量删除工序', 2, 'mes:mes_process_step:deleteBatch', '1', 4.00, 0, 1, '1', 0, 'admin', NOW()),
  ('2117700000000000015', '2117700000000000010', '导出工序', 2, 'mes:mes_process_step:exportXls', '1', 5.00, 0, 1, '1', 0, 'admin', NOW()),
  ('2117700000000000016', '2117700000000000010', '导入工序', 2, 'mes:mes_process_step:importExcel', '1', 6.00, 0, 1, '1', 0, 'admin', NOW());

-- 4. 三级菜单：「工艺流程」
INSERT INTO `sys_permission`(
  id, parent_id, name, url, component, component_name, redirect, menu_type,
  perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive,
  hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time
) VALUES (
  '2117700000000000020', '2117700000000000001', '工艺流程', '/mes/process/route', 'mes/process/route/MesProcessRouteList', 'mes-process-route', NULL, 1,
  NULL, '0', 2.00, 0, 'ant-design:node-index-outlined', 1, 0, 0,
  0, 0, 'MES工艺流程', '1', 0, 0, 'admin', NOW()
) ON DUPLICATE KEY UPDATE
  parent_id = '2117700000000000001',
  menu_type = 1,
  is_leaf = 0;

-- 工艺流程按钮权限
INSERT IGNORE INTO `sys_permission`(id, parent_id, name, menu_type, perms, perms_type, sort_no, is_route, is_leaf, status, del_flag, create_by, create_time)
VALUES
  ('2117700000000000021', '2117700000000000020', '添加工艺路线', 2, 'mes:mes_process_route:add', '1', 1.00, 0, 1, '1', 0, 'admin', NOW()),
  ('2117700000000000022', '2117700000000000020', '编辑工艺路线', 2, 'mes:mes_process_route:edit', '1', 2.00, 0, 1, '1', 0, 'admin', NOW()),
  ('2117700000000000023', '2117700000000000020', '删除工艺路线', 2, 'mes:mes_process_route:delete', '1', 3.00, 0, 1, '1', 0, 'admin', NOW()),
  ('2117700000000000024', '2117700000000000020', '批量删除工艺路线', 2, 'mes:mes_process_route:deleteBatch', '1', 4.00, 0, 1, '1', 0, 'admin', NOW()),
  ('2117700000000000025', '2117700000000000020', '导出工艺路线', 2, 'mes:mes_process_route:exportXls', '1', 5.00, 0, 1, '1', 0, 'admin', NOW()),
  ('2117700000000000026', '2117700000000000020', '导入工艺路线', 2, 'mes:mes_process_route:importExcel', '1', 6.00, 0, 1, '1', 0, 'admin', NOW());

-- 5. 授权给 admin 角色 (role_id: f6817f48af4fb3af11b9e8bf182f618b)
INSERT IGNORE INTO `sys_role_permission` (id, role_id, permission_id, operate_date, operate_ip) VALUES
  ('2117700000000000101', 'f6817f48af4fb3af11b9e8bf182f618b', '2117700000000000001', NOW(), '127.0.0.1'),
  ('2117700000000000110', 'f6817f48af4fb3af11b9e8bf182f618b', '2117700000000000010', NOW(), '127.0.0.1'),
  ('2117700000000000111', 'f6817f48af4fb3af11b9e8bf182f618b', '2117700000000000011', NOW(), '127.0.0.1'),
  ('2117700000000000112', 'f6817f48af4fb3af11b9e8bf182f618b', '2117700000000000012', NOW(), '127.0.0.1'),
  ('2117700000000000113', 'f6817f48af4fb3af11b9e8bf182f618b', '2117700000000000013', NOW(), '127.0.0.1'),
  ('2117700000000000114', 'f6817f48af4fb3af11b9e8bf182f618b', '2117700000000000014', NOW(), '127.0.0.1'),
  ('2117700000000000115', 'f6817f48af4fb3af11b9e8bf182f618b', '2117700000000000015', NOW(), '127.0.0.1'),
  ('2117700000000000116', 'f6817f48af4fb3af11b9e8bf182f618b', '2117700000000000016', NOW(), '127.0.0.1'),
  ('2117700000000000120', 'f6817f48af4fb3af11b9e8bf182f618b', '2117700000000000020', NOW(), '127.0.0.1'),
  ('2117700000000000121', 'f6817f48af4fb3af11b9e8bf182f618b', '2117700000000000021', NOW(), '127.0.0.1'),
  ('2117700000000000122', 'f6817f48af4fb3af11b9e8bf182f618b', '2117700000000000022', NOW(), '127.0.0.1'),
  ('2117700000000000123', 'f6817f48af4fb3af11b9e8bf182f618b', '2117700000000000023', NOW(), '127.0.0.1'),
  ('2117700000000000124', 'f6817f48af4fb3af11b9e8bf182f618b', '2117700000000000024', NOW(), '127.0.0.1'),
  ('2117700000000000125', 'f6817f48af4fb3af11b9e8bf182f618b', '2117700000000000025', NOW(), '127.0.0.1'),
  ('2117700000000000126', 'f6817f48af4fb3af11b9e8bf182f618b', '2117700000000000026', NOW(), '127.0.0.1');
