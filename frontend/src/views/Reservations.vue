<template>
  <div v-loading="loading">
    <el-row :gutter="14">
      <el-col :span="9">
        <el-card>
          <template #header><b>老人服务预约登记</b></template>
          <el-form :model="form" label-width="104px" size="small">
            <el-form-item label="老人姓名"><el-input v-model="form.elderName" /></el-form-item>
            <el-form-item label="手机后四位"><el-input v-model="form.phoneTail" maxlength="4" /></el-form-item>
            <el-form-item label="办理业务">
              <el-select v-model="form.businessType" style="width:100%">
                <el-option v-for="x in biz" :key="x.value" :label="x.label" :value="x.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="预约时间">
              <el-date-picker v-model="reserveTime" type="datetime" placeholder="选择到店时间"
                              value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
            </el-form-item>
            <el-form-item label="无障碍需求">
              <el-checkbox v-model="form.wheelchair">轮椅通行（坡道/低位窗）</el-checkbox>
              <el-checkbox v-model="form.hearingGuide">听力引导（助听设备）</el-checkbox>
            </el-form-item>
            <el-form-item label="适老安排">
              <el-checkbox v-model="form.escortNeeded">陪同引导</el-checkbox>
              <el-checkbox v-model="form.priorityWindow">敬老优先窗</el-checkbox>
              <el-checkbox v-model="form.materialPreReview">材料预审</el-checkbox>
            </el-form-item>
            <el-form-item label="家属联系方式"><el-input v-model="form.familyContact" placeholder="如 女儿 138****6677" /></el-form-item>
            <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
            <el-button type="primary" style="width:100%" @click="create">提交预约</el-button>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="15">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between">
              <b>适老预约列表（{{ list.length }}）</b>
              <el-button size="small" @click="load">刷新</el-button>
            </div>
          </template>
          <el-table :data="list" size="small">
            <el-table-column prop="elderName" label="老人" width="80" />
            <el-table-column label="业务" width="80">
              <template #default="{ row }">{{ l('businessTypes', row.businessType) }}</template>
            </el-table-column>
            <el-table-column label="预约时间" width="150">
              <template #default="{ row }">{{ fmt(row.reserveTime) }}</template>
            </el-table-column>
            <el-table-column label="特殊需求" min-width="150">
              <template #default="{ row }">
                <el-tag v-if="row.wheelchair" size="small" type="info" style="margin-right:4px">轮椅</el-tag>
                <el-tag v-if="row.hearingGuide" size="small" type="warning" style="margin-right:4px">听力</el-tag>
                <el-tag v-if="row.materialPreReview" size="small" type="success" style="margin-right:4px">材料预审</el-tag>
                <el-tag v-if="row.familyRemoteConfirmed" size="small" type="primary">家属已确认</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="陪同人" width="100">
              <template #default="{ row }">{{ row.escortStaff || '待安排' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="resTag(row.status)">{{ l('reservationStatuses', row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="排队号" width="70">
              <template #default="{ row }"><span class="mono-no">{{ row.ticketNo || '-' }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="230" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status==='PENDING'" size="small" type="primary" @click="openArrange(row)">安排</el-button>
                <el-button v-if="!row.familyRemoteConfirmed && row.status!=='CANCELLED'" size="small"
                           @click="familyConfirm(row)">家属远程确认</el-button>
                <el-button v-if="row.status==='ARRANGED'" size="small" type="success" @click="arrive(row)">到店取号</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card style="margin-top:14px" header="适老服务说明">
          <el-steps :active="4" align-center>
            <el-step title="预约登记" description="记录轮椅/听力/家属联系" />
            <el-step title="行前安排" description="陪同人 + 优先窗 + 材料预审 + 家属远程确认" />
            <el-step title="到店取号" description="自动高优先级并入敬老优先窗队列" />
            <el-step title="陪同办结" description="全程引导，档案回访关怀" />
          </el-steps>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="arrangeVisible" title="安排适老服务" width="480px">
      <el-form label-width="110px">
        <el-form-item label="陪同引导人">
          <el-select v-model="arrangeForm.escortStaff" style="width:100%">
            <el-option label="大堂引导员" value="大堂引导员" />
            <el-option label="安保 吴刚" value="安保吴刚" />
            <el-option label="志愿者引导" value="志愿者引导" />
          </el-select>
        </el-form-item>
        <el-form-item label="敬老优先窗"><el-switch v-model="arrangeForm.priorityWindow" /></el-form-item>
        <el-form-item label="材料预审"><el-switch v-model="arrangeForm.materialPreReview" /></el-form-item>
        <el-form-item label="家属已远程确认"><el-switch v-model="arrangeForm.familyRemoteConfirmed" /></el-form-item>
        <el-form-item label="家属确认备注"><el-input v-model="arrangeForm.familyNote" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="arrangeVisible=false">取消</el-button>
        <el-button type="primary" @click="confirmArrange">确认安排</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../api.js'
import { l } from '../dict.js'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const list = ref([])
const biz = ref([])
const reserveTime = ref(null)
const form = reactive({
  elderName: '', phoneTail: '', businessType: 'SOCIAL_CARD',
  wheelchair: false, hearingGuide: false,
  escortNeeded: true, priorityWindow: true, materialPreReview: true,
  familyContact: '', remark: ''
})

const arrangeVisible = ref(false)
const arrangeRow = ref(null)
const arrangeForm = reactive({ escortStaff: '大堂引导员', priorityWindow: true,
  materialPreReview: true, familyRemoteConfirmed: false, familyNote: '' })

async function load() {
  loading.value = true
  try {
    const [r, e] = await Promise.all([api.get('/reservations'), api.get('/meta/enums')])
    list.value = r
    biz.value = e.businessTypes
  } finally { loading.value = false }
}

async function create() {
  if (!form.elderName) return ElMessage.warning('请填写老人姓名')
  await api.post('/reservations', { ...form, reserveTime: reserveTime.value })
  ElMessage.success('预约已提交')
  Object.assign(form, { elderName: '', phoneTail: '', familyContact: '', remark: '',
    wheelchair: false, hearingGuide: false })
  reserveTime.value = null
  load()
}

function openArrange(row) {
  arrangeRow.value = row
  Object.assign(arrangeForm, { escortStaff: '大堂引导员', priorityWindow: true,
    materialPreReview: true, familyRemoteConfirmed: row.familyRemoteConfirmed, familyNote: row.familyNote || '' })
  arrangeVisible.value = true
}
async function confirmArrange() {
  await api.post(`/reservations/${arrangeRow.value.id}/arrange`, arrangeForm)
  ElMessage.success('已安排，并生成无障碍协同事项')
  arrangeVisible.value = false
  load()
}
async function familyConfirm(row) {
  await api.post(`/reservations/${row.id}/family-confirm`, { note: '家属视频确认知悉并同意办理' })
  ElMessage.success('家属远程确认完成')
  load()
}
async function arrive(row) {
  const t = await api.post(`/reservations/${row.id}/arrive`, {})
  ElMessage.success(`老人已到店，自动取号 ${t.ticketNo}，已分流至 ${t.assignedWindow || '敬老优先窗'}`)
  load()
}

function resTag(s) {
  return { PENDING: 'warning', ARRANGED: 'primary', ARRIVED: 'success', DONE: 'success', CANCELLED: 'info' }[s]
}
function fmt(t) { return t ? t.replace('T', ' ').slice(0, 16) : '-' }

onMounted(load)
</script>
