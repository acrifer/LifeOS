<template>
  <div class="page">
    <section class="hero card">
      <div>
        <p class="eyebrow">知识笔记库</p>
        <h1>记录、整理、提取行动项，都在同一页完成。</h1>
        <p>AI 操作现在改成异步任务流，提交后会自动轮询状态，不再阻塞当前编辑。</p>
      </div>
      <button class="primary-btn" @click="openCreateModal">新建笔记</button>
    </section>

    <section class="toolbar card">
      <div class="views">
        <button
          v-for="view in viewOptions"
          :key="view.id"
          class="chip"
          :class="{ active: selectedView === view.id }"
          @click="selectView(view.id)"
        >
          {{ view.label }}
        </button>
      </div>
      <div class="filters">
        <input v-model="searchKeyword" type="text" placeholder="搜索标题、正文、标签或摘要" @keyup.enter="fetchNotes" />
        <input v-model="tagFilter" type="text" placeholder="标签，例如：学习, Redis" @keyup.enter="fetchNotes" />
        <select v-model="summaryFilter">
          <option value="all">全部摘要状态</option>
          <option value="with">已有摘要</option>
          <option value="without">缺少摘要</option>
        </select>
        <select v-model="sortOption">
          <option value="updated">最近更新</option>
          <option value="created">最近创建</option>
          <option value="review">复习优先</option>
        </select>
        <button class="ghost-btn" @click="resetFilters">重置</button>
        <button class="secondary-btn" @click="fetchNotes">筛选</button>
      </div>
    </section>

    <section v-if="selectedView === 'review' && reviewQueue.length" class="card review-cockpit">
      <div class="row">
        <div>
          <p class="section-label">复习队列</p>
          <h2>先处理该回看的内容</h2>
        </div>
        <button class="ghost-btn" @click="openEditModal(currentReviewTarget?.id)" :disabled="!currentReviewTarget">
          打开当前目标
        </button>
      </div>

      <div class="review-stats">
        <article>
          <strong>{{ reviewInsights.total }}</strong>
          <span>待复习总数</span>
        </article>
        <article>
          <strong>{{ reviewInsights.overdue }}</strong>
          <span>已到期</span>
        </article>
        <article>
          <strong>{{ reviewInsights.today }}</strong>
          <span>今天安排</span>
        </article>
        <article>
          <strong>{{ reviewInsights.unscheduled }}</strong>
          <span>未排时间</span>
        </article>
      </div>

      <div class="review-grid">
        <article v-if="currentReviewTarget" class="review-focus">
          <p class="section-label">当前目标</p>
          <h3>{{ currentReviewTarget.title || '未命名笔记' }}</h3>
          <p class="focus-copy">{{ currentReviewTarget.summary || previewText(currentReviewTarget.content) }}</p>
          <div class="meta">
            <span>{{ reviewDueLabel(currentReviewTarget) }}</span>
            <span>{{ currentReviewTarget.lastReviewedAt ? `上次复习 ${formatDate(currentReviewTarget.lastReviewedAt)}` : '还没有复习记录' }}</span>
          </div>
          <div class="tags">
            <span v-for="tag in splitTags(currentReviewTarget.tags)" :key="tag" class="tag">#{{ tag }}</span>
          </div>
          <div class="actions">
            <button class="primary-btn" @click="completeReview(currentReviewTarget, 3)">今天看完，3 天后再看</button>
            <button class="secondary-btn" @click="completeReview(currentReviewTarget, 7)">记为重点，7 天后复习</button>
            <button class="ghost-btn" @click="markEvergreen(currentReviewTarget)">标记常青</button>
          </div>
        </article>

        <aside class="review-list">
          <div v-for="note in reviewQueue.slice(0, 5)" :key="note.id" class="review-item">
            <button class="review-link" @click="openEditModal(note.id)">
              <div>
                <strong>{{ note.title || '未命名笔记' }}</strong>
                <p>{{ note.tags || '还没有标签' }}</p>
              </div>
              <span>{{ reviewDueLabel(note) }}</span>
            </button>
            <div class="quick-actions">
              <button class="ghost-btn small" @click="completeReview(note, 2)">+2 天</button>
              <button class="ghost-btn small" @click="completeReview(note, 7)">+7 天</button>
            </div>
          </div>
        </aside>
      </div>
    </section>

    <section v-if="loading" class="card empty">正在加载笔记...</section>
    <section v-else-if="notes.length === 0" class="card empty">没有符合条件的笔记。</section>
    <section v-else class="grid">
      <article v-for="note in notes" :key="note.id" class="card note-card" @click="openEditModal(note.id)">
        <div class="row">
          <div class="badges">
            <span v-if="note.pinned" class="badge success">置顶</span>
            <span class="badge">{{ reviewLabel(note.reviewState) }}</span>
          </div>
          <button class="ghost-btn small" @click.stop="toggleCardPin(note)">
            {{ note.pinned ? '取消置顶' : '置顶' }}
          </button>
        </div>
        <h3>{{ note.title || '未命名笔记' }}</h3>
        <p class="preview">{{ previewText(note.content) }}</p>
        <div class="summary">{{ note.summary || '还没有 AI 摘要。' }}</div>
        <div class="meta">
          <span>{{ formatDate(note.updateTime) }}</span>
          <span>{{ reviewDueLabel(note) }}</span>
        </div>
        <div class="tags">
          <span v-for="tag in splitTags(note.tags).slice(0, 4)" :key="tag" class="tag">#{{ tag }}</span>
        </div>
        <button class="danger-btn" @click.stop="deleteNote(note.id)">删除</button>
      </article>
    </section>

    <div v-if="showModal" class="overlay" @click.self="closeModal">
      <div ref="modalRef" class="modal card">
        <div class="row modal-header">
          <div class="grow">
            <input v-model="currentNote.title" class="title-input" type="text" placeholder="笔记标题" />
            <p class="muted">{{ currentNote.id ? `更新于 ${formatDate(currentNote.updateTime)}` : '新建笔记' }}</p>
          </div>
          <div class="actions">
            <button class="ghost-btn" @click="toggleCurrentPin">{{ currentNote.pinned ? '取消置顶' : '置顶' }}</button>
            <button class="ghost-btn" @click="closeModal">关闭</button>
          </div>
        </div>

        <div class="modal-grid">
          <section class="card inner">
            <div class="form-grid">
              <input v-model="currentNote.tags" type="text" placeholder="标签，用逗号分隔" />
              <select v-model="currentNote.reviewState">
                <option value="NEW">新建</option>
                <option value="REVIEW">待复习</option>
                <option value="EVERGREEN">常青</option>
                <option value="ARCHIVED">已归档</option>
              </select>
              <input v-model="currentNote.nextReviewAt" type="datetime-local" />
            </div>
            <textarea v-model="currentNote.content" class="editor" placeholder="输入正文内容"></textarea>
            <div class="row">
              <span :class="{ error: saveStatus.type === 'error' }">{{ saveStatus.text }}</span>
              <div class="actions">
                <button class="ghost-btn" @click="closeModal">关闭</button>
                <button class="primary-btn" :disabled="saving" @click="saveNote">{{ saving ? '保存中...' : '保存笔记' }}</button>
              </div>
            </div>
          </section>

          <section class="side">
            <article class="card inner">
              <div class="row">
                <h4>AI 摘要</h4>
                <button class="secondary-btn" :disabled="!currentNote.id || aiBusy.summary" @click="runSummaryJob">
                  {{ aiBusy.summary ? '已提交' : '生成摘要' }}
                </button>
              </div>
              <p>{{ currentNote.summary || '生成一段简洁摘要，帮助快速回看。' }}</p>
              <small v-if="activeJobs.summary">状态：{{ jobStatusLabel(activeJobs.summary.status) }}</small>
            </article>

            <article class="card inner">
              <div class="row">
                <h4>整理建议</h4>
                <button class="secondary-btn" :disabled="!currentNote.id || aiBusy.organize" @click="runOrganizeJob">
                  {{ aiBusy.organize ? '已提交' : '整理笔记' }}
                </button>
              </div>
              <template v-if="organizeSuggestion">
                <p><strong>标题：</strong>{{ organizeSuggestion.suggestedTitle }}</p>
                <p><strong>标签：</strong>{{ organizeSuggestion.suggestedTags }}</p>
                <p><strong>摘要：</strong>{{ organizeSuggestion.summary }}</p>
                <button class="primary-btn full" @click="applyOrganizeSuggestion">应用整理建议</button>
              </template>
              <p v-else>生成标题、标签和摘要建议，但不会自动覆盖原内容。</p>
              <small v-if="activeJobs.organize">状态：{{ jobStatusLabel(activeJobs.organize.status) }}</small>
            </article>

            <article class="card inner">
              <div class="row">
                <h4>行动项提取</h4>
                <button class="secondary-btn" :disabled="!currentNote.id || aiBusy.extract" @click="runExtractJob">
                  {{ aiBusy.extract ? '已提交' : '提取任务' }}
                </button>
              </div>
              <template v-if="taskSuggestions.length">
                <label v-for="item in taskSuggestions" :key="item.title + item.description" class="task-item">
                  <input v-model="item.selected" type="checkbox" />
                  <div>
                    <strong>{{ item.title }}</strong>
                    <p>{{ item.description || '从笔记中提炼出的下一步。' }}</p>
                  </div>
                </label>
                <button class="primary-btn full" @click="createTasksFromSuggestions">创建选中任务</button>
              </template>
              <p v-else>从当前笔记里提取 1 到 5 条可执行任务。</p>
              <small v-if="activeJobs.extract">状态：{{ jobStatusLabel(activeJobs.extract.status) }}</small>
            </article>

            <article class="card inner">
              <div class="row">
                <h4>最近 AI 作业</h4>
                <button class="ghost-btn small" @click="fetchJobHistory">刷新</button>
              </div>
              <div v-if="jobHistory.length" class="job-list">
                <div v-for="job in jobHistory" :key="job.id" class="job-row">
                  <div class="grow">
                    <strong>{{ jobTypeLabel(job.jobType) }}</strong>
                    <p>{{ formatDate(job.createTime) }}</p>
                  </div>
                  <span>{{ jobStatusLabel(job.status) }}</span>
                </div>
              </div>
              <p v-else>当前笔记还没有 AI 作业记录。</p>
            </article>

            <article class="card inner">
              <h4>复习操作</h4>
              <div class="review-actions">
                <button class="ghost-btn" :disabled="!currentNote.id" @click="completeCurrentReview(2)">2 天后再看</button>
                <button class="ghost-btn" :disabled="!currentNote.id" @click="completeCurrentReview(7)">7 天后再看</button>
                <button class="ghost-btn" :disabled="!currentNote.id" @click="markCurrentEvergreen">标记常青</button>
              </div>
              <p class="muted">{{ currentNote.lastReviewedAt ? `最近复习：${formatDate(currentNote.lastReviewedAt)}` : '还没有复习记录。' }}</p>
            </article>

            <article class="card inner">
              <h4>关联任务</h4>
              <div v-if="relatedTasks.length" class="task-list">
                <div v-for="task in relatedTasks" :key="task.id" class="job-row">
                  <div class="grow">
                    <strong>{{ task.title }}</strong>
                    <p>{{ task.description || '暂无说明' }}</p>
                  </div>
                  <button v-if="task.status !== 2" class="ghost-btn small" @click="completeLinkedTask(task.id)">完成</button>
                </div>
              </div>
              <p v-else>还没有由这篇笔记生成的任务。</p>
            </article>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { noteApi } from '@/api/note'
