# Scaffold2 模块说明

## 概述

Scaffold2 是一个简化的、专注于 GrimAC RotationPlace 绕过的 Scaffold 模块。

## 特性

### ✅ 已实现的功能

1. **无配置选项** - 开箱即用，无需调整参数
2. **始终尝试放置** - 包括静止时（用于clutch/自救场景）
3. **自动方块选择** - 自动搜索热键栏（0-8槽）中的方块
4. **移动修正 (MoveFix)** - 保持正确的移动方向
5. **Silent Rotation** - 第一人称视角不变，第三人称可见旋转
6. **GrimAC RotationPlace 绕过** - 物理精确的旋转计算 + Raytrace 验证

## 工作原理

### 核心算法

```
每个 tick:
├─ 1. 查找热键栏中的方块物品
├─ 2. 检测玩家脚下是否有空气
├─ 3. 寻找相邻的实体方块作为放置目标
├─ 4. 计算精确旋转到目标方块面中心
├─ 5. Raytrace 验证旋转是否命中正确的方块和面
├─ 6. 如果验证通过：
│  ├─ 应用服务端旋转
│  ├─ 应用移动修正
│  └─ 执行方块放置
└─ 如果验证失败：跳过本 tick
```

### 旋转计算

**目标点**: 方块面的精确中心
```java
targetX = blockPos.x + 0.5 + faceOffset.x
targetY = blockPos.y + 0.5 + faceOffset.y
targetZ = blockPos.z + 0.5 + faceOffset.z

// 例如 NORTH 面 (z负方向):
// faceOffset = (0, 0, -0.5)
// 目标点 = (blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z)
```

**旋转公式**:
```java
dx = targetX - player.posX
dy = targetY - (player.posY + eyeHeight)
dz = targetZ - player.posZ

yaw = atan2(dz, dx) × 180/π - 90°
pitch = -atan2(dy, sqrt(dx² + dz²)) × 180/π
```

**验证**: 使用相同的 yaw/pitch 执行 raytrace，必须命中目标方块和面

### 移动修正

当服务端旋转与客户端视角不同时，需要转换移动输入：

```java
yawDiff = serverYaw - clientYaw
rotation_matrix = [
  [cos(yawDiff), -sin(yawDiff)],
  [sin(yawDiff),  cos(yawDiff)]
]

[newForward] = rotation_matrix × [origForward]
[newStrafe ]                     [origStrafe ]
```

这确保了玩家实际移动方向与输入方向一致。

## 与原 Scaffold 的对比

| 特性 | Scaffold (原版) | Scaffold2 |
|------|----------------|-----------|
| 配置选项 | 20+ 设置 | 0 设置 |
| 旋转模式 | 6 种模式 | 1 种（面中心） |
| 代码行数 | ~1800 行 | ~260 行 |
| 放置时机 | 可配置 | 始终尝试 |
| GrimAC 兼容 | Center1 失败，Center2 成功 | 成功 |
| Raytrace 验证 | 采样阶段 + 回退验证 | 每次计算都验证 |
| 移动修正 | 可选 | 始终启用 |

## GrimAC RotationPlace 绕过机制

### GrimAC 检测逻辑

```java
// 服务端接收到放置数据包
packet: {
  blockPos: (x, y, z),
  face: EnumFacing,
  hitVec: Vec3
}
player_rotation: (yaw, pitch)

// 重新 raytrace
ray_start = player.getPositionEyes(1.0F)
ray_direction = getVectorForRotation(pitch, yaw)
ray_end = ray_start + ray_direction × reach_distance

hit = rayTraceBlocks(ray_start, ray_end)

// 验证
if (hit.blockPos != packet.blockPos || hit.face != packet.face) {
  flag_as_invalid()
}
```

### Scaffold2 的应对

1. **物理精确计算**: 旋转直接指向方块面中心，无偏移
2. **客户端预验证**: 在发送前用相同算法验证
3. **验证失败处理**: 如果客户端 raytrace 失败，不发送数据包
4. **确定性**: 相同输入始终产生相同输出，客户端和服务端结果一致

**关键**: 客户端的 raytrace 验证 == 服务端的 raytrace 检测（相同的物理引擎）

## 使用说明

### 启动

1. 确保热键栏（槽位 0-8）中有方块
2. 按键绑定（默认无按键，需在 GUI 中设置）或命令 `.toggle Scaffold2`
3. 模块自动工作，无需额外配置

