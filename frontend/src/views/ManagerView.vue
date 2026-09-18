<template>
  <div class="page">
    <BranchBanners />

    <div class="kpi-row">
      <div class="kpi"><div class="v">{{ d.totalTickets ?? 0 }}</div><div class="l">累计排队记录</div></div>
      <div class="kpi orange"><div class="v">{{ (d.waiting??0)+(d.assigned??0) }}</div><div class="l">等待中（待分流+待叫号）</div></div>
      <div class="kpi green"><div class="v">{{ d.serving ?? 0 }}</div><div class="l">办理中</div></div>
      <div class="kpi red"><div class="v">{{ d.pending ?? 0 }}</div><div class="l">异常挂起</div></div>
      <div class="kpi orange"><div class="v">{{ d.waitingElderly ?? 0 }}</div><div class="l">等待中的老人</div></div>
      <div class="kpi red"><div class="v">{{ d.fraudPending ?? 0 }}</div><div class="l">反诈待核验</div></div>
      <div class="kpi purple"><div class="v">{{ d.openIssues ?? 0 }}</div><div class="l">未闭环事件</div></div>
      <div class="kpi"><div class="v">{{ waitAvg }}</div><div class="l">今日平均等待</div></div>
    </div>

    <div class="grid-2">
      <!-- 窗口负载 -->
      <div class="card">
        <h3>窗口负载与柜员排班 <span class="hint">依据柜员技能、窗口类型与实时负载分流</span></h3>
        <table>
          <thead><tr><th>窗口</th><th>类型</th><th>状态</th><th>柜员</th><th>今日办理</th><th>负载</th></tr></thead>
          <tbody>
            <tr v-for="c in store.counters" :key="c.id">
              <td>{{ c.name }}</td>
              <td class="small">{{ L.CounterType[c.type] }}</td>
              <td><span class="tag" :class="tag(L.CounterStatus, c.status).cls">{{ tag(L.CounterStatus, c.status).text }}</span></td>
              <td class="small">{{ staffName(c.tellerId) || '—' }}</td>
              <td>{{ c.servedCount }}</td>
              <td style="min-width:110px">
                <div class="counter-load"><i :class="{ hot: c.servedCount >= 8 }"
                  :style="{ width: Math.min(100, c.servedCount * 10) + '%' }"></i></div>
                <div class="small muted">均件 {{ Math.round(c.avgServeSeconds/60) }} 分钟</div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 现金 + 网点应急 -->
      <div class="card">
        <h3>现金库存与网点应急</h3>
        <div class="desc-list">
          <dt>今日库存</dt><dd class="mono" :style="{color: cashLow ? 'var(--red)' : 'var(--green)'}">
            ¥{{ (d.cash?.balance ?? 0).toLocaleString() }}<span v-if="d.cash?.replenishing" class="tag orange" style="margin-left:8px">调拨中</span>
          </dd>
          <dt>预警阈值</dt><dd class="mono">¥{{ (d.cash?.threshold ?? 0).toLocaleString() }}</dd>
        </div>
        <div v-if="cashLow" class="banner red" style="margin-top:10px">
          现金低于预警线，大额现金业务将发起调拨，请大堂经理主动告知客户预计等候时间
        </div>
        <div class="btn-row">
          <button class="btn btn-sm gray" @click="replenish">模拟现金调拨到账</button>
        </div>
        <hr style="border:none;border-top:1px solid var(--border);margin:14px 0" />
        <div class="small muted" style="margin-bottom:8px">养老金集中发放日 / 突发停电应急：</div>
        <div class="btn-row">
          <button class="btn orange" @click="setPension(true)">标记：养老金集中发放日</button>
          <button class="btn gray" @click="setPension(false)">解除发放高峰</button>
          <button class="btn red" @click="setOutage(true)">⚡ 触发突发停电</button>
          <button class="btn green" @click="setOutage(false)">供电恢复·恢复营业</button>
        </div>
        <div class="small muted" style="margin-top:8px">
          停电触发后：全部在办/待叫号单据自动挂起、启用手工叫号与安保维序；恢复后自动重新入队。
        </div>
      </div>
    </div>

    <!-- 未闭环事件 -->
    <div class="card">
      <h3>待协同处理事件 <span class="hint">长等 / 反诈 / 忘带证件 / 离岗 / 设备故障 / 投诉插队 / 停电</span></h3>
      <table>
        <thead><tr><th>类型</th><th>号码</th><th>内容</th><th>上报人</th><th>时间</th><th></th></tr></thead>
        <tbody>
          <tr v-for="e in openEvents" :key="e.id">
            <td><span class="tag red">{{ L.IssueType[e.type] }}</span></td>
            <td><a class="ticket-no" @click="openId = e.ticketId" style="cursor:pointer">{{ ticketNo(e.ticketId) }}</a></td>
            <td style="max-width:430px">{{ e.content }}</td>
            <td class="small">{{ e.actorName }}<div class="muted">{{ e.actorRole ? L.Role[e.actorRole] : '' }}</div></td>
            <td class="small">{{ fmtTime(e.createdAt) }}</td>
            <td><button class="btn btn-sm gray" @click="resolve(e)">闭环处理</button></td>
          </tr>
          <tr v-if="!openEvents.length"><td colspan="6"><div class="empty">暂无待处理事件</div></td></tr>
        </tbody>
      </table>
    </div>

    <!-- 全量队列 -->
    <div class="card">
      <h3>网点排队总览 <span class="hint">点击任意记录进行材料预审与分流</span>
        <select v-model="filter" style="margin-left:12px;font-size:12.5px">
          <option value="ALL">全部状态</option>
          <option v-for="(v,k) in L.TicketStatus" :key="k" :value="k">{{ v[0] }}</option>
        </select>
      </h3>
      <TicketTable :tickets="filtered" @open="openId = $event" />
    </div>

    <TicketDetailModal v-if="openId" :ticket-id="openId" :user="user"
                       @close="openId = null" @changed="store.refresh()" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { api } from '../api.js'
