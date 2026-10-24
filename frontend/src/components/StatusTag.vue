<template>
  <el-tag :type="meta.type || 'info'" :effect="effect" size="small" round class="status-tag">
    <span class="dot" :style="{ background: meta.color || currentColor }"></span>{{ meta.label || status || '-' }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'
import { STATUS_META, BORROW_STATUS_META, CHANGE_STATUS_META } from '@/utils/format'

const props = defineProps({
  status: { type: String, default: '' },
  kind: { type: String, default: 'contract' },
  effect: { type: String, default: 'light' }
})

const maps = {
  contract: STATUS_META,
  borrow: BORROW_STATUS_META,
  change: CHANGE_STATUS_META
}

const meta = computed(() => {
  const map = maps[props.kind] || STATUS_META
  return map[props.status] || { label: props.status || '-', type: 'info', color: '#909399' }
})

const currentColor = computed(() => {
  if (props.kind === 'contract') return '#909399'
  return '#909399'
})
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  border: none;
  padding: 0 9px;
  height: 23px;
}
.dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  margin-right: 5px;
}
</style>