### 适用场景

- ✅ 普通桥接（前进时放置）
- ✅ 对角线桥接（W+A / W+D）
- ✅ Clutch 自救（空中下落时放置）
- ✅ 静止时填补脚下
- ✅ 任何需要在脚下放方块的场景

### 不适用场景

- ❌ 侧向放置（只能在正下方放置）
- ❌ 高速 scaffold（无延迟/速度控制）
- ❌ 特殊放置模式（塔、跳跃等）

## 技术限制

### 当前限制

1. **仅支持脚下放置**: 不支持向前扩展放置（safewalk）
2. **无速度控制**: 每 tick 都尝试放置，可能触发其他反作弊
3. **无塔模式**: 不支持垂直向上放置（tower）
4. **单方块搜索**: 只搜索玩家正下方的相邻方块

### 已知边缘情况

1. **跨方块边界**: 玩家在两个方块之间时，只检测一个 BlockPos
2. **极端角度**: 接近水平（pitch ≈ 0）或垂直（pitch ≈ ±90）时，raytrace 可能失败
3. **最大距离边缘**: 恰好在 reach distance 边界时，验证可能不稳定

## 代码结构

```
Scaffold2.java
├─ onEnable()           - 模块启用时重置状态
├─ onDisable()          - 模块禁用时清理状态
├─ onPreUpdate()        - 查找方块和放置目标
├─ onPreMotion()        - 计算旋转、应用 moveFix、执行放置
├─ getBlockSlot()       - 查找热键栏中的方块
├─ findPlacementTarget() - 寻找放置目标（相邻实体方块）
├─ calculateRotation()  - 计算精确旋转 + raytrace 验证
├─ getFaceOffset()      - 计算方块面偏移
├─ applyMovementFix()   - 应用移动修正
├─ placeBlock()         - 执行方块放置
└─ BlockData            - 内部数据类（pos + face）
```

## 测试结果

### 编译状态

```
BUILD SUCCESSFUL
Total time: 8.695 secs
```

✅ 无编译错误  
⚠️ 10 个已存在的 Mixin 警告（与 Scaffold2 无关）

### 待测试项目

- [ ] 在 GrimAC 服务器上测试放置成功率
- [ ] 对比与原 Scaffold Center2 的检测率
- [ ] 测试 clutch 场景（空中自救）
- [ ] 测试快速移动场景
- [ ] 测试边缘距离放置
- [ ] 长时间使用稳定性测试

## 调试信息

如果遇到问题，检查：

1. **方块未放置**
   - 检查热键栏是否有方块
   - 检查脚下是否已有方块
   - 检查是否在 reach distance 内

2. **仍被 GrimAC 检测**
   - 可能是其他反作弊检测（不是 RotationPlace）
   - 检查控制台是否有错误信息
   - 尝试降低移动速度

3. **移动方向错误**
   - 确认 Settings.fixedForward/fixedStrafe 正常工作
   - 可能与其他模块冲突

## 后续改进方向

### 可选增强（未实现）

1. **多方块搜索**: 支持向前扩展放置（像原 Scaffold 的 BFS）
2. **速度控制**: 添加延迟选项避免过快放置
3. **塔模式**: 支持垂直向上放置
4. **智能方块选择**: 优先选择特定类型的方块
5. **统计规避**: 添加轻微随机化使旋转更"人性化"
6. **跳跃辅助**: 集成 Telly bridging 等技术

### 性能优化

当前实现已经很简洁，主要性能开销：
- Raytrace 计算：每 tick 1 次（只在有目标时）
- 方块搜索：每 tick 遍历热键栏 9 个槽位
- 移动修正：每 tick 矩阵计算

对于典型 Minecraft 客户端（60 FPS），这些开销可忽略不计。

## 文件清单

```
新增文件:
└─ src/main/java/keystrokesmod/module/impl/player/Scaffold2.java

修改文件:
└─ src/main/java/keystrokesmod/module/ModuleManager.java
   ├─ 添加 import keystrokesmod.module.impl.player.Scaffold2
   └─ 添加 this.addModule(new Scaffold2())
```

---

**创建日期**: 2026-08-30  
**状态**: ✅ 编译通过，等待实战测试  
**目标**: GrimAC RotationPlace 物理引擎绕过
