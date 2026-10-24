<template>
  <div class="page-container">
    <div class="print-only print-title">
      <h2>企业档案合同资料台账</h2>
      <div class="print-meta">
        打印时间：{{ printTime }} · 当前分类：{{ currentCategoryLabel }} · 记录数：{{ page.total }}
      </div>
    </div>

    <div class="page-heading">
      <div>
        <h2 class="page-title">档案台账</h2>
        <p class="page-desc">按全生命周期分类查看企业合同档案，支持组合筛选、导出 Excel 与台账打印。</p>
      </div>
      <div class="page-actions">
        <el-button v-if="userStore.hasPerm('ledger:export')" :loading="exporting" @click="handleExport">
          <el-icon><Download /></el-icon> 导出台账
        </el-button>
        <el-button v-if="userStore.hasPerm('ledger:print')" @click="handlePrint">
          <el-icon><Printer /></el-icon> 打印台账
        </el-button>
      </div>
    </div>

    <div class="ledger-tabs">
      <button
        v-for="item in categories"
        :key="item.value"
        class="ledger-tab"
        :class="{ active: query.category === item.value }"
        @click="switchCategory(item.value)"
      >
        <span class="tab-label">{{ item.label }}</span>
        <span class="tab-count">{{ counts[item.value || 'all'] ?? '—' }}</span>
      </button>
    </div>

    <div class="panel-card filter-card no-print">
      <el-form :model="query" label-width="72px" inline @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="合同编号 / 名称 / 签约方 / 档案号"
            clearable
            style="width: 248px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="档案编号">
          <el-input v-model="query.archiveNo" placeholder="档案编号" clearable style="width: 158px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="合同类型">
          <el-select v-model="query.contractType" placeholder="全部类型" clearable style="width: 136px">
            <el-option v-for="item in typeOptions" :key="item.itemValue" :label="item.itemLabel" :value="item.itemValue" />
          </el-select>
        </el-form-item>
        <el-form-item label="归属部门">
          <el-select v-model="query.deptId" placeholder="全部部门" clearable style="width: 136px">
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
            style="width: 250px"
          />
        </el-form-item>
        <el-form-item label="合同金额">
          <div class="range-inputs">
            <el-input-number v-model="query.amountMin" :min="0" :controls="false" placeholder="最低" style="width: 104px" />
            <span class="muted">—</span>
            <el-input-number v-model="query.amountMax" :min="0" :controls="false" placeholder="最高" style="width: 104px" />
          </div>
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

    <div class="panel-card table-card ledger-card">
      <div class="table-toolbar no-print">
        <div class="table-toolbar-title">{{ currentCategoryLabel }}</div>
        <div class="table-toolbar-actions">
          <span class="muted">共 {{ page.total }} 份档案</span>
          <el-button circle size="small" :loading="loading" @click="load"><el-icon><Refresh /></el-icon></el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="page.records" stripe show-summary :summary-method="summaryMethod">
        <el-table-column type="index" label="序号" width="48" align="center" />
        <el-table-column prop="archiveNo" label="档案编号" width="118" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="archive-code">{{ row.archiveNo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contractNo" label="合同编号" width="112" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="link-text" @click="$router.push(`/contracts/${row.id}`)">{{ row.contractNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contractName" label="合同名称" min-width="138" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="link-text" @click="$router.push(`/contracts/${row.id}`)">{{ row.contractName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contractTypeName" label="类型" width="84" />
        <el-table-column prop="partyB" label="签约对方" min-width="118" show-overflow-tooltip />
        <el-table-column prop="amount" label="合同金额" width="112" align="right">
          <template #default="{ row }"><span class="money">{{ formatMoney(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column prop="deptName" label="归属部门" width="88" />
        <el-table-column prop="securityLevelName" label="密级" width="68" />
        <el-table-column prop="signDate" label="签订日期" width="94">
          <template #default="{ row }">{{ formatDate(row.signDate) }}</template>
        </el-table-column>
        <el-table-column prop="expireDate" label="到期日期" width="94">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.expiringSoon }">{{ formatDate(row.expireDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="128" fixed="right" class-name="op-cell no-print">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/contracts/${row.id}`)">详情</el-button>
            <el-button
              v-if="['ARCHIVED', 'EXPIRED'].includes(row.status) && userStore.hasPerm('change:apply')"
              link
              type="primary"
              size="small"
              @click="$router.push(`/contracts/${row.id}?action=change`)"
            >变更</el-button>
            <el-button
              v-if="['ARCHIVED', 'EXPIRED'].includes(row.status) && userStore.hasPerm('borrow:apply')"
              link
              type="success"
              size="small"
              @click="$router.push(`/contracts/${row.id}?action=borrow`)"
            >借阅</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap no-print">
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'
import { getLedger, exportLedger } from '@/api/ledger'
import { getOptionDepts, getOptionDict } from '@/api/system'
import { downloadResponse } from '@/utils/download'
import { formatMoney, formatDate } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

const userStore = useUserStore()
const loading = ref(false)
const exporting = ref(false)
const page = ref({ records: [], total: 0 })
const signDateRange = ref([])
const typeOptions = ref([])
const deptOptions = ref([])
const counts = ref({})
const printTime = ref('')

const categories = [
  { label: '全部档案', value: '' },
  { label: '待归档', value: 'draft' },
  { label: '审核中', value: 'reviewing' },
  { label: '已驳回', value: 'rejected' },
  { label: '已归档', value: 'archived' },
  { label: '已到期', value: 'expired' }
]

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  category: '',
  keyword: '',
  archiveNo: '',
  contractType: '',
  deptId: null,
  amountMin: null,
  amountMax: null,
  expiringSoon: false,
  signDateStart: '',
  signDateEnd: ''
})

const currentCategoryLabel = computed(
  () => categories.find((item) => item.value === query.category)?.label || '全部档案'
)

function buildParams(extra = {}) {
  const params = { ...query, ...extra }
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
    page.value = await getLedger(buildParams())
  } finally {
    loading.value = false
  }
}

