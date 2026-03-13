<template>
  <div class="page">
    <section class="hero card">
      <div>
        <p class="eyebrow">知识总览</p>
        <h1>{{ greeting }}，{{ user?.username || '你' }}</h1>
        <p>这里集中展示待复习笔记、待整理内容、知识任务和最近一份周复盘。</p>
      </div>
      <div class="clock">
        <strong>{{ currentTime }}</strong>
        <span>{{ currentDate }}</span>
      </div>
    </section>

    <section class="priority-grid">
      <button class="card priority" @click="openNoteView('review')">
        <span>待复习笔记</span>
        <strong>{{ dashboard.notesToReviewCount || 0 }}</strong>
        <small>看看哪些内容该回顾了</small>
      </button>
      <button class="card priority" @click="openNoteView('organize')">
        <span>AI 待整理</span>
        <strong>{{ dashboard.aiInboxCount || 0 }}</strong>
        <small>补摘要、补标签、补结构</small>
      </button>
      <button class="card priority" @click="openTaskView('derived')">
        <span>知识任务</span>
        <strong>{{ dashboard.pendingExtractedTaskCount || 0 }}</strong>
        <small>来自笔记的下一步行动</small>
      </button>
    </section>

    <section class="metric-grid">
      <article class="card metric"><span>笔记总数</span><strong>{{ dashboard.noteCount || 0 }}</strong></article>
      <article class="card metric"><span>本周新增</span><strong>{{ dashboard.weekNewNoteCount || 0 }}</strong></article>
      <article class="card metric"><span>本周整理</span><strong>{{ dashboard.weekOrganizedNoteCount || 0 }}</strong></article>
      <article class="card metric"><span>待完成任务</span><strong>{{ dashboard.pendingTaskCount || 0 }}</strong></article>
      <article class="card metric"><span>本周完成任务</span><strong>{{ dashboard.weekCompletedTaskCount || 0 }}</strong></article>
    </section>

    <section class="spotlight-grid">
      <article class="card review-spotlight">
        <div class="row">
          <div>
            <p class="section-label">本周复盘</p>
            <h2>{{ weeklyReview?.headline || '还没有本周复盘' }}</h2>
          </div>
          <div class="actions">
            <button class="ghost-btn" @click="router.push('/ai')">查看历史</button>
            <button class="primary-btn" :disabled="weeklyBusy" @click="createWeeklyReview">
              {{ weeklyBusy ? '处理中...' : '重新生成' }}
            </button>
          </div>
        </div>
        <template v-if="weeklyReview">
          <p class="summary">{{ weeklyReview.summary }}</p>
          <div class="meta-row">
            <span>状态：{{ jobStatusLabel(weeklyJob?.status) }}</span>
            <span>{{ weeklyJob?.finishedTime ? `更新于 ${formatDate(weeklyJob.finishedTime)}` : '等待生成结果' }}</span>
          </div>
          <div class="pill-group">
            <span v-for="item in weeklyReview.highlights" :key="item" class="pill">{{ item }}</span>
          </div>
          <div class="detail-grid">
            <div>
              <strong>接下来关注</strong>
              <ul>
                <li v-for="item in weeklyReview.focusAreas" :key="item">{{ item }}</li>
              </ul>
            </div>
            <div>
              <strong>下一步行动</strong>
              <ul>
                <li v-for="item in weeklyReview.nextActions" :key="item">{{ item }}</li>
              </ul>
            </div>
          </div>
        </template>
        <template v-else>
          <p class="summary">首页现在会固定展示最近一次异步周复盘。你可以直接在这里刷新结果，或者去 AI 工作台查看完整历史。</p>
          <div class="meta-row">
            <span>状态：{{ jobStatusLabel(weeklyJob?.status) }}</span>
            <span>{{ weeklyJob ? formatDate(weeklyJob.createTime) : '尚未生成' }}</span>
          </div>
        </template>
      </article>

      <article class="card review-queue">
        <div class="row">
          <div>
            <p class="section-label">复习队列</p>
            <h2>{{ reviewQueue.length ? '今天该看这些' : '当前没有待复习内容' }}</h2>
          </div>
          <button class="ghost-btn" @click="openNoteView('review')">进入复习队列</button>
        </div>
        <div class="queue-stats">
          <article>
            <strong>{{ reviewStats.overdue }}</strong>
            <span>已到期</span>
          </article>
          <article>
            <strong>{{ reviewStats.today }}</strong>
            <span>今天安排</span>
          </article>
          <article>
            <strong>{{ reviewStats.unscheduled }}</strong>
            <span>未排时间</span>
          </article>
        </div>
        <div v-if="reviewQueue.length" class="queue-list">
          <button
            v-for="note in reviewQueue.slice(0, 4)"
            :key="note.id"
            class="queue-item"
            @click="openNoteDetail(note.id)"
          >
            <div>
              <strong>{{ note.title || '未命名笔记' }}</strong>
              <p>{{ note.tags || '还没有标签' }}</p>
            </div>
            <span>{{ reviewDueLabel(note) }}</span>
          </button>
        </div>
        <p v-else class="muted">最近可以把常青内容和需要复盘的笔记继续沉淀起来。</p>
      </article>
    </section>

    <section class="content-grid">
      <article class="card panel">
        <div class="row">
          <h2>最近更新</h2>
          <button class="ghost-btn" @click="router.push('/note')">进入笔记库</button>
        </div>
        <div v-if="dashboard.recentNotes?.length" class="list">
          <button
            v-for="note in dashboard.recentNotes"
            :key="note.id"
            class="list-row list-button"
            @click="openNoteDetail(note.id)"
          >
            <div>
              <strong>{{ note.title }}</strong>
              <p>{{ note.tags || '还没有标签' }}</p>
            </div>
            <small>{{ note.updatedAt }}</small>
          </button>
        </div>
        <p v-else>还没有最近笔记。</p>
      </article>

      <article class="card panel">
        <div class="row">
          <h2>高频主题</h2>
          <button class="ghost-btn" @click="router.push('/ai')">打开 AI 工作台</button>
        </div>
        <div v-if="dashboard.topTags?.length" class="tags">
          <div v-for="tag in dashboard.topTags" :key="tag.tag" class="tag-pill">
            <strong>#{{ tag.tag }}</strong>
            <span>{{ tag.count }}</span>
          </div>
        </div>
        <p v-else>先给几篇笔记补上标签，这里会出现你的主题分布。</p>
      </article>
    </section>

    <section class="content-grid">
      <article class="card panel">
        <div class="row">
          <h2>最近 AI 动作</h2>
          <button class="ghost-btn" @click="router.push('/ai')">查看全部历史</button>
        </div>
        <div v-if="recentAiJobs.length" class="list">
          <button
            v-for="job in recentAiJobs"
            :key="job.id"
            class="list-row list-button"
            @click="openAiHistory(job)"
          >
            <div>
              <strong>{{ job.noteTitle || jobTypeLabel(job.jobType) }}</strong>
              <p>{{ jobTypeLabel(job.jobType) }} · {{ formatDate(job.createTime) }}</p>
            </div>
            <span class="badge" :class="statusClass(job.status)">{{ jobStatusLabel(job.status) }}</span>
          </button>
        </div>
        <p v-else>还没有 AI 历史记录。</p>
      </article>

      <article class="card panel">
        <div class="row">
          <h2>近七天活跃度</h2>
          <button class="ghost-btn" @click="router.push('/ai')">查看 AI 作业</button>
        </div>
        <div v-if="dashboard.recentTrend?.length" class="trend">
          <div v-for="item in dashboard.recentTrend" :key="item.date" class="trend-item">
            <div class="bar-wrap">
              <div class="bar" :style="{ height: `${barHeight(item.count)}%` }"></div>
            </div>
            <strong>{{ item.count }}</strong>
            <span>{{ item.date }}</span>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { behaviorApi } from '@/api/behavior'
