# Scaffold Center1 旋转分析报告

## 关键发现：+1.0 坐标偏移Bug

**根本原因：** `RotationUtils.getRotations(Vec3)` 在计算yaw/pitch之前对所有坐标加了 +1.0。当Center1瞄准 `(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)` 这个点时，旋转计算会将其视为 `(blockPos.x + 1.5, blockPos.y + 1.5, blockPos.z + 1.5)` —— **偏离了整整一个方块的距离**。

## Center1 工作原理

### 旋转模式
Center (模式4) - 实现了"Telly桥接"，使用多点采样

### 目标点选择
- 每个面生成25个采样点，使用偏移值：`{0.15, 0.35, 0.5, 0.65, 0.85}` 在两个轴上
- 垂直于面的轴被约束到边缘（负方向面使用0.02，正方向面使用0.98）
- 例如：NORTH面使用 `z=0.02`，在x和y轴上采样5个偏移值

### 计算流程

1. **生成采样网格** - 为x, y, z轴创建偏移数组 `{0.15, 0.35, 0.5, 0.65, 0.85}`

2. **应用面约束** - 根据EnumFacing，将一个轴锁定到边缘值
   - NORTH → z=0.02
   - EAST → x=0.98
   - SOUTH → z=0.98
   - WEST → x=0.02
   - DOWN → y=0.02
   - UP → y=0.98

3. **遍历所有组合** - 对每个 (dx, dy, dz) 组合，创建目标点 `Vec3(blockPos.x + dx, blockPos.y + dy, blockPos.z + dz)`

4. **计算旋转（带+1.0 bug）** - `RotationUtils.getRotations(Vec3)` 会加1.0：
   ```java
   x = vec.xCoord + 1.0 - player.posX
   y = vec.yCoord + 1.0 - (player.posY + eyeHeight)
   z = vec.zCoord + 1.0 - player.posZ
   ```

5. **计算yaw/pitch**
   ```java
   yaw = player.rotationYaw + wrapAngleTo180(atan2(z, x) * 180/PI - 90° - player.rotationYaw)
   pitch = clamp(player.rotationPitch + wrapAngleTo180(-atan2(y, sqrt(x² + z²)) * 180/PI - player.rotationPitch), -90, 90)
   ```

6. **射线追踪验证** - 从玩家眼睛位置调用 `rayTraceCustom(blockReachDistance, yaw, pitch)`，使用 `getVectorForRotation` 投射射线

7. **验证命中** - 检查 MovingObjectPosition 是否命中正确的 blockPos **且** 正确的 EnumFacing，不匹配则拒绝

8. **角度距离选择** - 计算 `abs(wrapAngleTo180(yaw - currentYaw)) + abs(pitch - currentPitch)`，追踪最小值

9. **返回最佳或回退** - 如果找到有效旋转，返回角度距离最小的；否则回退到 `(targetBlock - lookVec)` 计算

### 射线追踪验证

- 在**每次**旋转计算之后执行
- 从玩家眼睛位置沿计算出的视线方向发射射线
- 必须满足：命中类型是方块 + 正确的BlockPos + 正确的EnumFacing
- 作为过滤器，拒绝由+1.0 bug导致的错误旋转

## 为什么会被GrimAC的RotationPlace检测

### GrimAC的期望

- 从玩家眼睛位置沿其服务端视线方向进行射线追踪
- 必须命中正在放置的确切方块面
- 射线追踪结果必须与放置数据包的face参数匹配

### 实际发生的情况

#### 1. 有效采样率降低
+1.0偏移导致许多采样的旋转瞄准错误位置。大多数无法通过内部射线追踪验证。这将有效采样池从25个减少到可能只有几个有效选项。

#### 2. 边缘情况失败
当玩家位置/角度处于到达距离极限或极端角度时，所有25个采样可能都无法通过验证。Center1会回退到 `(targetBlock - lookVec)`，而这**完全没有射线追踪验证**。

#### 3. 非确定性瞄准
选择哪个点取决于哪些采样通过验证**且**具有最小角度距离。在移动过程中，这可能在不同采样点之间突然切换，导致旋转不稳定。

#### 4. 边缘采样偏差
面约束轴使用0.02/0.98（靠近边缘）。结合+1.0 bug，旋转瞄准边缘之外 `1.02` 个方块或之前 `-0.02`，增加了不匹配概率。

