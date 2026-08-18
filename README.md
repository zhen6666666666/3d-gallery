# 3D 线上美术展厅

基于 Vue 3 + Three.js + WebSocket 开发的高性能 3D 线上美术展览平台。

## 项目功能特性
-  **3D 展厅漫游**：采用 第一人称视角 (PointerLockControls) 进行展厅探索。
-  **多人同屏互动**：支持 WebSocket 实时同步多玩家位置、旋转角度，实时显示玩家头顶 CSS2D 昵称。
-  **平面地图导航**：按下 [M] 键唤起 2D 展厅导航平面图，点击画作支持一键传送。
-  **画作展示与发布**：点击墙面画作可查看高清大图详情，支持实时发布新画作。

##  技术栈
- **前端框架**：Vue 3 + TypeScript + Vite
- **3D 引擎**：Three.js (FBXLoader, CSS2DRenderer, GSAP)
- **UI 框架**：Element Plus
- **通信支持**：WebSocket

## 本地运行指南
```bash
# 安装依赖
npm install

# 运行前端服务
npm run dev