import { noteApi } from '@/api/note'
import { userApi } from '@/api/user'

const router = useRouter()
const user = ref(null)
const dashboard = ref({})
const reviewQueue = ref([])
const recentAiJobs = ref([])
const weeklyReview = ref(null)
const weeklyJob = ref(null)
const weeklyBusy = ref(false)
const currentTime = ref('')
const currentDate = ref('')
let timer = null
let weeklyTimer = null

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const reviewStats = computed(() => {
  const endOfToday = new Date()
  endOfToday.setHours(23, 59, 59, 999)

  return reviewQueue.value.reduce((acc, note) => {
    if (!note.nextReviewAt) {
      acc.unscheduled += 1
      return acc
    }
    const due = new Date(note.nextReviewAt)
    if (due.getTime() < Date.now()) {
      acc.overdue += 1
    } else if (due.getTime() <= endOfToday.getTime()) {
      acc.today += 1
    }
    return acc
  }, { overdue: 0, today: 0, unscheduled: 0 })
})

const fetchUserInfo = async () => {
  user.value = await userApi.getInfo()
}

const fetchDashboard = async () => {
  dashboard.value = await behaviorApi.getDashboard()
}

const fetchReviewQueue = async () => {
  const notes = await noteApi.search({ reviewState: 'REVIEWABLE', sort: 'review' })
  reviewQueue.value = notes
    .slice()
    .sort((left, right) => {
      const leftTime = left.nextReviewAt ? new Date(left.nextReviewAt).getTime() : Number.MAX_SAFE_INTEGER
      const rightTime = right.nextReviewAt ? new Date(right.nextReviewAt).getTime() : Number.MAX_SAFE_INTEGER
      return leftTime - rightTime
    })
}

