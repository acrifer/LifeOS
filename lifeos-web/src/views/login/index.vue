<template>
  <div class="login-container">
    <div class="glass-panel">
      <!-- Decorator blobs -->
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>

      <div class="login-box">
        <div class="logo-area">
          <div class="logo-icon">🚀</div>
          <h2>LifeOS</h2>
          <p class="subtitle">{{ isLogin ? 'Welcome back' : 'Create an account' }}</p>
        </div>

        <form @submit.prevent="handleSubmit" class="form-area">
          <div class="input-group">
            <input 
              v-model="form.username" 
              type="text" 
              placeholder="Username" 
              required 
            />
          </div>
          
          <div class="input-group" v-if="!isLogin">
            <input 
              v-model="form.email" 
              type="email" 
              placeholder="Email" 
            />
          </div>

          <div class="input-group">
            <input 
              v-model="form.password" 
              type="password" 
              placeholder="Password" 
              required 
            />
          </div>

          <div class="error-msg" v-if="errorMessage">{{ errorMessage }}</div>

          <button type="submit" class="submit-btn" :disabled="loading">
            {{ loading ? 'Loading...' : (isLogin ? 'Sign In' : 'Sign Up') }}
          </button>
        </form>

        <div class="toggle-mode">
          <span @click="toggleMode">
            {{ isLogin ? "Don't have an account? Sign up" : 'Already have an account? Sign in' }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'

const router = useRouter()

const isLogin = ref(true)
const loading = ref(false)
const errorMessage = ref('')

const form = reactive({
  username: '',
  email: '',
  password: ''
})

const toggleMode = () => {
  isLogin.value = !isLogin.value
  errorMessage.value = ''
  form.username = ''
  form.password = ''
  form.email = ''
}

const handleSubmit = async () => {
  if (!form.username || !form.password) return
  
  loading.value = true
  errorMessage.value = ''
  
  try {
    if (isLogin.value) {
      const token = await userApi.login({
        username: form.username,
        password: form.password
      })
      
      // Store token
      localStorage.setItem('lifeos_token', token)
      
      // Navigate to dashboard
      router.push('/dashboard')
    } else {
      await userApi.register({
        username: form.username,
        password: form.password,
        email: form.email
      })
      
      // Auto switch to login
      isLogin.value = true
      errorMessage.value = 'Registration successful! Please sign in.'
    }
  } catch (error) {
    errorMessage.value = error.message || 'An error occurred. Please try again.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* Base container taking full screen */
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  /* Premium dark gradient background */
  background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #312e81 100%);
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
  color: white;
  overflow: hidden;
  position: relative;
}

/* Glassmorphism Panel */
.glass-panel {
  position: relative;
  width: 100%;
  max-width: 420px;
  padding: 40px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5), 
              inset 0 1px 1px rgba(255, 255, 255, 0.1);
  z-index: 10;
}

/* Ambient dynamic blobs behind the panel */
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  z-index: -1;
  opacity: 0.6;
  animation: float 10s infinite alternate ease-in-out;
}

.blob-1 {
  top: -20%;
  left: -20%;
  width: 250px;
  height: 250px;
  background: #6366f1; /* Indigo */
}

.blob-2 {
  bottom: -20%;
  right: -20%;
  width: 300px;
  height: 300px;
  background: #ec4899; /* Pink */
  animation-delay: -5s;
}

@keyframes float {
  0% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(20px, -30px) scale(1.1); }
  100% { transform: translate(-20px, 20px) scale(0.9); }
}

/* Content */
.logo-area {
  text-align: center;
  margin-bottom: 35px;
}

.logo-icon {
  font-size: 48px;
  margin-bottom: 10px;
  filter: drop-shadow(0 0 10px rgba(255, 255, 255, 0.3));
}

.logo-area h2 {
  font-size: 28px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(to right, #ffffff, #a5b4fc);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 1px;
}

.subtitle {
  color: #94a3b8;
  font-size: 14px;
  margin-top: 8px;
}

/* Form Styles */
.form-area {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.input-group {
  position: relative;
}

.input-group input {
  width: 100%;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(0, 0, 0, 0.2);
  color: white;
  font-size: 15px;
  transition: all 0.3s ease;
  outline: none;
  box-sizing: border-box;
}

.input-group input::placeholder {
  color: #64748b;
}

.input-group input:focus {
  border-color: #6366f1;
  background: rgba(0, 0, 0, 0.4);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.error-msg {
  color: #ef4444;
  font-size: 13px;
  text-align: center;
  background: rgba(239, 68, 68, 0.1);
  padding: 8px;
  border-radius: 8px;
}

.submit-btn {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  border: none;
  padding: 14px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(79, 70, 229, 0.3);
  margin-top: 5px;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.4);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.toggle-mode {
  text-align: center;
  margin-top: 25px;
  font-size: 14px;
  color: #94a3b8;
}

.toggle-mode span {
  cursor: pointer;
  transition: color 0.3s;
}

.toggle-mode span:hover {
  color: #a5b4fc;
  text-decoration: underline;
}
</style>
