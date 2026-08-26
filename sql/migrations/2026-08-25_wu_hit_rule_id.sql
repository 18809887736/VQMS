-- 戊·自由组合「命中规则 ID」留痕列（策略文档 §3.3.2 留痕条 / §3.3.5-4；2026-08-25 Leo /goal 代码侧落地）
-- 背景：FreeformPolicyEvaluator.Decision.ruleId（R001…）此前仅在求值结果内存态，审计无法从落账还原
--      每条指令的处置出处；本列把「哪条规则命中」持久化。
-- 口径：NULL = 预设模式（四键向量）/ 自由组合「全不中」兜底（COUNT_NORMAL）/ 未选套；
--      非 NULL = 戊规则表首中即断命中的规则（R001..R016，按应用时规则表序）。
-- 幂等：add column 无 IF NOT EXISTS（MySQL 8.4 不支持该子句），重放报 Duplicate column 属预期，跳过即可。

alter table vqms_regulation_cmd
  add column hit_rule_id varchar(8) default null
    comment '戊命中规则 ID（R001…；NULL=兜底/预设模式/未选套）'
    after disposition;
