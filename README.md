# 妖匣 / OpenSandbox（重建中）

> ⚠ **完整旧源码已恢复**：`/workspace/old/opensandbox`（43 个 Kotlin / 3165 行，git 历史到 `704d686`，含 `f3c5354`；工作树 `0.0.1-dev.13`）。
> 当前这棵树是 **38 文件 / 2007 行的重建骨架**（`0.0.2-dev.1`），不是完整旧源码；重建/合并以 old 为准。
> 先读：`/workspace/000-必读-抢救残片.md`。

## 当前骨架

- `versionName=0.0.2-dev.1`，`versionCode=1`（建议回到 old 的 `0.0.1-dev.13`）
- AGP 9.5.0 / Kotlin 2.4.21（内置）/ compileSdk 37 / minSdk 28
- 目标：dhi/she/shi 统一兼容层；工作资料已被 AOSP 证伪，实际按“完整次级用户沙箱”做

## 重建原则

- 以 old 为主项目，保留 `.git` 历史；当前骨架只取独有修复。
- 先恢复 dhi 策略 + shi shell；she 用官方 client API，不要 `Shizuku.onBinderReceived`。
- `am start --user` 不会切前台，打开应用要用 `am start-user; am switch-user; am start`。
- 建隔离空间后、重启后都要自动 `refresh`。