import { taskApi } from '@/api/task'

const router = useRouter()
const route = useRoute()

const viewOptions = [
  { id: 'all', label: '全部笔记', params: {} },
  { id: 'pinned', label: '已置顶', params: { pinned: true } },
  { id: 'review', label: '待复习', params: { reviewState: 'REVIEWABLE', sort: 'review' } },
  { id: 'recent', label: '最近更新', params: { sort: 'updated' } },
  { id: 'summary', label: '有 AI 摘要', params: { hasSummary: true } },
  { id: 'organize', label: '待整理', params: { needsOrganization: true } }
]

const loading = ref(true)
const saving = ref(false)
const showModal = ref(false)
const notes = ref([])
const tasks = ref([])
const jobHistory = ref([])
const searchKeyword = ref('')
const tagFilter = ref('')
const summaryFilter = ref('all')
const sortOption = ref('updated')
const selectedView = ref(route.query.view || 'all')
const organizeSuggestion = ref(null)
const taskSuggestions = ref([])
const originalNote = ref(null)
const saveStatus = ref({ type: 'info', text: '' })
const activeJobs = reactive({ summary: null, organize: null, extract: null })
const aiBusy = reactive({ summary: false, organize: false, extract: false })
const pollers = new Map()
const modalRef = ref(null)
let syncingRoute = false

