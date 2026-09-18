<template>
  <div v-loading="loading">
    <el-alert v-if="branch?.powerOutage" type="error" :closable="false" show-icon
              title="停电应急模式：暂停电子叫号" style="margin-bottom:14px" />
    <el-row :gutter="14">
      <el-col :span="7" v-for="w in windows" :key="w.windowNo">
        <el-card class="win-card" :class="{ offline: w.status!=='OPEN' }">
          <div class="win-head">
            <div>
              <span class="mono-no win-no">{{ w.windowNo }}</span>
              <el-tag size="small" style="margin-left:8px">{{ l('windowTypes', w.type) }}</el-tag>
            </div>
            <el-tag :type="winTag(w.status)" size="small">{{ l('windowStatuses', w.status) }}</el-tag>
          </div>
          <div class="win-staff">柜员：{{ w.staffName || '未分配' }} · 队列 {{ w.currentLoad }} 人</div>
          <div v-if="w.note" class="win-note">备注：{{ w.note }}</div>

          <div class="now-serving">
            <template v-if="currentOf(w)">
              <div class="ns-label">当前接待</div>
              <div class="ns-no mono-no">{{ currentOf(w).ticketNo }}</div>
              <div class="ns-name">{{ currentOf(w).customerName }} · {{ l('businessTypes', currentOf(w).businessType) }}</div>
              <div v-if="currentOf(w).elder" class="ns-elder">
                <el-icon><UserFilled /></el-icon> 老年客户 · 注意语速与风险提示
              </div>
            </template>
            <template v-else><div class="ns-empty">暂无客户</div></template>
          </div>

          <div class="win-actions">
            <el-button size="small" type="primary" :disabled="!canCall(w)" @click="callNext(w)">呼叫下一位</el-button>
            <el-button size="small" type="success" :disabled="!calledOf(w)" @click="start(w)">确认到窗</el-button>
            <el-button size="small" type="warning" :disabled="w.status==='BREAK'" @click="setStatus(w,'BREAK')">临时离岗</el-button>
            <el-button size="small" :disabled="w.status==='OPEN'" @click="setStatus(w,'OPEN')">回岗开放</el-button>
            <el-button v-if="w.type==='SELF_SERVICE'" size="small" type="danger"
                       :disabled="w.status==='FAULT'" @click="setStatus(w,'FAULT')">设备故障</el-button>
            <el-button v-if="w.type==='SELF_SERVICE' && w.status==='FAULT'" size="small" type="success"
                       @click="setStatus(w,'OPEN')">修复开放</el-button>
          </div>

          <!-- 当前客户业务操作 -->
          <template v-if="currentOf(w)">
            <el-divider style="margin:10px 0" />
            <div v-if="needsFraud(currentOf(w)) && !currentOf(w).fraudChecked" class="fraud-box">
              <el-tag type="danger" size="small">大额转账待反诈核验</el-tag>
              <div style="margin-top:6px">
                <el-button size="small" type="success" @click="fraudPass(currentOf(w))">核验通过</el-button>
                <el-button size="small" type="danger" @click="fraudBlock(currentOf(w))">拦截</el-button>
              </div>
            </div>
            <el-button size="small" type="primary" style="width:100%;margin-top:8px"
                       @click="finish(currentOf(w), w)">办结归档</el-button>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import api, { currentUser } from '../api.js'
import { l } from '../dict.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const windows = ref([])
const tickets = ref([])
const branch = ref(null)
let timer = null
const me = currentUser()

async function load() {
  const [w, t, b] = await Promise.all([api.get('/windows'), api.get('/tickets'), api.get('/branch')])
  windows.value = w
  tickets.value = t
  branch.value = b
}

