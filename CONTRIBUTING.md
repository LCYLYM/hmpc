# 贡献指南

感谢您对 PC 模式激活器项目的关注! 我们欢迎各种形式的贡献。

## 如何贡献

### 报告 Bug

在提交 Bug 报告前,请确保:
1. 检查 Issues 中是否已有类似问题
2. 准备详细的问题描述
3. 提供设备信息和日志

Bug 报告应包含:
- **设备型号**: 如 "Samsung Galaxy Tab S8"
- **Android 版本**: 如 "Android 13"
- **应用版本**: 如 "v1.0.0"
- **问题描述**: 详细描述发生了什么
- **复现步骤**: 如何重现这个问题
- **期望行为**: 应该发生什么
- **日志**: 如果可能,提供 logcat 日志

### 功能建议

我们欢迎新功能建议! 请:
1. 检查是否已有类似建议
2. 详细描述功能需求
3. 说明使用场景
4. 如果可能,提供实现思路

### 提交代码

#### 准备工作
1. Fork 本仓库
2. 创建特性分支: `git checkout -b feature/amazing-feature`
3. 确保代码符合项目风格

#### 代码规范

**Kotlin 代码**:
```kotlin
// 使用 4 空格缩进
class MyClass {
    private val myProperty: String = "value"
    
    fun myFunction(param: Int): Boolean {
        // 代码逻辑
        return true
    }
}

// 添加文档注释
/**
 * 创建虚拟显示器
 * @param width 显示器宽度
 * @param height 显示器高度
 * @return 创建成功返回 true
 */
fun createDisplay(width: Int, height: Int): Boolean {
    // 实现
}
```

**命名规范**:
- 类名: `PascalCase`
- 函数/变量: `camelCase`
- 常量: `UPPER_SNAKE_CASE`
- 资源ID: `snake_case`

#### 提交规范

提交信息格式:
```
<type>(<scope>): <subject>

<body>

<footer>
```

类型 (type):
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建/工具配置

示例:
```
feat(display): 添加自定义分辨率支持

允许用户在设置中自定义虚拟显示器的分辨率,
支持 720p, 1080p, 1440p 三种预设。

Closes #123
```

#### Pull Request

1. 确保代码通过编译
2. 添加必要的测试
3. 更新相关文档
4. 描述清楚改动内容
5. 关联相关 Issue

PR 模板:
```markdown
## 描述
简要描述这个 PR 做了什么

## 相关 Issue
Closes #123

## 改动类型
- [ ] Bug 修复
- [ ] 新功能
- [ ] 文档更新
- [ ] 代码重构

## 测试
描述如何测试这些改动

## 截图 (如果适用)
添加相关截图

## 检查清单
- [ ] 代码通过编译
- [ ] 已添加必要的测试
- [ ] 已更新文档
- [ ] 遵循代码规范
```

### 文档贡献

文档同样重要! 你可以:
- 修正错别字
- 改进表述
- 添加示例
- 翻译文档

### 测试贡献

帮助测试应用:
1. 在不同设备上测试
2. 报告兼容性问题
3. 提供测试反馈
4. 完善兼容性列表

## 开发环境设置

### 必需工具
- Android Studio Arctic Fox+
- JDK 8+
- Android SDK (API 21+)
- Git

### 克隆仓库
```bash
git clone https://github.com/yourusername/pc-mode-activator.git
cd pc-mode-activator
```

### 导入项目
1. 打开 Android Studio
2. File -> Open
3. 选择 `android-app` 目录
4. 等待 Gradle 同步

### 运行调试
```bash
# 构建项目
./gradlew build

# 安装到设备
./gradlew installDebug

# 查看日志
adb logcat | grep VirtualDisplay
```

## 社区准则

### 行为准则
- 尊重他人
- 包容不同观点
- 接受建设性批评
- 关注项目利益

### 沟通方式
- Issues: Bug 报告和功能建议
- Pull Requests: 代码贡献
- Discussions: 一般讨论

## 许可证

贡献的代码将使用 MIT 许可证发布。

## 致谢

感谢所有贡献者的付出! 🎉

## 联系方式

有问题? 可以通过以下方式联系:
- GitHub Issues
- Email: [your-email@example.com]

---

再次感谢您的贡献! ❤️
