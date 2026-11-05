# No Slime Spawning（史莱姆不生成）

一个 **Minecraft 1.21.1 / NeoForge 21.1.x** 的轻量模组：往游戏规则里新增一条 `doSlimeSpawning`。

```
/gamerule doSlimeSpawning false    # 关闭：史莱姆不再生成
/gamerule doSlimeSpawning true     # 开启：恢复原版行为（默认值）
```

规则是**每个世界独立保存**的（和其他 gamerule 一样写进 level.dat），也会出现在「编辑游戏规则」界面和 `/gamerule` 命令补全里，无需额外配置文件。

## 关闭后会发生什么

拦截的是「新的史莱姆实体进入世界」这一动作，因此以下路径全部被禁：

- 沼泽、史莱姆区块等**自然生成**
- 结构生成、刷怪笼、刷怪蛋
- `/summon minecraft:slime`
- 大史莱姆死亡时**分裂**出的小史莱姆

**不受影响**：岩浆怪（Magma Cube）；存档里**已经存在**的史莱姆（从磁盘加载时会保留）；其他任何生物。

> 如果你想要的是原版 `doMobSpawning` 那种「只禁自然生成、命令和刷怪笼仍可用」的语义，看下面「自定义」一节。

## 环境要求

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.x（编译目标 21.1.1，运行范围 `[21.1.1,)`） |
| Java | 21 |
| 构建工具 | Gradle 8.8+（或 IntelliJ IDEA） |

## 构建

本项目**不含 `gradle-wrapper.jar`**（避免塞二进制文件），请用以下任一方式：

1. **IntelliJ IDEA**：直接 `File → Open` 打开项目根目录，IDEA 会按 `gradle/wrapper/gradle-wrapper.properties` 自动补齐 Wrapper，然后运行 Gradle 任务 `build`。
2. **本机 Gradle**：确保 `JAVA_HOME` 指向 JDK 21，执行 `gradle build`。
3. **已有 NeoForge MDK 1.21.1 工程**：把本项目的 `src/`、`build.gradle`、`settings.gradle`、`gradle.properties` 覆盖过去即可，用 MDK 自带的 `gradlew build`。

构建时需要能访问 `https://maven.neoforged.net/releases`（下载 NeoForge 与 ModDevGradle）。产物：`build/libs/noslime-1.0.0.jar`。

也可以用 `gradle runServer` 起一个专用服务端，用 `gradle runClient` 起客户端来实测。

## 安装

把 jar 放进 `mods/` 文件夹。**只装在服务端就够了**（游戏规则由服务端裁决），单人游戏则装在客户端即可。原版客户端可以直接连接装了本模组的服务端。

## 目录结构

```
src/main/java/com/example/noslime/
├── NoSlimeMod.java        # 注册 gamerule、读取规则值
└── SlimeSpawnBlocker.java # 监听 EntityJoinLevelEvent 拦截史莱姆
src/main/templates/META-INF/neoforge.mods.toml  # 模组元数据（构建时替换占位符）
src/main/resources/
├── pack.mcmeta
└── assets/noslime/lang/   # 游戏规则在 GUI 里的中英文名称与说明
```

零 Mixin、零配置文件、不改动任何原版数据，靠一个事件 + 一条游戏规则实现。

## 实现原理

1. `GameRules.register("doSlimeSpawning", Category.SPAWNING, BooleanValue.create(true))` 放在主类的**静态字段**里。必须在任何 `GameRules` 实例创建之前完成，之后每个世界都会自带这条规则，并随存档持久化。
2. NeoForge 的 `EntityJoinLevelEvent` 覆盖了自然生成、刷怪笼、刷怪蛋、命令、分裂、世界生成等所有实体加入路径；取消事件后实体根本不会被加入世界。
3. 只在服务端（`ServerLevel`）取消，客户端不拦，避免出现「客户端看不见但服务端存在」的幽灵实体。
4. `loadedFromDisk()` 为 true 时放行，避免把存档里已有的史莱姆吞掉；`NoSlimeMod.isSlimeSpawningAllowed()` 对异常时序做了回退保护，规则缺失时按原版行为处理。

## 自定义

- **改规则名**：改 `NoSlimeMod.RULE_NAME`，同时把 `assets/noslime/lang/*.json` 里的两个键一起改掉。
- **改默认值为 false**（装了就直接不生成）：把 `BooleanValue.create(true)` 改成 `create(false)`。
- **连已有的史莱姆也清掉**：删掉 `SlimeSpawnBlocker` 里 `event.loadedFromDisk()` 那一段判断。
- **只禁自然生成**：把 `SlimeSpawnBlocker` 换成生成判定层面的拦截 —— 例如用 Mixin 在 `Slime.checkSlimeSpawnRules(...)` 的 HEAD 处 `cancellable` 注入，规则为 false 时 `setReturnValue(false)`；或用 `RegisterSpawnPlacementsEvent` 给 `EntityType.SLIME` 追加一个 AND 判定。这样 `/summon`、刷怪笼、分裂仍然可用，语义更接近原版 `doMobSpawning`。
- **改 mod id / 包名**：同步修改 `gradle.properties` 的 `mod_id`、`mod_group_id`、Java 包名、`@Mod` 常量、`@EventBusSubscriber(modid=...)`、`assets/` 下的命名空间目录以及 `pack.mcmeta`。

## 常见问题

- **编译报 `GameRules.register` / `BooleanValue.create` 不可见**：确认用的是 NeoForge 21.1.x 的开发环境（NeoForge 自带的 access transformer 会把它们公开），而不是纯净的原版反编译工程。
- **`gradle` 找不到 JDK 21**：确认 `JAVA_HOME`，或让 IDEA / `foojay-resolver-convention` 自动下载 toolchain。若 `settings.gradle` 里的 foojay 插件拉不下来，删掉整个 `plugins { ... }` 块并手动装好 JDK 21 即可。
- **Gradle 版本不兼容**：`gradle/wrapper/gradle-wrapper.properties` 默认 8.10；若 ModDevGradle 报错，改用本机 Gradle 8.8 或 9.x 试一次。
- **装了模组但 `/gamerule` 里看不到规则**：确认 jar 是从 `build/libs` 取的最新构建，且服务端日志里有 `[NoSlime] gamerule doSlimeSpawning registered`。
