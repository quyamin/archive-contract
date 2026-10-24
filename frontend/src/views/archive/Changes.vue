<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <h2 class="page-title">档案变更</h2>
        <p class="page-desc">已归档档案不可直接修改，所有变更均需提交申请并经审批，系统自动保留变更前后内容。</p>
      </div>
      <div class="page-actions">
        <el-button @click="$router.push('/ledger')"><el-icon><Search /></el-icon> 在台账中发起变更</el-button>
        <el-button :loading="loading" @click="load"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </div>
    </div>

    <div class="panel-card table-card">
      <div class="status-tabs">
        <el-radio-group v-model="query.status" size="small" @change="search">
          <el-radio-button value="">全部申请</el-radio-button>
          <el-radio-button value="PENDING">待审批</el-radio-button>
          <el-radio-button value="APPROVED">已通过</el-radio-button>
          <el-radio-button value="REJECTED">已驳回</el-radio-button>
        </el-radio-group>
        <span class="muted">共 {{ page.total }} 条变更申请</span>
      </div>

      <el-table v-loading="loading" :data="page.records" stripe>
        <el-table-column label="合同档案" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="contract-cell">
              <span class="link-text" @click="$router.push(`/contracts/${row.contractId}`)">{{ row.contractName || '合同 #' + row.contractId }}</span>
              <span class="muted">{{ row.contractNo }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="变更类型" width="112">
          <template #default="{ row }">{{ changeTypeLabel(row.changeType) }}</template>
        </el-table-column>
        <el-table-column prop="changeReason" label="变更原因" min-width="210" show-overflow-tooltip />
        <el-table-column prop="applicantName" label="申请人" width="92" />
        <el-table-column label="申请时间" width="146">
          <template #default="{ row }">{{ formatDateTime(row.applyTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="94">
          <template #default="{ row }"><StatusTag :status="row.status" kind="change" /></template>
        </el-table-column>
        <el-table-column prop="approverName" label="审批人" width="92">
          <template #default="{ row }">{{ row.approverName || '—' }}</template>
        </el-table-column>
        <el-table-column label="审批时间" width="146">
          <template #default="{ row }">{{ formatDateTime(row.approveTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="142" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDiff(row)">差异</el-button>
            <el-button
              v-if="row.status === 'PENDING' && userStore.hasPerm('change:review')"
              type="primary"
              size="small"
              @click="openReview(row)"
            >审批</el-button>
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

    <el-dialog v-model="diffVisible" title="变更内容对比" width="820px" top="6vh">
      <div class="diff-top">
        <div>
          <div class="diff-contract">{{ diffRow.contractName }}</div>
          <div class="muted">{{ diffRow.contractNo }} · {{ changeTypeLabel(diffRow.changeType) }}</div>
        </div>
        <StatusTag :status="diffRow.status" kind="change" />
      </div>
      <el-descriptions :column="2" size="small" border style="margin-bottom: 16px">
        <el-descriptions-item label="申请人">{{ diffRow.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatDateTime(diffRow.applyTime) }}</el-descriptions-item>
        <el-descriptions-item label="变更原因" :span="2">{{ diffRow.changeReason }}</el-descriptions-item>
        <el-descriptions-item v-if="diffRow.approverName" label="审批人">{{ diffRow.approverName }}</el-descriptions-item>
        <el-descriptions-item v-if="diffRow.approveTime" label="审批时间">{{ formatDateTime(diffRow.approveTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="diffRow.approveOpinion" label="审批意见" :span="2">{{ diffRow.approveOpinion }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="diffItems" size="small" border>
        <el-table-column prop="label" label="变更字段" width="130" />
        <el-table-column label="变更前" min-width="230">
          <template #default="{ row }"><span class="diff-before">{{ row.before }}</span></template>
        </el-table-column>
        <el-table-column label="变更后" min-width="230">
          <template #default="{ row }"><span class="diff-after">{{ row.after }}</span></template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="diffVisible = false">关闭</el-button>
        <el-button
          v-if="diffRow.status === 'PENDING' && userStore.hasPerm('change:review')"
          type="primary"
          @click="diffVisible = false; openReview(diffRow)"
        >前往审批</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="档案变更审批" width="620px">
      <div class="review-header">
        <div class="review-name">{{ reviewRow.contractName }}</div>
        <div class="muted">{{ reviewRow.contractNo }} · 申请人 {{ reviewRow.applicantName }}</div>
      </div>
      <div class="change-summary">
        <div class="change-summary-label">变更原因</div>
        <div class="change-summary-text">{{ reviewRow.changeReason }}</div>
        <div class="change-summary-label" style="margin-top: 12px">变更项（{{ diffItems.filter((item) => item.changed).length }} 项）</div>
        <div class="change-summary-list">
          <div v-for="item in diffItems.filter((i) => i.changed)" :key="item.key" class="change-summary-row">
            <span class="change-field">{{ item.label }}</span>
            <span class="diff-before">{{ item.before }}</span>
            <el-icon><Right /></el-icon>
            <span class="diff-after">{{ item.after }}</span>
          </div>
        </div>
      </div>
      <el-form label-position="top">
        <el-form-item label="审批动作">
          <el-radio-group v-model="reviewForm.action">
            <el-radio-button value="APPROVE">批准变更</el-radio-button>
            <el-radio-button value="REJECT">驳回申请</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="reviewForm.action === 'REJECT' ? '驳回意见（必填）' : '审批意见（选填）'">
          <el-input
            v-model="reviewForm.opinion"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="请填写审批意见"
          />
        </el-form-item>
      </el-form>
      <el-alert
        v-if="reviewForm.action === 'APPROVE'"
        type="warning"
        :closable="false"
        show-icon
        title="批准后变更内容将立即写入档案，并完整保留本次变更前后的快照记录。"
      />
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReview">确认审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getChanges, reviewChange } from '@/api/archive'
import { getOptionDepts, getOptionDict, getOptionUsers } from '@/api/system'
import { formatMoney, formatDate, formatDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

const userStore = useUserStore()
const loading = ref(false)
const page = ref({ records: [], total: 0 })
const query = reactive({ pageNum: 1, pageSize: 10, status: '' })
const changeTypeOptions = ref([])
const typeOptions = ref([])
const securityOptions = ref([])
const deptOptions = ref([])
const userOptions = ref([])

const diffVisible = ref(false)
const diffRow = ref({})
const diffItems = ref([])
const reviewVisible = ref(false)
const reviewRow = ref({})
const submitting = ref(false)
const reviewForm = reactive({ action: 'APPROVE', opinion: '' })

const fieldLabels = {
  contractName: '合同名称',
  partyA: '甲方',
  partyB: '乙方',
  signDate: '签订日期',
  effectiveDate: '生效日期',
  expireDate: '到期日期',
  amount: '合同金额',
  contractType: '合同类型',
  deptId: '归属部门',
  ownerId: '负责人',
  securityLevel: '密级',
  remark: '备注'
}

function changeTypeLabel(value) {
  return changeTypeOptions.value.find((item) => item.itemValue === value)?.itemLabel || value || '—'
}
function optionLabel(list, value, labelKey = 'itemLabel', valueKey = 'itemValue') {
  return list.find((item) => item[valueKey] === value)?.[labelKey] || value || '—'
}
function displayValue(key, value) {
  if (value === null || value === undefined || value === '') return '—'
  if (key === 'amount') return formatMoney(value)
  if (key === 'contractType') return optionLabel(typeOptions.value, value)
  if (key === 'securityLevel') return optionLabel(securityOptions.value, value)
  if (key === 'deptId') return optionLabel(deptOptions.value, value, 'deptName', 'id')
  if (key === 'ownerId') return optionLabel(userOptions.value, value, 'name', 'id')
  if (['signDate', 'effectiveDate', 'expireDate'].includes(key)) return formatDate(value)
  return String(value)
}
function parseContent(text) {
  try {
    return JSON.parse(text || '{}')
  } catch {
    return {}
  }
}

function buildDiff(row) {
  const before = parseContent(row.beforeContent)
  const after = parseContent(row.afterContent)
  return Object.keys(fieldLabels).map((key) => ({
    key,
    label: fieldLabels[key],
    before: displayValue(key, before[key]),
    after: displayValue(key, after[key]),
    changed: String(before[key] ?? '') !== String(after[key] ?? '')
  }))
}

async function load() {
  loading.value = true
  try {
    page.value = await getChanges({
      ...query,
      onlyMine: !userStore.hasPerm('change:review')
    })
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function viewDiff(row) {
  diffRow.value = row
  diffItems.value = buildDiff(row)
  diffVisible.value = true
}

function openReview(row) {
  reviewRow.value = row
  diffItems.value = buildDiff(row)
  reviewForm.action = 'APPROVE'
  reviewForm.opinion = ''
  reviewVisible.value = true
}

async function submitReview() {
  if (reviewForm.action === 'REJECT' && !reviewForm.opinion.trim()) {
    ElMessage.warning('驳回时必须填写审批意见')
    return
  }
  try {
    await ElMessageBox.confirm(
      reviewForm.action === 'APPROVE'
        ? '确认批准本次档案变更？批准后变更内容将写入正式档案。'
        : '确认驳回本次变更申请？',
      '变更审批',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  submitting.value = true
  try {
    await reviewChange(reviewRow.value.id, { ...reviewForm })
    ElMessage.success(reviewForm.action === 'APPROVE' ? '变更已批准并生效' : '变更申请已驳回')
    reviewVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function loadOptions() {
  const [changeTypes, types, security, depts, users] = await Promise.all([
    getOptionDict('change_type'),
    getOptionDict('contract_type'),
    getOptionDict('security_level'),
    getOptionDepts(),
    getOptionUsers()
  ])
  changeTypeOptions.value = changeTypes || []
  typeOptions.value = types || []
  securityOptions.value = security || []
  deptOptions.value = depts || []
  userOptions.value = users || []
}

onMounted(() => {
  loadOptions()
  load()
})
</script>

<style scoped>
.status-tabs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.contract-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.contract-cell .muted {
  font-size: 11px;
}
.diff-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}
.diff-contract {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 4px;
}
.diff-before {
  color: #98a2b3;
  text-decoration: line-through;
}
.diff-after {
  color: #20b26c;
  font-weight: 600;
}
.review-header {
  padding: 14px 16px;
  background: #f8faff;
  border-radius: 9px;
  margin-bottom: 14px;
}
.review-name {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 4px;
}
.change-summary {
  border: 1px dashed #dbe5fb;
  background: #fbfcff;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 16px;
}
.change-summary-label {
  font-size: 12px;
  font-weight: 600;
  color: #526079;
  margin-bottom: 6px;
}
.change-summary-text {
  font-size: 12.5px;
  color: #4c5872;
  line-height: 1.7;
}
.change-summary-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  padding: 4px 0;
  flex-wrap: wrap;
}
.change-field {
  width: 72px;
  color: #8b96ab;
  flex: 0 0 72px;
}
</style>
