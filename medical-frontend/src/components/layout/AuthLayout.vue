<script setup lang="ts">
const brandFeatures = [
  '在线预约挂号',
  '门诊签到与候诊',
  '处方查询与费用管理'
]
</script>

<template>
  <div class="auth-layout">
    <aside class="auth-brand" aria-label="系统介绍">
      <div class="brand-inner">
        <div class="brand-mark" aria-hidden="true">
          <svg class="brand-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect x="3" y="3" width="18" height="18" rx="4" stroke="currentColor" stroke-width="1.5" />
            <path d="M12 8v8M8 12h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
          </svg>
          <span class="brand-mark-label">医疗服务平台</span>
        </div>

        <h1 class="brand-title">医疗门诊系统</h1>
        <p class="brand-subtitle">专业、安全、便捷的门诊全流程服务</p>

        <div class="brand-illustration">
          <img
            src="/images/auth-hospital.png"
            alt="医院门诊大楼示意图"
            width="360"
            height="280"
            loading="eager"
            decoding="async"
          />
        </div>

        <ul class="brand-features">
          <li v-for="item in brandFeatures" :key="item">{{ item }}</li>
        </ul>
      </div>
    </aside>

    <main class="auth-main">
      <div class="auth-card">
        <slot />
      </div>
      <p class="auth-footer">医疗门诊系统 · 仅供学习与演示使用</p>
    </main>
  </div>
</template>

<style scoped>
.auth-layout {
  display: flex;
  min-height: 100%;
  width: 100%;
  background: #f4f7fb;
}

.auth-brand {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  background: linear-gradient(160deg, #e8f4f8 0%, #f0f7fa 45%, #eef6f3 100%);
  border-right: 1px solid #dce8ef;
}

.brand-inner {
  max-width: 420px;
  width: 100%;
}

.brand-mark {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.brand-icon {
  width: 36px;
  height: 36px;
  color: #0d6e6a;
  flex-shrink: 0;
}

.brand-mark-label {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: #0d6e6a;
}

.brand-title {
  margin: 0 0 10px;
  font-size: 28px;
  font-weight: 700;
  color: #1e3a4a;
  line-height: 1.3;
  letter-spacing: 0.02em;
}

.brand-subtitle {
  margin: 0 0 28px;
  font-size: 15px;
  line-height: 1.6;
  color: #5a7282;
}

.brand-illustration {
  margin-bottom: 28px;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 8px 24px rgba(30, 58, 74, 0.08);
  border: 1px solid #e2ecf2;
}

.brand-illustration img {
  display: block;
  width: 100%;
  height: auto;
  object-fit: cover;
}

.brand-features {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.brand-features li {
  position: relative;
  padding-left: 18px;
  font-size: 14px;
  color: #4a6272;
  line-height: 1.5;
}

.brand-features li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #0d9488;
}

.auth-main {
  flex: 0 0 480px;
  max-width: 520px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 32px;
  background: #f8fafc;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 36px 32px 32px;
  border: 1px solid #e5edf3;
  box-shadow: 0 4px 20px rgba(30, 58, 74, 0.06);
}

.auth-footer {
  margin-top: 24px;
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
}

@media (max-width: 900px) {
  .auth-layout {
    flex-direction: column;
  }

  .auth-brand {
    flex: none;
    border-right: none;
    border-bottom: 1px solid #dce8ef;
    padding: 32px 24px;
  }

  .brand-inner {
    max-width: 100%;
    text-align: center;
  }

  .brand-mark {
    justify-content: center;
  }

  .brand-illustration {
    max-width: 320px;
    margin-left: auto;
    margin-right: auto;
  }

  .brand-features {
    align-items: center;
  }

  .auth-main {
    flex: 1;
    max-width: none;
    width: 100%;
    padding: 24px 20px 32px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .brand-illustration,
  .auth-card {
    box-shadow: none;
  }
}
</style>
