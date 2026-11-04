# 适配 Shevery：为什么改架构（v0.3.0）

## 结论先行

Shevery 的"设备所有者 - 应用委派"只有 5 个范围，且**给不出工作资料**：

```
delegation-app-restrictions
delegation-block-uninstall
delegation-permission-grant
delegation-package-access
delegation-enable-system-app
```

`createProfile()` / `setProfileOwner()` 是 Device Owner 专属的隐藏 API，
委派机制无法把它们授出。所以"炼妖壶"那个**工作资料**隔离层，靠委派做不出来。

## Shevery 兼容 Dhizuku 的方式

- 它保留 Dhizuku 的 provider 协议，但**用自己包名的权威名**：
  `com.hamondev.shevery.dhizuku_server.provider`
- `getVersionCode() = 2000000000`、`getVersionName() = "14.6.2"`（这就是你看到的"Dhizuku 14.6.2"）
- 另外提供一个包名为 `com.rosan.dhizuku` 的 **stub**，把调用转发给上面那个权威名

因为 `Dhizuku.init()` 是"先找当前 Device Owner，再用它的包名拼权威名"，
所以同一份客户端代码可以同时适配官方 Dhizuku 和 Shevery —— **不需要改 API**。

## 在 Shevery 上跑不通的东西

| 能力 | 官方 Dhizuku | Shevery |
|---|---|---|
| `bindUserService`（把代码注入 DO 进程） | 支持 | **空实现，且抛 SecurityException** |
| `remoteProcess` | 支持 | 直接返回 null |
| `setDelegatedScopes` | 支持 | 支持（5 个范围） |

v0.1/v0.2 用的"注入 DO 进程再调 DevicePolicyManager"那套，在 Shevery 上必然失败。
这就是为什么要换成 v0.3 的委派模型。

## v0.3.0 的工作方式

1. `Dhizuku.init()` 自动锁定 Shevery；
2. 申请授权（Shevery 声明了 `com.hamondev.shevery.action.REQUEST_DHIZUKU_PERMISSION`）；
3. 调 `setDelegatedScopes` 把 5 个范围要到手；
4. 之后**用本应用自己的 UID** 调 `DevicePolicyManager`，参数里带上
   `SandboxAdmin` 这个 admin 组件做标识：

```java
dpm.setPackagesSuspended(admin, new String[]{pkg}, true);
dpm.setUninstallBlocked(admin, pkg, true);
```

失败时会回退到 `admin = null` 再试一次，两次的异常都会显示在 Toast 上。

## 下一步（按可行性排序）

1. **v0.4**：让"冻结"稳定下来——排查 `setPackagesSuspended` 在委派身份下是否需要
   额外的 `DELEGATION_PACKAGE_ACCESS`，以及 admin 参数该传什么。
2. **v0.5**：权限收紧（`DELEGATION_PERMISSION_GRANT`）——撤销应用的危险运行时权限，
   这是委派模型下最像"沙盒"的能力。
3. **工作资料**：只有三条路——官方 Dhizuku 当 DO（`createAndManageUser`）、
   Island 那种 ADB/root God mode、或系统自带的"添加工作资料"流程。
