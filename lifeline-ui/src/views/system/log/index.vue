<template>
  <div class="log-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="模块">
          <el-input v-model="query.module" placeholder="请输入模块" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="操作">
          <el-input v-model="query.action" placeholder="请输入操作" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="query.userId" placeholder="用户ID" clearable style="width: 100px" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <PageTable
        :columns="columns"
        :data="dataList"
        :total="total"
        :loading="loading"
        :page="query.page!"
        :size="query.size!"
        @page-change="handlePageChange"
        @size-change="handleSizeChange"
      >
        <template #result="{ row }">
          <el-tag :type="row.result === 'SUCCESS' ? 'success' : 'danger'" size="small">
            {{ row.result === 'SUCCESS' ? '成功' : '失败' }}
          </el-tag>
        </template>
        <template #errorMsg="{ row }">
          <el-tooltip v-if="row.errorMsg" :content="row.errorMsg" placement="top" :show-after="300">
            <span class="error-text">{{ row.errorMsg }}</span>
          </el-tooltip>
          <span v-else class="text-muted">-</span>
        </template>
        <template #params="{ row }">
          <el-tooltip v-if="row.params" :content="row.params" placement="top" :show-after="300">
            <span class="params-text">{{ row.params }}</span>
          </el-tooltip>
          <span v-else class="text-muted">-</span>
        </template>
      </PageTable>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { useTable } from '@/composables/useTable'
import { getOperationLogs } from '@/api/log'
import type { OperationLog } from '@/types/auth.d'
import PageTable from '@/components/common/PageTable.vue'

const dateRange = ref<[string, string] | null>(null)

const columns = [
  { prop: 'username', label: '操作人', width: 100 },
  { prop: 'module', label: '模块', width: 110 },
  { prop: 'action', label: '操作', width: 120 },
  { prop: 'method', label: '方法', minWidth: 180 },
  { prop: 'params', label: '参数', minWidth: 160, slot: 'params' },
  { prop: 'ip', label: 'IP', width: 130 },
  { prop: 'result', label: '结果', width: 80, align: 'center' as const, slot: 'result' },
  { prop: 'errorMsg', label: '错误信息', width: 140, slot: 'errorMsg' },
  { prop: 'operateTime', label: '操作时间', width: 170 },
]

const { loading, dataList, total, query, fetch, handlePageChange, handleSizeChange } = useTable<OperationLog>({
  fetchApi: getOperationLogs,
})

function handleSearch() {
  query.page = 1
  if (dateRange.value) {
    query.startTime = dateRange.value[0]
    query.endTime = dateRange.value[1]
  } else {
    query.startTime = undefined
    query.endTime = undefined
  }
  fetch()
}

function handleReset() {
  query.module = undefined
  query.action = undefined
  query.userId = undefined
  dateRange.value = null
  query.startTime = undefined
  query.endTime = undefined
  handleSearch()
}
</script>

<style scoped lang="scss">
.log-page {
  padding: $spacing-md;
}

.search-bar {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-md $spacing-md 0;
  margin-bottom: $spacing-md;
}

.table-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-md;
}

.error-text {
  display: inline-block;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: $danger-color;
  font-size: 12px;
  cursor: pointer;
}

.params-text {
  display: inline-block;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: $text-secondary;
  font-size: 12px;
  font-family: monospace;
  cursor: pointer;
}

.text-muted {
  color: $text-placeholder;
}
</style>
