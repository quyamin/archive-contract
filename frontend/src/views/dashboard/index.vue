<template>
  <div class="page-container" v-loading="loading">
    <div class="page-heading">
      <div>
        <h2 class="page-title">数据看板</h2>
        <p class="page-desc">{{ greeting }}，{{ userStore.displayName }}。以下是企业合同档案的整体运行概览。</p>
      </div>
      <div class="page-actions">
        <span class="date-chip"><el-icon><Calendar /></el-icon> {{ today }}</span>
        <el-button v-if="userStore.hasPerm('contract:add')" type="primary" @click="$router.push('/contracts/new')">
          <el-icon><Plus /></el-icon> 录入合同
        </el-button>
        <el-button v-if="userStore.hasPerm('ledger:view')" @click="$router.push('/ledger')">
          <el-icon><Collection /></el-icon> 档案台账
        </el-button>
      </div>
    </div>

    <div class="hero-card">
      <div class="hero-row">
        <div>
          <div class="hero-eyebrow">CONTRACT ASSETS · 合同资产总额</div>
          <div class="hero-amount">{{ formatMoney(data.totalAmount) }}</div>
          <div class="hero-desc">
            已归集 {{ data.archivedCount || 0 }} 份正式档案 · {{ data.totalContracts || 0 }} 份合同在册
          </div>
        </div>
        <div class="hero-side">
          <div class="hero-mini">
            <div class="label">待审核归档</div>
            <div class="value">{{ data.pendingReviewCount || 0 }}</div>
          </div>
          <div class="hero-mini">
            <div class="label">生效中借阅</div>
            <div class="value">{{ data.activeBorrowCount || 0 }}</div>
          </div>
          <div class="hero-mini">
            <div class="label">未读预警</div>
            <div class="value">{{ data.unreadWarnings || 0 }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card" style="--stat-color: #2f6bff; --stat-soft: #edf3ff">
        <div class="stat-label">
          <span class="stat-icon"><el-icon><Document /></el-icon></span>
          合同总量
        </div>
        <div class="stat-value">{{ data.totalContracts || 0 }}</div>
        <div class="stat-extra">覆盖 {{ typeCount }} 类合同业务</div>
      </div>
      <div class="stat-card" style="--stat-color: #20b26c; --stat-soft: #e9f9f1">
        <div class="stat-label">
          <span class="stat-icon"><el-icon><FolderChecked /></el-icon></span>
          正式归档
        </div>
        <div class="stat-value">{{ data.archivedCount || 0 }}</div>
        <div class="stat-extra">归档率 {{ archiveRate }}%</div>
      </div>
      <div class="stat-card" style="--stat-color: #e6a23c; --stat-soft: #fff6e8">
        <div class="stat-label">
          <span class="stat-icon"><el-icon><Timer /></el-icon></span>
          待审核
        </div>
        <div class="stat-value">{{ data.pendingReviewCount || 0 }}</div>
        <div class="stat-extra">等待档案管理员处理</div>
      </div>
      <div class="stat-card" style="--stat-color: #f05b5b; --stat-soft: #fff0f0">
        <div class="stat-label">
          <span class="stat-icon"><el-icon><Warning /></el-icon></span>
          临近到期
        </div>
        <div class="stat-value">{{ data.expiringCount || 0 }}</div>
        <div class="stat-extra">含合同到期与待付款节点</div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="panel-card chart-card">
        <div class="chart-head">
          <div>
            <div class="chart-title">合同类型分布</div>
            <div class="chart-subtitle">按合同金额构成统计</div>
          </div>
          <el-tag size="small" effect="plain">金额占比</el-tag>
        </div>
        <ChartBox :option="typeOption" height="280px" />
      </div>
      <div class="panel-card chart-card">
        <div class="chart-head">
          <div>
            <div class="chart-title">月度归档趋势</div>
            <div class="chart-subtitle">近 12 个月合同签订金额与数量</div>
          </div>
          <el-tag size="small" effect="plain">近一年</el-tag>
        </div>
        <ChartBox :option="monthOption" height="280px" />
      </div>
    </div>

    <div class="panel-card chart-card dept-chart">
      <div class="chart-head">
        <div>
          <div class="chart-title">部门合同金额排行</div>
          <div class="chart-subtitle">按归属部门统计合同金额（万元）</div>
        </div>
        <el-button v-if="userStore.hasPerm('stats:view')" link type="primary" @click="$router.push('/stats')">
          查看完整分析 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      <ChartBox :option="deptOption" height="250px" />
    </div>

    <div class="dashboard-bottom">
      <div class="panel-card table-card">
        <div class="table-toolbar">
          <div class="table-toolbar-title">最近合同</div>
          <el-button link type="primary" @click="$router.push('/contracts')">全部合同 <el-icon><ArrowRight /></el-icon></el-button>
        </div>
        <el-table :data="data.recentContracts || []" size="small">
          <el-table-column prop="contractName" label="合同名称" min-width="190" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="link-text" @click="$router.push(`/contracts/${row.id}`)">{{ row.contractName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="contractTypeName" label="类型" width="94" />
          <el-table-column prop="amount" label="金额" width="120" align="right">
            <template #default="{ row }"><span class="money">{{ formatCompactMoney(row.amount) }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="92">
            <template #default="{ row }"><StatusTag :status="row.status" /></template>
          </el-table-column>
          <el-table-column prop="signDate" label="签订日期" width="104">
            <template #default="{ row }">{{ formatDate(row.signDate) }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel-card warning-panel">
        <div class="table-toolbar">
          <div class="table-toolbar-title">最新风险预警</div>
          <el-button v-if="userStore.hasPerm('warning:view')" link type="primary" @click="$router.push('/warnings')">
            预警中心 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div v-if="(data.recentWarnings || []).length" class="recent-list">
          <div v-for="item in data.recentWarnings" :key="item.id" class="recent-item warning-item">
            <span class="warning-dot" :class="item.warningType"></span>
            <div style="min-width: 0; flex: 1">
              <div class="recent-title">{{ item.message }}</div>
              <div class="recent-time">
                {{ item.contractNo }} · {{ formatDate(item.warningDate) }}
                <el-tag v-if="!item.readStatus" size="small" type="danger" effect="plain" style="margin-left: 6px">未读</el-tag>
              </div>
            </div>
            <el-button link type="primary" size="small" @click="$router.push(`/contracts/${item.contractId}`)">查看</el-button>
          </div>
        </div>
        <div v-else class="empty-tip">暂无风险预警，档案状态良好</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'
import { getDashboard } from '@/api/stats'
import { formatMoney, formatCompactMoney, formatDate } from '@/utils/format'
import ChartBox from '@/components/ChartBox.vue'
import StatusTag from '@/components/StatusTag.vue'

const userStore = useUserStore()
const loading = ref(false)
const data = ref({})
const today = dayjs().format('YYYY年MM月DD日 dddd')

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
const typeCount = computed(() => (data.value.typeStats || []).length)
const archiveRate = computed(() => {
  const total = Number(data.value.totalContracts || 0)
  if (!total) return '0.0'
  return ((Number(data.value.archivedCount || 0) / total) * 100).toFixed(1)
})

const palette = ['#2f6bff', '#20b26c', '#e6a23c', '#8b5cf6', '#06b6d4', '#f05b5b', '#64748b']

const typeOption = computed(() => {
  const items = data.value.typeStats || []
  return {
    color: palette,
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const item = items[params.dataIndex] || {}
        return `${params.marker}${params.name}<br/>合同数量：${item.count || 0} 份<br/>合同金额：${formatCompactMoney(item.amount)}<br/>占比：${params.percent}%`
      }
    },
    legend: { type: 'scroll', bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11 } },
    series: [
      {
        type: 'pie',
        radius: ['46%', '69%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 4 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 13, fontWeight: 600, formatter: '{b}\n{d}%' } },
        data: items.map((item) => ({ name: item.name, value: Number(item.amount || 0) }))
      }
    ]
  }
})

const monthOption = computed(() => {
  const items = data.value.monthStats || []
  return {
    color: ['#2f6bff', '#20b26c'],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const index = params[0]?.dataIndex ?? 0
        const item = items[index] || {}
        return `${item.name}<br/>签订数量：${item.count || 0} 份<br/>合同金额：${formatCompactMoney(item.amount)}`
      }
    },
    grid: { left: 10, right: 14, top: 34, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: items.map((item) => item.name),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e7ebf3' } },
      axisLabel: { color: '#8b96ab', fontSize: 10.5 }
    },
    yAxis: [
      {
        type: 'value',
        name: '数量',
        nameTextStyle: { color: '#8b96ab', fontSize: 10 },
        splitLine: { lineStyle: { color: '#f1f4fa' } },
        axisLabel: { color: '#8b96ab', fontSize: 10.5 }
      },
      {
        type: 'value',
        name: '万元',
        nameTextStyle: { color: '#8b96ab', fontSize: 10 },
        splitLine: { show: false },
        axisLabel: { color: '#8b96ab', fontSize: 10.5, formatter: (value) => `${(value / 10000).toFixed(0)}` }
      }
    ],
    series: [
      {
        name: '签订数量',
        type: 'bar',
        barWidth: 14,
        itemStyle: { borderRadius: [5, 5, 0, 0] },
        data: items.map((item) => item.count || 0)
      },
      {
        name: '合同金额',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2 },
        areaStyle: { opacity: 0.08 },
        data: items.map((item) => Number(item.amount || 0))
      }
    ]
  }
})

