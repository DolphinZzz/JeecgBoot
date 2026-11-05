-- ====================================================================
-- 系统管理-编码规则微服务体系：网关路由追加 /system/** 路径谓词断言
-- 保证来自前端或外部微服务的 /system/autocode/get 等接口请求能正确转发至 jeecg-system
-- ====================================================================

UPDATE `sys_gateway_route`
SET `predicates` = '[{"args":["/sys/**","/system/**","/online/**","/bigscreen/**","/jmreport/**","/druid/**","/generic/**","/actuator/**","/drag/**","/oauth2/**","/defa/**","/demo/**","/jimubi/**","/airag/**","/openapi/**"],"name":"Path"}]'
WHERE `router_id` = 'jeecg-system'
  AND `predicates` NOT LIKE '%/system/**%';