const blankNote = () => ({
  id: null,
  title: '',
  content: '',
  tags: '',
  summary: '',
  pinned: false,
  reviewState: 'NEW',
  nextReviewAt: '',
  lastReviewedAt: '',
  updateTime: ''
})

const currentNote = ref(blankNote())

const relatedTasks = computed(() =>
  tasks.value.filter(task => String(task.sourceNoteId || '') === String(currentNote.value.id || ''))
)

const reviewQueue = computed(() =>
  notes.value
    .filter(note => ['REVIEW', 'EVERGREEN'].includes(note.reviewState))
    .slice()
    .sort((left, right) => {
      const leftTime = left.nextReviewAt ? new Date(left.nextReviewAt).getTime() : Number.MAX_SAFE_INTEGER
      const rightTime = right.nextReviewAt ? new Date(right.nextReviewAt).getTime() : Number.MAX_SAFE_INTEGER
      return leftTime - rightTime
    })
)

const reviewInsights = computed(() => {
  const now = Date.now()
  const endOfToday = new Date()
  endOfToday.setHours(23, 59, 59, 999)
  return reviewQueue.value.reduce((acc, note) => {
    acc.total += 1
    if (!note.nextReviewAt) {
      acc.unscheduled += 1
      return acc
    }
    const due = new Date(note.nextReviewAt).getTime()
    if (due < now) {
      acc.overdue += 1
    } else if (due <= endOfToday.getTime()) {
      acc.today += 1
    }
    return acc
  }, { total: 0, overdue: 0, today: 0, unscheduled: 0 })
})

