<script setup>
import { useAuthStore } from './stores/auth'
import SetupGuide from './components/SetupGuide.vue'

const auth = useAuthStore()
</script>

<template>
  <SetupGuide v-if="!auth.configured" />
  <router-view v-else-if="auth.initialized" />
  <div v-else class="splash">
    <div class="splash-icon">Memo</div>
    <p>加载中...</p>
  </div>
</template>

<style scoped>
.splash {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--c-text-soft);
}

.splash-icon {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: var(--c-primary);
  color: #fff;
  font-weight: 700;
  font-size: 15px;
}
</style>
