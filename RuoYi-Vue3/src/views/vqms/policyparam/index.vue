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
      选套即整组写入四约定键（undecodable_mode / invalid_tier_mode / partial_missing_mode / partial_missing_threshold_pct），
      写穿缓存、@Log 留痕。<span style="color:#909399">记账生效随统计管线（S4 调度启用后）；选套本身属政策拍板动作，权限默认仅授管理员。</span>
    </el-alert>

    <!-- 套别单选卡片（映射唯一权威 = 后端 PolicyPreset 枚举，前端只渲染标签） -->
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
          </div>
          <div style="color: #909399; font-size: 13px; margin-top: 6px">{{ p.description }}</div>
          <div style="font-size: 12px; margin-top: 8px; line-height: 1.8">
            判不了：{{ modeText(p.undecodableMode) }}<br/>
            档无效：{{ modeText(p.invalidTierMode) }}<br/>
            部分缺：{{ modeText(p.partialMissingMode) }}<template v-if="p.code === 'YI'">（可用度阈值可整定）</template>
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
  </div>
</template>

<script setup name="Policyparam">
import { listPolicyParam, getPolicyState, applyPreset } from "@/api/vqms/policyParam";

const { proxy } = getCurrentInstance();

const loading = ref(false);
const dataList = ref([]);
// 套别候选：与后端 PolicyPreset 枚举一一对应（仅渲染，不存映射）
const presets = [
  { code: "JIA", label: "甲", description: "宽松跳过", undecodableMode: "EXCLUDE_REPORTED", invalidTierMode: "EXCLUDE_REPORTED", partialMissingMode: "COUNT_NORMAL" },
  { code: "YI", label: "乙", description: "阈值剔除+计数", recommended: true, undecodableMode: "EXCLUDE_REPORTED", invalidTierMode: "EXCLUDE_REPORTED", partialMissingMode: "EXCLUDE_REPORTED" },
  { code: "BING", label: "丙", description: "计不合格", undecodableMode: "COUNT_UNQUALIFIED", invalidTierMode: "COUNT_UNQUALIFIED", partialMissingMode: "COUNT_UNQUALIFIED" },
  { code: "DING", label: "丁", description: "标记挂起", undecodableMode: "PEND_MARKED", invalidTierMode: "PEND_MARKED", partialMissingMode: "PEND_MARKED" }
];
const state = ref({});
const form = reactive({ presetCode: null, thresholdPct: undefined });

function modeText(mode) {
  return { COUNT_NORMAL: "正常记账", EXCLUDE_REPORTED: "剔除+计数", COUNT_UNQUALIFIED: "计不合格", PEND_MARKED: "挂起标记" }[mode] || mode;
}

function selectPreset(p) {
  form.presetCode = p.code;
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

getList();
getState();
</script>
