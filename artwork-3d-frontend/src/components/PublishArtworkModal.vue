<template>
  <el-dialog
    v-model="visible"
    title="🎨 发布作品到 3D 展厅"
    width="500px"
    destroy-on-close
    @close="handleClose"
  >
    <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
      <el-form-item label="作品名称" prop="title">
        <el-input v-model="form.title" placeholder="请输入作品标题" />
      </el-form-item>

      <el-form-item label="作者" prop="author">
        <el-input v-model="form.author" placeholder="请输入作者名字" />
      </el-form-item>

      <el-form-item label="创作年份" prop="year">
        <el-input v-model="form.year" placeholder="例如：2026" />
      </el-form-item>

      <el-form-item label="作品简介" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="分享这幅作品背后的故事..."
        />
      </el-form-item>

      <el-form-item label="画作图片" prop="url">
        <!-- 新增 :headers="uploadHeaders" -->
        <el-upload
          class="image-uploader"
          action="/api/artworks/upload"
          :headers="uploadHeaders"
          :show-file-list="false"
          :on-success="handleUploadSuccess"
          :before-upload="beforeUpload"
        >
          <img v-if="form.url" :src="form.url" class="preview-img" />
          <div v-else class="upload-placeholder">
            <span class="upload-icon">+</span>
            <span>点击上传图片</span>
          </div>
        </el-upload>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确认发布
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { useUserStore } from '@/stores/user' // 1. 引入 Pinia 用户 Store

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const userStore = useUserStore() // 2. 获取用户 Store 实例

const visible = ref(false)
const submitting = ref(false)
const formRef = ref(null)

// 3. 计算上传请求头，动态携带 Bearer Token
const uploadHeaders = computed(() => ({
  Authorization: userStore.token ? `Bearer ${userStore.token}` : ''
}))

// 监听父组件传进来的显示状态
watch(() => props.modelValue, (val) => {
  if (val && !userStore.token) {
    ElMessage.warning('请先登录后再发布作品！')
    emit('update:modelValue', false) // 强制关闭弹窗
    return
  }
  visible.value = val
})

const form = reactive({
  title: '',
  author: '',
  year: '',
  description: '',
  url: ''
})

const rules = {
  title: [{ required: true, message: '作品名称不能为空', trigger: 'blur' }],
  url: [{ required: true, message: '请先上传作品图片', trigger: 'change' }]
}

// 图片上传前的格式校验
const beforeUpload = (file) => {
  if (!userStore.token) {
    ElMessage.warning('请先登录后再上传图片！')
    return false
  }
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) ElMessage.error('只能上传图片文件！')
  if (!isLt5M) ElMessage.error('图片大小不能超过 5MB！')
  return isImage && isLt5M
}

// 图片上传成功回调
const handleUploadSuccess = (response) => {
  const rawPath = typeof response === 'string' ? response : response.data

  // 拼接后端端口服务地址
  const fullImageUrl = rawPath.startsWith('http') 
    ? rawPath 
    : `http://localhost:8080${rawPath}`

  // 修复：reactive 变量直接赋值，去掉 .value
  form.url = fullImageUrl
}

// 提交发布表单
const handleSubmit = async () => {
  if (!userStore.token) {
    ElMessage.warning('请先登录后再发布作品！')
    return
  }
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await axios.post('/api/artworks/publish', form, {
        headers: {
          Authorization: `Bearer ${userStore.token}`
        }
      })
      ElMessage.success('作品发布成功！已在 3D 展厅中挂载')
      emit('success')
      handleClose()
    } catch (err) {
      ElMessage.error('发布失败：' + (err.response?.data?.message || err.message))
    } finally {
      submitting.value = false
    }
  })
}

const handleClose = () => {
  visible.value = false
  emit('update:modelValue', false)
  // 重置表单
  if (formRef.value) formRef.value.resetFields()
  form.url = ''
}
</script>

<style scoped>
.image-uploader {
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  overflow: hidden;
  width: 160px;
  height: 160px;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: border-color 0.3s;
}
.image-uploader:hover {
  border-color: #409eff;
}
.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #8c939d;
  font-size: 13px;
}
.upload-icon {
  font-size: 28px;
  margin-bottom: 4px;
}
</style>