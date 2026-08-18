<template>
  <el-drawer
    v-model="visible"
    title="作品详情"
    direction="rtl"
    size="450px"
    :modal="false"
    :lock-scroll="false"
    :before-close="handleClose"
  >
    <div v-if="artwork" class="artwork-detail-container">
      <!-- 1. 作品封面（改用容器包裹，保持真实原图比例不被裁剪） -->
      <div class="artwork-cover-wrapper">
        <img 
          :src="artwork.imageUrl || artwork.url" 
          :alt="artwork.title" 
          class="artwork-cover" 
        />
      </div>

      <!-- 2. 作品基本信息 -->
      <div class="artwork-info">
        <h2 class="artwork-title">{{ artwork.title }}</h2>
        <p class="author-year">
          作者：{{ artwork.author || artwork.artist || '未知' }}
          <template v-if="artwork.year">（{{ artwork.year }}）</template>
        </p>
        <p class="description">{{ artwork.description || '暂无作品简介' }}</p>
      </div>

      <el-divider />

      <!-- 3. 点赞与收藏交互栏 -->
      <div class="action-bar">
        <el-button
          :type="interaction.isLiked ? 'danger' : 'default'"
          :icon="interaction.isLiked ? 'StarFilled' : 'Star'"
          round
          @click="handleToggleLike"
        >
          点赞 ({{ interaction.likeCount }})
        </el-button>

        <el-button
          :type="interaction.isFavorited ? 'warning' : 'default'"
          :icon="interaction.isFavorited ? 'CollectionTag' : 'Collection'"
          round
          @click="handleToggleFavorite"
        >
          收藏 ({{ interaction.favoriteCount }})
        </el-button>
      </div>

      <el-divider />

      <!-- 4. 评论区 -->
      <div class="comments-section">
        <h3>评论 ({{ comments.length }})</h3>

        <!-- 发表评论输入框 -->
        <div class="comment-input-box">
          <el-input
            v-model="newComment"
            type="textarea"
            :rows="2"
            placeholder="写下你的感受..."
            maxlength="200"
            show-word-limit
          />
          <el-button
            type="primary"
            class="submit-btn"
            :loading="submitting"
            @click="handlePublishComment"
          >
            发送评论
          </el-button>
        </div>

        <!-- 评论列表 -->
        <div class="comment-list">
          <div v-for="item in comments" :key="item.id" class="comment-item">
            <el-avatar :src="item.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" :size="36" />
            <div class="comment-content-box">
              <div class="comment-header">
                <span class="nickname">{{ item.nickname || '匿名用户' }}</span>
                <span class="time">{{ formatDate(item.createTime) }}</span>
              </div>
              <p class="content">{{ item.content }}</p>
            </div>
            <el-button
            v-if="item.userId === currentUserId"
            type="danger"
            link
            :icon="Delete"
            class="delete-btn"
            @click="handleDeleteComment(item.id)"
          >
            删除
          </el-button>
          </div>
          <el-empty v-if="comments.length === 0" description="暂无评论，快来抢沙发吧~" />
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled, Collection, CollectionTag, Delete } from '@element-plus/icons-vue'
import {
  getInteractionStatus,
  toggleLike,
  toggleFavorite,
  getComments,
  addComment,
  deleteComment
} from '@/api/artwork'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  artwork: { type: Object, default: () => null },
  currentUserId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:modelValue', 'refresh'])

const visible = ref(false)
const newComment = ref('')
const submitting = ref(false)
const comments = ref([])
const interaction = ref({
  isLiked: false,
  isFavorited: false,
  likeCount: 0,
  favoriteCount: 0
})

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.artwork?.id) {
    loadData(props.artwork.id)
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const loadData = async (artworkId) => {
  try {
    const [statusRes, commentsRes] = await Promise.all([
      getInteractionStatus(artworkId),
      getComments(artworkId)
    ])
    interaction.value = statusRes.data
    comments.value = commentsRes.data
  } catch (error) {
    ElMessage.error('获取详情数据失败')
  }
}

const handleToggleLike = async () => {
  try {
    const res = await toggleLike(props.artwork.id)
    interaction.value.isLiked = res.data.isLiked
    interaction.value.likeCount += res.data.isLiked ? 1 : -1
    ElMessage.success(res.data.isLiked ? '点赞成功' : '已取消点赞')
  } catch (error) {
    ElMessage.error('操作失败，请先登录')
  }
}

const handleToggleFavorite = async () => {
  try {
    const res = await toggleFavorite(props.artwork.id)
    interaction.value.isFavorited = res.data.isFavorited
    interaction.value.favoriteCount += res.data.isFavorited ? 1 : -1
    ElMessage.success(res.data.isFavorited ? '收藏成功' : '已取消收藏')
  } catch (error) {
    ElMessage.error('操作失败，请先登录')
  }
}

const handlePublishComment = async () => {
  if (!newComment.value.trim()) {
    return ElMessage.warning('评论内容不能为空')
  }
  submitting.value = true
  try {
    await addComment({
      artworkId: props.artwork.id,
      content: newComment.value.trim()
    })
    ElMessage.success('评论成功')
    newComment.value = ''
    const res = await getComments(props.artwork.id)
    comments.value = res.data
  } catch (error) {
    ElMessage.error('发表评论失败，请确认是否已登录')
  } finally {
    submitting.value = false
  }
}

const handleDeleteComment = (commentId) => {
  ElMessageBox.confirm('确定要删除该条评论吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteComment(commentId)
      ElMessage.success('删除成功')
      comments.value = comments.value.filter(item => item.id !== commentId)
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

const handleClose = () => {
  visible.value = false
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}
</script>

<style scoped>
.artwork-detail-container {
  display: flex;
  flex-direction: column;
  padding: 0 8px;
}

/* 封面展示外层容器：固定底色与最大高度 */
.artwork-cover-wrapper {
  width: 100%;
  max-height: 380px;
  background-color: #f1f5f9;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  margin-bottom: 16px;
}

/* 保证原图缩放不裁剪 */
.artwork-cover {
  max-width: 100%;
  max-height: 380px;
  width: auto;
  height: auto;
  object-fit: contain;
  display: block;
}

.artwork-title {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 8px 0;
}

.author-year {
  color: #64748b;
  font-size: 14px;
  margin: 0 0 12px 0;
}

.description {
  color: #334155;
  line-height: 1.6;
  font-size: 14px;
  margin: 0;
}

.action-bar {
  display: flex;
  justify-content: space-around;
  padding: 4px 0;
}

.comment-input-box {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.submit-btn {
  width: 100px;
}

.comment-list {
  margin-top: 20px;
}

.comment-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
  position: relative;
}

.comment-content-box {
  flex: 1;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.nickname {
  font-weight: bold;
  font-size: 13px;
  color: #333;
}

.time {
  font-size: 12px;
  color: #999;
}

.content {
  font-size: 14px;
  color: #444;
  margin: 0;
  word-break: break-all;
}

.delete-btn {
  position: absolute;
  right: 0;
  bottom: 8px;
}
</style>