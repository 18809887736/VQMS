<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" v-show="showSearch" :inline="true" label-width="68px">
      <el-form-item label="统计年份" prop="statYear">
        <el-date-picker
          v-model="queryParams.statYear"
          value-format="YYYY"
          type="year"
          placeholder="选择年份"
          clearable
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="电压等级" prop="vGrade">
        <el-select v-model="queryParams.vGrade" placeholder="电压等级" clearable style="width: 160px" @change="handleVGradeChange">
          <el-option v-for="dict in vqms_v_grade" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="母线" prop="busbarNum">
        <el-select v-model="queryParams.busbarNum" placeholder="母线" clearable style="width: 160px">
          <el-option v-for="b in busbarOptions" :key="b.busbarNum" :label="`${b.busbarName} (${b.busbarNum})`" :value="b.busbarNum" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['vqms:yearly:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="统计年份" prop="statYear" width="120" />
      <el-table-column label="母线" prop="busbarNum" width="80">
        <template #default="scope">{{ scope.row.busbarNum === '0' ? '主母线' : '副母线' }}</template>
      </el-table-column>
      <el-table-column label="电压等级" width="100" align="center">
        <template #default="scope"><dict-tag :options="vqms_v_grade" :value="scope.row.vGrade" /></template>
      </el-table-column>
      <el-table-column label="总分钟数" prop="totalMinutes" width="100" />
      <el-table-column label="合格分钟" prop="qualifiedMinutes" width="100" />
      <el-table-column label="超上限分钟" prop="overHighMinutes" width="110" />
      <el-table-column label="超下限分钟" prop="overLowMinutes" width="110" />
      <el-table-column label="合格率(%)" prop="qualificationRate" width="110" />
      <el-table-column label="平均电压(kV)" prop="avgSv" width="120" />
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup name="VqmsYearly">
import { listYearly } from '@/api/vqms/voltageYearly'

const { proxy } = getCurrentInstance()
const { vqms_v_grade } = proxy.useDict('vqms_v_grade')

const dataList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    statYear: undefined,
    vGrade: undefined,
    busbarNum: undefined
  }
})
const { queryParams } = toRefs(data)

// 阶段 2 占位：与 sql/vqms.sql busbar 种子数据一致；后端 /vqms/busbar 列表接口就绪后改为接口拉取
const busbarList = [
  { busbarNum: '0', busbarName: '220kV 东母线', vGrade: '1' },
  { busbarNum: '1', busbarName: '220kV 西母线', vGrade: '1' }
]
const busbarOptions = computed(() =>
  queryParams.value.vGrade ? busbarList.filter(b => b.vGrade === queryParams.value.vGrade) : busbarList
)

function handleVGradeChange() {
  // 等级切换后当前母线若不在该等级下，落到该等级第一条母线，避免组合出空结果
  if (queryParams.value.busbarNum && !busbarOptions.value.some(b => b.busbarNum === queryParams.value.busbarNum)) {
    queryParams.value.busbarNum = busbarOptions.value[0]?.busbarNum
  }
}

function getList() {
  loading.value = true
  listYearly(queryParams.value).then(response => {
    dataList.value = response.rows
    total.value = response.total
  }).catch(() => {
    dataList.value = []
    total.value = 0
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleExport() {
  proxy.download('vqms/yearly/export', { ...queryParams.value }, `yearly_${new Date().getTime()}.xlsx`)
}

getList()
</script>
