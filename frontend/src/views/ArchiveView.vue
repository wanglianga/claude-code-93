<template>
  <div class="page">
    <div class="kpi-row">
      <div class="kpi"><div class="v">{{ d.totalTickets ?? 0 }}</div><div class="l">总记录</div></div>
      <div class="kpi green"><div class="v">{{ d.done ?? 0 }}</div><div class="l">已办结</div></div>
      <div class="kpi"><div class="v">{{ waitAvg }}</div><div class="l">平均等待时长</div></div>
      <div class="kpi red"><div class="v">{{ d.fraudBlocked ?? 0 }}</div><div class="l">反诈拦截</div></div>
      <div class="kpi orange"><div class="v">{{ d.complaints ?? 0 }}</div><div class="l">投诉</div></div>
      <div class="kpi purple"><div class="v">{{ callbackRate }}%</div><div class="l">回访覆盖率</div></div>
    </div>

    <div class="grid-2">
      <!-- 渠道引导分布 -->
      <div class="card">
        <h3>客户渠道引导分布 <span class="hint">柜台 / 自助机 / 理财室 / 远程客服，避免全挤柜台</span></h3>
        <div class="bar-chart">
          <div class="bar-row" v-for="row in channelRows" :key="row.k">
            <span>{{ L.Channel[row.k] }}</span>
            <div class="bar-track"><div class="bar-fill" :style="{ width: row.pct + '%' }"></div></div>
            <span class="muted">{{ row.v }}</span>
          </div>
        </div>
        <div v-if="!channelTotal" class="empty">暂无分流数据</div>
      </div>

      <!-- 业务结果 -->
      <div class="card">
        <h3>办理结果 / 风险档案</h3>
        <div class="desc-list">
          <template v-for="r in resultRows" :key="r.k">
            <dt>{{ r.label }}</dt><dd>{{ r.v }}</dd>
          </template>
        </div>
        <hr style="border:none;border-top:1px solid var(--border);margin:12px 0" />
        <div class="small muted">反诈：待核 {{ d.fraudPending ?? 0 }} · 已拦截 {{ d.fraudBlocked ?? 0 }} · 未闭环事件 {{ d.openIssues ?? 0 }}</div>
      </div>
    </div>

    <!-- 运营档案明细 -->
    <div class="card">
      <h3>网点运营档案明细 <span class="hint">等待时长 · 业务结果 · 风险提示 · 投诉 · 回访</span>
        <select v-model="filter" style="margin-left:12px;font-size:12.5px">
          <option value="ALL">全部业务结果</option>
          <option v-for="(v,k) in L.ResultType" :key="k" :value="k">{{ v }}</option>
        </select>
      </h3>
      <table>
        <thead>
          <tr><th>号码</th><th>客户</th><th>业务</th><th>渠道</th><th>等待</th><th>结果</th>
            <th>风险提示</th><th>投诉</th><th>回访</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="t in rows" :key="t.id" @click="openId = t.id" style="cursor:pointer">
            <td><span class="ticket-no">{{ t.ticketNo }}</span></td>
            <td>{{ t.customerName }}</td>
            <td>{{ L.BusinessType[t.businessType] }}<span v-if="t.amount" class="small muted"> ¥{{ t.amount }}</span></td>
            <td class="small">{{ t.channel ? L.Channel[t.channel] : '—' }}</td>
            <td class="small">{{ waitText(t.waitSeconds) }}</td>
            <td class="small">{{ t.resultType ? L.ResultType[t.resultType] : '—' }}</td>
            <td class="small" style="max-width:200px">{{ t.riskNotice || '—' }}</td>
            <td class="small" style="max-width:170px">{{ t.complaint ? (t.complaintNote || '是') : '—' }}</td>
            <td class="small" style="max-width:170px">{{ t.callbackNote || '—' }}</td>
            <td><button class="btn btn-sm gray" @click.stop="openId = t.id">档案</button></td>
          </tr>
          <tr v-if="!rows.length"><td colspan="10"><div class="empty">暂无档案</div></td></tr>
        </tbody>
      </table>
    </div>

    <TicketDetailModal v-if="openId" :ticket-id="openId" :user="user"
                       @close="openId = null" @changed="store.refresh()" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { store } from '../store.js'
import { L, waitText } from '../labels.js'
import TicketDetailModal from '../components/TicketDetailModal.vue'

const props = defineProps({ user: Object })
const openId = ref(null)
const filter = ref('ALL')
const d = computed(() => store.dash || {})

const waitAvg = computed(() => waitText(d.value.avgWaitSeconds || 0))

const channelRows = computed(() => {
  const dist = d.value.channelDist || {}
  const total = Object.values(dist).reduce((a, b) => a + (b || 0), 0)
  return Object.keys(L.Channel).map(k => ({
    k, v: dist[k] || 0,
    pct: total ? Math.round((dist[k] || 0) * 100 / total) : 0
  }))
})
const channelTotal = computed(() => channelRows.value.reduce((a, r) => a + r.v, 0))

const resultRows = computed(() => {
  const done = store.tickets.filter(t => t.status === 'DONE')
  return Object.keys(L.ResultType).map(k => ({
    k, label: L.ResultType[k], v: done.filter(t => t.resultType === k).length
  }))
})

const rows = computed(() => {
  const archived = store.tickets.filter(t => t.status === 'DONE' || t.status === 'CANCELLED')
  return filter.value === 'ALL' ? archived : archived.filter(t => t.resultType === filter.value)
})

const callbackRate = computed(() => {
  const done = store.tickets.filter(t => t.status === 'DONE')
  if (!done.length) return 0
  return Math.round(done.filter(t => t.callbackNote).length * 100 / done.length)
})
</script>
