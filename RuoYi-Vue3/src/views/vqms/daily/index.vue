<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" v-show="showSearch" :inline="true" label-width="68px">
      <el-form-item label="统计日期" prop="statDate">
        <el-date-picker
          v-model="queryParams.statDate"
          value-format="YYYY-MM-DD"
          type="date"
          placeholder="选择日期"
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
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['vqms:daily:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="统计日期" prop="statDate" width="120" />
      <el-table-column label="母线" prop="busbarNum" width="80">
        <template #default="scope">{{ String(scope.row.busbarNum) === '0' ? '主母线' : '副母线' }}</template>
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

<script setup name="VqmsDaily">
import { listDaily } from '@/api/vqms/voltageDaily'
import { listBusbar } from '@/api/vqms/busbar'

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
    statDate: undefined,
    vGrade: undefined,
    busbarNum: undefined
  }
})
const { queryParams } = toRefs(data)

// 母线下拉走后端 /vqms/vqms_busbar/list；类型归一到字符串与字典编码对齐
const busbarList = ref([])
onMounted(() => {
  listBusbar().then(response => {
    busbarList.value = (response.rows || []).map(b => ({
      busbarNum: String(b.busbarNum),
      busbarName: b.busbarName,
      vGrade: b.vGrade == null ? undefined : String(b.vGrade)
    }))
  })
})
const busbarOptions = computed(() =>
  queryParams.value.vGrade ? busbarList.value.filter(b => b.vGrade === queryParams.value.vGrade) : busbarList.value
)

function handleVGradeChange() {
  // 等级切换后当前母线若不在该等级下，落到该等级第一条母线，避免组合出空结果
  if (queryParams.value.busbarNum && !busbarOptions.value.some(b => b.busbarNum === queryParams.value.busbarNum)) {
    queryParams.value.busbarNum = busbarOptions.value[0]?.busbarNum
  }
}

function getList() {
  loading.value = true
  listDaily(queryParams.value).then(response => {
    dataList.value = response.rows
    total.value = response.total
  }).catch(() => {
    // 后端未就绪：空态兜底，保证页面不崩
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
  proxy.download('vqms/stats/daily/export', { ...queryParams.value }, `daily_${new Date().getTime()}.xlsx`)
}

getList()
</script>
