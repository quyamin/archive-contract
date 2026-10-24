<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-desc">维护企业档案系统账号、所属部门与角色，实现岗位级权限与数据范围隔离。</p>
      </div>
      <div class="page-actions">
        <el-button v-if="userStore.hasPerm('system:user:add')" type="primary" @click="openAdd">
          <el-icon><Plus /></el-icon> 新增用户
        </el-button>
      </div>
    </div>

    <div class="panel-card filter-card">
      <el-form :model="query" label-width="70px" inline @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="账号 / 姓名 / 手机号" clearable style="width: 220px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="query.deptId" placeholder="全部部门" clearable style="width: 148px">
            <el-option v-for="item in deptOptions" :key="item.id" :label="item.deptName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label-width="0">
          <el-button type="primary" @click="search"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="panel-card table-card">
      <el-table v-loading="loading" :data="page.records" stripe>
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-avatar-sm" :class="{ disabled: row.status !== 1 }">{{ (row.realName || '?').slice(-1) }}</div>
              <div>
                <div class="user-name">{{ row.realName }}</div>
                <div class="muted">{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="deptName" label="所属部门" width="120" />
        <el-table-column label="角色" min-width="170">
          <template #default="{ row }">
            <el-tag v-for="name in row.roleNames" :key="name" size="small" effect="plain" style="margin-right: 4px; margin-bottom: 2px">
              {{ name }}
            </el-tag>
            <span v-if="!row.roleNames?.length" class="muted">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="122">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="!userStore.hasPerm('system:user:edit')"
              @change="toggleStatus(row, $event)"
            />
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="146">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPerm('system:user:edit')" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="userStore.hasPerm('system:user:edit')" link type="warning" size="small" @click="openReset(row)">重置密码</el-button>
            <el-button v-if="userStore.hasPerm('system:user:delete')" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="page.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="600px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="登录账号" prop="username">
              <el-input v-model="form.username" placeholder="请输入登录账号" :disabled="!!form.id" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
          <el-col v-if="!form.id" :span="12">
            <el-form-item label="初始密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="不少于6位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属部门" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择部门" filterable style="width: 100%">
                <el-option v-for="item in deptOptions" :key="item.id" :label="item.deptName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电子邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色">
              <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
                <el-option v-for="item in roleOptions" :key="item.id" :label="item.roleName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="禁用" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="重置密码" width="420px">
      <el-form label-position="top">
        <el-form-item label="用户">
          <el-input :model-value="`${resetUser.realName}（${resetUser.username}）`" disabled />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password placeholder="不少于6位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getUsers,
  addUser,
  updateUser,
  resetUserPassword,
  changeUserStatus,
  deleteUser,
  getOptionDepts,
  getOptionRoles
} from '@/api/system'
import { formatDateTime } from '@/utils/format'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const page = ref({ records: [], total: 0 })
const deptOptions = ref([])
const roleOptions = ref([])
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', deptId: null, status: null })

const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  deptId: null,
  status: 1,
  remark: '',
  roleIds: []
})
const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, message: '密码不能少于6位', trigger: 'blur' }
  ],
  deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }]
}

const resetVisible = ref(false)
const resetUser = ref({})
const newPassword = ref('')
const confirmPassword = ref('')

async function load() {
  loading.value = true
  try {
    page.value = await getUsers(query)
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function reset() {
  query.keyword = ''
  query.deptId = null
  query.status = null
  query.pageNum = 1
  load()
}

function resetForm() {
  Object.assign(form, {
    id: null,
    username: '',
    password: '',
    realName: '',
    phone: '',
    email: '',
    deptId: null,
    status: 1,
    remark: '',
    roleIds: []
  })
  formRef.value?.clearValidate()
}

function openAdd() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    realName: row.realName,
    phone: row.phone || '',
    email: row.email || '',
    deptId: row.deptId,
    status: row.status,
    remark: row.remark || '',
    roleIds: [...(row.roleIds || [])]
  })
  dialogVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload = { ...form }
    if (payload.id) {
      delete payload.password
      await updateUser(payload)
    } else {
      await addUser(payload)
    }
    ElMessage.success(payload.id ? '用户信息已更新' : '用户创建成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row, value) {
  const previous = value === 1 ? 0 : 1
  try {
    await changeUserStatus(row.id, value)
    ElMessage.success(value === 1 ? '账号已启用' : '账号已禁用')
  } catch {
    row.status = previous
  }
}

function openReset(row) {
  resetUser.value = row
  newPassword.value = ''
  confirmPassword.value = ''
  resetVisible.value = true
}

async function submitReset() {
  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage.warning('新密码不能少于6位')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  submitting.value = true
  try {
    await resetUserPassword(resetUser.value.id, newPassword.value)
    ElMessage.success('密码重置成功')
    resetVisible.value = false
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除用户“${row.realName}（${row.username}）”？`, '删除用户', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteUser(row.id)
    ElMessage.success('用户已删除')
    load()
  } catch {
    // cancelled
  }
}

async function loadOptions() {
  const [depts, roles] = await Promise.all([getOptionDepts(), getOptionRoles()])
  deptOptions.value = depts || []
  roleOptions.value = roles || []
}

onMounted(() => {
  loadOptions()
  load()
})
</script>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-avatar-sm {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #5d8cff, #2f6bff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex: 0 0 34px;
}
.user-avatar-sm.disabled {
  background: #c8cfdb;
}
.user-name {
  font-size: 12.5px;
  font-weight: 600;
}
.user-cell .muted {
  font-size: 11px;
  margin-top: 2px;
}
</style>
