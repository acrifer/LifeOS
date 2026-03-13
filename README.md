# LifeOS

> 一个面向个人使用场景的知识管理系统。  
> 把笔记、AI 整理、任务转化、复习队列和周复盘串成一条完整闭环。

## 项目简介

LifeOS 不是一个通用聊天工具，而是一个围绕个人知识沉淀构建的系统。  
项目的核心目标是把这条链路做通：

`快速记录 -> 组织沉淀 -> 搜索找回 -> AI 整理 -> 复习复用 -> 转化为行动项`

它适合作为：
- 校招 / 应届生项目展示
- 个人作品集项目
- 微服务、异步任务流、知识管理方向的练手项目

## 功能亮点

- 用户注册、登录、JWT 鉴权、用户资料维护
- 知识笔记创建、编辑、搜索、置顶、复习状态管理
- AI 摘要、AI 整理、AI 提取任务、AI 周复盘
- 从笔记中提取任务，并在任务完成后回写行为统计
- Dashboard 展示待复习笔记、AI inbox、知识任务和高频标签
- RocketMQ 支撑行为事件和 AI 作业异步任务流
- 一键启动环境与后端服务
- Swagger / OpenAPI 文档
- 可重复重置的测试数据

## 项目预览

当前项目已经具备完整的演示链路，推荐展示顺序：

1. 登录系统
2. 创建或编辑一篇笔记
3. 触发 AI 摘要或整理
4. 查看 AI 作业历史
5. 从笔记提取任务
6. 在任务页完成任务
7. 回到 Dashboard 查看统计变化
8. 触发周复盘查看本周总结

## 技术栈

**前端**
- Vue 3
- Vite
- Vue Router
- Pinia
- Axios

**后端**
- Java 17
- Spring Boot 3.2
- Spring Cloud Gateway
- Spring Cloud Alibaba Nacos
- MyBatis-Plus
- ShardingSphere
- Redis
- RocketMQ
- MySQL

**工程化**
- PowerShell 一键启动脚本
- `.env` 统一配置
- Swagger / OpenAPI
- Maven 多模块工程

## 系统架构

```text
lifeos-web
  -> /api/*
  -> lifeos-gateway
     -> lifeos-user-service
     -> lifeos-note-service
     -> lifeos-task-service
     -> lifeos-ai-service
     -> lifeos-behavior-service

MySQL
Redis
Nacos
RocketMQ
```

各模块职责：
- `lifeos-gateway`：统一入口、JWT 校验、登录态校验、服务转发
- `lifeos-user-service`：注册、登录、用户资料、密码管理
- `lifeos-note-service`：笔记、搜索、复习、AI 作业调度
- `lifeos-task-service`：任务创建、更新、完成、来源笔记关联
- `lifeos-ai-service`：摘要、整理、提取任务、周复盘处理
- `lifeos-behavior-service`：行为埋点、Dashboard 聚合统计
- `lifeos-common`：统一响应、JWT 工具等公共能力
- `lifeos-api`：服务间共享 DTO、Feign 接口、消息常量

## 目录结构

```text
LifeOS
├─ lifeos-web/                       前端工程
├─ lifeos-backend/                   后端多模块工程
│  ├─ lifeos-common/                 公共工具
│  ├─ lifeos-api/                    DTO / Feign / MQ 常量
│  ├─ lifeos-gateway/                网关
│  ├─ lifeos-user-service/           用户服务
│  ├─ lifeos-note-service/           笔记服务
│  ├─ lifeos-task-service/           任务服务
│  ├─ lifeos-ai-service/             AI 服务
│  ├─ lifeos-behavior-service/       行为服务
│  └─ db/                            SQL、迁移、测试种子数据
├─ start-env.ps1                     启动基础依赖
├─ start-backend.ps1                 启动后端服务
├─ start-all.ps1                     一键启动环境和后端
├─ stop-all.ps1                      一键停止
├─ status-all.ps1                    查看整体状态
├─ reset-test-data.ps1               重置测试数据
└─ .env.example                      本地环境变量模板
```

## 快速开始

### 运行要求

- Windows PowerShell
- JDK 17
- Node.js 20 或更高版本
- Maven 3.9 或更高版本
- Docker Desktop

