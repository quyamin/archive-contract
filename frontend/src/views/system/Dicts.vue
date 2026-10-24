<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <h2 class="page-title">字典配置</h2>
        <p class="page-desc">维护合同类型、档案密级、流程状态等业务字典，字典项将同步应用于录入、筛选与统计。</p>
      </div>
      <div class="page-actions">
        <el-button v-if="userStore.hasPerm('system:dict:edit')" type="primary" @click="openDictDialog()">
          <el-icon><Plus /></el-icon> 新增字典
        </el-button>
      </div>
    </div>

    <div class="dict-layout">
      <div class="panel-card dict-side">
        <div class="dict-side-head">
          <div class="table-toolbar-title">字典分类</div>
          <span class="muted">{{ dicts.length }} 类</span>
        </div>
        <div class="dict-list">
          <div
            v-for="item in dicts"
            :key="item.id"
            class="dict-item"
            :class="{ active: current?.id === item.id }"
            @click="selectDict(item)"
          >
            <div class="dict-item-main">
              <div class="dict-name">
                {{ item.dictName }}
                <el-tag v-if="item.status !== 1" size="small" type="info" effect="plain">停用</el-tag>
              </div>
              <div class="dict-code">{{ item.dictCode }}</div>
            </div>
            <div class="dict-item-count">{{ (item.items || []).length }}</div>
          </div>
          <el-empty v-if="!dicts.length" description="暂无字典" :image-size="70" />
        </div>
      </div>

      <div class="panel-card dict-main">
        <template v-if="current">
          <div class="dict-main-head">
            <div>
              <div class="dict-main-title">{{ current.dictName }}</div>
              <div class="muted">
                编码：{{ current.dictCode }} · {{ current.description || '暂无说明' }}
              </div>
            </div>
            <div class="table-toolbar-actions">
              <el-button v-if="userStore.hasPerm('system:dict:edit')" size="small" @click="openDictDialog(current)">
                <el-icon><Edit /></el-icon> 编辑字典
              </el-button>
              <el-button v-if="userStore.hasPerm('system:dict:delete')" size="small" type="danger" plain @click="handleDeleteDict(current)">
                <el-icon><Delete /></el-icon> 删除
              </el-button>
              <el-button v-if="userStore.hasPerm('system:dict:edit')" size="small" type="primary" @click="openItemDialog()">
                <el-icon><Plus /></el-icon> 新增字典项
              </el-button>
            </div>
          </div>

          <el-table :data="current.items || []" size="small" stripe>
            <el-table-column type="index" label="序号" width="64" align="center" />
            <el-table-column prop="itemLabel" label="字典标签" min-width="170" />
            <el-table-column prop="itemValue" label="字典值" min-width="170">
              <template #default="{ row }"><span class="dict-value">{{ row.itemValue }}</span></template>
            </el-table-column>
            <el-table-column prop="sort" label="排序" width="80" align="center" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="plain">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" align="center">
              <template #default="{ row }">
                <el-button v-if="userStore.hasPerm('system:dict:edit')" link type="primary" size="small" @click="openItemDialog(row)">编辑</el-button>
                <el-button v-if="userStore.hasPerm('system:dict:delete')" link type="danger" size="small" @click="handleDeleteItem(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
        <el-empty v-else description="请选择左侧字典分类" />
      </div>
    </div>

    <el-dialog v-model="dictDialogVisible" :title="dictForm.id ? '编辑字典' : '新增字典'" width="480px">
      <el-form ref="dictFormRef" :model="dictForm" :rules="dictRules" label-width="86px">
        <el-form-item label="字典编码" prop="dictCode">
          <el-input v-model="dictForm.dictCode" placeholder="如 contract_type" :disabled="!!dictForm.id" />
        </el-form-item>
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="dictForm.dictName" placeholder="如 合同类型" />
        </el-form-item>
        <el-form-item label="字典说明">
          <el-input v-model="dictForm.description" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="dictForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dictDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDict">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemDialogVisible" :title="itemForm.id ? '编辑字典项' : '新增字典项'" width="460px">
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="86px">
        <el-form-item label="所属字典">
          <el-input :model-value="current?.dictName" disabled />
        </el-form-item>
        <el-form-item label="字典标签" prop="itemLabel">
          <el-input v-model="itemForm.itemLabel" placeholder="页面上显示的名称" />
        </el-form-item>
        <el-form-item label="字典值" prop="itemValue">
          <el-input v-model="itemForm.itemValue" placeholder="系统内部存储值，如 PURCHASE" :disabled="!!itemForm.id" />
        </el-form-item>
        <el-form-item label="显示排序">
          <el-input-number v-model="itemForm.sort" :min="0" :max="999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="itemForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getDicts, saveDict, deleteDict, saveDictItem, deleteDictItem } from '@/api/system'

