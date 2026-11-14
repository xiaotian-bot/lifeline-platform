<template>
  <div class="error-page">
    <div class="error-content">
      <h1 class="error-code">403</h1>
      <p class="error-desc">抱歉，您没有权限访问该页面</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped lang="scss">
.error-page {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-color-page;
}

.error-content {
  text-align: center;
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  color: $primary-color;
  line-height: 1;
  margin-bottom: $spacing-md;
}

.error-desc {
  font-size: 16px;
  color: $text-secondary;
  margin-bottom: $spacing-xl;
}
</style>
