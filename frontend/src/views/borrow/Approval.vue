<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <h2 class="page-title">借阅审批</h2>
        <p class="page-desc">审批企业成员的档案借阅申请，控制敏感档案的下载权限与使用期限。</p>
      </div>
      <div class="page-actions">
        <el-tag type="warning" effect="light" size="large" round>
          <el-icon><Timer /></el-icon> 待审批 {{ pendingCount }} 条
        </el-tag>
        <el-button :loading="loading" @click="load"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </div>
    </div>

    <div class="panel-card table-card">
      <div class="status-tabs">
        <el-radio-group v-model="query.status" size="small" @change="search">
          <el-radio-button value="PENDING">待审批</el-radio-button>
          <el-radio-button value="">全部申请</el-radio-button>
          <el-radio-button value="APPROVED">已通过</el-radio-button>
          <el-radio-button value="REJECTED">已驳回</el-radio-button>
          <el-radio-button value="RETURNED">已归还</el-radio-button>
        </el-radio-group>
        <el-input
          v-model="query.keyword"
          placeholder="借阅单号 / 用途"
          clearable
          size="small"
          style="width: 200px"
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <el-table v-loading="loading" :data="page.records" stripe>
        <el-table-column prop="borrowNo" label="借阅单号" width="158" fixed />
        <el-table-column label="申请人" width="130">
          <template #default="{ row }">
            <div class="applicant-cell">
              <div class="applicant-avatar">{{ (row.applicantName || '?').slice(-1) }}</div>
              <div>
                <div>{{ row.applicantName }}</div>
                <div class="muted">{{ row.applicantDept }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="借阅档案" min-width="210" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="contract-cell">
              <span class="link-text" @click="$router.push(`/contracts/${row.contractId}`)">{{ row.contractName }}</span>
              <span class="muted">{{ row.contractNo }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="purpose" label="借阅用途" min-width="180" show-overflow-tooltip />
        <el-table-column label="借阅期限" width="260">
          <template #default="{ row }">
            <div class="period-cell">
              <span>{{ formatDateTime(row.startTime) }}</span>
              <span class="muted">至 {{ formatDateTime(row.endTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="94">
          <template #default="{ row }"><StatusTag :status="row.status" kind="borrow" /></template>
        </el-table-column>
        <el-table-column label="申请时间" width="146">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.canApprove"
              type="primary"
              size="small"
              @click="openReview(row)"
            >审批</el-button>
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
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

    <el-dialog v-model="reviewVisible" title="借阅审批" width="560px">
      <div class="review-card">
        <div class="review-name">{{ current.contractName }}</div>
        <div class="muted">{{ current.contractNo }}</div>
        <div class="review-rows">
          <div><span>申请人</span><b>{{ current.applicantName }} · {{ current.applicantDept }}</b></div>
          <div><span>借阅用途</span><b>{{ current.purpose }}</b></div>
          <div><span>借阅期限</span><b>{{ formatDateTime(current.startTime) }} 至 {{ formatDateTime(current.endTime) }}</b></div>
        </div>
      </div>
      <el-form label-position="top">
        <el-form-item label="审批动作">
          <el-radio-group v-model="reviewForm.action">
            <el-radio-button value="APPROVE">同意借阅</el-radio-button>
            <el-radio-button value="REJECT">驳回申请</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="reviewForm.action === 'REJECT' ? '驳回原因（必填）' : '审批意见（选填）'">
          <el-input v-model="reviewForm.opinion" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="请填写审批意见" />
        </el-form-item>
      </el-form>
      <el-alert
        v-if="reviewForm.action === 'APPROVE'"
        type="info"
        :closable="false"
        show-icon
        title="审批通过后，申请人可在借阅期限内下载该档案原件；到期后下载权限自动失效。"
      />
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReview">确认审批</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="借阅申请详情" width="560px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="借阅单号">{{ detailRow.borrowNo }}</el-descriptions-item>
        <el-descriptions-item label="借阅档案">{{ detailRow.contractNo }} · {{ detailRow.contractName }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detailRow.applicantName }}（{{ detailRow.applicantDept }}）</el-descriptions-item>
        <el-descriptions-item label="借阅用途">{{ detailRow.purpose }}</el-descriptions-item>
        <el-descriptions-item label="借阅期限">
          {{ formatDateTime(detailRow.startTime) }} 至 {{ formatDateTime(detailRow.endTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="当前状态"><StatusTag :status="detailRow.status" kind="borrow" /></el-descriptions-item>
        <el-descriptions-item label="审批人">{{ detailRow.approverName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="审批时间">{{ formatDateTime(detailRow.approveTime) }}</el-descriptions-item>
        <el-descriptions-item label="审批意见">{{ detailRow.approveOpinion || '—' }}</el-descriptions-item>
        <el-descriptions-item label="归还时间">{{ formatDateTime(detailRow.returnTime) }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatDateTime(detailRow.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detailRow.canApprove" type="primary" @click="detailVisible = false; openReview(detailRow)">审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBorrowApprovals, reviewBorrow } from '@/api/borrow'
import { formatDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

const loading = ref(false)
const submitting = ref(false)
const page = ref({ records: [], total: 0 })
const query = reactive({ pageNum: 1, pageSize: 10, status: 'PENDING', keyword: '' })
const reviewVisible = ref(false)
const detailVisible = ref(false)
const current = ref({})
const detailRow = ref({})
const reviewForm = reactive({ action: 'APPROVE', opinion: '' })

const pendingCount = computed(() => {
  if (query.status === 'PENDING') return page.value.total
  return page.value.records.filter((item) => item.status === 'PENDING').length
})

async function load() {
  loading.value = true
  try {
    page.value = await getBorrowApprovals(query)
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function openReview(row) {
  current.value = row
  reviewForm.action = 'APPROVE'
  reviewForm.opinion = ''
  reviewVisible.value = true
}

function viewDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

async function submitReview() {
  if (reviewForm.action === 'REJECT' && !reviewForm.opinion.trim()) {
    ElMessage.warning('驳回时必须填写审批意见')
    return
  }
  try {
    await ElMessageBox.confirm(
      reviewForm.action === 'APPROVE' ? '确认同意该借阅申请？' : '确认驳回该借阅申请？',
      '借阅审批',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  submitting.value = true
  try {
    await reviewBorrow(current.value.id, { ...reviewForm })
    ElMessage.success(reviewForm.action === 'APPROVE' ? '已同意借阅申请' : '已驳回借阅申请')
    reviewVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
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
.applicant-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
}
.applicant-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #edf3ff;
  color: #2f6bff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex: 0 0 28px;
}
.applicant-cell .muted {
  font-size: 10.5px;
}
.contract-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.contract-cell .muted {
  font-size: 11px;
}
.period-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 11.5px;
}
.review-card {
  background: #f8faff;
  border: 1px solid #edf2fc;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 16px;
}
.review-name {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 4px;
}
.review-rows {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.review-rows > div {
  display: flex;
  gap: 12px;
  font-size: 12.5px;
  line-height: 1.6;
}
.review-rows span {
  flex: 0 0 66px;
  color: #8b96ab;
}
.review-rows b {
  font-weight: 500;
  word-break: break-all;
}
</style>
