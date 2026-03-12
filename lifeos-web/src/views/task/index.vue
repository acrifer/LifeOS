<template>
  <div class="task-container">
    <div class="header">
      <div class="header-titles">
        <h1>Task Management</h1>
        <p>Organize your work, set deadlines, and track your progress.</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-primary" @click="showCreateModal = true">
          <span class="icon">+</span> New Task
        </button>
        <button class="btn btn-ai" @click="handleAiGenerate">
          <span class="icon">✨</span> AI Plan
        </button>
      </div>
    </div>

    <!-- Task List -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading tasks...</p>
    </div>

    <div v-else-if="tasks.length === 0" class="empty-state glass-panel">
      <div class="empty-icon">📝</div>
      <h3>No tasks yet</h3>
      <p>Get started by creating your first task or use AI to generate a plan.</p>
      <button class="btn btn-primary mt-4" @click="showCreateModal = true">Create Task</button>
    </div>

    <div v-else class="task-grid">
      <div 
        v-for="task in tasks" 
        :key="task.id" 
        class="task-card glass-panel"
        :class="{ 'completed': task.status === 2 }"
      >
        <div class="task-header">
          <h3 class="task-title" :class="{ 'strike': task.status === 2 }">{{ task.title }}</h3>
          <span class="status-badge" :class="getStatusClass(task.status)">
            {{ getStatusText(task.status) }}
          </span>
        </div>
        
        <p class="task-desc">{{ task.description || 'No description provided.' }}</p>
        
        <div class="task-meta">
          <div v-if="task.deadline" class="meta-item deadline" :class="{ 'overdue': isOverdue(task.deadline) && task.status !== 2 }">
            <span class="icon">⏱</span> {{ formatDate(task.deadline) }}
          </div>
          
          <div class="tags-container" v-if="task.tags">
            <span v-for="tag in task.tags.split(',')" :key="tag" class="tag">#{{ tag.trim() }}</span>
          </div>
        </div>

        <div class="task-actions" v-if="task.status !== 2">
          <button class="btn-icon check" @click="completeTask(task.id)" title="Complete Task">
            ✓
          </button>
          <button class="btn-icon delete" @click="deleteTask(task.id)" title="Delete Task">
            ✕
          </button>
        </div>
      </div>
    </div>

    <!-- Create Modal -->
    <div class="modal-backdrop" v-if="showCreateModal" @click.self="showCreateModal = false">
      <div class="modal glass-panel">
        <h2>Create New Task</h2>
        <form @submit.prevent="submitTask">
          <div class="form-group">
            <label>Title <span class="required">*</span></label>
            <input type="text" v-model="newTask.title" required placeholder="e.g., Learn Redis" />
          </div>
          
          <div class="form-group">
            <label>Description</label>
            <textarea v-model="newTask.description" rows="3" placeholder="Add some details..."></textarea>
          </div>
          
          <div class="form-row">
            <div class="form-group">
              <label>Deadline</label>
              <input type="datetime-local" v-model="newTask.deadline" />
            </div>
            <div class="form-group">
              <label>Tags (Comma separated)</label>
              <input type="text" v-model="newTask.tags" placeholder="e.g., Backend, Study" />
            </div>
          </div>
          
          <div class="modal-actions">
            <button type="button" class="btn btn-ghost" @click="showCreateModal = false">Cancel</button>
            <button type="submit" class="btn btn-primary" :disabled="submitting">
              {{ submitting ? 'Saving...' : 'Create Task' }}
            </button>
          </div>
        </form>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { taskApi } from '@/api/task'

const tasks = ref([])
const loading = ref(true)
const showCreateModal = ref(false)
const submitting = ref(false)

const newTask = ref({
  title: '',
  description: '',
  deadline: '',
  tags: ''
})

const fetchTasks = async () => {
  loading.value = true
  try {
    const res = await taskApi.getList()
    tasks.value = res || []
  } catch (error) {
    console.error('Failed to fetch tasks', error)
  } finally {
    loading.value = false
  }
}

const submitTask = async () => {
  if (!newTask.value.title) return
  
  submitting.value = true
  try {
    const payload = { ...newTask.value }
    if (payload.deadline) {
      payload.deadline = new Date(payload.deadline).toISOString()
    } else {
      payload.deadline = null
    }
    
    await taskApi.create(payload)
    showCreateModal.value = false
    
    // Reset form
    newTask.value = { title: '', description: '', deadline: '', tags: '' }
    
    // Refresh list
    fetchTasks()
  } catch (error) {
    alert('Failed to create task')
  } finally {
    submitting.value = false
  }
}

const completeTask = async (id) => {
  try {
    await taskApi.complete(id)
    fetchTasks()
  } catch (error) {
    alert('Failed to complete task')
  }
}

