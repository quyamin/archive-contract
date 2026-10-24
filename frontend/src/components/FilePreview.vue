<template>
  <el-dialog
    v-model="visible"
    :title="attachment?.originalName || '附件预览'"
    width="920px"
    top="5vh"
    destroy-on-close
    class="preview-dialog"
    @closed="cleanup"
  >
    <div v-loading="loading" class="preview-body">
      <template v-if="!loading">
        <img v-if="type === 'image' && fileUrl" :src="fileUrl" class="preview-image" alt="附件预览" />
        <iframe v-else-if="type === 'pdf' && fileUrl" :src="fileUrl" class="preview-frame"></iframe>
        <div v-else-if="type === 'docx' && fileUrl" ref="docxContainer" class="docx-container"></div>
        <el-result
          v-else
          icon="info"
          title="该文件类型暂不支持在线预览"
          sub-title="可下载原件后使用本地软件查看"
        >
          <template #extra>
            <el-button v-if="canDownload" type="primary" @click="download">下载原件</el-button>
          </template>
        </el-result>
      </template>
    </div>
    <template #footer>
      <div class="preview-footer">
        <span class="muted">{{ attachment?.fileExt?.toUpperCase() }} · {{ fileSize(attachment?.fileSize) }}</span>
        <div>
          <el-button @click="visible = false">关闭</el-button>
          <el-button v-if="canDownload" type="primary" :loading="downloading" @click="download">
            <el-icon><Download /></el-icon> 下载原件
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { renderAsync } from 'docx-preview'
import { previewAttachment, downloadAttachment } from '@/api/attachment'
import { downloadResponse } from '@/utils/download'
import { fileSize } from '@/utils/format'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  attachment: { type: Object, default: null },
  canDownload: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})
const loading = ref(false)
const downloading = ref(false)
const fileUrl = ref('')
const docxContainer = ref(null)
const type = computed(() => {
  const ext = (props.attachment?.fileExt || '').toLowerCase()
  if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext)) return 'image'
  if (ext === 'pdf') return 'pdf'
  if (ext === 'docx') return 'docx'
  return 'other'
})

watch(
  () => props.modelValue,
  async (open) => {
    if (open && props.attachment) {
      await loadFile()
    }
  }
)

async function loadFile() {
  cleanup()
  loading.value = true
  try {
    const response = await previewAttachment(props.attachment.id)
    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'application/octet-stream'
    })
    if (type.value === 'image' || type.value === 'pdf') {
      fileUrl.value = URL.createObjectURL(blob)
    } else if (type.value === 'docx') {
      fileUrl.value = 'docx'
      await nextTick()
      if (docxContainer.value) {
        await renderAsync(blob, docxContainer.value, null, {
          inWrapper: true,
          ignoreWidth: false,
          ignoreHeight: false,
          breakPages: true,
          ignoreFonts: false
        })
      }
    }
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function download() {
  if (!props.attachment) return
  downloading.value = true
  try {
    const response = await downloadAttachment(props.attachment.id)
    downloadResponse(response, props.attachment.originalName)
    ElMessage.success('附件下载成功')
  } finally {
    downloading.value = false
  }
}

function cleanup() {
  if (fileUrl.value && fileUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(fileUrl.value)
  }
  fileUrl.value = ''
  if (docxContainer.value) docxContainer.value.innerHTML = ''
}
</script>

<style scoped>
.preview-body {
  min-height: 360px;
  max-height: 72vh;
  overflow: auto;
  background: #f5f7fb;
  border-radius: 8px;
}
.preview-frame {
  width: 100%;
  height: 72vh;
  border: none;
  background: #fff;
}
.preview-image {
  display: block;
  max-width: 100%;
  margin: 0 auto;
}
.docx-container {
  background: #fff;
  padding: 20px;
}
.docx-container :deep(.docx-wrapper) {
  background: #f5f7fb;
  padding: 12px;
}
.preview-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
</style>
