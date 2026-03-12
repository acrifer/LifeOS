<template>
  <div class="note-container">
    <div class="header">
      <div class="header-titles">
        <h1>Notes & Knowledge</h1>
        <p>Capture your thoughts, ideas, and learnings in one place.</p>
      </div>
      <div class="header-actions">
        <div class="search-bar">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="Search notes or tags..."
            @keyup.enter="handleSearch"
          />
          <button class="btn-search" @click="handleSearch">Search</button>
        </div>
        <button class="btn btn-primary" @click="openCreateModal">
          <span class="icon">+</span> New Note
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading your knowledge base...</p>
    </div>

    <div v-else-if="notes.length === 0" class="empty-state glass-panel">
      <div class="empty-icon">N</div>
      <h3>No notes found</h3>
      <p>{{ searchKeyword ? 'Try a different search term.' : 'Start writing to build your personal wiki.' }}</p>
      <button v-if="!searchKeyword" class="btn btn-primary mt-4" @click="openCreateModal">Create First Note</button>
      <button v-else class="btn btn-ghost mt-4" @click="clearSearch">Clear Search</button>
    </div>

    <div v-else class="note-grid">
      <div
        v-for="note in notes"
        :key="note.id"
        class="note-card glass-panel"
        @click="openEditModal(note)"
      >
        <div class="note-header">
          <h3 class="note-title">{{ note.title || 'Untitled Note' }}</h3>
          <span class="summary-chip" :class="summaryStateClass(note)">
            {{ summaryStateLabel(note) }}
          </span>
        </div>

        <p class="note-preview">{{ getBodyPreview(note.content) }}</p>
        <p class="summary-preview" :class="{ placeholder: !note.summary }">
          {{ getSummaryPreview(note) }}
        </p>

        <div class="note-footer">
          <div class="time-meta">
            <span>Created {{ formatDateTime(note.createTime) }}</span>
            <span>Updated {{ formatDateTime(note.updateTime) }}</span>
          </div>
          <div v-if="note.tags" class="tags-container">
            <span v-for="tag in note.tags.split(',').slice(0, 2)" :key="tag" class="tag">#{{ tag.trim() }}</span>
            <span v-if="note.tags.split(',').length > 2" class="tag-more">...</span>
          </div>
        </div>

        <button class="btn-delete" @click.stop="deleteNote(note.id)">X</button>
      </div>
    </div>

    <div v-if="showModal" class="modal-backdrop" @click.self="closeModal">
      <div class="modal glass-panel note-modal">
        <div class="modal-header">
          <div class="modal-header-main">
            <input
              v-model="currentNote.title"
              type="text"
              class="title-input"
              placeholder="Note Title"
            />
            <div class="meta-row">
              <span>Created {{ formatDateTime(currentNote.createTime) || 'Not saved yet' }}</span>
              <span>Updated {{ formatDateTime(currentNote.updateTime) || 'Not saved yet' }}</span>
            </div>
          </div>
          <button class="btn-close" @click="closeModal">X</button>
        </div>

        <div class="modal-body">
          <div class="editor-toolbar">
            <input
              v-model="currentNote.tags"
              type="text"
              class="tags-input"
              placeholder="Add tags separated by comma (e.g., Guide, Docker)"
            />
          </div>

          <label class="toggle-row">
            <input v-model="autoSummaryEnabled" type="checkbox" />
            <span>保存后自动总结</span>
          </label>

          <textarea
            v-model="currentNote.content"
            class="markdown-editor"
            placeholder="Write your note in Markdown here..."
          ></textarea>

          <div class="ai-summary" :class="summaryStateClass(currentNote)">
            <div class="ai-summary-header">
              <h4>AI Summary</h4>
              <span class="summary-chip" :class="summaryStateClass(currentNote)">
                {{ summaryStateLabel(currentNote) }}
              </span>
            </div>
            <p>{{ getSummaryPreview(currentNote, true) }}</p>
          </div>
        </div>

        <div class="modal-actions">
          <span class="save-status">{{ saveStatus }}</span>
          <button
            v-if="currentNote.id"
            class="btn btn-secondary"
            :disabled="saving || currentAiState === 'generating'"
            @click="triggerSummary(false)"
          >
            {{ currentNote.summary ? '重新生成总结' : '生成总结' }}
          </button>
          <button class="btn btn-primary" :disabled="saving" @click="saveNote">
            {{ saving ? 'Saving...' : 'Save Note' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { noteApi } from '@/api/note'

const SUMMARY_TIMEOUT_MESSAGE = 'AI 仍在生成，可稍后刷新或重新打开查看'

const notes = ref([])
const loading = ref(true)
const showModal = ref(false)
const saving = ref(false)
const saveStatus = ref('')
const searchKeyword = ref('')
const autoSummaryEnabled = ref(true)
const currentAiState = ref('idle')
const noteAiStateMap = ref({})
const originalNote = ref(null)

const currentNote = ref({
  id: null,
  title: '',
  content: '',
  tags: '',
  summary: '',
  createTime: null,
  updateTime: null
})

const fetchNotes = async () => {
  loading.value = true
  try {
    const res = await noteApi.getList()
    notes.value = res || []
    syncSummaryState(notes.value)
  } catch (error) {
    console.error('Failed to fetch notes', error)
  } finally {
    loading.value = false
  }
}

const syncSummaryState = (items) => {
  const nextStateMap = { ...noteAiStateMap.value }
  items.forEach((note) => {
    const localState = nextStateMap[note.id]
    if (note.summary) {
      nextStateMap[note.id] = 'ready'
    } else if (localState === 'ready') {
      nextStateMap[note.id] = 'idle'
    } else if (!localState) {
      nextStateMap[note.id] = 'idle'
    }
  })
  noteAiStateMap.value = nextStateMap
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    return fetchNotes()
  }

  loading.value = true
  try {
    const res = await noteApi.search(searchKeyword.value)
    notes.value = res || []
    syncSummaryState(notes.value)
  } catch (error) {
    console.error('Search failed', error)
  } finally {
    loading.value = false
  }
}

const clearSearch = () => {
  searchKeyword.value = ''
  fetchNotes()
}

const openCreateModal = () => {
  currentNote.value = {
    id: null,
    title: '',
    content: '',
    tags: '',
    summary: '',
    createTime: null,
    updateTime: null
  }
  originalNote.value = null
  currentAiState.value = 'idle'
  autoSummaryEnabled.value = true
  showModal.value = true
  saveStatus.value = ''
}

const openEditModal = async (note) => {
  currentNote.value = { ...note }
  originalNote.value = { ...note }
  currentAiState.value = getSummaryState(note)
  autoSummaryEnabled.value = true
  showModal.value = true
  saveStatus.value = ''

  try {
    const detail = await noteApi.getDetail(note.id)
    currentNote.value = { ...detail }
    originalNote.value = { ...detail }
    currentAiState.value = getSummaryState(detail)
  } catch (error) {
    console.error('Failed to fetch note detail', error)
  }
}

const closeModal = () => {
  showModal.value = false
}

const saveNote = async () => {
  saving.value = true
  saveStatus.value = '正在保存笔记...'
  try {
    const payload = { ...currentNote.value }
    const isNew = !payload.id
    const shouldAutoGenerate = autoSummaryEnabled.value && shouldTriggerSummaryAfterSave(payload, originalNote.value)

    if (payload.id) {
      await noteApi.update(payload)
    } else {
      payload.id = await noteApi.create(payload)
    }

    const detail = await noteApi.getDetail(payload.id)
    currentNote.value = { ...detail }
    originalNote.value = { ...detail }
    currentAiState.value = getSummaryState(detail)
    saveStatus.value = '已保存'
    await fetchNotes()

    if (shouldAutoGenerate && hasSummarySource(detail)) {
      await triggerSummary(true)
    } else if (isNew && !hasSummarySource(detail)) {
      saveStatus.value = '已保存。内容为空，未自动生成总结'
    } else if (!shouldAutoGenerate) {
      saveStatus.value = '已保存。自动总结已关闭'
    }
  } catch (error) {
    console.error('Failed to save note', error)
    saveStatus.value = '保存失败'
  } finally {
    saving.value = false
  }
}

const triggerSummary = async (triggeredBySave) => {
  if (!currentNote.value.id) {
    saveStatus.value = '请先保存笔记，再生成总结'
    return
  }

  if (!hasSummarySource(currentNote.value)) {
    saveStatus.value = '内容为空，不建议生成总结'
    return
  }

  currentAiState.value = 'generating'
  noteAiStateMap.value = {
    ...noteAiStateMap.value,
    [currentNote.value.id]: 'generating'
  }
  saveStatus.value = triggeredBySave ? '已保存，正在生成总结...' : '正在生成总结...'

  try {
    const result = await noteApi.generateSummary(currentNote.value.id)
    if (result.status === 'SUCCESS') {
      currentNote.value.summary = result.summary
      currentAiState.value = 'ready'
      noteAiStateMap.value = {
        ...noteAiStateMap.value,
        [currentNote.value.id]: 'ready'
      }
      saveStatus.value = '总结已生成'
      const detail = await noteApi.getDetail(currentNote.value.id)
      currentNote.value = { ...detail }
      originalNote.value = { ...detail }
      await fetchNotes()
      return
    }

    currentAiState.value = currentNote.value.summary ? 'ready' : 'idle'
    noteAiStateMap.value = {
      ...noteAiStateMap.value,
      [currentNote.value.id]: currentAiState.value
    }
    saveStatus.value = result.errorMessage || '总结生成失败'
  } catch (error) {
    const isTimeout = error?.code === 'ECONNABORTED' || String(error?.message || '').toLowerCase().includes('timeout')
    if (isTimeout) {
      currentAiState.value = 'pending'
      noteAiStateMap.value = {
        ...noteAiStateMap.value,
        [currentNote.value.id]: 'pending'
      }
      saveStatus.value = SUMMARY_TIMEOUT_MESSAGE
      await fetchNotes()
      return
    }

    console.error('Failed to generate summary', error)
    currentAiState.value = currentNote.value.summary ? 'ready' : 'idle'
    noteAiStateMap.value = {
      ...noteAiStateMap.value,
      [currentNote.value.id]: currentAiState.value
    }
    saveStatus.value = '总结生成失败'
  }
}

const deleteNote = async (id) => {
  if (!confirm('Delete this note permanently?')) return
  try {
    await noteApi.delete(id)
    const nextMap = { ...noteAiStateMap.value }
    delete nextMap[id]
    noteAiStateMap.value = nextMap
    fetchNotes()
  } catch (error) {
    alert('Failed to delete note')
  }
}

const shouldTriggerSummaryAfterSave = (nextNote, previousNote) => {
  if (!autoSummaryEnabled.value) {
    return false
  }
  if (!previousNote) {
    return hasSummarySource(nextNote)
  }
  return (nextNote.title || '') !== (previousNote.title || '')
    || (nextNote.content || '') !== (previousNote.content || '')
    || (nextNote.tags || '') !== (previousNote.tags || '')
}

const hasSummarySource = (note) => {
  return Boolean((note?.title || '').trim() || (note?.content || '').trim())
}

const getSummaryState = (note) => {
  return noteAiStateMap.value[note.id] || (note.summary ? 'ready' : 'idle')
}

const summaryStateLabel = (note) => {
  const state = note.id === currentNote.value.id ? currentAiState.value : getSummaryState(note)
  if (state === 'generating') return '生成中'
  if (state === 'pending') return '后台生成中'
  if (state === 'ready') return '已生成'
  return '未生成'
}

const summaryStateClass = (note) => {
  const state = note.id === currentNote.value.id ? currentAiState.value : getSummaryState(note)
  return {
    generating: state === 'generating',
    pending: state === 'pending',
    ready: state === 'ready',
    idle: state === 'idle'
  }
}

const getBodyPreview = (content) => {
  if (!content) return 'No content...'
  const plainText = content.replace(/[#*`_\[\]]/g, '')
  return plainText.length > 80 ? `${plainText.substring(0, 80)}...` : plainText
}

const getSummaryPreview = (note, isDetail = false) => {
  const state = note.id === currentNote.value.id ? currentAiState.value : getSummaryState(note)
  if (note.summary) {
    if (isDetail) return note.summary
    return note.summary.length > 120 ? `${note.summary.substring(0, 120)}...` : note.summary
  }
  if (state === 'generating') return 'AI 正在生成总结，请稍候。'
  if (state === 'pending') return SUMMARY_TIMEOUT_MESSAGE
  return '尚未生成 AI 总结。'
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString([], {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchNotes()
})
</script>

<style scoped>
.note-container {
  padding: 40px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 40px;
  gap: 20px;
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
  align-items: center;
  flex-wrap: wrap;
}

.search-bar {
  display: flex;
  background: white;
  border-radius: 12px;
  padding: 4px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
}

.search-bar input {
  border: none;
  background: transparent;
  padding: 8px 16px;
  width: 250px;
  font-size: 14px;
  outline: none;
}

.btn-search {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 600;
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
  background: #0ea5e9;
  color: white;
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.3);
}

.btn-primary:hover {
  background: #0284c7;
  transform: translateY(-2px);
}

.btn-secondary {
  background: #e0f2fe;
  color: #0369a1;
}

.btn-secondary:hover {
  background: #bae6fd;
}

.btn-ghost {
  background: transparent;
  color: #64748b;
}

.glass-panel {
  background: white;
  border-radius: 20px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
  border: 1px solid #f1f5f9;
}

.note-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.note-card {
  padding: 24px;
  position: relative;
  transition: all 0.3s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  min-height: 280px;
}

.note-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  border-color: #e0f2fe;
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.note-title {
  margin: 0;
  font-size: 20px;
  color: #0f172a;
  font-weight: 700;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-preview {
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
  margin: 14px 0 10px 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.summary-preview {
  color: #0f172a;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 18px 0;
  flex-grow: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.summary-preview.placeholder {
  color: #78716c;
}

.summary-chip {
  align-self: flex-start;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.summary-chip.ready {
  color: #0f766e;
  background: #ccfbf1;
}

.summary-chip.generating {
  color: #1d4ed8;
  background: #dbeafe;
}

.summary-chip.pending {
  color: #92400e;
  background: #fef3c7;
}

.summary-chip.idle {
  color: #475569;
  background: #e2e8f0;
}

.note-footer {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  border-top: 1px solid #f1f5f9;
  padding-top: 16px;
  margin-top: auto;
}

.time-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 11px;
  color: #94a3b8;
  font-weight: 600;
}

.tags-container {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.tag {
  font-size: 11px;
  color: #0ea5e9;
  background: #e0f2fe;
  padding: 4px 8px;
  border-radius: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.tag-more {
  font-size: 11px;
  color: #94a3b8;
  padding: 4px;
}

.btn-delete {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #fee2e2;
  color: #ef4444;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s;
}

.note-card:hover .btn-delete {
  opacity: 1;
}

.btn-delete:hover {
  background: #ef4444;
  color: white;
}

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
  animation: fadeIn 0.2s;
}

.note-modal {
  width: 90%;
  max-width: 860px;
  height: 88vh;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.modal-header {
  padding: 20px 30px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: #f8fafc;
  gap: 16px;
}

.modal-header-main {
  flex: 1;
}

.title-input {
  width: 100%;
  border: none;
  background: transparent;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  outline: none;
  margin-bottom: 8px;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 12px;
  color: #64748b;
}

.btn-close {
  background: transparent;
  border: none;
  font-size: 16px;
  font-weight: 700;
  color: #64748b;
  cursor: pointer;
}

.modal-body {
  padding: 30px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
}

.editor-toolbar {
  display: flex;
  gap: 16px;
}

.tags-input {
  width: 100%;
  padding: 10px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.toggle-row {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #334155;
  font-weight: 600;
}

.markdown-editor {
  flex-grow: 1;
  min-height: 300px;
  padding: 20px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.6;
  font-family: Consolas, 'Liberation Mono', Menlo, monospace;
  resize: none;
  outline: none;
}

.markdown-editor:focus,
.tags-input:focus {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.1);
}

.ai-summary {
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
}

.ai-summary.ready {
  background: linear-gradient(to right, #ecfeff, #e0f2fe);
  border-color: #67e8f9;
}

.ai-summary.generating {
  background: linear-gradient(to right, #eff6ff, #dbeafe);
  border-color: #93c5fd;
}

.ai-summary.pending {
  background: linear-gradient(to right, #fffbeb, #fef3c7);
  border-color: #fbbf24;
}

.ai-summary-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 8px;
}

.ai-summary h4 {
  margin: 0;
  color: #0f172a;
}

.ai-summary p {
  margin: 0;
  color: #334155;
  font-size: 14px;
  line-height: 1.7;
}

.modal-actions {
  padding: 20px 30px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  background: #f8fafc;
}

.save-status {
  flex: 1;
  color: #64748b;
  font-size: 14px;
}

.empty-state,
.loading-state {
  text-align: center;
  padding: 80px 40px;
}

.empty-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 24px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  background: #e0f2fe;
  color: #0284c7;
}

.empty-state h3 {
  font-size: 24px;
  color: #1e293b;
  margin: 0 0 12px 0;
}

.empty-state p {
  color: #64748b;
  margin: 0;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #0ea5e9;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  100% { transform: rotate(360deg); }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (max-width: 768px) {
  .note-container {
    padding: 20px;
  }

  .header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .search-bar input {
    width: 100%;
  }

  .note-footer,
  .ai-summary-header,
  .modal-actions,
  .note-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
