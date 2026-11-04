# 术语表（项目内约定）

| 简称 | 全称 | 在本项目里的角色 |
|---|---|---|
| **shi** | Shizuku | 提供 **ADB shell 级**的通道（uid 2000）。能跑 `pm` / `cmd` / `dpm` 这类只有 shell 能执行的命令。 |
| **she** | Shevery | 提供 **Device Owner** 通道，并兼容 Dhizuku 协议；可把委派范围（delegated scopes）授给普通应用。 |
| **dhi** | Dhizuku | 同上，官方实现。she 是它的兼容替代品。 |

> 以后文档、代码注释、issue 一律用 she / shi / dhi，不再写全称。

## 两条通道各自能给什么

| 能力 | shi（shell） | she/dhi（DO 委派） |
|---|---|---|
| 创建受管用户 / 工作资料 | ✅ `pm create-user --profileOf … --managed` | ❌ 需 MANAGE_USERS，DO 不自带 |
| 把应用装进指定用户 | ✅ `pm install-existing --user` | ❌ |
| 指定 Profile Owner | ✅ `dpm set-profile-owner --user` | ❌ `setProfileOwner` 是 @SystemApi |
| 冻结 / 暂停应用 | ✅ `pm suspend` | ✅ `DELEGATION_PACKAGE_ACCESS` |
| 禁止卸载 | ✅ | ✅ `DELEGATION_BLOCK_UNINSTALL` |
| 运行时权限授予/撤销 | ✅ `pm grant/revoke` | ✅ `DELEGATION_PERMISSION_GRANT` |
| 跨用户启动应用 | ✅ `am start-user` / `cmd package` | ❌ |

结论：**炼妖壶（工作资料隔离）只能靠 shi 那条通道建起来**，
she/dhi 负责建好之后的日常管控。两条通道不是二选一，是分工。

## 产品目标（一句话）

把"妖怪"（不想让它看见主空间数据、也不想让它随便跑的应用）关进一个独立的空间里，
开门、关门、喂食都由本应用统一管；权限来源可以是 she、dhi 或 shi，缺一个也能降级运行。
