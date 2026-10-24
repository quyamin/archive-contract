<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <h2 class="page-title">统计分析</h2>
        <p class="page-desc">从合同类型、部门、年度、金额区间与签约伙伴等维度分析合同资产结构，为经营决策提供依据。</p>
      </div>
      <div class="page-actions">
        <el-button v-if="userStore.hasPerm('contract:export')" @click="$router.push('/contracts')">
          <el-icon><DocumentCopy /></el-icon> 合同明细
        </el-button>
        <el-button v-if="userStore.hasPerm('stats:export')" type="primary" :loading="exporting" @click="handleExport">
          <el-icon><Download /></el-icon> 导出统计报表
        </el-button>
      </div>
    </div>

    <div class="panel-card filter-card">
      <el-form :model="query" label-width="72px" inline @submit.prevent>
        <el-form-item label="统计期间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="归属部门">
          <el-select v-model="query.deptId" placeholder="全部部门" clearable style="width: 150px">
            <el-option v-for="item in deptOptions" :key="item.id" :label="item.deptName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同类型">
          <el-select v-model="query.contractType" placeholder="全部类型" clearable style="width: 150px">
            <el-option v-for="item in typeOptions" :key="item.itemValue" :label="item.itemLabel" :value="item.itemValue" />
          </el-select>
        </el-form-item>
        <el-form-item label-width="0">
          <el-button type="primary" :loading="loading" @click="load"><el-icon><Search /></el-icon> 开始分析</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="stat-grid">
      <div class="stat-card" style="--stat-color: #2f6bff; --stat-soft: #edf3ff">
        <div class="stat-label"><span class="stat-icon"><el-icon><Document /></el-icon></span>合同总数</div>
        <div class="stat-value">{{ stats.totalCount || 0 }}</div>
        <div class="stat-extra">{{ queryLabel }}</div>
      </div>
      <div class="stat-card" style="--stat-color: #20b26c; --stat-soft: #e9f9f1">
        <div class="stat-label"><span class="stat-icon"><el-icon><Money /></el-icon></span>合同总额</div>
        <div class="stat-value compact">{{ formatCompactMoney(stats.totalAmount) }}</div>
        <div class="stat-extra">{{ formatMoney(stats.totalAmount) }}</div>
      </div>
      <div class="stat-card" style="--stat-color: #8b5cf6; --stat-soft: #f3edff">
        <div class="stat-label"><span class="stat-icon"><el-icon><Histogram /></el-icon></span>平均合同金额</div>
        <div class="stat-value compact">{{ formatCompactMoney(avgAmount) }}</div>
        <div class="stat-extra">按合同数量平均测算</div>
      </div>
      <div class="stat-card" style="--stat-color: #e6a23c; --stat-soft: #fff6e8">
        <div class="stat-label"><span class="stat-icon"><el-icon><OfficeBuilding /></el-icon></span>覆盖部门</div>
        <div class="stat-value">{{ (stats.deptStats || []).length }}</div>
        <div class="stat-extra">涉及 {{ (stats.partnerStats || []).length }} 家签约伙伴</div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="panel-card chart-card">
        <div class="chart-head">
          <div>
            <div class="chart-title">合同类型金额分布</div>
            <div class="chart-subtitle">各业务类型合同金额占比</div>
          </div>
        </div>
        <ChartBox :option="typeOption" height="290px" />
      </div>
      <div class="panel-card chart-card">
        <div class="chart-head">
          <div>
            <div class="chart-title">部门合同金额</div>
            <div class="chart-subtitle">按归属部门统计合同金额（万元）</div>
          </div>
        </div>
        <ChartBox :option="deptOption" height="290px" />
      </div>
      <div class="panel-card chart-card">
        <div class="chart-head">
          <div>
            <div class="chart-title">年度合同趋势</div>
            <div class="chart-subtitle">按签订年度统计数量与金额</div>
          </div>
        </div>
        <ChartBox :option="yearOption" height="290px" />
      </div>
      <div class="panel-card chart-card">
        <div class="chart-head">
          <div>
            <div class="chart-title">金额区间分布</div>
            <div class="chart-subtitle">合同金额区间数量结构</div>
          </div>
        </div>
        <ChartBox :option="amountRangeOption" height="290px" />
      </div>
      <div class="panel-card chart-card">
        <div class="chart-head">
          <div>
            <div class="chart-title">月度签订走势</div>
            <div class="chart-subtitle">按签订月份统计合同数量与金额</div>
          </div>
        </div>
        <ChartBox :option="monthOption" height="290px" />
      </div>
      <div class="panel-card chart-card">
        <div class="chart-head">
          <div>
            <div class="chart-title">重点签约伙伴 TOP 10</div>
            <div class="chart-subtitle">按乙方合同金额排行</div>
          </div>
        </div>
        <ChartBox :option="partnerOption" height="290px" />
      </div>
    </div>

    <div class="panel-card table-card partner-table">
      <div class="table-toolbar">
        <div class="table-toolbar-title">签约伙伴明细</div>
        <span class="muted">共 {{ (stats.partnerStats || []).length }} 家合作伙伴</span>
      </div>
      <el-table :data="stats.partnerStats || []" size="small" stripe>
        <el-table-column type="index" label="排名" width="64" />
        <el-table-column prop="name" label="签约伙伴（乙方）" min-width="240" />
        <el-table-column prop="count" label="合同数量" width="110" align="center" />
        <el-table-column prop="amount" label="合同金额" width="160" align="right">
          <template #default="{ row }"><span class="money">{{ formatMoney(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column label="金额占比" min-width="200">
          <template #default="{ row }">
            <div class="ratio-cell">
              <el-progress :percentage="ratio(row.amount)" :show-text="false" :stroke-width="7" color="#2f6bff" />
              <span>{{ ratio(row.amount) }}%</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'
