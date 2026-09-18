<template>
  <router-view v-if="$route.meta.public" />
  <el-container v-else style="height: 100vh">
    <el-aside width="218px" style="background: #102347">
      <div class="logo">
        <el-icon :size="22"><OfficeBuilding /></el-icon>
        <span>建行网点服务平台</span>
      </div>
      <el-menu :default-active="activeMenu" router background-color="#102347"
               text-color="#c4d2ec" active-text-color="#ffd98a">
        <el-menu-item index="/dashboard"><el-icon><DataBoard /></el-icon><span>运营看板</span></el-menu-item>
        <el-menu-item index="/tickets"><el-icon><Tickets /></el-icon><span>排队分流</span></el-menu-item>
        <el-menu-item index="/teller"><el-icon><Monitor /></el-icon><span>窗口叫号</span></el-menu-item>
        <el-menu-item index="/reservations"><el-icon><UserFilled /></el-icon><span>适老预约</span></el-menu-item>
        <el-menu-item index="/events"><el-icon><Bell /></el-icon><span>协同事件</span></el-menu-item>
        <el-menu-item index="/archives"><el-icon><FolderOpened /></el-icon><span>运营档案</span></el-menu-item>
      </el-menu>
      <div class="board-link">
        <el-button size="small" plain @click="$router.push('/board')">
          <el-icon><View /></el-icon> 叫号大屏
        </el-button>
      </div>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div class="top-title">{{ $route.meta.title || '网点排队叫号与适老服务预约平台' }}</div>
        <div class="top-user">
          <el-tag size="small" type="warning" effect="dark">{{ roleLabel }}</el-tag>
          <span style="margin: 0 10px">{{ user.name }}</span>
          <el-button link type="primary" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { currentUser, logout } from './api.js'
import { LABELS } from './dict.js'

const route = useRoute()
const user = currentUser() || {}
const activeMenu = computed(() => route.path)
const roleLabel = LABELS.roles[user.role] || user.role
</script>

<style scoped>
.logo {
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  padding: 18px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.board-link { padding: 12px 16px; }
.board-link .el-button { width: 100%; color: #c4d2ec; background: rgba(255,255,255,.08); border: none; }
.topbar {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e8ebf2;
}
.top-title { font-size: 16px; font-weight: 700; color: #1f2d3d; }
.top-user { display: flex; align-items: center; font-size: 13px; color: #555; }
.el-menu { border-right: none; }
</style>