const fetchAiHistory = async () => {
  recentAiJobs.value = await noteApi.getJobs({ limit: 6 })
}

const fetchWeeklyReview = async () => {
  const jobs = await noteApi.getJobs({ jobType: 'WEEKLY_REVIEW', limit: 1 })
  weeklyJob.value = jobs[0] || null
  weeklyReview.value = weeklyJob.value?.result?.weeklyReview || null
  if (weeklyJob.value && ['PENDING', 'PROCESSING'].includes(weeklyJob.value.status)) {
    weeklyBusy.value = true
    pollWeeklyJob(weeklyJob.value.id)
  }
}

const createWeeklyReview = async () => {
  weeklyJob.value = await noteApi.createWeeklyReview()
  weeklyBusy.value = true
  pollWeeklyJob(weeklyJob.value.id)
}

const pollWeeklyJob = (jobId) => {
  stopWeeklyPoll()
  const runner = async () => {
    weeklyJob.value = await noteApi.getJob(jobId)
    weeklyReview.value = weeklyJob.value?.result?.weeklyReview || null
    if (weeklyJob.value.status === 'SUCCESS' || weeklyJob.value.status === 'FAILED') {
      weeklyBusy.value = false
      await Promise.all([fetchAiHistory(), fetchDashboard()])
      stopWeeklyPoll()
      return
    }
    weeklyTimer = window.setTimeout(runner, 2000)
  }
  runner()
}

const stopWeeklyPoll = () => {
  if (weeklyTimer) {
    window.clearTimeout(weeklyTimer)
    weeklyTimer = null
  }
}

const updateClock = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  currentDate.value = now.toLocaleDateString('zh-CN', { weekday: 'long', month: 'long', day: 'numeric' })
}

const openNoteView = (view) => {
  router.push({ path: '/note', query: { view } })
}

const openTaskView = (view) => {
  router.push({ path: '/task', query: { view } })
}

const openNoteDetail = (noteId) => {
  router.push({ path: '/note', query: { noteId } })
}

const openAiHistory = (job) => {
  const query = { history: job.id }
  if (job.noteId) query.noteId = job.noteId
  router.push({ path: '/ai', query })
}

const barHeight = (count) => {
  const values = (dashboard.value.recentTrend || []).map(item => item.count)
  const max = Math.max(...values, 1)
  return Math.max((count / max) * 100, count > 0 ? 12 : 6)
}

const reviewDueLabel = (note) => {
  if (!note.nextReviewAt) return '未排时间'
  const due = new Date(note.nextReviewAt)
  const diff = due.getTime() - Date.now()
  if (diff < 0) return '已到期'
  if (diff <= 24 * 60 * 60 * 1000) return '今天'
  return formatDate(note.nextReviewAt)
}

const formatDate = (value) =>
  value
    ? new Date(value).toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
    : '未设置时间'

const jobTypeLabel = (value) => ({ SUMMARY: '摘要', ORGANIZE: '整理', EXTRACT_TASKS: '提取任务', WEEKLY_REVIEW: '周复盘' }[value] || value)
const jobStatusLabel = (value) => ({ PENDING: '排队中', PROCESSING: '处理中', SUCCESS: '已完成', FAILED: '失败' }[value] || '未知状态')
const statusClass = (value) => ({
  success: value === 'SUCCESS',
  failed: value === 'FAILED'
})

