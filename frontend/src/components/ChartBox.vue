<template>
  <div ref="chartRef" class="chart-box" :style="{ height }"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '280px' }
})

const chartRef = ref(null)
let chart = null
let observer = null

function render() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value, null, { renderer: 'canvas' })
  chart.setOption(props.option, true)
}

onMounted(() => {
  nextTick(render)
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(chartRef.value)
})

watch(
  () => props.option,
  () => nextTick(render),
  { deep: true }
)

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.chart-box {
  width: 100%;
  min-height: 220px;
}
</style>
