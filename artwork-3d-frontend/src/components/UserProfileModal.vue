<template>
  <el-dialog
    v-model="visible"
    title="👤 个人主页"
    width="680px"
    destroy-on-close
    class="profile-dialog"
    @close="handleClose"
  >
    <!-- 头部个人简讯 -->
    <div class="user-header">
      <el-avatar :size="64" :src="profile.avatar" class="header-avatar">
        {{ profile.username?.charAt(0).toUpperCase() }}
      </el-avatar>
      <div class="header-meta">
        <h3>{{ profile.nickname || profile.username }}</h3>
        <p class="user-tag">@{{ profile.username }}</p>
      </div>
    </div>

    <!-- 选项卡切换 -->
    <el-tabs v-model="activeTab" @tab-change="loadTabData" class="profile-tabs">
      <el-tab-pane label="🎨 我的发布" name="published">
        <div v-loading="loading" class="artwork-grid">
          <div v-for="item in publishedList" :key="item.id" class="artwork-card">
            <img :src="item.url" class="card-img" />
            <div class="card-title">{{ item.title }}</div>
          </div>
          <el-empty v-if="!publishedList.length && !loading" description="暂无发布作品" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="❤️ 我的点赞" name="likes">
        <div v-loading="loading" class="artwork-grid">
          <div v-for="item in likedList" :key="item.id" class="artwork-card">
            <img :src="item.url" class="card-img" />
            <div class="card-title">{{ item.title }}</div>
          </div>
          <el-empty v-if="!likedList.length && !loading" description="暂无点赞作品" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="⭐ 我的收藏" name="favorites">
        <div v-loading="loading" class="artwork-grid">
          <div v-for="item in favoritedList" :key="item.id" class="artwork-card">
            <img :src="item.url" class="card-img" />
            <div class="card-title">{{ item.title }}</div>
          </div>
          <el-empty v-if="!favoritedList.length && !loading" description="暂无收藏作品" />
        </div>
      </el-tab-pane>

      <!-- 修改资料选项卡 -->
      <el-tab-pane label="⚙️ 修改资料" name="edit">
        <el-form :model="editForm" label-width="80px" class="edit-form">
          <el-form-item label="头像上传">
            <el-upload
              class="avatar-uploader"
              action="/api/artworks/upload"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
            >
              <img v-if="editForm.avatar" :src="editForm.avatar" class="avatar-preview" />
              <div v-else class="avatar-upload-icon">+</div>
            </el-upload>
          </el-form-item>

          <el-form-item label="用户名">
            <el-input v-model="editForm.username" placeholder="请输入用户名" />
          </el-form-item>

          <el-form-item label="昵称">
            <el-input v-model="editForm.nickname" placeholder="请输入个性昵称" />
          </el-form-item>

          <el-form-item label="邮箱">
            <el-input v-model="editForm.email" placeholder="请输入邮箱" />
          </el-form-item>

          <el-form-item label="新密码">
            <el-input
              v-model="editForm.password"
              type="password"
              show-password
              placeholder="留空表示不修改密码"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSaveProfile">保存修改</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const userStore = useUserStore()
const visible = ref(false)
const activeTab = ref('published')
const loading = ref(false)
const saving = ref(false)

const profile = ref({})
const publishedList = ref([])
const likedList = ref([])
const favoritedList = ref([])

const editForm = reactive({
  username: '',
  nickname: '',
  avatar: '',
  email: '',
  password: ''
})

const authHeader = computed(() => ({
  Authorization: userStore.token ? `Bearer ${userStore.token}` : ''
}))

const uploadHeaders = computed(() => authHeader.value)

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    fetchProfile()
    loadTabData(activeTab.value)
  }
})

// 获取个人信息
const fetchProfile = async () => {
  try {
    const res = await axios.get('/api/user/profile', { headers: authHeader.value })
    profile.value = res.data
    editForm.username = res.data.username || ''
    editForm.nickname = res.data.nickname || ''
    editForm.avatar = res.data.avatar || ''
    editForm.email = res.data.email || ''
    editForm.password = ''
  } catch (err) {
    ElMessage.error('获取个人信息失败')
  }
}

// 切换选项卡加载数据
const loadTabData = async (tab) => {
  loading.value = true
  try {
    if (tab === 'published') {
      const res = await axios.get('/api/artworks/my/published', { headers: authHeader.value })
      publishedList.value = res.data
    } else if (tab === 'likes') {
      const res = await axios.get('/api/artworks/my/likes', { headers: authHeader.value })
      likedList.value = res.data
    } else if (tab === 'favorites') {
      const res = await axios.get('/api/artworks/my/favorites', { headers: authHeader.value })
      favoritedList.value = res.data
    }
  } catch (err) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

// 头像上传成功处理
const handleAvatarSuccess = (response) => {
  const rawPath = typeof response === 'string' ? response : response.data
  editForm.avatar = rawPath.startsWith('http') ? rawPath : `http://localhost:8080${rawPath}`
}

// 保存个人资料修改
const handleSaveProfile = async () => {
  saving.value = true
  try {
    await axios.put('/api/user/profile', editForm, { headers: authHeader.value })
    ElMessage.success('更新成功！')

    // 更新 Pinia 全局状态，驱动暂停面板 (ThreeCanvas.vue) 实时同步显示新头像与昵称
    userStore.setUserInfo({
      username: editForm.username,
      nickname: editForm.nickname,
      avatar: editForm.avatar,
      email: editForm.email
    })

    fetchProfile()
  } catch (err) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleClose = () => {
  visible.value = false
  emit('update:modelValue', false)
}
</script>

<style scoped>
.user-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.header-meta h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}
.user-tag {
  margin: 4px 0 0 0;
  color: #8c8c8c;
  font-size: 13px;
}
.profile-tabs {
  margin-top: 16px;
}
.artwork-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  min-height: 200px;
}
.artwork-card {
  border-radius: 8px;
  overflow: hidden;
  background: #f9f9f9;
  border: 1px solid #eee;
}
.card-img {
  width: 100%;
  height: 120px;
  object-fit: cover;
}
.card-title {
  padding: 6px 8px;
  font-size: 12px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.edit-form {
  margin-top: 12px;
  max-width: 400px;
}
.avatar-uploader {
  width: 80px;
  height: 80px;
  border: 1px dashed #d9d9d9;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
}
.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-upload-icon {
  font-size: 24px;
  color: #8c8c8c;
}
</style>