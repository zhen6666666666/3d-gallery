import { ref } from 'vue'
import * as THREE from 'three'
import { CSS2DObject } from 'three/examples/jsm/renderers/CSS2DRenderer.js'
import { FBXLoader } from 'three/examples/jsm/loaders/FBXLoader.js'
import * as SkeletonUtils from 'three/examples/jsm/utils/SkeletonUtils.js'
import { useUserStore } from '@/stores/user'

export interface RemotePlayer {
  userId: number
  nickname: string
  mesh: THREE.Group
  targetPosition: THREE.Vector3
  targetRotationY: number
}

export function useMultiplayer(scene: THREE.Scene) {
  const userStore = useUserStore()
  const socket = ref<WebSocket | null>(null)
  const remotePlayers = new Map<number, RemotePlayer>()
  
  const localUserId = ref<number | null>(null)

  // 1. 全局缓存解析好的 FBX 基础模型
  let basePlayerModel: THREE.Group | null = null

  // 2. 预加载 FBX 模型函数
  const loadModel = (): Promise<void> => {
    return new Promise((resolve) => {
      const loader = new FBXLoader()
      loader.load(
        '/models/player.fbx',
        (fbx) => {
          basePlayerModel = fbx

          basePlayerModel.scale.set(0.01, 0.01, 0.01)

          // 开启模型阴影
          basePlayerModel.traverse((child) => {
            if ((child as THREE.Mesh).isMesh) {
              child.castShadow = true
              child.receiveShadow = true
            }
          })

          console.log('FBX 远程玩家 3D 模型加载成功！')
          resolve()
        },
        undefined,
        (error) => {
          console.error('加载 FBX 人物模型失败，降级使用默认胶囊体:', error)
          resolve()
        }
      )
    })
  }

  // 3. 初始化 WebSocket 连接
  const initWebSocket = async () => {
    // 先异步加载 FBX 模型资源
    await loadModel()

    const wsUrl = 'ws://localhost:8080/ws/exhibition'
    socket.value = new WebSocket(wsUrl)

    socket.value.onopen = () => {
      console.log('WebSocket 连接成功！')

      // 获取用户 ID 与 昵称，未登录则生成匿名游客身份
      const userId = userStore.userInfo?.id || Math.floor(Math.random() * 100000)
      localUserId.value = userId
      
      const nickname = userStore.userInfo?.nickname || userStore.userInfo?.username || `游客_${userId}`
      const avatar = userStore.userInfo?.avatar || ''

      sendWsMessage('USER_JOIN', userId, {
        userId,
        username: nickname,
        nickname,
        avatar,
        x: 0,
        y: 1.6,
        z: -10,
        rotationY: 0
      })
    }

    socket.value.onmessage = (event) => {
      const msg = JSON.parse(event.data)
      handleServerMessage(msg)
    }

    socket.value.onclose = () => {
      console.log('WebSocket 连接已关闭')
    }
  }

  // 4. 处理服务端推过来的消息
  const handleServerMessage = (msg: { type: string; senderId: number; data: any }) => {
    if (msg.type !== 'INIT_USERS' && localUserId.value !== null && msg.senderId === localUserId.value) {
      return
    }

    switch (msg.type) {
      case 'INIT_USERS': {
        const playersList = msg.data || []
        playersList.forEach((playerData: any) => {
          if (playerData.userId !== localUserId.value) {
            addRemotePlayer(playerData)
          }
        })
        break
      }
      case 'USER_JOIN': {
        if (msg.senderId !== localUserId.value) {
          addRemotePlayer(msg.data)
        }
        break
      }
      case 'USER_MOVE': {
        if (msg.senderId !== localUserId.value) {
          updateRemotePlayerTarget(msg.data)
        }
        break
      }
      case 'USER_LEAVE': {
        removeRemotePlayer(msg.data)
        break
      }
    }
  }

  // 5. 创建远程玩家 FBX 模型 + 头顶 CSS2D 昵称
  const addRemotePlayer = (data: any) => {
    if (remotePlayers.has(data.userId)) return

    const playerGroup = new THREE.Group()

    if (basePlayerModel) {
      const characterModel = SkeletonUtils.clone(basePlayerModel) as THREE.Group

      characterModel.position.set(0, -1.6, 0)

      playerGroup.add(characterModel)
    } else {
      const geometry = new THREE.CapsuleGeometry(0.4, 1, 4, 8)
      const material = new THREE.MeshStandardMaterial({ color: 0x409eff })
      const bodyMesh = new THREE.Mesh(geometry, material)
      bodyMesh.position.y = -0.8
      playerGroup.add(bodyMesh)
    }

    // 头顶 HTML 昵称标签 (CSS2D)
    const nameDiv = document.createElement('div')
    nameDiv.className = 'remote-player-nametag'
    nameDiv.innerText = data.nickname || data.username
    const nameLabel = new CSS2DObject(nameDiv)
    
    nameLabel.position.set(0, 0.3, 0) 
    playerGroup.add(nameLabel)

    // 默认 y 如果未传，设为相机高度 1.6
    playerGroup.position.set(data.x || 0, data.y ?? 1.6, data.z || 0)
    playerGroup.rotation.y = data.rotationY || 0

    scene.add(playerGroup)

    remotePlayers.set(data.userId, {
      userId: data.userId,
      nickname: data.nickname || data.username,
      mesh: playerGroup,
      targetPosition: new THREE.Vector3(data.x || 0, data.y ?? 1.6, data.z || 0),
      targetRotationY: data.rotationY || 0
    })
  }

  // 6. 更新目标姿态
  const updateRemotePlayerTarget = (data: any) => {
    const player = remotePlayers.get(data.userId)
    if (player) {
      player.targetPosition.set(data.x, data.y, data.z)
      player.targetRotationY = data.rotationY
    }
  }

  // 7. 移除离线玩家
  const removeRemotePlayer = (userId: number) => {
    const player = remotePlayers.get(userId)
    if (player) {
      player.mesh.traverse((child) => {
        if (child instanceof CSS2DObject) {
          child.element.remove()
        }
      })
      scene.remove(player.mesh)
      remotePlayers.delete(userId)
    }
  }

  // 8. 向后端发送位置同步数据
  const sendMove = (x: number, y: number, z: number, rotationY: number) => {
    if (!socket.value || socket.value.readyState !== WebSocket.OPEN) return
    if (localUserId.value === null) return

    sendWsMessage('USER_MOVE', localUserId.value, {
      userId: localUserId.value,
      x,
      y,
      z,
      rotationY
    })
  }

  const sendWsMessage = (type: string, senderId: number, data: any) => {
    socket.value?.send(JSON.stringify({ type, senderId, data }))
  }

  // 9. 每帧调用的插值更新 (Lerp)
  const updateLerp = () => {
    remotePlayers.forEach((player) => {
      player.mesh.position.lerp(player.targetPosition, 0.15)
      player.mesh.rotation.y += (player.targetRotationY - player.mesh.rotation.y) * 0.15
    })
  }

  const dispose = () => {
    remotePlayers.forEach((player) => {
      player.mesh.traverse((child) => {
        if (child instanceof CSS2DObject) {
          child.element.remove()
        }
      })
      scene.remove(player.mesh)
    })
    remotePlayers.clear()
  }

  const closeWebSocket = () => {
    socket.value?.close()
  }

  return {
    initWebSocket,
    sendMove,
    updateLerp,
    closeWebSocket,
    dispose
  }
}