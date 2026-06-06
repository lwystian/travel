<template>
  <div class="backend-layout">
    <Sidebar />

    <section class="backend-main">
      <Navbar />

      <main class="content-container">
        <router-view v-slot="{ Component }">
          <transition name="backend-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </section>
  </div>
</template>

<script setup>
import Sidebar from '@/components/backend/Sidebar.vue'
import Navbar from '@/components/backend/Navbar.vue'
</script>

<style lang="scss" scoped>
.backend-layout {
  display: flex;
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
  color: #172033;
  background: #ffffff;
}

.backend-main {
  display: flex;
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: hidden;
  flex-direction: column;
}

.content-container {
  position: relative;
  flex: 1;
  min-width: 0;
  padding: 24px;
  background: #ffffff;
  overflow-y: auto;
  overflow-x: hidden;
  scroll-behavior: smooth;

  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-thumb {
    border: 2px solid transparent;
    border-radius: 999px;
    background: rgba(100, 116, 139, 0.34);
    background-clip: content-box;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }
}

.backend-fade-enter-active,
.backend-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.backend-fade-enter-from,
.backend-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
