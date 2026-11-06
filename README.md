# 妖匣（Yaoxia）

> 适配 **dhizuku / shevery / shizuku** 三种权限来源的 Android 应用隔离工具。
> 目标：用一层统一兼容层，做出「把应用关进独立空间」的沙盒体验。

许可证：**MPL-2.0**（见 [`LICENSE`](LICENSE)）

---

## 它做什么

- **应用冻结**：批量冻结/解冻（`setPackagesSuspended`），支持长按多选；
- **禁止卸载**：给关键应用加卸载锁（`setUninstallBlocked`）；
- **隔离空间**：创建一个独立用户空间，把应用装进去、在里面启动；
- **三种权限来源**：dhi（Dhizuku）、she（Shevery）、shi（Shizuku）自动选择并失败换道；
- **外观**：Material 3 / MIUI（Miuix）双皮肤 + 「梦幻泡泡」玻璃质感 + 三种字体可切换。

## 环境要求

| 项 | 要求 |
|---|---|
| Android | 8.0+（minSdk 28），建议 13+ 以获得完整折射效果 |
| 权限来源 | 任一：Dhizuku（设备所有者）、Shevery、Shizuku（已授权） |
| 构建 | JDK 21、Android SDK 37、build-tools 36 |

## 构建

```bash
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`

签名：把 `keystore.properties`（不入库）放到仓库根目录：

```properties
storeFile=keystore.jks
storePassword=***
keyAlias=***
keyPassword=***
```

## 自检（不看界面也能验证链路）

```bash
adb shell am start -n com.opensandbox.dhizuku/.ui.MainActivity --es action selftest
adb logcat -d | grep YaoxiaTest
```

覆盖：provider 可用性 → 委派连接 → 委派范围 → shell 通道 → `pm list users` 解析 →
隔离空间定位 → 空间内应用数 → 策略只读查询 → 应用列表加载。

## 架构

```
ui/          MainActivity · SandboxApp · 四个页面 · Glass / SkinComponents
apps/        AppCatalog（并行+缓存）· AppControl · PolicyAppControl · FrozenStore
channels/    PolicyChannel · ShellChannel · Dhizuku / Shevery / Shizuku 实现
platform/    Capability · ProviderId · CapabilityProvider · CapabilityRouter · ProviderRegistry ★
delegation/  DelegationService · DelegationScopes · DhizukuProtocol
provision/   ShellCommands · UserListParser · ProfileApps · Shell/Dhizuku Provisioner
core/        Outcome · AsyncTaskRunner · FailureReporter · SelfTest
logic/       纯 Kotlin 模块：ShellCommands / UserListParser（带 JUnit 测试）
```

依赖方向单向：`ui → apps / channels / provision → platform / delegation → core`。
`platform/ProviderRegistry.kt` 是**唯一**的通道注册表，加一行即接入新的权限来源。

### 关于「隔离空间」

Android 的**工作资料（managed profile）在第三方应用范围内无法创建**——需要
`MANAGE_PROFILE_AND_DEVICE_OWNERS`（signature 级）。因此本项目用**完整次级用户**做隔离：
打开其中的应用会切换前台用户、本应用被挂起，这是系统硬限制，界面上有明确提示。

## 测试

```bash
./gradlew :logic:test        # 纯逻辑单测
```

## 许可与致谢

- 本项目：MPL-2.0；
- 第三方依赖与设计参考（Island / Shelter / InstallerX 的边界说明）见 [`docs/ATTRIBUTION.md`](docs/ATTRIBUTION.md)；
- 「梦幻泡泡效果」的设计取舍见 [`docs/梦幻泡泡效果.md`](docs/梦幻泡泡效果.md)。