import { getStats, exportStats } from '@/api/stats'
import { getOptionDepts, getOptionDict } from '@/api/system'
import { downloadResponse } from '@/utils/download'
import { formatMoney, formatCompactMoney } from '@/utils/format'
import ChartBox from '@/components/ChartBox.vue'

const userStore = useUserStore()
const loading = ref(false)
const exporting = ref(false)
const stats = ref({})
const dateRange = ref([])
const deptOptions = ref([])
const typeOptions = ref([])
const query = reactive({ deptId: null, contractType: '' })

const palette = ['#2f6bff', '#20b26c', '#e6a23c', '#8b5cf6', '#06b6d4', '#f05b5b', '#64748b', '#f59e0b', '#14b8a6', '#6366f1']

const avgAmount = computed(() => {
  const count = Number(stats.value.totalCount || 0)
  if (!count) return 0
  return Number(stats.value.totalAmount || 0) / count
})

const queryLabel = computed(() => {
  const parts = []
  if (dateRange.value?.length === 2) parts.push(`${dateRange.value[0]} 至 ${dateRange.value[1]}`)
  if (query.contractType) {
    parts.push(typeOptions.value.find((item) => item.itemValue === query.contractType)?.itemLabel || query.contractType)
  }
  if (query.deptId) {
    parts.push(deptOptions.value.find((item) => item.id === query.deptId)?.deptName || '')
  }
  return parts.length ? parts.join(' · ') : '全部合同档案'
})

function buildParams() {
  const params = { deptId: query.deptId || null, contractType: query.contractType || '' }
  if (dateRange.value?.length === 2) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  return params
}

function ratio(amount) {
  const total = Number(stats.value.totalAmount || 0)
  if (!total) return 0
  return Number(((Number(amount || 0) / total) * 100).toFixed(1))
}

