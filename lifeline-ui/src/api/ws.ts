import { getAccessToken } from '@/utils/auth'

export type WsMessageHandler = (data: unknown) => void

export class MonitorWebSocket {
  private ws: WebSocket | null = null
  private url: string
  private reconnectAttempts = 0
  private maxReconnectAttempts = 10
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private onMessage: WsMessageHandler
  private onConnect: (() => void) | null
  private onDisconnect: (() => void) | null
  private shouldReconnect = true

  constructor(
    areaId: number,
    onMessage: WsMessageHandler,
    onConnect?: () => void,
    onDisconnect?: () => void
  ) {
    const token = getAccessToken() || ''
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    this.url = `${protocol}//${host}/ws/monitor/${areaId}?token=${token}`
    this.onMessage = onMessage
    this.onConnect = onConnect || null
    this.onDisconnect = onDisconnect || null
  }

  connect(): void {
    if (this.ws?.readyState === WebSocket.OPEN) return

    try {
      this.ws = new WebSocket(this.url)

      this.ws.onopen = () => {
        this.reconnectAttempts = 0
        this.onConnect?.()
      }

      this.ws.onmessage = (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data)
          this.onMessage(data)
        } catch {
          this.onMessage(event.data)
        }
      }

      this.ws.onclose = () => {
        this.onDisconnect?.()
        if (this.shouldReconnect) {
          this.scheduleReconnect()
        }
      }

      this.ws.onerror = () => {
        this.ws?.close()
      }
    } catch {
      if (this.shouldReconnect) {
        this.scheduleReconnect()
      }
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) return

    // 指数退避: 1s, 2s, 4s, 8s... 最大30s
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000)
    this.reconnectAttempts++

    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, delay)
  }

  disconnect(): void {
    this.shouldReconnect = false
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  send(data: unknown): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(typeof data === 'string' ? data : JSON.stringify(data))
    }
  }
}
