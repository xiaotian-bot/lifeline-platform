import { onMounted, onUnmounted } from 'vue'
import { MonitorWebSocket } from '@/api/ws'
import { useMonitorStore } from '@/stores/monitor'
import type { WsMonitorMessage, SensorData } from '@/types/monitor.d'

interface UseWebSocketOptions {
  areaId: number
  onMessage?: (data: WsMonitorMessage) => void
  autoConnect?: boolean
}

export function useWebSocket({ areaId, onMessage, autoConnect = true }: UseWebSocketOptions) {
  const monitorStore = useMonitorStore()
  let wsInstance: MonitorWebSocket | null = null

  function connect() {
    if (wsInstance) return

    wsInstance = new MonitorWebSocket(
      areaId,
      (raw: unknown) => {
        const msg = raw as WsMonitorMessage
        // 自动更新 store 中的设备实时数据
        if (msg.type === 'data' && msg.payload) {
          const data = msg.payload as SensorData
          if (data.deviceId) {
            monitorStore.updateDeviceData(data.deviceId, data)
          }
        }
        onMessage?.(msg)
      },
      () => {
        monitorStore.setConnected(true)
      },
      () => {
        monitorStore.setConnected(false)
      }
    )
    wsInstance.connect()
  }

  function disconnect() {
    wsInstance?.disconnect()
    wsInstance = null
    monitorStore.setConnected(false)
  }

  function send(data: unknown) {
    wsInstance?.send(data)
  }

  if (autoConnect) {
    onMounted(() => {
      connect()
    })

    onUnmounted(() => {
      disconnect()
    })
  }

  return {
    connect,
    disconnect,
    send,
  }
}