const currentReviewTarget = computed(() => {
  const now = Date.now()
  const dueNow = reviewQueue.value.find(note => note.nextReviewAt && new Date(note.nextReviewAt).getTime() <= now)
  if (dueNow) return dueNow
  const dueToday = reviewQueue.value.find(note => note.nextReviewAt && reviewDueLabel(note) === '今天')
  return dueToday || reviewQueue.value[0] || null
})

const buildRouteQuery = (noteId = null) => {
  const query = {}
  if (selectedView.value && selectedView.value !== 'all') query.view = selectedView.value
  if (noteId) query.noteId = String(noteId)
  return query
}

const buildParams = () => {
  const preset = viewOptions.find(item => item.id === selectedView.value)?.params || {}
  const params = { sort: sortOption.value, ...preset }
  if (searchKeyword.value.trim()) params.keyword = searchKeyword.value.trim()
  if (tagFilter.value.trim()) params.tags = tagFilter.value.trim()
  if (summaryFilter.value === 'with') params.hasSummary = true
  if (summaryFilter.value === 'without') params.hasSummary = false
  return params
}

const fetchNotes = async () => {
  loading.value = true
  try {
    notes.value = await noteApi.search(buildParams())
  } finally {
    loading.value = false
  }
}

const fetchTasks = async () => {
  tasks.value = await taskApi.getList()
}

const fetchJobHistory = async () => {
  if (!currentNote.value.id) {
    jobHistory.value = []
    return
  }
  jobHistory.value = await noteApi.getJobs({ noteId: currentNote.value.id, limit: 8 })
}

const applyNoteDetail = (detail) => {
  currentNote.value = {
    id: detail.id,
    title: detail.title || '',
    content: detail.content || '',
    tags: detail.tags || '',
    summary: detail.summary || '',
    pinned: !!detail.pinned,
    reviewState: detail.reviewState || 'NEW',
    nextReviewAt: toDateTimeLocal(detail.nextReviewAt),
    lastReviewedAt: detail.lastReviewedAt || '',
    updateTime: detail.updateTime
  }
  originalNote.value = { ...currentNote.value }
}

