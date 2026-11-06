# 第三方与参考来源

## 直接依赖（运行时）

| 项目 | 版本 | 许可证 | 用途 |
|---|---|---|---|
| Dhizuku-API | 2.5.4 | Apache-2.0 | 设备所有者能力（dhi 通道） |
| Shizuku API / provider | 13.0.0 | Apache-2.0 | shell 通道（shi / she 复用同一 SDK） |
| Backdrop | 2.0.1 | Apache-2.0 | 液态玻璃/折射绘制 |
| Kyant Shapes | 1.2.1 | Apache-2.0 | 折射所需的圆角 SDF 形状 |
| Miuix (miuix-ui) | 0.9.4 | Apache-2.0 | MIUI 风格组件 |
| Jetpack Compose | BOM 2026.09.00 | Apache-2.0 | UI 框架 |
| Roboto | 系统内置 | Apache-2.0 | 「默认字体」拉丁字形 |
| HarmonyOS Sans SC | 按需下载 | 华为字体许可 | 「鸿蒙字体」选项 |
| Noto Sans SC | 按需下载 | OFL-1.1 | 「默认字体」中文字形 |

字体均**不打包**进 APK，由用户在设置里主动下载到应用私有目录。

## 设计参考（未复制代码）

| 项目 | 许可证 | 参考内容 |
|---|---|---|
| Island（炼妖壶） | Apache-2.0 | 应用列表缓存与失效策略、批量操作的并发模型、`DevicePolicies` 式的能力收敛思路 |
| Shelter | GPL-3.0 | 仅阅读其工作资料方案，未使用任何代码 |
| InstallerX Revived | GPL-3.0 | 仅参考悬浮导航栏的视觉观感，未使用其代码 |
| AndroidLiquidGlass (Kyant0) | Apache-2.0 | 背景层采样 + 高光/阴影/色差的调用结构 |

## 说明

- 本项目采用 **MPL-2.0**（Mozilla Public License 2.0，见仓库根 `LICENSE`）。
- MPL-2.0 是**文件级** copyleft：本项目自己的源文件受 MPL 约束，第三方依赖保持各自许可证。
- Apache-2.0 依赖（Compose / Backdrop / Miuix / Dhizuku / Shizuku）与 MPL-2.0 兼容，可正常引用。
- **GPL-3.0 项目的代码仍然不得进入本仓库**（GPL 单向兼容 MPL，反向不允许）；
  Shelter、InstallerX 仅在 `docs/` 中记录观感参考，不构成衍生作品。
