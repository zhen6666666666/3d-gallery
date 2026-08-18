<template>
  <div class="gallery-wrapper">

    <!-- 3D WebGL 渲染容器 -->
    <div ref="canvasContainer" class="canvas-container" @click="enterGallery"></div>

    <!-- 加载与首次进入提示屏 -->
    <div v-if="loading || !hasEntered" class="enter-mask" @click="enterGallery">
      <template v-if="loading">
        <el-progress type="circle" :percentage="loadProgress" :color="colors" />
        <p class="loading-text">3D 白色展厅构建中...</p>
      </template>
      <template v-else>
        <div class="start-btn-box">
          <div class="pulse-icon">🏛️</div>
          <h2>点击任意位置进入 3D 展厅</h2>
          <p>[W/A/S/D] 移动 | [M] 平面地图 | [F] 发布作品 | 点击画作查看详情</p>
        </div>
      </template>
    </div>

    <!-- 视角中心准星 (仅漫游时显示) -->
    <div v-if="isLocked && !loading" class="crosshair" :class="{ active: isHoveredArt }">+</div>

    <!-- 按下 ESC 退出漫游后显示操作指南弹窗 -->
    <div
      v-if="!isLocked && hasEntered && !dialogVisible && !showMapModal && !publishModalVisible && !loading"
      class="pause-modal-mask"
      @click="enterGallery"
    >
      <div class="pause-modal-card" @click.stop>
        <div class="pause-icon">⏸️</div>
        <h3>漫游已暂停</h3>
        <p class="pause-subtitle">按 ESC 键已释放鼠标控制，点击按钮或背景恢复漫游</p>

        <!-- 登录 / 注册 / 用户状态 区域 -->
        <div class="user-account-box">
          <template v-if="userStore.token">
            <div class="user-info-inner">
              <div class="user-profile" @click="openProfileModal">
                <el-avatar :size="32" :src="userStore.userInfo?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
                <span class="nickname">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              </div>
              <el-button type="danger" link size="small" @click="userStore.logout">退出登录</el-button>
            </div>
          </template>

          <template v-else>
            <el-button type="primary" class="login-btn" round @click="router.push('/login')">
              🔑 登录 / 注册
            </el-button>
          </template>
        </div>

        <!-- 操作说明列表 -->
        <div class="tips-list">
          <div class="tip-item">
            <span class="key-badge">W A S D</span>
            <span class="tip-desc">控制前后左右移动</span>
          </div>
          <div class="tip-item">
            <span class="key-badge">M</span>
            <span class="tip-desc">打开 2D 展厅平面导航地图</span>
          </div>
          <div class="tip-item">
            <span class="key-badge">F</span>
            <span class="tip-desc">发布新作品到 3D 展厅</span>
          </div>
          <div class="tip-item">
            <span class="key-badge">鼠标左键</span>
            <span class="tip-desc">点击墙上画作查看高清详情</span>
          </div>
        </div>

        <!-- 按钮区域：发布作品与继续漫游 -->
        <button class="publish-btn" @click="openPublishModal">
          🎨 发布作品 [F]
        </button>

        <button class="resume-btn" @click="enterGallery">
          ▶ 继续漫游
        </button>
      </div>
    </div>

    <!-- 按 M 弹出的平面地图导航 Modal -->
    <div v-if="showMapModal" class="map-modal-mask" @click.self="closeMapModal">
      <div class="map-modal-content">
        <div class="map-header">
          <h3>🏛️ 展厅纵深平面地图</h3>
          <span class="map-close-btn" @click="closeMapModal">✕</span>
        </div>
        
        <div class="map-toolbar">
          <p class="map-subtitle">点击任意作品，直接传送至画作正前方</p>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索作品名称、作者、简介..."
            clearable
            :prefix-icon="Search"
            class="map-search-input"
          />
        </div>

        <div class="corridor-map">
          <div class="wall-line left-wall"></div>
          <div class="corridor-path">
            <span class="path-label">← 展厅主通道 →</span>
            <div
              class="player-dot"
              :style="{ top: `${playerMapPosPercent.z}%`, left: `${playerMapPosPercent.x}%` }"
              title="当前位置"
            >
              <div class="player-arrow" :style="{ transform: `rotate(${playerMapAngle}deg)` }">▲</div>
            </div>
          </div>
          <div class="wall-line right-wall"></div>

          <div
            v-for="art in filteredArtworksList"
            :key="art.id"
            class="map-artwork-node"
            :class="art.position[0] < 0 ? 'node-left' : 'node-right'"
            :style="{ top: `${getArtZPercent(art.position[2])}%` }"
            @click="teleportFromMap(art)"
          >
            <div class="node-badge">#{{ art.id }}</div>
            <div class="node-info">
              <div class="node-title" :title="art.title">{{ art.title }}</div>
              <div class="node-author">{{ art.artist }}</div>
            </div>
          </div>

          <div v-if="filteredArtworksList.length === 0" class="map-empty-box">
            <el-empty description="未找到匹配的作品" :image-size="70" />
          </div>
        </div>
      </div>
    </div>

    <!-- 画作详情弹窗 -->
    <ArtworkDetailDrawer
      v-model="dialogVisible"
      :artwork="currentArtwork"
      :current-user-id="userStore.userInfo?.id"
    />

    <!-- 发布作品弹窗 -->
    <PublishArtworkModal
      v-model="publishModalVisible"
      @success="handlePublishSuccess"
    />

    <!-- 用户个人资料弹窗 -->
    <UserProfileModal
      v-model="profileModalVisible"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, markRaw, watch } from 'vue'
