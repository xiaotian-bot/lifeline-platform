import { ref, reactive } from 'vue'
import type { PageQuery } from '@/types/api.d'

interface UseTableOptions<T> {
  fetchApi: (params: Record<string, unknown>) => Promise<{ records: T[]; total: number }>
  defaultQuery?: Record<string, unknown>
  immediate?: boolean
}

export function useTable<T>({ fetchApi, defaultQuery = {}, immediate = true }: UseTableOptions<T>) {
  const loading = ref(false)
  const dataList = ref<T[]>([])
  const total = ref(0)

  const query = reactive<PageQuery & Record<string, unknown>>({
    page: 1,
    size: 20,
    ...defaultQuery,
  })

  async function fetch() {
    loading.value = true
    try {
      const res = await fetchApi(query as Record<string, unknown>)
      dataList.value = res.records
      // 后端 Long→String 序列化：total 可能为字符串，统一归一为 number（历史数据本就是 number 不受影响）
      total.value = Number(res.total) || 0
    } catch (error) {
      console.error('表格数据加载失败:', error)
    } finally {
      loading.value = false
    }
  }

  function reset() {
    query.page = 1
    query.size = 20
    // 重置搜索条件（保留分页参数）
    Object.keys(query).forEach((key) => {
      if (key !== 'page' && key !== 'size' && key !== 'orderBy' && key !== 'orderDir') {
        if (key in defaultQuery) {
          query[key] = defaultQuery[key]
        } else {
          delete query[key]
        }
      }
    })
    fetch()
  }

  function handlePageChange(page: number) {
    query.page = page
    fetch()
  }

  function handleSizeChange(size: number) {
    query.size = size
    query.page = 1
    fetch()
  }

  if (immediate) {
    fetch()
  }

  return {
    loading,
    dataList,
    total,
    query,
    fetch,
    reset,
    handlePageChange,
    handleSizeChange,
  }
}
