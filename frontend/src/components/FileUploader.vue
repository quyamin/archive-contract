<template>
  <div class="file-uploader">
    <div class="uploader-toolbar">
      <div class="muted">共 {{ attachments.length }} 个附件</div>
      <div v-if="editable && userStore.hasPerm('attachment:upload')" class="uploader-actions">
        <el-select v-model="fileType" size="small" style="width: 118px">
          <el-option v-for="item in fileTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-upload
          ref="uploadRef"
          :show-file-list="false"
          :http-request="customUpload"
          :before-upload="beforeUpload"
          :accept="accept"
          multiple
        >
          <el-button type="primary" plain size="small" :loading="uploading">
            <el-icon><Upload /></el-icon> 上传附件
          </el-button>
        </el-upload>
      </div>
    </div>

    <div v-loading="loading">
      <template v-if="attachments.length">
        <div v-for="item in attachments" :key="item.id" class="attachment-row">
          <div class="attachment-info">
            <div class="file-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div style="min-width: 0">
              <div class="attachment-name" :title="item.originalName">{{ item.originalName }}</div>
              <div class="attachment-meta">
                <el-tag size="small" effect="plain" :type="fileTypeMeta(item.fileType).type || 'info'" style="margin-right: 6px">
                  {{ fileTypeMeta(item.fileType).label }}
                </el-tag>
                {{ fileSize(item.fileSize) }} · {{ item.uploaderName || '-' }} · {{ formatDateTime(item.uploadTime) }}
              </div>
            </div>
          </div>
          <div class="attachment-actions">
            <el-button
              v-if="item.previewable && userStore.hasPerm('attachment:preview')"
              link
              type="primary"
              @click="openPreview(item)"
            >
              <el-icon><View /></el-icon> 预览
            </el-button>
            <el-button
              v-if="canDownload && userStore.hasPerm('contract:download')"
              link
              type="primary"
              @click="download(item)"
            >
              <el-icon><Download /></el-icon> 下载
            </el-button>
            <el-button
              v-if="editable && userStore.hasPerm('attachment:delete')"
              link
              type="danger"
              @click="remove(item)"
            >
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </div>
        </div>
      </template>
      <div v-else class="empty-tip">
        <el-icon size="28" color="#c4ccda"><FolderOpened /></el-icon>
        <div style="margin-top: 8px">暂无附件{{ editable ? '，请上传 PDF、Word 或图片' : '' }}</div>
      </div>
    </div>

    <FilePreview
      v-model="previewVisible"
      :attachment="previewItem"
      :can-download="canDownload"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getAttachments,
  uploadAttachments,
  deleteAttachment,
  downloadAttachment
} from '@/api/attachment'
import { downloadResponse } from '@/utils/download'
import { fileSize, formatDateTime, FILE_TYPE_META } from '@/utils/format'
import FilePreview from './FilePreview.vue'

const props = defineProps({
  contractId: { type: [Number, String], required: true },
  editable: { type: Boolean, default: false },
  canDownload: { type: Boolean, default: false }
})
const emit = defineEmits(['change'])

const userStore = useUserStore()
const attachments = ref([])
const loading = ref(false)
const uploading = ref(false)
const previewVisible = ref(false)
const previewItem = ref(null)
const uploadRef = ref(null)
const fileType = ref('ORIGINAL')
const accept = '.pdf,.doc,.docx,.xls,.xlsx,.jpg,.jpeg,.png,.gif'
const fileTypes = [
  { label: '合同原件', value: 'ORIGINAL' },
  { label: '补充协议', value: 'SUPPLEMENT' },
  { label: '审批单', value: 'APPROVAL' },
  { label: '验收材料', value: 'ACCEPTANCE' },
  { label: '其他附件', value: 'OTHER' }
]

function fileTypeMeta(value) {
  return FILE_TYPE_META[value] || { label: '其他附件', type: 'info' }
}

async function load() {
  if (!props.contractId) return
  loading.value = true
  try {
    attachments.value = await getAttachments(props.contractId)
    emit('change', attachments.value.length)
  } finally {
    loading.value = false
  }
}

function beforeUpload(file) {
  const ext = file.name.split('.').pop()?.toLowerCase()
  const allowed = accept.split(',').map((item) => item.replace('.', ''))
  if (!allowed.includes(ext)) {
    ElMessage.error('不支持的文件格式，请上传 PDF、Word、Excel 或图片')
    return false
  }
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('单个附件不能超过 50MB')
    return false
  }
  return true
}

async function customUpload({ file }) {
  uploading.value = true
  const formData = new FormData()
  formData.append('contractId', props.contractId)
  formData.append('fileType', fileType.value)
  formData.append('files', file)
  try {
    await uploadAttachments(formData)
    ElMessage.success('附件上传成功')
    await load()
  } finally {
    uploading.value = false
    uploadRef.value?.clearFiles()
  }
}

function openPreview(item) {
  previewItem.value = item
  previewVisible.value = true
}

async function download(item) {
  const response = await downloadAttachment(item.id)
  downloadResponse(response, item.originalName)
  ElMessage.success('附件下载成功')
}

async function remove(item) {
  try {
    await ElMessageBox.confirm(`确认删除附件“${item.originalName}”？`, '删除附件', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteAttachment(item.id)
    ElMessage.success('附件已删除')
    await load()
  } catch {
    // cancelled
  }
}

watch(() => props.contractId, load)
onMounted(load)
defineExpose({ refresh: load })
</script>

<style scoped>
.uploader-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.uploader-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.attachment-actions {
  display: flex;
  gap: 4px;
  flex: 0 0 auto;
}
</style>