const deptOption = computed(() => {
  const items = [...(data.value.deptStats || [])].sort((a, b) => Number(a.amount || 0) - Number(b.amount || 0))
  return {
    color: ['#5d8cff'],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const item = items[params[0]?.dataIndex ?? 0] || {}
        return `${item.name}<br/>合同数量：${item.count || 0} 份<br/>合同金额：${formatCompactMoney(item.amount)}`
      }
    },
    grid: { left: 10, right: 50, top: 16, bottom: 8, containLabel: true },
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
        barWidth: 12,
        itemStyle: {
          borderRadius: [0, 6, 6, 0],
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: '#9dbcff' },
              { offset: 1, color: '#2f6bff' }
            ]
          }
        },
        label: {
          show: true,
          position: 'right',
          color: '#8b96ab',
          fontSize: 10.5,
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
    data.value = await getDashboard()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.date-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #8b96ab;
  font-size: 12.5px;
  background: #fff;
  border: 1px solid #e7ebf3;
  border-radius: 8px;
  padding: 7px 12px;
}
.dept-chart {
  margin-top: 16px;
}
.dashboard-bottom {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(340px, 0.85fr);
  gap: 16px;
  margin-top: 16px;
}
.warning-panel {
  padding: 16px 18px 18px;
}
.warning-item {
  align-items: flex-start;
}
.warning-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-top: 6px;
  flex: 0 0 7px;
  background: #e6a23c;
}
.warning-dot.EXPIRY {
  background: #e6a23c;
}
.warning-dot.PAYMENT {
  background: #f05b5b;
}
@media (max-width: 1280px) {
  .dashboard-bottom {
    grid-template-columns: 1fr;
  }
}
</style>