import { store } from '../store.js'
import { L, tag, fmtTime } from '../labels.js'
import { waitText } from '../labels.js'
import { toast } from '../toast.js'
import BranchBanners from '../components/BranchBanners.vue'
import TicketTable from '../components/TicketTable.vue'
import TicketDetailModal from '../components/TicketDetailModal.vue'

const props = defineProps({ user: Object })
const openId = ref(null)
const filter = ref('ALL')

const d = computed(() => store.dash || {})
const waitAvg = computed(() => waitText(d.value.avgWaitSeconds || 0))
const cashLow = computed(() => d.value.cash && d.value.cash.balance <= d.value.cash.threshold)
const openEvents = computed(() => store.events.filter(e => !e.resolved))
const filtered = computed(() =>
  filter.value === 'ALL' ? store.tickets : store.tickets.filter(t => t.status === filter.value))

function staffName(id) { return store.staff.find(s => s.id === id)?.displayName || '' }
function ticketNo(id) { return store.tickets.find(t => t.id === id)?.ticketNo || ('#' + id) }

async function resolve(e) {
  await api.post('/events/' + e.id + '/resolve',
    { resolution: '大堂经理已协调相关岗位现场处置', resumeTicket: true })
  toast('事件已闭环，单据恢复队列')
  await store.refresh()
}

async function replenish() {
  await api.put('/cash', { balance: 1200000, replenishing: false, note: '调拨现金到账' })
  toast('现金调拨到账')
  await store.refresh()
}

async function setOutage(active) {
  await api.post('/branch/outage', { active, note: active ? '片区线路检修导致停电，启动应急预案' : '电力恢复，设备重启完成' })
  toast(active ? '停电应急已启动，单据统一挂起' : '已恢复营业')
  await store.refresh()
}

async function setPension(active) {
  await api.post('/branch/pension-day', { active })
  toast(active ? '已标记养老金集中发放日' : '已解除发放高峰标记')
  await store.refresh()
}
</script>