import ArtworkDetailDrawer from './ArtworkDetailDrawer.vue'
import PublishArtworkModal from './PublishArtworkModal.vue'
import UserProfileModal from './UserProfileModal.vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMultiplayer } from '@/composables/useMultiplayer'
import * as THREE from 'three'
import { PointerLockControls } from 'three/examples/jsm/controls/PointerLockControls.js'
import { CSS2DRenderer } from 'three/examples/jsm/renderers/CSS2DRenderer.js'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { gsap } from 'gsap'

const router = useRouter()
const userStore = useUserStore()

interface BackendArtwork {
  id: number
  title: string
  author: string
  year: string
  description: string
  url: string
  positionIndex: number
}

interface ExtendedArtworkConfig {
  id: number
  title: string
  artist: string
  author: string
  year: string
  description: string
  imageUrl: string
  url: string
  position: [number, number, number]
  rotation: [number, number, number]
  likeCount: number
}

const canvasContainer = ref<HTMLDivElement | null>(null)
const loading = ref(true)
const hasEntered = ref(false)
const loadProgress = ref(0)
const isLocked = ref(false)
const isHoveredArt = ref(false)
const showMapModal = ref(false)
const profileModalVisible = ref(false)
const publishModalVisible = ref(false)
const searchKeyword = ref('')

const CORRIDOR_WIDTH = 8
const CORRIDOR_LENGTH = 32

const playerMapPosPercent = reactive({ x: 50, z: 50 })
const playerMapAngle = ref(0)

const dialogVisible = ref(false)
const currentArtwork = ref<ExtendedArtworkConfig | null>(null)
const artworksList = ref<ExtendedArtworkConfig[]>([])

const filteredArtworksList = computed(() => {
  if (!searchKeyword.value.trim()) {
    return artworksList.value
  }
  const keyword = searchKeyword.value.trim().toLowerCase()
  return artworksList.value.filter((art) => {
    const titleMatch = art.title?.toLowerCase().includes(keyword)
    const authorMatch = (art.artist || art.author || '').toLowerCase().includes(keyword)
    const descMatch = art.description?.toLowerCase().includes(keyword)
    return titleMatch || authorMatch || descMatch
  })
})

let scene: THREE.Scene | null = null
let camera: THREE.PerspectiveCamera | null = null
let renderer: THREE.WebGLRenderer | null = null
let labelRenderer: CSS2DRenderer | null = null
let controls: PointerLockControls | null = null
let animationFrameId: number | null = null