const openCreateModal = async () => {
  currentNote.value = blankNote()
  originalNote.value = null
  organizeSuggestion.value = null
  taskSuggestions.value = []
  jobHistory.value = []
  resetAiState()
  saveStatus.value = { type: 'info', text: '' }
  showModal.value = true
  await nextTick()
  modalRef.value?.scrollTo({ top: 0 })
  await syncRoute(null)
}

const openEditModal = async (noteOrId) => {
  const noteId = typeof noteOrId === 'object' ? noteOrId.id : noteOrId
  if (!noteId) return
  showModal.value = true
  await nextTick()
  modalRef.value?.scrollTo({ top: 0 })
  organizeSuggestion.value = null
  taskSuggestions.value = []
  resetAiState()
  const detail = await noteApi.getDetail(noteId)
  applyNoteDetail(detail)
  await Promise.all([fetchJobHistory(), syncRoute(noteId)])
}

const closeModal = async () => {
  stopAllPollers()
  showModal.value = false
  await syncRoute(null)
}

const syncRoute = async (noteId) => {
  syncingRoute = true
  try {
    await router.replace({ path: '/note', query: buildRouteQuery(noteId) })
  } finally {
    syncingRoute = false
  }
}

const saveNote = async () => {
  saving.value = true
  saveStatus.value = { type: 'info', text: '正在保存笔记...' }
  try {
    if (!currentNote.value.id) {
      const noteId = await noteApi.create({
        title: currentNote.value.title,
        content: currentNote.value.content,
        tags: currentNote.value.tags,
        pinned: currentNote.value.pinned,
        reviewState: currentNote.value.reviewState,
        nextReviewAt: serializeDateTime(currentNote.value.nextReviewAt)
      })
      applyNoteDetail(await noteApi.getDetail(noteId))
      await syncRoute(noteId)
    } else {
      await noteApi.update({
        id: currentNote.value.id,
        title: currentNote.value.title,
        content: currentNote.value.content,
        tags: currentNote.value.tags,
        summary: currentNote.value.summary
      })
      if (!originalNote.value || currentNote.value.pinned !== originalNote.value.pinned) {
        await noteApi.updatePin(currentNote.value.id, currentNote.value.pinned)
      }
      if (!originalNote.value || currentNote.value.reviewState !== originalNote.value.reviewState || currentNote.value.nextReviewAt !== originalNote.value.nextReviewAt) {
        await noteApi.updateReview(currentNote.value.id, {
          reviewState: currentNote.value.reviewState,
          nextReviewAt: serializeDateTime(currentNote.value.nextReviewAt)
        })
      }
      applyNoteDetail(await noteApi.getDetail(currentNote.value.id))
    }
    saveStatus.value = { type: 'success', text: '已保存。' }
    await Promise.all([fetchNotes(), fetchTasks(), fetchJobHistory()])
  } catch (error) {
    console.error(error)
    saveStatus.value = { type: 'error', text: '保存失败，请稍后重试。' }
  } finally {
    saving.value = false
  }
}

const deleteNote = async (noteId) => {
  if (!window.confirm('确定删除这篇笔记吗？')) return
  await noteApi.delete(noteId)
  if (String(currentNote.value.id || '') === String(noteId)) await closeModal()
  await fetchNotes()
}

const toggleCardPin = async (note) => {
  await noteApi.updatePin(note.id, !note.pinned)
  await fetchNotes()
}

const toggleCurrentPin = () => {
  currentNote.value.pinned = !currentNote.value.pinned
}

const applyOrganizeSuggestion = () => {
  if (!organizeSuggestion.value) return
  currentNote.value.title = organizeSuggestion.value.suggestedTitle || currentNote.value.title
  currentNote.value.tags = organizeSuggestion.value.suggestedTags || currentNote.value.tags
  currentNote.value.summary = organizeSuggestion.value.summary || currentNote.value.summary
  saveStatus.value = { type: 'success', text: '整理建议已应用，记得保存。' }
}

const createTasksFromSuggestions = async () => {
  const selected = taskSuggestions.value.filter(item => item.selected)
  if (!selected.length) {
    saveStatus.value = { type: 'error', text: '请至少选择一条任务建议。' }
    return
  }
  await Promise.all(selected.map(item => taskApi.create({
    title: item.title,
    description: item.description,
    tags: item.tags || currentNote.value.tags,
    sourceNoteId: currentNote.value.id
  })))
  taskSuggestions.value = taskSuggestions.value.filter(item => !item.selected)
  saveStatus.value = { type: 'success', text: '已创建关联任务。' }
  await fetchTasks()
}

