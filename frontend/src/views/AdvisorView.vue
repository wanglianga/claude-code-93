<template>
  <div class="page">
    <div class="banner blue">
      今日理财经理排班在岗，理财咨询统一引导至理财室并完成风险评估双录；高风险产品需客户本人确认。
    </div>
    <div class="grid-2">
      <div class="card">
        <h3>理财室排队 / 待咨询</h3>
        <TicketTable :tickets="wealthQueue" @open="openId = $event" />
      </div>
      <div class="card">
        <h3>我在办的咨询单</h3>
        <TicketTable :tickets="serving" @open="openId = $event" />
        <div v-if="!serving.length" class="empty">暂无办理中的咨询</div>
      </div>
    </div>
    <TicketDetailModal v-if="openId" :ticket-id="openId" :user="user"
                       @close="openId = null" @changed="store.refresh()" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { store } from '../store.js'
import TicketTable from '../components/TicketTable.vue'
import TicketDetailModal from '../components/TicketDetailModal.vue'

const props = defineProps({ user: Object })
const openId = ref(null)
const wealthQueue = computed(() =>
  store.tickets.filter(t => t.businessType === 'WEALTH_CONSULT' &&
    (t.status === 'WAITING' || t.status === 'ASSIGNED' || t.status === 'PENDING')))
const serving = computed(() =>
  store.tickets.filter(t => t.assigneeId === props.user.id && t.status === 'SERVING'))
</script>