// 多人同屏控制对象引用
let multiplayer: ReturnType<typeof useMultiplayer> | null = null

const raycaster = new THREE.Raycaster()
const centerVector = new THREE.Vector2(0, 0)
let lastTime = performance.now()
const moveState = { forward: false, backward: false, left: false, right: false }
const MOVE_SPEED = 4.5
const interactiveArtworks: THREE.Mesh[] = []

const props = defineProps({
  drawerOpen: {
    type: Boolean,
    default: false
  }
})

const openProfileModal = () => {
  profileModalVisible.value = true
  if (controls) controls.unlock()
}

watch(() => props.drawerOpen, (isOpen) => {
  if (!camera) return

  const width = window.innerWidth
  const height = window.innerHeight
  const drawerWidth = 450

  if (isOpen) {
    camera.setViewOffset(width, height, drawerWidth / 2, 0, width, height)
  } else {
    camera.clearViewOffset()
  }
  
  camera.updateProjectionMatrix()
})

watch(dialogVisible, (val) => {
  if (!val) {
    handleDialogClosed()
  }
})

watch(publishModalVisible, (val) => {
  if (!val && !dialogVisible.value && !showMapModal.value && hasEntered.value) {
    if (controls) controls.lock()
  }
})

watch(profileModalVisible, (val) => {
  if (!val && !dialogVisible.value && !showMapModal.value && !publishModalVisible.value && hasEntered.value) {
    if (controls) controls.lock()
  }
})

const colors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#409eff', percentage: 80 },
  { color: '#67c23a', percentage: 100 }
]

const getCorridorTransform = (index: number): { position: [number, number, number]; rotation: [number, number, number] } => {
  const isLeft = index % 2 === 0
  const x = isLeft ? -3.9 : 3.9
  const z = -10 + Math.floor(index / 2) * 6.5
  const rotationY = isLeft ? Math.PI / 2 : -Math.PI / 2

  return {
    position: [x, 1.8, z],
    rotation: [0, rotationY, 0]
  }
}

const getArtZPercent = (z: number) => {
  const minZ = -14
  const maxZ = 14
  return Math.max(8, Math.min(92, ((z - minZ) / (maxZ - minZ)) * 100))
}

const updateMinimapData = () => {
  if (!camera) return

  const xPercent = ((camera.position.x + CORRIDOR_WIDTH / 2) / CORRIDOR_WIDTH) * 100
  const zPercent = ((camera.position.z + CORRIDOR_LENGTH / 2) / CORRIDOR_LENGTH) * 100

  playerMapPosPercent.x = Math.max(5, Math.min(95, xPercent))
  playerMapPosPercent.z = Math.max(5, Math.min(95, zPercent))

  const dir = new THREE.Vector3()
  camera.getWorldDirection(dir)
  const angleRad = Math.atan2(dir.x, dir.z)
  playerMapAngle.value = (angleRad * 180) / Math.PI + 180
}

const teleportFromMap = (art: ExtendedArtworkConfig) => {
  closeMapModal()
  teleportToArtwork(art)
}

const teleportToArtwork = (art: ExtendedArtworkConfig) => {
  if (!camera) return

  const isLeft = art.position[0] < 0
  const targetX = isLeft ? -1.8 : 1.8
  const targetZ = art.position[2]

  gsap.to(camera.position, {
    x: targetX,
    y: 1.6,
    z: targetZ,
    duration: 1.2,
    ease: 'power2.inOut',
    onUpdate: () => {
      if (camera) {
        camera.lookAt(art.position[0], art.position[1], art.position[2])
      }
    },
    onComplete: () => {
      ElMessage.success({ message: `已传送至：《${art.title}》`, grouping: true })
      if (controls && !dialogVisible.value && !showMapModal.value && !publishModalVisible.value) {
        controls.lock()
      }
    }
  })
}

