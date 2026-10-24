<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <h2 class="page-title">组织机构</h2>
        <p class="page-desc">维护企业部门层级结构。部门用于合同归属、数据权限范围与统计分析维度。</p>
      </div>
      <div class="page-actions">
        <el-button v-if="userStore.hasPerm('system:dept:edit')" type="primary" @click="openAdd(null)">
          <el-icon><Plus /></el-icon> 新增部门
        </el-button>
        <el-button :loading="loading" @click="load"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </div>
    </div>

    <div class="panel-card table-card">
      <el-table
        v-loading="loading"
        :data="tree"
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="deptName" label="部门名称" min-width="230">
          <template #default="{ row }">
            <span class="dept-name">
              <el-icon><OfficeBuilding /></el-icon> {{ row.deptName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="deptCode" label="部门编码" width="130">
          <template #default="{ row }"><span class="dept-code">{{ row.deptCode }}</span></template>
        </el-table-column>
        <el-table-column prop="leader" label="负责人" width="110">
          <template #default="{ row }">{{ row.leader || '—' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" width="140">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="plain">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" align="center">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPerm('system:dept:edit')" link type="primary" size="small" @click="openAdd(row)">新增下级</el-button>
            <el-button v-if="userStore.hasPerm('system:dept:edit')" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="userStore.hasPerm('system:dept:delete')" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑部门' : '新增部门'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="tree"
            :props="{ label: 'deptName', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="不选则为一级部门"
            style="width: 100%"
            :render-after-expand="false"
          />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="部门编码" prop="deptCode">
              <el-input v-model="form.deptCode" placeholder="如 D008" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门名称" prop="deptName">
              <el-input v-model="form.deptName" placeholder="请输入部门名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-input v-model="form.leader" placeholder="负责人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序">
              <el-input-number v-model="form.sort" :min="0" :max="999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="停用" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getDeptTree, saveDept, deleteDept } from '@/api/system'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const tree = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  parentId: null,
  deptCode: '',
  deptName: '',
  leader: '',
  phone: '',
  sort: 1,
  status: 1
})
const rules = {
  deptCode: [{ required: true, message: '请输入部门编码', trigger: 'blur' }],
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    tree.value = await getDeptTree()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: null,
    parentId: null,
    deptCode: '',
    deptName: '',
    leader: '',
    phone: '',
    sort: 1,
    status: 1
  })
  formRef.value?.clearValidate()
}

function openAdd(parent) {
  resetForm()
  form.parentId = parent?.id || null
  form.sort = parent ? (parent.children?.length || 0) + 1 : tree.value.length + 1
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    parentId: row.parentId || null,
    deptCode: row.deptCode,
    deptName: row.deptName,
    leader: row.leader || '',
    phone: row.phone || '',
    sort: row.sort || 0,
    status: row.status
  })
  dialogVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await saveDept({ ...form })
    ElMessage.success(form.id ? '部门信息已更新' : '部门创建成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  if (row.children?.length) {
    ElMessage.warning('该部门存在下级部门，请先处理下级部门')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除部门“${row.deptName}”？`, '删除部门', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteDept(row.id)
    ElMessage.success('部门已删除')
    load()
  } catch {
    // cancelled
  }
}

onMounted(load)
</script>

<style scoped>
.dept-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}
.dept-name .el-icon {
  color: #2f6bff;
}
.dept-code {
  font-family: "Roboto Mono", monospace;
  font-size: 11.5px;
  color: #8b96ab;
}
</style>
