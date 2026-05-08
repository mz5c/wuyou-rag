import { createApp, h } from 'vue'

let container = null
let counter = 0

function showMessage(message, type = 'info', duration = 3000) {
  if (!container) {
    container = document.createElement('div')
    container.className = 'w-message-container'
    document.body.appendChild(container)
  }

  const id = ++counter
  const wrapper = document.createElement('div')
  wrapper.className = `w-message w-message--${type}`
  wrapper.dataset.id = id
  wrapper.style.animation = 'w-message-in 0.25s ease-out'

  const iconMap = { success: 'check', error: 'close', warning: 'warning', info: 'info' }
  const iconName = iconMap[type] || 'info'

  wrapper.innerHTML = `
    <svg class="w-message__icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="${iconName === 'check' ? 'M20 6L9 17l-5-5' : iconName === 'close' ? 'M18 6L6 18M6 6l12 12' : iconName === 'warning' ? 'M12 9v4m0 4h.01M10.29 3.86l-8.6 14.86A2 2 0 0 0 3.4 21h17.2a2 2 0 0 0 1.72-2.96l-8.6-14.86a2 2 0 0 0-3.43 0z' : 'M12 16v-4m0-4h.01M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z'}"/>
    </svg>
    <span class="w-message__text">${message}</span>
  `

  container.appendChild(wrapper)

  setTimeout(() => {
    wrapper.style.animation = 'w-message-out 0.2s ease-in forwards'
    setTimeout(() => wrapper.remove(), 200)
  }, duration)

  return id
}

export const WMessage = {
  success: (msg) => showMessage(msg, 'success'),
  error: (msg) => showMessage(msg, 'error'),
  warning: (msg) => showMessage(msg, 'warning'),
  info: (msg) => showMessage(msg, 'info')
}

// Inject styles
const style = document.createElement('style')
style.textContent = `
.w-message-container {
  position: fixed;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  pointer-events: none;
}
.w-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: var(--radius-md, 10px);
  background: var(--color-surface, #fff);
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  font-size: var(--font-size-base, 14px);
  pointer-events: auto;
  min-width: 200px;
}
.w-message--success { border-left: 3px solid #059669; }
.w-message--error { border-left: 3px solid #dc2626; }
.w-message--warning { border-left: 3px solid #d97706; }
.w-message--info { border-left: 3px solid #6b7280; }
.w-message__icon { flex-shrink: 0; }
.w-message--success .w-message__icon { color: #059669; }
.w-message--error .w-message__icon { color: #dc2626; }
.w-message--warning .w-message__icon { color: #d97706; }
.w-message--info .w-message__icon { color: #6b7280; }
@keyframes w-message-in { from { opacity: 0; transform: translateY(-12px); } to { opacity: 1; transform: translateY(0); } }
@keyframes w-message-out { from { opacity: 1; } to { opacity: 0; transform: translateY(-8px); } }
`
document.head.appendChild(style)