const createFallbackTexture = (title: string, author: string) => {
  const canvas = document.createElement('canvas')
  canvas.width = 512
  canvas.height = 512
  const ctx = canvas.getContext('2d')!

  const gradient = ctx.createLinearGradient(0, 0, 512, 512)
  gradient.addColorStop(0, '#f8fafc')
  gradient.addColorStop(1, '#e2e8f0')
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, 512, 512)

  ctx.strokeStyle = '#d97706'
  ctx.lineWidth = 12
  ctx.strokeRect(20, 20, 472, 472)

  ctx.fillStyle = '#0f172a'
  ctx.font = 'bold 28px sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText(title, 256, 230)

  ctx.fillStyle = '#475569'
  ctx.font = '20px sans-serif'
  ctx.fillText(author, 256, 280)

  const texture = new THREE.CanvasTexture(canvas)
  texture.colorSpace = THREE.SRGBColorSpace
  return texture
}

const initEngine = () => {
  if (!canvasContainer.value) return

  const width = canvasContainer.value.clientWidth
  const height = canvasContainer.value.clientHeight

  scene = markRaw(new THREE.Scene())
  scene.background = new THREE.Color(0xf8fafc)

  camera = markRaw(new THREE.PerspectiveCamera(60, width / height, 0.1, 100))
  camera.position.set(0, 1.6, -14)
  camera.lookAt(0, 1.6, 10)

  renderer = markRaw(
    new THREE.WebGLRenderer({ antialias: true, powerPreference: 'high-performance' })
  )
  renderer.setSize(width, height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true

  canvasContainer.value.appendChild(renderer.domElement)

  labelRenderer = markRaw(new CSS2DRenderer())
  labelRenderer.setSize(width, height)
  labelRenderer.domElement.style.position = 'absolute'
  labelRenderer.domElement.style.top = '0px'
  labelRenderer.domElement.style.left = '0px'
  labelRenderer.domElement.style.pointerEvents = 'none'
  canvasContainer.value.appendChild(labelRenderer.domElement)

  setupLighting()
  buildCorridorEnvironment()
  loadArtworksFromBackend()
  setupPointerLock()

  // 初始化多人同屏 WebSocket 逻辑
  multiplayer = useMultiplayer(scene)
  multiplayer.initWebSocket()

  window.addEventListener('resize', handleResize)
  renderLoop()
}

const loadArtworksFromBackend = async () => {
  if (!scene) return
  const currentScene = scene

  let backendData: BackendArtwork[] = []

  try {
    const res = await fetch('http://localhost:8080/api/artworks')
    if (res.ok) {
      backendData = await res.json()
    } else {
      throw new Error(`HTTP ${res.status}`)
    }
  } catch (err) {
    console.warn('获取后端 API 失败，准备降级处理:', err)
  }

  const formattedArtworks: ExtendedArtworkConfig[] = backendData.map((item, idx) => {
    const transform = getCorridorTransform(idx)
    return {
      id: item.id,
      title: item.title,
      artist: item.author,
      author: item.author,
      year: item.year || '未知',
      description: item.description,
      imageUrl: item.url,
      url: item.url,
      position: transform.position,
      rotation: transform.rotation,
      likeCount: Math.floor(Math.random() * 80) + 20
    }
  })

  artworksList.value = formattedArtworks

  if (formattedArtworks.length === 0) {
    loading.value = false
    return
  }

  let loadedCount = 0
  const totalCount = formattedArtworks.length
  const textureLoader = new THREE.TextureLoader()
  textureLoader.setCrossOrigin('anonymous')

  formattedArtworks.forEach((art) => {
    const artGroup = new THREE.Group()
    artGroup.position.set(...art.position)
    artGroup.rotation.set(...art.rotation)

    const applyCanvasTexture = (tex: THREE.Texture) => {
      let w = 1.6
      let h = 2.0

      const img = tex.image as HTMLImageElement | HTMLCanvasElement | undefined

      if (img && img.width && img.height) {
        const aspect = img.width / img.height
        h = 2.0
        w = h * aspect

        if (w > 2.8) {
          w = 2.8
          h = w / aspect
        }
      }

      const frameDepth = 0.08
      const borderWidth = 0.12

      const frameGeo = new THREE.BoxGeometry(w + borderWidth, h + borderWidth, frameDepth)
      const frameMat = new THREE.MeshStandardMaterial({ color: 0x1e1e24, roughness: 0.3 })
      const frameMesh = new THREE.Mesh(frameGeo, frameMat)
      frameMesh.position.z = -frameDepth / 2
      artGroup.add(frameMesh)

      const canvasGeo = new THREE.PlaneGeometry(w, h)
      const canvasMat = new THREE.MeshStandardMaterial({ map: tex, roughness: 0.2 })
      const canvasMesh = new THREE.Mesh(canvasGeo, canvasMat)
      canvasMesh.position.z = 0.001
      canvasMesh.userData = art
      interactiveArtworks.push(canvasMesh)
      artGroup.add(canvasMesh)

      loadedCount++
      loadProgress.value = Math.round((loadedCount / totalCount) * 100)
      if (loadedCount >= totalCount) {
        setTimeout(() => {
          loading.value = false
        }, 300)
      }
    }

    textureLoader.load(
      art.imageUrl,
      (texture) => {
        texture.colorSpace = THREE.SRGBColorSpace
        applyCanvasTexture(texture)
      },
      undefined,
      () => {
        console.warn(`画作《${art.title}》网络加载失败，生成备用材质`)
        applyCanvasTexture(createFallbackTexture(art.title, art.artist))
      }
    )

    const spotLight = new THREE.SpotLight(0xfff8e7, 3.0)
    spotLight.position.set(0, 1.5, 1.0)
    spotLight.target = artGroup
    spotLight.angle = Math.PI / 4
    spotLight.penumbra = 0.4
    artGroup.add(spotLight)

    currentScene.add(artGroup)
  })
}

const handleCanvasClick = () => {
  if (!controls || !controls.isLocked || !camera) return

  raycaster.setFromCamera(centerVector, camera)
  const intersects = raycaster.intersectObjects(interactiveArtworks, false)

  if (intersects.length > 0) {
    const artData = intersects[0].object.userData as ExtendedArtworkConfig
    if (artData) {
      currentArtwork.value = artData
      dialogVisible.value = true
      controls.unlock()
    }
  }
}

const checkRaycastHover = () => {
  if (!controls || !controls.isLocked || !camera) {
    isHoveredArt.value = false
    return
  }

  raycaster.setFromCamera(centerVector, camera)
  const intersects = raycaster.intersectObjects(interactiveArtworks, false)
  isHoveredArt.value = intersects.length > 0
}

const setupPointerLock = () => {
  if (!camera || !renderer) return
  controls = markRaw(new PointerLockControls(camera, renderer.domElement))

  controls.addEventListener('lock', () => {
    isLocked.value = true
    hasEntered.value = true
  })

  controls.addEventListener('unlock', () => {
    if (!document.hasFocus()) {
      resetMoveState()
      return
    }

    // 只有在页面处于焦点状态下解锁，才唤起暂停面板
    if (
      hasEntered.value &&
      !showMapModal.value &&
      !publishModalVisible.value &&
      !profileModalVisible.value &&
      !dialogVisible.value
    ) {
      isLocked.value = false
    }

    resetMoveState()
  })

  window.addEventListener('keydown', handleKeyDown)
  window.addEventListener('keyup', handleKeyUp)
  renderer.domElement.addEventListener('click', handleCanvasClick)
}

// 提取按键重置逻辑
const resetMoveState = () => {
  moveState.forward = false
  moveState.backward = false
  moveState.left = false
  moveState.right = false
}

const openPublishModal = () => {
  publishModalVisible.value = true
  if (controls) controls.unlock()
}

const handlePublishSuccess = () => {
  loadArtworksFromBackend()
}

const handleKeyDown = (e: KeyboardEvent) => {
  const target = e.target as HTMLElement
  if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA')) {
    return
  }

  // 如果弹窗开启中，不响应漫游按键
  if (dialogVisible.value || showMapModal.value || publishModalVisible.value || profileModalVisible.value) return

  if (e.code === 'KeyM') {
    e.preventDefault()
    showMapModal.value ? closeMapModal() : openMapModal()
    return
  }

  if (e.code === 'KeyF') {
    e.preventDefault()
    publishModalVisible.value ? (publishModalVisible.value = false) : openPublishModal()
    return
  }

  switch (e.code) {
    case 'KeyW': case 'ArrowUp': moveState.forward = true; break
    case 'KeyS': case 'ArrowDown': moveState.backward = true; break
    case 'KeyA': case 'ArrowLeft': moveState.left = true; break
    case 'KeyD': case 'ArrowRight': moveState.right = true; break
  }
}

