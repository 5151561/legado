# 第三步：基础组件与容器阶段规划

## Summary
- 当前项目已经完成第二步的主题真值收口，可以进入第三步。
- 第三步不做大规模 Compose 重写，也不追求一次性把所有旧组件替换成 Material 3 原生组件。
- 第三步的目标是先统一全局容器和共享组件的标准，再批量清理页面里直接写死 `primaryColor/backgroundColor/accentColor` 的代码。
- 验收标准优先看语义一致和 token 接入，不看组件血统。

## Current State
- 主题真值与 compat bridge 已建立，`LegadoThemeState` 可同时服务 Compose、View 和系统栏。
- 首页底部导航已经先行切到 Compose `NavigationBar`，这部分应作为第三步的标准样板，而不是重做。
- 当前最大的技术债集中在：
  - `TitleBar/Toolbar` 体系仍大量手动 `setBackgroundColor(primaryColor)`
  - 复杂 Dialog 和表单弹窗数量多，容器风格不统一
  - `TextInputLayout`、`TabLayout`、`Snackbar`、`FAB` 仍缺少统一的 MD3 语义入口
  - 多个页面仍直接读取 `primaryColor/backgroundColor/accentColor/accentColor`

## 组件标准表

### 总原则
- 标准优先级：`LegadoThemeState -> ComponentTokens -> Component Style -> Page`
- Compose 场景优先采用 Material 3 原生组件。
- View 场景优先保留现有组件外壳，只统一 token、shape、state、elevation、ripple。
- 只有旧组件无法满足语义一致性时，才升级为 Material 组件替换。

| 组件类别 | 标准语义 | 默认实现策略 | 必须统一的 token | 何时必须替换原组件 | 当前项目建议 |
| --- | --- | --- | --- | --- | --- |
| `TitleBar / Toolbar` | `Top App Bar` | 保留现有 `TitleBar` 和 `Toolbar` 外壳，统一 style applier | `container` `content` `navigationIcon` `actionIcon` `title` `subtitle` `divider` `elevation` | 旧实现无法统一菜单 tint、icon tint、透明/滚动状态时 | 先不重写，做项目级 top app bar helper |
| Bottom navigation | `NavigationBar` | Compose 继续用 M3，旧 View 只做兼容封装 | `container` `activeIndicator` `selectedIcon` `selectedText` `unselectedIcon` `unselectedText` `badge` | 剩余旧 View 导航仍是主路径且无法对齐语义时 | 以 `MainActivityScreen` 为标准，不再扩展旧 `ThemeBottomNavigationVIew` |
| `Dialog` | `Basic Dialog / Alert Dialog` | 简单弹窗统一 Material 风格 builder，复杂弹窗继续走 `BaseDialogFragment` | `container` `onContainer` `headline` `supportingText` `action` `shape` `scrim` `elevation` | 页面仍在手动拼 `Toolbar + 背景色 + 按钮染色` 时 | 第三步重点治理对象 |
| `BottomSheet` | `Modal Bottom Sheet` | Compose 用 `ModalBottomSheet`，View 侧统一 sheet chrome | `sheetContainer` `sheetContent` `dragHandle` `shape` `scrim` `tonalElevation` | 旧实现不能统一 shape/insets/scrim/底部安全区时 | 阅读菜单可作为标准样板 |
| 输入组件 | `Text Field / Switch / Checkbox / Slider` | 保留现有自定义输入包装，但改成 token 化 | `focus` `outline` `cursor` `label` `supportingText` `error` `disabled` `thumb` `track` | 旧控件无法表达 focus/error/disabled 状态时 | 先统一 `TextInputLayout` 和通用 tint helper |
| `Tab` | `Primary/Secondary Tabs` | 先保留 `TabLayout`，通过 helper 统一 | `container` `indicator` `selectedText` `unselectedText` `divider` | 旧 `TabLayout` 选中态、滚动态和指示器无法稳定对齐时 | 第三步高优先级共享组件 |
| `Card` | `Card / Elevated Card / Outlined Card` | 保留 `CardView`/`MaterialCardView`，优先统一容器语义 | `container` `content` `outline` `elevation` `pressedOverlay` `shape` | 页面大量手动描边/背景，无法统一状态层时 | 先从共享卡片和弹窗容器入手 |
| `FAB` | `FAB / Small FAB / Extended FAB` | 保留 `FloatingActionButton`，统一 tint 和尺寸语义 | `container` `content` `pressed` `disabled` `elevation` | 需要 extended FAB 或多尺寸语义时 | 先统一 tint/ripple，再决定是否升级变体 |
| `Snackbar` | `Snackbar` | 保留 Material Snackbar，统一 style helper | `container` `content` `action` `dismiss` `shape` `elevation` | 页面直接 `Snackbar.make()` 且风格分散时 | 必须抽 helper，禁止页面裸调 |
| 菜单/Popup | `Menu / Dropdown` | 保留现有 popup/menu 机制，统一 overlay 与 token | `container` `text` `icon` `selected` `shape` `elevation` | 背景、文字、图标状态无法与主题一致时 | 跟 `Toolbar` 一起治理 |
| Preference 类组件 | `List Item / Setting Item` | 保留现有 preference 体系，统一 item token | `container` `headline` `supporting` `leadingIcon` `trailing` `divider` | 旧 preference 仍依赖 raw color 且状态混乱时 | 放在第三步后半段 |

