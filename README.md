# Raven bS-997 — 可编译工程还原

从 `raven-bS-997.jar` 反编译还原的 Minecraft 1.8.9 Forge mod 工程。

## 环境要求

ForgeGradle 2.1 只能在 **Java 8** 下运行(系统默认的 Java 25 会直接失败)。
`build.sh` 已固定 `JAVA_HOME`,推荐用它代替直接调用 `gradlew`。

## 构建

```bash
./build.sh setupDecompWorkspace   # 首次执行,下载并反编译 Minecraft
./build.sh build                  # 产物: build/libs/raven-bS-997.jar
```

生成 IDE 工程:

```bash
./build.sh idea      # 或 eclipse
```

## 还原过程中的关键点

**成员名是 SRG,类名不是。** 原 jar 的类名已是 `net/minecraft/client/Minecraft`
这样的可读形式,但字段与方法仍是 `func_71410_x` / `field_71439_g`。因此本工程
用 MCP `stable_22` 映射表把 965 个 SRG 标识符批量改写为可读名(脚本见
`../work/remap.py`)。

有 26 个 `func_*` / `field_*` 名字保留原样,因为 MCP 本身也未给它们命名
(例如 `S27PacketExplosion.func_149144_d`、`GuiScreenBook.field_175386_A`)。
这些在 MCP 工作区里就是这个名字,可以正常编译。

**mappings 必须保持 `stable_22`。** 源码是按这份映射表改写的,换成别的版本
(如 MDK 默认的 `stable_20`)会导致大量符号找不到。

**内联库改为 Maven 依赖。** 原 jar 里打包了 Mixin 0.7.11、Java-WebSocket
和 SLF4J。工程改为声明依赖并在打包时 shade 进产物,行为与原 jar 一致。

**refmap 由构建时生成。** 原 `mixins.raven.refmap.json` 没有保留,它由 Mixin
的注解处理器在编译期重新生成,因为源码已是 MCP 名,需要 refmap 才能在运行时
映射回 SRG。生成结果与原 jar 里的 refmap 完全一致(38 个 mixin 类逐条相同)。

**`@Shadow` / `@Overwrite` 成员需要额外的 srg。** refmap 只覆盖 `@Inject` 等
注解里的目标字符串,不管 `@Shadow` 方法自身的名字。这些成员必须在产物里被重命名
为 SRG,否则运行时绑定失败。因此 build.gradle 让注解处理器额外输出
`build/mixins.raven.srg`,再通过 `reobfJar.addSecondarySrgFile` 参与重混淆。
少了这一步,`MixinEntityPlayer` 等类的 shadow 方法会停留在 MCP 名。

**shade 时必须排除 `META-INF/versions/**`。** ForgeGradle 2.1 的 ASM 太老,
读不懂 Java 9+ 的 `module-info.class`,`reobfJar` 会抛出无消息的
`IllegalArgumentException`。原 jar 也没有这些条目。

注意这个坑只在 `clean` 之后才暴露 — 增量构建会拿旧 jar 去重混淆,看起来是成功的。
改完构建脚本后请用 `./build.sh clean build` 验证。

## 还原结果验证

用 `javap` 逐类比对产物与原 jar 的签名:

- 类数量:1004 / 1004 完全一致,无缺失无多余
- mod 类签名:357 个中 355 个逐字节一致
- 剩下 2 个(`script/Manager`、`module/impl/player/InvManager`)差异仅在
  javac 自动生成的合成成员上 —— `access$N` 的编号顺序不同、一个 lambda 的
  static 修饰不同。两边合成成员数量相同且各自自洽,不影响行为。
- refmap:38 个 mixin 类的映射逐条一致

手工修改仅 11 处,全部是反编译产物,不是逻辑改动:

- 10 处 mixin 自引用转型。反编译器把 `(Target)(Object)this` 简化成了
  `(Target)this`,而 mixin 类在编译期并不继承目标类,必须绕 `Object`。
  同理 `this == mc.thePlayer` 改为 `(Object)this == mc.thePlayer`。
- 1 处 `Utils.getEnum` 里多余的 `(Enum[])` 转型,擦除后与泛型返回值冲突。

## 结构

```
src/main/java/keystrokesmod/    312 个类
├── mixin/impl/                 44 个 mixin(含 accessor 接口)
├── module/impl/                123 个功能模块
├── script/                     JS/Java 脚本引擎与数据包封装
├── clickgui/                   GUI
└── utility/, helper/, event/
src/main/resources/             mcmod.info、mixins.raven.json、assets
```