const handleKeyUp = (e: KeyboardEvent) => {
  const target = e.target as HTMLElement
  if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA')) {
    return
  }

  switch (e.code) {
    case 'KeyW': case 'ArrowUp': moveState.forward = false; break
    case 'KeyS': case 'ArrowDown': moveState.backward = false; break
    case 'KeyA': case 'ArrowLeft': moveState.left = false; break
    case 'KeyD': case 'ArrowRight': moveState.right = false; break
  }
}

const openMapModal = () => {
  showMapModal.value = true
  if (controls) controls.unlock()
}

const closeMapModal = () => {
  showMapModal.value = false
  if (controls && !dialogVisible.value && !publishModalVisible.value) {
    controls.lock()
  }
}

const enterGallery = () => {
  if (controls && !dialogVisible.value && !showMapModal.value && !publishModalVisible.value && !profileModalVisible.value) {
    controls.lock()
  }
}

const handleDialogClosed = () => {
  currentArtwork.value = null
  if (controls && !dialogVisible.value && !showMapModal.value && !publishModalVisible.value && !profileModalVisible.value) {
    controls.lock()
  }
}

const updateMovement = () => {
  const currentTime = performance.now()
  const delta = (currentTime - lastTime) / 1000
  lastTime = currentTime

  if (controls && controls.isLocked) {
    const actualSpeed = MOVE_SPEED * delta
    if (moveState.forward) controls.moveForward(actualSpeed)
    if (moveState.backward) controls.moveForward(-actualSpeed)
    if (moveState.left) controls.moveRight(-actualSpeed)
    if (moveState.right) controls.moveRight(actualSpeed)

    if (camera) {
      camera.position.x = Math.max(-2.8, Math.min(2.8, camera.position.x))
      camera.position.z = Math.max(-15, Math.min(15, camera.position.z))
      camera.position.y = 1.6

      // 向服务端实时推送当前玩家的坐标与水平朝向 (Yaw Angle)
      if (multiplayer) {
        const dir = new THREE.Vector3()
        camera.getWorldDirection(dir)
        const rotationY = Math.atan2(dir.x, dir.z)
        multiplayer.sendMove(camera.position.x, camera.position.y, camera.position.z, rotationY)
      }
    }
  }
}