#### 5. 角度距离优先级
选择"最平滑"的旋转（最接近当前视角）可能会牺牲可靠性。一个不太平滑的旋转可能会命中更稳定的点，但会被拒绝。

### 失败场景示例

```
玩家位置: (10.5, 64.62, 20.5)
目标方块: (11, 64, 21) 
目标面: NORTH (z方向负)
采样点: (11.5, 64.5, 21.02) - 面中心，z约束到边缘

计算旋转时:
- 预期使用: (11.5, 64.5, 21.02)
- 实际使用: (12.5, 65.5, 22.02) [+1.0偏移]

结果射线追踪:
- 玩家以计算出的角度看向 (12.5, 65.5, 22.02) 方向
- 射线可能完全错过 (11, 64, 21) 或命中错误的面
- 验证失败，这个采样被拒绝

如果所有25个采样都失败:
- 使用回退旋转 (targetBlock - lookVec)
- 没有验证这是否实际命中正确的面
- 发送到服务器 → GrimAC检测到视线与放置面不匹配
```

## 修复方案

### 方案1：移除+1.0偏移Bug（影响最大）

**代码位置：** `RotationUtils.getRotations(Vec3 vec)`

```java
// 当前代码（有BUG）：
double x = vec.xCoord + 1.0 - player.posX;
double y = vec.yCoord + 1.0 - (player.posY + player.getEyeHeight());
double z = vec.zCoord + 1.0 - player.posZ;

// 修复后：
double x = vec.xCoord - player.posX;
double y = vec.yCoord - (player.posY + player.getEyeHeight());
double z = vec.zCoord - player.posZ;
```

**优点：**
- 解决根本原因
- 所有25个采样点将瞄准正确位置
- 大幅提高验证通过率
- 消除回退路径

**缺点：**
- 可能影响其他使用 `getRotations()` 的模块
- 需要全面测试

### 方案2：在旋转计算前预验证射线追踪

```java
// 在计算采样点的旋转之前：
Vec3 eyePos = player.getPositionEyes(1.0F);
Vec3 targetPos = new Vec3(blockPos.x + dx, blockPos.y + dy, blockPos.z + dz);
MovingObjectPosition directTrace = mc.theWorld.rayTraceBlocks(
    eyePos, targetPos, false, false, true
);

// 只有当存在直接视线时才计算旋转
if (directTrace != null && 
    directTrace.getBlockPos().equals(blockPos) && 
    directTrace.sideHit == facing) {
    float[] rotation = RotationUtils.getRotations(targetPos);
    // ... 继续验证
}
```

**优点：**
- 提前过滤不可能的采样
- 不改变核心旋转计算逻辑

**缺点：**
- 没有修复底层bug
- 增加额外的射线追踪调用（性能影响）

### 方案3：使用Center2模式代替

Center2（模式5）瞄准点击面的精确中心，在垂直轴上使用 `±0.5`。它是确定性的，不使用多重采样，因此受+1.0 bug的采样效率低下影响较小。

**优点：**
- 更可预测，有利于反作弊合规
- 简单的单点瞄准

**缺点：**
- 不如Center1的多点方法灵活
- 可能在某些角度下不太平滑

### 方案4：实现符合GrimAC的验证

```java
// 选择最佳旋转后，验证它是否符合GrimAC的期望：
Vec3 lookVec = RotationUtils.getVectorForRotation(selectedPitch, selectedYaw);
Vec3 reachVec = eyePos.addVector(
    lookVec.xCoord * reach, 
    lookVec.yCoord * reach, 
    lookVec.zCoord * reach
);
MovingObjectPosition grimacTrace = mc.theWorld.rayTraceBlocks(
    eyePos, reachVec, false, false, true
);

if (grimacTrace == null || 
    !grimacTrace.getBlockPos().equals(targetBlock) || 
    grimacTrace.sideHit != clickedFace) {
    // 旋转会被GrimAC检测 - 尝试下一个采样或中止放置
    return false;
}
```

**优点：**
- 添加明确的GrimAC感知验证作为最终检查
- 深度防御策略
- 不改变现有计算逻辑

**缺点：**
- 如果所有采样都失败，可能导致无法放置方块

### 方案5：改进采样策略

