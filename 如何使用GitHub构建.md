# 🚀 使用 GitHub Actions 在线构建 APK 完整教程

## 📝 准备工作

### 1. 确保您有 GitHub 账号
- 如果没有，前往 https://github.com 注册

### 2. 安装 Git（如果还没有）
```powershell
# 使用 Chocolatey 安装
choco install git -y

# 或从官网下载
# https://git-scm.com/download/win
```

## 🎯 步骤 1: 创建 GitHub 仓库

### 方式 A: 通过网页创建
1. 登录 GitHub
2. 点击右上角 "+" → "New repository"
3. 填写仓库信息：
   - **Repository name**: `pc-mode-activator` (或其他名称)
   - **Description**: `Android app to activate PC mode on tablets`
   - **Visibility**: Public 或 Private
   - **不要**勾选 "Initialize this repository with a README"
4. 点击 "Create repository"

### 方式 B: 使用 GitHub CLI
```powershell
# 安装 GitHub CLI
choco install gh -y

# 登录
gh auth login

# 创建仓库
gh repo create pc-mode-activator --public
```

## 🎯 步骤 2: 初始化本地 Git 仓库并推送

### 在项目目录执行以下命令：

```powershell
# 切换到项目根目录
cd "D:\vscode\gayhub\pc模式"

# 初始化 Git 仓库
git init

# 添加所有文件
git add .

# 提交
git commit -m "Initial commit: PC Mode Activator Android App"

# 添加远程仓库（替换成您的用户名和仓库名）
git remote add origin https://github.com/您的用户名/pc-mode-activator.git

# 推送到 GitHub
git branch -M main
git push -u origin main
```

### 如果遇到认证问题：

**使用 Personal Access Token (推荐)**

1. 访问: https://github.com/settings/tokens
2. 点击 "Generate new token" → "Generate new token (classic)"
3. 设置名称: `PC Mode Build`
4. 勾选权限: `repo` (所有子选项)
5. 点击 "Generate token"
6. **复制生成的 token（只显示一次）**

7. 推送时输入凭据：
   ```
   Username: 您的GitHub用户名
   Password: 粘贴刚才复制的 token
   ```

## 🎯 步骤 3: 触发自动构建

### 自动触发（推送代码后）
1. 代码推送成功后，GitHub Actions 会自动开始构建
2. 访问您的仓库页面
3. 点击 **"Actions"** 标签
4. 您会看到 "Build Android APK" 工作流正在运行

### 手动触发
1. 进入仓库的 **Actions** 页面
2. 点击左侧 **"Build Android APK"**
3. 点击右侧 **"Run workflow"** 按钮
4. 选择分支（通常是 `main`）
5. 点击绿色的 **"Run workflow"** 按钮

## 🎯 步骤 4: 监控构建进度

1. 在 Actions 页面，点击正在运行的 workflow
2. 您会看到构建步骤：
   - ✅ 检出代码
   - ✅ 设置 JDK 17
   - ✅ 授予 Gradle 执行权限
   - ✅ 构建 Debug APK
   - ✅ 构建 Release APK
   - ✅ 上传 APK
   - ✅ 生成构建报告

3. 点击每个步骤可以查看详细日志

## 🎯 步骤 5: 下载构建的 APK

### 构建成功后：

1. 滚动到页面底部
2. 找到 **"Artifacts"** 部分
3. 您会看到：
   - `app-debug` (Debug 版本 APK)
   - `app-release-unsigned` (Release 未签名版本)

4. 点击相应的 artifact 名称下载（会下载一个 ZIP 文件）
5. 解压 ZIP 文件获得 APK

### APK 文件说明：
- **app-debug.apk**: 
  - ✅ 可以直接安装使用
  - ✅ 包含调试信息
  - ❌ 文件较大
  - ✅ 推荐用于测试

- **app-release-unsigned.apk**: 
  - ❌ 需要签名才能安装
  - ✅ 文件较小
  - ✅ 性能更好
  - 需要额外签名步骤

## 🎯 步骤 6: 安装到平板设备

### 方式 A: USB 传输
```powershell
# 使用 ADB (如果有)
adb install app-debug.apk

# 或者直接复制文件
# 1. 连接设备到电脑
# 2. 将 APK 文件复制到设备
# 3. 在设备上打开文件管理器安装
```

### 方式 B: 无线传输
1. 使用文件分享应用（如：LocalSend、Nearby Share）
2. 将 APK 发送到平板
3. 在平板上点击 APK 文件安装

### 安装前准备：
1. 在平板上启用 **"未知来源"** 或 **"允许安装未知应用"**
   - 设置 → 安全 → 未知来源
   - 或 设置 → 应用 → 特殊应用权限 → 安装未知应用

## 📊 GitHub Actions 配额

### 公共仓库：
- ✅ **完全免费**
- ✅ 无限构建分钟数

### 私有仓库（免费账户）：
- ✅ 每月 2000 分钟构建时间
- 每次构建大约需要 3-5 分钟
- 足够进行约 400-600 次构建

## 🔧 后续更新流程

修改代码后重新构建：

```powershell
# 切换到项目目录
cd "D:\vscode\gayhub\pc模式"

# 查看修改的文件
git status

# 添加修改
git add .

# 提交
git commit -m "描述您的修改"

# 推送到 GitHub（会自动触发构建）
git push
```

## 🎨 自定义构建（可选）

### 修改构建配置
编辑 `.github/workflows/build-apk.yml`：

```yaml
# 只在推送 tag 时构建
on:
  push:
    tags:
      - 'v*'

# 或定时构建
on:
  schedule:
    - cron: '0 0 * * 0'  # 每周日午夜
```

## ❓ 常见问题

### Q1: 构建失败怎么办？
**A**: 
1. 查看 Actions 页面的错误日志
2. 常见原因：
   - Gradle 配置错误
   - 依赖下载失败
   - 代码语法错误
3. 修复后重新推送代码

### Q2: 如何构建签名的 Release 版本？
**A**: 需要配置签名密钥，步骤较复杂，建议先使用 Debug 版本测试

### Q3: Artifacts 找不到了？
**A**: Artifacts 默认保留 30 天，过期后会自动删除

### Q4: 可以在本地查看构建日志吗？
**A**: 可以使用 `act` 工具在本地模拟 GitHub Actions：
```powershell
choco install act-cli -y
cd "D:\vscode\gayhub\pc模式"
act
```

## 🎉 总结

使用 GitHub Actions 构建的优势：
- ✅ 无需本地安装 Android SDK（节省 5GB+ 空间）
- ✅ 云端构建，不占用本地资源
- ✅ 自动化，推送即构建
- ✅ 免费（公共仓库无限制）
- ✅ 构建历史完整记录
- ✅ 支持多平台（Linux、macOS、Windows）

现在您可以开始使用了！🚀