const setupLighting = () => {
  if (!scene) return
  scene.add(new THREE.AmbientLight(0xffffff, 0.85))

  const hemiLight = new THREE.HemisphereLight(0xffffff, 0xe2e8f0, 0.6)
  hemiLight.position.set(0, 5, 0)
  scene.add(hemiLight)
}

const buildCorridorEnvironment = () => {
  if (!scene) return

  const width = CORRIDOR_WIDTH
  const length = CORRIDOR_LENGTH
  const height = 4.5

  const floorGeo = new THREE.PlaneGeometry(width, length)
  const floorMat = new THREE.MeshStandardMaterial({ color: 0xe2e8f0, roughness: 0.15, metalness: 0.05 })
  const floor = new THREE.Mesh(floorGeo, floorMat)
  floor.rotation.x = -Math.PI / 2
  scene.add(floor)

  const ceilingGeo = new THREE.PlaneGeometry(width, length)
  const ceilingMat = new THREE.MeshStandardMaterial({ color: 0xf8fafc, roughness: 0.9 })
  const ceiling = new THREE.Mesh(ceilingGeo, ceilingMat)
  ceiling.position.y = height
  ceiling.rotation.x = Math.PI / 2
  scene.add(ceiling)

  const wallMat = new THREE.MeshStandardMaterial({ color: 0xf1f5f9, roughness: 0.8 })

  const leftWall = new THREE.Mesh(new THREE.PlaneGeometry(length, height), wallMat)
  leftWall.position.set(-width / 2, height / 2, 0)
  leftWall.rotation.y = Math.PI / 2
  scene.add(leftWall)

  const rightWall = new THREE.Mesh(new THREE.PlaneGeometry(length, height), wallMat)
  rightWall.position.set(width / 2, height / 2, 0)
  rightWall.rotation.y = -Math.PI / 2
  scene.add(rightWall)

  const backWall = new THREE.Mesh(new THREE.PlaneGeometry(width, height), wallMat)
  backWall.position.set(0, height / 2, -length / 2)
  scene.add(backWall)

  const frontWall = new THREE.Mesh(new THREE.PlaneGeometry(width, height), wallMat)
  frontWall.position.set(0, height / 2, length / 2)
  frontWall.rotation.y = Math.PI
  scene.add(frontWall)
}