const userStore = useUserStore()
const dicts = ref([])
const current = ref(null)
const submitting = ref(false)

const dictDialogVisible = ref(false)
const dictFormRef = ref(null)
const dictForm = reactive({ id: null, dictCode: '', dictName: '', description: '', status: 1 })
const dictRules = {
  dictCode: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }]
}

const itemDialogVisible = ref(false)
const itemFormRef = ref(null)
const itemForm = reactive({ id: null, dictId: null, itemLabel: '', itemValue: '', sort: 1, status: 1 })
const itemRules = {
  itemLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  itemValue: [{ required: true, message: '请输入字典值', trigger: 'blur' }]
}

async function load(keepId) {
  const selectedId = keepId || current.value?.id
  dicts.value = await getDicts()
  current.value = dicts.value.find((item) => item.id === selectedId) || dicts.value[0] || null
}

function selectDict(item) {
  current.value = item
}

function openDictDialog(row) {
  Object.assign(dictForm, row
    ? { id: row.id, dictCode: row.dictCode, dictName: row.dictName, description: row.description || '', status: row.status }
    : { id: null, dictCode: '', dictName: '', description: '', status: 1 })
  dictDialogVisible.value = true
  dictFormRef.value?.clearValidate()
}

async function submitDict() {
  const valid = await dictFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const code = dictForm.dictCode
    await saveDict({ ...dictForm })
    ElMessage.success(dictForm.id ? '字典已更新' : '字典创建成功')
    dictDialogVisible.value = false
    await load()
    current.value = dicts.value.find((item) => item.dictCode === code) || current.value
  } finally {
    submitting.value = false
  }
}

async function handleDeleteDict(row) {
  try {
    await ElMessageBox.confirm(`确认删除字典“${row.dictName}”及其全部字典项？`, '删除字典', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteDict(row.id)
    ElMessage.success('字典已删除')
    current.value = null
    load()
  } catch {
    // cancelled
  }
}

function openItemDialog(row) {
  Object.assign(itemForm, row
    ? {
        id: row.id,
        dictId: current.value.id,
        itemLabel: row.itemLabel,
        itemValue: row.itemValue,
        sort: row.sort || 0,
        status: row.status
      }
    : {
        id: null,
        dictId: current.value.id,
        itemLabel: '',
        itemValue: '',
        sort: (current.value.items?.length || 0) + 1,
        status: 1
      })
  itemDialogVisible.value = true
  itemFormRef.value?.clearValidate()
}

async function submitItem() {
  const valid = await itemFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await saveDictItem({ ...itemForm })
    ElMessage.success(itemForm.id ? '字典项已更新' : '字典项创建成功')
    itemDialogVisible.value = false
    await load(current.value.id)
  } finally {
    submitting.value = false
  }
}

async function handleDeleteItem(row) {
  try {
    await ElMessageBox.confirm(`确认删除字典项“${row.itemLabel}”？`, '删除字典项', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteDictItem(row.id)
    ElMessage.success('字典项已删除')
    await load(current.value.id)
  } catch {
    // cancelled
  }
}

onMounted(() => load())
</script>

<style scoped>
.dict-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}
.dict-side {
  padding: 16px;
}
.dict-side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.dict-list {
  max-height: 620px;
  overflow: auto;
}
.dict-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 11px 12px;
  border-radius: 9px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.16s;
  margin-bottom: 4px;
}
.dict-item:hover {
  background: #f7f9ff;
}
.dict-item.active {
  background: #edf3ff;
  border-color: #c9d8ff;
}
.dict-name {
  font-size: 12.5px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 5px;
}
.dict-code {
  font-size: 10.5px;
  color: #98a2b3;
  margin-top: 3px;
  font-family: "Roboto Mono", monospace;
}
.dict-item-count {
  min-width: 24px;
  height: 22px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid #e7ebf3;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10.5px;
  color: #526079;
}
.dict-item.active .dict-item-count {
  background: #2f6bff;
  border-color: #2f6bff;
  color: #fff;
}
.dict-main {
  padding: 16px 18px;
  min-height: 420px;
}
.dict-main-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding-bottom: 14px;
  margin-bottom: 12px;
  border-bottom: 1px solid #f0f2f7;
}
.dict-main-title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 4px;
}
.dict-value {
  font-family: "Roboto Mono", monospace;
  font-size: 11.5px;
  color: #2f6bff;
  background: #f2f7ff;
  padding: 2px 6px;
  border-radius: 4px;
}
@media (max-width: 900px) {
  .dict-layout {
    grid-template-columns: 1fr;
  }
}
</style>