const typeOption = computed(() => {
  const items = stats.value.typeStats || []
  return {
    color: palette,
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const item = items[params.dataIndex] || {}
        return `${params.marker}${params.name}<br/>合同数量：${item.count || 0} 份<br/>合同金额：${formatCompactMoney(item.amount)}<br/>金额占比：${params.percent}%`
      }
    },
    legend: { type: 'scroll', bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11 } },
    series: [
      {
        type: 'pie',
        radius: ['44%', '68%'],
        center: ['50%', '44%'],
        itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 4 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 13, fontWeight: 600, formatter: '{b}\n{d}%' } },
        data: items.map((item) => ({ name: item.name, value: Number(item.amount || 0) }))
      }
    ]
  }
})

const deptOption = computed(() => {
  const items = [...(stats.value.deptStats || [])].sort((a, b) => Number(a.amount || 0) - Number(b.amount || 0))
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const item = items[params[0]?.dataIndex ?? 0] || {}
        return `${item.name}<br/>合同数量：${item.count || 0} 份<br/>合同金额：${formatCompactMoney(item.amount)}`
      }
    },
    grid: { left: 10, right: 60, top: 16, bottom: 10, containLabel: true },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f1f4fa' } },
      axisLabel: { color: '#8b96ab', fontSize: 10.5, formatter: (value) => `${(value / 10000).toFixed(0)}万` }
    },
    yAxis: {
      type: 'category',
      data: items.map((item) => item.name),
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { color: '#4c5872', fontSize: 11.5 }
    },
    series: [
      {
        type: 'bar',
        barWidth: 13,
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: {
            type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [{ offset: 0, color: '#9dbcff' }, { offset: 1, color: '#2f6bff' }]
          }
        },
        label: {
          show: true,
          position: 'right',
          fontSize: 10.5,
          color: '#8b96ab',
          formatter: (params) => formatCompactMoney(items[params.dataIndex]?.amount)
        },
        data: items.map((item) => Number(item.amount || 0))
      }
    ]
  }
})

const yearOption = computed(() => {
  const items = stats.value.yearStats || []
  return {
    color: ['#2f6bff', '#8b5cf6'],
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const index = params[0]?.dataIndex ?? 0
        const item = items[index] || {}
        return `${item.name} 年<br/>合同数量：${item.count || 0} 份<br/>合同金额：${formatCompactMoney(item.amount)}`
      }
    },
    grid: { left: 12, right: 16, top: 36, bottom: 10, containLabel: true },
    xAxis: {
      type: 'category',
      data: items.map((item) => item.name),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e7ebf3' } },
      axisLabel: { color: '#8b96ab', fontSize: 11 }
    },
    yAxis: [
      { type: 'value', name: '金额', splitLine: { lineStyle: { color: '#f1f4fa' } }, axisLabel: { color: '#8b96ab', fontSize: 10.5, formatter: (v) => `${(v / 10000).toFixed(0)}万` } },
      { type: 'value', name: '数量', splitLine: { show: false }, axisLabel: { color: '#8b96ab', fontSize: 10.5 } }
    ],
    series: [
      {
        name: '合同金额',
        type: 'bar',
        barWidth: 28,
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [{ offset: 0, color: '#5d8cff' }, { offset: 1, color: '#b9cdff' }]
          }
        },
        data: items.map((item) => Number(item.amount || 0))
      },
      {
        name: '合同数量',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 2.5, color: '#8b5cf6' },
        itemStyle: { color: '#8b5cf6' },
        data: items.map((item) => item.count || 0)
      }
    ]
  }
})

const amountRangeOption = computed(() => {
  const items = stats.value.amountRangeStats || []
  return {
    color: ['#06b6d4', '#2f6bff', '#8b5cf6', '#e6a23c', '#f05b5b'],
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const item = items[params.dataIndex] || {}
        return `${params.marker}${params.name}<br/>合同数量：${item.count || 0} 份<br/>合同金额：${formatCompactMoney(item.amount)}<br/>数量占比：${params.percent}%`
      }
    },
    legend: { type: 'scroll', bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11 } },
    series: [
      {
        type: 'pie',
        radius: ['40%', '66%'],
        center: ['50%', '45%'],
        roseType: 'radius',
        itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 12, formatter: '{b}\n{d}%' } },
        data: items.map((item) => ({ name: item.name, value: item.count || 0 }))
      }
    ]
  }
})

