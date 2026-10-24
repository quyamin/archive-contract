<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <h2 class="page-title">风险预警</h2>
        <p class="page-desc">系统每日自动扫描合同到期与付款节点，按配置的提前天数推送预警消息，帮助业务提前处置风险。</p>
      </div>
      <div class="page-actions">
        <el-button v-if="userStore.hasPerm('warning:manage')" :loading="scanning" @click="handleScan">
          <el-icon><Refresh /></el-icon> 执行扫描
        </el-button>
        <el-button
          v-if="unreadCount > 0"
          type="primary"
          plain
          :loading="markingAll"
          @click="handleReadAll"
        >
          <el-icon><Select /></el-icon> 全部标为已读
        </el-button>
        <el-button :loading="loading" @click="loadAll"><el-icon><RefreshRight /></el-icon> 刷新</el-button>
      </div>
    </div>

    <div class="warning-overview">
      <div class="warning-stat unread">
        <div class="ws-icon"><el-icon><Bell /></el-icon></div>
        <div>
          <div class="ws-value">{{ unreadCount }}</div>
          <div class="ws-label">未读预警</div>
        </div>
      </div>
      <div class="warning-stat expiry">
        <div class="ws-icon"><el-icon><Calendar /></el-icon></div>
        <div>
          <div class="ws-value">{{ expiryOnPage }}</div>
          <div class="ws-label">本页合同到期预警</div>
        </div>
      </div>
      <div class="warning-stat payment">
        <div class="ws-icon"><el-icon><Money /></el-icon></div>
        <div>
          <div class="ws-value">{{ paymentOnPage }}</div>
          <div class="ws-label">本页付款节点预警</div>
        </div>
      </div>
      <div class="warning-stat total">
        <div class="ws-icon"><el-icon><Tickets /></el-icon></div>
        <div>
          <div class="ws-value">{{ page.total }}</div>
          <div class="ws-label">预警记录总数</div>
        </div>
      </div>
    </div>

    <div class="panel-card table-card">
      <div class="warning-filter">
        <el-radio-group v-model="query.warningType" size="small" @change="search">
          <el-radio-button value="">全部类型</el-radio-button>
          <el-radio-button value="EXPIRY">合同到期</el-radio-button>
          <el-radio-button value="PAYMENT">付款节点</el-radio-button>
        </el-radio-group>
        <div class="filter-right">
          <el-select v-model="query.readStatus" placeholder="全部状态" clearable size="small" style="width: 116px" @change="search">
            <el-option label="未读" :value="0" />
            <el-option label="已读" :value="1" />
          </el-select>
          <el-input
            v-model="query.keyword"
            placeholder="搜索合同编号 / 名称 / 预警内容"
            clearable
            size="small"
            style="width: 250px"
            @keyup.enter="search"
            @clear="search"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
      </div>

      <div v-loading="loading" class="warning-list">
        <template v-if="page.records.length">
          <div
            v-for="item in page.records"
            :key="item.id"
            class="warning-card"
            :class="{ unread: !item.readStatus, payment: item.warningType === 'PAYMENT' }"
          >
            <div class="warning-left">
              <div class="warning-icon" :class="item.warningType.toLowerCase()">
                <el-icon><Calendar v-if="item.warningType === 'EXPIRY'" /><Money v-else /></el-icon>
              </div>
              <div class="warning-content">
                <div class="warning-title">
                  <el-tag size="small" :type="item.warningType === 'EXPIRY' ? 'warning' : 'danger'" effect="plain">
                    {{ item.warningType === 'EXPIRY' ? '合同到期' : '付款节点' }}
                  </el-tag>
                  <span class="warning-contract link-text" @click="$router.push(`/contracts/${item.contractId}`)">
                    {{ item.contractNo }} · {{ item.contractName }}
                  </span>
                  <el-badge v-if="!item.readStatus" is-dot type="danger" />
                </div>
                <div class="warning-message">{{ item.message }}</div>
                <div class="warning-meta">
                  <span><el-icon><Clock /></el-icon> 预警日期 {{ formatDate(item.warningDate) }}</span>
                  <span>生成时间 {{ formatDateTime(item.createTime) }}</span>
                </div>
              </div>
            </div>
            <div class="warning-actions">
              <el-button size="small" @click="viewContract(item)">查看合同</el-button>
              <el-button v-if="!item.readStatus" size="small" type="primary" plain @click="markRead(item)">
                标为已读
              </el-button>
              <el-tag v-else size="small" type="info" effect="plain">已读</el-tag>
            </div>
          </div>
        </template>
        <el-empty v-else description="暂无预警记录，合同状态良好" />
      </div>

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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getWarnings,
  getUnreadCount,
  markWarningRead,
  markAllWarningsRead,
  scanWarnings
} from '@/api/warning'
import { formatDate, formatDateTime } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const scanning = ref(false)
const markingAll = ref(false)
const page = ref({ records: [], total: 0 })
const unreadCount = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, warningType: '', readStatus: null, keyword: '' })

