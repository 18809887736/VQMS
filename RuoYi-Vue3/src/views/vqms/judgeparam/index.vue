<template>
  <div class="app-container">
    <el-alert type="info" :closable="false" style="margin-bottom: 12px">
      两档窗口无缝拼接：[1, t_fast] ∪ [t_fast+1, 5] = [1, 5]——任何 t_fast 整定值下无空档、无重叠。
      <span style="color:#909399">t_econ 与分档阈值为锁定行（政策值/写死），不可修改。</span>
    </el-alert>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['vqms:judgeparam:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="参数键" prop="paramKey" width="170" />
      <el-table-column label="名称" prop="name" width="170" />
      <el-table-column label="值" prop="paramValue" width="80">
        <template #default="scope">
          <span>{{ scope.row.paramValue }}</span>
          <el-tag v-if="isLocked(scope.row)" size="small" type="info" style="margin-left: 6px">锁定</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="值域" width="110">
        <template #default="scope">[{{ scope.row.valueMin }}, {{ scope.row.valueMax }}]</template>
      </el-table-column>
      <el-table-column label="说明" prop="description" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="140" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip :content="isLocked(scope.row) ? '锁定行不可修改' : '修改'" placement="top">
            <span>
              <el-button link type="primary" icon="Edit" :disabled="isLocked(scope.row)" @click="handleUpdate(scope.row)" v-hasPermi="['vqms:judgeparam:edit']"></el-button>
            </span>
          </el-tooltip>
          <el-tooltip :content="isDeletable(scope.row) ? '删除' : '判定必需参数不可删除'" placement="top">
            <span>
              <el-button link type="primary" icon="Delete" :disabled="!isDeletable(scope.row)" @click="handleDelete(scope.row)" v-hasPermi="['vqms:judgeparam:remove']"></el-button>
            </span>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="title" v-model="open" width="480px" append-to-body>
      <el-form ref="judgeParamRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="参数键" prop="paramKey">
          <el-input v-model="form.paramKey" :disabled="form.paramId != null" placeholder="如 t_fast" />
        </el-form-item>
        <el-form-item label="参数名称" prop="name">
          <el-input v-model="form.name" placeholder="参数名称" />
        </el-form-item>
        <el-form-item :label="form.paramId != null ? '参数值' : '参数值(分钟)'" prop="paramValue">
          <el-input-number v-model="form.paramValue" :step="1" step-strictly controls-position="right" />
          <span v-if="form.valueMin != null" style="margin-left: 8px; color: #909399">值域 [{{ form.valueMin }}, {{ form.valueMax }}]</span>
        </el-form-item>
        <el-form-item label="值域下限" prop="valueMin" v-if="form.paramId == null">
          <el-input-number v-model="form.valueMin" controls-position="right" />
        </el-form-item>
        <el-form-item label="值域上限" prop="valueMax" v-if="form.paramId == null">
          <el-input-number v-model="form.valueMax" controls-position="right" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="VqmsJudgeParam">
import { listJudgeParam, addJudgeParam, updateJudgeParam, delJudgeParam } from '@/api/vqms/judgeParam'

const { proxy } = getCurrentInstance()

const dataList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const title = ref('')

// 锁定/必需语义（v5.0 §6.2.5）：t_econ 写死、分档阈值政策值；四行种子全为判定必需
const LOCKED_KEYS = ['t_econ', 'tier_threshold_fast', 'tier_threshold_econ']
const ESSENTIAL_KEYS = [...LOCKED_KEYS, 't_fast']

const data = reactive({
  form: {},
  rules: {
    paramKey: [{ required: true, message: '参数键不能为空', trigger: 'blur' }],
    name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
    paramValue: [{ required: true, message: '参数值不能为空', trigger: 'blur' }]
  }
})
const { form, rules } = toRefs(data)

function isLocked(row) {
  return LOCKED_KEYS.includes(row.paramKey)
}

function isDeletable(row) {
  return !ESSENTIAL_KEYS.includes(row.paramKey)
}

function getList() {
  loading.value = true
  listJudgeParam().then(response => {
    dataList.value = response.rows
  }).finally(() => {
    loading.value = false
  })
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增判定参数'
}

function handleUpdate(row) {
  reset()
  form.value = { ...row }
  open.value = true
  title.value = '修改判定参数'
}

function submitForm() {
  proxy.$refs['judgeParamRef'].validate(valid => {
    if (!valid) return
    if (form.value.paramId != null) {
      updateJudgeParam(form.value).then(() => {
        proxy.$modal.msgSuccess('修改成功（判定侧下次读取即生效）')
        open.value = false
        getList()
      }).catch(() => {})
    } else {
      addJudgeParam(form.value).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        open.value = false
        getList()
      }).catch(() => {})
    }
  })
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除参数"' + row.paramKey + '"?').then(function () {
    return delJudgeParam(row.paramId)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function reset() {
  form.value = {
    paramId: undefined,
    paramKey: undefined,
    name: undefined,
    paramValue: undefined,
    valueMin: undefined,
    valueMax: undefined,
    description: undefined
  }
  proxy.resetForm('judgeParamRef')
}

function cancel() {
  open.value = false
  reset()
}

getList()
</script>
