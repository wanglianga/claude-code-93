<template>
  <div>
    <el-row :gutter="14">
      <!-- 左侧：取号 -->
      <el-col :span="7">
        <el-card class="section-card">
          <template #header><b>客户取号 / 预约登记</b></template>
          <el-form :model="form" label-width="92px" size="small">
            <el-form-item label="客户姓名"><el-input v-model="form.customerName" /></el-form-item>
            <el-form-item label="手机后四位"><el-input v-model="form.phoneTail" maxlength="4" /></el-form-item>
            <el-form-item label="客户类型">
              <el-select v-model="form.customerType" style="width:100%">
                <el-option v-for="x in opts.customerTypes||[]" :key="x.value" :label="x.label" :value="x.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="取号来源">
              <el-select v-model="form.source" style="width:100%">
                <el-option v-for="x in opts.sources||[]" :key="x.value" :label="x.label" :value="x.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="业务类型">
              <el-select v-model="form.businessType" style="width:100%">
                <el-option v-for="x in opts.businessTypes||[]" :key="x.value" :label="x.label" :value="x.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="风险等级">
              <el-radio-group v-model="form.riskLevel">
                <el-radio-button v-for="x in opts.riskLevels||[]" :key="x.value" :value="x.value">{{ x.label }}</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="金额(万元)">
              <el-input-number v-model="form.amountWan" :min="0" :precision="2" style="width:100%"
                               :disabled="form.businessType!=='TRANSFER' && form.businessType!=='LARGE_CASH'" />
            </el-form-item>
            <el-form-item label="预计时长">
              <el-input-number v-model="form.estimatedMinutes" :min="5" :max="120" /> 分钟
            </el-form-item>
            <el-form-item label="材料完整度">
              <el-slider v-model="form.materialScore" show-input :max="100" style="width:100%" />
            </el-form-item>
            <el-form-item label="适老/无障碍">
              <el-checkbox v-model="form.elder">老人</el-checkbox>
              <el-checkbox v-model="form.wheelchair">轮椅</el-checkbox>
              <el-checkbox v-model="form.hearingGuide">听力引导</el-checkbox>
            </el-form-item>
            <el-button type="primary" style="width:100%" :loading="saving" @click="createTicket">生成排队号</el-button>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：队列 -->
      <el-col :span="17">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b>实时排队队列（{{ tickets.length }}）</b>
              <div>
                <el-radio-group v-model="filter" size="small">
                  <el-radio-button value="ALL">全部</el-radio-button>
                  <el-radio-button value="WAITING">候场</el-radio-button>
                  <el-radio-button value="ACTIVE">进行中</el-radio-button>
                  <el-radio-button value="DONE">已结束</el-radio-button>
                </el-radio-group>
                <el-button size="small" style="margin-left:8px" @click="load">刷新</el-button>
              </div>
            </div>
          </template>
          <el-table :data="filtered" size="small" :row-class-name="rowClass" height="640">
            <el-table-column label="号码" width="78">
              <template #default="{ row }">
                <span class="mono-no" style="font-size:16px;color:#1d4e9e">{{ row.ticketNo }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="customerName" label="客户" width="85" />
            <el-table-column label="类型" width="82">
              <template #default="{ row }">
                <el-tag size="small">{{ l('customerTypes', row.customerType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="业务" width="80">
              <template #default="{ row }">{{ l('businessTypes', row.businessType) }}</template>
            </el-table-column>
            <el-table-column label="风险" width="70">
              <template #default="{ row }">
                <el-tag size="small" :type="RISK_TAG[row.riskLevel]">{{ l('riskLevels', row.riskLevel) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="适老" width="110">
              <template #default="{ row }">
                <el-icon v-if="row.elder" color="#7a5af8"><UserFilled /></el-icon>
                <el-icon v-if="row.wheelchair" color="#0e8a8a" style="margin-left:4px"><Guide /></el-icon>
                <el-icon v-if="row.hearingGuide" color="#e6832a" style="margin-left:4px"><Microphone /></el-icon>
                <span v-if="!row.elder&&!row.wheelchair&&!row.hearingGuide">-</span>
              </template>
            </el-table-column>
            <el-table-column label="渠道/窗口" width="120">
              <template #default="{ row }">
                <div>{{ l('channels', row.channel) }}</div>
                <div class="mono-no" v-if="row.assignedWindow" style="font-size:12px;color:#888">{{ row.assignedWindow }}</div>
              </template>
            </el-table-column>
            <el-table-column label="等待" width="64">
              <template #default="{ row }">{{ waited(row) }}′</template>
            </el-table-column>
            <el-table-column label="状态" width="86">
              <template #default="{ row }">
                <el-tag size="small" :type="STATUS_TAG[row.status]">{{ l('ticketStatuses', row.status) }}</el-tag>
                <el-tag v-if="row.suspended" size="small" type="info" style="margin-left:2px">挂起</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="215" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status==='WAITING' && row.channel==='UNDECIDED'" size="small" type="primary"
                           @click="openSuggest(row)">智能分流</el-button>
                <el-button v-if="row.status==='WAITING' && row.channel!=='UNDECIDED'" size="small"
                           @click="openSuggest(row, true)">改派</el-button>
                <el-button v-if="needsFraud(row) && !row.fraudChecked" size="small" type="danger"
                           @click="openFraud(row)">反诈</el-button>
                <el-button v-if="row.status==='CALLED'" size="small" type="success"
                           @click="start(row)">开始办理</el-button>
                <el-button v-if="row.status==='SERVING'" size="small" type="success"
                           @click="openFinish(row)">办结</el-button>
                <el-button size="small" @click="openDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 分流建议对话框 -->
    <el-dialog v-model="suggestVisible" title="分流建议（材料完整度 / 柜员技能 / 窗口负载 / 库存 / 适老）" width="620px">
      <el-alert v-if="suggestion.blocked" type="error" :closable="false"
                :title="suggestion.reasons?.[0]" show-icon style="margin-bottom:10px" />
      <template v-else>
        <el-descriptions :column="2" border size="small" style="margin-bottom:12px">
          <el-descriptions-item label="建议渠道">
            <el-tag type="success">{{ l('channels', suggestion.channel) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="建议窗口">
            <span class="mono-no">{{ suggestion.windowNo || '由渠道现场安排' }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <el-alert v-for="(r, i) in suggestion.reasons||[]" :key="i" :title="r" :closable="false"
                  :type="r.startsWith('预警') ? 'error' : 'info'" style="margin-bottom:6px" />
        <el-form inline style="margin-top:10px">
          <el-form-item label="确认渠道">
            <el-select v-model="routeForm.channel" style="width:150px">
              <el-option v-for="x in opts.channels||[]" :key="x.value" :label="x.label" :value="x.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="窗口">
            <el-select v-model="routeForm.windowNo" clearable style="width:120px" placeholder="自动/指定">
              <el-option v-for="w in windows" :key="w.windowNo"
                         :label="w.windowNo + ' ' + l('windowTypes', w.type)" :value="w.windowNo" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="routeForm.materialPreChecked">已完成材料预审</el-checkbox>
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="suggestVisible=false">取消</el-button>
        <el-button type="primary" :disabled="suggestion.blocked" @click="confirmRoute">确认分流</el-button>
      </template>
    </el-dialog>

    <!-- 反诈核验对话框 -->
    <el-dialog v-model="fraudVisible" title="大额转账反诈核验（合规闸门）" width="560px">
      <el-alert type="error" :closable="false" show-icon
                :title="`客户 ${fraudTicket?.customerName} 申请转账 ${fraudTicket?.amountWan} 万元，超过 20 万元阈值`"
                description="需完成：核实身份与收款人关系、资金用途、是否接到陌生电话/指令、屏幕共享等；必要时联系家属。"
                style="margin-bottom:12px" />
      <el-checkbox v-model="fraudForm.q1">收款人身份与关系已核实</el-checkbox><br/>
      <el-checkbox v-model="fraudForm.q2">资金用途合理，客户表述清晰</el-checkbox><br/>
      <el-checkbox v-model="fraudForm.q3">未发现陌生来电/短信/屏幕共享诱导</el-checkbox><br/>
      <el-checkbox v-model="fraudForm.q4">已进行反诈提示并由客户确认</el-checkbox>
      <template #footer>
        <el-button type="danger" @click="intercept">核验不通过，拦截劝阻</el-button>
        <el-button type="success" :disabled="!fraudAllChecked" @click="passFraud">四项全部通过，放行</el-button>
      </template>
    </el-dialog>

    <!-- 办结对话框 -->
    <el-dialog v-model="finishVisible" title="办结并归档" width="480px">
      <el-form label-width="88px">
        <el-form-item label="业务结果">
          <el-input v-model="finishForm.outcome" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="finishVisible=false">取消</el-button>
        <el-button type="primary" @click="confirmFinish">确认办结</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" size="460px" :title="`排队详情 ${detail?.ticketNo||''}`">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="客户">{{ detail.customerName }}（{{ l('customerTypes', detail.customerType) }}）</el-descriptions-item>
          <el-descriptions-item label="业务/金额">{{ l('businessTypes', detail.businessType) }} / {{ detail.amountWan }} 万元</el-descriptions-item>
          <el-descriptions-item label="来源/风险">{{ l('sources', detail.source) }} / {{ l('riskLevels', detail.riskLevel) }}</el-descriptions-item>
          <el-descriptions-item label="适老需求">
            老人{{ detail.elder?'✓':'✗' }} · 轮椅{{ detail.wheelchair?'✓':'✗' }} · 听力引导{{ detail.hearingGuide?'✓':'✗' }}
          </el-descriptions-item>
          <el-descriptions-item label="材料">完整度 {{ detail.materialScore }} · 预审{{ detail.materialPreChecked?'已做':'未做' }}</el-descriptions-item>
          <el-descriptions-item label="反诈">
            {{ detail.fraudChecked ? '已通过：' + detail.fraudResult : (needsFraud(detail) ? '待核验' : '不涉及') }}
          </el-descriptions-item>
          <el-descriptions-item label="家属确认">
            {{ detail.familyConfirmed ? '已确认：' + detail.familyNote : '未确认' }}
          </el-descriptions-item>
          <el-descriptions-item label="渠道/窗口">{{ l('channels', detail.channel) }} / {{ detail.assignedWindow||'-' }}</el-descriptions-item>
          <el-descriptions-item label="投诉">{{ detail.complaint || '无' }}</el-descriptions-item>
          <el-descriptions-item label="结果">{{ detail.outcome || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin:16px 0 8px">关联协同事件</h4>
        <el-timeline>
          <el-timeline-item v-for="e in detailEvents" :key="e.id"
                            :type="e.status==='RESOLVED'?'success':'danger'"
                            :timestamp="fmt(e.raisedAt)">
            <b>{{ l('eventTypes', e.type) }}</b>（{{ l('roles', e.ownerRole) }}）<br/>
            {{ e.description }}
            <div v-if="e.resolution" style="color:#16a05f">处置：{{ e.resolution }}</div>
          </el-timeline-item>
          <el-timeline-item v-if="!detailEvents.length" style="color:#999">暂无</el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import api from '../api.js'
import { l, RISK_TAG, STATUS_TAG } from '../dict.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const opts = ref({})
const tickets = ref([])
const windows = ref([])
const filter = ref('WAITING')
const saving = ref(false)
let timer = null

const form = reactive({
  customerName: '', phoneTail: '', customerType: 'NORMAL', source: 'WALK_IN',
  businessType: 'OPEN_ACCOUNT', riskLevel: 'LOW', amountWan: 0,
  estimatedMinutes: 15, materialScore: 100,
  elder: false, wheelchair: false, hearingGuide: false
})

const suggestVisible = ref(false)
const suggestion = ref({})
const suggestTicket = ref(null)
const routeForm = reactive({ channel: 'COUNTER', windowNo: null, materialPreChecked: false })

const fraudVisible = ref(false)
const fraudTicket = ref(null)
const fraudForm = reactive({ q1: false, q2: false, q3: false, q4: false })
const fraudAllChecked = computed(() => Object.values(fraudForm).every(Boolean))

const finishVisible = ref(false)
const finishTicket = ref(null)
const finishForm = reactive({ outcome: '业务办理成功' })

const detailVisible = ref(false)
const detail = ref(null)
const detailEvents = ref([])

const filtered = computed(() => {
  const active = ['CALLED', 'SERVING']
  const done = ['COMPLETED', 'CANCELLED', 'NOSHOW']
  return tickets.value.filter(t =>
    filter.value === 'ALL' ? true
      : filter.value === 'WAITING' ? t.status === 'WAITING'
      : filter.value === 'ACTIVE' ? active.includes(t.status)
      : done.includes(t.status))
})

async function load() {
  const [t, w, e] = await Promise.all([api.get('/tickets'), api.get('/windows'), api.get('/meta/enums')])
  tickets.value = t
  windows.value = w
  if (!opts.value.businessTypes) opts.value = e
}

async function createTicket() {
  if (!form.customerName) return ElMessage.warning('请填写客户姓名')
  saving.value = true
  try {
    const t = await api.post('/tickets', form)
    ElMessage.success('已取号 ' + t.ticketNo)
    Object.assign(form, { customerName: '', phoneTail: '', amountWan: 0, materialScore: 100,
      elder: false, wheelchair: false, hearingGuide: false })
    filter.value = 'WAITING'
    await load()
  } finally { saving.value = false }
}

function needsFraud(row) {
  return row.businessType === 'TRANSFER' && Number(row.amountWan) >= 20
}

async function openSuggest(row, reassign = false) {
  suggestTicket.value = row
  const s = await api.get(`/tickets/${row.id}/route-suggest`)
  suggestion.value = s
  routeForm.channel = s.channel === 'UNDECIDED' ? 'COUNTER' : s.channel
  routeForm.windowNo = s.windowNo
  routeForm.materialPreChecked = row.materialPreChecked
  suggestVisible.value = true
}

async function confirmRoute() {
  await api.post(`/tickets/${suggestTicket.value.id}/route`, routeForm)
  ElMessage.success('分流完成')
  suggestVisible.value = false
  load()
}

async function start(row) {
  await api.post(`/tickets/${row.id}/start`, {})
  ElMessage.success('已开始办理')
  load()
}

function openFinish(row) {
  finishTicket.value = row
  finishForm.outcome = '业务办理成功'
  finishVisible.value = true
}
async function confirmFinish() {
  try {
    await api.post(`/tickets/${finishTicket.value.id}/finish`, finishForm)
    ElMessage.success('已办结并进入运营档案')
    finishVisible.value = false
    load()
  } catch (_) { /* 拦截器已提示 */ }
}

function openFraud(row) {
  fraudTicket.value = row
  Object.assign(fraudForm, { q1: false, q2: false, q3: false, q4: false })
  fraudVisible.value = true
}
async function passFraud() {
  await api.post(`/tickets/${fraudTicket.value.id}/fraud-pass`, { note: '反诈四问全部通过' })
  ElMessage.success('反诈核验通过，可继续办理')
  fraudVisible.value = false
  load()
}
async function intercept() {
  const { value } = await ElMessageBox.prompt('请记录拦截/劝阻原因', '反诈拦截', {
    inputValue: '客户称接到自称公检法电话要求转账，已劝阻并联系家属'
  }).catch(() => ({ value: null }))
  if (value === null || value === undefined) return
  await api.post(`/tickets/${fraudTicket.value.id}/fraud-intercept`, { reason: value })
  ElMessage.warning('已执行反诈拦截并归档')
  fraudVisible.value = false
  load()
}

async function openDetail(row) {
  detail.value = row
  detailEvents.value = await api.get(`/tickets/${row.id}/events`)
  detailVisible.value = true
}

function waited(row) {
  if (row.status === 'COMPLETED' || row.status === 'CANCELLED') return '-'
  return Math.max(0, Math.round((Date.now() - new Date(row.createdAt).getTime()) / 60000))
}
function rowClass({ row }) {
  if (row.suspended) return 'row-suspended'
  if (row.elder && row.status === 'WAITING') return 'row-elder'
  return ''
}
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '' }

onMounted(() => { load(); timer = setInterval(load, 8000) })
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
:deep(.row-elder) { background: #f6f3ff !important; }
:deep(.row-suspended) { background: #f4f4f5 !important; color: #999; }
</style>
