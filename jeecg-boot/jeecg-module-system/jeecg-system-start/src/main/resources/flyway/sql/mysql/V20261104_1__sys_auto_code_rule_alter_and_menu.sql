-- 1. 补齐 sys_auto_code_rule 备注与创建时间等标准审计字段
ALTER TABLE `sys_auto_code_rule`
  ADD COLUMN `remark` varchar(500) NULL COMMENT '备注',
  ADD COLUMN `create_by` varchar(32) NULL COMMENT '创建人',
  ADD COLUMN `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  ADD COLUMN `update_by` varchar(32) NULL COMMENT '修改人',
  ADD COLUMN `update_time` datetime NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间';

-- 2. 编码规则主菜单（挂载到系统管理 d7d6e2e4e2934f2c9385a623fd98c6f3）
INSERT INTO `sys_permission`(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
VALUES ('179372000000001', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '编码规则', '/system/autocode', 'system/autocode/index', NULL, NULL, 1, NULL, '1', 60.00, 0, 'ant-design:barcode-outlined', 1, 1, 0, 0, 0, '系统编码规则管理', '1', 0, 0, 'admin', NOW(), NULL, NULL, 0);

-- 3. 按钮权限
INSERT INTO `sys_permission`(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('179372000000002', '179372000000001', '添加编码规则', NULL, NULL, 0, NULL, NULL, 2, 'system:autocode:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);

INSERT INTO `sys_permission`(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('179372000000003', '179372000000001', '编辑编码规则', NULL, NULL, 0, NULL, NULL, 2, 'system:autocode:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);

INSERT INTO `sys_permission`(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('179372000000004', '179372000000001', '删除编码规则', NULL, NULL, 0, NULL, NULL, 2, 'system:autocode:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);

INSERT INTO `sys_permission`(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('179372000000005', '179372000000001', '批量删除编码规则', NULL, NULL, 0, NULL, NULL, 2, 'system:autocode:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);

INSERT INTO `sys_permission`(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('179372000000006', '179372000000001', '导出编码规则', NULL, NULL, 0, NULL, NULL, 2, 'system:autocode:exportXls', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);

INSERT INTO `sys_permission`(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('179372000000007', '179372000000001', '导入编码规则', NULL, NULL, 0, NULL, NULL, 2, 'system:autocode:importExcel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);

-- 4. 授权管理员角色 (admin role_id: f6817f48af4fb3af11b9e8bf182f618b)
INSERT INTO `sys_role_permission` (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179372000000011', 'f6817f48af4fb3af11b9e8bf182f618b', '179372000000001', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179372000000012', 'f6817f48af4fb3af11b9e8bf182f618b', '179372000000002', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179372000000013', 'f6817f48af4fb3af11b9e8bf182f618b', '179372000000003', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179372000000014', 'f6817f48af4fb3af11b9e8bf182f618b', '179372000000004', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179372000000015', 'f6817f48af4fb3af11b9e8bf182f618b', '179372000000005', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179372000000016', 'f6817f48af4fb3af11b9e8bf182f618b', '179372000000006', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179372000000017', 'f6817f48af4fb3af11b9e8bf182f618b', '179372000000007', NULL, NOW(), '127.0.0.1');