const renderLoop = () => {
  animationFrameId = requestAnimationFrame(renderLoop)

  updateMovement()
  checkRaycastHover()
  updateMinimapData()

  // 逐帧更新远程玩家位置平滑插值 (Lerp)
  if (multiplayer) {
    multiplayer.updateLerp()
  }

  if (scene && camera) {
    if (renderer) {
      renderer.render(scene, camera)
    }
    if (labelRenderer) {
      labelRenderer.render(scene, camera)
    }
  }
}

const handleResize = () => {
  if (!canvasContainer.value || !camera || !renderer) return
  const width = canvasContainer.value.clientWidth
  const height = canvasContainer.value.clientHeight
  camera.aspect = width / height
  camera.updateProjectionMatrix()
  renderer.setSize(width, height)

  if (labelRenderer) {
    labelRenderer.setSize(width, height)
  }
}

onMounted(() => {
  initEngine()
})

onBeforeUnmount(() => {
  if (animationFrameId) cancelAnimationFrame(animationFrameId)
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('keydown', handleKeyDown)
  window.removeEventListener('keyup', handleKeyUp)

  if (multiplayer) {
    multiplayer.closeWebSocket()
    multiplayer.dispose()
  }

  if (controls) controls.dispose()
  if (renderer) {
    renderer.dispose()
    renderer.forceContextLoss()
    renderer.domElement.remove()
  }
  if (labelRenderer) {
    labelRenderer.domElement.remove()
  }
  scene = null
  camera = null
  renderer = null
  labelRenderer = null
  controls = null
  multiplayer = null
})
</script>

<style scoped>
/* 远程玩家头顶 CSS2D 昵称标签样式穿透 */
:deep(.remote-player-nametag) {
  background: rgba(15, 23, 42, 0.75);
  color: #ffffff;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  border: 1px solid rgba(255, 255, 255, 0.25);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(4px);
  pointer-events: none;
  transform: translateY(-50%);
}

.gallery-wrapper {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background-color: #f8fafc;
  user-select: none;
}

.user-account-box {
  margin-bottom: 16px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-info-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.user-profile:hover {
  background: rgba(0, 0, 0, 0.05);
}

.user-profile:hover .nickname {
  color: #4f46e5;
}

.nickname {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.login-btn {
  width: 100%;
  font-weight: 600;
  font-size: 14px;
}

.canvas-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.crosshair {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #334155;
  font-size: 26px;
  pointer-events: none;
  font-weight: 400;
  transition: all 0.2s;
  text-shadow: 0 0 2px rgba(255, 255, 255, 0.8);
}
.crosshair.active {
  color: #16a34a;
  transform: translate(-50%, -50%) scale(1.5);
}

.pause-modal-mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.25);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 80;
  cursor: pointer;
  animation: fadeIn 0.25s ease-out;
}

