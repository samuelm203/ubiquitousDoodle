<script setup>
import { onMounted, onBeforeUnmount } from 'vue'

defineProps({ title: { type: String, required: true } })
const emit = defineEmits(['close'])

function onKey(e) {
  if (e.key === 'Escape') emit('close')
}
onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <div class="modal-backdrop" @mousedown.self="emit('close')">
    <div class="modal" role="dialog" aria-modal="true" :aria-label="title">
      <header class="modal-head">
        <h2>{{ title }}</h2>
        <button class="btn btn-ghost btn-icon" aria-label="Schliessen" @click="emit('close')">✕</button>
      </header>
      <slot />
    </div>
  </div>
</template>
