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
      选套即整组写入约定键（甲乙丙丁 = 四键向量；戊 = 自由组合规则行族 freeform_rule_*），
      写穿缓存、@Log 留痕。<span style="color:#909399">记账生效随统计管线（S4 调度启用后）；选套本身属政策拍板动作，权限默认仅授管理员。</span>
    </el-alert>

    <!-- 套别单选卡片（甲乙丙丁映射权威 = 后端 PolicyPreset 枚举；戊=自由组合 §3.3，规则表经对话框编辑） -->
    <el-row :gutter="12" style="margin-bottom: 16px">
      <el-col :span="6" v-for="p in presets" :key="p.code">
        <el-card
          shadow="hover"
          :style="form.presetCode === p.code ? { borderColor: '#409eff', borderWidth: '2px' } : {}"
          @click="selectPreset(p)"
          style="cursor: pointer"
        >
          <div>
            <b>{{ p.label }}</b>
            <el-tag v-if="p.recommended" size="small" type="success" style="margin-left: 8px">推荐</el-tag>
            <el-tag v-if="p.freeform" size="small" type="warning" style="margin-left: 8px">自定义</el-tag>
          </div>
          <div style="color: #909399; font-size: 13px; margin-top: 6px">{{ p.description }}</div>
          <div v-if="!p.freeform" style="font-size: 12px; margin-top: 8px; line-height: 1.8">
            判不了：{{ modeText(p.undecodableMode) }}<br/>
            档无效：{{ modeText(p.invalidTierMode) }}<br/>
            部分缺：{{ modeText(p.partialMissingMode) }}<template v-if="p.code === 'YI'">（可用度阈值可整定）</template>
          </div>
          <div v-else style="font-size: 12px; margin-top: 8px; line-height: 1.8">
            原子条件自由组装有序规则表<br/>
            （首中即断；点击卡片编辑规则）
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-form label-width="140px" style="max-width: 560px">
      <el-form-item label="乙档阈值(%)" v-if="form.presetCode === 'YI'">
        <el-input-number v-model="form.thresholdPct" :min="0" :max="100" :step="5" step-strictly controls-position="right" />
        <span style="margin-left: 8px; color: #909399">部分缺可用度阈值，默认 50；留空用默认</span>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleApply" v-hasPermi="['vqms:policyparam:apply']">应用选套</el-button>
      </el-form-item>
    </el-form>

    <!-- 当前四键原值（只读） -->
    <el-divider content-position="left">当前参数（vqms_policy_param 原值，只读）</el-divider>
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="参数键" prop="paramKey" width="280" />
      <el-table-column label="值" prop="paramValue" width="220" />
      <el-table-column label="名称" prop="name" :show-overflow-tooltip="true" />
    </el-table>

    <!-- 戊·自由组合规则编辑对话框（策略文档 §3.3） -->
    <el-dialog v-model="ffDialog.visible" title="戊·自由组合 — 规则表编辑" width="720px">
      <el-alert type="info" :closable="false" style="margin-bottom: 12px">
        每行一条规则：<b>表达式 -&gt; 处置动作</b>。顺序即优先级（首中即断；全不中按正常记账）。<br/>
        原子：A1 解码失败 / A1A 脏写 / A1B 循环码非法 / A1C 缺t₀ / A2 档不可判 / A3 部分缺 / A4 可用度低于τ。<br/>
        连接词 &amp;（且）、|（或）——同层混用须一层括号分组；! 取反（原子或括号组）；动作：COUNT_NORMAL / EXCLUDE_REPORTED / COUNT_UNQUALIFIED / PEND_MARKED。
        <template v-if="state.freeformRules">示例：A1 | (A2 &amp; !A3) -> EXCLUDE_REPORTED</template>
      </el-alert>
      <el-form label-width="120px">
        <el-form-item label="规则表（有序）">
          <el-input
            v-model="ffDialog.rulesText"
            type="textarea"
            :rows="8"
            :placeholder="ffPlaceholder"
            style="font-family: monospace"
          />
        </el-form-item>
        <el-form-item label="可用度阈值 τ(%)">
          <el-input-number v-model="ffDialog.thresholdPct" :min="0" :max="100" :step="5" step-strictly controls-position="right" />
          <span style="margin-left: 8px; color: #909399">全局单值，默认 50；A4 引用时消费</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ffDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="handleApplyFreeform" v-hasPermi="['vqms:policyparam:apply']">校验并应用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Policyparam">
