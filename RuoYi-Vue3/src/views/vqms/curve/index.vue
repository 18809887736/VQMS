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
        <span>电压曲线（high_SV / low_SV / average_SV）</span>
      </template>
      <div ref="chartRef" style="height: 480px" v-loading="loading"></div>
      <div v-if="!loading && !hasData" class="empty-tip">暂无数据</div>
    </el-card>
  </div>
</template>

<script setup name="VqmsCurve">
import * as echarts from 'echarts'
import { listCurve } from '@/api/vqms/curve'

const { proxy } = getCurrentInstance()
const { vqms_v_grade } = proxy.useDict('vqms_v_grade')
const chartRef = ref(null)
const loading = ref(false)
const hasData = ref(false)
let chartInstance = null

// 阶段 2 占位：与 sql/vqms.sql busbar 种子数据一致；后端 /vqms/busbar 列表接口就绪后改为接口拉取
const busbarList = [
  { busbarNum: '0', busbarName: '220kV 东母线', vGrade: '1' },
  { busbarNum: '1', busbarName: '220kV 西母线', vGrade: '1' }
]
const busbarOptions = computed(() =>
  queryParams.vGrade ? busbarList.filter(b => b.vGrade === queryParams.vGrade) : busbarList
)

const queryParams = reactive({
  timeRange: [],
  vGrade: undefined,
  busbarNum: '0'
})

function handleVGradeChange() {
  // 等级切换后当前母线若不在该等级下，落到该等级第一条母线，避免组合出空结果
  if (queryParams.busbarNum && !busbarOptions.value.some(b => b.busbarNum === queryParams.busbarNum)) {
    queryParams.busbarNum = busbarOptions.value[0]?.busbarNum
  }
}

// 阶段 2：后端未就绪时用 mock 数据渲染曲线骨架
const mockCurve = () => {
  const times = []
  const high = []
  const low = []
  const avg = []
  for (let i = 0; i < 60; i++) {
    times.push(`${String(i).padStart(2, '0')}:00`)
    const base = 234
    high.push(+(base + 0.8 + Math.sin(i / 10) * 0.5).toFixed(2))
    low.push(+(base - 0.8 - Math.sin(i / 10) * 0.5).toFixed(2))
    avg.push(base)
  }
  return { times, high, low, avg }
}

function renderChart({ times, high, low, avg }) {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value, 'macarons')
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['high_SV', 'low_SV', 'average_SV'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: times },
    yAxis: { type: 'value', name: 'kV' },
    series: [
      { name: 'high_SV', type: 'line', data: high, smooth: true },
      { name: 'low_SV', type: 'line', data: low, smooth: true },
      { name: 'average_SV', type: 'line', data: avg, smooth: true, lineStyle: { type: 'dashed' } }
    ]
  })
  window.addEventListener('resize', () => chartInstance && chartInstance.resize())
}

function handleQuery() {
  loading.value = true
  listCurve({
    beginTime: queryParams.timeRange?.[0],
    endTime: queryParams.timeRange?.[1],
    vGrade: queryParams.vGrade,
    busbarNum: queryParams.busbarNum
  }).then(response => {
    // 后端就绪后：response.rows 转为 times/high/low/avg 渲染
    const mock = mockCurve()
    hasData.value = true
    renderChart(mock)
  }).catch(() => {
    // 后端未就绪：mock 兜底渲染骨架
    hasData.value = true
    renderChart(mockCurve())
  }).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleQuery()
})

onBeforeUnmount(() => {
  chartInstance && chartInstance.dispose()
})
</script>

<style scoped>
.empty-tip {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
</style>
