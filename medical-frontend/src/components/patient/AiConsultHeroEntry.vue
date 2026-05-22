<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()

const goConsult = () => {
  router.push('/patient/consult')
}
</script>

<template>
  <button
    type="button"
    class="ai-hero-entry"
    aria-label="进入 AI 智能问诊：多 Agent 协作与知识图谱辅助分析"
    @click="goConsult"
  >
    <div class="hero-bubble">
      <span class="bubble-glow" aria-hidden="true" />
      <span class="bubble-ring" aria-hidden="true" />
      <span class="bubble-tail" aria-hidden="true" />

      <span class="sparkle sparkle-a" aria-hidden="true" />
      <span class="sparkle sparkle-b" aria-hidden="true" />
      <span class="sparkle sparkle-c" aria-hidden="true" />

      <span class="ai-sprite-wrap" aria-hidden="true">
        <svg
          class="ai-sprite"
          viewBox="0 0 56 56"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          <defs>
            <linearGradient id="heroSpriteBody" x1="8" y1="12" x2="48" y2="48">
              <stop offset="0%" stop-color="#5eead4" />
              <stop offset="50%" stop-color="#14b8a6" />
              <stop offset="100%" stop-color="#0d9488" />
            </linearGradient>
            <linearGradient id="heroSpriteCheek" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stop-color="#99f6e4" stop-opacity="0.9" />
              <stop offset="100%" stop-color="#2dd4bf" stop-opacity="0.4" />
            </linearGradient>
          </defs>
          <line x1="28" y1="10" x2="28" y2="4" stroke="#0f766e" stroke-width="2" stroke-linecap="round" />
          <circle cx="28" cy="3" r="4" fill="#fef08a" stroke="#fbbf24" stroke-width="1.5" class="antenna-glow" />
          <ellipse cx="28" cy="34" rx="22" ry="19" fill="url(#heroSpriteBody)" stroke="#fff" stroke-width="2" />
          <ellipse cx="18" cy="36" rx="5" ry="3" fill="url(#heroSpriteCheek)" />
          <ellipse cx="38" cy="36" rx="5" ry="3" fill="url(#heroSpriteCheek)" />
          <ellipse cx="21" cy="31" rx="4" ry="5" fill="#134e4a" />
          <ellipse cx="35" cy="31" rx="4" ry="5" fill="#134e4a" />
          <circle cx="22" cy="30" r="1.5" fill="#ecfdf5" />
          <circle cx="36" cy="30" r="1.5" fill="#ecfdf5" />
          <path d="M22 38 Q28 43 34 38" stroke="#134e4a" stroke-width="1.8" stroke-linecap="round" fill="none" />
          <ellipse cx="8" cy="32" rx="5" ry="4" fill="#2dd4bf" stroke="#0f766e" stroke-width="1" class="sprite-wave" />
        </svg>
      </span>

      <div class="hero-copy">
        <div class="hero-copy-top">
          <span class="ai-chip">
            <svg class="chip-spark" width="10" height="10" viewBox="0 0 10 10" aria-hidden="true">
              <path d="M5 0 L6 4 L10 5 L6 6 L5 10 L4 6 L0 5 L4 4 Z" fill="currentColor" />
            </svg>
            AI 智能模块
          </span>
          <span class="hero-tag">多 Agent · 知识图谱</span>
        </div>
        <h2 class="hero-title">智能问诊助手</h2>
        <p class="hero-desc">
          描述症状即可获得 AI 初步分析，结合知识图谱证据与多 Agent 协作，为您提供专业参考建议。
        </p>
      </div>

      <span class="hero-cta" aria-hidden="true">
        <span class="cta-text">立即问诊</span>
        <el-icon class="cta-icon"><ArrowRight /></el-icon>
      </span>
    </div>
  </button>
</template>

<style scoped>
.ai-hero-entry {
  display: block;
  width: 100%;
  margin: 0 0 24px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
}

.ai-hero-entry:focus-visible .hero-bubble {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 4px;
}

.hero-bubble {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20px;
  width: 100%;
  min-height: 88px;
  padding: 20px 24px;
  border-radius: 20px;
  background: linear-gradient(135deg, #ffffff 0%, #ecfdf5 40%, #99f6e4 100%);
  border: 2px solid #5eead4;
  box-shadow:
    0 8px 28px rgba(13, 148, 136, 0.2),
    0 0 0 1px rgba(13, 148, 136, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 1);
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease;
  animation: hero-float 3.5s ease-in-out infinite;
  overflow: hidden;
}

.bubble-glow {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 10% 20%, rgba(254, 240, 138, 0.35), transparent 50%),
    radial-gradient(ellipse at 90% 80%, rgba(45, 212, 191, 0.4), transparent 55%);
  pointer-events: none;
}

