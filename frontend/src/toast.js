import { reactive } from 'vue'

export const toastState = reactive({ msg: '', type: 'ok', show: false })
let hideTimer = null

export function toast(msg, type = 'ok') {
  toastState.msg = msg
  toastState.type = type
  toastState.show = true
  if (hideTimer) clearTimeout(hideTimer)
  hideTimer = setTimeout(() => { toastState.show = false }, 2600)
}
