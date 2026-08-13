<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" v-show="showSearch" :inline="true" label-width="68px">
      <el-form-item label="统计月份" prop="statMonth">
        <el-date-picker
          v-model="queryParams.statMonth"
          value-format="YYYY-MM"
          type="month"
          placeholder="选择月份"
          clearable
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="母线" prop="busbarNum">
        <el-select v-model="queryParams.busbarNum" placeholder="母线" clearable style="width: 160px">
          <el-option label="主母线 (0)" value="0" />
          <el-option label="副母线 (1)" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['vqms:monthly:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="统计月份" prop="statMonth" width="120" />
      <el-table-column label="母线" prop="busbarNum" width="80">
        <template #default="scope">{{ scope.row.busbarNum === '0' ? '主母线' : '副母线' }}</template>
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

<script setup name="VqmsMonthly">
import { listMonthly } from '@/api/vqms/voltageMonthly'

const { proxy } = getCurrentInstance()

const dataList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    statMonth: undefined,
    busbarNum: undefined
  }
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listMonthly(queryParams.value).then(response => {
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
  proxy.download('vqms/monthly/export', { ...queryParams.value }, `monthly_${new Date().getTime()}.xlsx`)
}

getList()
</script>
