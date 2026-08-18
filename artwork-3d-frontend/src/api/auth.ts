import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000
})

// 请求拦截器：注入 JWT
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config;
}, error => Promise.reject(error))

// 响应拦截器：统一报错提醒
request.interceptors.response.use(
  res => res.data,
  error => {
    const msg = error.response?.data || '请求失败，请稍后再试'
    ElMessage.error(typeof msg === 'string' ? msg : '网络异常')
    return Promise.reject(error)
  }
)

export interface LoginDTO {
  username: string
  password: string
}

export interface RegisterDTO {
  username: string
  password: string
  nickname?: string
}

export interface LoginVO {
  token: string
  id: number
  username: string
  nickname: string
  avatar: string
}

export const loginApi = (data: LoginDTO): Promise<LoginVO> => request.post('/auth/login', data)
export const registerApi = (data: RegisterDTO) => request.post('/auth/register', data)