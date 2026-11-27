# android_sticky_note

android_sticky_note 是一个面向 Android 的待办管理工具，配合 Zectrix 墨水屏设备与极趣云平台使用。它提供手机端待办管理、桌面小组件和云端同步能力，让待办可以在 App、小组件与墨水屏设备之间形成轻量联动。

自 v0.1.0 起，界面全面重构为「暖纸墨韵」风格：无底栏极简导航，支持日间/夜间、多种纸张背景与主题色，桌面小组件同步升级为多风格、多尺寸。

> 本项目由 PASSHEEP 提出需求、测试与验收，主要通过 Codex + GPT-5.4 协作开发完成；v0.1.0 的 UI 重构由 Antigravity 协助完成。

## 下载安装

前往 [Releases](https://github.com/passheep/android_sticky_note/releases) 下载最新版 APK 安装（要求 Android 8.0 及以上）。

## 项目预览

| App 首页                     | 新增 / 编辑便利贴 |
|----------------------------| --- |
| ![App 首页](./imgs/home.jpg) | ![新增 / 编辑便利贴](./imgs/edit.jpg) |

| 小组件风格与实时预览 | 外观与首页设置 |
| --- | --- |
| ![小组件风格与实时预览](./imgs/widget.jpg) | ![外观与首页设置](./imgs/config.jpg) |

## 项目说明

本项目不是独立离线待办应用，需要关联极趣云平台和墨水屏设备使用。App 通过极趣云平台 Open API 获取设备与待办数据，并将用户在 App 或桌面小组件上的操作同步到平台。

适用场景：

- 在 Android 手机上管理 Zectrix 极趣 Note 4 相关待办。
- 在桌面小组件中快速查看、刷新和勾选待办。
- 通过极趣云平台 API 与墨水屏设备保持数据联动。

## 获取 API Key

1. 访问 https://cloud.zectrix.com/home/api-keys 创建 API Key。
2. 在 App「设置」页面开启接入平台。
3. 填写平台地址，例如 `https://cloud.zectrix.com`。
4. 添加 API Key。
5. 点击测试平台连接，并选择需要关联的设备。
6. 若出现同步不及时的情况，请开启“精确闹钟权限”、关闭电池优化、允许后台运行。

## 已完成功能

### 待办管理

- 待办清单：新增、编辑、删除、完成/取消完成。
- 列表分组：按「已逾期」「今天」「后续计划」「已完成」分组展示，逾期项以提示色标注。
- 首页筛选：顶部可切换「全部 / 未完成 / 已完成」，并可在设置中配置「首页默认展示」。
- 左滑操作：左滑列表项展开「复制」「删除」按钮，再点击按钮执行，避免误触；复制会创建一条完全相同的待办。
- 快捷创建：底部胶囊快速新增，弹出详细编辑后自动聚焦并唤起输入法。
- 自动定位：新增、修改、复制待办后，列表自动滚动到对应条目并短暂高亮。
- 动效与性能：页面/弹窗转场、完成/取消完成动画；大列表分段加载，滚动保持流畅。

### 外观自定义

- 日间 / 夜间 / 跟随系统。
- 背景样式：暖纸、纯白、方格、点阵。
- 主题色：5 套纸墨配色，默认墨绿。

### 云端同步

- 通过极趣云平台 Open API 拉取与更新待办。
- 拉取设备列表，并按设备绑定待办数据。
- 后台刷新：基于 `AlarmManager`、`WorkManager` 和系统广播定期刷新。
- 同步周期：支持 60s、120s、300s、600s。

### 桌面小组件

- 4 种风格 × 6 种主题，设置页提供实时预览。
- 多尺寸：2×2、4×2、4×4，可在桌面任意拖动调整大小。
- 顶部 Tab 可切换「全部 / 未完成 / 已完成」，并可在设置中配置「小组件默认展示」。
- 滚动列表、手动刷新、点击勾选完成（带完成动画反馈）。
- 浅色 / 深色 / 跟随系统，支持透明度调节。

## 待完成功能

- 某些情况下杀掉后台出现同步延时或失效。
- 图片推送到墨水屏设备。
- 闹钟提醒与系统通知。
- 更完整的重复任务管理。
- 多设备之间更细粒度的冲突处理。

## 技术栈

- 语言：Kotlin
- UI：Jetpack Compose，自定义「纸墨」设计系统（基于 Material 3 组件）
- 架构：MVVM + 分层数据模块
- 依赖注入：Hilt
- 网络：Retrofit、OkHttp、Kotlinx Serialization
- 配置存储：DataStore Preferences
- 本地数据库：Room
- 后台任务：WorkManager、AlarmManager
- 小组件：Android AppWidgetProvider、RemoteViews、RemoteViewsService

## 项目架构

```text
app/src/main/java/com/passheep/sticky_note
├── core
│   ├── model          # Todo、Device、同步状态等领域模型
│   ├── repository     # 仓库接口
│   └── settings       # 应用配置模型与接口
├── data
│   ├── cloud          # 云端待办状态容器
│   ├── local          # Room 数据库、DAO、Entity
│   ├── network        # Retrofit/OkHttp 网络配置
│   ├── remote         # API Service、DTO、远端映射
│   ├── settings       # DataStore 配置持久化
│   └── sync           # 后台同步、闹钟调度、启动恢复
├── feature
│   └── app            # 主界面（待办列表、编辑弹窗）、设置页、纸墨通用组件
├── ui
│   └── theme          # 应用主题（配色、背景、日夜间）
└── widget             # 桌面小组件、风格解析、RemoteViews 渲染、点击与完成动画
```

当前版本的小组件会通过平台接口获取最新待办并更新 UI，不以本地待办数据库作为主要数据源。

界面设计规范见 [DESIGN.md](./DESIGN.md)。

## 本地运行

环境要求：

- Android Studio
- JDK 17 及以上
- Android SDK 35
- 使用项目自带的 Gradle Wrapper（`./gradlew :app:assembleDebug`）

## 开源说明

本项目使用 MIT License 开源，详见 [LICENSE](./LICENSE)。

## 相关链接

- 项目地址：https://github.com/passheep/android_sticky_note
- 极趣云平台：https://cloud.zectrix.com
- 极趣科技 Wiki：https://wiki.zectrix.com
