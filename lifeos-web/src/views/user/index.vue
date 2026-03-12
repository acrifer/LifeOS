<template>
  <div class="user-container">
    <div class="header">
      <h1>User Profile</h1>
      <p>Manage your personal information and preferences.</p>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading your profile...</p>
    </div>

    <div v-else-if="user" class="profile-card glass-panel">
      <div class="avatar-section">
        <div class="avatar">{{ user.username.charAt(0).toUpperCase() }}</div>
        <div class="user-titles">
          <h2>{{ user.username }}</h2>
          <span class="role">Pro Member</span>
        </div>
      </div>

      <div class="info-section">
        <div class="info-group">
          <label>Email</label>
          <div class="info-value">{{ user.email || 'Not provided' }}</div>
        </div>
        <div class="info-group">
          <label>User ID</label>
          <div class="info-value">#{{ user.id }}</div>
        </div>
        <div class="info-group">
          <label>Joined Date</label>
          <div class="info-value">{{ formatDate(user.createTime) }}</div>
        </div>
      </div>
      
      <div class="actions">
        <button class="btn btn-outline" @click="handleLogout">
          Sign Out
        </button>
      </div>
    </div>

    <div v-else class="error-msg">
      Could not load user information.
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'

const router = useRouter()

const user = ref(null)
const loading = ref(true)

const fetchUserInfo = async () => {
  try {
    user.value = await userApi.getInfo()
  } catch (error) {
    console.error('Failed to fetch user info', error)
  } finally {
    loading.value = false
  }
}

const handleLogout = () => {
  localStorage.removeItem('lifeos_token')
  router.push('/login')
}

const formatDate = (dateString) => {
  if (!dateString) return 'Just now'
  return new Date(dateString).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style scoped>
.user-container {
  padding: 40px;
  max-width: 800px;
  margin: 0 auto;
}

.header {
  margin-bottom: 30px;
}

.header h1 {
  font-size: 28px;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.header p {
  color: #64748b;
  margin: 0;
}

.glass-panel {
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
  border: 1px solid #f1f5f9;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 24px;
  padding-bottom: 30px;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 30px;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 700;
  box-shadow: 0 10px 15px -3px rgba(99, 102, 241, 0.3);
}

.user-titles h2 {
  margin: 0 0 5px 0;
  color: #1e293b;
  font-size: 24px;
}

.role {
  display: inline-block;
  padding: 4px 12px;
  background: #f1f5f9;
  color: #475569;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.info-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
}

.info-group label {
  display: block;
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 16px;
  color: #0f172a;
  font-weight: 500;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.btn {
  padding: 10px 24px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-outline {
  background: transparent;
  border: 2px solid #ef4444;
  color: #ef4444;
}

.btn-outline:hover {
  background: #fef2f2;
}

.loading-state {
  text-align: center;
  padding: 60px;
  color: #64748b;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #6366f1;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-msg {
  background: #fef2f2;
  color: #ef4444;
  padding: 16px;
  border-radius: 12px;
  text-align: center;
}
</style>