.bubble-ring {
  position: absolute;
  inset: 0;
  border-radius: 20px;
  border: 2px solid transparent;
  background: linear-gradient(135deg, #2dd4bf, #fbbf24, #7c3aed) border-box;
  -webkit-mask:
    linear-gradient(#fff 0 0) padding-box,
    linear-gradient(#fff 0 0);
  mask:
    linear-gradient(#fff 0 0) padding-box,
    linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  opacity: 0;
  transition: opacity 0.22s ease;
  pointer-events: none;
}

.bubble-tail {
  position: absolute;
  left: 32px;
  bottom: -10px;
  width: 20px;
  height: 20px;
  background: linear-gradient(135deg, #d1fae5, #6ee7b7);
  border-right: 2px solid #5eead4;
  border-bottom: 2px solid #5eead4;
  transform: rotate(45deg);
  border-radius: 0 0 6px 0;
  pointer-events: none;
}

.sparkle {
  position: absolute;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 0 10px #fde047;
  opacity: 0.6;
  animation: sparkle-pulse 2.2s ease-in-out infinite;
}

.sparkle-a { top: 12px; right: 120px; width: 8px; height: 8px; }
.sparkle-b { top: 24px; right: 200px; width: 6px; height: 6px; animation-delay: 0.5s; }
.sparkle-c { bottom: 16px; right: 160px; width: 7px; height: 7px; animation-delay: 1s; }

.ai-sprite-wrap {
  flex-shrink: 0;
  width: 72px;
  height: 72px;
  transition: transform 0.22s ease;
}

.ai-sprite {
  width: 72px;
  height: 72px;
  filter: drop-shadow(0 4px 8px rgba(13, 148, 136, 0.3));
}

.antenna-glow { animation: antenna-blink 2s ease-in-out infinite; }
.sprite-wave { transform-origin: 12px 32px; animation: wave-hand 1.2s ease-in-out infinite; }

.hero-copy {
  flex: 1;
  min-width: 0;
  position: relative;
  z-index: 1;
}

.hero-copy-top {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}

.ai-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px 3px 8px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.06em;
  color: #fff;
  background: linear-gradient(90deg, #7c3aed 0%, #0d9488 100%);
  border-radius: 8px;
}

.chip-spark { color: #fde047; }

.hero-tag {
  font-size: 12px;
  font-weight: 600;
  color: #0f766e;
  padding: 2px 10px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 6px;
  border: 1px solid #a7f3d0;
}

.hero-title {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 700;
  color: #134e4a;
  line-height: 1.3;
}

.hero-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: #115e59;
  max-width: 520px;
}

.hero-cta {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 44px;
  padding: 0 20px;
  background: linear-gradient(135deg, #0d9488, #059669);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  border-radius: 22px;
  box-shadow: 0 4px 14px rgba(13, 148, 136, 0.4);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.cta-icon {
  font-size: 18px;
}

.ai-hero-entry:hover .hero-bubble {
  transform: translateY(-4px);
  box-shadow:
    0 16px 40px rgba(13, 148, 136, 0.28),
    0 0 0 3px rgba(45, 212, 191, 0.35);
}

.ai-hero-entry:hover .bubble-ring { opacity: 0.85; }
.ai-hero-entry:hover .ai-sprite-wrap { transform: scale(1.08) rotate(-5deg); }
.ai-hero-entry:hover .hero-cta {
  transform: scale(1.05);
  box-shadow: 0 6px 18px rgba(13, 148, 136, 0.5);
}

@keyframes hero-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

@keyframes sparkle-pulse {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.1); }
}

@keyframes antenna-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.55; }
}

@keyframes wave-hand {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(-12deg); }
  75% { transform: rotate(8deg); }
}

@media (max-width: 768px) {
  .hero-bubble {
    flex-wrap: wrap;
    padding: 16px;
    gap: 12px;
  }

  .ai-sprite-wrap {
    width: 56px;
    height: 56px;
  }

  .ai-sprite {
    width: 56px;
    height: 56px;
  }

  .hero-title {
    font-size: 18px;
  }

  .hero-desc {
    font-size: 13px;
  }

  .hero-cta {
    width: 100%;
    justify-content: center;
    margin-top: 4px;
  }

  .sparkle { display: none; }
}

@media (prefers-reduced-motion: reduce) {
  .hero-bubble { animation: none; }
  .sparkle, .antenna-glow, .sprite-wave { animation: none; }
  .ai-hero-entry:hover .hero-bubble,
  .ai-hero-entry:hover .ai-sprite-wrap,
  .ai-hero-entry:hover .hero-cta { transform: none; }
}
</style>