.pause-modal-card {
  width: 420px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  padding: 32px 28px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08), inset 0 0 0 1px rgba(255, 255, 255, 0.5);
  text-align: center;
  color: #0f172a;
  cursor: default;
}

.pause-icon {
  font-size: 36px;
  margin-bottom: 8px;
}

.pause-modal-card h3 {
  margin: 0 0 6px 0;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.pause-subtitle {
  margin: 0 0 20px 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.tips-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
  text-align: left;
}

.tip-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.6);
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.key-badge {
  background: #0f172a;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 6px;
  letter-spacing: 1px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.tip-desc {
  font-size: 13px;
  color: #334155;
  font-weight: 500;
}

.publish-btn {
  width: 100%;
  padding: 12px 0;
  background: #4f46e5;
  color: #ffffff;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
  margin-bottom: 10px;
}

.publish-btn:hover {
  background: #4338ca;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.4);
}

.resume-btn {
  width: 100%;
  padding: 12px 0;
  background: #ea580c;
  color: #ffffff;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(234, 88, 12, 0.3);
}

.resume-btn:hover {
  background: #c2410c;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(234, 88, 12, 0.4);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.96);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.enter-mask {
  position: absolute;
  inset: 0;
  background: rgba(248, 250, 252, 0.92);
  backdrop-filter: blur(8px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 100;
  cursor: pointer;
}
.loading-text {
  color: #475569;
  margin-top: 16px;
  font-size: 15px;
}
.start-btn-box {
  text-align: center;
  color: #0f172a;
}
.pulse-icon {
  font-size: 48px;
  margin-bottom: 12px;
  animation: pulse 2s infinite;
}
.start-btn-box h2 {
  font-size: 22px;
  margin-bottom: 8px;
}
.start-btn-box p {
  color: #64748b;
  font-size: 14px;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.map-modal-mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(8px);
  z-index: 90;
  display: flex;
  align-items: center;
  justify-content: center;
}

.map-modal-content {
  width: 720px;
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  color: #0f172a;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.map-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.map-header h3 {
  margin: 0;
  font-size: 20px;
  color: #ea580c;
}
.map-close-btn {
  cursor: pointer;
  font-size: 20px;
  color: #94a3b8;
}
.map-close-btn:hover {
  color: #0f172a;
}

.map-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 10px 0 16px;
}
.map-subtitle {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
}
.map-search-input {
  width: 280px;
}

.corridor-map {
  position: relative;
  height: 420px;
  background: #f1f5f9;
  border-radius: 12px;
  border: 1px dashed #cbd5e1;
  overflow: hidden;
}

.corridor-path {
  position: absolute;
  left: 30%;
  right: 30%;
  top: 0;
  bottom: 0;
  background: #ffffff;
  border-left: 2px dashed #94a3b8;
  border-right: 2px dashed #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
}
.path-label {
  writing-mode: vertical-lr;
  letter-spacing: 4px;
  color: #94a3b8;
  font-size: 12px;
}

.player-dot {
  position: absolute;
  width: 14px;
  height: 14px;
  background: #16a34a;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  box-shadow: 0 0 10px #16a34a;
  transition: all 0.1s linear;
  z-index: 2;
}
.player-arrow {
  font-size: 10px;
  color: #fff;
  text-align: center;
  line-height: 14px;
}

.map-artwork-node {
  position: absolute;
  width: 140px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 8px;
  cursor: pointer;
  transform: translateY(-50%);
  transition: all 0.2s;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  z-index: 3;
}
.map-artwork-node:hover {
  border-color: #ea580c;
  transform: translateY(-50%) scale(1.05);
}
.node-left {
  left: 12px;
}
.node-right {
  right: 12px;
}

.node-badge {
  display: inline-block;
  background: #ea580c;
  color: #fff;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  margin-bottom: 4px;
  font-weight: bold;
}
.node-title {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.node-author {
  font-size: 11px;
  color: #64748b;
}

.map-empty-box {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(241, 245, 249, 0.85);
  z-index: 4;
}
</style>