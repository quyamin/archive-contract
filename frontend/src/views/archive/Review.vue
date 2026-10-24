<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <h2 class="page-title">归档审核</h2>
        <p class="page-desc">审核业务部门提交的归档申请，核验合同信息与附件完整性，通过后生成唯一档案编号。</p>
      </div>
      <div class="page-actions">
        <el-tag type="warning" effect="light" size="large" round>
          <el-icon><Timer /></el-icon> 待审核 {{ page.total }} 份
        </el-tag>
        <el-button :loading="loading" @click="load"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </div>
    </div>

    <div class="panel-card filter-card">
      <el-form :model="query" label-width="72px" inline @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="合同编号 / 名称 / 签约方" clearable style="width: 240px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="合同类型">
          <el-select v-model="query.contractType" clearable placeholder="全部类型" style="width: 132px">
            <el-option v-for="item in typeOptions" :key="item.itemValue" :label="item.itemLabel" :value="item.itemValue" />
          </el-select>
        </el-form-item>
        <el-form-item label="归属部门">
          <el-select v-model="query.deptId" clearable placeholder="全部部门" style="width: 132px">
            <el-option v-for="item in deptOptions" :key="item.id" :label="item.deptName" :value="item.id" />
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
        <el-table-column prop="contractNo" label="合同编号" width="146" fixed>
          <template #default="{ row }">
            <span class="link-text" @click="goDetail(row)">{{ row.contractNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contractName" label="合同名称" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="link-text" @click="goDetail(row)">{{ row.contractName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contractTypeName" label="类型" width="100" />
        <el-table-column label="签约双方" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">
            <div>{{ row.partyA }}</div>
            <div class="muted">↔ {{ row.partyB }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="合同金额" width="138" align="right">
          <template #default="{ row }"><span class="money">{{ formatMoney(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column prop="deptName" label="归属部门" width="104" />
        <el-table-column prop="ownerName" label="负责人" width="88" />
        <el-table-column label="附件" width="72" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.attachmentCount ? 'success' : 'danger'" effect="plain">
              {{ row.attachmentCount || 0 }} 份
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="signDate" label="签订日期" width="106">
          <template #default="{ row }">{{ formatDate(row.signDate) }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="146">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="142" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row)">详情</el-button>
            <el-button type="primary" size="small" @click="openReview(row)">审核</el-button>
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

    <el-drawer v-model="drawerVisible" title="归档审核" size="620px" destroy-on-close>
      <template v-if="current">
        <div class="review-head">
          <div class="review-name">{{ current.contractName }}</div>
          <div class="review-meta">
            {{ current.contractNo }} · {{ current.contractTypeName }} · {{ current.deptName }}
          </div>
          <div class="review-amount money">{{ formatMoney(current.amount) }}</div>
        </div>

        <div class="review-section-title">合同信息核验</div>
        <div class="review-grid">
          <div><span>甲方</span><b>{{ current.partyA }}</b></div>
          <div><span>乙方</span><b>{{ current.partyB }}</b></div>
          <div><span>签订日期</span><b>{{ formatDate(current.signDate) }}</b></div>
          <div><span>到期日期</span><b>{{ formatDate(current.expireDate) }}</b></div>
          <div><span>负责人</span><b>{{ current.ownerName }}</b></div>
          <div><span>密级</span><b>{{ current.securityLevelName }}</b></div>
        </div>

        <div class="review-section-title">
          附件核验
          <el-tag size="small" :type="current.attachmentCount ? 'success' : 'danger'" effect="plain" style="margin-left: 6px">
            {{ current.attachmentCount || 0 }} 份
          </el-tag>
        </div>
        <div class="attachment-box">
          <FileUploader :contract-id="current.id" :editable="false" :can-download="true" />
        </div>

        <div class="review-section-title">审核结论</div>
        <el-form label-position="top">
          <el-form-item label="审核动作">
            <el-radio-group v-model="reviewForm.action">
              <el-radio-button value="APPROVE">通过归档</el-radio-button>
              <el-radio-button value="REJECT">驳回申请</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="reviewForm.action === 'REJECT' ? '驳回原因（必填）' : '审核意见（选填）'">
            <el-input
              v-model="reviewForm.opinion"
              type="textarea"
              :rows="4"
              maxlength="300"
              show-word-limit
              :placeholder="reviewForm.action === 'REJECT' ? '请说明资料缺失或不符合归档要求的具体原因' : '可填写核验结论，如“合同信息与原件一致，同意归档”'"
            />
          </el-form-item>
        </el-form>
        <el-alert
          v-if="reviewForm.action === 'APPROVE'"
          type="success"
          :closable="false"
          show-icon
          title="审核通过后系统将自动生成唯一档案编号，档案数据转为只读状态，后续修改须走变更审批流程。"
        />
      </template>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitReview">确认审核</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReviewList, reviewContract } from '@/api/archive'
import { getOptionDepts, getOptionDict } from '@/api/system'
import { formatMoney, formatDate, formatDateTime } from '@/utils/format'
import FileUploader from '@/components/FileUploader.vue'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const drawerVisible = ref(false)
const current = ref(null)
const page = ref({ records: [], total: 0 })
const typeOptions = ref([])
const deptOptions = ref([])
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  contractType: '',
  deptId: null
})
const reviewForm = reactive({ action: 'APPROVE', opinion: '' })

async function load() {
  loading.value = true
  try {
    page.value = await getReviewList(query)
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
  query.contractType = ''
  query.deptId = null
  query.pageNum = 1
  load()
}

function goDetail(row) {
  router.push(`/contracts/${row.id}`)
}

function openReview(row) {
  current.value = row
  reviewForm.action = 'APPROVE'
  reviewForm.opinion = ''
  drawerVisible.value = true
}

async function submitReview() {
  if (reviewForm.action === 'REJECT' && !reviewForm.opinion.trim()) {
    ElMessage.warning('驳回时必须填写审核意见')
    return
  }
  try {
    await ElMessageBox.confirm(
      reviewForm.action === 'APPROVE'
        ? '确认审核通过？通过后将生成正式档案编号，档案内容不可直接修改。'
        : '确认驳回归档申请？申请人可根据意见补充资料后重新提交。',
      reviewForm.action === 'APPROVE' ? '通过归档' : '驳回归档',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  submitting.value = true
  try {
    await reviewContract(current.value.id, { ...reviewForm })
    ElMessage.success(reviewForm.action === 'APPROVE' ? '归档审核通过，档案已生成' : '已驳回归档申请')
    drawerVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
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
.review-head {
  padding: 18px 20px;
  background: linear-gradient(120deg, #f5f8ff, #fbfcff);
  border: 1px solid #e8eefc;
  border-radius: 12px;
  margin-bottom: 18px;
}
.review-name {
  font-size: 17px;
  font-weight: 700;
  line-height: 1.5;
}
.review-meta {
  font-size: 12px;
  color: #8b96ab;
  margin-top: 6px;
}
.review-amount {
  font-size: 22px;
  color: #2f6bff;
  margin-top: 12px;
}
.review-section-title {
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: 700;
  margin: 18px 0 10px;
  padding-left: 9px;
  border-left: 3px solid #2f6bff;
}
.review-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 18px;
}
.review-grid > div {
  display: flex;
  gap: 8px;
  font-size: 12.5px;
  line-height: 1.6;
}
.review-grid span {
  color: #8b96ab;
  flex: 0 0 64px;
}
.review-grid b {
  font-weight: 500;
  word-break: break-all;
}
.attachment-box {
  border: 1px solid #eef1f7;
  border-radius: 10px;
  padding: 14px;
  background: #fcfdff;
}
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
