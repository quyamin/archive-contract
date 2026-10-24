<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <h2 class="page-title">合同台账</h2>
        <p class="page-desc">集中管理企业合同从录入到归档的全过程，支持附件检索、批量导入与台账导出。</p>
      </div>
      <div class="page-actions">
        <el-button v-if="userStore.hasPerm('contract:import')" @click="handleTemplate">
          <el-icon><Download /></el-icon> 导入模板
        </el-button>
        <el-button v-if="userStore.hasPerm('contract:import')" @click="importVisible = true">
          <el-icon><Upload /></el-icon> 批量导入
        </el-button>
        <el-button v-if="userStore.hasPerm('contract:export')" :loading="exporting" @click="handleExport">
          <el-icon><DocumentCopy /></el-icon> 导出台账
        </el-button>
        <el-button v-if="userStore.hasPerm('contract:add')" type="primary" @click="$router.push('/contracts/new')">
          <el-icon><Plus /></el-icon> 录入合同
        </el-button>
      </div>
    </div>

    <div class="panel-card filter-card">
      <el-form :model="query" label-width="72px" inline @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="合同编号 / 名称 / 签约方 / 档案号" clearable style="width: 250px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="合同类型">
          <el-select v-model="query.contractType" placeholder="全部类型" clearable style="width: 132px">
            <el-option v-for="item in typeOptions" :key="item.itemValue" :label="item.itemLabel" :value="item.itemValue" />
          </el-select>
        </el-form-item>
        <el-form-item label="归属部门">
          <el-select v-model="query.deptId" placeholder="全部部门" clearable style="width: 132px">
            <el-option v-for="item in deptOptions" :key="item.id" :label="item.deptName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="签订日期">
          <el-date-picker
            v-model="signDateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 246px"
          />
        </el-form-item>
        <el-form-item label="合同金额">
          <div class="range-inputs">
            <el-input-number v-model="query.amountMin" :min="0" :controls="false" placeholder="最低" style="width: 105px" />
            <span class="muted">—</span>
            <el-input-number v-model="query.amountMax" :min="0" :controls="false" placeholder="最高" style="width: 105px" />
          </div>
        </el-form-item>
        <el-form-item label="附件内容">
          <el-input v-model="query.attachmentKeyword" placeholder="附件名称或全文关键字" clearable style="width: 210px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="到期预警">
          <el-checkbox v-model="query.expiringSoon" border size="small">仅看临近到期</el-checkbox>
        </el-form-item>
        <el-form-item label-width="0">
          <el-button type="primary" @click="search"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="panel-card table-card">
      <div class="status-tabs">
        <el-radio-group v-model="query.status" size="small" @change="search">
          <el-radio-button value="">全部合同</el-radio-button>
          <el-radio-button value="DRAFT">待归档</el-radio-button>
          <el-radio-button value="PENDING">审核中</el-radio-button>
          <el-radio-button value="REJECTED">已驳回</el-radio-button>
          <el-radio-button value="ARCHIVED">已归档</el-radio-button>
          <el-radio-button value="EXPIRED">已到期</el-radio-button>
        </el-radio-group>
        <div class="table-toolbar-actions">
          <span class="muted">共 {{ page.total }} 条记录</span>
          <el-button circle size="small" :loading="loading" @click="load"><el-icon><Refresh /></el-icon></el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="page.records" stripe>
        <el-table-column prop="contractNo" label="合同编号" width="112" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="link-text" @click="view(row)">{{ row.contractNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contractName" label="合同名称" min-width="174" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="name-cell">
              <span class="link-text" @click="view(row)">{{ row.contractName }}</span>
              <el-tag v-if="row.expiringSoon" size="small" type="warning" effect="plain" style="margin-left: 6px">
                {{ row.expireDays }}天后到期
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="contractTypeName" label="类型" width="74" />
        <el-table-column label="签约双方" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="party-cell">
              <div>{{ row.partyA }}</div>
              <div class="muted">↔ {{ row.partyB }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="合同金额" width="108" align="right">
          <template #default="{ row }"><span class="money">{{ formatMoney(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column prop="deptName" label="归属部门" width="80" />
        <el-table-column prop="ownerName" label="负责人" width="82" />
        <el-table-column prop="signDate" label="签订日期" width="92">
          <template #default="{ row }">{{ formatDate(row.signDate) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="78">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="archiveNo" label="档案号" width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.archiveNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="附件" width="48" align="center">
          <template #default="{ row }">
            <span class="attachment-count"><el-icon><Paperclip /></el-icon>{{ row.attachmentCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" class-name="op-cell">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="view(row)">详情</el-button>
            <el-button
              v-if="row.canEdit && userStore.hasPerm('contract:edit')"
              link
              type="primary"
              size="small"
              @click="$router.push(`/contracts/${row.id}/edit`)"
            >编辑</el-button>
            <el-button
              v-if="row.canEdit && userStore.hasPerm('contract:submit')"
              link
              type="success"
              size="small"
              @click="handleSubmit(row)"
            >提交</el-button>
            <el-button
              v-if="row.canEdit && userStore.hasPerm('contract:delete')"
              link
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="page.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog v-model="importVisible" title="批量导入合同" width="540px" @closed="resetImport">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 18px">
        <template #title>
          请先下载导入模板，按模板格式填写后上传。支持 .xlsx / .xls 文件，单次最多导入 1000 条。
        </template>
      </el-alert>
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        :show-file-list="true"
        accept=".xlsx,.xls"
        :on-change="onImportFileChange"
        :on-exceed="onImportExceed"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 Excel 文件拖到此处，或 <em>点击选择文件</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 Excel 模板文件，导入结果会逐行提示。</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importFile" @click="handleImport">开始导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resultVisible" title="导入结果" width="680px">
      <div class="import-summary">
        <div class="import-stat success">
          <div class="num">{{ importResult.successCount || 0 }}</div>
          <div class="label">成功导入</div>
        </div>
        <div class="import-stat fail">
          <div class="num">{{ importResult.failCount || 0 }}</div>
          <div class="label">导入失败</div>
        </div>
        <div class="import-stat">
          <div class="num">{{ (importResult.successCount || 0) + (importResult.failCount || 0) }}</div>
          <div class="label">数据总条数</div>
        </div>
      </div>
      <template v-if="(importResult.messages || []).length">
        <div class="import-message-title">导入提示</div>
        <el-scrollbar max-height="320px">
          <div v-for="(message, index) in importResult.messages" :key="index" class="import-message">
            <el-icon color="#e6a23c"><WarningFilled /></el-icon>
            <span>{{ message }}</span>
          </div>
        </el-scrollbar>
      </template>
      <el-empty v-else description="全部数据导入成功" :image-size="80" />
      <template #footer>
        <el-button type="primary" @click="resultVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getContracts,
  submitContract,
  deleteContract,
  importContracts,
  downloadImportTemplate,
  exportContracts
} from '@/api/contract'
import { getOptionDepts, getOptionDict } from '@/api/system'
import { downloadResponse } from '@/utils/download'
import { formatMoney, formatDate } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const exporting = ref(false)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  contractType: '',
  deptId: null,
  status: '',
  attachmentKeyword: '',
  expiringSoon: false,
  amountMin: null,
  amountMax: null,
  signDateStart: '',
  signDateEnd: ''
})
const page = ref({ records: [], total: 0 })
const signDateRange = ref([])
const typeOptions = ref([])
const deptOptions = ref([])

const importVisible = ref(false)
const importing = ref(false)
const importFile = ref(null)
const uploadRef = ref(null)
const resultVisible = ref(false)
const importResult = ref({})

function buildParams() {
  const params = { ...query }
  if (signDateRange.value?.length === 2) {
    params.signDateStart = signDateRange.value[0]
    params.signDateEnd = signDateRange.value[1]
  } else {
    params.signDateStart = ''
    params.signDateEnd = ''
  }
  if (!params.amountMin) params.amountMin = null
  if (!params.amountMax) params.amountMax = null
  return params
}

async function load() {
  loading.value = true
  try {
    page.value = await getContracts(buildParams())
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function reset() {
  Object.assign(query, {
    pageNum: 1,
    keyword: '',
    contractType: '',
    deptId: null,
    status: '',
    attachmentKeyword: '',
    expiringSoon: false,
    amountMin: null,
    amountMax: null
  })
  signDateRange.value = []
  load()
}

function view(row) {
  router.push(`/contracts/${row.id}`)
}

async function handleSubmit(row) {
  try {
    await ElMessageBox.confirm(
      `确认将合同“${row.contractName}”提交归档审核？提交后需档案管理员审核通过方可正式归档。`,
      '提交归档',
      { type: 'warning', confirmButtonText: '提交审核', cancelButtonText: '取消' }
    )
    await submitContract(row.id)
    ElMessage.success('已提交归档审核')
    load()
  } catch {
    // cancelled
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除合同“${row.contractName}”？仅草稿或已驳回合同允许删除，删除后不可恢复。`,
      '删除合同',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deleteContract(row.id)
    ElMessage.success('合同已删除')
    load()
  } catch {
    // cancelled
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const response = await exportContracts(buildParams())
    downloadResponse(response, '合同档案台账.xlsx')
    ElMessage.success('台账导出成功')
  } finally {
    exporting.value = false
  }
}

async function handleTemplate() {
  const response = await downloadImportTemplate()
  downloadResponse(response, '合同批量导入模板.xlsx')
}

function onImportFileChange(file) {
  importFile.value = file.raw
}

function onImportExceed(files) {
  uploadRef.value?.clearFiles()
  const raw = files[0]
  uploadRef.value?.handleStart(raw)
  importFile.value = raw
}

async function handleImport() {
  if (!importFile.value) return
  importing.value = true
  const formData = new FormData()
  formData.append('file', importFile.value)
  try {
    importResult.value = await importContracts(formData)
    importVisible.value = false
    resultVisible.value = true
    if (importResult.value.successCount > 0) load()
  } finally {
    importing.value = false
  }
}

function resetImport() {
  importFile.value = null
  uploadRef.value?.clearFiles()
}

async function loadOptions() {
  const [types, depts] = await Promise.all([getOptionDict('contract_type'), getOptionDepts()])
  typeOptions.value = types || []
  deptOptions.value = depts || []
}

onMounted(() => {
  loadOptions()
  load()
})
</script>

<style scoped>
.range-inputs {
  display: flex;
  align-items: center;
  gap: 5px;
}
.name-cell {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
}
.party-cell {
  line-height: 1.55;
  font-size: 12.5px;
}
.attachment-count {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: #8b96ab;
  font-size: 12px;
}
:deep(.op-cell .cell) {
  padding: 0 4px;
}
:deep(.op-cell .el-button + .el-button) {
  margin-left: 6px;
}
.status-tabs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.import-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.import-stat {
  text-align: center;
  padding: 16px 8px;
  background: #f8faff;
  border-radius: 10px;
}
.import-stat .num {
  font-size: 24px;
  font-weight: 700;
  color: #1f2a44;
}
.import-stat.success .num {
  color: #20b26c;
}
.import-stat.fail .num {
  color: #f05b5b;
}
.import-stat .label {
  font-size: 11.5px;
  color: #8b96ab;
  margin-top: 5px;
}
.import-message-title {
  font-size: 12.5px;
  font-weight: 600;
  color: #526079;
  margin-bottom: 8px;
}
.import-message {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  padding: 8px 10px;
  background: #fffaf0;
  border-radius: 7px;
  font-size: 12px;
  color: #7a5a12;
  margin-bottom: 6px;
  line-height: 1.6;
}
</style>
