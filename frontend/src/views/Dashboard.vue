<template>
  <div v-loading="loading">
    <!-- 网点状态应急条 -->
    <el-alert v-if="d.branch?.powerOutage" type="error" :closable="false" show-icon
              title="网点停电应急中：电子叫号已挂起，启用手工登记，请安保维持现场秩序"
              style="margin-bottom:14px" />
    <el-alert v-else-if="d.branch?.pensionDay" type="warning" :closable="false" show-icon
              title="今日养老金集中发放日：老年客户高峰，敬老优先窗已加权"
              style="margin-bottom:14px" />

    <el-row :gutter="14">
      <el-col :span="4"><div class="stat-card bg-blue"><div class="stat-num">{{ d.waiting ?? 0 }}</div><div class="stat-label">候场中</div><el-icon class="stat-icon"><Clock /></el-icon></div></el-col>
      <el-col :span="4"><div class="stat-card bg-orange"><div class="stat-num">{{ d.called ?? 0 }}</div><div class="stat-label">已叫号</div><el-icon class="stat-icon"><Bell /></el-icon></div></el-col>
      <el-col :span="4"><div class="stat-card bg-purple"><div class="stat-num">{{ d.serving ?? 0 }}</div><div class="stat-label">办理中</div><el-icon class="stat-icon"><Loading /></el-icon></div></el-col>
      <el-col :span="4"><div class="stat-card bg-green"><div class="stat-num">{{ d.completed ?? 0 }}</div><div class="stat-label">已办结</div><el-icon class="stat-icon"><CircleCheck /></el-icon></div></el-col>
      <el-col :span="4"><div class="stat-card bg-red"><div class="stat-num">{{ d.openEvents ?? 0 }}</div><div class="stat-label">待处置事件</div><el-icon class="stat-icon"><Warning /></el-icon></div></el-col>
      <el-col :span="4"><div class="stat-card bg-teal"><div class="stat-num">{{ d.cashTotalWan ?? 0 }}</div><div class="stat-label">现金库存(万元)</div><el-icon class="stat-icon"><Wallet /></el-icon></div></el-col>
    </el-row>

    <el-row :gutter="14" style="margin-top:14px">
      <el-col :span="12">
        <el-card class="section-card" header="窗口负载与状态">
          <el-table :data="d.windows || []" size="small">
            <el-table-column prop="windowNo" label="窗口" width="70" />
            <el-table-column label="类型" width="110">
              <template #default="{ row }">{{ l('windowTypes', row.type) }}</template>
            </el-table-column>
            <el-table-column prop="staffName" label="在岗" width="90" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="winTag(row.status)" size="small">{{ l('windowStatuses', row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="在办/等候" width="90">
              <template #default="{ row }">
                <el-progress :percentage="loadPct(row.currentLoad)" :stroke-width="10"
                             :format="() => row.currentLoad + '人'" />
              </template>
            </el-table-column>
            <el-table-column prop="note" label="备注" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="section-card" header="渠道引导分布（避免全部挤压柜台）">
          <div v-for="(v, k) in d.channelDistribution || {}" :key="k" class="bar-row">
            <div class="bar-label">{{ l('channels', k) }}</div>
            <el-progress :percentage="pct(v)" :stroke-width="14" :format="() => v + '单'"
                         :color="channelColor(k)" />
          </div>
          <el-divider style="margin:12px 0" />
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="平均等待">{{ Math.round(d.avgWait || 0) }} 分钟</el-descriptions-item>
            <el-descriptions-item label="适老候场">{{ d.elderWaiting ?? 0 }} 人</el-descriptions-item>
            <el-descriptions-item label="投诉数">{{ d.complaintCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="渠道可优化">
              <el-tag type="danger" size="small">{{ d.misrouted ?? 0 }} 单</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-card header="网点应急与高峰管控（大堂经理）">
      <el-form inline>
        <el-form-item label="突发停电">
          <el-switch v-model="branch.powerOutage" active-text="停电应急模式"
                     @change="saveBranch" />
        </el-form-item>
        <el-form-item label="养老金发放日">
          <el-switch v-model="branch.pensionDay" active-text="养老金高峰"
                     @change="saveBranch" />
        </el-form-item>
        <el-form-item label="网点公告">
          <el-input v-model="branch.notice" style="width:300px" placeholder="现场广播/公告内容" />
        </el-form-item>
        <el-button type="primary" @click="saveBranch">发布</el-button>
        <el-button @click="load">刷新</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api, { currentUser } from '../api.js'
import { l } from '../dict.js'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const d = ref({})
const branch = reactive({ powerOutage: false, pensionDay: false, notice: '' })

async function load() {
  loading.value = true
  try {
    d.value = await api.get('/dashboard')
    Object.assign(branch, d.value.branch || {})
  } finally {
    loading.value = false
  }
}
async function saveBranch() {
  d.value.branch = await api.post('/branch', branch)
  Object.assign(branch, d.value.branch)
  ElMessage.success('网点状态已更新')
  load()
}
function pct(v) {
  const total = Object.values(d.value.channelDistribution || {}).reduce((a, b) => a + b, 0)
  return total ? Math.round(v * 100 / total) : 0
}
function loadPct(n) { return Math.min(100, n * 25) }
function winTag(s) { return { OPEN: 'success', CLOSED: 'info', BREAK: 'warning', FAULT: 'danger' }[s] }
function channelColor(k) {
  return { COUNTER: '#2f6bff', PRIORITY_WINDOW: '#7a5af8', SELF_MACHINE: '#16a05f',
    WEALTH_ROOM: '#e6832a', REMOTE_CS: '#0e8a8a', UNDECIDED: '#909399' }[k] || '#409eff'
}

onMounted(load)
</script>

<style scoped>
.bar-row { display: flex; align-items: center; margin-bottom: 10px; }
.bar-label { width: 92px; font-size: 13px; color: #444; }
.bar-row :deep(.el-progress) { flex: 1; }
</style>