async function loadCounts() {
  const base = buildParams({ pageSize: 1, pageNum: 1 })
  const results = await Promise.all(
    categories.map(async (item) => {
      const data = await getLedger({ ...base, category: item.value })
      return [item.value || 'all', data.total]
    })
  )
  counts.value = Object.fromEntries(results)
}

function switchCategory(category) {
  query.category = category
  query.pageNum = 1
  load()
}

function search() {
  query.pageNum = 1
  load()
  loadCounts()
}

function reset() {
  Object.assign(query, {
    pageNum: 1,
    category: query.category,
    keyword: '',
    archiveNo: '',
    contractType: '',
    deptId: null,
    amountMin: null,
    amountMax: null,
    expiringSoon: false
  })
  signDateRange.value = []
  load()
  loadCounts()
}

function summaryMethod({ columns, data }) {
  return columns.map((column, index) => {
    if (index === 0) return '本页合计'
    if (column.property === 'amount') {
      const total = data.reduce((sum, row) => sum + Number(row.amount || 0), 0)
      return formatMoney(total)
    }
    return ''
  })
}

async function handleExport() {
  exporting.value = true
  try {
    const response = await exportLedger(buildParams())
    downloadResponse(response, `档案资料台账_${currentCategoryLabel.value}_${dayjs().format('YYYYMMDD')}.xlsx`)
    ElMessage.success('台账导出成功')
  } finally {
    exporting.value = false
  }
}

function handlePrint() {
  printTime.value = dayjs().format('YYYY-MM-DD HH:mm')
  setTimeout(() => window.print(), 80)
}

async function loadOptions() {
  const [types, depts] = await Promise.all([getOptionDict('contract_type'), getOptionDepts()])
  typeOptions.value = types || []
  deptOptions.value = depts || []
}

onMounted(() => {
  loadOptions()
  load()
  loadCounts()
})
</script>

<style scoped>
.ledger-tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.ledger-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 10px;
  border: 1px solid #e7ebf3;
  background: #fff;
  cursor: pointer;
  transition: all 0.16s;
  box-shadow: 0 4px 14px rgba(30, 52, 97, 0.04);
}
.ledger-tab:hover {
  border-color: #b9cdff;
  color: #2f6bff;
}
.ledger-tab.active {
  background: linear-gradient(120deg, #2f6bff, #5d8cff);
  border-color: #2f6bff;
  color: #fff;
  box-shadow: 0 8px 18px rgba(47, 107, 255, 0.24);
}
.tab-label {
  font-size: 12.5px;
  font-weight: 500;
}
.tab-count {
  min-width: 22px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: #f2f5fb;
  color: #526079;
  font-size: 10.5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}
.ledger-tab.active .tab-count {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}
.range-inputs {
  display: flex;
  align-items: center;
  gap: 5px;
}
.archive-code {
  font-family: "Roboto Mono", monospace;
  font-size: 11.5px;
  color: #2f6bff;
}
:deep(.op-cell .cell) {
  padding: 0 4px;
}
:deep(.op-cell .el-button + .el-button) {
  margin-left: 6px;
}
.print-title {
  display: none;
}
@media print {
  .ledger-tabs {
    display: none;
  }
  .print-title {
    display: block;
    text-align: center;
    margin-bottom: 14px;
  }
  .print-title h2 {
    margin: 0 0 8px;
    font-size: 20px;
  }
  .print-meta {
    font-size: 12px;
    color: #555;
  }
}
</style>
