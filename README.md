# Legado Frontend M3 Remix

基于 [Legado / 开源阅读](https://github.com/5151561/legado) 的前端改造实验项目。

这个仓库的重点不是重写 Legado 的阅读内核，而是：

- 使用 `Kotlin + Jetpack Compose` 逐步替换原有 Android 前端界面
- 以 `Material 3` 为主要设计语言重做视觉和交互层
- 尽量复用原项目已经很成熟的阅读、书源、目录、替换规则、订阅和数据流逻辑
- 用 AI 辅助完成一轮偏实验性质的 `vibe coding` 式界面迁移

如果你熟悉原版 Legado，可以把这个仓库理解为：

> 在尽量不动核心能力的前提下，对 Legado 做一次 Compose 化、M3 化、AI 协作式的前端重构尝试。

## 项目定位

本项目当前关注的是前端体验改造，而不是另起炉灶做一个全新的阅读器。

因此整体策略是：

- 保留原 Legado 的核心业务能力和数据结构
- 优先替换书架、我的、目录、阅读菜单等用户高频触达界面
- 使用 Compose 承接新的界面表达
- 在接入已有后端逻辑的同时，避免破坏原有阅读功能链路

目前你在仓库里看到的改造方向，主要集中在：

- Material 3 风格主题和配色
- Compose 化的主界面和局部页面
- 逐步替换 XML/View/Fragment 时代的前端壳层
- 在不丢功能入口的前提下重做阅读菜单与交互结构

## 技术说明

本次改造的关键词是：

- `Kotlin`
- `Jetpack Compose`
- `Material 3`
- `AI-assisted vibe coding`

这里说的 vibe coding，不是无约束地“让 AI 随便生成界面”，而是：

- 先确定迁移目标和交互语义
- 再让 AI 辅助重构 Compose UI
- 同时把现有业务回调、状态和后端逻辑重新接回去
- 边改边编译、边验证、边修正

也就是说，这个仓库既是一个 UI 改造项目，也是一次 AI 协作开发实践。

## 与原 Legado 的关系

本仓库是建立在原版 Legado 基础上的前端改造分支，不是独立重写，也不是官方新版本说明页。

需要特别说明的是：

- 原始能力、工程结构和大量业务代码来自 Legado
- 本仓库主要修改的是前端界面层与部分交互组织方式
- 阅读器核心能力仍然高度依赖原项目已有实现
- 如果你想了解完整功能、书源规则、社区和正式说明，应优先参考原项目文档

## 原 Legado 项目简介

Legado / 开源阅读 是一个 Android 平台上的开源阅读项目。按原项目 README 的定位，它是一个自由、开源的小说阅读器。

原项目的一些核心特性包括：

- 自定义书源与规则解析
- 搜索、发现和订阅内容
- 本地 `TXT`、`EPUB` 阅读
- 替换净化、目录规则、朗读等扩展能力
- 高度自定义的阅读界面
- 多种翻页模式和阅读排版设置

需要注意的是，Legado 本身不直接提供内容，通常需要用户自行导入书源、订阅源或本地书籍。

## 为什么要做这次改造

原版 Legado 的功能成熟度很高，但部分前端界面仍然保留了较强的传统 Android View 时代痕迹。

这次改造的目标主要是：

- 用更现代的 Compose 架构承接新界面
- 把视觉语言统一到 Material 3
- 保留原项目高完成度的后端与阅读能力
- 验证 AI 在复杂旧项目渐进式重构中的可行性

换句话说，这不是“复刻一个阅读”，而是“在 Legado 上做前端现代化迁移”。

## 原项目参考链接

以下内容都来自原 Legado 项目现有 README 或其关联官方链接，建议一并阅读：

- 原项目仓库: [https://github.com/5151561/legado](https://github.com/5151561/legado)
- 原项目主页: [https://gedoor.github.io](https://gedoor.github.io)
- 官方站点: [https://www.legado.top/](https://www.legado.top/)
- 官方帮助文档: [https://www.yuque.com/legado/wiki](https://www.yuque.com/legado/wiki)
- 社区说明: [https://www.yuque.com/legado/wiki/community](https://www.yuque.com/legado/wiki/community)
- 书源规则教程: [https://mgz0227.github.io/The-tutorial-of-Legado/](https://mgz0227.github.io/The-tutorial-of-Legado/)
- 原项目免责声明: [https://gedoor.github.io/Disclaimer](https://gedoor.github.io/Disclaimer)
- 原项目 Web 书架: [https://github.com/gedoor/legado_web_bookshelf](https://github.com/gedoor/legado_web_bookshelf)
- 原项目 Web 书源编辑器: [https://github.com/gedoor/legado_web_source_editor](https://github.com/gedoor/legado_web_source_editor)

仓库内也保留了一部分原项目帮助文档和说明文件，可从这里继续看：

- 应用帮助: [app/src/main/assets/web/help/md/appHelp.md](/Users/changlepan/StudioProjects/legado2/app/src/main/assets/web/help/md/appHelp.md)
- 阅读菜单帮助: [app/src/main/assets/web/help/md/readMenuHelp.md](/Users/changlepan/StudioProjects/legado2/app/src/main/assets/web/help/md/readMenuHelp.md)
- 更新日志: [app/src/main/assets/updateLog.md](/Users/changlepan/StudioProjects/legado2/app/src/main/assets/updateLog.md)

## 当前仓库的改造重点

这个分支目前更关注这些事情：

- Compose 化主界面结构
- Material 3 主题、容器、导航和阅读菜单风格统一
- 在不丢失原有功能的情况下替换旧前端实现
- 将新的 UI 继续接回 Legado 原有后端和阅读流程

如果你是为了使用稳定完整功能，请优先参考原项目。
如果你是为了研究 Compose 迁移、Material 3 改造、旧 Android 工程的 AI 协作重构，这个仓库更有参考价值。

## To-do / 迁移清单

下面这份清单描述的是这个仓库当前这轮前端改造的大致进度。

### 已完成

- [x] Material 3 风格主题与全局配色整理
- [x] 主界面底部导航与整体壳层风格调整
- [x] 书架页面 Compose 化迁移
- [x] 我的页面 Compose 化迁移
- [x] 目录页面 Compose 化迁移
- [x] 阅读菜单改造成 Material 3 风格 Compose 菜单层
- [x] 阅读菜单右上角更多功能入口恢复为 `ModalBottomSheet`
- [x] 新界面接回原有后端数据与主要交互逻辑
- [x] 发现页 Material 3 改造
- [x] RSS / 订阅页 Material 3 改造

### 进行中 / 待完成

- [ ] 阅读正文页面本体完全 Compose 化
- [ ] 搜索菜单与阅读内其它弹层统一成 Material 3 风格
- [ ] 阅读设置、翻页动画、排版配置等子面板继续重构
- [ ] 更多历史页面和设置入口的 Compose 化替换
- [ ] 视觉细节、动效和状态过渡继续收口

## 声明

- 本仓库是基于 Legado 的二次改造实验
- 原项目的功能设计、规则生态和大量业务实现应归功于 Legado 原作者与社区贡献者
- 本仓库的新增价值主要在于前端界面迁移、M3 化和 AI 协作改造过程
- 这个 readme 也是 AI 生成的///
