<template>
  <div class="page-container">
    <div class="page-heading">
      <div>
        <h2 class="page-title">系统参数</h2>
        <p class="page-desc">配置系统名称、文件存储规则、风险预警提前天数与借阅期限等运行参数，修改后即时生效。</p>
      </div>
      <div class="page-actions">
        <el-button :loading="loading" @click="load"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </div>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="参数修改将记录到操作日志。文件存储路径调整后，新上传附件将保存到新路径，历史附件仍按原路径读取。"
      style="margin-bottom: 16px"
    />

    <div v-loading="loading" class="config-groups">
      <div v-for="group in groups" :key="group.name" class="panel-card config-card">
        <div class="config-group-head">
          <div class="config-group-icon">
            <el-icon><component :is="group.icon" /></el-icon>
          </div>
          <div>
            <div class="config-group-title">{{ group.name }}</div>
            <div class="muted">{{ group.items.length }} 项配置</div>
          </div>
        </div>
        <div class="config-list">
          <div v-for="item in group.items" :key="item.id" class="config-item">
            <div class="config-info">
              <div class="config-name">
                {{ item.configName }}
                <el-tag size="small" effect="plain" type="info">{{ item.configKey }}</el-tag>
              </div>
              <div class="config-desc">{{ item.remark || '暂无说明' }}</div>
            </div>
            <div class="config-value-wrap">
              <span class="config-value" :title="item.configValue">{{ item.configValue }}</span>
              <el-button
                v-if="userStore.hasPerm('system:config:edit')"
                link
                type="primary"
                size="small"
                @click="openEdit(item)"
              >
                <el-icon><Edit /></el-icon> 修改
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="修改系统参数" width="480px">
      <el-form label-position="top">
        <el-form-item label="配置名称">
          <el-input :model-value="current.configName" disabled />
        </el-form-item>
        <el-form-item label="配置键">
          <el-input :model-value="current.configKey" disabled />
        </el-form-item>
        <el-form-item label="配置值" required>
          <el-input
            v-model="configValue"
            :type="isLongText ? 'textarea' : 'text'"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请输入配置值"
          />
        </el-form-item>
        <el-form-item label="说明">
          <div class="muted" style="font-size: 12px; line-height: 1.7">{{ current.remark }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getConfigs, updateConfig } from '@/api/system'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const configs = ref([])
const dialogVisible = ref(false)
const current = ref({})
const configValue = ref('')

const groupIcons = {
  基础配置: 'Setting',
  文件存储: 'FolderOpened',
  风险预警: 'Warning',
  借阅管理: 'Reading'
}

const groups = computed(() => {
  const map = new Map()
  configs.value.forEach((item) => {
    const name = item.configGroup || '其他配置'
    if (!map.has(name)) map.set(name, { name, icon: groupIcons[name] || 'Tools', items: [] })
    map.get(name).items.push(item)
  })
  return [...map.values()]
})

const isLongText = computed(() => (current.value.configValue || '').length > 60)

async function load() {
  loading.value = true
  try {
    configs.value = await getConfigs()
  } finally {
    loading.value = false
  }
}

function openEdit(item) {
  current.value = item
  configValue.value = item.configValue
  dialogVisible.value = true
}

async function submit() {
  if (!configValue.value?.trim()) {
    ElMessage.warning('配置值不能为空')
    return
  }
  submitting.value = true
  try {
    await updateConfig({
      configKey: current.value.configKey,
      configValue: configValue.value.trim()
    })
    ElMessage.success('系统参数已更新')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.config-groups {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.config-card {
  padding: 18px 20px;
}
.config-group-head {
  display: flex;
  align-items: center;
  gap: 11px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f0f2f7;
}
.config-group-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #edf3ff;
  color: #2f6bff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex: 0 0 38px;
}
.config-group-title {
  font-size: 14px;
  font-weight: 700;
}
.config-group-head .muted {
  font-size: 10.5px;
  margin-top: 3px;
}
.config-list {
  padding-top: 4px;
}
.config-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 13px 0;
  border-bottom: 1px dashed #eef1f7;
}
.config-item:last-child {
  border-bottom: none;
}
.config-info {
  min-width: 0;
}
.config-name {
  font-size: 12.5px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
}
.config-name .el-tag {
  font-family: "Roboto Mono", monospace;
  font-size: 9.5px;
}
.config-desc {
  font-size: 11px;
  color: #98a2b3;
  margin-top: 5px;
  line-height: 1.6;
}
.config-value-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
}
.config-value {
  max-width: 210px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: "Roboto Mono", monospace;
  font-size: 11.5px;
  background: #f8faff;
  border: 1px solid #edf1f8;
  border-radius: 6px;
  padding: 4px 8px;
  color: #2f6bff;
}
@media (max-width: 1100px) {
  .config-groups {
    grid-template-columns: 1fr;
  }
}
</style>
