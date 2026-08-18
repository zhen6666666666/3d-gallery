<template>
  <el-dialog
    v-model="visible"
    :title="activeTab === 'login' ? '🏛️ 登录展厅账号' : '✨ 注册新账号'"
    width="400px"
    center
    destroy-on-close
    class="auth-dialog"
    @closed="resetForm"
  >
    <el-tabs v-model="activeTab" class="auth-tabs" stretch>
      <!-- 登录 Tab -->
      <el-tab-pane label="登录" name="login">
        <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top">
          <el-form-item label="账号" prop="username">
            <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password prefix-icon="Lock" />
          </el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">
            立即登录
          </el-button>
        </el-form>
      </el-tab-pane>

      <!-- 注册 Tab -->
      <el-tab-pane label="注册" name="register">
        <el-form ref="regFormRef" :model="regForm" :rules="regRules" label-position="top">
          <el-form-item label="账号" prop="username">
            <el-input v-model="regForm.username" placeholder="建议 4-16 位字母/数字" prefix-icon="User" />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="regForm.nickname" placeholder="展厅中展示的称呼" prefix-icon="Postcard" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="regForm.password" type="password" placeholder="建议 6 位以上" show-password prefix-icon="Lock" />
          </el-form-item>
          <el-button type="success" class="submit-btn" :loading="loading" @click="handleRegister">
            提交注册
          </el-button>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits(['update:modelValue', 'login-success'])

const userStore = useUserStore()
const activeTab = ref<'login' | 'register'>('login')
const loading = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

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
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
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
        visible.value = false
        emit('login-success')
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

const resetForm = () => {
  loginForm.username = ''
  loginForm.password = ''
  regForm.username = ''
  regForm.password = ''
  regForm.nickname = ''
}
</script>

<style scoped>
.submit-btn {
  width: 100%;
  margin-top: 12px;
  padding: 12px 0;
  font-weight: 600;
  border-radius: 8px;
}
</style>