默认依赖环境：
- MySQL
- Redis
- Nacos
- RocketMQ

默认目录：
- `Nacos`：`C:\environment\nacos`
- `RocketMQ`：`C:\environment\rocketmq-all-5.4.0-bin-release`

### 1. 准备配置

复制配置模板：

```powershell
Copy-Item .env.example .env
```

至少确认这些配置已经填写：
- `MYSQL_JDBC_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `NACOS_SERVER_ADDR`
- `LIFEOS_JWT_SECRET`

如果没有配置 `LIFEOS_AI_API_KEY`，AI 服务会使用本地 mock 结果，项目仍可正常演示。

### 2. 启动环境和后端

一键启动：

```powershell
powershell -ExecutionPolicy Bypass -File .\start-all.ps1
```

常用命令：

```powershell
powershell -ExecutionPolicy Bypass -File .\start-env.ps1
powershell -ExecutionPolicy Bypass -File .\start-backend.ps1
powershell -ExecutionPolicy Bypass -File .\status-all.ps1
powershell -ExecutionPolicy Bypass -File .\stop-all.ps1
```

更多说明见 [BACKEND_STARTUP.md](./BACKEND_STARTUP.md)。

### 3. 启动前端

```powershell
cd .\lifeos-web
npm install
npm run dev
```

### 4. 构建项目

前端：

```powershell
cd .\lifeos-web
npm run build
```

后端：

```powershell
cd .\lifeos-backend
mvn test
mvn package -DskipTests
```

## 接口文档

统一 Swagger UI 入口：

```text
http://127.0.0.1:8080/swagger-ui.html
```

聚合后的 OpenAPI JSON：
- `http://127.0.0.1:8080/service-docs/user`
- `http://127.0.0.1:8080/service-docs/note`
- `http://127.0.0.1:8080/service-docs/task`
- `http://127.0.0.1:8080/service-docs/ai`
- `http://127.0.0.1:8080/service-docs/behavior`

## 测试数据

项目支持一键清空并重建测试数据：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\reset-test-data.ps1
```

测试账号统一密码：

```text
Pass123456
```

默认会生成 5 个带真实使用习惯的测试用户：
- `liwen_pm`：产品经理，偏发布、访谈、首页设计
- `zhouyi_dev`：后端开发，偏异步链路、Redis、接口联调
- `heqing_fit`：健身用户，偏训练、饮食、睡眠恢复
- `susu_creator`：内容创作者，偏选题、脚本、品牌合作
- `chenyu_grad`：研究生，偏论文、实验设计、导师反馈

## 核心业务能力

### 用户系统
- 用户注册
- 用户登录
- JWT 登录态
- 用户资料修改
- 密码修改
- 退出登录

### 知识笔记
- 新建笔记
- 编辑笔记
- 删除笔记
- 关键词搜索
- 标签筛选
- 置顶
- 复习状态管理
- 复习队列视图

### AI 工作流
- 异步摘要生成
- 异步整理建议
- 异步任务提取
- 异步周复盘
- AI 作业历史查看

### 任务系统
- 普通任务
- 来源于笔记的任务
- 任务完成
- 与笔记双向关联展示

### Dashboard
- 待复习笔记数
- 本周新增笔记数
- 本周整理完成数
- 待处理 AI 作业
- 待执行知识任务
- 最近更新笔记
- 高频标签

## 项目亮点

- 不是简单 CRUD，而是有明确业务主线的知识管理系统
- 微服务拆分清晰，职责边界明确
- 使用 Gateway + JWT + Redis 完成统一鉴权
- 使用 RocketMQ 实现行为埋点和 AI 作业异步化
- 使用 ShardingSphere 对笔记表做分表
- 支持一键环境启动、测试种子数据和 Swagger 文档

## 当前状态

当前版本已经适合作为校招 / 应届生项目展示。  
它已经具备完整闭环，但仍然定位为“可运行、可演示、可扩展”的项目，而不是面向生产环境的最终版本。

仍可继续改进的方向包括：
- 更严格的安全策略
- 更完整的监控、重试和死信队列
- 更多自动化测试
- 更细粒度的 UTF-8 清理

## License

仅用于学习、演示和个人项目展示。
