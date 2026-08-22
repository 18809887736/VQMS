<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" v-show="showSearch" :inline="true" label-width="80px">
      <el-form-item label="电压等级" prop="vGrade">
        <el-select v-model="queryParams.vGrade" placeholder="电压等级" clearable style="width: 160px">
          <el-option v-for="dict in vqms_v_grade" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="母线" prop="busbarNum">
        <el-select v-model="queryParams.busbarNum" placeholder="母线" clearable filterable style="width: 200px">
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['vqms:threshold:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['vqms:threshold:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['vqms:threshold:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['vqms:threshold:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" prop="thresholdId" width="80" />
      <el-table-column label="母线编号" prop="busbarNum" width="100" />
      <el-table-column label="电压等级" prop="vGrade" width="100">
        <template #default="scope">
          <dict-tag :options="vqms_v_grade" :value="scope.row.vGrade" />
        </template>
      </el-table-column>
      <el-table-column label="口径" prop="criterionType" width="140">
        <template #default="scope">
          <span>{{ scope.row.criterionType === 'AVC' ? 'AVC 控制达标率' : scope.row.criterionType === 'GB' ? '国标±10%' : scope.row.criterionType }}</span>
        </template>
      </el-table-column>
      <el-table-column label="容差(kV)" prop="toleranceV" width="110" />
      <el-table-column label="生效起始日" prop="effectiveFrom" width="130" />
      <el-table-column label="生效结束日" prop="effectiveTo" width="130">
        <template #default="scope">
          <span>{{ scope.row.effectiveTo || '至今' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="修改" placement="top">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['vqms:threshold:edit']"></el-button>
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['vqms:threshold:remove']"></el-button>
          </el-tooltip>
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

    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="thresholdRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="母线" prop="busbarNum">
          <el-select v-model="form.busbarNum" placeholder="请选择母线" filterable>
            <el-option v-for="b in busbarList" :key="b.busbarNum" :label="`${b.busbarName} (${b.busbarNum})`" :value="b.busbarNum" />
          </el-select>
        </el-form-item>
        <el-form-item label="口径" prop="criterionType">
          <el-select v-model="form.criterionType" placeholder="请选择口径">
            <el-option label="AVC 控制达标率" value="AVC" />
            <el-option label="国标±10%" value="GB" />
          </el-select>
        </el-form-item>
        <el-form-item label="容差(kV)" prop="toleranceV">
          <el-input-number v-model="form.toleranceV" :precision="3" :step="0.1" controls-position="right" />
        </el-form-item>
        <el-form-item label="生效起始日" prop="effectiveFrom">
          <el-date-picker v-model="form.effectiveFrom" type="date" value-format="YYYY-MM-DD" placeholder="选择生效起始日" />
        </el-form-item>
        <el-form-item label="生效结束日" prop="effectiveTo">
          <el-date-picker v-model="form.effectiveTo" type="date" value-format="YYYY-MM-DD" placeholder="留空=至今有效" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="VqmsThreshold">
import { listThreshold, getThreshold, addThreshold, updateThreshold, delThreshold } from '@/api/vqms/threshold'
import { listBusbar } from '@/api/vqms/busbar'

const { proxy } = getCurrentInstance()
const { vqms_v_grade } = proxy.useDict('vqms_v_grade')

// 母线下拉走后端 /vqms/vqms_busbar/list；类型归一到字符串与字典编码对齐
const busbarList = ref([])
const busbarOptions = computed(() =>
  queryParams.value.vGrade ? busbarList.value.filter(b => b.vGrade === queryParams.value.vGrade) : busbarList.value
)
onMounted(() => {
  listBusbar().then(response => {
    busbarList.value = (response.rows || []).map(b => ({
      busbarNum: String(b.busbarNum),
      busbarName: b.busbarName,
      vGrade: b.vGrade == null ? undefined : String(b.vGrade)
    }))
  })
})

const dataList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    vGrade: undefined,
    busbarNum: undefined
  },
  rules: {
    busbarNum: [{ required: true, message: '母线编号不能为空', trigger: 'blur' }],
    criterionType: [{ required: true, message: '口径不能为空', trigger: 'change' }],
    effectiveFrom: [{ required: true, message: '生效起始日不能为空', trigger: 'change' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listThreshold(queryParams.value).then(response => {
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

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增母线阈值'
}

function handleUpdate(row) {
  const id = row.thresholdId || ids.value[0]
  getThreshold(id).then(response => {
    Object.assign(form.value, response.data)
    // 下拉 option 值为字符串，后端回填是 number——归一避免 el-option 严格匹配失配显示裸编号
    if (form.value.busbarNum != null) {
      form.value.busbarNum = String(form.value.busbarNum)
    }
    open.value = true
    title.value = '修改母线阈值'
  }).catch(() => {})
}

function submitForm() {
  proxy.$refs['thresholdRef'].validate(valid => {
    if (!valid) return
    if (form.value.thresholdId != null) {
      updateThreshold(form.value).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        open.value = false
        getList()
      }).catch(() => {})
    } else {
      addThreshold(form.value).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      }).catch(() => {})
    }
  })
}

function handleDelete(row) {
  const delIds = row.thresholdId || ids.value
  proxy.$modal.confirm('是否确认删除母线阈值编号为"' + delIds + '"的数据项?').then(function () {
    return delThreshold(delIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('vqms/threshold/export', { ...queryParams.value }, `threshold_${new Date().getTime()}.xlsx`)
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.thresholdId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function reset() {
  form.value = {
    thresholdId: undefined,
    busbarNum: undefined,
    criterionType: undefined,
    toleranceV: undefined,
    effectiveFrom: undefined,
    effectiveTo: undefined
  }
  proxy.resetForm('thresholdRef')
}

function cancel() {
  open.value = false
  reset()
}

getList()
</script>