const completeLinkedTask = async (taskId) => {
  await taskApi.complete(taskId)
  await fetchTasks()
}

const updateReviewState = async (note, nextReviewAt, reviewState = null) => {
  await noteApi.updateReview(note.id, {
    reviewState: reviewState || (note.reviewState === 'EVERGREEN' ? 'EVERGREEN' : 'REVIEW'),
    nextReviewAt
  })
  if (String(currentNote.value.id || '') === String(note.id)) {
    applyNoteDetail(await noteApi.getDetail(note.id))
    await fetchJobHistory()
  }
  await fetchNotes()
}

const completeReview = async (note, days) => {
  const next = new Date()
  next.setDate(next.getDate() + days)
  await updateReviewState(note, next.toISOString(), note.reviewState === 'EVERGREEN' ? 'EVERGREEN' : 'REVIEW')
}

const markEvergreen = async (note) => {
  const next = new Date()
  next.setDate(next.getDate() + 14)
  await updateReviewState(note, next.toISOString(), 'EVERGREEN')
}

const completeCurrentReview = async (days) => {
  if (!currentNote.value.id) return
  await completeReview(currentNote.value, days)
  saveStatus.value = { type: 'success', text: `已记录复习，下次安排在 ${days} 天后。` }
}

const markCurrentEvergreen = async () => {
  if (!currentNote.value.id) return
  await markEvergreen(currentNote.value)
  saveStatus.value = { type: 'success', text: '已标记为常青内容。' }
}

const runSummaryJob = () => startJob('summary', () => noteApi.generateSummary(currentNote.value.id))
const runOrganizeJob = () => startJob('organize', () => noteApi.organize(currentNote.value.id))
const runExtractJob = () => startJob('extract', () => noteApi.extractTasks(currentNote.value.id))

const startJob = async (kind, submit) => {
  try {
    aiBusy[kind] = true
    activeJobs[kind] = await submit()
    await fetchJobHistory()
    pollJob(kind, activeJobs[kind].id)
  } catch (error) {
    console.error(error)
    aiBusy[kind] = false
    saveStatus.value = { type: 'error', text: 'AI 任务提交失败。' }
  }
}

const pollJob = (kind, jobId) => {
  stopPoller(kind)
  const runner = async () => {
    const job = await noteApi.getJob(jobId)
    activeJobs[kind] = job
    if (job.status === 'SUCCESS') {
      aiBusy[kind] = false
      applyJobResult(kind, job.result || {})
      await Promise.all([fetchNotes(), fetchJobHistory()])
      stopPoller(kind)
      return
    }
    if (job.status === 'FAILED') {
      aiBusy[kind] = false
      saveStatus.value = { type: 'error', text: job.errorMessage || 'AI 作业执行失败。' }
      await fetchJobHistory()
      stopPoller(kind)
      return
    }
    pollers.set(kind, window.setTimeout(runner, 2000))
  }
  runner()
}

const applyJobResult = (kind, result) => {
  if (kind === 'summary') {
    currentNote.value.summary = result.summary || currentNote.value.summary
    saveStatus.value = { type: 'success', text: '摘要已生成。' }
  } else if (kind === 'organize') {
    organizeSuggestion.value = {
      suggestedTitle: result.suggestedTitle || '',
      suggestedTags: result.suggestedTags || '',
      summary: result.summary || ''
    }
    saveStatus.value = { type: 'success', text: '整理建议已就绪。' }
  } else if (kind === 'extract') {
    taskSuggestions.value = (result.tasks || []).map(item => ({ ...item, selected: true }))
    saveStatus.value = { type: 'success', text: '任务建议已提取。' }
  }
}

const stopPoller = (kind) => {
  const timer = pollers.get(kind)
  if (timer) {
    clearTimeout(timer)
    pollers.delete(kind)
  }
}

const stopAllPollers = () => {
  Array.from(pollers.keys()).forEach(stopPoller)
  resetAiState()
}

const resetAiState = () => {
  Object.keys(aiBusy).forEach(key => { aiBusy[key] = false })
  Object.keys(activeJobs).forEach(key => { activeJobs[key] = null })
}

