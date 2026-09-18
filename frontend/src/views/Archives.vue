<template>
  <div v-loading="loading">
    <el-row :gutter="14" style="margin-bottom:14px">
      <el-col :span="6"><div class="stat-card bg-blue"><div class="stat-num">{{ list.length }}</div><div class="stat-label">归档业务量</div></div></el-col>
      <el-col :span="6"><div class="stat-card bg-green"><div class="stat-num">{{ avgWait }}′</div><div class="stat-label">平均等待时长</div></div></el-col>
      <el-col :span="6"><div class="stat-card bg-red"><div class="stat-num">{{ complaintCount }}</div><div class="stat-label">投诉留痕</div></div></el-col>
      <el-col :span="6"><div class="stat-card bg-orange"><div class="stat-num">{{ revisitRate }}%</div><div class="stat-label">回访完成率</div></div></el-col>
    </el-row>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b>网点运营档案（等待时长 / 业务结果 / 风险提示 / 投诉 / 回访 / 渠道正确性）</b>
          <el-radio-group v-model="kw" size="small" style="margin-right:8px">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="complaint">有投诉</el-radio-button>
            <el-radio-button value="elder">老年客户</el-radio-button>
            <el-radio-button value="revisit">待回访</el-radio-button>
            <el-radio-button value="misroute">渠道可优化</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="filtered" size="small">
        <el-table-column label="号码" width="80">
          <template #default="{ row }"><span class="mono-no">{{ row.ticketNo }}</span></template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" width="85" />
        <el-table-column label="业务" width="80">
          <template #default="{ row }">{{ l('businessTypes', row.businessType) }}</template>
        </el-table-column>
        <el-table-column label="渠道" width="100">
          <template #default="{ row }">
            {{ l('channels', row.channel) }}
            <el-tag v-if="!row.channelCorrect" type="danger" size="small" style="margin-left:2px">可改自助</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="等待/办理" width="90">
          <template #default="{ row }">
            <span :style="{color: row.waitMinutes>=20?'#d84a4a':'#333'}">{{ row.waitMinutes }}′</span>
            / {{ row.serveMinutes }}′
          </template>
        </el-table-column>
        <el-table-column prop="outcome" label="业务结果" min-width="150" show-overflow-tooltip />
        <el-table-column prop="riskTip" label="风险提示" min-width="150" show-overflow-tooltip />
        <el-table-column label="投诉" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.complaintRaised" type="danger" size="small">有</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="回访/满意度" width="150">
          <template #default="{ row }">
            <template v-if="row.revisitDone">
              <el-rate :model-value="row.satisfaction" disabled size="small" />
              <div style="font-size:12px;color:#666">{{ row.revisitResult }}</div>
            </template>
            <el-button v-else size="small" type="primary" @click="openRevisit(row)">登记回访</el-button>
          </template>
        </el-table-column>
        <el-table-column label="归档时间" width="140">
          <template #default="{ row }">{{ fmt(row.archivedAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="revisitVisible" title="客户回访登记" width="460px">
      <el-form label-width="92px">
        <el-form-item label="满意度">
          <el-rate v-model="revisitForm.satisfaction" />
        </el-form-item>
        <el-form-item label="回访结论">
          <el-input v-model="revisitForm.revisitResult" type="textarea" :rows="3"
                    placeholder="等待感受、服务态度、适老关怀、改进建议" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="revisitVisible=false">取消</el-button>
        <el-button type="primary" @click="saveRevisit">保存回访</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../api.js'
import { l } from '../dict.js'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const list = ref([])
const kw = ref('')

const revisitVisible = ref(false)
const revisitRow = ref(null)
const revisitForm = reactive({ satisfaction: 5, revisitResult: '' })

const filtered = computed(() => list.value.filter(a =>
  kw.value === 'complaint' ? a.complaintRaised
    : kw.value === 'elder' ? a.elder
    : kw.value === 'revisit' ? !a.revisitDone
    : kw.value === 'misroute' ? !a.channelCorrect
    : true))

const avgWait = computed(() => {
  const v = list.value.filter(a => a.waitMinutes > 0)
  return v.length ? Math.round(v.reduce((s, a) => s + a.waitMinutes, 0) / v.length) : 0
})
const complaintCount = computed(() => list.value.filter(a => a.complaintRaised).length)
const revisitRate = computed(() => list.value.length
  ? Math.round(list.value.filter(a => a.revisitDone).length * 100 / list.value.length) : 0)

async function load() {
  loading.value = true
  try { list.value = await api.get('/archives') } finally { loading.value = false }
}
function openRevisit(row) {
  revisitRow.value = row
  revisitForm.satisfaction = 5
  revisitForm.revisitResult = '客户对服务满意，等待时间可接受'
  revisitVisible.value = true
}
async function saveRevisit() {
  await api.post(`/archives/${revisitRow.value.id}/revisit`, revisitForm)
  ElMessage.success('回访已登记')
  revisitVisible.value = false
  load()
}
function fmt(t) { return t ? t.replace('T', ' ').slice(5, 16) : '' }

onMounted(load)
</script>
