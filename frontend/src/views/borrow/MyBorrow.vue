<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <h2 class="page-title">我的借阅</h2>
        <p class="page-desc">查看本人档案借阅申请与审批进度。借阅审批通过后，可在有效期限内下载档案原件。</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" @click="$router.push('/ledger')">
          <el-icon><Plus /></el-icon> 发起借阅
        </el-button>
        <el-button :loading="loading" @click="load"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </div>
    </div>

    <div class="borrow-tips">
      <div class="tip-item">
        <el-icon><Timer /></el-icon>
        <div><b>单次最长 90 天</b><span>到期后下载权限自动失效</span></div>
      </div>
      <div class="tip-item">
        <el-icon><Lock /></el-icon>
        <div><b>审批后可下载</b><span>未获批准的档案仅支持在线预览</span></div>
      </div>
      <div class="tip-item">
        <el-icon><DocumentChecked /></el-icon>
        <div><b>全程留痕</b><span>借阅记录进入台账并接受审计</span></div>
      </div>
    </div>

    <div class="panel-card table-card">
      <div class="status-tabs">
        <el-radio-group v-model="query.status" size="small" @change="search">
          <el-radio-button value="">全部记录</el-radio-button>
          <el-radio-button value="PENDING">待审批</el-radio-button>
          <el-radio-button value="APPROVED">借阅中</el-radio-button>
          <el-radio-button value="RETURNED">已归还</el-radio-button>
          <el-radio-button value="REJECTED">已驳回</el-radio-button>
          <el-radio-button value="EXPIRED">已到期</el-radio-button>
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
        <el-table-column label="档案" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="contract-cell">
              <span class="link-text" @click="$router.push(`/contracts/${row.contractId}`)">{{ row.contractName }}</span>
              <span class="muted">{{ row.contractNo }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="purpose" label="借阅用途" min-width="190" show-overflow-tooltip />
        <el-table-column label="借阅期限" width="266">
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
        <el-table-column prop="approverName" label="审批人" width="90">
          <template #default="{ row }">{{ row.approverName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="approveOpinion" label="审批意见" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.approveOpinion || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/contracts/${row.contractId}`)">
              查看档案
            </el-button>
            <el-button
              v-if="row.canReturn"
              link
              type="success"
              size="small"
              @click="handleReturn(row)"
            >归还</el-button>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyBorrows, returnBorrow } from '@/api/borrow'
import { formatDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

const loading = ref(false)
const page = ref({ records: [], total: 0 })
const query = reactive({ pageNum: 1, pageSize: 10, status: '', keyword: '' })

async function load() {
  loading.value = true
  try {
    page.value = await getMyBorrows(query)
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

async function handleReturn(row) {
  try {
    await ElMessageBox.confirm(
      `确认归还档案“${row.contractName}”？归还后本次借阅下载权限将立即失效。`,
      '归还档案',
      { type: 'warning', confirmButtonText: '确认归还', cancelButtonText: '取消' }
    )
    await returnBorrow(row.id)
    ElMessage.success('档案已归还')
    load()
  } catch {
    // cancelled
  }
}

onMounted(load)
</script>

<style scoped>
.borrow-tips {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}
.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: #fff;
  border: 1px solid #e7ebf3;
  border-radius: 11px;
  padding: 14px 16px;
  box-shadow: 0 8px 30px rgba(30, 52, 97, 0.05);
}
.tip-item > .el-icon {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: #edf3ff;
  color: #2f6bff;
  font-size: 16px;
  flex: 0 0 32px;
}
.tip-item b {
  display: block;
  font-size: 12.5px;
  margin-bottom: 3px;
}
.tip-item span {
  font-size: 11px;
  color: #8b96ab;
}
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
.period-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 11.5px;
}
@media (max-width: 900px) {
  .borrow-tips {
    grid-template-columns: 1fr;
  }
}
</style>
