<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'

const route = useRoute()
const isLogin = computed(() => route.path === '/login')
</script>

<template>
  <header v-if="!isLogin">
    <div class="logo-bar">🚀 LifeOS</div>
    <nav>
      <RouterLink to="/dashboard">Dashboard</RouterLink>
      <RouterLink to="/task">Task</RouterLink>
      <RouterLink to="/note">Note</RouterLink>
      <RouterLink to="/ai">AI</RouterLink>
      <RouterLink to="/user">User</RouterLink>
    </nav>
  </header>

  <main :class="{ 'full-screen': isLogin }">
    <RouterView />
  </main>
</template>

<style scoped>
header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2rem;
  height: 60px;
  background: white;
  border-bottom: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

.logo-bar {
  font-weight: bold;
  font-size: 1.25rem;
  color: #1e293b;
}

nav {
  display: flex;
  gap: 20px;
}

nav a.router-link-exact-active {
  color: #4f46e5;
  font-weight: 600;
  position: relative;
}

nav a.router-link-exact-active::after {
  content: '';
  position: absolute;
  bottom: -20px;
  left: 0;
  right: 0;
  height: 2px;
  background-color: #4f46e5;
}

nav a {
  color: #64748b;
  text-decoration: none;
  font-size: 0.95rem;
  transition: color 0.2s;
}

nav a:hover {
  color: #334155;
}

main {
  min-height: calc(100vh - 60px);
  background-color: #f8fafc;
}

main.full-screen {
  min-height: 100vh;
  margin: 0;
  padding: 0;
}
</style>
