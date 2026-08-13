-- =============================================================================
-- VQMS 业务菜单初始化（阶段 2）
-- menu_id 从 2000 起（sys_menu 内置到 ~1061，auto_increment 起点 2000，不冲突）
-- 列顺序：(menu_id, menu_name, parent_id, order_num, path, component, query,
--         route_name, is_frame, is_cache, menu_type, visible, status, perms, icon,
--         create_by, create_time, update_by, update_time, remark)
-- component 约定：值 = src/views/ 下相对路径（无 views/ 前缀、无 .vue 后缀）
--   如 'vqms/daily/index' -> src/views/vqms/daily/index.vue
-- 执行后需重新登录（菜单在登录时由 getRouters 接口拉取）
-- =============================================================================

-- ---------- 一级目录（M，parent_id=0）----------
-- 2000 合格率统计
insert into sys_menu values('2000', '合格率统计', '0', '5', 'stats', null, '', '', 1, 0, 'M', '0', '0', '', 'dashboard', 'admin', sysdate(), '', null, 'VQMS 合格率统计目录');
-- 2010 曲线查询
insert into sys_menu values('2010', '曲线查询', '0', '6', 'curve', null, '', '', 1, 0, 'M', '0', '0', '', 'chart', 'admin', sysdate(), '', null, 'VQMS 曲线查询目录');
-- 2020 AVC 考核
insert into sys_menu values('2020', 'AVC 考核', '0', '7', 'avc', null, '', '', 1, 0, 'M', '0', '0', '', 'monitor', 'admin', sysdate(), '', null, 'VQMS AVC 考核目录');
-- 2030 系统配置
insert into sys_menu values('2030', '系统配置', '0', '8', 'config-vqms', null, '', '', 1, 0, 'M', '0', '0', '', 'system', 'admin', sysdate(), '', null, 'VQMS 系统配置目录');

-- ---------- 合格率统计 -> 菜单（C）----------
insert into sys_menu values('2001', '日报', '2000', '1', 'daily', 'vqms/daily/index', '', 'VqmsDaily', 1, 0, 'C', '0', '0', 'vqms:daily:list', 'date', 'admin', sysdate(), '', null, '电压合格率日报');
insert into sys_menu values('2002', '月报', '2000', '2', 'monthly', 'vqms/monthly/index', '', 'VqmsMonthly', 1, 0, 'C', '0', '0', 'vqms:monthly:list', 'date', 'admin', sysdate(), '', null, '电压合格率月报');
insert into sys_menu values('2003', '年报', '2000', '3', 'yearly', 'vqms/yearly/index', '', 'VqmsYearly', 1, 0, 'C', '0', '0', 'vqms:yearly:list', 'date', 'admin', sysdate(), '', null, '电压合格率年报');
-- 按钮：导出
insert into sys_menu values('2004', '日报导出', '2001', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:daily:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2005', '月报导出', '2002', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:monthly:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2006', '年报导出', '2003', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:yearly:export', '#', 'admin', sysdate(), '', null, '');

-- ---------- 曲线查询 -> 菜单（C）----------
insert into sys_menu values('2011', '电压曲线', '2010', '1', 'voltage', 'vqms/curve/index', '', 'VqmsCurve', 1, 0, 'C', '0', '0', 'vqms:curve:list', 'chart', 'admin', sysdate(), '', null, '母线电压曲线查询');

-- ---------- AVC 考核 -> 菜单（C）----------
insert into sys_menu values('2021', '投运率', '2020', '1', 'runtime', 'vqms/avc-runtime/index', '', 'VqmsAvcRuntime', 1, 0, 'C', '0', '0', 'vqms:avc-runtime:list', 'chart', 'admin', sysdate(), '', null, 'AVC 装置投运率');
insert into sys_menu values('2022', '调节合格率', '2020', '2', 'regulation', 'vqms/avc-regulation/index', '', 'VqmsAvcRegulation', 1, 0, 'C', '0', '0', 'vqms:avc-regulation:list', 'chart', 'admin', sysdate(), '', null, 'AVC 调节合格率（快速性/经济性两档平行）');
-- 按钮：导出
insert into sys_menu values('2023', '投运率导出', '2021', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:avc-runtime:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2024', '调节合格率导出', '2022', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:avc-regulation:export', '#', 'admin', sysdate(), '', null, '');

-- ---------- 系统配置 -> 阈值管理 菜单（C）+ 按钮（F）----------
insert into sys_menu values('2031', '阈值管理', '2030', '1', 'threshold', 'vqms/threshold/index', '', 'VqmsThreshold', 1, 0, 'C', '0', '0', 'vqms:threshold:list', 'edit', 'admin', sysdate(), '', null, '母线电压阈值管理');
insert into sys_menu values('2032', '阈值查询', '2031', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:threshold:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2033', '阈值新增', '2031', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:threshold:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2034', '阈值修改', '2031', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:threshold:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2035', '阈值删除', '2031', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:threshold:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2036', '阈值导出', '2031', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:threshold:export', '#', 'admin', sysdate(), '', null, '');

-- ---------- 角色授权 ----------
-- admin（role_id=1）默认拥有全部菜单（all 权限），无需配 sys_role_menu。
-- 给普通角色（role_id=2，若依默认「普通角色」）授权 VQMS 菜单，使其可见。
-- 执行前请确认 role_id=2 存在；若现场角色不同，按需调整。
insert into sys_role_menu values('2', '2000');
insert into sys_role_menu values('2', '2001');
insert into sys_role_menu values('2', '2002');
insert into sys_role_menu values('2', '2003');
insert into sys_role_menu values('2', '2004');
insert into sys_role_menu values('2', '2005');
insert into sys_role_menu values('2', '2006');
insert into sys_role_menu values('2', '2010');
insert into sys_role_menu values('2', '2011');
insert into sys_role_menu values('2', '2020');
insert into sys_role_menu values('2', '2021');
insert into sys_role_menu values('2', '2022');
insert into sys_role_menu values('2', '2023');
insert into sys_role_menu values('2', '2024');
insert into sys_role_menu values('2', '2030');
insert into sys_role_menu values('2', '2031');
insert into sys_role_menu values('2', '2032');
insert into sys_role_menu values('2', '2033');
insert into sys_role_menu values('2', '2034');
insert into sys_role_menu values('2', '2035');
insert into sys_role_menu values('2', '2036');
