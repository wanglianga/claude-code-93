import { createRouter, createWebHashHistory } from 'vue-router'
import { currentUser } from './api.js'

import Login from './views/Login.vue'
import Board from './views/Board.vue'
import Dashboard from './views/Dashboard.vue'
import Tickets from './views/Tickets.vue'
import Teller from './views/Teller.vue'
import Reservations from './views/Reservations.vue'
import Events from './views/Events.vue'
import Archives from './views/Archives.vue'

const routes = [
  { path: '/login', component: Login, meta: { public: true } },
  { path: '/board', component: Board, meta: { public: true } },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: Dashboard, meta: { title: '运营看板' } },
  { path: '/tickets', component: Tickets, meta: { title: '排队分流' } },
  { path: '/teller', component: Teller, meta: { title: '窗口叫号' } },
  { path: '/reservations', component: Reservations, meta: { title: '适老预约' } },
  { path: '/events', component: Events, meta: { title: '协同事件' } },
  { path: '/archives', component: Archives, meta: { title: '运营档案' } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  if (!currentUser()) return '/login'
  return true
})

export default router
