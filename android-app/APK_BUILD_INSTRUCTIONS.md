# APK 构建说明

由于 Java 版本兼容性问题，建议使用以下方法之一构建 APK：

## 方法 1: 使用 Android Studio (推荐)

1. 下载并安装 [Android Studio](https://developer.android.com/studio)
2. 打开 Android Studio
3. File → Open → 选择 `android-app` 目录
4. 等待 Gradle 同步完成（Android Studio 会自动处理）
5. Build → Build Bundle(s) / APK(s) → Build APK(s)
6. APK 会生成在: `app/build/outputs/apk/debug/app-debug.apk`

## 方法 2: 安装 JDK 17 后使用命令行

### 下载 JDK 17
- [Adoptium (推荐)](https://adoptium.net/temurin/releases/?version=17)
- [Oracle JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)

### 配置环境变量
1. 安装 JDK 17 到 `C:\Program Files\Java\jdk-17`
2. 设置 JAVA_HOME:
   ```
   setx JAVA_HOME "C:\Program Files\Java\jdk-17"
   ```
3. 重新打开 PowerShell

### 构建
```powershell
cd "d:\vscode\gayhub\pc模式\android-app"
.\gradlew.bat assembleDebug
```

##方法 3: 在线构建服务

可以使用 GitHub Actions 或其他 CI/CD 服务自动构建。

## 当前问题

您的系统安装的是 Java 22，但 Gradle 8.5 最高支持 Java 21。
需要:
- 安装 Java 17 或 Java 21
- 或使用 Android Studio（自带兼容的 JDK）

## 快速解决方案

推荐直接使用 Android Studio，它会自动处理所有版本兼容性问题。