const expiryOnPage = computed(() => page.value.records.filter((item) => item.warningType === 'EXPIRY').length)
const paymentOnPage = computed(() => page.value.records.filter((item) => item.warningType === 'PAYMENT').length)

async function load() {
  loading.value = true
  try {
    page.value = await getWarnings(query)
  } finally {
    loading.value = false
  }
}

async function loadUnread() {
  unreadCount.value = await getUnreadCount()
}

function search() {
  query.pageNum = 1
  load()
}

async function loadAll() {
  await Promise.all([load(), loadUnread()])
}

function viewContract(item) {
  if (!item.readStatus) markRead(item, false)
  router.push(`/contracts/${item.contractId}`)
}

async function markRead(item, notify = true) {
  await markWarningRead(item.id)
  item.readStatus = 1
  if (unreadCount.value > 0) unreadCount.value -= 1
  window.dispatchEvent(new CustomEvent('warnings-updated'))
  if (notify) ElMessage.success('已标为已读')
}

async function handleReadAll() {
  markingAll.value = true
  try {
    await markAllWarningsRead()
    unreadCount.value = 0
    page.value.records.forEach((item) => { item.readStatus = 1 })
    window.dispatchEvent(new CustomEvent('warnings-updated'))
    ElMessage.success('全部预警已标为已读')
  } finally {
    markingAll.value = false
  }
}

async function handleScan() {
  try {
    await ElMessageBox.confirm(
      '系统将立即扫描合同到期与付款节点风险，并按规则向相关负责人推送预警。是否继续？',
      '执行预警扫描',
      { type: 'info', confirmButtonText: '开始扫描', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  scanning.value = true
  try {
    const result = await scanWarnings()
    ElMessage.success(
      `扫描完成：新增合同到期预警 ${result.expiryWarnings || 0} 条、付款节点预警 ${result.paymentWarnings || 0} 条`
    )
    await loadAll()
  } finally {
    scanning.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.warning-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}
.warning-stat {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e7ebf3;
  box-shadow: 0 8px 30px rgba(30, 52, 97, 0.05);
}
.ws-icon {
  width: 42px;
  height: 42px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex: 0 0 42px;
}
.warning-stat.unread .ws-icon {
  background: #fff0f0;
  color: #f05b5b;
}
.warning-stat.expiry .ws-icon {
  background: #fff6e8;
  color: #e6a23c;
}
.warning-stat.payment .ws-icon {
  background: #fdf0f5;
  color: #d9538a;
}
.warning-stat.total .ws-icon {
  background: #edf3ff;
  color: #2f6bff;
}
.ws-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.1;
}
.ws-label {
  font-size: 11.5px;
  color: #8b96ab;
  margin-top: 5px;
}
.warning-filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.filter-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.warning-list {
  min-height: 200px;
}
.warning-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 16px 16px;
  border: 1px solid #eef1f7;
  border-radius: 11px;
  margin-bottom: 10px;
  transition: all 0.16s;
  background: #fff;
}
.warning-card:hover {
  border-color: #d5e2ff;
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(47, 107, 255, 0.07);
}
.warning-card.unread {
  background: linear-gradient(90deg, #fffdf8, #ffffff 36%);
  border-left: 3px solid #e6a23c;
}
.warning-card.unread.payment {
  background: linear-gradient(90deg, #fff8fa, #ffffff 36%);
  border-left-color: #f05b5b;
}
.warning-left {
  display: flex;
  gap: 13px;
  min-width: 0;
  flex: 1;
}
.warning-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 38px;
  font-size: 18px;
}
.warning-icon.expiry {
  background: #fff6e8;
  color: #e6a23c;
}
.warning-icon.payment {
  background: #fff0f0;
  color: #f05b5b;
}
.warning-content {
  min-width: 0;
}
.warning-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.warning-contract {
  font-size: 13px;
  font-weight: 600;
}
.warning-message {
  font-size: 12.5px;
  color: #4c5872;
  line-height: 1.7;
  margin-top: 7px;
}
.warning-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 11px;
  color: #98a2b3;
  margin-top: 8px;
}
.warning-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.warning-actions {
  display: flex;
  align-items: center;
  gap: 7px;
  flex: 0 0 auto;
}
@media (max-width: 1100px) {
  .warning-overview {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 700px) {
  .warning-card {
    flex-direction: column;
  }
  .warning-overview {
    grid-template-columns: 1fr;
  }
}
</style>
