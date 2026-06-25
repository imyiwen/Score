---
name: ScoreCheck 成绩管理系统 - 可爱风格 Vue 前端
overview: 基于现有 Spring Boot 后端，开发一个可爱风格、交互丰富的 Vue 3 前端。系统包含：登录页（带滑块验证码）、学生成绩查询页、教师工作台（成绩管理、学生管理、用户管理）。
design:
  architecture:
    framework: vue
  styleKeywords:
    - 可爱
    - 糖果色
    - 圆角
    - 微动画
    - 卡片式
    - 友好交互
  fontSystem:
    fontFamily: Source Han Sans SC
    heading:
      size: 28px
      weight: 700
    subheading:
      size: 20px
      weight: 500
    body:
      size: 15px
      weight: 400
  colorSystem:
    primary:
      - "#FF6B9D"
      - "#FF8FB1"
      - "#FFB3C6"
    background:
      - "#FFF5F8"
      - "#FFFFFF"
      - "#F8F4FF"
    text:
      - "#4A4A4A"
      - "#7A7A7A"
      - "#FFFFFF"
    functional:
      - "#7C5CFF"
      - "#FFD93D"
      - "#4CAF50"
      - "#FF5252"
todos:
  - id: init-vue-project
    content: 使用Vite初始化Vue 3 + TypeScript项目，配置Element Plus和路由
    status: completed
  - id: create-api-modules
    content: 创建API封装模块，实现验证码、登录、成绩、用户等接口调用
    status: completed
    dependencies:
      - init-vue-project
  - id: build-slider-captcha
    content: 开发滑块验证码交互组件，支持拖拽验证
    status: completed
    dependencies:
      - init-vue-project
  - id: implement-login-page
    content: 实现登录页面，整合验证码和表单提交
    status: completed
    dependencies:
      - build-slider-captcha
  - id: create-student-query
    content: 开发学生成绩查询页面，表单+成绩卡片展示
    status: completed
    dependencies:
      - init-vue-project
  - id: build-teacher-dashboard
    content: 实现教师仪表盘，成绩表格和筛选功能
    status: completed
    dependencies:
      - create-api-modules
  - id: develop-user-management
    content: 开发用户管理页面，CRUD操作界面
    status: completed
    dependencies:
      - create-api-modules
  - id: create-import-page
    content: 实现Excel导入页面，支持学生信息和成绩导入
    status: completed
    dependencies:
      - create-api-modules
  - id: add-animations-styles
    content: 添加可爱动画效果和全局样式优化
    status: completed
    dependencies:
      - implement-login-page
  - id: test-integration
    content: 集成测试，验证前后端联调
    status: completed
    dependencies:
      - develop-user-management
---

## 产品概述

ScoreCheck成绩管理系统前端是一个可爱的Vue 3单页应用，为学生和教师提供成绩查询、成绩管理等功能。

## 核心功能

### 1. 学生端

- **成绩查询**：输入姓名和身份证号查询个人各科目成绩
- **成绩展示**：以卡片形式展示各科目分数，支持总分统计

### 2. 教师端

- **滑块验证码登录**：拖拽滑块完成人机验证
- **成绩查询**：按班级、姓名模糊查询学生成绩
- **Excel导入**：上传学生信息和成绩Excel文件
- **用户管理**：增删改查教师账户

### 3. 通用

- **可爱动画交互**：按钮悬停、页面切换、加载状态等动画效果
- **响应式布局**：适配不同屏幕尺寸
- **友好提示**：Toast通知、确认对话框等交互反馈

## 页面规划

- 登录页：验证码+登录表单
- 学生成绩查询页：查询表单+成绩展示
- 教师控制台：仪表盘+成绩表格
- 用户管理页：用户列表+CRUD操作
- 导入管理页：学生信息导入+成绩导入

## 技术栈选择

