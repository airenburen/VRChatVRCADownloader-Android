# VRChat VRCA Downloader Android

VRChat VRCA Downloader 的 Android 版本，用于管理并下载自己 VRChat 账号下的 Avatar 文件。


## ⚠️ 警告

本工具连同仓库都是由AI编写和操作的，仅供娱乐

**严禁将本工具用于任何恶意用途！**

本工具旨在帮助你找回丢失的资产。
请不要请求或修改本工具，用于窃取不属于你自己的资产！

## 功能特性

### 账号访问
- 支持手动粘贴 `auth` Cookie 登录
- 支持内置 WebView 自动抓取 `auth` Cookie
- 支持重新登录获取新 Cookie
- 不保存账号密码

### 模型列表
- 一键同步账号下的 Avatar 模型
- 搜索过滤功能
- 显示 Avatar 预览图与名称信息
- 多选下载（全选/取消全选）

### 下载任务管理
- 多任务并发下载
- 实时显示总体进度与下载速度
- 失败/超时任务一键重试
- 终止单个/选中/全部任务
- 清理已完成任务

### 文件管理
- 使用 Android 原生文件管理器选择下载路径
- 支持 SAF (Storage Access Framework)
- 支持 Android 10+ 外部存储写入
- 文件名模板变量：`{short_name}`, `{name}`, `{version}`, `{id}`, `{date}`

### 缓存系统
- 使用 App 内部存储缓存头像图片
- 缓存 Avatar 列表数据
- 一键清除缓存

### 网络设置
- 支持 HTTP/HTTPS 代理
- 代理连通性测试
- 自动重试机制（处理 429 限流）

## 技术栈

- **语言**: Kotlin
- **最低 SDK**: Android 7.0 (API 24)
- **目标 SDK**: Android 14 (API 34)
- **包名**: `aina.vrcadl`
- **架构**: MVVM + Repository Pattern
- **网络**: OkHttp3 + Gson
- **图片加载**: Glide
- **异步**: Kotlin Coroutines
- **UI**: Material Design 3 (浅色主题)

## 项目结构

```
app/src/main/java/aina/vrcadl/
├── VRChatApp.kt                 # Application 类
├── BuildConfig.kt               # 构建配置
├── api/
│   └── VRChatApi.kt            # VRChat API 接口
├── cache/
│   └── CacheManager.kt         # 缓存管理器
├── data/
│   └── model/
│       ├── Avatar.kt           # Avatar 数据模型
│       ├── DownloadTask.kt     # 下载任务模型
│       └── FileResponse.kt     # API 响应模型
├── download/
│   ├── DownloadManager.kt      # 下载管理器
│   └── DownloadService.kt      # 前台下载服务
├── ui/
│   ├── MainActivity.kt         # 主活动
│   ├── WebViewActivity.kt      # WebView 登录活动
│   ├── SettingsActivity.kt     # 设置活动
│   ├── adapter/
│   │   ├── AvatarAdapter.kt    # Avatar 列表适配器
│   │   ├── DownloadAdapter.kt  # 下载任务适配器
│   │   └── ViewPagerAdapter.kt # ViewPager 适配器
│   └── fragment/
│       ├── AvatarListFragment.kt   # Avatar 列表片段
│       └── DownloadsFragment.kt    # 下载任务片段
└── utils/
    └── PreferenceManager.kt    # 偏好设置管理器
```

## 构建说明

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17 或更高版本
- Android SDK 34

### 签名信息
- **CN**: ainaaina
- **O**: ainaaina
- **C**: cn
- Keystore: `app/vrca.keystore`

### 构建步骤

1. 克隆项目
```bash
git clone https://github.com/airenburen/VRChatVRCADownloader-Android.git
cd VRChatVRCADownloader-Android
```

2. 使用 Android Studio 打开项目，或使用命令行构建:
```bash
./gradlew assembleRelease
```

3. 安装 APK:
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

## 使用方法

1. **登录**
   - 打开应用后，选择登录方式:
     - 手动粘贴 `auth` Cookie
     - 点击"内置登录"使用 WebView 登录 VRChat

2. **重新登录**
   - 点击右上角刷新图标可重新登录获取新 Cookie

3. **同步 Avatar 列表**
   - 登录成功后，点击"同步列表"按钮获取账号下的 Avatar

4. **下载 Avatar**
   - 单个下载: 点击 Avatar 项右侧的下载按钮
   - 批量下载: 勾选多个 Avatar，点击"下载选中项"

5. **管理下载任务**
   - 切换到"下载任务"标签页查看下载进度
   - 支持暂停、重试、取消下载任务

6. **设置**
   - 点击右上角设置图标进入设置页面
   - 可配置下载路径、文件名模板、代理等

## 权限说明

- `INTERNET`: 访问网络
- `ACCESS_NETWORK_STATE`: 检查网络状态
- `WRITE_EXTERNAL_STORAGE` (Android 9 及以下): 写入外部存储
- `READ_EXTERNAL_STORAGE` (Android 9 及以下): 读取外部存储
- `FOREGROUND_SERVICE`: 前台服务（下载服务）
- `POST_NOTIFICATIONS` (Android 13+): 发送通知

## 与原桌面版的区别

| 功能 | 桌面版 | Android 版 |
|------|--------|-----------|
| 登录方式 | Cookie/内置登录页 | Cookie/WebView |
| 文件选择 | 系统文件对话框 | Android SAF |
| 缓存位置 | 程序目录 cache/ | App 内部存储 |
| 下载路径 | 任意文件夹 | 通过 SAF 选择的文件夹 |
| AssetRipper 联动 | 支持 | 暂不支持 |
| 主题 | 深色 | 浅色（白里带点蓝） |

## 免责声明

本工具为第三方辅助工具，仅用于个人账号资产管理与下载。

- 所有数据请求均通过 VRChat 官方公开 API 完成
- 不提供或支持任何破解、绕过权限或非法访问行为
- 不包含对 VRChat 客户端/服务器/资源的注入或篡改
- 不存储、不上传、不分享用户账号密码或 Cookie

## 许可证

WTFPL (Do What The F*** You Want To Public License)

## 致谢

- 原桌面版作者: [Null-K](https://github.com/Null-K)
- VRChat API 文档社区
