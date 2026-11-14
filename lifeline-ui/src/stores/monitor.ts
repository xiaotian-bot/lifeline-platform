import { defineStore } from 'pinia'
import { shallowRef } from 'vue'
import type { SensorData } from '@/types/monitor.d'

export const useMonitorStore = defineStore('monitor', () => {
  const realtimeData = shallowRef<SensorData[]>([])
  const connected = shallowRef(false)

  function setRealtimeData(data: SensorData[]): void {
    realtimeData.value = data
  }

  function updateDeviceData(deviceId: string, data: SensorData): void {
    const idx = realtimeData.value.findIndex((d) => d.deviceId === deviceId)
    if (idx >= 0) {
      const copy = [...realtimeData.value]
      copy[idx] = data
      realtimeData.value = copy
    } else {
      realtimeData.value = [...realtimeData.value, data]
    }
  }

  function setConnected(status: boolean): void {
    connected.value = status
  }

  function reset(): void {
    realtimeData.value = []
    connected.value = false
  }

  return {
    realtimeData,
    connected,
    setRealtimeData,
    updateDeviceData,
    setConnected,
    reset,
  }
})
