import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi, registerApi, type LoginDTO, type RegisterDTO, type LoginVO } from '@/api/auth'
import { ElMessage } from 'element-plus'

export interface UserInfo {
  id: number
  username: string
  nickname?: string
  avatar?: string
  email?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(
    JSON.parse(localStorage.getItem('userInfo') || 'null')
  )

  // 更新全局用户信息并同步存储到 localStorage
  const setUserInfo = (info: Partial<UserInfo>) => {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...info }
    } else {
      userInfo.value = info as UserInfo
    }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  const login = async (dto: LoginDTO) => {
    const res = await loginApi(dto)
    token.value = res.token
    userInfo.value = {
      id: res.id,
      username: res.username,
      nickname: res.nickname,
      avatar: res.avatar
    }
    localStorage.setItem('token', res.token)
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    ElMessage.success(`欢迎回来，${res.nickname || res.username}！`)
  }

  const register = async (dto: RegisterDTO) => {
    await registerApi(dto)
    ElMessage.success('注册成功，请登录！')
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    ElMessage.info('已退出登录')
  }

  return { token, userInfo, setUserInfo, login, register, logout }
})