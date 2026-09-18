<template>
  <div>
    <div class="banner red" v-for="a in outageAlerts" :key="a.id">
      ⚠ {{ a.title }}：{{ a.content }}（{{ a.bizDate }}）
    </div>
    <div class="banner orange" v-for="a in pensionAlerts" :key="a.id">
      👴 {{ a.title }}：{{ a.content }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { store } from '../store.js'

const outageAlerts = computed(() => (store.dash?.alerts || []).filter(a => a.alertType === 'OUTAGE'))
const pensionAlerts = computed(() => (store.dash?.alerts || []).filter(a => a.alertType === 'PENSION_DAY'))
</script>