onMounted(async () => {
  await Promise.all([fetchUserInfo(), fetchDashboard(), fetchReviewQueue(), fetchAiHistory(), fetchWeeklyReview()])
  updateClock()
  timer = setInterval(updateClock, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  stopWeeklyPoll()
})
</script>

<style scoped>
.page { max-width: 1280px; margin: 0 auto; padding: 32px; }
.card { background: #fff; border: 1px solid #d8e3ec; border-radius: 24px; box-shadow: 0 18px 32px rgba(15, 23, 42, 0.06); }
.hero { display: flex; justify-content: space-between; gap: 24px; padding: 32px; margin-bottom: 18px; background: linear-gradient(135deg, #0f172a, #164e63); color: #fff; }
.eyebrow, .section-label { margin: 0 0 10px; font-size: 12px; letter-spacing: 0.18em; text-transform: uppercase; color: rgba(255,255,255,0.72); }
.section-label { color: #5e7388; }
.hero h1 { margin: 0 0 12px; font-size: 40px; line-height: 1.05; }
.hero p { margin: 0; line-height: 1.8; }
.clock { min-width: 220px; background: rgba(255,255,255,0.12); border: 1px solid rgba(255,255,255,0.18); border-radius: 20px; padding: 20px 24px; display: grid; align-content: center; }
.clock strong { font-size: 34px; margin-bottom: 8px; }
.priority-grid, .metric-grid, .content-grid, .spotlight-grid { display: grid; gap: 16px; margin-bottom: 18px; }
.priority-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.metric-grid { grid-template-columns: repeat(5, minmax(0, 1fr)); }
.priority, .metric { padding: 22px; border: none; text-align: left; cursor: pointer; }
.priority span, .metric span, .panel p, .list-row p, .trend-item span, .summary, .muted, .meta-row { color: #607487; }
.priority strong, .metric strong { font-size: 34px; color: #0f172a; }
.spotlight-grid, .content-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.review-spotlight, .review-queue, .panel { padding: 20px; }
.row { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
.summary { line-height: 1.8; margin-bottom: 14px; }
.meta-row { display: flex; gap: 14px; flex-wrap: wrap; margin-bottom: 12px; font-size: 13px; }
.pill-group { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 14px; }
.pill { padding: 8px 12px; border-radius: 999px; background: #edf5f7; color: #124e61; font-size: 13px; }
.detail-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.detail-grid strong { color: #102033; }
.detail-grid ul { margin: 10px 0 0; padding-left: 18px; color: #607487; line-height: 1.7; }
.queue-stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.queue-stats article { padding: 14px; border-radius: 18px; background: #f8fbfd; border: 1px solid #e1eaf1; display: grid; gap: 6px; }
.queue-stats strong { font-size: 28px; color: #102033; }
.queue-list, .list { display: grid; gap: 12px; }
.queue-item, .list-button { border: none; width: 100%; text-align: left; cursor: pointer; }
.queue-item, .list-row { display: flex; justify-content: space-between; gap: 12px; padding: 14px; border-radius: 18px; border: 1px solid #e1eaf1; background: #f8fbfd; }
.ghost-btn, .primary-btn, .badge { border: none; border-radius: 14px; padding: 10px 12px; font: inherit; }
.ghost-btn { background: #f5f9fc; color: #375068; cursor: pointer; }
.primary-btn { background: #103d5d; color: #fff; cursor: pointer; }
.primary-btn:disabled { opacity: 0.65; cursor: not-allowed; }
.badge { background: #eaf0f6; color: #31516c; align-self: center; }
.badge.success { background: #dcfce7; color: #166534; }
.badge.failed { background: #fee2e2; color: #b91c1c; }
.tags { display: flex; flex-wrap: wrap; gap: 12px; }
.tag-pill { display: flex; gap: 10px; padding: 12px 14px; border-radius: 16px; background: #f8fbfd; border: 1px solid #e1eaf1; color: #102033; }
.trend { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 12px; align-items: end; min-height: 220px; }
.trend-item { display: grid; justify-items: center; gap: 8px; }
.bar-wrap { width: 100%; height: 140px; border-radius: 18px; background: #edf4f8; display: flex; align-items: end; justify-content: center; padding: 8px; }
.bar { width: 100%; border-radius: 12px; background: linear-gradient(180deg, #1d4ed8, #0f766e); }
@media (max-width: 1100px) {
  .priority-grid, .metric-grid, .spotlight-grid, .content-grid, .detail-grid { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .page { padding: 20px; }
  .hero { flex-direction: column; }
  .queue-stats { grid-template-columns: 1fr; }
}
</style>
