<template>
  <div class="app-container">
    <el-alert type="info" :closable="false" style="margin-bottom: 12px">
      调节合格率 = 快速性档 + 经济性档，<b>两档平行考察、互不隶属</b>，各出合格率与罚款（相加得总罚款）。
      <span style="color:#909399">（口径以 AVC考核核心算法_草稿 为准，定稿中）</span>
    </el-alert>

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
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['vqms:avc:regulation:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="统计月份" prop="statMonth" width="110" />
      <el-table-column label="并网主体" prop="gridSubject" :show-overflow-tooltip="true" />
      <el-table-column label="发令次数" prop="cmdCount" width="100" />
      <el-table-column label="快速性合格率(%)" prop="fastRate" width="150" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.fastRate >= 100 ? 'success' : 'danger'">{{ scope.row.fastRate }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="经济性合格率(%)" prop="econRate" width="150" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.econRate >= 100 ? 'success' : 'danger'">{{ scope.row.econRate }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="快速性罚款" prop="fastPenalty" width="110" />
      <el-table-column label="经济性罚款" prop="econPenalty" width="110" />
      <el-table-column label="总罚款" prop="totalPenalty" width="110">
        <template #default="scope">
          <span style="color:#b91c1c;font-weight:600">{{ scope.row.totalPenalty }}</span>
        </template>
      </el-table-column>
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

<script setup name="VqmsAvcRegulation">
import { listRegulation } from '@/api/vqms/avcRegulation'

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
  listRegulation(queryParams.value).then(response => {
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
  proxy.download('vqms/avc-regulation/export', { ...queryParams.value }, `avc_regulation_${new Date().getTime()}.xlsx`)
}

getList()
</script>
