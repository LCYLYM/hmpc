# 快速构建指南

## 使用 Android Studio 构建 APK

### 方法 1: 构建 Debug APK

1. 打开 Android Studio
2. File → Open → 选择 `android-app` 目录
3. 等待 Gradle 同步完成
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
5. 等待构建完成
6. 点击通知中的 "locate" 找到 APK 文件

APK 位置: `android-app/app/build/outputs/apk/debug/app-debug.apk`

### 方法 2: 使用命令行

```bash
cd android-app

# Windows (PowerShell)
.\gradlew.bat assembleDebug

# 或使用 cmd
gradlew.bat assembleDebug

# APK 输出位置
# app\build\outputs\apk\debug\app-debug.apk
```

## 构建 Release APK (需要签名)

### 步骤 1: 生成密钥库

```bash
# 在 android-app 目录下执行
keytool -genkey -v -keystore pc-mode.keystore -alias pc-mode -keyalg RSA -keysize 2048 -validity 10000
```

按提示输入:
- 密钥库密码 (记住这个密码!)
- 您的名字和组织信息

### 步骤 2: 配置签名

创建文件 `android-app/keystore.properties`:

```properties
storePassword=你的密钥库密码
keyPassword=你的密钥密码
keyAlias=pc-mode
storeFile=../pc-mode.keystore
```

### 步骤 3: 修改 app/build.gradle

在文件开头添加:

```gradle
def keystorePropertiesFile = rootProject.file("keystore.properties")
def keystoreProperties = new Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}

android {
    signingConfigs {
        release {
            if (keystorePropertiesFile.exists()) {
                keyAlias keystoreProperties['keyAlias']
                keyPassword keystoreProperties['keyPassword']
                storeFile file(keystoreProperties['storeFile'])
                storePassword keystoreProperties['storePassword']
            }
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### 步骤 4: 构建 Release APK

```bash
# Windows
.\gradlew.bat assembleRelease

# APK 输出位置
# app\build\outputs\apk\release\app-release.apk
```

## 快速构建 (无签名的 Debug APK)

如果您只是想快速测试,使用 Debug APK 即可:

```bash
cd d:\vscode\gayhub\pc模式\android-app

# 构建 Debug APK
.\gradlew.bat assembleDebug

# 安装到已连接的设备
.\gradlew.bat installDebug
```

## 常见问题

### 问题 1: gradlew.bat 不存在

运行以下命令生成 wrapper:

```bash
# 在 android-app 目录下
gradle wrapper --gradle-version 8.2
```

### 问题 2: 构建失败

检查:
- Java JDK 是否已安装 (需要 JDK 8 或更高版本)
- Android SDK 是否已配置
- 网络连接是否正常 (需要下载依赖)

### 问题 3: 找不到 APK

APK 文件位置:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## 建议

对于第一次构建,推荐:
1. 使用 Android Studio 构建 (更直观)
2. 构建 Debug APK (无需签名配置)
3. 在真实设备上测试

## 输出文件说明

- **app-debug.apk**: 
  - 未签名或使用 debug 签名
  - 可以直接安装测试
  - 不能发布到应用商店
  - 文件较大

- **app-release.apk**: 
  - 使用您的密钥签名
  - 可以发布到应用商店
  - 经过优化,文件较小
