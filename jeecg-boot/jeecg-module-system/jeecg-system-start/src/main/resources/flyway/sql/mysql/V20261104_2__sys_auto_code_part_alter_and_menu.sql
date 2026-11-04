-- 1. 补齐 sys_auto_code_part 备注与审计字段
ALTER TABLE `sys_auto_code_part`
  ADD COLUMN `remark` varchar(500) NULL COMMENT '备注',
  ADD COLUMN `create_by` varchar(32) NULL COMMENT '创建人',
  ADD COLUMN `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  ADD COLUMN `update_by` varchar(32) NULL COMMENT '修改人',
  ADD COLUMN `update_time` datetime NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间';

-- 2. 规则组成页面动态路由（作为隐藏路由挂载在编码规则菜单下）
INSERT INTO `sys_permission`(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
VALUES ('179372000000021', '179372000000001', '规则组成', '/autocodePart/index/:id', 'system/autocode/part/index', NULL, NULL, 1, NULL, '1', 1.00, 0, NULL, 1, 1, 0, 1, 0, '系统编码规则组成管理', '1', 0, 0, 'admin', NOW(), NULL, NULL, 0);

-- 3. 按钮权限
INSERT INTO `sys_permission`(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('179372000000022', '179372000000021', '添加规则组成', NULL, NULL, 0, NULL, NULL, 2, 'system:autocodePart:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);

INSERT INTO `sys_permission`(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('179372000000023', '179372000000021', '编辑规则组成', NULL, NULL, 0, NULL, NULL, 2, 'system:autocodePart:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);

INSERT INTO `sys_permission`(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('179372000000024', '179372000000021', '删除规则组成', NULL, NULL, 0, NULL, NULL, 2, 'system:autocodePart:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);

-- 4. 授权管理员角色 (admin role_id: f6817f48af4fb3af11b9e8bf182f618b)
INSERT INTO `sys_role_permission` (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179372000000031', 'f6817f48af4fb3af11b9e8bf182f618b', '179372000000021', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179372000000032', 'f6817f48af4fb3af11b9e8bf182f618b', '179372000000022', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179372000000033', 'f6817f48af4fb3af11b9e8bf182f618b', '179372000000023', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179372000000034', 'f6817f48af4fb3af11b9e8bf182f618b', '179372000000024', NULL, NOW(), '127.0.0.1');
