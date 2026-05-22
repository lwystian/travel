<template>
  <main class="site-access-notice" :class="`is-${mode}`">
    <section class="notice-shell">
      <div class="brand-mark">
        <span>{{ mode === 'closed' ? 'OFF' : 'PC' }}</span>
      </div>
      <p class="eyebrow">{{ mode === 'closed' ? 'Service Notice' : 'Desktop Experience' }}</p>
      <h1>{{ title }}</h1>
      <p class="message">{{ message }}</p>
      <div class="notice-divider"></div>
      <p class="contact">{{ contact }}</p>
      <div v-if="supportQrImageUrl || supportCredential" class="support-card">
        <img v-if="supportQrImageUrl" :src="supportQrImageUrl" alt="客服二维码" />
        <div>
          <strong>客服凭证</strong>
          <span>{{ supportCredential || '请扫码打开客服入口' }}</span>
        </div>
      </div>
      <div class="notice-actions">
        <button type="button" @click="reloadPage">重新检测</button>
        <button v-if="supportUrl || supportCredential" type="button" class="support-btn" @click="openSupport">
          {{ copied ? '已复制凭证' : supportButtonText }}
        </button>
      </div>
    </section>
  </main>
</template>

<script setup>
import { ref } from 'vue'

const copied = ref(false)

const props = defineProps({
  mode: {
    type: String,
    default: 'closed'
  },
  title: {
    type: String,
    default: '网站维护中'
  },
  message: {
    type: String,
    default: '我们正在进行系统维护与服务升级，完成后将第一时间恢复访问。'
  },
  contact: {
    type: String,
    default: '如有紧急订单或出行问题，请联系官方客服处理。'
  },
  supportButtonText: {
    type: String,
    default: '联系官方客服'
  },
  supportUrl: {
    type: String,
    default: ''
  },
  supportCredential: {
    type: String,
    default: ''
  },
  supportQrImageUrl: {
    type: String,
    default: ''
  }
})

const reloadPage = () => {
  window.location.reload()
}

const openSupport = async () => {
  if (props.supportUrl) {
    window.open(props.supportUrl, '_blank', 'noopener')
    return
  }
  if (props.supportCredential && navigator.clipboard) {
    await navigator.clipboard.writeText(props.supportCredential)
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 1800)
  }
}
</script>

<style scoped>
.site-access-notice {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px;
  background:
    radial-gradient(circle at 18% 16%, rgba(59, 130, 246, 0.18), transparent 30%),
    radial-gradient(circle at 82% 12%, rgba(20, 184, 166, 0.18), transparent 28%),
    linear-gradient(135deg, #f8fafc 0%, #eef4ff 48%, #f7fbff 100%);
  color: #102033;
}

.site-access-notice.is-mobile {
  background:
    radial-gradient(circle at 16% 18%, rgba(245, 158, 11, 0.18), transparent 30%),
    radial-gradient(circle at 84% 20%, rgba(14, 165, 233, 0.16), transparent 30%),
    linear-gradient(135deg, #fffaf1 0%, #f5fbff 54%, #f8fafc 100%);
}

.notice-shell {
  width: min(720px, 100%);
  padding: 52px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 32px 90px rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(18px);
  text-align: center;
}

.brand-mark {
  width: 82px;
  height: 82px;
  display: grid;
  place-items: center;
  margin: 0 auto 22px;
  border-radius: 24px;
  background: linear-gradient(135deg, #0f172a, #1d4ed8);
  color: #fff;
  box-shadow: 0 18px 40px rgba(37, 99, 235, 0.32);
  font-weight: 800;
  letter-spacing: 0.04em;
}

.is-mobile .brand-mark {
  background: linear-gradient(135deg, #0f172a, #0f766e);
  box-shadow: 0 18px 40px rgba(15, 118, 110, 0.28);
}

.eyebrow {
  margin: 0 0 12px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
  text-transform: uppercase;
}

.is-mobile .eyebrow {
  color: #0f766e;
}

h1 {
  margin: 0;
  font-size: clamp(32px, 6vw, 54px);
  line-height: 1.08;
  font-weight: 900;
  color: #0f172a;
}

.message {
  max-width: 580px;
  margin: 22px auto 0;
  color: #475569;
  font-size: 18px;
  line-height: 1.9;
}

.notice-divider {
  width: 72px;
  height: 4px;
  margin: 28px auto;
  border-radius: 999px;
  background: #2563eb;
}

.is-mobile .notice-divider {
  background: #0f766e;
}

.contact {
  margin: 0;
  color: #64748b;
  line-height: 1.8;
}

.support-card {
  width: min(430px, 100%);
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 24px auto 0;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #f8fafc;
  text-align: left;
}

.support-card img {
  width: 82px;
  height: 82px;
  flex: 0 0 82px;
  object-fit: cover;
  border-radius: 14px;
  background: #fff;
}

.support-card strong,
.support-card span {
  display: block;
}

.support-card strong {
  color: #0f172a;
}

.support-card span {
  margin-top: 6px;
  color: #64748b;
  line-height: 1.6;
  word-break: break-all;
}

.notice-actions {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-top: 30px;
  flex-wrap: wrap;
}

.notice-actions button,
.notice-actions a {
  min-width: 132px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #334155;
  font-weight: 700;
  text-decoration: none;
  cursor: pointer;
}

.notice-actions button {
  border-color: transparent;
  background: #0f172a;
  color: #fff;
}

.notice-actions .support-btn {
  background: #fff;
  color: #0f172a;
  border-color: #cbd5e1;
}

@media (max-width: 640px) {
  .site-access-notice {
    padding: 18px;
  }

  .notice-shell {
    padding: 34px 22px;
    border-radius: 22px;
  }

  .message {
    font-size: 16px;
  }
}
</style>
