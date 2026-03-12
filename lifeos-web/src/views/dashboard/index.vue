<template>
  <div class="dashboard-container">
    <div class="welcome-banner">
      <div class="welcome-content">
        <h1>Welcome back, <span class="highlight">{{ user?.username || 'Explorer' }}</span></h1>
        <p>Here is your live LifeOS summary for the last 7 days.</p>
      </div>
      <div class="datetime-widget">
        <div class="time">{{ currentTime }}</div>
        <div class="date">{{ currentDate }}</div>
      </div>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon task">T</div>
        <div class="stat-info">
          <h3>{{ dashboard.pendingTaskCount }}</h3>
          <p>Tasks pending</p>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon note">N</div>
        <div class="stat-info">
          <h3>{{ dashboard.noteCount }}</h3>
          <p>Notes created</p>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon ai">W</div>
        <div class="stat-info">
          <h3>{{ dashboard.weekCompletedTaskCount }}</h3>
          <p>Tasks finished this week</p>
        </div>
      </div>
    </div>

    <div class="trend-panel">
      <div class="panel-header">
        <h2>Recent activity</h2>
        <span>Last 7 days</span>
      </div>
      <div class="trend-grid">
        <div
          v-for="item in dashboard.recentTrend"
          :key="item.date"
          class="trend-item"
        >
          <div class="trend-bar-wrap">
            <div class="trend-bar" :style="{ height: `${getBarHeight(item.count)}%` }"></div>
          </div>
          <strong>{{ item.count }}</strong>
          <span>{{ item.date }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { userApi } from '@/api/user'
import { behaviorApi } from '@/api/behavior'

const user = ref(null)
const currentTime = ref('')
const currentDate = ref('')
const dashboard = ref({
  pendingTaskCount: 0,
  noteCount: 0,
  weekCompletedTaskCount: 0,
  recentTrend: []
})
let timer = null

const fetchUserInfo = async () => {
  try {
    user.value = await userApi.getInfo()
  } catch (error) {
    console.error('Failed to fetch user in dashboard', error)
  }
}

const fetchDashboard = async () => {
  try {
    dashboard.value = await behaviorApi.getDashboard()
  } catch (error) {
    console.error('Failed to fetch dashboard stats', error)
  }
}

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  currentDate.value = now.toLocaleDateString([], { weekday: 'long', month: 'long', day: 'numeric' })
}

const getBarHeight = (count) => {
  const values = dashboard.value.recentTrend.map(item => item.count)
  const max = Math.max(...values, 1)
  return Math.max((count / max) * 100, count > 0 ? 12 : 6)
}

onMounted(() => {
  fetchUserInfo()
  fetchDashboard()
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.dashboard-container {
  padding: 40px;
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-banner {
  background: linear-gradient(135deg, #1e1b4b 0%, #4338ca 100%);
  border-radius: 20px;
  padding: 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: white;
  margin-bottom: 40px;
  box-shadow: 0 20px 25px -5px rgba(67, 56, 202, 0.4);
}

.welcome-content h1 {
  margin: 0 0 10px 0;
  font-size: 32px;
  font-weight: 700;
}

.highlight {
  color: #a5b4fc;
}

.welcome-content p {
  margin: 0;
  color: #c7d2fe;
  font-size: 16px;
}

.datetime-widget {
  text-align: right;
  background: rgba(255, 255, 255, 0.1);
  padding: 15px 25px;
  border-radius: 16px;
  backdrop-filter: blur(10px);
}

.time {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 5px;
}

.date {
  font-size: 14px;
  color: #c7d2fe;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
  margin-bottom: 28px;
}

.stat-card {
  background: white;
  border-radius: 20px;
  padding: 30px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  border: 1px solid #f1f5f9;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
}

.stat-icon.task { background: #fee2e2; color: #ef4444; }
.stat-icon.note { background: #fef3c7; color: #f59e0b; }
.stat-icon.ai { background: #e0e7ff; color: #4f46e5; }

.stat-info h3 {
  margin: 0 0 5px 0;
  font-size: 28px;
  color: #0f172a;
}

.stat-info p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
}

.trend-panel {
  background: white;
  border-radius: 20px;
  padding: 28px 30px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

.panel-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 24px;
}

.panel-header h2 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.panel-header span {
  color: #64748b;
  font-size: 14px;
}

.trend-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 14px;
}

.trend-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.trend-bar-wrap {
  width: 100%;
  height: 140px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: linear-gradient(180deg, #eef2ff 0%, #f8fafc 100%);
  border-radius: 16px;
  padding: 10px;
}

.trend-bar {
  width: 100%;
  border-radius: 12px;
  background: linear-gradient(180deg, #4f46e5 0%, #0ea5e9 100%);
  min-height: 6px;
}

.trend-item strong {
  color: #0f172a;
  font-size: 18px;
}

.trend-item span {
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 768px) {
  .dashboard-container {
    padding: 20px;
  }

  .welcome-banner {
    padding: 28px;
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }

  .datetime-widget {
    width: 100%;
    text-align: left;
  }

  .trend-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
