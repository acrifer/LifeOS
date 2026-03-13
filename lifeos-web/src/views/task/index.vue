<template>
  <div class="task-page">
    <section class="task-hero">
      <div>
        <p class="eyebrow">任务层</p>
        <h1>让笔记里的想法真正变成行动。</h1>
        <p>
          这里保留轻量任务管理，重点是让从笔记提取出来的行动项清晰可见、容易完成。
        </p>
      </div>
      <button class="primary-btn" @click="showCreateModal = true">新建任务</button>
    </section>

    <section class="stat-strip">
      <article class="stat-card">
        <strong>{{ pendingCount }}</strong>
        <span>待完成</span>
      </article>
      <article class="stat-card">
        <strong>{{ derivedCount }}</strong>
        <span>来自笔记</span>
      </article>
      <article class="stat-card">
        <strong>{{ completedCount }}</strong>
        <span>已完成</span>
      </article>
    </section>

    <section class="task-toolbar">
      <label>
        <span>视图</span>
        <select v-model="viewMode">
          <option value="all">全部任务</option>
          <option value="derived">来自笔记</option>
          <option value="manual">手动创建</option>
          <option value="completed">已完成</option>
        </select>
      </label>
      <label>
        <span>搜索</span>
        <input v-model="searchKeyword" type="text" placeholder="搜索任务标题或来源笔记" />
      </label>
    </section>

    <section v-if="loading" class="empty-panel">正在加载任务...</section>
    <section v-else-if="filteredTasks.length === 0" class="empty-panel">
      <h2>当前视图下没有任务</h2>
      <p>你可以手动创建任务，或者从笔记里提取行动项。</p>
    </section>
    <section v-else class="task-grid">
      <article
        v-for="task in filteredTasks"
        :key="task.id"
        class="task-card"
        :class="{ completed: task.status === 2 }"
      >
        <div class="task-card-top">
          <span class="status-pill" :class="statusClass(task.status)">{{ statusLabel(task.status) }}</span>
          <div class="card-actions">
            <button v-if="task.status !== 2" class="ghost-btn" @click="completeTask(task.id)">完成</button>
            <button class="ghost-btn danger" @click="deleteTask(task.id)">删除</button>
          </div>
        </div>

        <h3>{{ task.title }}</h3>
        <p class="task-description">{{ task.description || '暂无任务说明。' }}</p>

        <div v-if="task.sourceNoteId" class="source-box">
          <span class="source-label">来源笔记</span>
          <strong>{{ sourceNoteTitle(task.sourceNoteId) }}</strong>
        </div>

        <div class="task-meta">
          <span v-if="task.deadline">截止 {{ formatDate(task.deadline) }}</span>
          <span>{{ task.sourceNoteId ? '知识任务' : '手动任务' }}</span>
        </div>

        <div class="tag-list">
          <span v-for="tag in splitTags(task.tags)" :key="tag" class="tag-chip">#{{ tag }}</span>
        </div>
      </article>
    </section>

    <div v-if="showCreateModal" class="modal-backdrop" @click.self="showCreateModal = false">
      <div class="modal-card">
        <h2>创建手动任务</h2>
        <div class="form-grid">
          <label>
            <span>标题</span>
            <input v-model="newTask.title" type="text" placeholder="任务标题" />
          </label>
          <label>
            <span>截止时间</span>
            <input v-model="newTask.deadline" type="datetime-local" />
          </label>
        </div>
        <label>
          <span>说明</span>
          <textarea v-model="newTask.description" rows="4" placeholder="补充任务说明"></textarea>
        </label>
        <label>
          <span>标签</span>
          <input v-model="newTask.tags" type="text" placeholder="多个标签用逗号分隔" />
        </label>
        <div class="modal-actions">
          <button class="ghost-btn" @click="showCreateModal = false">取消</button>
          <button class="primary-btn" :disabled="submitting" @click="submitTask">
            {{ submitting ? '保存中...' : '创建任务' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { noteApi } from '@/api/note'
import { taskApi } from '@/api/task'

const route = useRoute()

const loading = ref(true)
const submitting = ref(false)
const showCreateModal = ref(false)
const viewMode = ref('all')
const searchKeyword = ref('')
const tasks = ref([])
const noteMap = ref({})

const newTask = ref({
  title: '',
  description: '',
  deadline: '',
  tags: ''
})

const filteredTasks = computed(() => {
  return tasks.value.filter(task => {
    if (viewMode.value === 'derived' && !task.sourceNoteId) {
      return false
    }
    if (viewMode.value === 'manual' && task.sourceNoteId) {
      return false
    }
    if (viewMode.value === 'completed' && task.status !== 2) {
      return false
    }
    if (!searchKeyword.value.trim()) {
      return true
    }
    const keyword = searchKeyword.value.trim().toLowerCase()
    const sourceTitle = sourceNoteTitle(task.sourceNoteId).toLowerCase()
    return (
      (task.title || '').toLowerCase().includes(keyword) ||
      (task.description || '').toLowerCase().includes(keyword) ||
      sourceTitle.includes(keyword)
    )
  })
})

const pendingCount = computed(() => tasks.value.filter(task => task.status !== 2).length)
const completedCount = computed(() => tasks.value.filter(task => task.status === 2).length)
const derivedCount = computed(() => tasks.value.filter(task => task.sourceNoteId).length)

const fetchTasks = async () => {
  loading.value = true
  try {
    tasks.value = await taskApi.getList()
  } catch (error) {
    console.error('Failed to fetch tasks', error)
  } finally {
    loading.value = false
  }
}

const fetchNotes = async () => {
  try {
    const notes = await noteApi.getList()
    noteMap.value = notes.reduce((accumulator, note) => {
      accumulator[String(note.id)] = note.title || '未命名笔记'
      return accumulator
    }, {})
  } catch (error) {
    console.error('Failed to fetch note titles for tasks', error)
  }
}

const submitTask = async () => {
  if (!newTask.value.title.trim()) {
    return
  }
  submitting.value = true
  try {
    await taskApi.create({
      title: newTask.value.title,
      description: newTask.value.description,
      deadline: newTask.value.deadline ? new Date(newTask.value.deadline).toISOString() : null,
      tags: newTask.value.tags
    })
    newTask.value = { title: '', description: '', deadline: '', tags: '' }
    showCreateModal.value = false
    await fetchTasks()
  } catch (error) {
    console.error('Failed to create task', error)
  } finally {
    submitting.value = false
  }
}

const completeTask = async (taskId) => {
  try {
    await taskApi.complete(taskId)
    await fetchTasks()
  } catch (error) {
    console.error('Failed to complete task', error)
  }
}

const deleteTask = async (taskId) => {
  if (!confirm('确认删除这个任务吗？')) {
    return
  }
  try {
    await taskApi.delete(taskId)
    await fetchTasks()
  } catch (error) {
    console.error('Failed to delete task', error)
  }
}

const statusLabel = (status) => {
  switch (status) {
    case 2:
      return '已完成'
    case 1:
      return '进行中'
    default:
      return '待处理'
  }
}

const statusClass = (status) => {
  switch (status) {
    case 2:
      return 'completed'
    case 1:
      return 'progress'
    default:
      return 'pending'
  }
}

const sourceNoteTitle = (noteId) => {
  if (!noteId) {
    return '手动任务'
  }
  return noteMap.value[String(noteId)] || `笔记 #${noteId}`
}

const splitTags = (value) =>
  (value || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)

const formatDate = (value) => {
  if (!value) {
    return '未设置截止时间'
  }
  return new Date(value).toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

watch(
  () => route.query.view,
  value => {
    if (typeof value === 'string' && ['all', 'derived', 'manual', 'completed'].includes(value)) {
      viewMode.value = value
    }
  },
  { immediate: true }
)

onMounted(async () => {
  await Promise.all([fetchTasks(), fetchNotes()])
})
</script>

<style scoped>
.task-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px;
}

.task-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-end;
  background: linear-gradient(135deg, #111827, #1d4ed8);
  color: white;
  border-radius: 28px;
  padding: 32px;
  margin-bottom: 20px;
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.task-hero h1 {
  margin: 0 0 12px;
  font-size: 38px;
  line-height: 1.15;
  max-width: 760px;
}

.task-hero p {
  margin: 0;
  max-width: 760px;
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.7;
}

.stat-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card,
.task-toolbar,
.task-card,
.empty-panel,
.modal-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
}

.stat-card {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-card strong {
  font-size: 34px;
  color: #0f172a;
}

.stat-card span {
  color: #64748b;
}

.task-toolbar {
  padding: 18px;
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.task-toolbar label,
.form-grid label,
.modal-card label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-weight: 600;
  color: #475569;
  font-size: 13px;
}

input,
select,
textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  font: inherit;
}

.task-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 18px;
}

.task-card {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
}

.task-card.completed {
  opacity: 0.7;
}

.task-card-top,
.card-actions,
.modal-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.status-pill {
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 700;
}

.status-pill.pending {
  background: #fee2e2;
  color: #b91c1c;
}

.status-pill.progress {
  background: #fef3c7;
  color: #92400e;
}

.status-pill.completed {
  background: #dcfce7;
  color: #166534;
}

.ghost-btn,
.primary-btn {
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.ghost-btn {
  background: #f8fafc;
  color: #334155;
  border-radius: 12px;
  padding: 10px 12px;
}

.ghost-btn.danger {
  color: #b91c1c;
}

.primary-btn {
  background: #1d4ed8;
  color: white;
  border-radius: 16px;
  padding: 12px 18px;
  font-weight: 700;
}

.task-card h3,
.modal-card h2 {
  margin: 0;
  color: #0f172a;
}

.task-description {
  margin: 0;
  color: #475569;
  line-height: 1.6;
}

.source-box {
  padding: 14px;
  border-radius: 18px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
}

.source-label {
  display: block;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: #1d4ed8;
  margin-bottom: 6px;
}

.task-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #64748b;
  font-size: 13px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-chip {
  background: #e0f2fe;
  color: #0369a1;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 700;
}

.empty-panel {
  padding: 80px 24px;
  text-align: center;
  color: #64748b;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 40;
}

.modal-card {
  width: min(560px, 100%);
  padding: 24px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 18px 0;
}

.modal-actions {
  margin-top: 18px;
}

@media (max-width: 900px) {
  .task-page {
    padding: 20px;
  }

  .task-hero,
  .task-toolbar,
  .form-grid,
  .stat-strip {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
