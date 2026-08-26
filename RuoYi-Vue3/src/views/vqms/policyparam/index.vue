<template>
  <div class="app-container">
    <!-- 页面三态横幅（§8.7）：未选套 / 已选套 -->
    <el-alert
      :type="state.selectedCode ? 'success' : 'warning'"
      :closable="false"
      style="margin-bottom: 12px"
      :title="state.stateLabel || '加载中…'"
    />
    <el-alert type="info" :closable="false" style="margin-bottom: 12px">
      应用戊·自由组合即整组写入自由组合键族（freeform_rule_001..N + freeform_threshold_pct），
      写穿缓存、@Log 留痕。<span style="color:#909399">记账生效随统计管线（S4 调度启用后）；策略应用属政策拍板动作，权限默认仅授管理员。</span>
    </el-alert>

    <!-- 套别（2026-08-25 拍板：固定候选全部退役，唯一候选=戊·自由组合，策略文档 §3.3.4） -->
    <el-row :gutter="12" style="margin-bottom: 16px">
      <el-col :span="6">
        <el-card
          shadow="hover"
          :style="form.presetCode === 'WU' ? { borderColor: '#409eff', borderWidth: '2px' } : {}"
          @click="selectPreset(presets[0])"
          style="cursor: pointer"
        >
          <div>
            <b>戊</b>
            <el-tag size="small" type="warning" style="margin-left: 8px">唯一候选</el-tag>
          </div>
          <div style="color: #909399; font-size: 13px; margin-top: 6px">自由组合（§3.3）</div>
          <div style="font-size: 12px; margin-top: 8px; line-height: 1.8">
            原子条件自由组装有序规则表<br/>
            （首中即断；点击卡片编辑规则并应用）
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 当前四键原值（只读） -->
    <el-divider content-position="left">当前参数（vqms_policy_param 原值，只读）</el-divider>
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="参数键" prop="paramKey" width="280" />
      <el-table-column label="值" prop="paramValue" width="220" />
      <el-table-column label="名称" prop="name" :show-overflow-tooltip="true" />
    </el-table>

    <!-- 戊·自由组合规则构建器（策略文档 §3.3；DSL 序列化经 rulesDsl.js，校验以服务端为准） -->
    <el-dialog v-model="ffDialog.visible" title="戊·自由组合 — 规则表编辑" width="900px" top="6vh" class="scrollbar">
      <el-alert v-if="ffDialog.corrupted" type="error" :closable="false" style="margin-bottom: 12px">
        检测到规则源数据异常（损坏或部分行无法解析）——为防误覆写，<b>「校验并应用」已禁用</b>；请核对后修正再试。
      </el-alert>
      <el-alert type="info" :closable="false" style="margin-bottom: 12px">
        规则自上而下依次判定，<b>首中即断</b>（命中即按该规则处置，全不中按正常记账）。
        括号组用于「先组内再组外」的组合；「非」= 取反。可用度阈值 τ 供 A4 消费（全局单值）。
      </el-alert>

      <div v-for="(r, ri) in ffDialog.rules" :key="ri" class="rule-card">
        <div class="rule-head">
          <b>规则 {{ ri + 1 }}</b>
          <el-tag size="small" type="info" class="rule-preview" :title="serializeRule(r)">{{ serializeRule(r) }}</el-tag>
          <span class="flex-spacer"></span>
          <el-button link :disabled="ri === 0" @click="moveRule(ri, -1)">上移</el-button>
          <el-button link :disabled="ri === ffDialog.rules.length - 1" @click="moveRule(ri, 1)">下移</el-button>
          <el-button link type="danger" @click="removeRule(ri)">删除</el-button>
        </div>

        <div v-if="r.terms.length > 1" class="connector-row">
          <span class="row-label">项间连接</span>
          <el-radio-group v-model="r.join" size="small">
            <el-radio-button value="AND">全部满足（且）</el-radio-button>
            <el-radio-button value="OR">任一满足（或）</el-radio-button>
          </el-radio-group>
        </div>

        <div v-for="(t, ti) in r.terms" :key="ti" class="term-block">
          <!-- 括号组 -->
          <template v-if="t.kind === 'group'">
            <div class="group-box">
              <div class="group-head">
                <el-checkbox v-model="t.neg">整组取反（非）</el-checkbox>
                <span class="row-label" v-if="t.atoms.length > 1">组内连接</span>
                <el-radio-group v-if="t.atoms.length > 1" v-model="t.join" size="small">
                  <el-radio-button value="AND">且</el-radio-button>
                  <el-radio-button value="OR">或</el-radio-button>
                </el-radio-group>
                <span class="flex-spacer"></span>
                <el-button link type="danger" @click="removeTerm(r, ti)">删除本组</el-button>
              </div>
              <div v-for="(a, ai) in t.atoms" :key="ai" class="atom-row inset">
                <el-select v-model="a.atom" style="width: 260px" size="small">
                  <el-option v-for="o in atomOptions" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
                <el-checkbox v-model="a.neg" size="small">非</el-checkbox>
                <el-button link type="danger" size="small" @click="t.atoms.splice(ai, 1)">移除</el-button>
              </div>
              <el-button link size="small" @click="t.atoms.push({ atom: 'A2', neg: false })">+ 组内条件</el-button>
            </div>
          </template>
          <!-- 单原子 -->
          <template v-else>
            <div class="atom-row">
              <el-select v-model="t.atom" style="width: 260px" size="small">
                <el-option v-for="o in atomOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
              <el-checkbox v-model="t.neg" size="small">非</el-checkbox>
              <el-button link type="danger" size="small" @click="removeTerm(r, ti)">移除</el-button>
            </div>
          </template>
        </div>

        <div class="term-actions">
          <el-button link size="small" @click="r.terms.push({ kind: 'atom', atom: 'A2', neg: false })">+ 条件</el-button>
          <el-button link size="small" @click="r.terms.push({ kind: 'group', neg: false, join: 'AND', atoms: [{ atom: 'A2', neg: false }] })">+ 括号组</el-button>
        </div>

        <div class="action-row">
          <span class="row-label">处置动作</span>
          <el-select v-model="r.action" style="width: 260px" size="small">
            <el-option v-for="o in actionOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <span v-if="r.action === 'COUNT_NORMAL'" class="hint">
            {{ ruleRefs(r).has('A3') ? '' : '⚠ 触发须含 A3（部分缺），否则无法应用' }}
          </span>
        </div>
      </div>

      <el-button style="width: 100%; margin-top: 8px" @click="addRule">+ 添加规则</el-button>

      <el-form label-width="140px" style="margin-top: 16px">
        <el-form-item label="可用度阈值 τ(%)">
          <el-input-number v-model="ffDialog.thresholdPct" :min="0" :max="100" :step="5" step-strictly controls-position="right" />
          <span style="margin-left: 8px; color: #909399">全局单值，默认 50；A4 引用时消费</span>
        </el-form-item>
      </el-form>

      <el-collapse style="margin-top: 4px">
        <el-collapse-item title="高级文本模式（直接编辑 DSL，可从构建器生成/解析回）" name="adv">
          <el-input v-model="ffDialog.advancedText" type="textarea" :rows="6" style="font-family: monospace" />
          <div style="margin-top: 8px">
            <el-button size="small" @click="fillAdvancedText">从构建器生成文本</el-button>
            <el-button size="small" type="primary" plain @click="parseAdvancedText">解析文本到构建器</el-button>
          </div>
        </el-collapse-item>
      </el-collapse>

      <template #footer>
        <el-button @click="ffDialog.visible = false">取消</el-button>
        <el-button type="primary" :disabled="ffDialog.corrupted" @click="handleApplyFreeform" v-hasPermi="['vqms:policyparam:apply']">校验并应用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Policyparam">
