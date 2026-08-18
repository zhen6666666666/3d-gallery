<template>
  <div class="gallery-container">
    <!-- 1. 3D 展厅画布 -->
    <ThreeCanvas
      ref="threeCanvasRef"
      :drawer-open="drawerVisible"
      @select-artwork="handleSelectArtwork"
    />

    <!-- 2. 右上角悬浮“发布作品”按钮 -->
    <div class="publish-btn-wrapper">
      <el-button type="primary" size="large" round @click="publishModalVisible = true">
        ➕ 发布作品
      </el-button>
    </div>

    <!-- 3. 作品详情抽屉 -->
    <ArtworkDetailDrawer
      v-model="drawerVisible"
      :artwork="selectedArtwork"
      :current-user-id="currentUserId"
    />

    <!-- 4. 发布作品弹窗组件 -->
    <PublishArtworkModal
      v-model="publishModalVisible"
      @success="handlePublishSuccess"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ThreeCanvas from './ThreeCanvas.vue'
import ArtworkDetailDrawer from './ArtworkDetailDrawer.vue'
import PublishArtworkModal from './PublishArtworkModal.vue'

const threeCanvasRef = ref(null)
const drawerVisible = ref(false)
const publishModalVisible = ref(false)
const selectedArtwork = ref(null)
const currentUserId = ref(1) // 当前登录用户 ID

const handleSelectArtwork = (artworkData) => {
  if (!artworkData) return
  selectedArtwork.value = artworkData
  drawerVisible.value = true
}

// 作品发布成功后，刷新 3D 画布中的画作数据
const handlePublishSuccess = () => {
  if (threeCanvasRef.value && typeof threeCanvasRef.value.reloadArtworks === 'function') {
    threeCanvasRef.value.reloadArtworks()
  } else {
    window.location.reload() // 兜底全页刷新
  }
}

onMounted(() => {
  currentUserId.value = 1
})
</script>

<style scoped>
.gallery-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background-color: #1a1a1a;
}

/* 顶部悬浮按钮样式 */
.publish-btn-wrapper {
  position: absolute;
  top: 24px;
  right: 24px;
  z-index: 10;
}
</style>