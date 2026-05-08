import { createApp, h } from 'vue'
import WDialog from './WDialog.vue'

export function WMessageBox(options) {
  const { title, message, confirmText = '确定', cancelText = '取消', type = 'warning' } = typeof options === 'string'
    ? { title: '提示', message: options }
    : options

  return new Promise((resolve, reject) => {
    const container = document.createElement('div')
    document.body.appendChild(container)

    const app = createApp({
      render() {
        return h(WDialog, {
          visible: true,
          title,
          width: '420px',
          'onUpdate:visible': (val) => { if (!val) { reject(new Error('canceled')); cleanup() } }
        }, {
          default: () => h('p', { style: 'color: var(--color-text-secondary); line-height: 1.6;' }, message),
          footer: () => [
            h('button', {
              class: 'w-button w-button--default w-button--medium',
              style: 'margin-right: 8px',
              onClick: () => { reject(new Error('canceled')); cleanup() }
            }, cancelText),
            h('button', {
              class: 'w-button w-button--primary w-button--medium',
              onClick: () => { resolve(); cleanup() }
            }, confirmText)
          ]
        })
      }
    })

    function cleanup() {
      app.unmount()
      container.remove()
    }

    app.mount(container)
  })
}

export const WConfirm = WMessageBox
