<template>
  <div class="auth-page-container">
    <!-- 背景艺术装饰球/微光效果 -->
    <div class="bg-glow glow-1"></div>
    <div class="bg-glow glow-2"></div>

    <div class="auth-card">
      <div class="header-section">
        <div class="logo-icon">🏛️</div>
        <h2 class="title">3D 虚拟艺术展厅</h2>
        <p class="subtitle">探索沉浸式数字艺术空间</p>
      </div>

      <el-tabs v-model="activeTab" class="auth-tabs" stretch>
        <!-- 登录面板 -->
        <el-tab-pane label="账号登录" name="login">
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-position="top"
            size="large"
            @keyup.enter="handleLogin"
          >
            <el-form-item label="账号" prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                show-password
                prefix-icon="Lock"
              />
            </el-form-item>
            <el-button
              type="primary"
              class="submit-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form>
        </el-tab-pane>

        <!-- 注册面板 -->
        <el-tab-pane label="新用户注册" name="register">
          <el-form
            ref="regFormRef"
            :model="regForm"
            :rules="regRules"
            label-position="top"
            size="large"
            @keyup.enter="handleRegister"
          >
            <el-form-item label="账号" prop="username">
              <el-input v-model="regForm.username" placeholder="建议 4-20 位字符" prefix-icon="User" />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="regForm.nickname" placeholder="展厅内显示的称呼" prefix-icon="Postcard" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="regForm.password"
                type="password"
                placeholder="密码不能少于 6 位"
                show-password
                prefix-icon="Lock"
              />
            </el-form-item>
            <el-button
              type="success"
              class="submit-btn"
              :loading="loading"
              @click="handleRegister"
            >
              注 册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <!-- 底部返回首页按钮 -->
      <div class="back-home">
        <span @click="router.push('/')">← 暂不登录，以游客身份进入展厅</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'login' | 'register'>('login')
const loading = ref(false)

const loginFormRef = ref<FormInstance>()
const regFormRef = ref<FormInstance>()

const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '', nickname: '' })

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const regRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码不能少于 6 位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.login(loginForm)
        router.push('/') // 登录成功后跳转回 3D 展厅
      } finally {
        loading.value = false
      }
    }
  })
}

const handleRegister = async () => {
  if (!regFormRef.value) return
  await regFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.register(regForm)
        activeTab.value = 'login'
        loginForm.username = regForm.username
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.auth-page-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #0f172a;
  overflow: hidden;
}

/* 艺术感炫光底纹 */
.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.4;
}
.glow-1 {
  width: 400px;
  height: 400px;
  background: #ea580c;
  top: -100px;
  left: -100px;
}
.glow-2 {
  width: 450px;
  height: 450px;
  background: #3b82f6;
  bottom: -150px;
  right: -100px;
}

/* 毛玻璃登录卡片 */
.auth-card {
  position: relative;
  z-index: 10;
  width: 420px;
  padding: 40px 36px 28px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

.header-section {
  text-align: center;
  margin-bottom: 24px;
}

.logo-icon {
  font-size: 40px;
  margin-bottom: 6px;
}

.title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #64748b;
}

.submit-btn {
  width: 100%;
  margin-top: 10px;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
}

.back-home {
  margin-top: 24px;
  text-align: center;
}

.back-home span {
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  transition: color 0.2s;
}

.back-home span:hover {
  color: #ea580c;
}
</style>