import { listPolicyParam, getPolicyState, applyFreeform } from "@/api/vqms/policyParam";
import {
  ATOM_OPTIONS, ACTION_OPTIONS, MAX_RULES, parseRuleLine, serializeRule,
  refsOfRule, newRule, validateRulesClient
} from "./rulesDsl";

const { proxy } = getCurrentInstance();

const loading = ref(false);
const dataList = ref([]);
// 套别（2026-08-25 拍板）：固定候选全部退役，唯一候选=戊·自由组合（策略文档 §3.3.4）；
// 规则表语义权威在后端校验器，前端仅构建与渲染
const presets = [
  { code: "WU", label: "戊", description: "自由组合（§3.3）", freeform: true }
];
const state = ref({});
const form = reactive({ presetCode: null, thresholdPct: undefined });
const atomOptions = ATOM_OPTIONS;
const actionOptions = ACTION_OPTIONS;
const ffDialog = reactive({ visible: false, rules: [], thresholdPct: 50, advancedText: "", corrupted: false });

function selectPreset(p) {
  form.presetCode = p.code;
  if (p.freeform) {
    openFreeformDialog();
  }
}

function openFreeformDialog() {
  const lines = state.value.freeformRules;
  if (state.value.selectedCode === "WU" && !lines) {
    // 后端规则表损坏态（currentState catch 分支不下发 freeformRules）——显性告警，禁用应用
    ffDialog.corrupted = true;
    ffDialog.rules = [newRule()];
  } else if (lines) {
    ffDialog.corrupted = false;
    const failed = [];
    ffDialog.rules = [];
    lines.forEach((line, i) => {
      try {
        ffDialog.rules.push(parseRuleLine(line));
      } catch (e) {
        failed.push("第 " + (i + 1) + " 行: " + (e.message || e));
      }
    });
    if (failed.length) {
      ffDialog.corrupted = true;
      proxy.$modal.msgError("部分规则行无法解析（已跳过，应用已禁用）：" + failed.join("；"));
    }
    if (!ffDialog.rules.length) ffDialog.rules = [newRule()];
  } else {
    // 首次配置（未选套）：从空白默认开始
    ffDialog.corrupted = false;
    ffDialog.rules = [newRule()];
  }
  const t = state.value.freeformThresholdPct;
  ffDialog.thresholdPct = (t === 0 || t) ? t : 50;
  ffDialog.advancedText = ffDialog.rules.map(serializeRule).join("\n");
  ffDialog.visible = true;
}

