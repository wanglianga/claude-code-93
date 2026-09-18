<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="brand">
        <el-icon :size="30" color="#c8161d"><CreditCard /></el-icon>
        <div>
          <div class="brand-title">中国建设银行</div>
          <div class="brand-sub">网点排队叫号与适老服务预约平台</div>
        </div>
      </div>
      <el-form :model="form" @keyup.enter="doLogin">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" size="large" type="password"
                    placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading"
                   @click="doLogin">登 录</el-button>
      </el-form>
      <el-divider>演示账号（密码均为 123456）</el-divider>
      <div class="accounts">
        <el-tag v-for="a in accounts" :key="a.u" class="acc-tag"
                @click="quick(a.u)" effect="plain">
          {{ a.label }}：{{ a.u }}
        </el-tag>
      </div>
      <div class="board-entry">
        <router-link to="/board">→ 进入叫号大屏（无需登录）</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api.js'

const router = useRouter()
const form = reactive({ username: 'manager01', password: '123456' })
const loading = ref(false)

const accounts = [
  { u: 'manager01', label: '大堂经理' },
  { u: 'teller01', label: '现金柜员' },
  { u: 'teller04', label: '敬老窗柜员' },
  { u: 'wealth01', label: '理财经理' },
  { u: 'security01', label: '安保' },
  { u: 'cs01', label: '客服' }
]

function quick(u) {
  form.username = u
  form.password = '123456'
}

async function doLogin() {
  if (!form.username || !form.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    const data = await api.post('/auth/login', form)
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data))
    ElMessage.success('欢迎，' + data.name)
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0b1f45 0%, #163a75 55%, #1d4e9e 100%);
}
.login-card {
  width: 420px;
  background: #fff;
  border-radius: 14px;
  padding: 34px 34px 26px;
  box-shadow: 0 24px 60px rgba(0,0,0,.35);
}
.brand { display: flex; align-items: center; gap: 12px; margin-bottom: 26px; }
.brand-title { font-size: 20px; font-weight: 800; color: #c8161d; letter-spacing: 2px; }
.brand-sub { font-size: 13px; color: #555; margin-top: 2px; }
.accounts { display: flex; flex-wrap: wrap; gap: 8px; }
.acc-tag { cursor: pointer; }
.board-entry { text-align: center; margin-top: 16px; font-size: 13px; }
</style>
