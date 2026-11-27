# 智能便利贴 设计规范

更新：2026-11-27

## 方向
以「暖纸墨韵（方向 A）」电子纸记事本体验为核心：全屏无底栏极简外壳、纸面分组清单流与可定制纸张纹理，与极趣云电子墨水屏硬件保持一致的温润克制质感（用户确认）。

## 品牌资产（不动）
- 应用名称「智能便利贴」与应用图标 `ic_launcher`（用户确认）
- 极趣云平台与电子墨水屏硬件协同定位（用户确认）

## 语义角色
所有主题令牌集中在 `app/src/main/java/com/passheep/sticky_note/ui/theme/Theme.kt`（`StickyNoteTokens`）与 `app/src/main/java/com/passheep/sticky_note/ui/theme/Color.kt`（`PaperPalette` / `toAccentSpec`），页面与组件不直接写死色值（Agent 推断）：

- `canvas` / `surface` / `surfaceMuted`：分别承担全屏纸面画布、分组软表面与柔底输入/分段槽；随 `ThemeMode`（跟随系统/日间/夜间）与 `AppBackgroundStyle`（暖纸/纯白/方格/点阵）联动（用户确认）。
- `accent` / `accentSoft` / `onAccent`：承担主按钮、当前选中态、日期高亮与设备在线点；由 `AppAccentTheme` 控制，默认墨青绿，支持松烟靛、赭陶棕、黛藕紫、松墨灰五套同风格双模高对比色（用户确认）。
- `inkPrimary` / `inkSecondary` / `inkMuted`：待办标题统一使用 `inkPrimary`（已完成用 `inkMuted` 加删除线），不再全篇着色；优先级仅通过左侧勾选圆圈与轻量语义标签（`urgent` / `important`）区分（Agent 推断）。
- 字体与数字：正文 16sp、标题 28sp / 22sp、辅助标签 12sp 以上；所有时间、日期、计数均使用 `tnum` 等宽数字对齐（Agent 推断）。
- 形状与分隔：分组卡片圆角 20dp 配 0.8dp 轻边框，控件圆角 14dp，主清单采用无外框纸面流配 0.6dp `hairline` 缩进分隔线（Agent 推断）。
- 动效：路由切换采用 `AnimatedContent` 250ms 轻横向推入与返回淡出；背景画布切换 220ms 平滑过渡；待办完成态使用弹性勾选与 `animateItem` 分组滑移动画；快捷输入与重复规则展开采用顺滑 `AnimatedVisibility`（用户确认）。

## 可用组件
- 基础组件体系：Jetpack Compose + Material 3（`androidx.compose.material3`）与 Material Icons Extended（`Icons.Rounded` / `Icons.AutoMirrored.Rounded`），行为组件如 `DatePickerDialog`、`ModalBottomSheet`、`AlertDialog` 沿用 M3 并通过 `StickyNoteTheme` 注入暖纸 `ColorScheme`（Agent 推断）。
- `app/src/main/java/com/passheep/sticky_note/feature/app/PaperComponents.kt`：提供 `PaperCanvasBackdrop`（四材质画布绘制）、`PaperSubPageTopBar`、`PaperGroupLabel`、`PaperSectionCard`、`PaperHairline`、`PaperSegmentedControl`、`PaperTextField`、`PaperPickerField`、`PaperDropdownField`、`PaperSwitchRow`、`PaperPrimaryButton`、`PaperSecondaryButton`、`PaperNavRow`、`PaperInfoRow`、`PaperEmptyCard`；后续新页面优先复用（Agent 推断）。

## 样板页面
- `app/src/main/java/com/passheep/sticky_note/feature/app/TodosScreen.kt`：待办主页与编辑弹层样板，实现无底栏极简顶栏、分组纸面清单与底部悬浮快捷输入槽（用户确认）。
- `app/src/main/java/com/passheep/sticky_note/feature/app/SettingsAndOtherScreens.kt`：二级设置、设备切换与关于页样板，包含日间/夜间、纸张背景微缩预览、主题色板及桌面小组件实时预览选择器（用户确认）。
- `app/src/main/java/com/passheep/sticky_note/feature/app/AppShellScreen.kt`：无底栏极简路由外壳，已移除旧式侧滑抽屉（用户确认）。
- `app/src/main/java/com/passheep/sticky_note/widget/WidgetStyleResolver.kt` 与 `app/src/main/java/com/passheep/sticky_note/widget/StickyNoteRemoteWidgetRenderer.kt`：桌面小组件样式体系，保留经典原版（`CLASSIC`），新增暖纸墨韵（`PAPER`）、实体便利贴（`STICKY_NOTE`）、极简墨水屏（`EINK`）与 6 套配色主题（含跟随 App 联动）（用户确认）。

## 用户取舍
- 2026-11-27：选定「方向 A · 暖纸墨韵」与「无底栏极简风」（首页底部全部留给快捷新增，设备与设置放顶栏右侧，关于页收进设置底部）；设置中必须支持日间/夜间模式、纸张/纯白/网格背景切换、以及默认墨绿并可选多套同风格主题强调色（用户确认）。
- 2026-11-27：桌面小组件同步升级，保留经典原版并新增暖纸墨韵、实体便利贴、极简墨水屏风格与 6 套配色主题；提供 2×2 / 4×2 / 4×4 多种规格且支持桌面任意缩放；顶部加号右侧提供可切换的「全部 / 未完成 / 已完成」Tab 并在设置中支持配置小组件默认展示；深色模式去除外圈亮边，所有 `setColorFilter` 色值强制不透明避免 `SRC_ATOP` 透出白边（用户确认）。
- 2026-11-27：首页去掉大标题与未接入说明文字，筛选栏直接紧贴顶栏；支持设置中配置「首页默认展示」；待办左滑展示【复制】【删除】双按钮再点击执行（防误触）；创建待办弹出详细编辑框自动聚焦并弹输入法，添加/复制后自动滚动定位并柔和闪烁高亮；应用图标切换为暖纸墨绿便利贴自适应图标（用户确认）。

## 不要做
- 不要重新引入左上角汉堡侧滑抽屉或固定底部导航栏（用户确认）。
- 不要给每条待办单独套厚重阴影大圆角白盒，也不要把普通待办标题染成高饱和彩色（Agent 推断）。
- 不要在页面中直接使用未经暖纸样式包装的默认 M3 亮蓝渐变胶囊或带勾 `SingleChoiceSegmentedButtonRow`（Agent 推断）。
- 网格与点阵背景线条保持低透明度（`patternStroke`），不得喧宾夺主影响正文 4.5:1 以上可读性（Agent 推断）。
