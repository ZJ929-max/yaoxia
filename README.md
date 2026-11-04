# OpenSandbox

适配 Dhizuku 协议（含 Shevery 兼容实现）的 Android 沙盒工具，v0.5.0 起按分层架构组织。

## 分层

```
ui/          MainActivity, AppListAdapter, AppRow

apps/        AppCatalog（有哪些应用）
             AppControl（接口：冻结/禁止卸载）
             PolicyAppControl（DevicePolicyManager 实现）
             FrozenStore（本地状态持久化）

delegation/  DelegationService（接口：连接/授权/委派范围）
             DhizukuDelegationService（协议实现）
             DelegationScopes（委派范围单一来源）
             DhizukuProtocol（协议名单一来源）

core/        Outcome（结果类型）
             AsyncTaskRunner（唯一线程边界）
```

依赖方向只有一个：`ui → apps / delegation → core`。
`apps` 不知道 `Dhizuku` 的存在，`delegation` 也不知道 `DevicePolicyManager` 的管控细节。

## 三条约束怎么落的

**高内聚**：每个类只有一个变化理由。`PolicyAppControl` 只处理 DPM 调用与
"admin 组件 / null"的回退差异；`AppCatalog` 只查询启动器可见应用；
`AppListAdapter` 只负责绘制，不含任何业务判断。

**低耦合**：界面依赖 `DelegationService` / `AppControl` 两个接口。
要支持新的设备所有者实现，只需新增一个 `DelegationService` 实现类，
在 `MainActivity` 一行替换，其余代码不动。

**零硬编码**：
- 协议字符串（官方包名、权威名后缀）集中在 `DhizukuProtocol`，由包名推导；
- 委派范围集中在 `DelegationScopes`，界面从 `supportedScopes()` 取；
- 所有 UI 文案在 `res/values/strings.xml`；
- 尺寸与颜色在 `dimens.xml` / `colors.xml`；
- 版本号用 `BuildConfig.VERSION_NAME`；
- admin 组件由 `new ComponentName(context, SandboxAdmin.class)` 推导，不写字面量。

> 说明：协议名不可能"不存在"，零硬编码的工程含义是**每个字面量只出现一次，且由结构推导**。
> 全仓库扫描确认：`com.rosan.dhizuku` 等字面量只出现在 `DhizukuProtocol.java`。

## 线程模型

所有 binder / PackageManager 调用都在 `AsyncTaskRunner` 的单线程池里执行，
结果统一回到主线程再更新 UI；`MainActivity` 里没有任何 `new Thread()` 或 `Executor`。

## 构建

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64   # Dhizuku-API 2.5.4 是 Java 21 字节码
export ANDROID_HOME=/opt/android-sdk
/opt/gradle-8.7/bin/gradle assembleRelease
```

签名用 `keystore.properties` + `keystore.jks`（在 `.gitignore` 里）。
**丢了就无法覆盖升级**，请自行备份。

## 版本号策略

早期开发最容易浪费的就是版本号。这里的约定是**语义版本与构建号分离**：

| 字段 | 规则 | 写在哪 |
|---|---|---|
| `versionName` | 只在功能变化时改，格式 `0.MINOR.PATCH` | `version.properties` |
| `versionCode` | 每次构建 +1，**永不回退** | `version.properties` |

- 单一来源：`version.properties`，`build.gradle.kts` 读取，代码里不再出现版本字面量；
- 构建入口：`tools/release.sh`（无参 = 只涨构建号；带参 = 同时改版本名）；
- **不能回退**：Android 拒绝安装 `versionCode` 比已装版本低的应用
  （`INSTALL_FAILED_VERSION_DOWNGRADE`）。所以“把号吐出来”只能靠卸载重装，
  且只在本机自用、没发布过的情况下成立。
