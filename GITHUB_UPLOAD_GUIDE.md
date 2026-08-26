# GitHub 上传指南

## 前置准备

### 1. 安装 Git

```bash
# macOS
brew install git

# Ubuntu/Debian
sudo apt-get install git

# Windows
# 下载安装 Git for Windows: https://git-scm.com/download/win
```

### 2. 配置 Git

```bash
# 设置用户信息
git config --global user.name "你的名字"
git config --global user.email "你的邮箱"
```

### 3. 创建 GitHub 仓库

1. 访问 [GitHub](https://github.com)
2. 点击右上角 "+" -> "New repository"
3. 填写仓库信息：
   - **Repository name**: `ai-fence-gateway-demo`
   - **Description**: `AI 安全围栏网关 Demo - 敏感信息检测与脱敏`
   - **Visibility**: 选择 Public 或 Private
   - **不要**勾选 "Initialize this repository with a README"
4. 点击 "Create repository"

### 4. 生成 Personal Access Token（如果需要）

1. 访问 GitHub Settings -> Developer settings -> Personal access tokens
2. 点击 "Generate new token"
3. 选择权限：`repo`（完整仓库访问权限）
4. 生成并保存 token

## 上传方式

### 方式一：使用上传脚本（推荐）

```bash
# 1. 进入项目目录
cd /home/cbj/workspace/ai-fence-gateway-demo

# 2. 添加执行权限
chmod +x UPLOAD_TO_GITHUB.sh

# 3. 运行上传脚本
./UPLOAD_TO_GITHUB.sh

# 4. 按提示输入 GitHub 用户名和仓库名
```

### 方式二：手动上传

```bash
# 1. 进入项目目录
cd /home/cbj/workspace/ai-fence-gateway-demo

# 2. 初始化 git 仓库
git init

# 3. 添加所有文件
git add .

# 4. 提交代码
git commit -m "feat: 初始化 AI 安全围栏网关 Demo"

# 5. 添加远程仓库（替换 YOUR_USERNAME）
git remote add origin https://github.com/YOUR_USERNAME/ai-fence-gateway-demo.git

# 6. 推送代码
git push -u origin main
```

### 方式三：使用 GitHub CLI

```bash
# 1. 安装 GitHub CLI
brew install gh  # macOS
# 或参考: https://github.com/cli/cli#installation

# 2. 登录
gh auth login

# 3. 创建仓库并推送
cd /home/cbj/workspace/ai-fence-gateway-demo
gh repo create ai-fence-gateway-demo --public --source=. --remote=origin --push
```

## 上传后配置

### 1. 设置仓库描述

在仓库页面点击 "About" 区域的齿轮图标：
- **Description**: `AI 安全围栏网关 Demo - 敏感信息检测与脱敏`
- **Website**: （可选）
- **Topics**: `java`, `spring-boot`, `security`, `ai`, `desensitization`

### 2. 设置仓库主页

将 `PROJECT_README.md` 重命名为 `README.md`：

```bash
# 在 GitHub 网页上操作，或本地执行：
git mv PROJECT_README.md README.md
git commit -m "docs: 重命名 README 文件"
git push
```

### 3. 创建 Release

1. 在仓库页面点击 "Releases"
2. 点击 "Create a new release"
3. 填写信息：
   - **Tag version**: `v1.0.0`
   - **Release title**: `v1.0.0 - 初始版本`
   - **Description**: 版本说明
4. 点击 "Publish release"

### 4. 设置分支保护（可选）

1. 进入仓库 Settings -> Branches
2. 点击 "Add rule"
3. 设置规则：
   - **Branch name pattern**: `main`
   - **Require pull request reviews before merging**: 勾选
   - **Require status checks to pass before merging**: 勾选

## 常见问题

### Q1: 推送时提示权限错误

```
remote: Permission to username/repo.git denied to user.
fatal: unable to access 'https://github.com/username/repo.git/': The requested URL returned error: 403
```

**解决方案**：
1. 检查 GitHub 用户名是否正确
2. 使用 Personal Access Token 替代密码
3. 检查仓库权限设置

### Q2: 推送时提示仓库不存在

```
fatal: repository 'https://github.com/username/repo.git/' not found
```

**解决方案**：
1. 确认仓库已创建
2. 检查仓库名称拼写
3. 确认仓库 URL 正确

### Q3: 文件太大无法上传

```
remote: error: File xxx.jar is 123.45 MB; this exceeds GitHub's file size limit of 100 MB
```

**解决方案**：
1. 检查 `.gitignore` 是否正确配置
2. 删除大文件：`git rm --cached xxx.jar`
3. 使用 Git LFS：`git lfs track "*.jar"`

### Q4: 如何更新代码

```bash
# 1. 拉取最新代码
git pull origin main

# 2. 修改代码

# 3. 提交并推送
git add .
git commit -m "feat: 新功能描述"
git push origin main
```

### Q5: 如何创建分支

```bash
# 1. 创建并切换到新分支
git checkout -b feature/new-feature

# 2. 修改代码

# 3. 提交
git add .
git commit -m "feat: 新功能"

# 4. 推送分支
git push origin feature/new-feature

# 5. 在 GitHub 上创建 Pull Request
```

## 项目结构（上传后）

```
ai-fence-gateway-demo/
├── .git/                          # Git 版本控制
├── .gitignore                     # Git 忽略配置
├── LICENSE                        # Apache 2.0 许可证
├── README.md                      # 项目说明（从 PROJECT_README.md 重命名）
├── pom.xml                        # Maven 配置
├── sql/                           # DDL 脚本
├── docs/                          # 文档
├── rules/                         # 规则配置
├── src/                           # 源代码
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
└── UPLOAD_TO_GITHUB.sh           # 上传脚本
```

## 推荐的 GitHub 仓库设置

### 1. 仓库描述

```
AI 安全围栏网关 Demo - 敏感信息检测与脱敏中间件
```

### 2. Topics 标签

```
java, spring-boot, mybatis, oracle, security, ai, desensitization, gateway
```

### 3. 仓库首页展示

- 显示 README.md
- 显示目录结构
- 显示最近提交

## 后续维护

### 1. 定期更新

```bash
# 拉取最新代码
git pull origin main

# 修改后提交
git add .
git commit -m "fix: 修复问题描述"
git push origin main
```

### 2. 版本管理

```bash
# 创建标签
git tag -a v1.0.0 -m "版本 1.0.0"
git push origin v1.0.0
```

### 3. 代码审查

1. 创建 Pull Request
2. 邀请团队成员审查
3. 合并到 main 分支

## 联系方式

如有问题，请联系：
- GitHub Issues: 在仓库中创建 Issue
- 邮箱: [待填写]
