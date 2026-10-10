# 文档审查系统 MVP 实施计划

**Goal:** 实现文档上传、分段、多模型审查、报告生成和反馈闭环的可用 MVP。

**Architecture:** Spring Boot 后端提供 REST API，静态单页前端负责上传与结果查看。解析、分段、审查、聚合、报告各自独立。AI 调用抽象为 Mock 和 OpenAI 兼容两种实现。

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring Data JPA, SQLite, PDFBox, Apache POI, Jackson YAML, vanilla HTML/CSS/JavaScript.

**Spec:** docs/superpowers/specs/2026-10-06-doc-review-mvp-design.md

## Global Constraints

- Java 21，包根为 `com.docreview`。
- SQLite MVP，数据库路径 `./data/docreview.db`。
- 上传文件保存在 `./data/uploads`。
- 所有模型调用输出必须映射为结构化 finding。
- 报告必须包含 AI 结果不构成法律意见的免责声明。

## Tasks

1. 建立 Maven、Spring Boot、SQLite 配置和启动类。
2. 实现 Markdown/TXT、DOCX、PDF 解析，以及 section/clause/paragraph/token 分段。
3. 建立 sessions、segments、findings 的 JPA 实体和 Repository。
4. 实现 YAML 模板加载、Mock 审查、OpenAI 兼容审查、Jaccard 去重和风险评分。
5. 实现异步编排、Markdown/HTML 报告、REST API 和异常处理。
6. 实现单页 Web 界面，覆盖上传、进度、结果和反馈。
7. 编写 README，运行测试并构建可执行 JAR。
