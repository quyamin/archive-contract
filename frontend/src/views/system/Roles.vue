<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <h2 class="page-title">角色权限</h2>
        <p class="page-desc">基于 RBAC 模型配置角色菜单与按钮权限。权限变更后，用户重新登录或刷新权限即可生效。</p>
      </div>
      <div class="page-actions">
        <el-button v-if="userStore.hasPerm('system:role:edit')" type="primary" @click="openAdd">
          <el-icon><Plus /></el-icon> 新增角色
        </el-button>
      </div>
    </div>

    <div class="role-grid">
      <div v-for="role in roles" :key="role.id" class="panel-card role-card">
        <div class="role-head">
          <div class="role-avatar" :class="role.roleCode.toLowerCase()">
            <el-icon><Avatar /></el-icon>
          </div>
          <div style="flex: 1; min-width: 0">
            <div class="role-name">
              {{ role.roleName }}
              <el-tag size="small" effect="plain" :type="role.status === 1 ? 'success' : 'info'">
                {{ role.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </div>
            <div class="role-code">{{ role.roleCode }}</div>
          </div>
          <el-dropdown v-if="userStore.hasPerm('system:role:edit')" trigger="click" @command="(cmd) => handleCommand(cmd, role)">
            <el-button link><el-icon><MoreFilled /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit">编辑角色</el-dropdown-item>
                <el-dropdown-item v-if="userStore.hasPerm('system:role:delete')" command="delete" divided>
                  <span style="color: #f05b5b">删除角色</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="role-desc">{{ role.description || '暂无角色说明' }}</div>
        <div class="role-meta">
          <div class="role-meta-item">
            <span class="num">{{ role.userCount || 0 }}</span>
            <span class="label">关联用户</span>
          </div>
          <div class="role-meta-item">
            <span class="num">{{ (role.permissionIds || []).length }}</span>
            <span class="label">权限节点</span>
          </div>
          <div class="role-meta-item">
            <span class="num">{{ role.status === 1 ? '正常' : '停用' }}</span>
            <span class="label">角色状态</span>
          </div>
        </div>
        <div class="role-perms">
          <el-tag
            v-for="code in previewPerms(role)"
            :key="code"
            size="small"
            effect="plain"
            type="info"
          >{{ code }}</el-tag>
          <span v-if="(role.permissionIds || []).length > 4" class="muted more-perm">
            +{{ role.permissionIds.length - 4 }}
          </span>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色权限' : '新增角色'" width="680px" top="5vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="角色编码" prop="roleCode">
              <el-input v-model="form.roleCode" placeholder="如 LEGAL" :disabled="!!form.id" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色名称" prop="roleName">
              <el-input v-model="form.roleName" placeholder="如 法务专员" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色说明">
              <el-input v-model="form.description" type="textarea" :rows="2" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="权限配置">
          <div class="perm-panel">
            <div class="perm-toolbar">
              <span class="muted">勾选菜单与按钮权限，支持半选联动</span>
              <div>
                <el-button link type="primary" size="small" @click="checkAll">全选</el-button>
                <el-button link size="small" @click="clearAll">清空</el-button>
                <el-button link type="primary" size="small" @click="expandAll">展开全部</el-button>
              </div>
            </div>
            <el-tree
              ref="treeRef"
              :data="permissionTree"
              show-checkbox
              node-key="id"
              default-expand-all
              :props="{ label: 'permName', children: 'children' }"
              class="perm-tree"
            >
              <template #default="{ data }">
                <span class="perm-node">
                  <span>{{ data.permName }}</span>
                  <span class="perm-code">{{ data.permCode }}</span>
                  <el-tag v-if="data.permType === 'MENU'" size="small" effect="plain">菜单</el-tag>
                </span>
              </template>
            </el-tree>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存角色</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getRoles, getPermissionTree, saveRole, deleteRole } from '@/api/system'

const userStore = useUserStore()
const roles = ref([])
const permissionTree = ref([])
const leafIds = ref(new Set())
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const treeRef = ref(null)
const form = reactive({
  id: null,
  roleCode: '',
  roleName: '',
  description: '',
  status: 1,
  permissionIds: []
})
const rules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

function collectLeafIds(nodes) {
  nodes.forEach((node) => {
    if (!node.children?.length) leafIds.value.add(node.id)
    else collectLeafIds(node.children)
  })
}

function previewPerms(role) {
  return (role.permissionIds || []).slice(0, 4).map((id) => findPermCode(permissionTree.value, id)).filter(Boolean)
}

function findPermCode(nodes, id) {
  for (const node of nodes) {
    if (node.id === id) return node.permCode
    if (node.children?.length) {
      const found = findPermCode(node.children, id)
      if (found) return found
    }
  }
  return null
}

async function load() {
  roles.value = await getRoles()
}

function openAdd() {
  Object.assign(form, {
    id: null,
    roleCode: '',
    roleName: '',
    description: '',
    status: 1,
    permissionIds: []
  })
  dialogVisible.value = true
  setTimeout(() => treeRef.value?.setCheckedKeys([]), 0)
}

function openEdit(role) {
  Object.assign(form, {
    id: role.id,
    roleCode: role.roleCode,
    roleName: role.roleName,
    description: role.description || '',
    status: role.status,
    permissionIds: [...(role.permissionIds || [])]
  })
  dialogVisible.value = true
  const leafChecks = (role.permissionIds || []).filter((id) => leafIds.value.has(id))
  setTimeout(() => treeRef.value?.setCheckedKeys(leafChecks), 0)
}

function checkAll() {
  treeRef.value?.setCheckedKeys([...leafIds.value])
}

function clearAll() {
  treeRef.value?.setCheckedKeys([])
}

function expandAll() {
  const nodes = treeRef.value?.store?.nodesMap || {}
  Object.values(nodes).forEach((node) => { node.expanded = true })
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const checked = treeRef.value?.getCheckedKeys() || []
  const halfChecked = treeRef.value?.getHalfCheckedKeys() || []
  submitting.value = true
  try {
    await saveRole({
      ...form,
      permissionIds: [...new Set([...checked, ...halfChecked])]
    })
    ElMessage.success(form.id ? '角色权限已更新' : '角色创建成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleCommand(command, role) {
  if (command === 'edit') {
    openEdit(role)
    return
  }
  if (command === 'delete') {
    try {
      await ElMessageBox.confirm(`确认删除角色“${role.roleName}”？`, '删除角色', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      })
      await deleteRole(role.id)
      ElMessage.success('角色已删除')
      load()
    } catch {
      // cancelled
    }
  }
}

async function loadPermissionTree() {
  permissionTree.value = await getPermissionTree()
  leafIds.value = new Set()
  collectLeafIds(permissionTree.value)
}

onMounted(async () => {
  await loadPermissionTree()
  await load()
})
</script>

<style scoped>
.role-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}
.role-card {
  padding: 18px 18px 16px;
  display: flex;
  flex-direction: column;
}
.role-head {
  display: flex;
  align-items: flex-start;
  gap: 11px;
}
.role-avatar {
  width: 40px;
  height: 40px;
  border-radius: 11px;
  background: #edf3ff;
  color: #2f6bff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 19px;
  flex: 0 0 40px;
}
.role-avatar.admin {
  background: #fff0f0;
  color: #f05b5b;
}
.role-avatar.archivist {
  background: #e9f9f1;
  color: #20b26c;
}
.role-avatar.entry {
  background: #fff6e8;
  color: #e6a23c;
}
.role-avatar.viewer {
  background: #f3edff;
  color: #8b5cf6;
}
.role-name {
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
}
.role-code {
  font-size: 10.5px;
  color: #98a2b3;
  margin-top: 4px;
  font-family: "Roboto Mono", monospace;
}
.role-desc {
  font-size: 12px;
  color: #6b778e;
  line-height: 1.7;
  margin: 14px 0;
  min-height: 40px;
}
.role-meta {
  display: flex;
  gap: 18px;
  padding: 11px 0;
  border-top: 1px dashed #edf0f6;
  border-bottom: 1px dashed #edf0f6;
}
.role-meta-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.role-meta-item .num {
  font-size: 16px;
  font-weight: 700;
  color: #1f2a44;
}
.role-meta-item .label {
  font-size: 10.5px;
  color: #8b96ab;
}
.role-perms {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 12px;
}
.more-perm {
  font-size: 10.5px;
  display: inline-flex;
  align-items: center;
}
.perm-panel {
  width: 100%;
  border: 1px solid #e7ebf3;
  border-radius: 9px;
  overflow: hidden;
}
.perm-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f8faff;
  border-bottom: 1px solid #eef1f7;
  font-size: 11.5px;
}
.perm-tree {
  max-height: 340px;
  overflow: auto;
  padding: 8px 6px;
}
.perm-node {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
}
.perm-code {
  font-size: 10.5px;
  color: #a0a9ba;
  font-family: "Roboto Mono", monospace;
}
@media (max-width: 1400px) {
  .role-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 1000px) {
  .role-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 680px) {
  .role-grid {
    grid-template-columns: 1fr;
  }
}
</style>
