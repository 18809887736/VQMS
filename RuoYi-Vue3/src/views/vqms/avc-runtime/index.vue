<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" v-show="showSearch" :inline="true" label-width="80px">
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
      <el-form-item label="并网主体" prop="gridSubject">
        <el-input v-model="queryParams.gridSubject" placeholder="并网主体名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['vqms:avc:runtime:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="统计月份" prop="statMonth" width="110" />
      <el-table-column label="并网主体" prop="gridSubject" :show-overflow-tooltip="true" />
      <el-table-column label="并网运行(分)" prop="gridMinutes" width="120" />
      <el-table-column label="投运(分)" prop="runtimeMinutes" width="100" />
      <el-table-column label="非电网退出(分)" prop="nonGridExitMinutes" width="130" />
      <el-table-column label="电网退出(分)" prop="gridExitMinutes" width="120" />
      <el-table-column label="投运率(%)" prop="runtimeRate" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.runtimeRate >= 99 ? 'success' : 'danger'">{{ scope.row.runtimeRate }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="缺额(百分点)" prop="deficit" width="110" />
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

<script setup name="VqmsAvcRuntime">
import { listRuntime } from '@/api/vqms/avcRuntime'

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
    gridSubject: undefined
  }
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listRuntime(queryParams.value).then(response => {
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
  proxy.download('vqms/avc/runtime/export', { ...queryParams.value }, `avc_runtime_${new Date().getTime()}.xlsx`)
}

getList()
</script>
