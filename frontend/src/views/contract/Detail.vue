<template>
  <div class="page-container" v-loading="loading">
    <div class="page-heading">
      <div style="min-width: 0">
        <div class="heading-back">
          <el-button link @click="$router.push('/contracts')"><el-icon><ArrowLeft /></el-icon> 合同台账</el-button>
          <span class="muted">/</span>
          <span class="muted">{{ contract.contractNo }}</span>
        </div>
        <h2 class="page-title detail-title">{{ contract.contractName || '合同详情' }}</h2>
        <div class="title-tags">
          <StatusTag :status="contract.status" />
          <el-tag size="small" effect="plain">{{ contract.contractTypeName }}</el-tag>
          <el-tag size="small" effect="plain" type="info">{{ contract.securityLevelName }}</el-tag>
          <el-tag v-if="contract.expiringSoon" size="small" type="warning">
            <el-icon><Warning /></el-icon> {{ contract.expireDays }} 天后到期
          </el-tag>
        </div>
      </div>
      <div class="page-actions">
        <el-button v-if="contract.canDownload && userStore.hasPerm('contract:download')" @click="downloadFirst">
          <el-icon><Download /></el-icon> 下载原件
        </el-button>
        <el-button
          v-if="isArchived && userStore.hasPerm('borrow:apply')"
          type="primary"
          plain
          @click="borrowVisible = true"
        >
          <el-icon><Reading /></el-icon> 申请借阅
        </el-button>
        <el-button
          v-if="isArchived && userStore.hasPerm('change:apply') && canApplyChange"
          @click="openChangeDialog"
        >
          <el-icon><EditPen /></el-icon> 申请变更
        </el-button>
        <el-button
          v-if="contract.status === 'PENDING' && userStore.hasPerm('archive:review')"
          type="primary"
          @click="openReviewDialog"
        >
          <el-icon><Checked /></el-icon> 归档审核
        </el-button>
        <el-button
          v-if="contract.canEdit && userStore.hasPerm('contract:edit')"
          type="primary"
          @click="$router.push(`/contracts/${contract.id}/edit`)"
        >
          <el-icon><Edit /></el-icon> 编辑
        </el-button>
        <el-button
          v-if="contract.canEdit && userStore.hasPerm('contract:submit')"
          type="success"
          @click="handleSubmit"
        >
          <el-icon><Promotion /></el-icon> 提交归档
        </el-button>
      </div>
    </div>

    <div class="summary-strip">
      <div class="summary-item">
        <div class="label">合同金额</div>
        <div class="value money">{{ formatMoney(contract.amount) }}</div>
      </div>
      <div class="summary-item">
        <div class="label">档案编号</div>
        <div class="value archive-no">{{ contract.archiveNo || '尚未归档' }}</div>
      </div>
      <div class="summary-item">
        <div class="label">归档时间</div>
        <div class="value">{{ formatDateTime(contract.archiveTime) }}</div>
      </div>
      <div class="summary-item">
        <div class="label">附件数量</div>
        <div class="value">{{ contract.attachmentCount || 0 }} 份</div>
      </div>
      <div class="summary-item">
        <div class="label">合同版本</div>
        <div class="value">V{{ (contract.version || 0) + 1 }}</div>
      </div>
    </div>

    <el-alert
      v-if="isArchived && !contract.canDownload"
      type="warning"
      :closable="false"
      show-icon
      title="该档案已进入借阅管控：可在线预览，下载原件需提交借阅申请并经档案管理员审批通过。"
      style="margin-bottom: 16px"
    />

    <div class="panel-card detail-card">
      <div class="section-title">基础信息</div>
      <div class="detail-grid">
        <div class="detail-item"><span class="label">合同编号</span><span class="value">{{ contract.contractNo }}</span></div>
        <div class="detail-item"><span class="label">合同名称</span><span class="value">{{ contract.contractName }}</span></div>
        <div class="detail-item"><span class="label">合同类型</span><span class="value">{{ contract.contractTypeName }}</span></div>
        <div class="detail-item"><span class="label">甲方</span><span class="value">{{ contract.partyA }}</span></div>
        <div class="detail-item"><span class="label">乙方</span><span class="value">{{ contract.partyB }}</span></div>
        <div class="detail-item"><span class="label">合同金额</span><span class="value money">{{ formatMoney(contract.amount) }}</span></div>
        <div class="detail-item"><span class="label">签订日期</span><span class="value">{{ formatDate(contract.signDate) }}</span></div>
        <div class="detail-item"><span class="label">生效日期</span><span class="value">{{ formatDate(contract.effectiveDate) }}</span></div>
        <div class="detail-item">
          <span class="label">到期日期</span>
          <span class="value" :class="{ 'text-danger': contract.expiringSoon }">{{ formatDate(contract.expireDate) }}</span>
        </div>
        <div class="detail-item"><span class="label">归属部门</span><span class="value">{{ contract.deptName }}</span></div>
        <div class="detail-item"><span class="label">负责人</span><span class="value">{{ contract.ownerName }}</span></div>
        <div class="detail-item"><span class="label">密级</span><span class="value">{{ contract.securityLevelName }}</span></div>
        <div class="detail-item"><span class="label">录入人</span><span class="value">{{ contract.createByName }}</span></div>
        <div class="detail-item"><span class="label">录入时间</span><span class="value">{{ formatDateTime(contract.createTime) }}</span></div>
        <div class="detail-item"><span class="label">归档人</span><span class="value">{{ contract.archiveByName || '—' }}</span></div>
        <div class="detail-item" style="grid-column: 1 / -1">
          <span class="label">备注说明</span><span class="value">{{ contract.remark || '—' }}</span>
        </div>
      </div>
    </div>

    <div class="panel-card detail-card">
      <div class="section-title">合同附件</div>
      <FileUploader
        v-if="contract.id"
        :contract-id="contract.id"
        :editable="false"
        :can-download="!!contract.canDownload"
      />
    </div>

    <div class="panel-card detail-card">
      <div class="section-title">付款节点</div>
      <el-table v-if="(contract.paymentNodes || []).length" :data="contract.paymentNodes" size="small">
        <el-table-column prop="nodeName" label="节点名称" min-width="160" />
        <el-table-column label="计划付款日期" width="140">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.expiringSoon }">
              {{ formatDate(row.planPayDate) }}
              <el-tag v-if="row.expiringSoon" size="small" type="warning" effect="plain">临近</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="付款金额" width="140" align="right">
          <template #default="{ row }"><span class="money">{{ formatMoney(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column label="支付状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.paidStatus === 1 ? 'success' : 'info'" effect="plain">
              {{ row.paidStatus === 1 ? '已支付' : '未支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      </el-table>
      <div v-else class="empty-tip">暂无付款节点</div>
    </div>

    <div class="detail-bottom">
      <div class="panel-card detail-card">
        <div class="section-title">归档审核轨迹</div>
        <el-timeline v-if="(contract.reviews || []).length">
          <el-timeline-item
            v-for="item in contract.reviews"
            :key="item.id"
            :timestamp="formatDateTime(item.reviewTime)"
            :type="reviewType(item.action)"
            placement="top"
          >
            <div class="timeline-title">
              {{ reviewActionLabel(item.action) }}
              <span class="muted" style="font-weight: 400; margin-left: 6px">{{ item.reviewerName }}</span>
            </div>
            <div class="timeline-desc">{{ item.opinion || '—' }}</div>
            <div v-if="item.archiveNo" class="timeline-archive">档案号：{{ item.archiveNo }}</div>
          </el-timeline-item>
        </el-timeline>
        <div v-else class="empty-tip">暂无审核记录</div>
      </div>

      <div class="panel-card detail-card">
        <div class="section-title">档案变更记录</div>
        <el-table v-if="(contract.changes || []).length" :data="contract.changes" size="small">
          <el-table-column label="变更类型" width="110">
            <template #default="{ row }">{{ changeTypeLabel(row.changeType) }}</template>
          </el-table-column>
          <el-table-column prop="changeReason" label="变更原因" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" width="88">
            <template #default="{ row }"><StatusTag :status="row.status" kind="change" /></template>
          </el-table-column>
          <el-table-column prop="applicantName" label="申请人" width="86" />
          <el-table-column label="申请时间" width="140">
            <template #default="{ row }">{{ formatDateTime(row.applyTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="76" align="center">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="viewDiff(row)">差异</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-tip">暂无变更记录，归档信息保持原始状态</div>
      </div>
    </div>

    <el-dialog v-model="borrowVisible" title="申请借阅档案" width="520px" @closed="resetBorrow">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="借阅申请经档案管理员审批通过后，方可在借阅期限内下载档案原件。单次借阅最长 90 天。"
        style="margin-bottom: 16px"
      />
      <el-form ref="borrowFormRef" :model="borrowForm" :rules="borrowRules" label-width="88px">
        <el-form-item label="借阅档案">
          <el-input :model-value="`${contract.contractNo} · ${contract.contractName}`" disabled />
        </el-form-item>
        <el-form-item label="借阅用途" prop="purpose">
          <el-input v-model="borrowForm.purpose" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="请说明借阅用途与使用场景" />
        </el-form-item>
        <el-form-item label="借阅期限" prop="timeRange">
          <el-date-picker
            v-model="borrowForm.timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            :default-time="defaultBorrowTime"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="borrowVisible = false">取消</el-button>
        <el-button type="primary" :loading="borrowSubmitting" @click="submitBorrow">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="归档审核" width="560px">
      <div class="review-contract">
        <div class="review-contract-title">{{ contract.contractNo }} · {{ contract.contractName }}</div>
        <div class="muted">
          {{ contract.partyA }} ↔ {{ contract.partyB }} · {{ formatMoney(contract.amount) }} · {{ contract.attachmentCount }} 份附件
        </div>
      </div>
      <el-form label-width="86px">
        <el-form-item label="审核结论">
          <el-radio-group v-model="reviewForm.action">
            <el-radio value="APPROVE">审核通过并归档</el-radio>
            <el-radio value="REJECT">驳回申请</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见" :required="reviewForm.action === 'REJECT'">
          <el-input v-model="reviewForm.opinion" type="textarea" :rows="4" maxlength="300" show-word-limit placeholder="审核通过时可选填；驳回时必须填写具体原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">确认审核</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="changeVisible" title="申请档案变更" width="820px" top="4vh">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="归档档案不可直接修改。请填写变更内容并说明原因，经档案管理员审批通过后系统自动覆盖档案信息并完整留痕。"
        style="margin-bottom: 16px"
      />
      <el-form ref="changeFormRef" :model="changeForm" :rules="changeRules" label-width="96px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="变更类型" prop="changeType">
              <el-select v-model="changeForm.changeType" placeholder="请选择" style="width: 100%">
                <el-option v-for="item in changeTypeOptions" :key="item.itemValue" :label="item.itemLabel" :value="item.itemValue" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合同金额">
              <el-input-number v-model="changeForm.amount" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="合同名称">
              <el-input v-model="changeForm.contractName" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="甲方">
              <el-input v-model="changeForm.partyA" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="乙方">
              <el-input v-model="changeForm.partyB" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="签订日期">
              <el-date-picker v-model="changeForm.signDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生效日期">
              <el-date-picker v-model="changeForm.effectiveDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="到期日期">
              <el-date-picker v-model="changeForm.expireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="合同类型">
              <el-select v-model="changeForm.contractType" style="width: 100%">
                <el-option v-for="item in typeOptions" :key="item.itemValue" :label="item.itemLabel" :value="item.itemValue" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="归属部门">
              <el-select v-model="changeForm.deptId" filterable style="width: 100%">
                <el-option v-for="item in deptOptions" :key="item.id" :label="item.deptName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="负责人">
              <el-select v-model="changeForm.ownerId" filterable style="width: 100%">
                <el-option v-for="item in userOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="密级">
              <el-select v-model="changeForm.securityLevel" style="width: 100%">
                <el-option v-for="item in securityOptions" :key="item.itemValue" :label="item.itemLabel" :value="item.itemValue" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="changeForm.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="变更原因" prop="changeReason">
              <el-input v-model="changeForm.changeReason" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="请说明变更背景、依据及具体内容" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="change-preview">
          <div class="change-preview-title">本次变更项（{{ changeDiffs.length }}）</div>
          <div v-if="!changeDiffs.length" class="muted">尚未修改任何字段</div>
          <div v-for="item in changeDiffs" :key="item.key" class="change-diff-row">
            <span class="change-field">{{ item.label }}</span>
            <span class="change-before">{{ item.before }}</span>
            <el-icon><Right /></el-icon>
            <span class="change-after">{{ item.after }}</span>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="changeVisible = false">取消</el-button>
        <el-button type="primary" :loading="changeSubmitting" :disabled="!changeDiffs.length" @click="submitChange">提交变更申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="diffVisible" title="变更内容对比" width="760px">
      <div class="diff-header">
        <span>{{ diffRow.changeType ? changeTypeLabel(diffRow.changeType) : '档案变更' }}</span>
        <StatusTag :status="diffRow.status" kind="change" />
      </div>
      <div class="muted" style="margin-bottom: 14px">{{ diffRow.changeReason }}</div>
      <el-table :data="diffItems" size="small">
        <el-table-column prop="label" label="字段" width="120" />
        <el-table-column label="变更前" min-width="220">
          <template #default="{ row }"><span class="diff-before">{{ row.before }}</span></template>
        </el-table-column>
        <el-table-column label="变更后" min-width="220">
          <template #default="{ row }"><span class="diff-after">{{ row.after }}</span></template>
        </el-table-column>
      </el-table>
      <div v-if="diffRow.approveOpinion" class="diff-opinion">
        审批意见：{{ diffRow.approveOpinion }}（{{ diffRow.approverName }} · {{ formatDateTime(diffRow.approveTime) }}）
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'
import { getContract, submitContract } from '@/api/contract'
import { downloadAttachment } from '@/api/attachment'
import { reviewContract, applyChange } from '@/api/archive'
import { applyBorrow } from '@/api/borrow'
import { getOptionDepts, getOptionDict, getOptionUsers } from '@/api/system'
import { formatMoney, formatDate, formatDateTime } from '@/utils/format'
import { downloadResponse } from '@/utils/download'
import StatusTag from '@/components/StatusTag.vue'
import FileUploader from '@/components/FileUploader.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const contract = ref({})
const attachmentCount = ref(0)

const borrowVisible = ref(false)
const borrowFormRef = ref(null)
const borrowSubmitting = ref(false)
const borrowForm = reactive({ purpose: '', timeRange: [] })
const defaultBorrowTime = [new Date(2000, 0, 1, 9, 0, 0), new Date(2000, 0, 1, 18, 0, 0)]
const borrowRules = {
  purpose: [{ required: true, message: '请填写借阅用途', trigger: 'blur' }],
  timeRange: [{ required: true, type: 'array', len: 2, message: '请选择借阅期限', trigger: 'change' }]
}

const reviewVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewForm = reactive({ action: 'APPROVE', opinion: '' })

const changeVisible = ref(false)
const changeFormRef = ref(null)
const changeSubmitting = ref(false)
const changeTypeOptions = ref([])
const typeOptions = ref([])
const securityOptions = ref([])
const deptOptions = ref([])
const userOptions = ref([])
const changeForm = reactive({})
const changeRules = {
  changeType: [{ required: true, message: '请选择变更类型', trigger: 'change' }],
  changeReason: [{ required: true, message: '请填写变更原因', trigger: 'blur' }]
}
const diffVisible = ref(false)
const diffRow = ref({})
const diffItems = ref([])

const isArchived = computed(() => ['ARCHIVED', 'EXPIRED'].includes(contract.value.status))
const canApplyChange = computed(() => {
  const profileId = userStore.profile?.id
  return (
    userStore.hasPerm('contract:view:all') ||
    userStore.isAdmin ||
    contract.value.createBy === profileId ||
    contract.value.ownerId === profileId
  )
})

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

function reviewActionLabel(action) {
  return { SUBMIT: '提交归档申请', APPROVE: '审核通过', REJECT: '审核驳回' }[action] || action
}
function reviewType(action) {
  return { SUBMIT: 'primary', APPROVE: 'success', REJECT: 'danger' }[action] || 'info'
}
function changeTypeLabel(value) {
  return changeTypeOptions.value.find((item) => item.itemValue === value)?.itemLabel || value || '档案变更'
}
function optionLabel(list, value, labelKey = 'itemLabel', valueKey = 'itemValue') {
  return list.find((item) => item[valueKey] === value)?.[labelKey] || value || '—'
}

async function load() {
  loading.value = true
  try {
    contract.value = await getContract(route.params.id)
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [types, security, changeTypes, depts, users] = await Promise.all([
    getOptionDict('contract_type'),
    getOptionDict('security_level'),
    getOptionDict('change_type'),
    getOptionDepts(),
    getOptionUsers()
  ])
  typeOptions.value = types || []
  securityOptions.value = security || []
  changeTypeOptions.value = changeTypes || []
  deptOptions.value = depts || []
  userOptions.value = users || []
}

async function downloadFirst() {
  const attachment = (contract.value.attachments || [])[0]
  if (!attachment) {
    ElMessage.warning('该合同暂无附件')
    return
  }
  const response = await downloadAttachment(attachment.id)
  downloadResponse(response, attachment.originalName)
  ElMessage.success('附件下载成功')
}

function openReviewDialog() {
  reviewForm.action = 'APPROVE'
  reviewForm.opinion = ''
  reviewVisible.value = true
}

async function submitReview() {
  if (reviewForm.action === 'REJECT' && !reviewForm.opinion.trim()) {
    ElMessage.warning('驳回时必须填写审核意见')
    return
  }
  reviewSubmitting.value = true
  try {
    await reviewContract(contract.value.id, { ...reviewForm })
    ElMessage.success(reviewForm.action === 'APPROVE' ? '审核通过，档案已正式归档' : '已驳回归档申请')
    reviewVisible.value = false
    await load()
  } finally {
    reviewSubmitting.value = false
  }
}

async function handleSubmit() {
  try {
    await ElMessageBox.confirm(
      '提交后合同将进入归档审核，审核期间不可编辑。确认提交？',
      '提交归档',
      { type: 'warning', confirmButtonText: '确认提交', cancelButtonText: '取消' }
    )
    await submitContract(contract.value.id)
    ElMessage.success('合同已提交归档审核')
    await load()
  } catch {
    // cancelled
  }
}

function resetBorrow() {
  borrowForm.purpose = ''
  borrowForm.timeRange = []
}

async function submitBorrow() {
  const valid = await borrowFormRef.value?.validate().catch(() => false)
  if (!valid) return
  borrowSubmitting.value = true
  try {
    await applyBorrow({
      contractId: contract.value.id,
      purpose: borrowForm.purpose,
      startTime: borrowForm.timeRange[0],
      endTime: borrowForm.timeRange[1]
    })
    ElMessage.success('借阅申请已提交，请等待档案管理员审批')
    borrowVisible.value = false
  } finally {
    borrowSubmitting.value = false
  }
}

async function openChangeDialog() {
  Object.assign(changeForm, {
    changeType: '',
    changeReason: '',
    contractName: contract.value.contractName,
    partyA: contract.value.partyA,
    partyB: contract.value.partyB,
    signDate: contract.value.signDate,
    effectiveDate: contract.value.effectiveDate,
    expireDate: contract.value.expireDate,
    amount: Number(contract.value.amount),
    contractType: contract.value.contractType,
    deptId: contract.value.deptId,
    ownerId: contract.value.ownerId,
    securityLevel: contract.value.securityLevel,
    remark: contract.value.remark || ''
  })
  changeVisible.value = true
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

const changeDiffs = computed(() => {
  const originals = {
    contractName: contract.value.contractName,
    partyA: contract.value.partyA,
    partyB: contract.value.partyB,
    signDate: contract.value.signDate,
    effectiveDate: contract.value.effectiveDate,
    expireDate: contract.value.expireDate,
    amount: contract.value.amount === null ? null : Number(contract.value.amount),
    contractType: contract.value.contractType,
    deptId: contract.value.deptId,
    ownerId: contract.value.ownerId,
    securityLevel: contract.value.securityLevel,
    remark: contract.value.remark || ''
  }
  return Object.keys(fieldLabels)
    .filter((key) => {
      const before = originals[key]
      const after = changeForm[key]
      if (key === 'amount') return Number(before || 0) !== Number(after || 0)
      return (before ?? '') !== (after ?? '')
    })
    .map((key) => ({
      key,
      label: fieldLabels[key],
      before: displayValue(key, originals[key]),
      after: displayValue(key, changeForm[key]),
      rawAfter: changeForm[key]
    }))
})

async function submitChange() {
  const valid = await changeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!changeDiffs.value.length) {
    ElMessage.warning('请至少修改一个字段后再提交')
    return
  }
  if (changeForm.effectiveDate && changeForm.expireDate && changeForm.effectiveDate > changeForm.expireDate) {
    ElMessage.error('生效日期不能晚于到期日期')
    return
  }
  const changeFields = {}
  changeDiffs.value.forEach((item) => {
    changeFields[item.key] = item.rawAfter === '' ? null : item.rawAfter
  })
  changeSubmitting.value = true
  try {
    await applyChange({
      contractId: contract.value.id,
      changeType: changeForm.changeType,
      changeReason: changeForm.changeReason,
      changeFields
    })
    ElMessage.success('变更申请已提交，等待档案管理员审批')
    changeVisible.value = false
    await load()
  } finally {
    changeSubmitting.value = false
  }
}

function parseContent(text) {
  try {
    return JSON.parse(text || '{}')
  } catch {
    return {}
  }
}

function viewDiff(row) {
  diffRow.value = row
  const before = parseContent(row.beforeContent)
  const after = parseContent(row.afterContent)
  diffItems.value = Object.keys(fieldLabels)
    .filter((key) => String(before[key] ?? '') !== String(after[key] ?? ''))
    .map((key) => ({
      label: fieldLabels[key],
      before: displayValue(key, before[key]),
      after: displayValue(key, after[key])
    }))
  diffVisible.value = true
}

onMounted(async () => {
  await loadOptions()
  await load()
  const action = route.query.action
  if (action === 'borrow' && isArchived.value && userStore.hasPerm('borrow:apply')) {
    borrowVisible.value = true
  } else if (action === 'change' && isArchived.value && userStore.hasPerm('change:apply') && canApplyChange.value) {
    openChangeDialog()
  }
})
</script>

<style scoped>
.heading-back {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12.5px;
}
.detail-title {
  max-width: 800px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.title-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.archive-no {
  font-family: "Roboto Mono", monospace;
  font-size: 14px;
}
.detail-card {
  padding: 18px 22px;
  margin-bottom: 16px;
}
.detail-bottom {
  display: grid;
  grid-template-columns: minmax(0, 0.8fr) minmax(0, 1.2fr);
  gap: 16px;
}
.timeline-title {
  font-size: 13px;
  font-weight: 600;
}
.timeline-desc {
  font-size: 12px;
  color: #6b778e;
  margin-top: 5px;
  line-height: 1.65;
}
.timeline-archive {
  display: inline-block;
  margin-top: 6px;
  font-size: 11px;
  color: #2f6bff;
  background: #edf3ff;
  border-radius: 5px;
  padding: 2px 7px;
  font-family: "Roboto Mono", monospace;
}
.review-contract {
  padding: 12px 14px;
  background: #f8faff;
  border-radius: 9px;
  margin-bottom: 16px;
}
.review-contract-title {
  font-weight: 600;
  font-size: 13.5px;
  margin-bottom: 6px;
}
.change-preview {
  background: #f8faff;
  border: 1px dashed #dbe5fb;
  border-radius: 9px;
  padding: 12px 14px;
  margin-bottom: 4px;
}
.change-preview-title {
  font-size: 12.5px;
  font-weight: 600;
  margin-bottom: 8px;
}
.change-diff-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 0;
  font-size: 12px;
  flex-wrap: wrap;
}
.change-field {
  width: 72px;
  color: #8b96ab;
  flex: 0 0 72px;
}
.change-before {
  color: #98a2b3;
  text-decoration: line-through;
}
.change-after {
  color: #20b26c;
  font-weight: 600;
}
.diff-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  margin-bottom: 8px;
}
.diff-before {
  color: #98a2b3;
}
.diff-after {
  color: #20b26c;
  font-weight: 600;
}
.diff-opinion {
  margin-top: 14px;
  padding: 10px 12px;
  background: #f8faff;
  border-radius: 8px;
  font-size: 12px;
  color: #526079;
}
@media (max-width: 1100px) {
  .detail-bottom {
    grid-template-columns: 1fr;
  }
}
</style>
