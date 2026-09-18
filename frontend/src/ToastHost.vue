<template>
  <div v-if="toastState.show" class="toast" :class="{ err: toastState.type === 'err' }">
    {{ toastState.type === 'err' ? '✕ ' : '✓ ' }}{{ toastState.msg }}
  </div>
</template>

<script setup>
import { toastState } from './toast.js'
</script>
