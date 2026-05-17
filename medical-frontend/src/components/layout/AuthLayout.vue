<script setup lang="ts">
</script>

<template>
  <div class="auth-layout">
    <div class="auth-content">
      <div class="auth-logo">
        <h1>🏥 医疗门诊系统</h1>
      </div>
      <div class="auth-card">
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-layout {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #06b6d4 0%, #14b8a6 100%);
}

.auth-content {
  text-align: center;
}

.auth-logo {
  margin-bottom: 30px;
}

.auth-logo h1 {
  color: white;
  font-size: 32px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}

.auth-card {
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  min-width: 400px;
}
</style>
