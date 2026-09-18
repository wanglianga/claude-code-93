import { reactive } from 'vue'
import { api } from './api'

/** 全局共享数据：轮询刷新，六方看到同一份排队实况 */
export const store = reactive({
  ready: false,
  tickets: [],
  counters: [],
  events: [],
  staff: [],
  dash: null,

  async refresh(silent = true) {
    try {
      const [t, c, e, s, d] = await Promise.all([
        api.get('/tickets'), api.get('/counters'),
        api.get('/events'), api.get('/staff'), api.get('/dashboard')
      ])
      this.tickets = t
      this.counters = c
      this.events = e
      this.staff = s
      this.dash = d
      this.ready = true
    } catch (err) {
      if (!silent) throw err
    }
  }
})

let timer = null
export function startPolling() {
  store.refresh()
  stopPolling()
  timer = setInterval(() => store.refresh(), 5000)
}
export function stopPolling() {
  if (timer) { clearInterval(timer); timer = null }
}