const monthOption = computed(() => {
  const items = stats.value.monthStats || []
  return {
    color: ['#20b26c', '#2f6bff'],
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const index = params[0]?.dataIndex ?? 0
        const item = items[index] || {}
        return `${item.name}<br/>合同数量：${item.count || 0} 份<br/>合同金额：${formatCompactMoney(item.amount)}`
      }
    },
    grid: { left: 12, right: 16, top: 36, bottom: 10, containLabel: true },
    xAxis: {
      type: 'category',
      data: items.map((item) => item.name),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e7ebf3' } },
      axisLabel: { color: '#8b96ab', fontSize: 10, rotate: items.length > 8 ? 30 : 0 }
    },
    yAxis: [
      { type: 'value', name: '数量', splitLine: { lineStyle: { color: '#f1f4fa' } }, axisLabel: { color: '#8b96ab', fontSize: 10.5 } },
      { type: 'value', name: '万元', splitLine: { show: false }, axisLabel: { color: '#8b96ab', fontSize: 10.5, formatter: (v) => (v / 10000).toFixed(0) } }
    ],
    series: [
      {
        name: '合同数量',
        type: 'bar',
        barWidth: 16,
        itemStyle: { borderRadius: [5, 5, 0, 0] },
        data: items.map((item) => item.count || 0)
      },
      {
        name: '合同金额',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        areaStyle: { opacity: 0.08 },
        data: items.map((item) => Number(item.amount || 0))
      }
    ]
  }
})

const partnerOption = computed(() => {
  const items = [...(stats.value.partnerStats || [])]
    .sort((a, b) => Number(a.amount || 0) - Number(b.amount || 0))
    .slice(-10)
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const item = items[params[0]?.dataIndex ?? 0] || {}
        return `${item.name}<br/>合同数量：${item.count || 0} 份<br/>合同金额：${formatCompactMoney(item.amount)}`
      }
    },
    grid: { left: 10, right: 64, top: 12, bottom: 8, containLabel: true },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f1f4fa' } },
      axisLabel: { color: '#8b96ab', fontSize: 10.5, formatter: (v) => `${(v / 10000).toFixed(0)}万` }
    },
    yAxis: {
      type: 'category',
      data: items.map((item) => item.name.length > 12 ? `${item.name.slice(0, 12)}…` : item.name),
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { color: '#4c5872', fontSize: 11 }
    },
    series: [
      {
        type: 'bar',
        barWidth: 11,
        itemStyle: {
          borderRadius: [0, 5, 5, 0],
          color: {
            type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [{ offset: 0, color: '#b8a3ff' }, { offset: 1, color: '#8b5cf6' }]
          }
        },
        label: {
          show: true,
          position: 'right',
          fontSize: 10,
          color: '#8b96ab',
          formatter: (params) => formatCompactMoney(items[params.dataIndex]?.amount)
        },
        data: items.map((item) => Number(item.amount || 0))
      }
    ]
  }
})

async function load() {
  loading.value = true
  try {
    stats.value = await getStats(buildParams())
  } finally {
    loading.value = false
  }
}

function reset() {
  dateRange.value = []
  query.deptId = null
  query.contractType = ''
  load()
}

async function handleExport() {
  exporting.value = true
  try {
    const response = await exportStats(buildParams())
    downloadResponse(response, `合同统计分析报表_${dayjs().format('YYYYMMDD')}.xlsx`)
    ElMessage.success('统计报表导出成功')
  } finally {
    exporting.value = false
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
.stat-value.compact {
  font-size: 24px;
}
.chart-grid {
  margin-bottom: 16px;
}
.partner-table {
  margin-top: 0;
}
.ratio-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11.5px;
  color: #8b96ab;
}
.ratio-cell .el-progress {
  flex: 1;
}
</style>
