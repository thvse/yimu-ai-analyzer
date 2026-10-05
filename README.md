# 一木记账 AI 分析助手 (Yimu AI Analyzer)

基于 Android 原生 (Kotlin + Jetpack Compose) 构建的个人财务智能分析应用。能够直接读取并解密一木记账软件导出的官方 AES-256 加密备份包，提取底层 SQLite 数据库，并驱动大模型（DeepSeek / OpenAI）对个人账本进行深度自然语言对话分析。

---

## 🌟 核心特性

1. **底层逆向级解密 (zip4j)**：
   - 自动检测手机 `/sdcard/Documents/一木记账/` 目录下的最新备份包（如 `6.5.6_xxxx.zip`）。
   - 采用一木记账同款 **WinZip AES-256** 算法，输入用户数字 ID 一键秒级提取明文 `Custom.db`。
2. **纯本地 SQLite 解析**：
   - 原生只读连接 `Custom.db`，精确解析 `bill`（账单表）、`parentcategory`（一级分类）、`childcategory`（二级分类）、`asset`（资产账户）。
   - 毫秒级计算月度总支出、总收入、结余、各项分类支出占比排行榜。
3. **AI 财务健康顾问 (DeepSeek / OpenAI)**：
   - 动态将用户的账本数据摘要（总收支、分类排行、精选最近交易）注入到 AI Agent 系统提示词中。
   - 支持自然语言任意提问：
     - *“深度诊断我当前的消费结构”*
     - *“帮我统计餐饮外卖到底花了多少钱？”*
     - *“根据我的习惯，给出3个最有效的省钱建议”*
     - *“排查最近有没有异常突发的大额开销？”*
4. **现代化 Material 3 界面**：
   - 采用 Jetpack Compose 构建，界面简洁流畅，沉浸式深浅色适配。

---

## 📁 项目工程结构

```
yimu_ai_app/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/yimu/ai/
│   │   │   ├── MainActivity.kt           // 主入口与底部导航控制器
│   │   │   ├── crypto/
│   │   │   │   └── BackupDecryptor.kt    // zip4j AES-256 解密与重新打包模块
│   │   │   ├── data/
│   │   │   │   ├── Model.kt              // 数据模型 (账单、分类、统计汇总)
│   │   │   │   └── YimuDbReader.kt       // 原生 SQLite 聚合统计与查询引擎
│   │   │   ├── ai/
│   │   │   │   ├── AiChatClient.kt       // OkHttp 大模型客户端 (DeepSeek/OpenAI)
│   │   │   │   └── PromptEngine.kt       // 财务上下文动态注入提示词引擎
│   │   │   └── ui/
│   │   │       ├── theme/                // 现代绿色调主题系统
│   │   │       └── screens/
│   │   │           ├── HomeScreen.kt     // 账本概览与分类排行榜
│   │   │           ├── ChatScreen.kt     // AI 对话交互界面
│   │   │           └── SettingsScreen.kt // 用户ID与API Key配置
│   │   └── res/
└── build.gradle.kts                      // Gradle 依赖管理
```

---

## 🚀 如何使用 Android Studio 运行

1. 打开 **Android Studio**，选择 **Open**。
2. 选择本工程根目录：`C:\Users\Thvse\.gemini\antigravity\scratch\yimu_ai_app`。
3. 等待 Gradle 依赖同步完成。
4. 将你的手机（如 Redmi K80）通过 USB 或无线调试连接电脑。
5. 点击右上角绿色 **Run 'app'** 按钮即可一键编译并安装到手机。

---

## 📱 手机端使用步骤

1. **准备备份**：打开手机上的 **一木记账** App $\rightarrow$ “我的” $\rightarrow$ “数据备份” $\rightarrow$ 执行一次本地备份（文件保存在 `/sdcard/Documents/一木记账/`）。
2. **启动本应用**：
   - 首次启动允许“所有文件访问权限”（用于读取一木记账文档目录）。
   - 前往 **设置** 页面：
     - 输入你的 **一木记账用户数字ID**（一木记账“我的”->点击头像查看）。
     - 输入你的 **AI API Key**（如 DeepSeek Key）。
   - 点击 **“立即解密并同步账本”**。
3. **开始使用**：
   - 在 **“概览”** 页查看图表、分类排名与明细。
   - 在 **“AI顾问”** 页直接向 AI 提问任何账务问题！