`mixins.raven.json` 列出 41 个 mixin,但源码里有 44 个:
`IAccessorItemRenderer`、`MixinGuiIngame`、`MixinGuiIngameForge` 未被注册。
原 jar 也是如此,属于原作者留下的死代码,已按原样保留。

## 后续开发经验

### AimVisualizer 模块

新增 `AimVisualizer` 模块(render 分类),用于可视化 KillAura 和 Scaffold 的服务端瞄准点:

- **KillAura**:在当前攻击目标位置渲染一条射线 + 圆点
- **Scaffold**:渲染当前瞄准点的射线,并将历史点以圆点形式持久保留,过期时间由 `scaffoldHistoryTime` 滑块控制(0–10000 ms,默认 2000 ms)
- `onDisable()` 时清空历史队列
- 在 `ModuleManager` 中注册该模块

### Scaffold — Center2 旋转模式

在 Scaffold 的旋转模式数组中新增 `Center2`(index 5),瞄准行为:

- 瞄准点 = 旧方块中心 + `blockFacing` 方向上 ±0.5 的偏移,即被点击面的中心(面上,不是偏移 0.5 进新方块)
- 与 `Center` 的区别:`Center` 使用 `backwardsYaw`(反向)而 `Center2` 使用精确指向该面的 yaw
- hitVec 公式:`blockX + 0.5 + getCoord(blockFacing, "x")` —— **不**加 `* 0.5`,否则坐标会落在面内而非面上
- 使用 `getRotationsToWorldPoint()` 而非 `RotationUtils.getRotations(Vec3)`,因为后者对每个坐标多加了 `+1.0`(为角坐标设计),对世界坐标会导致 rayTrace 始终失败

**关键陷阱:`RotationUtils.getRotations(Vec3)` 的 +1.0 偏置**

```java
// RotationUtils.getRotations(Vec3 vec) 内部:
double dx = vec.xCoord + 1.0 - player.posX;  // ← +1.0 是针对方块角坐标的
```

对精确世界坐标(如面中心)必须绕过这个方法,改用直接 atan2 计算:

```java
private float[] getRotationsToWorldPoint(double worldX, double worldY, double worldZ) {
    double dx = worldX - mc.thePlayer.posX;
    double dy = worldY - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
    double dz = worldZ - mc.thePlayer.posZ;
    float yaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
    yaw = mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw);
    float pitch = (float)(-(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz)) * (180.0 / Math.PI)));
    return new float[]{yaw, MathHelper.clamp_float(pitch, -90.0F, 90.0F)};
}
```

**`centerRots()` 的 yaw 问题**

原 `centerRots()` 使用 `backwardsYaw = rotationYaw - hardcodedYaw()`,这是背对移动方向的 yaw,与被点击的方块无关。GrimAC 的 RotationPlace 检测会对这个 yaw 做 rayTrace 验证,背向 yaw 必然失败。`Center2` 改为使用 `blockRotations[0]`(由 `calculateCenter2ModeRotations()` 算出的精确 yaw),rayTrace 能命中目标面。

### Scaffold — 旋转速度滑块

新增 `rotationSpeed` SliderSetting(1–180 °/tick,默认 180),在 `handleV()` 中限制每 tick 的 yaw 变化量:

```java
float yawDifference = Utils.getAngleDifference(this.lastYawS, this.getSmooth);
float maxRotation = (float)this.rotationSpeed.getInput();
if (Math.abs(yawDifference) > maxRotation) {
    yawDifference = Math.copySign(maxRotation, yawDifference);
}
this.getSmooth = this.lastYawS + yawDifference;
```

### 构建排错

**brace mismatch 排查方法**:错误 "需要class, interface或enum" 出现在方法体语句上,意味着该语句处于类级别而非方法内部——即前面某个方法缺少开头的 `{` 或多了一个 `}`。定位方法:找到第一条报错行,向上找最近一个应有方法签名的位置,检查签名是否完整。

本次具体原因:Edit 操作的 `old_string` 只匹配了 `private void centerRots(ClientRotationEvent e) {` 这一行,`new_string` 末尾重复写了这行作为新方法的开头,但实际替换后原方法签名消失,方法体变成孤立代码。修复时只需在孤立方法体前补回签名即可。
