<template>
  <div class="page-table">
    <el-table
      v-loading="loading"
      :data="data"
      :border="border"
      :stripe="stripe"
      :height="height"
      @sort-change="handleSortChange"
    >
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :sortable="col.sortable"
        :fixed="col.fixed"
        :align="col.align || 'left'"
      >
        <template v-if="col.slot" #default="scope">
          <slot :name="col.slot" v-bind="scope" />
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper" v-if="showPagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="currentSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="emit('page-change', currentPage)"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface ColumnConfig {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  sortable?: boolean | 'custom'
  fixed?: boolean | 'left' | 'right'
  align?: 'left' | 'center' | 'right'
  slot?: string
}

const props = withDefaults(defineProps<{
  columns: ColumnConfig[]
  data: unknown[]
  total: number
  loading: boolean
  page: number
  size: number
  border?: boolean
  stripe?: boolean
  height?: string | number
  showPagination?: boolean
}>(), {
  border: true,
  stripe: true,
  showPagination: true,
})

const emit = defineEmits<{
  'page-change': [page: number]
  'size-change': [size: number]
  'sort-change': [sort: { prop: string; order: string }]
}>()

const currentPage = computed({
  get: () => props.page,
  set: (val) => emit('page-change', val),
})

const currentSize = computed({
  get: () => props.size,
  set: (val) => emit('size-change', val),
})

function handleSizeChange(val: number) {
  emit('size-change', val)
}

function handleSortChange({ prop, order }: { prop: string; order: string | null }) {
  emit('sort-change', { prop, order: order || '' })
}
</script>

<style scoped lang="scss">
.page-table {
  width: 100%;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: $spacing-md 0;
}
</style>
