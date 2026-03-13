<template>
  <div class="account-page">
    <section class="hero-card">
      <div class="hero-copy">
        <p class="eyebrow">账户中心</p>
        <h1>统一管理你的账号资料</h1>
        <p>在这里查看账户信息、更新基础资料、修改密码，并快速确认当前账号关联的数据状态。</p>
      </div>

      <div class="hero-stats">
        <article>
          <strong>{{ dashboard.noteCount || 0 }}</strong>
          <span>笔记总数</span>
        </article>
        <article>
          <strong>{{ dashboard.pendingTaskCount || 0 }}</strong>
          <span>待办任务</span>
        </article>
        <article>
          <strong>{{ dashboard.weekCompletedTaskCount || 0 }}</strong>
          <span>本周完成</span>
        </article>
      </div>
    </section>

    <section v-if="loading" class="state-card">
      <div class="spinner"></div>
      <p>正在加载账户信息...</p>
    </section>

    <section v-else-if="user" class="content-grid">
      <article class="profile-card">
        <div class="profile-head">
          <div class="avatar">{{ user.username?.charAt(0)?.toUpperCase() || 'U' }}</div>
          <div>
            <h2>{{ user.username }}</h2>
            <p>{{ user.email || '未填写邮箱' }}</p>
          </div>
        </div>

        <div class="meta-grid">
          <div class="meta-item">
            <span>用户 ID</span>
            <strong>#{{ user.id }}</strong>
          </div>
          <div class="meta-item">
            <span>注册时间</span>
            <strong>{{ formatDate(user.createTime) }}</strong>
          </div>
          <div class="meta-item">
            <span>当前状态</span>
            <strong>已登录</strong>
          </div>
        </div>

        <div class="helper-card">
          <h3>管理建议</h3>
          <ul>
            <li>修改用户名后，当前登录态会自动刷新。</li>
            <li>建议补全邮箱，便于后续扩展通知或找回能力。</li>
            <li>修改密码后，当前会话会继续保持登录，但会刷新为新令牌。</li>
          </ul>
        </div>

        <button class="danger-btn" :disabled="loggingOut" @click="handleLogout">
          {{ loggingOut ? '退出中...' : '退出登录' }}
        </button>
      </article>

      <div class="manage-column">
        <article class="panel-card">
          <div class="panel-header">
            <div>
              <h3>基础资料</h3>
              <p>更新用户名和邮箱。</p>
            </div>
          </div>

          <form class="form-grid" @submit.prevent="submitProfile">
            <label>
              <span>用户名</span>
              <input v-model="profileForm.username" type="text" placeholder="请输入用户名" />
            </label>
            <label>
              <span>邮箱</span>
              <input v-model="profileForm.email" type="email" placeholder="请输入邮箱" />
            </label>

            <div v-if="profileMessage" class="message" :class="{ success: profileSuccess }">
              {{ profileMessage }}
            </div>

            <div class="form-actions">
              <button type="button" class="ghost-btn" @click="resetProfileForm">重置</button>
              <button type="submit" class="primary-btn" :disabled="savingProfile">
                {{ savingProfile ? '保存中...' : '保存资料' }}
              </button>
            </div>
          </form>
        </article>

        <article class="panel-card">
          <div class="panel-header">
            <div>
              <h3>密码修改</h3>
              <p>修改当前账号密码。</p>
            </div>
          </div>

          <form class="form-grid" @submit.prevent="submitPassword">
            <label>
              <span>当前密码</span>
              <input v-model="passwordForm.currentPassword" type="password" placeholder="请输入当前密码" />
            </label>
            <label>
              <span>新密码</span>
              <input v-model="passwordForm.newPassword" type="password" placeholder="至少 6 位" />
            </label>
            <label>
              <span>确认新密码</span>
              <input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
            </label>

            <div v-if="passwordMessage" class="message" :class="{ success: passwordSuccess }">
              {{ passwordMessage }}
            </div>

            <div class="form-actions">
              <button type="button" class="ghost-btn" @click="resetPasswordForm">清空</button>
              <button type="submit" class="primary-btn" :disabled="savingPassword">
                {{ savingPassword ? '提交中...' : '更新密码' }}
              </button>
            </div>
          </form>
        </article>
      </div>
    </section>

    <section v-else class="state-card error-card">
      <p>账户信息加载失败，请刷新后重试。</p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { behaviorApi } from '@/api/behavior'
import { userApi } from '@/api/user'

const router = useRouter()

const user = ref(null)
const dashboard = ref({})
const loading = ref(true)
const savingProfile = ref(false)
const savingPassword = ref(false)
const loggingOut = ref(false)
const profileMessage = ref('')
const profileSuccess = ref(false)
const passwordMessage = ref('')
const passwordSuccess = ref(false)

