<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="80px">
      <el-form-item label="时间范围" prop="timeRange">
        <el-date-picker
          v-model="queryParams.timeRange"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="datetimerange"
          range-separator="-"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          style="width: 380px"
        />
      </el-form-item>
      <el-form-item label="电压等级" prop="vGrade">
        <el-select v-model="queryParams.vGrade" placeholder="电压等级" clearable style="width: 160px" @change="handleVGradeChange">
          <el-option v-for="dict in vqms_v_grade" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="母线" prop="busbarNum">
        <el-select v-model="queryParams.busbarNum" placeholder="母线" style="width: 160px">
          <el-option v-for="b in busbarOptions" :key="b.busbarNum" :label="`${b.busbarName} (${b.busbarNum})`" :value="b.busbarNum" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
      </el-form-item>
    </el-form>

    <el-card shadow="never">
      <template #header>
        <span>电压曲线（high_SV / low_SV，逐分钟观测极值）</span>
      </template>
      <div ref="chartRef" style="height: 480px" v-loading="loading"></div>
      <div v-if="!loading && !hasData" class="empty-tip">暂无数据（选择母线与时间范围后查询）</div>
      <div v-if="truncated" class="empty-tip">窗口超过 {{ pageSize }} 分钟，仅显示前 {{ pageSize }} 个点（完整回查请缩小时间范围）</div>
    </el-card>
  </div>
</template>

<script setup name="VqmsCurve">
import * as echarts from 'echarts'
import { listCurve } from '@/api/vqms/curve'
import { listBusbar } from '@/api/vqms/busbar'

const { proxy } = getCurrentInstance()
const { vqms_v_grade } = proxy.useDict('vqms_v_grade')
const chartRef = ref(null)
const loading = ref(false)
const hasData = ref(false)
const truncated = ref(false)
let chartInstance = null

// 母线下拉走后端（v5.0 §10.1 /vqms/vqms_busbar/list）；类型归一到字符串与字典编码对齐
const busbarList = ref([])
const busbarOptions = computed(() =>
  queryParams.vGrade ? busbarList.value.filter(b => b.vGrade === queryParams.vGrade) : busbarList.value
)

const queryParams = reactive({
  timeRange: [],
  vGrade: undefined,
  busbarNum: undefined
})

// 图表一次渲染窗口内全部点：用接口上限 500（§10.2）；更优消费模式（聚合/分窗/hasMore 探测）待真实数据回放定稿
const pageSize = 500

function handleVGradeChange() {
  // 等级切换后当前母线若不在该等级下，落到该等级第一条母线，避免组合出空结果
  if (queryParams.busbarNum && !busbarOptions.value.some(b => b.busbarNum === queryParams.busbarNum)) {
    queryParams.busbarNum = busbarOptions.value[0]?.busbarNum
  }
}

function renderChart(rows) {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value, 'macarons')
    window.addEventListener('resize', handleResize)
  }
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['high_SV', 'low_SV'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: rows.map(r => r.saveTime) },
    yAxis: { type: 'value', name: 'kV', scale: true },
    series: [
      { name: 'high_SV', type: 'line', data: rows.map(r => r.highSV), smooth: true },
      { name: 'low_SV', type: 'line', data: rows.map(r => r.lowSV), smooth: true }
    ]
  })
}

function handleResize() {
  chartInstance && chartInstance.resize()
}

function handleQuery() {
  if (!queryParams.timeRange?.[0] || !queryParams.timeRange?.[1]) {
    proxy.$modal.msgWarning('请选择时间范围')
    return
  }
  loading.value = true
  listCurve({
    startTime: queryParams.timeRange[0],
    endTime: queryParams.timeRange[1],
    busbarNum: queryParams.busbarNum,
    pageNum: 1,
    pageSize
  }).then(response => {
    const rows = response.rows || []
    hasData.value = rows.length > 0
    truncated.value = (response.total || 0) > rows.length
    if (hasData.value) {
      renderChart(rows)
    }
  }).catch(() => {
    hasData.value = false
  }).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  listBusbar().then(response => {
    busbarList.value = (response.rows || []).map(b => ({
      busbarNum: String(b.busbarNum),
      busbarName: b.busbarName,
      vGrade: b.vGrade == null ? undefined : String(b.vGrade)
    }))
    if (!queryParams.busbarNum) {
      queryParams.busbarNum = busbarList.value[0]?.busbarNum
    }
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance && chartInstance.dispose()
  chartInstance = null
})
</script>

<style scoped>
.empty-tip {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
</style>
