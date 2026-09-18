<template>
  <div class="page">
    <div class="kpi-row">
      <div class="kpi red"><div class="v">{{ d.complaints ?? 0 }}</div><div class="l">投诉记录</div></div>
      <div class="kpi orange"><div class="v">{{ longWaitCount }}</div><div class="l">长等客户（待关怀）</div></div>
      <div class="kpi green"><div class="v">{{ callbackDone }}</div><div class="l">已回访</div></div>
      <div class="kpi"><div class="v">{{ d.done ?? 0 }}</div><div class="l">今日办结</div></div>
    </div>

    <div class="grid-2">
      <div class="card">
        <h3>投诉与插队纠纷处理</h3>
        <table>
          <thead><tr><th>号码</th><th>客户</th><th>投诉/事件</th><th>状态</th><th></th></tr></thead>
          <tbody>
            <tr v-for="t in complaintTickets" :key="t.id">
              <td><span class="ticket-no">{{ t.ticketNo }}</span></td>
              <td>{{ t.customerName }}</td>
              <td class="small" style="max-width:280px">{{ t.complaintNote || '—' }}</td>
              <td><span class="tag" :class="tag(L.TicketStatus, t.status).cls">{{ tag(L.TicketStatus, t.status).text }}</span></td>
              <td><button class="btn btn-sm" @click="openId = t.id">介入处理</button></td>
            </tr>
            <tr v-if="!complaintTickets.length"><td colspan="5"><div class="empty">暂无投诉</div></td></tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <h3>办结客户回访（进入运营档案）</h3>
        <table>
          <thead><tr><th>号码</th><th>客户</th><th>业务</th><th>回访</th><th></th></tr></thead>
          <tbody>
            <tr v-for="t in doneTickets" :key="t.id">
              <td><span class="ticket-no">{{ t.ticketNo }}</span></td>
              <td>{{ t.customerName }}</td>
              <td>{{ L.BusinessType[t.businessType] }}</td>
              <td class="small">{{ t.callbackNote || '未回访' }}</td>
              <td><button class="btn btn-sm" @click="openId = t.id">填写回访</button></td>
            </tr>
            <tr v-if="!doneTickets.length"><td colspan="5"><div class="empty">暂无办结记录</div></td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="card">
      <h3>全部排队记录（客服可远程协助取号与渠道引导）</h3>
      <TicketTable :tickets="store.tickets" @open="openId = $event" />
    </div>

    <TicketDetailModal v-if="openId" :ticket-id="openId" :user="user"
                       @close="openId = null" @changed="store.refresh()" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { store } from '../store.js'
import { L, tag } from '../labels.js'
import TicketTable from '../components/TicketTable.vue'
import TicketDetailModal from '../components/TicketDetailModal.vue'

const props = defineProps({ user: Object })
const openId = ref(null)
const d = computed(() => store.dash || {})
const complaintTickets = computed(() =>
  store.tickets.filter(t => t.complaint ||
    store.events.some(e => e.ticketId === t.id && e.type === 'JUMP_COMPLAINT')))
const longWaitCount = computed(() =>
  store.tickets.filter(t => t.longWaitNotified && !['DONE', 'CANCELLED'].includes(t.status)).length)
const doneTickets = computed(() => store.tickets.filter(t => t.status === 'DONE'))
const callbackDone = computed(() => doneTickets.value.filter(t => t.callbackNote).length)
</script>
