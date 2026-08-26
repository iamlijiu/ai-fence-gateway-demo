#!/bin/bash

# AI 安全围栏网关 Demo - GitHub 上传脚本
# 使用方法：./UPLOAD_TO_GITHUB.sh

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}AI 安全围栏网关 Demo - GitHub 上传${NC}"
echo -e "${GREEN}========================================${NC}"

# 检查 git 是否安装
if ! command -v git &> /dev/null; then
    echo -e "${RED}错误：git 未安装，请先安装 git${NC}"
    exit 1
fi

# 检查是否已经是 git 仓库
if [ -d ".git" ]; then
    echo -e "${YELLOW}警告：当前目录已是 git 仓库${NC}"
    read -p "是否继续？(y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# 获取 GitHub 仓库信息
echo -e "${YELLOW}请输入 GitHub 仓库信息：${NC}"
read -p "GitHub 用户名: " GITHUB_USERNAME
read -p "仓库名称 (默认: ai-fence-gateway-demo): " REPO_NAME
REPO_NAME=${REPO_NAME:-ai-fence-gateway-demo}

# 构建仓库 URL
REPO_URL="https://github.com/${GITHUB_USERNAME}/${REPO_NAME}.git"

echo -e "${GREEN}仓库地址：${REPO_URL}${NC}"
read -p "确认上传？(y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
fi

# 初始化 git 仓库（如果需要）
if [ ! -d ".git" ]; then
    echo -e "${YELLOW}初始化 git 仓库...${NC}"
    git init
fi

# 添加所有文件
echo -e "${YELLOW}添加文件...${NC}"
git add .

# 提交
echo -e "${YELLOW}提交代码...${NC}"
git commit -m "feat: 初始化 AI 安全围栏网关 Demo

- 核心功能：规则引擎、敏感信息脱敏、请求拦截、异常降级
- 预置规则：9类敏感信息（身份证、手机号、邮箱、银行卡等）
- 监控统计：MetricsCounter、熔断器、围栏配置
- 测试覆盖：99个测试用例（单元测试+集成测试）
- 文档完善：DDL脚本、Oracle配置、规则说明"

# 添加远程仓库
echo -e "${YELLOW}添加远程仓库...${NC}"
git remote remove origin 2>/dev/null || true
git remote add origin "$REPO_URL"

# 推送代码
echo -e "${YELLOW}推送代码到 GitHub...${NC}"
echo -e "${YELLOW}如果提示输入密码，请输入 GitHub Personal Access Token${NC}"
git push -u origin main

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}上传完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "仓库地址：${GREEN}https://github.com/${GITHUB_USERNAME}/${REPO_NAME}${NC}"
echo ""
echo -e "后续操作："
echo -e "1. 访问仓库查看代码"
echo -e "2. 添加 README.md 说明"
echo -e "3. 设置仓库描述和标签"
echo ""