const selectView = async (viewId) => {
  selectedView.value = viewId
  await syncRoute(currentNote.value.id || null)
  await fetchNotes()
}

const resetFilters = async () => {
  searchKeyword.value = ''
  tagFilter.value = ''
  summaryFilter.value = 'all'
  sortOption.value = 'updated'
  selectedView.value = 'all'
  await syncRoute(currentNote.value.id || null)
  await fetchNotes()
}

const splitTags = (value) => (value || '').split(',').map(item => item.trim()).filter(Boolean)
const previewText = (value) => {
  const text = (value || '').replace(/\s+/g, ' ').trim()
  if (!text) return '这篇笔记还没有正文内容。'
  return text.length > 96 ? `${text.slice(0, 96)}...` : text
}

const reviewLabel = (value) => ({ REVIEW: '待复习', EVERGREEN: '常青', ARCHIVED: '已归档' }[value] || '新建')
const jobTypeLabel = (value) => ({ SUMMARY: '摘要', ORGANIZE: '整理', EXTRACT_TASKS: '提取任务', WEEKLY_REVIEW: '周复盘' }[value] || value)
const jobStatusLabel = (value) => ({ PENDING: '排队中', PROCESSING: '处理中', SUCCESS: '已完成', FAILED: '失败' }[value] || '未知状态')
const formatDate = (value) => value ? new Date(value).toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : '未设置时间'

const reviewDueLabel = (note) => {
  if (!['REVIEW', 'EVERGREEN'].includes(note.reviewState)) {
    return note.nextReviewAt ? `下次复习 ${formatDate(note.nextReviewAt)}` : '未安排复习'
  }
  if (!note.nextReviewAt) return '未排时间'
  const due = new Date(note.nextReviewAt).getTime()
  const now = Date.now()
  const endOfToday = new Date()
  endOfToday.setHours(23, 59, 59, 999)
  if (due < now) return '已到期'
  if (due <= endOfToday.getTime()) return '今天'
  return `下次复习 ${formatDate(note.nextReviewAt)}`
}

const toDateTimeLocal = (value) => value ? new Date(new Date(value).getTime() - new Date(value).getTimezoneOffset() * 60000).toISOString().slice(0, 16) : ''
const serializeDateTime = (value) => value ? new Date(value).toISOString() : null

watch(() => route.query.view, async value => {
  const nextView = viewOptions.some(item => item.id === value) ? value : 'all'
  if (selectedView.value !== nextView) {
    selectedView.value = nextView
    await fetchNotes()
  }
})

watch(() => route.query.noteId, async value => {
  if (syncingRoute) return
  if (value) {
    if (!showModal.value || String(currentNote.value.id || '') !== String(value)) {
      await openEditModal(value)
    }
    return
  }
  if (showModal.value) {
    stopAllPollers()
    showModal.value = false
  }
})

onMounted(async () => {
  await Promise.all([fetchNotes(), fetchTasks()])
  if (route.query.noteId) {
    await openEditModal(route.query.noteId)
  }
})

onBeforeUnmount(() => {
  stopAllPollers()
  document.body.style.overflow = ''
  document.body.style.touchAction = ''
})

watch(showModal, (visible) => {
  document.body.style.overflow = visible ? 'hidden' : ''
  document.body.style.touchAction = visible ? 'none' : ''
})
</script>