function currentOf(w) {
  return tickets.value.find(t => t.assignedWindow === w.windowNo && t.status === 'SERVING')
}
function calledOf(w) {
  return tickets.value.find(t => t.assignedWindow === w.windowNo && t.status === 'CALLED')
}
function canCall(w) {
  if (w.status !== 'OPEN' || branch.value?.powerOutage) return false
  if (currentOf(w)) return false
  return tickets.value.some(t => t.assignedWindow === w.windowNo && t.status === 'WAITING')
}
function needsFraud(t) {
  return t.businessType === 'TRANSFER' && Number(t.amountWan) >= 20
}
function winTag(s) { return { OPEN: 'success', CLOSED: 'info', BREAK: 'warning', FAULT: 'danger' }[s] }

async function callNext(w) {
  try {
    const t = await api.post(`/windows/${w.windowNo}/call-next`, {})
    ElMessage.success(`请 ${t.ticketNo} 到 ${w.windowNo} 号窗口`)
    load()
  } catch (_) {}
}
async function start(w) {
  const t = calledOf(w)
  await api.post(`/tickets/${t.id}/start`, {})
  ElMessage.success('客户已到窗，开始办理')
  load()
}
async function setStatus(w, status) {
  let note = null
  if (status === 'BREAK' || status === 'FAULT') {
    const r = await ElMessageBox.prompt(status === 'BREAK' ? '离岗原因（可选）' : '故障情况说明',
      l('windowStatuses', status), { inputValue: status === 'BREAK' ? '轮休/用餐' : '吞卡/系统报错，已报修' })
      .catch(() => null)
    if (!r) return
    note = r.value
  }
  await api.post(`/windows/${w.windowNo}/status`, { status, note })
  ElMessage.success('窗口状态已更新，并已生成协同事件')
  load()
}
async function fraudPass(t) {
  await api.post(`/tickets/${t.id}/fraud-pass`, { note: '柜员反诈四问通过' })
  ElMessage.success('核验通过')
  load()
}
async function fraudBlock(t) {
  const { value } = await ElMessageBox.prompt('拦截原因', '反诈拦截',
    { inputValue: '疑似诈骗，已劝阻并登记' }).catch(() => ({ value: null }))
  if (value === null || value === undefined) return
  await api.post(`/tickets/${t.id}/fraud-intercept`, { reason: value })
  ElMessage.warning('已拦截归档')
  load()
}
async function finish(t, w) {
  if (needsFraud(t) && !t.fraudChecked) {
    return ElMessage.error('大额转账必须先通过反诈核验')
  }
  const { value } = await ElMessageBox.prompt('业务结果', '办结',
    { inputValue: '业务办理成功' }).catch(() => ({ value: null }))
  if (value === null || value === undefined) return
  await api.post(`/tickets/${t.id}/finish`, { outcome: value })
  ElMessage.success('已办结，等待/办理时长等已入运营档案')
  load()
}

onMounted(() => { loading.value = true; load().finally(() => loading.value = false); timer = setInterval(load, 6000) })
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.win-card { margin-bottom: 14px; min-height: 250px; }
.win-card.offline { opacity: .85; background: #fafafa; }
.win-head { display: flex; justify-content: space-between; align-items: center; }
.win-no { font-size: 22px; color: #1d4e9e; }
.win-staff { font-size: 13px; color: #555; margin: 8px 0; }
.win-note { font-size: 12px; color: #e6832a; margin-bottom: 6px; }
.now-serving { background: #f0f5ff; border-radius: 8px; padding: 12px; text-align: center; min-height: 88px; }
.ns-label { font-size: 12px; color: #666; }
.ns-no { font-size: 30px; color: #c8161d; line-height: 1.3; }
.ns-name { font-size: 13px; color: #333; }
.ns-elder { margin-top: 4px; font-size: 12px; color: #7a5af8; }
.ns-empty { color: #aaa; padding: 24px 0; }
.win-actions { margin-top: 10px; display: flex; flex-wrap: wrap; gap: 6px; }
.fraud-box { background: #fff1f0; border: 1px dashed #d84a4a; border-radius: 8px; padding: 8px; text-align: center; }
</style>
