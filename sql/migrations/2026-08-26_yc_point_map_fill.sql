-- vqms_yc_point_map 数据填充（2026-08-26）：按 docs/数据源头（草稿）.md 定号一览（2026-08-18 考据，
-- 证据链出自对端配置库 QHeatAvcRtdb）补全注册表。
-- 原则：
--   1) 全部 gate_enabled=0——现场核对前保守空转（GateFilter 只读 gate_enabled=1，本批零运行时影响）；
--   2) 真实候选与合成占位并存、point_name 自明（合成占位行加撞号警示）；
--   3) 证据链写进 remark，现场核对后逐点换号/置 1；
--   4) 幂等：重放报 Duplicate entry（yc_num 主键）属预期，跳过即可。

-- 1) 合成占位行：改名自明 + 撞号/换号警示入 remark（保留供合成库联调）
update vqms_yc_point_map set point_name = 'AVC投退(合成占位)',
  remark = '合成占位 3009：真实库 yx3009 = 四号机组下闭锁总信号（JS_DATA js109），撞号不同义、勿配真实环境；真实候选 yx1001'
  where yc_num = 3009 and point_name = 'AVC投退';

update vqms_yc_point_map set point_name = '主母线号(合成占位)',
  remark = '合成占位 4001：真实候选 yc3（对端 BUSBAR_GROUP.MainBarYcNum = 3，值域预期 0/1 = 东/西母线，待现场核对）'
  where yc_num = 4001 and point_name = '主母线号';

-- 2) 现场候选/新增点（定号一览 2026-08-18）
insert into vqms_yc_point_map (yc_num, point_name, point_type, unit, state_1_label, state_0_label, gate_enabled, remark) values
  (1001, 'AVC投退(现场候选)', 'yx', null, '投入', '退出', 0,
   '真实候选：AVC_INFO.AVCStatusYxNum=1001，CHUNNEL_YX 通道 3/4 转发佐证；⚠️ 语义待现场核对（不排除装置通信/闭锁状态）'),
  (3,    '主母线号(现场候选)', 'busbar_id', null, null, null, 0,
   '真实候选：BUSBAR_GROUP.MainBarYcNum=3；值域预期 0/1=东/西母线待现场核对；CHUNNEL_YC 无此点、写入来源待核'),
  (8,    '实时母线电压·东母(busbar 0)', 'voltage', 'kV', null, null, 0,
   'BUSBAR.realVYcNum=8（PlanVReferenceYcNum 同点自证）；CHUNNEL_YC = 1#高压采集 ×0.01；待现场核对'),
  (14,   '实时母线电压·西母(busbar 1)', 'voltage', 'kV', null, null, 0,
   'BUSBAR.realVYcNum=14（PlanVReferenceYcNum 同点自证）；CHUNNEL_YC = 2#高压采集 ×0.01；待现场核对'),
  (216,  '实时总有功·1号机', 'power', 'kW', null, null, 0,
   'GENERATOR.pYcNum=216（CHUNNEL_YC 1#机组采集 ×100）；与 yc316 由 VQMS 相加得全厂总有功；待现场核对'),
  (316,  '实时总有功·2号机', 'power', 'kW', null, null, 0,
   'GENERATOR.pYcNum=316（CHUNNEL_YC 2#机组采集 ×100）；与 yc216 由 VQMS 相加得全厂总有功；待现场核对'),
  (511,  '并网信号·正母单元', 'analog', null, null, null, 0,
   '对端 JS 算（外部DB/JS/JS_并网与母线号.sql）；编码 = 带电(1/0)×10 + 并网机组数；电厂并网 = yc511≥10 OR yc512≥10；RuntimePipeline 常量直读'),
  (512,  '并网信号·副母单元', 'analog', null, null, null, 0,
   '同 yc511（副母单元）；对端 JS 周期求值'),
  (521,  'AVC退出原因·正母', 'analog', null, null, null, 0,
   '接口已定待对端实现；三态 0=未退出 / 1=电网原因(免责) / 2=非电网原因(扣罚)'),
  (522,  'AVC退出原因·副母', 'analog', null, null, null, 0,
   '接口已定待对端实现；三态同 yc521（副母）');