function addRule() {
  if (ffDialog.rules.length >= MAX_RULES) {
    proxy.$modal.msgWarning("规则数超上限 " + MAX_RULES);
    return;
  }
  ffDialog.rules.push(newRule());
}

function removeRule(ri) {
  ffDialog.rules.splice(ri, 1);
}

function moveRule(ri, delta) {
  const target = ri + delta;
  const arr = ffDialog.rules;
  if (target < 0 || target >= arr.length) return;
  [arr[ri], arr[target]] = [arr[target], arr[ri]];
}

function removeTerm(rule, ti) {
  rule.terms.splice(ti, 1);
}

function ruleRefs(rule) {
  return refsOfRule(rule);
}

function fillAdvancedText() {
  ffDialog.advancedText = ffDialog.rules.map(serializeRule).join("\n");
}

function parseAdvancedText() {
  const lines = (ffDialog.advancedText || "").split("\n").map(l => l.trim()).filter(l => l.length > 0);
  if (!lines.length) {
    proxy.$modal.msgError("文本为空——规则表至少一条");
    return;
  }
  try {
    const parsed = lines.map(parseRuleLine);
    if (parsed.length > MAX_RULES) {
      proxy.$modal.msgWarning("已载入 " + parsed.length + " 条规则——超上限 " + MAX_RULES + "，应用前请精简");
    } else {
      proxy.$modal.msgSuccess("解析成功，" + parsed.length + " 条规则已载入构建器");
    }
    ffDialog.rules = parsed;
    ffDialog.corrupted = false;
  } catch (e) {
    proxy.$modal.msgError("解析失败：" + (e.message || e));
  }
}

function getList() {
  loading.value = true;
  listPolicyParam().then(res => {
    dataList.value = res.rows;
    loading.value = false;
  });
}

function getState() {
  getPolicyState().then(res => {
    state.value = res.data || {};
    if (res.data && res.data.selectedCode) {
      form.presetCode = res.data.selectedCode;
      // 戊的持久化值（规则行/τ）由对话框打开时经 freeformRules/freeformThresholdPct 回填
    }
  });
}

function handleApplyFreeform() {
  const rules = ffDialog.rules;
  const errors = validateRulesClient(rules);
  if (errors.length) {
    proxy.$modal.msgWarning(errors.slice(0, 3).join("；") + (errors.length > 3 ? "（等 " + errors.length + " 项）" : ""));
    return;
  }
  // 高级文本与构建器不一致时阻断——防「改了文本忘解析」被无声丢弃
  const generated = rules.map(serializeRule).join("\n");
  if ((ffDialog.advancedText || "").trim() && ffDialog.advancedText.trim() !== generated.trim()) {
    proxy.$modal.msgWarning("高级文本与构建器当前状态不一致：请先「解析文本到构建器」或「从构建器生成文本」后再应用");
    return;
  }
  const data = { rules: rules.map(serializeRule), thresholdPct: ffDialog.thresholdPct };
  proxy.$modal.confirm(`确认应用戊·自由组合（${rules.length} 条规则）？校验失败将整体拒绝、原策略保持不变。`).then(() => {
    applyFreeform(data).then(res => {
      const hint = res.data && res.data.reductionHint;
      proxy.$modal.msgSuccess(hint ? `应用成功（等价规约提示 ≡ ${hint}，请自查是否误配）` : "应用成功");
      ffDialog.visible = false;
      getList();
      getState();
    });
  }).catch(() => {});
}

getList();
getState();
</script>

<style scoped>
.rule-card {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 12px;
  background: #fafafa;
}
.rule-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.rule-preview {
  font-family: monospace;
  max-width: 480px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.flex-spacer {
  flex: 1;
}
.connector-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.row-label {
  color: #909399;
  font-size: 13px;
}
.term-block {
  margin-bottom: 6px;
}
.atom-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}
.atom-row.inset {
  margin-left: 24px;
}
.group-box {
  border: 1px dashed #c0c4cc;
  border-radius: 4px;
  padding: 8px 10px;
  background: #fff;
}
.group-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.term-actions {
  margin: 4px 0 8px;
}
.action-row {
  display: flex;
  align-items: center;
  gap: 8px;
  border-top: 1px solid #ebeef5;
  padding-top: 8px;
}
.hint {
  color: #e6a23c;
  font-size: 12px;
}
</style>
