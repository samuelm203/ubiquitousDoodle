<script setup>
import { computed } from 'vue'
import { STATUS } from '../format.js'

const props = defineProps({ status: { type: String, required: true } })
const info = computed(() => STATUS[props.status] ?? { label: props.status, tone: 'neutral' })
</script>

<template>
  <span class="badge" :class="`badge-${info.tone}`">{{ info.label }}</span>
</template>