```java
// 使用更保守的偏移值，避免边缘
double[] SAFE_OFFSETS = {0.25, 0.35, 0.5, 0.65, 0.75};  // 远离0.15和0.85边缘值

// 或者使用自适应采样：从中心开始，逐渐扩展
double[][] PRIORITY_SAMPLES = {
    {0.5, 0.5},    // 中心 - 最高优先级
    {0.4, 0.5}, {0.6, 0.5}, {0.5, 0.4}, {0.5, 0.6},  // 十字
    {0.35, 0.35}, {0.65, 0.35}, {0.35, 0.65}, {0.65, 0.65},  // 对角线
    // ... 更多采样
};

// 找到第一个有效的就返回，而不是寻找角度最小的
for (double[] sample : PRIORITY_SAMPLES) {
    float[] rot = calculateAndValidate(sample[0], sample[1]);
    if (rot != null) return rot;
}
```

**优点：**
- 提高找到有效旋转的概率
- 优先考虑更可靠的中心点
- 减少不必要的边缘采样

## 推荐实施方案

**组合方案：方案1（移除+1.0偏移）+ 方案4（添加GrimAC验证）**

这种方法：
1. 解决根本原因（+1.0 bug）
2. 添加深度防御（明确的GrimAC验证）
3. 最大程度提高与反作弊的兼容性

### 实施步骤

1. **修复RotationUtils.getRotations()**
   - 移除+1.0偏移
   - 测试所有使用该方法的模块

2. **在Scaffold中添加GrimAC验证**
   - 在选择最终旋转后进行验证
   - 如果验证失败，尝试下一个候选或跳过该tick

3. **测试验证**
   - 在GrimAC服务器上测试各种角度和距离
   - 验证不会出现误报（正常放置被阻止）
   - 确认通过率提高

## 技术细节

### 代码位置

- **Scaffold模块**: `src/main/java/keystrokesmod/module/impl/player/Scaffold.java`
  - Center1实现: `calculateCenterModeRotations()` (行1194-1278)
  - 旋转应用: `centerRots()` (行1084-1121)
  
- **旋转工具**: `src/main/java/keystrokesmod/utility/RotationUtils.java`
  - Bug位置: `getRotations(Vec3)` 方法（添加+1.0的地方）
  - 射线追踪: `rayTraceCustom()` 方法
  
- **旋转助手**: `src/main/java/keystrokesmod/helper/RotationHelper.java`
  - 服务端旋转发送: `onPreMotion()`
  - 移动修正: `fixMovement()`

### 相关配置

- **旋转模式枚举** (Scaffold.java 行84):
  - 0: Disabled
  - 1: Simple
  - 2: Offset
  - 3: Precise
  - 4: Center (当前分析的模式)
  - 5: Center2

- **采样偏移值**: `{0.15, 0.35, 0.5, 0.65, 0.85}`
- **面边缘约束**: `0.02` (负面) / `0.98` (正面)
- **方块到达距离**: 通常为 4.5 方块

## 附加说明

### 为什么现有代码"有时"能工作

射线追踪验证作为过滤器，可以捕获大多数由+1.0 bug产生的错误旋转。在理想条件下（玩家在良好位置和角度），25个采样中的一些仍然会通过验证，因为：

1. 射线追踪具有一定的容差
2. 某些采样点的偏移"碰巧"仍然指向正确的方块面
3. 多个采样增加了至少一个有效的概率

但在边缘情况下（极端角度、最大距离、快速移动），所有采样都可能失败，导致使用未验证的回退旋转。

### Center1 vs Center2

| 特性 | Center1 (模式4) | Center2 (模式5) |
|------|----------------|-----------------|
| 采样策略 | 25点网格采样 | 单点（面中心） |
| 目标点 | 动态（取决于验证和角度） | 固定（±0.5在垂直轴） |
| 受+1.0 bug影响 | 高（降低采样效率） | 中（影响单一计算） |
| 验证 | 每个采样都验证 | 单次验证 |
| 平滑度 | 优化（选择最接近当前角度） | 标准 |
| 可靠性 | 取决于采样成功率 | 更一致 |
| Telly跳跃 | 是（每2 tick跳跃） | 否 |

---

**报告生成时间**: 2026-08-30  
**分析方法**: 多阶段工作流 + 对抗性验证  
**置信度**: 高（3/3验证者确认诊断）
