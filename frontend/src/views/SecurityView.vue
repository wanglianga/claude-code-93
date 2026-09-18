<template>
  <div class="page">
    <BranchBanners />
    <div class="kpi-row">
      <div class="kpi red"><div class="v">{{ d.fraudPending ?? 0 }}</div><div class="l">反诈待核验</div></div>
      <div class="kpi red"><div class="v">{{ d.fraudBlocked ?? 0 }}</div><div class="l">今日拦截</div></div>
      <div class="kpi purple"><div class="v">{{ wheelchairCount }}</div><div class="l">无障碍引导需求</div></div>
      <div class="kpi orange"><div class="v">{{ outageOn ? '应急中' : '正常' }}</div><div class="l">网点状态</div></div>
    </div>

    <div class="grid-2">
      <div class="card">
        <h3>大额业务反诈核验队列 <span class="hint">大额转账/取现触发，核验前业务挂起</span></h3>
        <TicketTable :tickets="fraudQueue" @open="openId = $event" />
        <div v-if="!fraudQueue.length" class="empty">暂无待核验业务</div>
      </div>
      <div class="card">
        <h3>无障碍 / 维序任务</h3>
        <table>
          <thead><tr><th>号码</th><th>客户</th><th>需求</th><th></th></tr></thead>
          <tbody>
            <tr v-for="t in needAssist" :key="t.id">
              <td><span class="ticket-no">{{ t.ticketNo }}</span></td>
              <td>{{ t.customerName }}</td>
              <td>
                <span v-if="t.wheelchair" class="tag purple">轮椅·开启无障碍通道</span>
                <span v-if="t.hearingAssist" class="tag purple">听力引导</span>
                <span v-if="t.elderly" class="tag orange">老人陪同</span>
              </td>
              <td><button class="btn btn-sm" @click="openId = t.id">查看记录</button></td>
            </tr>
            <tr v-if="!needAssist.length"><td colspan="4"><div class="empty">暂无无障碍任务</div></td></tr>
          </tbody>
        </table>
        <div class="btn-row">
          <button class="btn red" @click="outage(true)">上报：网点突发停电</button>
          <button class="btn green" @click="outage(false)">上报：供电恢复</button>
        </div>
      </div>
    </div>

    <TicketDetailModal v-if="openId" :ticket-id="openId" :user="user"
                       @close="openId = null" @changed="store.refresh()" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { api } from '../api.js'
import { store } from '../store.js'
import BranchBanners from '../components/BranchBanners.vue'
import TicketTable from '../components/TicketTable.vue'
import TicketDetailModal from '../components/TicketDetailModal.vue'
import { toast } from '../toast.js'

const props = defineProps({ user: Object })
const openId = ref(null)
const d = computed(() => store.dash || {})
const fraudQueue = computed(() =>
  store.tickets.filter(t => t.fraudStatus === 'PENDING'))
const needAssist = computed(() =>
  store.tickets.filter(t => (t.wheelchair || t.hearingAssist) &&
    !['DONE', 'CANCELLED'].includes(t.status)))
const outageOn = computed(() => (store.dash?.alerts || []).some(a => a.alertType === 'OUTAGE' && a.active))

async function outage(active) {
  await api.post('/branch/outage', { active, note: active ? '安保上报：网点停电，已启动手工叫号与现场维序' : '安保确认：供电恢复' })
  toast(active ? '停电应急已启动' : '已恢复')
  await store.refresh()
}
</script>
