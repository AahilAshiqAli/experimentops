export const TOAST_TYPES = {
  SUCCESS: 'success',
  ERROR: 'error',
  INFO: 'info',
  WARNING: 'warning',
} as const

type ToastType = (typeof TOAST_TYPES)[keyof typeof TOAST_TYPES]

type ToastOptions = {
  duration?: number
  message: string
  type?: ToastType
}

const TOAST_CONTAINER_ID = 'experimentops-toast-container'
const DEFAULT_DURATION = 4500

const toastIcons: Record<ToastType, string> = {
  success: '✓',
  error: '!',
  info: 'i',
  warning: '!',
}

function getContainer() {
  const existingContainer = document.getElementById(TOAST_CONTAINER_ID)

  if (existingContainer) {
    return existingContainer
  }

  const container = document.createElement('div')
  container.className = 'toast-notification toast-notification-topRight'
  container.id = TOAST_CONTAINER_ID
  container.setAttribute('aria-live', 'polite')
  container.setAttribute('aria-atomic', 'true')
  document.body.append(container)

  return container
}

function removeToast(notice: HTMLElement) {
  notice.classList.add('toast-notification-notice-leave')

  window.setTimeout(() => {
    notice.remove()

    const container = document.getElementById(TOAST_CONTAINER_ID)
    if (container?.childElementCount === 0) {
      container.remove()
    }
  }, 200)
}

function open({
  type = TOAST_TYPES.INFO,
  message,
  duration = DEFAULT_DURATION,
}: ToastOptions) {
  if (!message || typeof document === 'undefined') {
    return
  }

  const notice = document.createElement('div')
  notice.className = `toast-notification-notice toast-notification-notice-${type}`
  notice.setAttribute('role', type === TOAST_TYPES.ERROR ? 'alert' : 'status')

  const icon = document.createElement('span')
  icon.className = `toast-notification-notice-icon toast-notification-notice-icon-${type}`
  icon.setAttribute('aria-hidden', 'true')
  icon.textContent = toastIcons[type]

  const messageElement = document.createElement('div')
  messageElement.className = 'toast-notification-notice-message'
  messageElement.textContent = message

  const closeButton = document.createElement('button')
  closeButton.className = 'toast-notification-close-x'
  closeButton.type = 'button'
  closeButton.setAttribute('aria-label', 'Close notification')
  closeButton.innerHTML = '&times;'

  const content = document.createElement('div')
  content.className = 'toast-notification-notice-content'
  content.append(icon, messageElement)
  notice.append(content, closeButton)

  const container = getContainer()
  container.append(notice)

  const timeoutId = window.setTimeout(() => removeToast(notice), duration)
  closeButton.addEventListener('click', () => {
    window.clearTimeout(timeoutId)
    removeToast(notice)
  })
}

export const Toaster = {
  open,
  success: (message: string, duration?: number) =>
    open({ type: TOAST_TYPES.SUCCESS, message, duration }),
  error: (message: string, duration?: number) =>
    open({ type: TOAST_TYPES.ERROR, message, duration }),
  info: (message: string, duration?: number) =>
    open({ type: TOAST_TYPES.INFO, message, duration }),
  warning: (message: string, duration?: number) =>
    open({ type: TOAST_TYPES.WARNING, message, duration }),
}
