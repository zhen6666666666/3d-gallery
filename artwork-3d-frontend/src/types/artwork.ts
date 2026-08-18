export interface ArtworkConfig {
  id: number
  title: string
  artist: string
  imageUrl: string
  // 3D 空间中的位置 [x, y, z]
  position: [number, number, number]
  // 旋转角度 [rx, ry, rz]（单位：弧度，如 Math.PI）
  rotation: [number, number, number]
  // 尺寸 [宽, 高]
  size: [number, number]
}