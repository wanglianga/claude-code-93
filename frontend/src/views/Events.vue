<template>
  <div v-loading="loading">
    <el-row :gutter="14">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b>协同事件（大堂经理 / 柜员 / 理财经理 / 安保 / 客服 / 客户 同一排队记录）</b>
              <el-radio-group v-model="statusFilter" size="small">
                <el-radio-button value="OPEN">待处理</el-radio-button>
                <el-radio-button value="ALL">全部</el-radio-button>
                <el-radio-button value="RESOLVED">已处置</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <el-table :data="filtered" size="small">
            <el-table-column label="类型" width="150">
              <template #default="{ row }">
                <el-tag :type="eventTag(row.type)" size="small">{{ l('eventTypes', row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="排队号" width="80">
              <template #default="{ row }">
                <span class="mono-no">{{ row.ticketNo || (row.windowNo ? row.windowNo + '窗' : '网点') }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="情况说明" min-width="240" show-overflow-tooltip />
            <el-table-column label="责任角色" width="100">
              <template #default="{ row }">
                <el-tag size="small" type="warning">{{ l('roles', row.ownerRole) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="提出人/时间" width="140">
              <template #default="{ row }">
                <div>{{ row.raisedBy }}</div>
                <div style="color:#999;font-size:12px">{{ fmt(row.raisedAt) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status==='RESOLVED'?'success':'danger'">
                  {{ l('eventStatuses', row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status!=='RESOLVED'" size="small" type="primary"
                           @click="openResolve(row)">处置</el-button>
                <span v-else style="color:#16a05f;font-size:12px">{{ row.resolution }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="section-card" header="发起协同事件">
          <el-form :model="form" label-width="84px" size="small">
            <el-form-item label="事件类型">
              <el-select v-model="form.type" style="width:100%">
                <el-option v-for="x in eventTypes" :key="x.value" :label="x.label" :value="x.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="关联排队号">
              <el-select v-model="form.ticketId" filterable clearable style="width:100%" placeholder="网点级事件可空">
                <el-option v-for="t in waitingTickets" :key="t.id"
                           :label="`${t.ticketNo} ${t.customerName} ${l('businessTypes', t.businessType)}`"
                           :value="t.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="关联窗口">
              <el-select v-model="form.windowNo" clearable style="width:100%">
                <el-option v-for="w in windows" :key="w.windowNo" :label="w.windowNo" :value="w.windowNo" />
              </el-select>
            </el-form-item>
            <el-form-item label="责任角色">
              <el-select v-model="form.ownerRole" style="width:100%">
                <el-option v-for="x in roles" :key="x.value" :label="x.label" :value="x.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="情况说明">
              <el-input v-model="form.description" type="textarea" :rows="3" :placeholder="placeholder" />
            </el-form-item>
            <el-button type="primary" style="width:100%" @click="raise">提交事件</el-button>
          </el-form>
        </el-card>

        <el-card header="常见协同场景">
          <el-timeline>
            <el-timeline-item type="danger">客户长时间等待 → 经理安抚 + 优先级提升</el-timeline-item>
            <el-timeline-item type="danger">大额转账 → 柜员反诈核验，必要时拦截劝阻</el-timeline-item>
            <el-timeline-item type="warning">老人忘带证件 → 一次性告知/改约/家属递送</el-timeline-item>
            <el-timeline-item type="warning">柜员离岗/自助机故障 → 客户改派其他窗口</el-timeline-item>
            <el-timeline-item type="primary">投诉插队 → 客服登记投诉并补偿优先</el-timeline-item>
            <el-timeline-item type="info">突发停电 → 安保维持秩序 + 手工登记挂起</el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="resolveVisible" title="处置协同事件" width="460px">
      <el-alert :title="resolveRow?.description" type="info" :closable="false" style="margin-bottom:12px" />
      <el-input v-model="resolution" type="textarea" :rows="3" placeholder="处置过程与结果" />
      <template #footer>
        <el-button @click="resolveVisible=false">取消</el-button>
        <el-button type="primary" @click="confirmResolve">完成处置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import api from '../api.js'
import { l } from '../dict.js'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const events = ref([])
const windows = ref([])
const tickets = ref([])
const eventTypes = ref([])
const roles = ref([])
const statusFilter = ref('OPEN')
let timer = null

const form = reactive({ type: 'LONG_WAIT', ticketId: null, windowNo: null,
  ownerRole: 'MANAGER', description: '' })

const resolveVisible = ref(false)
const resolveRow = ref(null)
const resolution = ref('')

const waitingTickets = computed(() =>
  tickets.value.filter(t => ['WAITING', 'CALLED', 'SERVING'].includes(t.status)))

const filtered = computed(() => {
  const sorted = [...events.value].sort((a, b) => b.id - a.id)
  if (statusFilter.value === 'ALL') return sorted
  return sorted.filter(e => statusFilter.value === 'OPEN'
    ? e.status !== 'RESOLVED' : e.status === 'RESOLVED')
})

const placeholder = computed(() => ({
  LONG_WAIT: '如：客户情绪焦虑，已等待 25 分钟',
  MISSING_ID: '如：老人忘带身份证，已告知子女拍照/改约',
  JUMP_QUEUE: '如：客户投诉有人不按号办理，要求解释',
  POWER_OUTAGE: '如：片区停电，启用应急照明',
  TELLER_AWAY: '如：W2 柜员临时被调去复核',
  MACHINE_FAULT: '如：M1 智能柜员机吞卡'
}[form.type] || '请描述现场情况'))

async function load() {
  loading.value = true
  try {
    const [e, w, t, m] = await Promise.all([
      api.get('/events'), api.get('/windows'), api.get('/tickets'), api.get('/meta/enums')
    ])
    events.value = e; windows.value = w; tickets.value = t
    eventTypes.value = m.eventTypes; roles.value = m.roles
  } finally { loading.value = false }
}

async function raise() {
  if (!form.description) return ElMessage.warning('请填写情况说明')
  await api.post('/events', form)
  ElMessage.success('事件已发起，相关角色将协同处理')
  form.description = ''
  load()
}

function openResolve(row) {
  resolveRow.value = row
  resolution.value = row.type === 'MISSING_ID' ? '已一次性告知所需材料并协助改约/联系家属递送'
    : row.type === 'JUMP_QUEUE' ? '已向客户解释叫号规则，被影响客户提升优先级并致歉'
    : row.type === 'LONG_WAIT' ? '已安抚客户并安排下一位办理'
    : '已现场处置完成'
  resolveVisible.value = true
}
async function confirmResolve() {
  await api.post(`/events/${resolveRow.value.id}/resolve`, { resolution: resolution.value })
  ElMessage.success('事件已处置')
  resolveVisible.value = false
  load()
}

function eventTag(t) {
  return { FRAUD_CHECK: 'danger', POWER_OUTAGE: 'danger', MACHINE_FAULT: 'danger',
    LONG_WAIT: 'warning', MISSING_ID: 'warning', TELLER_AWAY: 'warning',
    JUMP_QUEUE: 'primary', LARGE_CASH: 'warning', PENSION_RUSH: 'info',
    ACCESSIBILITY: 'success', GENERAL: 'info' }[t] || 'info'
}
function fmt(t) { return t ? t.replace('T', ' ').slice(5, 16) : '' }

onMounted(() => { load(); timer = setInterval(load, 10000) })
onUnmounted(() => clearInterval(timer))
</script>