const profileForm = reactive({
  username: '',
  email: ''
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const syncProfileForm = () => {
  profileForm.username = user.value?.username || ''
  profileForm.email = user.value?.email || ''
}

const resetPasswordForm = () => {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

const fetchPageData = async () => {
  loading.value = true
  try {
    const [userInfo, dashboardStats] = await Promise.all([
      userApi.getInfo(),
      behaviorApi.getDashboard().catch(() => ({}))
    ])
    user.value = userInfo
    dashboard.value = dashboardStats || {}
    syncProfileForm()
  } catch (error) {
    console.error('Failed to fetch account page data', error)
  } finally {
    loading.value = false
  }
}

const resetProfileForm = () => {
  profileMessage.value = ''
  profileSuccess.value = false
  syncProfileForm()
}

const submitProfile = async () => {
  savingProfile.value = true
  profileMessage.value = ''
  profileSuccess.value = false

  try {
    const token = await userApi.updateProfile({
      username: profileForm.username,
      email: profileForm.email
    })
    localStorage.setItem('lifeos_token', token)
    await fetchPageData()
    profileMessage.value = '资料已更新。'
    profileSuccess.value = true
  } catch (error) {
    profileMessage.value = error.message || '资料更新失败。'
  } finally {
    savingProfile.value = false
  }
}

const submitPassword = async () => {
  passwordMessage.value = ''
  passwordSuccess.value = false

  if (!passwordForm.currentPassword || !passwordForm.newPassword) {
    passwordMessage.value = '请填写完整的密码信息。'
    return
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordMessage.value = '两次输入的新密码不一致。'
    return
  }

  savingPassword.value = true
  try {
    const token = await userApi.updatePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword
    })
    localStorage.setItem('lifeos_token', token)
    resetPasswordForm()
    passwordMessage.value = '密码已更新。'
    passwordSuccess.value = true
  } catch (error) {
    passwordMessage.value = error.message || '密码更新失败。'
  } finally {
    savingPassword.value = false
  }
}

const handleLogout = async () => {
  loggingOut.value = true
  try {
    await userApi.logout()
  } catch (error) {
    console.error('Failed to logout', error)
  } finally {
    localStorage.removeItem('lifeos_token')
    loggingOut.value = false
    router.push('/login')
  }
}

const formatDate = (dateString) => {
  if (!dateString) {
    return '刚刚'
  }
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

onMounted(() => {
  fetchPageData()
})
</script>

<style scoped>
.account-page {
  max-width: 1240px;
  margin: 0 auto;
  padding: 32px;
}

.hero-card,
.profile-card,
.panel-card,
.state-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #d7e2eb;
  border-radius: 28px;
  box-shadow: 0 20px 40px rgba(13, 30, 47, 0.06);
}

.hero-card {
  padding: 30px;
  margin-bottom: 20px;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 20px;
  align-items: center;
}

.eyebrow {
  margin: 0 0 10px;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-size: 12px;
  color: #0b5f6f;
  font-weight: 700;
}

.hero-copy h1 {
  margin: 0 0 10px;
  color: #102033;
  font-size: 36px;
}

.hero-copy p:last-child {
  margin: 0;
  color: #61778d;
  line-height: 1.8;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.hero-stats article {
  padding: 18px 16px;
  border-radius: 20px;
  background: #f7fafc;
  border: 1px solid #e0e8ef;
}

.hero-stats strong {
  display: block;
  margin-bottom: 8px;
  font-size: 28px;
  color: #102033;
}

.hero-stats span {
  color: #61778d;
  font-size: 14px;
}

.content-grid {
  display: grid;
  grid-template-columns: 0.92fr 1.08fr;
  gap: 18px;
}

.profile-card {
  padding: 26px;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 18px;
}

.avatar {
  width: 84px;
  height: 84px;
  border-radius: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  font-weight: 800;
  color: white;
  background: linear-gradient(135deg, #0f766e, #0f4c81);
  box-shadow: 0 16px 28px rgba(15, 76, 129, 0.18);
}

.profile-head h2,
.panel-header h3,
.helper-card h3 {
  margin: 0 0 6px;
  color: #102033;
}

.profile-head p,
.panel-header p,
.helper-card li {
  margin: 0;
  color: #61778d;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.meta-item {
  padding: 16px;
  border-radius: 18px;
  background: #f8fbfd;
  border: 1px solid #e0e8ef;
}

.meta-item span {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  color: #708497;
}

.meta-item strong {
  color: #102033;
  font-size: 16px;
}

.helper-card {
  padding: 18px 20px;
  border-radius: 22px;
  background: #f7fafc;
  border: 1px solid #e0e8ef;
}

.helper-card ul {
  margin: 0;
  padding-left: 18px;
}

.helper-card li + li {
  margin-top: 10px;
}

.manage-column {
  display: grid;
  gap: 18px;
}

.panel-card {
  padding: 24px;
}

.panel-header {
  margin-bottom: 18px;
}

.form-grid {
  display: grid;
  gap: 16px;
}

.form-grid label {
  display: grid;
  gap: 8px;
  color: #475b71;
  font-size: 14px;
  font-weight: 600;
}

.form-grid input {
  width: 100%;
  border: 1px solid #cfdae4;
  background: #f8fbfd;
  color: #102033;
  border-radius: 16px;
  padding: 13px 14px;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-grid input:focus {
  border-color: #0f766e;
  box-shadow: 0 0 0 4px rgba(15, 118, 110, 0.1);
}

.message {
  padding: 12px 14px;
  border-radius: 14px;
  color: #b42318;
  background: rgba(180, 35, 24, 0.08);
}

.message.success {
  color: #0f6e53;
  background: rgba(15, 118, 110, 0.08);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.primary-btn,
.ghost-btn,
.danger-btn {
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.primary-btn {
  padding: 12px 18px;
  border-radius: 16px;
  background: linear-gradient(135deg, #0f766e, #0f4c81);
  color: white;
  font-weight: 700;
}

.ghost-btn {
  padding: 12px 18px;
  border-radius: 16px;
  background: #f5f9fc;
  color: #36506a;
  font-weight: 700;
}

.danger-btn {
  margin-top: auto;
  padding: 14px 18px;
  border-radius: 16px;
  background: #fff5f4;
  color: #b42318;
  font-weight: 700;
}

.state-card {
  min-height: 260px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 14px;
  color: #61778d;
}

.error-card {
  color: #b42318;
}

.spinner {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 3px solid rgba(15, 118, 110, 0.18);
  border-top-color: #0f766e;
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 980px) {
  .account-page {
    padding: 20px;
  }

  .hero-card,
  .content-grid,
  .meta-grid,
  .hero-stats {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column;
  }
}
</style>