const deleteTask = async (id) => {
  if (!confirm('Are you sure you want to delete this task?')) return
  try {
    await taskApi.delete(id)
    fetchTasks()
  } catch (error) {
    alert('Failed to delete task')
  }
}

const handleAiGenerate = () => {
  alert('AI Plan Generation will be implemented when the AI service is ready!')
}

// Helpers
const getStatusText = (status) => {
  switch(status) {
    case 0: return 'Pending'
    case 1: return 'In Progress'
    case 2: return 'Completed'
    default: return 'Unknown'
  }
}

const getStatusClass = (status) => {
  switch(status) {
    case 0: return 'badge-pending'
    case 1: return 'badge-progress'
    case 2: return 'badge-completed'
    default: return ''
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

const isOverdue = (dateStr) => {
  if (!dateStr) return false
  return new Date(dateStr) < new Date()
}

onMounted(() => {
  fetchTasks()
})
</script>

<style scoped>
.task-container {
  padding: 40px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 40px;
}

.header h1 {
  font-size: 32px;
  color: #1e293b;
  margin: 0 0 8px 0;
  font-weight: 800;
}

.header p {
  color: #64748b;
  margin: 0;
  font-size: 16px;
}

.header-actions {
  display: flex;
  gap: 16px;
}

.btn {
  padding: 10px 20px;
  border-radius: 12px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-primary {
  background: #6366f1;
  color: white;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.btn-primary:hover {
  background: #4f46e5;
  transform: translateY(-2px);
}

.btn-ai {
  background: linear-gradient(135deg, #a855f7 0%, #ec4899 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(168, 85, 247, 0.3);
}

.btn-ai:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(168, 85, 247, 0.4);
}

.btn-ghost {
  background: transparent;
  color: #64748b;
}

.btn-ghost:hover {
  background: #f1f5f9;
  color: #1e293b;
}

.glass-panel {
  background: white;
  border-radius: 20px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
  border: 1px solid #f1f5f9;
}

.task-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.task-card {
  padding: 24px;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
}

.task-card:hover {
  transform: translateY(-5px) scale(1.02);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.task-card.completed {
  opacity: 0.7;
  background: #f8fafc;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.task-title {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
  font-weight: 700;
  line-height: 1.4;
  padding-right: 12px;
}

.task-title.strike {
  text-decoration: line-through;
  color: #94a3b8;
}

.status-badge {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 20px;
  font-weight: 700;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.badge-pending { background: #fee2e2; color: #ef4444; }
.badge-progress { background: #fef3c7; color: #d97706; }
.badge-completed { background: #dcfce7; color: #16a34a; }

.task-desc {
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 20px 0;
  flex-grow: 1;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.meta-item {
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 4px;
  background: #f1f5f9;
  padding: 4px 10px;
  border-radius: 6px;
  font-weight: 500;
}

.meta-item.overdue {
  background: #fef2f2;
  color: #ef4444;
}

.tags-container {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag {
  font-size: 11px;
  color: #4f46e5;
  background: #e0e7ff;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 600;
}

.task-actions {
  position: absolute;
  top: 24px;
  right: 24px;
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.2s;
  background: white;
  padding: 4px;
  border-radius: 8px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.task-card:hover .task-actions {
  opacity: 1;
}

.btn-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: white;
  transition: all 0.2s;
  font-size: 14px;
}

.btn-icon.check { background: #10b981; }
.btn-icon.check:hover { background: #059669; transform: scale(1.1); }
.btn-icon.delete { background: #ef4444; }
.btn-icon.delete:hover { background: #dc2626; transform: scale(1.1); }

/* Modal Styles */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  animation: fadeIn 0.3s;
}

.modal {
  width: 100%;
  max-width: 500px;
  padding: 32px;
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal h2 {
  margin: 0 0 24px 0;
  font-size: 24px;
  color: #1e293b;
}

.form-group {
  margin-bottom: 20px;
}

.form-row {
  display: flex;
  gap: 20px;
}

.form-row .form-group {
  flex: 1;
}

label {
  display: block;
  font-size: 13px;
  color: #475569;
  font-weight: 600;
  margin-bottom: 8px;
}

.required { color: #ef4444; }

input, textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  font-size: 15px;
  transition: all 0.2s;
  background: #f8fafc;
  font-family: inherit;
  box-sizing: border-box;
}

input:focus, textarea:focus {
  outline: none;
  border-color: #6366f1;
  background: white;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 32px;
}

.empty-state {
  text-align: center;
  padding: 80px 40px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 24px;
  opacity: 0.5;
}

.empty-state h3 {
  font-size: 24px;
  color: #1e293b;
  margin: 0 0 12px 0;
}

.empty-state p {
  color: #64748b;
  margin: 0;
  max-width: 400px;
  margin: 0 auto;
}

.loading-state {
  text-align: center;
  padding: 80px;
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

.mt-4 { margin-top: 24px; }

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
