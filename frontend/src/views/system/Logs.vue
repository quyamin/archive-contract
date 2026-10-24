<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <h2 class="page-title">操作日志</h2>
        <p class="page-desc">系统对关键业务操作进行全量审计留痕。日志由数据库触发器保护，任何账号均无法修改或删除。</p>
      </div>
      <div class="page-actions">
        <el-tag type="success" effect="plain" size="large" round>
          <el-icon><Lock /></el-icon> 审计日志 · 不可篡改
        </el-tag>
        <el-button :loading="loading" @click="load"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </div>
    </div>

    <div class="panel-card filter-card">
      <el-form :model="query" label-width="72px" inline @submit.prevent>
        <el-form-item label="操作人">
          <el-input v-model="query.username" placeholder="姓名" clearable style="width: 140px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="业务模块">
          <el-select v-model="query.module" placeholder="全部模块" clearable style="width: 140px">
            <el-option v-for="item in modules" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作内容">
          <el-input v-model="query.operation" placeholder="操作动作" clearable style="width: 150px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="执行结果">
          <el-select v-model="query.status" placeholder="全部结果" clearable style="width: 120px">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAIL" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作时间">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 330px"
          />
        </el-form-item>
        <el-form-item label-width="0">
          <el-button type="primary" @click="search"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="panel-card table-card">
      <div class="table-toolbar">
        <div class="table-toolbar-title">审计记录</div>
        <span class="muted">共 {{ page.total }} 条</span>
      </div>
      <el-table v-loading="loading" :data="page.records" stripe>
        <el-table-column label="操作时间" width="158">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="操作人" width="96">
          <template #default="{ row }">{{ row.username || '系统' }}</template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="106">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.module }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="operation" label="操作内容" min-width="150" />
        <el-table-column label="请求" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="method-tag" :class="row.requestMethod?.toLowerCase()">{{ row.requestMethod }}</span>
            <span class="request-uri">{{ row.requestUri }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP 地址" width="130" />
        <el-table-column label="耗时" width="88" align="right">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.durationMs > 1000 }">{{ row.durationMs ?? '—' }} ms</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="84">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'SUCCESS' ? 'success' : 'danger'" effect="plain">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
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

    <el-drawer v-model="drawerVisible" title="操作日志详情" size="560px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="日志编号">#{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.username || '系统' }}</el-descriptions-item>
        <el-descriptions-item label="业务模块">{{ detail.module }}</el-descriptions-item>
        <el-descriptions-item label="操作内容">{{ detail.operation }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ detail.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="请求地址"><span class="mono">{{ detail.requestUri }}</span></el-descriptions-item>
        <el-descriptions-item label="客户端 IP">{{ detail.ip }}</el-descriptions-item>
        <el-descriptions-item label="执行耗时">{{ detail.durationMs }} ms</el-descriptions-item>
        <el-descriptions-item label="执行结果">
          <el-tag size="small" :type="detail.status === 'SUCCESS' ? 'success' : 'danger'" effect="plain">
            {{ detail.status === 'SUCCESS' ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="异常信息">
          <span class="text-danger">{{ detail.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <div class="detail-section">
        <div class="detail-section-title">请求参数</div>
        <div class="params-box">{{ detail.requestParams || '—' }}</div>
      </div>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="审计日志写入后不可修改、不可删除，仅支持查询。"
        style="margin-top: 16px"
      />
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getLogs } from '@/api/system'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const page = ref({ records: [], total: 0 })
const timeRange = ref([])
const drawerVisible = ref(false)
const detail = ref({})
const modules = ['登录认证', '合同档案', '归档审核', '档案变更', '档案借阅', '附件管理', '风险预警', '统计分析', '档案台账', '用户管理', '角色权限', '组织机构', '字典配置', '系统参数']
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  module: '',
  operation: '',
  status: '',
  startTime: '',
  endTime: ''
})

function buildParams() {
  const params = { ...query }
  if (timeRange.value?.length === 2) {
    params.startTime = timeRange.value[0]
    params.endTime = timeRange.value[1]
  }
  return params
}

async function load() {
  loading.value = true
  try {
    page.value = await getLogs(buildParams())
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
    username: '',
    module: '',
    operation: '',
    status: '',
    startTime: '',
    endTime: ''
  })
  timeRange.value = []
  load()
}

function viewDetail(row) {
  detail.value = row
  drawerVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.method-tag {
  display: inline-block;
  min-width: 46px;
  text-align: center;
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
  margin-right: 7px;
  background: #edf3ff;
  color: #2f6bff;
}
.method-tag.post {
  background: #e9f9f1;
  color: #20b26c;
}
.method-tag.put {
  background: #fff6e8;
  color: #c47b12;
}
.method-tag.delete {
  background: #fff0f0;
  color: #f05b5b;
}
.request-uri {
  font-family: "Roboto Mono", monospace;
  font-size: 11px;
  color: #526079;
}
.mono {
  font-family: "Roboto Mono", monospace;
  font-size: 11.5px;
}
.detail-section {
  margin-top: 18px;
}
.detail-section-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 9px;
}
.params-box {
  background: #0f1c33;
  color: #a8c0ff;
  border-radius: 9px;
  padding: 14px;
  font-family: "Roboto Mono", monospace;
  font-size: 11px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 280px;
  overflow: auto;
}
</style>
