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
      <el-form-item label="母线" prop="busbarNum">
        <el-select v-model="queryParams.busbarNum" placeholder="母线" style="width: 160px">
          <el-option label="主母线 (0)" value="0" />
          <el-option label="副母线 (1)" value="1" />
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
const chartRef = ref(null)
const loading = ref(false)
const hasData = ref(false)
let chartInstance = null

const queryParams = reactive({
  timeRange: [],
  busbarNum: '0'
})

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
