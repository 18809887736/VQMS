-- =============================================================================
-- VQMS 业务菜单初始化（阶段 2；2026-08-21 D5 修订：avc-runtime/avc-regulation perms 改冒号段，对齐 v5.0 §10.1）
-- menu_id 从 2000 起（sys_menu 内置到 ~1061，auto_increment 起点 2000，不冲突）
-- 列顺序：(menu_id, menu_name, parent_id, order_num, path, component, query,
--         route_name, is_frame, is_cache, menu_type, visible, status, perms, icon,
--         create_by, create_time, update_by, update_time, remark)
-- component 约定：值 = src/views/ 下相对路径（无 views/ 前缀、无 .vue 后缀）
--   如 'vqms/daily/index' -> src/views/vqms/daily/index.vue
-- 执行后需重新登录（菜单在登录时由 getRouters 接口拉取）
-- =============================================================================

-- ---------- VQMS 根目录（2026-08-25 菜单规划：业务四目录收拢至「电压质量」根下，与 RuoYi 平台菜单分层）----------
-- 1500 电压质量（根，M，parent_id=0）
insert into sys_menu values('1500', '电压质量', '0', '1', 'vqms', null, '', '', 1, 0, 'M', '0', '0', '', 'dashboard', 'admin', sysdate(), '', null, 'VQMS 业务根目录');
-- ---------- 二级目录（M，parent_id=1500）----------
-- 2010 曲线查询
insert into sys_menu values('2010', '曲线查询', '1500', '1', 'curve', null, '', '', 1, 0, 'M', '0', '0', '', 'chart', 'admin', sysdate(), '', null, 'VQMS 曲线查询目录');
-- 2000 电压合格率（原「合格率统计」，2026-08-25 改名消歧——AVC 下另有调节合格率）
insert into sys_menu values('2000', '电压合格率', '1500', '2', 'stats', null, '', '', 1, 0, 'M', '0', '0', '', 'dashboard', 'admin', sysdate(), '', null, 'VQMS 电压合格率统计目录');
-- 2020 AVC考核
insert into sys_menu values('2020', 'AVC考核', '1500', '3', 'avc', null, '', '', 1, 0, 'M', '0', '0', '', 'monitor', 'admin', sysdate(), '', null, 'VQMS AVC 考核目录');
-- 2030 考核配置（原「系统配置」，避免与 RuoYi 系统管理/参数设置混淆）
insert into sys_menu values('2030', '考核配置', '1500', '4', 'config-vqms', null, '', '', 1, 0, 'M', '0', '0', '', 'system', 'admin', sysdate(), '', null, 'VQMS 考核配置目录');

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
insert into sys_menu values('2021', '投运率', '2020', '1', 'runtime', 'vqms/avc-runtime/index', '', 'VqmsAvcRuntime', 1, 0, 'C', '0', '0', 'vqms:avc:runtime:list', 'chart', 'admin', sysdate(), '', null, 'AVC 装置投运率');
insert into sys_menu values('2022', '调节合格率', '2020', '2', 'regulation', 'vqms/avc-regulation/index', '', 'VqmsAvcRegulation', 1, 0, 'C', '0', '0', 'vqms:avc:regulation:list', 'chart', 'admin', sysdate(), '', null, 'AVC 调节合格率（快速性/经济性两档平行）');
-- 按钮：导出
insert into sys_menu values('2023', '投运率导出', '2021', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:avc:runtime:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2024', '调节合格率导出', '2022', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:avc:regulation:export', '#', 'admin', sysdate(), '', null, '');

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

-- ---------- 母线下拉支撑接口授权（D5 增补 2026-08-21：/vqms/vqms_busbar/list 供 5 个母线维度页面级联，各自挂 F 保证按页授权即可用） ----------
insert into sys_menu values('2037', '母线选择-曲线', '2011', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:vqms_busbar:list', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2038', '母线选择-日报', '2001', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:vqms_busbar:list', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2039', '母线选择-月报', '2002', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:vqms_busbar:list', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2040', '母线选择-年报', '2003', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:vqms_busbar:list', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2041', '母线选择-阈值', '2031', '6', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:vqms_busbar:list', '#', 'admin', sysdate(), '', null, '');
insert into sys_role_menu values('2', '2037');
insert into sys_role_menu values('2', '2038');
insert into sys_role_menu values('2', '2039');
insert into sys_role_menu values('2', '2040');
insert into sys_role_menu values('2', '2041');

-- ---------- 判定参数（D7 增补 2026-08-22：§6.2.5 / §10.1） ----------
insert into sys_menu values('2042', '判定参数', '2030', '2', 'judgeparam', 'vqms/judgeparam/index', '', 'VqmsJudgeParam', 1, 0, 'C', '0', '0', 'vqms:judgeparam:list', 'edit', 'admin', sysdate(), '', null, '判定整定参数管理（t_fast 整定 / 锁定行）');
insert into sys_menu values('2043', '判定参数新增', '2042', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:judgeparam:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2044', '判定参数修改', '2042', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:judgeparam:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2045', '判定参数删除', '2042', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:judgeparam:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_role_menu values('2', '2042');
insert into sys_role_menu values('2', '2043');
insert into sys_role_menu values('2', '2044');
insert into sys_role_menu values('2', '2045');


-- ---------- 策略参数（S5 UI 选套页，2026-08-24：§8.7 策略参数页 / §10.1） ----------
-- perms 全小写 vqms:policyparam:*；唯一写路径 apply（无逐行 add/edit/remove，杜绝绕过预设）
insert into sys_menu values('2046', '策略参数', '2030', '3', 'policyparam', 'vqms/policyparam/index', '', 'VqmsPolicyParam', 1, 0, 'C', '0', '0', 'vqms:policyparam:list', 'checkbox', 'admin', sysdate(), '', null, '数据不可用策略选套页（甲乙丙丁，选套值留空待政策拍板）');
insert into sys_menu values('2047', '策略选套应用', '2046', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:policyparam:apply', '#', 'admin', sysdate(), '', null, '');
insert into sys_role_menu values('2', '2046');
insert into sys_role_menu values('2', '2047');


-- ---------- 母线台账管理（2026-08-26：vqms_busbar 此前仅 DB 手工维护，现场数据接入后补页面 CRUD） ----------
insert into sys_menu values('2048', '母线管理', '2030', '4', 'busbar', 'vqms/busbar/index', '', 'VqmsBusbar', 1, 0, 'C', '0', '0', 'vqms:vqms_busbar:list', 'build', 'admin', sysdate(), '', null, '母线台账管理（busbar CRUD）');
insert into sys_menu values('2049', '母线查询', '2048', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:vqms_busbar:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2050', '母线新增', '2048', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:vqms_busbar:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2051', '母线修改', '2048', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:vqms_busbar:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2052', '母线删除', '2048', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:vqms_busbar:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_role_menu values('2', '2048');
insert into sys_role_menu values('2', '2049');
insert into sys_role_menu values('2', '2050');
insert into sys_role_menu values('2', '2051');
insert into sys_role_menu values('2', '2052');
-- viewer(201) 只读
insert into sys_role_menu values('201', '2048');
insert into sys_role_menu values('201', '2049');
