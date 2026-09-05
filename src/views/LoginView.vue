<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const mode = ref('signin')
const email = ref('')
const password = ref('')
const error = ref('')
const submitting = ref(false)
const signUpSent = ref(false)

const errorMessages = {
  'Invalid login credentials': '邮箱或密码错误',
  'User already registered': '该邮箱已注册，请直接登录',
  'Email not confirmed': '邮箱尚未验证，请先查收确认邮件',
}

async function submit() {
  error.value = ''
  if (!email.value.trim() || !password.value) {
    error.value = '请输入邮箱和密码'
    return
  }
  if (password.value.length < 6) {
    error.value = '密码至少 6 位'
    return
  }
  submitting.value = true
  try {
    if (mode.value === 'signup') {
      const err = await auth.signUp(email.value.trim(), password.value)
      if (err) {
        error.value = errorMessages[err.message] ?? err.message
        return
      }
      signUpSent.value = true
    } else {
      const err = await auth.signIn(email.value.trim(), password.value)
      if (err) {
        error.value = errorMessages[err.message] ?? err.message
        return
      }
      router.replace({ name: 'list' })
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login">
    <div class="login-card">
      <div class="logo">Memo</div>
      <h1>个人备忘录</h1>
      <p class="sub">云端同步，随处可查</p>

      <template v-if="!signUpSent">
        <form @submit.prevent="submit">
          <input v-model="email" type="email" placeholder="邮箱" autocomplete="email" />
          <input
            v-model="password"
            type="password"
            :placeholder="mode === 'signup' ? '设置密码（至少 6 位）' : '密码'"
            autocomplete="current-password"
          />
          <p v-if="error" class="error-text">{{ error }}</p>
          <button class="btn-primary" type="submit" :disabled="submitting">
            {{ submitting ? '请稍候...' : mode === 'signup' ? '注册' : '登录' }}
          </button>
        </form>
        <button class="switch" @click="mode = mode === 'signin' ? 'signup' : 'signin'">
          {{ mode === 'signin' ? '没有账号？去注册' : '已有账号？去登录' }}
        </button>
      </template>

      <template v-else>
        <p class="sent">注册成功！验证邮件已发送到<br />{{ email }}</p>
        <p class="hint">点击邮件中的确认链接后返回本页登录。若无法收到邮件，可在 Supabase Dashboard → Authentication → Providers 关闭邮箱确认。</p>
        <button class="btn-primary" @click="signUpSent = false">返回登录</button>
      </template>
    </div>
  </div>
</template>

<style scoped>
.login {
  min-height: 100%;
  display: flex;
  align-items: center;
  padding: 24px 16px;
}

.login-card {
  width: 100%;
  max-width: 380px;
  margin: 0 auto;
  background: var(--c-card);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 32px 24px;
  text-align: center;
}

.logo {
  width: 64px;
  height: 64px;
  margin: 0 auto 14px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: var(--c-primary);
  color: #fff;
  font-weight: 700;
  font-size: 14px;
}

h1 {
  font-size: 20px;
}

.sub {
  color: var(--c-text-soft);
  font-size: 13px;
  margin: 4px 0 24px;
}

form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  text-align: left;
}

input {
  padding: 13px 14px;
  border-radius: 10px;
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  transition: border-color 0.15s;
}

input:focus {
  border-color: var(--c-primary);
}

.error-text {
  margin-top: -2px;
}

.switch {
  margin-top: 18px;
  font-size: 14px;
  color: var(--c-primary-dark);
}

.sent {
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 12px;
}

.hint {
  font-size: 13px;
  color: var(--c-text-soft);
  line-height: 1.6;
  margin-bottom: 18px;
  text-align: left;
}
</style>