### 组件达标规则
- 只要组件满足统一 token、shape、state、elevation、ripple，就算达标。
- 组件出现下列任一情况，就进入必须替换队列：
  - 不能统一默认、选中、禁用、focus、error、pressed 状态
  - 不能适配日夜、EInk、背景图
  - 不能与系统栏、标题栏、内容区形成一致语义
  - 仍必须依赖 `primaryColor/backgroundColor/accentColor` 这类原始色

## 第三步执行计划

### 3.1 容器骨架收口
- 建立内部 `LegadoComponentTokens`，它只由 `LegadoThemeState` 纯映射生成，不引入第二套主题真值。
- 统一 top app bar 标准：
  - `TitleBar` 保留现有自定义 View 形态，但切到 MD3 top app bar 语义。
  - `Toolbar` 新增统一 style helper，统一 background/content/icon/menu/divider/elevation。
  - `BaseFragment.setSupportToolbar()` 和所有菜单 tint 逻辑都走同一个 helper。
- 统一 Dialog 和 BottomSheet 的容器标准：
  - 简单确认/输入/列表弹窗统一走 Material 风格 builder/theme overlay。
  - 复杂弹窗继续基于 `BaseDialogFragment`，统一 shape、surface、title bar、scrim、insets 和底部安全区。
- 底部导航以现有 Compose `NavigationBar` 为标准实现；旧 View 导航只做兼容封装，不再继续扩展旧能力。

### 3.2 共享组件与 token 化
- 新增共享组件 style/applier，而不是让页面自己拼：
  - `TopAppBarStyleApplier`
  - `DialogChromeApplier`
  - `TextInputStyleApplier`
  - `TabLayoutStyleApplier`
  - `FabStyleApplier`
  - `SnackbarStyleApplier`
- 共享组件先改 token 接入：
  - `TextInputLayout` 改成读 input token，不再直读 `ThemeStore.accentColor`
  - `TintHelper` 改成围绕组件语义 token，而不是原始色
  - `Snackbar` 统一通过 helper 创建和着色
  - `TabLayout` 统一使用 helper 设置背景、文字、indicator
  - `FAB` 统一 tint、ripple、disabled、elevation
- 对现有自定义 View 不做大规模重写，只改颜色、shape、stroke、icon tint、ripple 的取值入口。

### 3.3 页面与家族批次迁移
- 第一批迁移的是高复用家族，不是零散业务页面：
  - `Toolbar + 列表/表单` 型 Dialog 家族
  - 各类导入/管理/编辑对话框家族
  - 带 `TabLayout` 的编辑页和对话框家族
  - 带 FAB、Snackbar、输入表单的搜索/配置/选择页
- 页面迁移顺序固定为：
  1. 先把页面里直接 `setBackgroundColor(primaryColor)`、`setSelectedTabIndicatorColor(accentColor)`、`Snackbar.make()` 的调用替换成共享 helper 或 token
  2. 再决定页面保留旧组件外壳还是顺手替到 Material 组件
  3. 只有共享 chrome 已接入的页面，才进入更深层的组件替换
- 阅读器正文绘制、分页和 Canvas 类自定义 View 不纳入第三步核心目标，只处理外围 chrome、弹窗、搜索栏、菜单栏和设置容器。

## APIs / Interfaces
- 保留现有主题公开 API，不新增新的主题入口。
- 新增内部组件层接口：
  - `LegadoComponentTokens`
  - `fun Context.legadoComponentTokens(): LegadoComponentTokens`
  - `fun Toolbar.applyLegadoTopAppBarStyle(...)`
  - `fun TitleBar.applyLegadoTopAppBarStyle(...)`
  - `fun TextInputLayout.applyLegadoInputStyle(...)`
  - `fun TabLayout.applyLegadoTabsStyle(...)`
  - `fun FloatingActionButton.applyLegadoFabStyle(...)`
  - `fun Snackbar.applyLegadoSnackbarStyle(...)`
  - `DialogChromeApplier`
- 所有接口都必须遵守单向映射：`LegadoThemeState -> token -> component`。

## Test Plan
- 组件映射单测：
  - top app bar、dialog、input、tab、fab、snackbar token 在日/夜、纯黑、背景图、EInk 下输出稳定。
- 集成回归：
  - 一个 `TitleBar` Activity
  - 一个 Toolbar 型 Dialog
  - 一个 `TabLayout` 页面
  - 一个表单页
  - 一个带 FAB/Snackbar 的页面
- 手工 smoke：
  - 切换日夜、EInk、背景图、透明状态栏
  - 打开至少 3 个复杂 Dialog、2 个带 Tab 的页面、1 个带输入表单的编辑页、1 个带 FAB 的页面
  - 检查 focus、disabled、pressed、error、badge、menu tint、底部安全区
- 构建验证：
  - 至少完成一次可用的 Kotlin 编译校验和相关页面验证，再进入大规模页面迁移

## Assumptions
- 第三步继续采用混合 View/Compose 路线，不做全局 Compose 化。
- `MainActivityScreen` 的 Compose `NavigationBar` 视为第三步已完成的标准样板。
- 第三步不要求把所有 `CardView`/`Toolbar`/`Dialog` 一次性替换成 Material 原生组件，要求是先统一共享容器和取色/取形逻辑。
- 阅读器正文绘制、自定义分页、Canvas 类控件不纳入第三步核心改造范围。
