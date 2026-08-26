-- 母线台账管理菜单（2026-08-26：vqms_busbar 此前仅 DB 手工维护；现场数据接入后补页面 CRUD）
-- 挂 考核配置(2030) 下，序 4（阈值管理/判定参数/策略参数之后）；viewer(201) 授只读两键

insert into sys_menu values('2048', '母线管理', '2030', '4', 'busbar', 'vqms/busbar/index', '', 'VqmsBusbar', 1, 0, 'C', '0', '0', 'vqms:vqms_busbar:list', 'build', 'admin', sysdate(), '', null, '母线台账管理（busbar CRUD）');
insert into sys_menu values('2049', '母线查询', '2048', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:vqms_busbar:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2050', '母线新增', '2048', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:vqms_busbar:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2051', '母线修改', '2048', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:vqms_busbar:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2052', '母线删除', '2048', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'vqms:vqms_busbar:remove', '#', 'admin', sysdate(), '', null, '');

-- viewer 只读（列表+查询）
insert into sys_role_menu values (201, 2048);
insert into sys_role_menu values (201, 2049);
