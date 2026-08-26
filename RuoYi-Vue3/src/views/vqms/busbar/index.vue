<template>
  <div class="app-container">
    <!-- 搜索区（客户端过滤：母线台账为小表，全量加载） -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="母线名称" prop="busbarName">
        <el-input v-model="queryParams.busbarName" placeholder="名称模糊过滤" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="电压等级" prop="vGrade">
        <el-select v-model="queryParams.vGrade" placeholder="电压等级" clearable style="width: 160px">
          <el-option v-for="dict in vqms_v_grade" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px">
          <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['vqms:vqms_busbar:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="filteredList">
      <el-table-column label="母线号" prop="busbarNum" width="90" />
      <el-table-column label="母线名称" prop="busbarName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="电压等级" prop="vGrade" width="110">
        <template #default="scope">
          <dict-tag :options="vqms_v_grade" :value="scope.row.vGrade" />
        </template>
      </el-table-column>
      <el-table-column label="所属组" prop="groupNum" width="90" />
      <el-table-column label="额定电压(kV)" prop="nominalKv" width="120" />
      <el-table-column label="实时电压点" prop="realtimeYcNum" width="110" />
      <el-table-column label="状态" prop="status" width="90">
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['vqms:vqms_busbar:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['vqms:vqms_busbar:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/修改对话框 -->
    <el-dialog :title="title" v-model="open" width="560px" append-to-body>
      <el-form ref="busbarRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="母线号" prop="busbarNum">
          <el-input-number v-model="form.busbarNum" :min="0" controls-position="right" :disabled="form._edit" style="width: 200px" />
          <span style="margin-left: 8px; color: #909399" v-if="!form._edit">主键，创建后不可改</span>
        </el-form-item>
        <el-form-item label="母线名称" prop="busbarName">
          <el-input v-model="form.busbarName" placeholder="如：220kV 东母线" style="width: 260px" />
        </el-form-item>
        <el-form-item label="电压等级" prop="vGrade">
          <el-select v-model="form.vGrade" style="width: 200px">
            <el-option v-for="dict in vqms_v_grade" :key="dict.value" :label="dict.label" :value="Number(dict.value)" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属组号" prop="groupNum">
          <el-input-number v-model="form.groupNum" :min="0" controls-position="right" style="width: 200px" />
          <span style="margin-left: 8px; color: #909399">须在 vqms_busbar_group 中存在</span>
        </el-form-item>
        <el-form-item label="额定电压(kV)" prop="nominalKv">
          <el-input-number v-model="form.nominalKv" :min="0" :precision="3" :step="0.5" controls-position="right" style="width: 200px" />
        </el-form-item>
        <el-form-item label="实时电压点" prop="realtimeYcNum">
          <el-input-number v-model="form.realtimeYcNum" :min="0" controls-position="right" style="width: 200px" />
          <span style="margin-left: 8px; color: #909399">yc 点位号，可空</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="VqmsBusbar">
import { listBusbar, getBusbar, addBusbar, updateBusbar, delBusbar, thresholdCount } from "@/api/vqms/busbar";

const { proxy } = getCurrentInstance();
const { vqms_v_grade, sys_normal_disable } = proxy.useDict("vqms_v_grade", "sys_normal_disable");

const loading = ref(false);
const showSearch = ref(true);
const busbarList = ref([]);
const open = ref(false);
const title = ref("");

const queryParams = reactive({ busbarName: "", vGrade: undefined, status: undefined });

const filteredList = computed(() => {
  return busbarList.value.filter(b => {
    if (queryParams.busbarName && !(b.busbarName || "").includes(queryParams.busbarName)) return false;
    if (queryParams.vGrade !== undefined && queryParams.vGrade !== null && queryParams.vGrade !== "" && String(b.vGrade) !== String(queryParams.vGrade)) return false;
    if (queryParams.status && b.status !== queryParams.status) return false;
    return true;
  });
});

const form = reactive({});
const rules = {
  busbarNum: [{ required: true, message: "母线号不能为空", trigger: "blur" }],
  busbarName: [{ required: true, message: "母线名称不能为空", trigger: "blur" }],
  vGrade: [{ required: true, message: "电压等级不能为空", trigger: "change" }],
  nominalKv: [{ required: true, message: "额定电压不能为空", trigger: "blur" }]
};

function getList() {
  loading.value = true;
  listBusbar().then(res => {
    busbarList.value = res.rows || [];
    loading.value = false;
  });
}

function handleQuery() { /* filteredList 为计算属性，输入即生效 */ }
function resetQuery() {
  queryParams.busbarName = "";
  queryParams.vGrade = undefined;
  queryParams.status = undefined;
}

function reset() {
  form.busbarNum = undefined;
  form.busbarName = undefined;
  form.vGrade = undefined;
  form.groupNum = undefined;
  form.nominalKv = undefined;
  form.realtimeYcNum = undefined;
  form.status = "0";
  form.remark = undefined;
  form._edit = false;
  proxy.resetForm("busbarRef");
}

function handleAdd() {
  reset();
  open.value = true;
  title.value = "新增母线";
}

function handleUpdate(row) {
  reset();
  form.busbarNum = row.busbarNum;
  form.busbarName = row.busbarName;
  form.vGrade = row.vGrade;
  form.groupNum = row.groupNum;
  form.nominalKv = row.nominalKv;
  form.realtimeYcNum = row.realtimeYcNum;
  form.status = row.status;
  form.remark = row.remark;
  form._edit = true;
  open.value = true;
  title.value = "修改母线";
}

function cancel() {
  open.value = false;
  reset();
}

function submitForm() {
  proxy.$refs["busbarRef"].validate(valid => {
    if (!valid) return;
    if (form._edit) {
      updateBusbar(form).then(() => {
        proxy.$modal.msgSuccess("修改成功");
        open.value = false;
        getList();
      });
    } else {
      addBusbar(form).then(() => {
        proxy.$modal.msgSuccess("新增成功");
        open.value = false;
        getList();
      });
    }
  });
}

function handleDelete(row) {
  thresholdCount(row.busbarNum).then(res => {
    const cnt = Number(res.data) || 0;
    if (cnt > 0) {
      proxy.$modal.msgWarning(`无法删除「${row.busbarName}（${row.busbarNum}）」：有 ${cnt} 条阈值配置引用该母线，请先在阈值管理中清理`);
      return;
    }
    proxy.$modal.confirm(`确认删除母线「${row.busbarName}（${row.busbarNum}）」？`).then(() => {
      return delBusbar(row.busbarNum);
    }).then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    }).catch(() => {});
  }).catch(() => {});
}

getList();
</script>
