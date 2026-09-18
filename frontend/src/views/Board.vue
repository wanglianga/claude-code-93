<template>
  <div class="board">
    <div class="board-head">
      <div class="bh-left">
        <el-icon :size="30"><CreditCard /></el-icon>
        <span>中国建设银行 · 智慧网点叫号大屏</span>
      </div>
      <div class="bh-right">
        <el-tag v-if="data.branch?.pensionDay" type="warning" effect="dark" size="large">养老金集中发放日</el-tag>
        <el-tag v-if="data.branch?.powerOutage" type="danger" effect="dark" size="large">停电应急 · 手工登记</el-tag>
        <span class="clock">{{ clock }}</span>
      </div>
    </div>

    <div class="board-body">
      <div class="calling-panel">
        <div class="panel-title">正在呼叫</div>
        <div v-if="calling.length" class="call-list">
          <div v-for="t in calling" :key="t.id" class="call-item">            <span class="call-no mono-no">{{ t.ticketNo }}</span>
            <span class="call-win">请前往 <b>{{ t.assignedWindow }}</b> 号窗口</span>
          </div>
        </div>
        <div v-else class="empty-big">请留意叫号</div>
        <div class="notice">
          <el-icon><Bell /></el-icon>
          老年客户、行动不便客户可使用敬老优先窗；如需陪同引导、听力辅助请联系大堂经理。
        </div>
      </div>

      <div class="waiting-panel">
        <div class="panel-title">候场队列（敬老/VIP 自动优先）</div>
        <div class="wait-grid">
          <div v-for="t in data.waiting || []" :key="t.id" class="wait-item"
               :class="{ elder: t.elder }">
            <span class="mono-no w-no">{{ t.ticketNo }}</span>
            <span class="w-biz">{{ bizLabel(t.businessType) }}</span>
            <span class="w-win">{{ t.assignedWindow || '待分配' }}</span>
          </div>
          <div v-if="!(data.waiting||[]).length" class="empty-big">当前无候场客户</div>
        </div>
      </div>
    </div>

    <div class="board-foot">
      <div v-for="w in data.windows || []" :key="w.windowNo" class="foot-win"
           :class="{ off: w.status!=='OPEN' }">
        <span class="fw-no mono-no">{{ w.windowNo }}</span>
        <span class="fw-status">{{ statusLabel(w.status) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import api from '../api.js'
import { LABELS } from '../dict.js'

const data = ref({})
const clock = ref('')
let timer, clockTimer

const calling = computed(() => (data.value.calling || []).filter(t => t.assignedWindow))

function bizLabel(v) { return LABELS.businessTypes[v] || v }
function statusLabel(v) { return LABELS.windowStatuses[v] || v }
function tick() {
  const d = new Date()
  clock.value = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
function pad(n) { return String(n).padStart(2, '0') }

async function load() {
  try { data.value = await api.get('/display/board') } catch (_) {}
}

onMounted(() => {
  load(); tick()
  timer = setInterval(load, 5000)
  clockTimer = setInterval(tick, 1000)
})
onUnmounted(() => { clearInterval(timer); clearInterval(clockTimer) })
</script>

<style scoped>
.board { height: 100vh; background: linear-gradient(160deg, #081a3d, #123c80); color: #fff; display: flex; flex-direction: column; }
.board-head { height: 72px; display: flex; align-items: center; justify-content: space-between;
  padding: 0 30px; background: rgba(0,0,0,.25); }
.bh-left { display: flex; align-items: center; gap: 12px; font-size: 24px; font-weight: 800; letter-spacing: 2px; }
.bh-right { display: flex; align-items: center; gap: 14px; }
.clock { font-size: 20px; font-family: Consolas, monospace; }
.board-body { flex: 1; display: flex; gap: 18px; padding: 18px 24px; min-height: 0; }
.calling-panel { width: 38%; background: rgba(255,255,255,.07); border-radius: 14px; padding: 20px;
  display: flex; flex-direction: column; }
.waiting-panel { flex: 1; background: rgba(255,255,255,.07); border-radius: 14px; padding: 20px; min-width: 0; }
.panel-title { font-size: 18px; font-weight: 700; margin-bottom: 16px; color: #ffd98a; }
.call-list { flex: 1; display: flex; flex-direction: column; gap: 14px; justify-content: center; }
.call-item { background: linear-gradient(135deg, rgba(200,22,29,.85), rgba(230,80,60,.85));
  border-radius: 12px; padding: 20px 24px; display: flex; align-items: center; justify-content: space-between;
  animation: blink 1.6s ease-in-out infinite; }
.call-no { font-size: 52px; }
.call-win { font-size: 22px; }
@keyframes blink { 0%,100% { transform: scale(1); } 50% { transform: scale(1.02); } }
.notice { margin-top: 16px; font-size: 14px; color: #cdd9f5; display: flex; gap: 8px; align-items: flex-start; }
.wait-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; align-content: start;
  height: calc(100% - 40px); overflow: hidden; }
.wait-item { background: rgba(255,255,255,.1); border-radius: 10px; padding: 14px 16px;
  display: flex; flex-direction: column; gap: 4px; }
.wait-item.elder { background: rgba(122,90,248,.35); border: 1px solid #a48bff; }
.w-no { font-size: 26px; color: #ffd98a; }
.w-biz { font-size: 14px; color: #dbe5ff; }
.w-win { font-size: 13px; color: #9fb4e8; }
.empty-big { color: #7f93c2; font-size: 20px; margin: auto; }
.board-foot { height: 86px; background: rgba(0,0,0,.25); display: flex; align-items: center;
  justify-content: space-around; padding: 0 20px; }
.foot-win { text-align: center; }
.fw-no { display: block; font-size: 26px; color: #fff; }
.fw-status { font-size: 12px; color: #8fc0ff; }
.foot-win.off .fw-no { color: #6f7fa8; }
.foot-win.off .fw-status { color: #d84a4a; }
</style>
