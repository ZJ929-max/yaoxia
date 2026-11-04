# OpenSandbox（v0.1.0）

一个面向 **Dhizuku** 的 Android 开源沙盒工具。思路参考 Shelter / Insular 的"隔离"目标，
但权限来源换成 Dhizuku 的 Device Owner 转授——用户不用跑 `adb dpm set-device-owner`。

本仓库代码为原创，**没有复制 Shelter / Insular 的任何代码**（它们都是 GPLv3）。
目前你可以自选许可证；一旦日后搬运它们的代码，整个项目必须转为 GPLv3 并公开源码。

---

## v0.1.0 能做什么

1. 连接 Dhizuku（`Dhizuku.init`），弹出授权请求，等待用户允许。
2. 授权后，把 `RemoteDpmService` **注入 Dhizuku 自己的进程**运行
   （Dhizuku 用 `createPackageContext + Class.forName` 反射创建它）。
   因此这个服务里调用 `DevicePolicyManager` 的身份是 Dhizuku 本身，权限检查才通得过。
3. 列出带启动图标的应用，点一下即 **冻结 / 解冻**（`setPackagesSuspended`）。
   冻结后应用图标变灰、无法启动、后台也被停掉——这是最基础的一层沙盒。
4. 冻结状态落盘在 SharedPreferences：即使应用从启动器消失，也能在这里解冻回来。

## v0.1.0 还做不到

- 创建工作资料（Work Profile）并把应用装进去——需要 `createProfile` / `setProfileOwner`，
  属于 v0.2。
- 跨用户安装、应用克隆、按应用断网等。
- **未在真机上验证过**。它只通过了编译与签名校验，实际行为要你在手机上跑一遍才知道。

---

## 安装步骤

1. 手机装好 **Dhizuku**，并用 **Shizuku**（ADB 或 root）把它激活成 Device Owner。
2. 安装 `OpenSandbox-0.1.0.apk`（本仓库根目录外，或 `app/build/outputs/apk/release/`）。
3. 打开应用 → 点"连接 Dhizuku 并授权" → 在 Dhizuku 弹窗里点允许。
4. 状态栏显示"沙盒引擎已就绪"后，列表出现，点任意应用即可冻结。

> 装不上/闪退：把状态栏文字或 `adb logcat | grep opensandbox` 的输出发我。

---

## 工程结构

```
app/src/main/aidl/com/opensandbox/dhizuku/IRemoteDpm.aidl   跨进程接口（事务号写死）
app/src/main/java/com/opensandbox/dhizuku/RemoteDpmService.java  跑在 Dhizuku 进程里的特权端
app/src/main/java/com/opensandbox/dhizuku/MainActivity.java       本应用界面（纯原生 View，无 AndroidX）
```

为什么 AIDL 的事务号要写死：Dhizuku 的 `UserService.startService` 会向你的 binder 发
`FIRST_CALL_TRANSACTION + 1`，销毁时发 `+2`。所以第 1、2 号方法必须是"创建/销毁"钩子。

## 依赖

- `io.github.iamr0s:Dhizuku-API:2.4`（MIT）。选 2.4 是因为它的 class 文件是 Java 8 字节码；
  2.5.4 起是 Java 21 字节码，2.6.0 起 AAR 还要求 `compileSdk 37`。

## 构建

```bash
export ANDROID_HOME=/opt/android-sdk
/opt/gradle-8.7/bin/gradle assembleRelease
```

签名用 `keystore.properties` + `keystore.jks`（已在 `.gitignore` 中）。
**这两个文件丢了，以后就无法覆盖安装升级**，请自己备份一份到安全的地方。

## 路线图

- v0.2：通过 Dhizuku 创建 / 接管工作资料，列出资料内的应用
- v0.3：把应用装进工作资料、跨资料启动
- v0.4：冻结策略组、开机自动恢复、导出/导入配置