- **框架**：Vue 3 + TypeScript
- **构建工具**：Vite 5
- **UI组件库**：Element Plus（启用可爱主题配置）
- **状态管理**：Pinia
- **路由**：Vue Router 4
- **HTTP客户端**：Axios
- **图标**：Lucide Icons
- **样式**：SCSS + CSS变量

## 技术架构

### 目录结构

```
score-frontend/
├── src/
│   ├── api/              # API接口封装
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── composables/      # 组合式函数
│   ├── layouts/          # 布局组件
│   ├── router/           # 路由配置
│   ├── stores/           # Pinia状态管理
│   ├── styles/           # 全局样式
│   ├── types/            # TypeScript类型定义
│   ├── utils/            # 工具函数
│   ├── views/            # 页面组件
│   │   ├── Login.vue
│   │   ├── StudentQuery.vue
│   │   ├── TeacherDashboard.vue
│   │   ├── UserManagement.vue
│   │   └── ImportData.vue
│   ├── App.vue
│   └── main.ts
├── public/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

### 核心模块设计

#### API封装模块 (api/)

- `captcha.ts`：验证码获取和校验
- `auth.ts`：登录登出接口
- `score.ts`：成绩查询接口
- `student.ts`：学生管理接口
- `user.ts`：用户管理接口

#### 状态管理模块 (stores/)

- `auth.ts`：认证状态（Token、用户信息）
- `score.ts`：成绩数据缓存

#### 关键组件

- `SliderCaptcha.vue`：滑块验证码组件
- `ScoreCard.vue`：成绩卡片组件
- `DataTable.vue`：数据表格组件
- `UploadExcel.vue`：Excel上传组件

### 数据流设计

```
用户操作 → Vue组件 → Pinia Store → Axios API → 后端接口
              ↑                                    ↓
              └───────── 响应数据 ─────────────────┘
```

### 关键实现细节

#### 滑块验证码交互

- 拖拽滑块背景图移动拼图
- 拖拽完成调用`/captcha/check`验证
- 验证成功后触发登录请求
- 支持重新生成验证码

#### 成绩展示

- 组件化展示各科目成绩
- 计算并显示总分
- 支持按考试名称分组展示

#### Excel导入

- 使用el-upload组件
- 显示上传进度
- 成功后显示导入结果

### 性能优化

- 路由懒加载
- 组件按需引入
- 请求拦截器统一处理Token

## 设计风格

采用可爱活泼的糖果色系设计，营造轻松愉悦的用户体验。

### 配色方案

- **主色**：#FF6B9D（樱花粉）- 用于重要按钮和强调
- **辅色**：#7C5CFF（梦幻紫）- 用于辅助元素
- **点缀**：#FFD93D（柠檬黄）- 用于提示和徽章
- **背景**：#FFF5F8（淡粉白）- 页面整体背景
- **卡片**：#FFFFFF（纯白）- 卡片背景
- **文字**：#4A4A4A（深灰）- 主要文字

### 圆角设计

- 按钮：12px圆角
- 卡片：16px圆角
- 输入框：10px圆角

### 动画效果

- 页面切换：淡入淡出 + 轻微上移（300ms）
- 按钮悬停：缩放1.05 + 阴影增强
- 卡片悬停：轻微上浮 + 阴影增强
- 加载状态：可爱的旋转圆圈
- Toast提示：从右侧滑入

### 字体系统

- 标题：思源黑体 Bold, 24-32px
- 副标题：思源黑体 Medium, 18-20px
- 正文：思源黑体 Regular, 14-16px
- 按钮：思源黑体 Medium, 14px

## Agent Extensions

### Skill

- **lucide-icons**
- 用途：搜索和下载可爱的SVG图标
- 预期效果：获取所有UI所需的图标资源（箭头、搜索、上传、用户、锁等图标）

### Skill

- **modern-web-app**
- 用途：参考Vue项目初始化最佳实践
- 预期效果：确保Vue 3 + TypeScript + Vite项目结构规范