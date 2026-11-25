<script setup>
import { toasts } from '../notify.js'
</script>

<template>
  <div class="toasts" aria-live="polite">
    <div v-for="t in toasts" :key="t.id" class="toast" :class="`toast-${t.kind}`">{{ t.message }}</div>
  </div>
</template>
