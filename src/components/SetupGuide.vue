<script setup>
import { ref } from 'vue'

const showSql = ref(false)
const sqlContent = ref('')

async function toggleSql() {
  showSql.value = !showSql.value
  if (showSql.value && !sqlContent.value) {
    try {
      const res = await fetch('/schema.sql')
      sqlContent.value = await res.text()
    } catch {
      sqlContent.value = '无法加载 SQL 内容，请打开项目文件 supabase/schema.sql 手动复制。'
    }
  }
}
</script>

<template>
  <div class="setup">
    <div class="setup-card">
      <h1>欢迎使用个人备忘录</h1>
      <p class="sub">首次使用需要完成 3 步云端配置，大约 5 分钟。</p>

      <section>
        <h2><span class="num">1</span>创建免费 Supabase 项目</h2>
        <p>
          打开
          <a href="https://supabase.com" target="_blank" rel="noreferrer">supabase.com</a>
          注册并新建一个 Project，名字随意（如 personal-memo）。
        </p>
      </section>

      <section>
        <h2><span class="num">2</span>执行建表 SQL</h2>
        <p>在 Supabase Dashboard 左侧进入「SQL Editor」，粘贴下面的脚本并点击 Run：</p>
        <button class="sql-toggle" @click="toggleSql">
          {{ showSql ? '收起 SQL 脚本' : '展开 SQL 脚本' }}
        </button>
        <pre v-if="showSql"><code>{{ sqlContent }}</code></pre>
      </section>

      <section>
        <h2><span class="num">3</span>填入项目密钥</h2>
        <p>
          在 Supabase 的「Project Settings → API」中找到 Project URL 和 anon public key，
          复制项目根目录的 <code>.env.example</code> 为 <code>.env</code>，替换成你的值：
        </p>
        <pre><code>VITE_SUPABASE_URL=https://你的项目.supabase.co
VITE_SUPABASE_ANON_KEY=你的anon-key</code></pre>
        <p class="hint">保存 .env 后重启开发服务器（或重新部署），刷新本页即可进入登录界面。</p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.setup {
  min-height: 100%;
  padding: 24px 16px;
}

.setup-card {
  max-width: 640px;
  margin: 0 auto;
  background: var(--c-card);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 28px 22px;
}

h1 {
  font-size: 22px;
  margin-bottom: 6px;
}

.sub {
  color: var(--c-text-soft);
  font-size: 14px;
  margin-bottom: 20px;
}

section {
  margin-bottom: 20px;
}

h2 {
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.num {
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--c-primary);
  color: #fff;
  font-size: 13px;
  flex-shrink: 0;
}

p {
  font-size: 14px;
  line-height: 1.7;
  color: var(--c-text);
}

a {
  color: var(--c-primary-dark);
}

code {
  background: var(--c-bg);
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 13px;
}

pre {
  margin: 10px 0;
  padding: 12px;
  background: #1c1917;
  color: #f5f5f4;
  border-radius: 10px;
  overflow-x: auto;
  font-size: 12px;
  line-height: 1.6;
  max-height: 320px;
  overflow-y: auto;
}

.sql-toggle {
  margin-top: 8px;
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid var(--c-border);
  font-size: 13px;
  color: var(--c-primary-dark);
}

.hint {
  margin-top: 8px;
  color: var(--c-text-soft);
  font-size: 13px;
}
</style>
