# FloatingTimer 悬浮倒计时助手

FloatingTimer 是一款轻量、自定义度高的 Android 悬浮倒计时助手。参考经典屏幕助手 (MIPop) 交互设计，提供悬浮球置顶、贴边隐藏与拉出、短按叠加倒计时、长按额度菜单、自定义 Beep 蜂鸣/震动/抖动/闪烁提醒等丰富功能。

---

## ✨ 核心特性

- **悬浮球与贴边隐藏**：
  - 半透明悬浮球实时显示剩余倒计时秒数，可开启环形进度条与高亮背景切换。
  - 手动拖至屏幕边缘自动折叠为半透明贴边抓手（Teal 圆角样式）。
  - 贴边状态从边缘向屏幕内拉动直接还原为悬浮球并停留在拉出位置。
- **倒计时与加时机制**：
  - 短按悬浮球直接启动/按选定额度叠加倒计时（如点击两次 15s 额度增加至 30s）。
  - 长按悬浮球弹出额度选择气泡菜单，快速切换 5s/10s/15s/20s/30s/60s 及自定义额度。
- **多样化提醒与测试**：
  - 声音提醒（自定义 Beep 蜂鸣时长与音量）。
  - 震动提醒（自定义震动时长与强度）。
  - 视觉提醒（倒计时结束悬浮球抖动 Shake 与闪烁 Flash 动画）。
  - 主界面提供一键“⚡ 测试倒计时结束提醒与动画”预览按钮。
- **主题与自定义额度**：
  - 支持日间/夜间/跟随系统主题（默认夜间深色模式）。
  - 支持动态添加/删除任意加时秒数额度。
- **离线日志与调试 (AppLogger)**：
  - 内置全局崩溃捕获与日志记录，主界面支持一键“复制运行日志”。

---

## 📖 需求与规格说明

完整的产品需求规格、交互细节与架构设计请参阅：[REQUIREMENTS.md](REQUIREMENTS.md)。

---

## 🚀 CI/CD 自动构建与发布

本项目已配置 **GitHub Actions** 工作流（`.github/workflows/build.yml`）：
- 每次 `push` 代码至 `main` 或 `master` 分支时自动触发构建。
- 自动使用 Gradle 编译 Debug 与 Release APK，并自动创建 GitHub Release 上传 `FloatingTimer.apk` 作为 Release Asset。

---

## ⚙️ GitHub 仓库 Releases 权限配置指南 (解决 403 报错)

GitHub 仓库默认对 GitHub Actions 自动生成的 `GITHUB_TOKEN` 限制为只读权限（Read repository contents），因此自动创建 Release 会触发 `Resource not accessible by integration (403)` 错误。

请按以下步骤开启仓库写权限：
1. 打开您的 GitHub 仓库 `jiafjioajfijaiofnkan/floatingtimer`。
2. 进入 **Settings** -> **Actions** -> **General**。
3. 滚动到页面底部的 **Workflow permissions** 区域。
4. 将选项从默认的 *Read repository contents permission* 切换为 **Read and write permissions**。
5. 勾选下方 **Allow GitHub Actions to create and approve pull requests**（可选）。
6. 点击 **Save** 保存设置。

保存后，后续每次 Commit/Push 构建均可成功在 Releases 页面生成对应版本的 Tag & Release Asset！
