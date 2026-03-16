# Compose Material 3 Screen Spec

这套规范用于当前 Kotlin + Jetpack Compose 的 Material Design 3 迁移。
当前基准风格以“书源管理页”的扁平、效率优先风格为主，而不是大面积概览卡或浮岛。

## 页面分层

1. `沉浸态`
   - 典型页面：阅读界面
   - 允许浮岛、半透明遮罩、悬浮控制条
   - 不要求与管理页使用相同的容器密度

2. `浏览态`
   - 典型页面：书架、目录
   - 页面骨架保持扁平，优先标题栏、筛选栏和列表内容的连续性
   - 只有确实需要强调的信息才使用容器

3. `管理态`
   - 典型页面：书源管理、规则管理、设置
   - 优先信息密度和批量操作效率
   - 使用统一的页面头部和列表行容器，不把每个动作都做成单独浮岛

## 容器规则

- 页面头部：使用贴边 `surface` 容器，承载标题、搜索和筛选。
- 页面主体：使用 `background` 作为底色，横向留白统一为 `16dp`。
- 区块容器：默认使用低存在感的 `surface` 分组，不默认上大圆角高海拔卡片。
- 列表行：优先连续排列，通过留白、分组标题、选中态和分隔来组织信息。
- 操作条：顶栏和底栏保持贴边 `surface`，不要与阅读页的悬浮浮岛混用。

## Token

- 横向留白：`LegadoPageDefaults.HorizontalPadding`
- 区块间距：`LegadoPageDefaults.SectionSpacing`
- 区块内边距：`LegadoPageDefaults.SectionInnerPadding`
- 紧凑项间距：`LegadoPageDefaults.CompactItemSpacing`

## 实施要求

- 组件优先使用 `MaterialTheme.shapes`，避免再次写散圆角。
- 同一页面内，优先统一容器语法，再微调颜色和动效。
- 阅读页保持沉浸态例外，不强行与管理页统一成同一种“浮岛密度”。