import { listPolicyParam, getPolicyState, applyPreset, applyFreeform } from "@/api/vqms/policyParam";

const { proxy } = getCurrentInstance();

const loading = ref(false);
const dataList = ref([]);
// 套别候选：甲乙丙丁与后端 PolicyPreset 枚举一一对应（仅渲染，不存映射）；
// 戊=自由组合（策略文档 §3.3，2026-08-25），规则表语义权威在后端校验器
const presets = [
  { code: "JIA", label: "甲", description: "宽松跳过", undecodableMode: "EXCLUDE_REPORTED", invalidTierMode: "EXCLUDE_REPORTED", partialMissingMode: "COUNT_NORMAL" },
  { code: "YI", label: "乙", description: "阈值剔除+计数", recommended: true, undecodableMode: "EXCLUDE_REPORTED", invalidTierMode: "EXCLUDE_REPORTED", partialMissingMode: "EXCLUDE_REPORTED" },
  { code: "BING", label: "丙", description: "计不合格", undecodableMode: "COUNT_UNQUALIFIED", invalidTierMode: "COUNT_UNQUALIFIED", partialMissingMode: "COUNT_UNQUALIFIED" },
  { code: "DING", label: "丁", description: "标记挂起", undecodableMode: "PEND_MARKED", invalidTierMode: "PEND_MARKED", partialMissingMode: "PEND_MARKED" },
  { code: "WU", label: "戊", description: "自由组合（§3.3）", freeform: true }
];
const state = ref({});
const form = reactive({ presetCode: null, thresholdPct: undefined });
const ffDialog = reactive({ visible: false, rulesText: "", thresholdPct: 50 });
const ffPlaceholder = "A1B -> PEND_MARKED\nA1 -> EXCLUDE_REPORTED\n(A2 & !A3) -> PEND_MARKED\nA3 & A4 -> EXCLUDE_REPORTED";

function modeText(mode) {
  return { COUNT_NORMAL: "正常记账", EXCLUDE_REPORTED: "剔除+计数", COUNT_UNQUALIFIED: "计不合格", PEND_MARKED: "挂起标记" }[mode] || mode;
}

function selectPreset(p) {
  form.presetCode = p.code;
  if (p.freeform) {
    // 打开规则编辑对话框；已生效的戊规则回填（state 由 /state 接口下发）
    ffDialog.rulesText = (state.value.freeformRules || []).join("\n");
    ffDialog.thresholdPct = state.value.freeformThresholdPct || 50;
    ffDialog.visible = true;
    return;
  }
  form.thresholdPct = p.code === "YI" ? 50 : undefined;
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
    }
  });
}

function handleApply() {
  if (!form.presetCode) {
    proxy.$modal.msgWarning("请先选择套别");
    return;
  }
  const data = { presetCode: form.presetCode };
  if (form.presetCode === "YI" && form.thresholdPct != null) {
    data.thresholdPct = form.thresholdPct;
  }
  proxy.$modal.confirm(`确认应用套别「${form.presetCode}」？整组覆盖写入四约定键。`).then(() => {
    applyPreset(data).then(() => {
      proxy.$modal.msgSuccess("应用成功");
      getList();
      getState();
    });
  }).catch(() => {});
}

function handleApplyFreeform() {
  const rules = (ffDialog.rulesText || "")
    .split("\n")
    .map(l => l.trim())
    .filter(l => l.length > 0);
  if (rules.length === 0) {
    proxy.$modal.msgWarning("规则表至少一条");
    return;
  }
  const data = { rules, thresholdPct: ffDialog.thresholdPct };
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
