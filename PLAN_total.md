# legado2 参考 `legado-with-MD3-main` 的 MD3 迁移计划

## Summary
以“先换底座、再收口主题、最后迁核心页面”的顺序推进，不直接照搬参考仓库，而是抽取其有效结构并兼容 `legado2` 现有能力。目标是在不打断现有功能的前提下，把当前“`AppCompat + ThemeStore + 局部 Compose MD3`”过渡到“`Material 3 主题底座 + 统一色彩语义 + 分阶段页面迁移`”。

默认策略：
- 不做一次性大迁移。
- 不先追参考仓库的全部新依赖和高版本前提。
- 先保留现有业务结构、导航结构、配置入口和主题配置数据。
- 参考仓库只作为架构和主题实现样板，不作为代码直接同步源。

## Key Changes

### 1. 主题底座阶段
- 把应用基础主题从 `Theme.AppCompat.DayNight.NoActionBar` 迁到 Material 3 基础主题，优先落在当前主题入口和基础 Activity 上。
- 以当前 [BaseActivity.kt](/Users/changlepan/StudioProjects/legado2/app/src/main/java/io/legado/app/base/BaseActivity.kt) 为切入点，对齐参考仓库的“系统栏、动态色、主题选择”职责分配，但保留 `legado2` 现有生命周期与沉浸式逻辑。
- 新增一层“MD3 语义色映射”，把现有 `ThemeStore` 中的 `primary/background/accent/navigation/status/text` 映射到 `colorPrimary / colorSurface / colorSurfaceContainer / colorOnSurface / colorOutline` 等 MD3 roles。
- 首阶段不移除 `ThemeStore`，而是把它降级为“主题数据来源”，不再让 View 代码直接依赖裸色值做样式决策。
- 动态取色仅作为可选主题模式接入；若设备或配置不支持，回退到现有主题配置生成的静态色板。

### 2. 主题模型收口阶段
- 合并当前 View 系主题和 Compose 系主题的配色来源，避免 [Theme.kt](/Users/changlepan/StudioProjects/legado2/app/src/main/java/io/legado/app/ui/compose/theme/Theme.kt) 与传统主题各自维护一套逻辑。
- 建立统一的主题解析器，输入仍然是现有偏好项和主题配置，输出统一的：
  - View 主题属性
  - Compose `ColorScheme`
  - 系统栏颜色
  - 扩展语义色（success、warning、reader 特殊色）
- 保留现有“自定义主题、背景图、透明模式、纯黑模式”能力，但定义固定优先级：
  1. 透明/背景图模式决定 surface/background 是否透明
  2. 纯黑模式只覆盖深色主题下的背景相关 roles
  3. 自定义主题色生成基础色板
  4. 动态色仅在选中动态主题时生效
- `ThemeConfig` 的外部存储结构先不改；内部新增转换层，避免立即触碰导入/导出兼容性。

### 3. 基础组件与容器阶段
- 优先替换全局影响最大的容器组件，而不是先改零散页面：
  - Toolbar/TitleBar 体系切到 Material 3 风格组件和色彩语义
  - Bottom navigation 切到标准 `NavigationBar` 语义与样式
  - Dialog/BottomSheet 统一切到 Material 风格实现
  - 常用输入组件、Tab、Card、FAB、Snackbar 使用 MD3 token
- 对现有自定义 View 不做大规模重写；先改它们读取颜色和形状的方式，使其来自统一主题接口。
- 对直接写死 `primaryColor/backgroundColor/accentColor` 的页面，优先改成读语义 token，再决定是否换组件。

### 4. 页面迁移阶段
按“高可见度 + 低业务风险”顺序迁移，顺序固定如下：
1. 主题设置页：用于验证主题模型是否闭环。
2. 主界面容器：底部导航、顶层背景、宿主 Fragment/Compose 容器。
3. 我的页面、探索页、RSS 页：视觉收益高，阅读链路风险低。
4. 书源管理/规则管理等已部分 Compose 化页面：用统一主题收口。
5. 阅读相关页面：最后做，只做样式与容器层调整，不先动分页/渲染逻辑。

迁移原则：
- 页面先“换主题与容器”，后“换布局语言”。
- Fragment 页面允许继续存在，不要求立即全面 Compose 化。
- 只有当页面已经有 Compose 容器或明显收益时，才参考样板仓库改成更完整的 Compose Screen。

### 5. 依赖与版本策略
- 不完整复制参考仓库依赖树。
- 首阶段仅引入当前迁移必需依赖；不因主题迁移同时引入与业务无关的 Koin、Haze、Adaptive、Expressive 特性。
- Material 3 Expressive 相关能力默认不作为第一阶段必选项；除非现有依赖已经稳定支持，否则先用稳定 MD3 token 和组件完成统一。
- 保持 `legado2` 当前 `minSdk`、业务模块结构、数据层和导航组织不变，避免把 UI 迁移和平台升级绑定。

## Public APIs / Interfaces / Types
- 新增统一主题解析接口，作为 View 和 Compose 的唯一主题入口；外部输入继续使用现有 `ThemeConfig`/偏好项，输出统一主题对象。
- `ThemeStore` 对外接口暂不删除，但标记为兼容层；新代码禁止直接依赖其裸色 getter。
- Compose 主题入口保留一个统一公共函数，替代当前分散的主题计算逻辑。
- 系统栏着色、背景透明、纯黑模式、背景图模式统一走主题解析结果，不允许页面自行拼装颜色规则。

## Test Plan
### 主题与兼容性
- 亮色、暗色、纯黑、自定义主题、动态色、透明主题、背景图主题分别启动应用，检查主界面、设置页、弹窗、底部导航颜色正确。
- 导入/导出主题配置后，应用结果与导入前一致。
- 旧用户升级后，不重置主题，不出现白底白字或黑底黑字。

### 页面与组件
- 主界面切页、返回、旋转、多窗口、沉浸式状态栏场景下，导航栏和状态栏颜色稳定。
- Toolbar、Tab、Dialog、BottomSheet、Snackbar、FAB、输入框在明暗主题下对比度合格。
- Fragment 容器页与 Compose 页混用时，背景与系统栏不闪烁。

### 阅读与高风险路径
- 进入阅读页、目录页、书籍详情页时，不因主题迁移导致崩溃或触控区异常。
- 阅读页保留原有背景图、透明状态栏、导航栏隐藏等行为。
- 低版本设备上动态色正确回退，不因缺失 API 崩溃。

## Assumptions
- 默认以参考仓库的“结构思路”借鉴，不复制其完整 UI 风格、依赖升级和页面重写方案。
- 第一阶段不做数据库、业务逻辑、导航架构重构。
- 第一阶段不要求所有 XML 页面立刻 Material 化，只要求先接入统一主题语义。
- 第一阶段不启用参考仓库里的更激进 Expressive/Glass/Adaptive 视觉方案，避免迁移复杂度失控。
- 后续实际实施时，以“每阶段可独立提交、可回归验证、可中途停止”为交付标准。