<style scoped>
.page { max-width: 1280px; margin: 0 auto; padding: 32px; }
.card { background: #fff; border: 1px solid #d8e3ec; border-radius: 24px; box-shadow: 0 18px 32px rgba(15, 23, 42, 0.06); }
.hero { display: flex; justify-content: space-between; gap: 24px; padding: 32px; margin-bottom: 18px; background: linear-gradient(135deg, #103d5d, #0f766e); color: #fff; }
.eyebrow, .section-label { margin: 0 0 10px; font-size: 12px; letter-spacing: 0.18em; text-transform: uppercase; color: rgba(255,255,255,0.72); }
.section-label { color: #5e7388; }
.hero h1 { margin: 0 0 12px; font-size: 38px; line-height: 1.08; }
.hero p { margin: 0; line-height: 1.8; }
.toolbar, .review-cockpit { padding: 18px; margin-bottom: 18px; display: grid; gap: 14px; }
.views, .filters, .row, .actions, .badges, .tags, .quick-actions, .review-actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.filters input, .filters select, .form-grid input, .form-grid select, .title-input, .editor { border: 1px solid #d7e2eb; border-radius: 14px; background: #f8fbfd; padding: 12px 14px; font: inherit; color: #102033; }
.filters input, .filters select { flex: 1; min-width: 180px; }
.chip, .primary-btn, .secondary-btn, .ghost-btn, .danger-btn { border: none; border-radius: 14px; padding: 11px 14px; font: inherit; cursor: pointer; }
.chip, .ghost-btn { background: #f5f9fc; color: #375068; }
.chip.active, .primary-btn { background: #103d5d; color: #fff; }
.secondary-btn { background: #dbf0f0; color: #0f5f67; }
.danger-btn { background: #fff1f2; color: #be123c; }
.small { padding: 8px 10px; font-size: 13px; }
.empty { padding: 26px; color: #607487; }
.review-stats, .review-grid { display: grid; gap: 16px; }
.review-stats { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.review-stats article, .review-focus, .review-item { padding: 16px; border-radius: 18px; border: 1px solid #e1eaf1; background: #f8fbfd; }
.review-stats strong { font-size: 28px; color: #102033; }
.review-grid { grid-template-columns: 1.15fr 0.85fr; }
.review-focus { display: grid; gap: 12px; }
.focus-copy, .preview, .meta, .summary, .muted, .task-item p, .job-row p, .review-link p { color: #607487; line-height: 1.7; }
.review-list { display: grid; gap: 12px; }
.review-link { width: 100%; border: none; background: transparent; display: flex; justify-content: space-between; gap: 12px; text-align: left; cursor: pointer; padding: 0; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(290px, 1fr)); gap: 18px; }
.note-card { padding: 20px; display: grid; gap: 12px; cursor: pointer; }
.badge, .tag { background: #eaf0f6; color: #31516c; border-radius: 999px; padding: 6px 10px; font-size: 12px; }
.badge.success { background: #dcfce7; color: #166534; }
.summary { min-height: 84px; padding: 14px; border-radius: 18px; background: #f8fbfd; border: 1px solid #e1eaf1; }
.meta { display: flex; flex-direction: column; gap: 4px; font-size: 12px; }
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding:
    calc(var(--app-header-height, 96px) + 16px)
    max(16px, env(safe-area-inset-right))
    max(16px, env(safe-area-inset-bottom))
    max(16px, env(safe-area-inset-left));
  overflow: auto;
  overscroll-behavior: contain;
  z-index: 80;
}
.modal {
  width: min(1320px, 100%);
  max-height: calc(100vh - var(--app-header-height, 96px) - 32px);
  overflow: auto;
  padding: 24px;
  margin: 0 auto;
}
.modal-header { margin-bottom: 18px; }
.grow { flex: 1; }
.title-input { width: 100%; font-size: 28px; font-weight: 700; margin-bottom: 10px; }
.modal-grid { display: grid; grid-template-columns: 1.15fr 0.85fr; gap: 18px; }
.inner { padding: 18px; }
.form-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-bottom: 12px; }
.editor { width: 100%; min-height: 360px; max-height: min(60vh, 720px); resize: vertical; }
.side { display: grid; gap: 16px; }
.full { width: 100%; margin-top: 12px; }
.task-item, .job-row { display: flex; gap: 10px; align-items: flex-start; padding: 12px; border-radius: 16px; background: #f8fbfd; border: 1px solid #e1eaf1; }
.task-item + .task-item, .job-row + .job-row { margin-top: 10px; }
.error { color: #b91c1c; }
@media (max-width: 1100px) { .modal-grid, .form-grid, .review-stats, .review-grid { grid-template-columns: 1fr; } }
@media (max-width: 768px) {
  .page { padding: 20px; }
  .hero, .modal-header { flex-direction: column; align-items: stretch; }
  .overlay {
    padding:
      calc(var(--app-header-height, 132px) + 12px)
      12px
      12px;
  }
  .modal {
    max-height: calc(100vh - var(--app-header-height, 132px) - 24px);
    padding: 18px;
  }
}
</style>
