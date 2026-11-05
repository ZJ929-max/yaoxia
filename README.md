# OpenSandbox

适配 Dhizuku 协议（含 she 兼容实现）的 Android 沙盒工具。
目标：把 dhi / she / shi 三种权限来源统一到一层兼容层里，向"炼妖壶"式隔离演进。

## 技术栈（2026-11 升级后）

| 项 | 版本 |
|---|---|
| 语言 | **Kotlin 2.4.21**（源码 100% Kotlin，无 Java） |
| 构建 | AGP **9.5.0** + Gradle **9.8.2**（wrapper 自带） |
| SDK | compileSdk / targetSdk **37**，minSdk 28 |
| build-tools | **36.0.0**（x86_64，经 qemu 包装运行于 aarch64） |

> AGP 9 起 Kotlin 支持内置，**不再需要** `org.jetbrains.kotlin.android` 插件；
> 再手动加会直接报错。

## 分层

```
ui/          MainActivity, AppListAdapter, AppRow
apps/        AppCatalog（有哪些应用）· AppControl（接口）· PolicyAppControl · FrozenStore
channels/    PolicyChannel（接口）· DhizukuPolicyChannel · ShellChannel · ShizukuShellChannel
platform/    Capability · ProviderId · CapabilityProvider · CapabilityRouter · ProviderRegistry ★
delegation/  DelegationService · DelegationScopes · DhizukuProtocol
core/        Outcome · AsyncTaskRunner
```

依赖方向只有一条：`ui → apps / channels → platform / delegation → core`。

## 兼容层

`platform/ProviderRegistry.kt` 是**唯一**需要改的文件：

```kotlin
fun providers(context: Context) = listOf(
    DhizukuPolicyChannel(context, ProviderId.SHE, PACKAGE_SHE, DelegationScopes.ALL),
    DhizukuPolicyChannel(context, ProviderId.DHI, PACKAGE_DHI, DelegationScopes.ALL),
    ShizukuShellChannel(context)
)
```

- 顺序即优先级；`CapabilityRouter` 按「接口类型 + 可用性」挑通道；
- 不同能力可落到不同提供方（策略走 she、shell 走 shi）——即混合调用；
- 新增一家（新的权限授予器 / 新的 shell），只加一行。

## 构建

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64
export ANDROID_HOME=/workspace/.toolchain/android-sdk   # 见 local.properties
./gradlew assembleRelease
```

本机特点（aarch64 + proot）：

- 官方 build-tools 只发 x86_64，需要 `qemu-x86_64-static -L /workspace/.toolchain/amd64-root`
  包一层；包装脚本见 `/workspace/.toolchain/setup.sh`；
- `gradle.properties` 里 `android.aapt2FromMavenOverride` 指向包装后的 aapt2；
- Kotlin 编译守护进程在 proot 下会失败，已设 `kotlin.compiler.execution.strategy=in-process`。

## 版本号策略

- `versionCode` **固定为 1**：任意两版可互相覆盖安装（同签名）；
- `versionName` 只在功能变化时改；
- 唯一来源 `version.properties`，由 `build.gradle.kts` 读取。

## 已知限制

- 工作资料（真正的工作资料）无法由第三方 App 创建，需要 `MANAGE_USERS`（signature|privileged）；
  只能走 shi 的 shell 通道（`pm create-user --profileOf … --managed`）。
- 冻结/禁止卸载依赖 she/dhi 的委派范围；shizuku 通道目前只提供 shell 执行能力。
