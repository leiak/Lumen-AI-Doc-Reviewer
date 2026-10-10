# Lumen-Doc-Reviewer — 多模型文档审查系统

## 1. 项目定位

### 1.1 一句话
将现有代码评审的多模型投票机制迁移到文档审查场景，支持合同、论文、技术方案等结构化文档的多角度并行审查与汇总。

### 1.2 核心问题
- 文档审查通常只看一遍，容易遗漏条款冲突、逻辑漏洞、合规风险。
- 不同角色关注点不同（法务看合规、技术看可行性、业务看商业逻辑），需要多视角。
- 人工评审成本高、周期长。

## 3. 目标用户

| 场景 | 角色 | 关注点 |
|---|---|---|
| 合同审查 | 法务 / 律师 | 条款风险、责任划分、违约条款 |
| 技术方案评审 | 架构师 / 安全 | 可行性、安全性、可维护性 |
| 论文审查 | 审稿人 | 逻辑完整性、数据支撑、引用规范 |
| 内容审核 | 运营 / 合规 | 敏感词、合规、事实准确性 |

## 3. 核心业务流程（逻辑闭环）

``` 

## 17. 规模化技术架构（1000 人企业）

### 17.1 存储升级

| 组件 | MVP | 生产（1000 人） |
|---|---|---|
| 数据库 | SQLite | PostgreSQL 16（JSONB 存 findings，全文搜索检索条款） |
| 文件存储 | 本地磁盘 | MinIO / S3（加密存储 + 版本控制） |
| 缓存 | 无 | Redis（模板缓存 + 限流） |
| 消息队列 | 无 | RabbitMQ（文档解析异步任务） |
| 部署 | 单 JAR | Docker → K8s |
| OCR | Tesseract（本地） | PaddleOCR Service（独立容器）+ Google Vision fallback |

### 17.2 可扩展性设计

| 维度 | 方案 |
|---|---|
| 文档解析异步 | 上传 → MQ → 解析 Worker 池（按文档类型分区） |
| 大文档分块 | 按章节分块并行评审，结果合并 |
| 多语言并发 | 不同语言文档路由到不同 Worker |
| 文件加密 | S3 SSE-KMS 加密 + 传输 TLS |
| 数据保留策略 | 按文档类型配置保留期（合同 7 年，一般文档 1 年） |
| 审计追踪 | 所有查看/修改/导出操作记录 audit_log |

### 17.3 SSO 与权限

```yaml
auth:
  provider: saml
  groups:
    - group: legal-team
      permission: review_and_annotate
    - group: tech-leads
      permission: review
    - group: hr
      permission: upload_and_view
  document-access:
    - pattern: "**/contracts/**"
      read: [legal-team, exec-team]
    - pattern: "**/technical/**"
      read: [tech-leads, engineering]
```

## 18. 业务场景全覆盖（1000 人企业）

### 18.1 多部门场景

| 部门 | 文档类型 | 审查角色 | 合规要求 |
|---|---|---|---|
| 法务 | 合同 / NDA / 政策 | 条款风险 / 合规 | 审计日志 7 年 |
| 技术 | 方案 / RFC / API 文档 | 可行性 / 安全 | 版本追溯 |
| HR | 政策 / Offer / 竞业 | 合规 / 公平性 | 隐私保护 |
| 财务 | 采购 / 预算 / 报告 | 准确性 / 合规 | SOX 审计 |
| 安全 | 安全策略 / 事件报告 | 完整性 / 合规 | ISO 27001 |

### 18.2 审批流集成

```yaml
workflow:
  contract-review:
    steps:
      - reviewer: ai_multi_model
        output: findings_report
      - reviewer: legal_counsel
        action: confirm_or_edit_findings
      - approver: department_head
        action: approve_for_signature
      - executor: docsign_integration
  escalation:
    - if: critical_finding_unresolved_48h
      action: notify_gc
```

### 18.3 文档模板管理

- 总部维护标准模板（NDA / 采购 / 劳动合同）
- 各地区可自定义本地化模板（劳动法差异）
- 模板版本管理 + 变更审批
- 模板使用统计（哪个模板使用频率高，AI 发现的共性问题）

## 19. 开源竞品借鉴

### 19.1 竞品对比

| 项目 | 核心差异 | 可借鉴点 |
|---|---|---|
| [claude-legal-skill](https://github.com/evolsb/claude-legal-skill) | 基于 CUAD 数据集（41 类法律风险），位置感知审查 | **CUAD 风险分类**（41 种风险类型作为审查 checklist）；**立场感知**（"我是买方" → 审查角度不同） |
| [AI Legal Claude](https://github.com/ZubairTrabzada/ai-legal-claude) | 5 个并行 agent，Contract Safety Score (0-100) | **合同安全分数**（量化输出，用户一眼看到风险等级）；**/legal compare**（合同版本 diff，只审变更部分） |
| [legal-redline-tools](https://github.com/evolsb/legal-redline-tools) | 从 JSON redlines 生成 Word Track Changes / PDF annotation | **结构化红线输出**：AI 输出 JSON → 工具渲染为 Word/PDF 修订标记 |
| LexNLP / OpenContracts | 学术 NLP 库，条款提取 | 条款自动分段（clause segmentation） |

### 19.2 直接借鉴的设计

```yaml
# 1. CUAD 风险分类体系（直接引入为 checklist）
risk-categories:  # 来自 CUAD 数据集
  - id: liability_cap
    name: "责任上限"
    check: "是否有合理的责任上限条款"
  - id: indemnification
    name: "赔偿条款"
    check: "赔偿范围是否对等"
  # ... 41 类完整清单

# 2. 立场感知审查（从 claude-legal-skill 借鉴）
position-aware:
  enabled: true
  positions: [buyer, seller, customer, vendor, receiving_party, disclosing_party]
  example: "审查保密条款时：如果我是接收方 → 关注保密期限是否过长、定义是否过宽"

# 3. 合同安全分数（从 AI Legal Claude 借鉴）
safety-score:
  range: 0-100
  calculation: "critical(-15) + major(-8) + minor(-3) 从 100 起扣"
  display: "报告首页大字显示 + 颜色（绿>80 / 黄50-80 / 红<50）"

# 4. 版本对比（从 /legal compare 借鉴）
version-diff:
  enabled: true
  strategy: "diff v1/v2 → 只审变更条款 → 输出增量报告（而非全量重审）"
```
用户上传文档（PDF/Word/Markdown）
  → 文档解析器提取文本 + 结构（章节/条款/段落）
  → 按角色拆分审查任务（法务/技术/合规…）
  → 多模型并行审查（每个角色用不同模型）
  → 每个 reviewer 输出结构化 findings
  → Aggregator 汇总去重（复用现有 Jaccard）
  → HumanGate 判定（高风险条款需人工确认）
  → 输出审查报告（Markdown/HTML/PDF）
  → 用户确认/修改意见
  → 意见反馈存储 → 用于优化审查 prompt → 闭环
```

## 4. 状态图拓扑

```
[parse_document]
    ↓
[segment_document]  (按章节/条款分块)
    ↓
┌─ [reviewer_legal]  ─┐
├─ [reviewer_tech]   ─┤  并行
├─ [reviewer_compliance]─┤
└─ [reviewer_business]─┘
    ↓
[aggregate_findings]
    ↓
[human_gate]  (critical 需人工确认)
    ↓
[generate_report]
    ↓
[user_feedback]  (确认/驳回意见)
    ↓
[update_prompts]  (闭环：反馈优化审查规则)
```

## 5. 配置示例

```yaml
council:
  name: doc-review-legal
  version: "1.0"
reviewers:
  - role: legal_risk
    model: claude-sonnet-5
    prompt: "审查以下合同条款，识别：责任不对等、违约风险、模糊表述、单方权利条款"
    output-schema:
      - clause_id: string
      - risk_type: enum[risk, ambiguity, missing_protection]
      - severity: enum[critical, major, minor, info]
      - description: string
      - suggested_revision: string
  - role: compliance
    model: deepseek-chat
    prompt: "检查以下条款是否符合相关法规要求"
  - role: business_logic
    model: minimax-text-01
    prompt: "从商业角度审查条款是否合理、是否存在商业漏洞"
document:
  format: [pdf, docx, md]
  chunk-by: section       # section / clause / paragraph / token
  max-chunk-tokens: 30000
```

## 6. 数据模型

```sql
CREATE TABLE document_sessions (
  id TEXT PRIMARY KEY,
  doc_name TEXT,
  doc_type TEXT,              -- contract / proposal / paper / other
  file_path TEXT,
  status TEXT,                -- pending / reviewing / done / archived
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE document_segments (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  session_id TEXT REFERENCES document_sessions(id),
  section_path TEXT,          -- e.g. "第3条/第2款"
  content TEXT,
  token_count INTEGER
);

CREATE TABLE doc_findings (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  session_id TEXT,
  segment_id INTEGER,
  reviewer_role TEXT,
  severity TEXT,
  risk_type TEXT,
  description TEXT,
  suggested_revision TEXT,
  user_action TEXT DEFAULT NULL   -- accepted / rejected / modified
);
```

## 7. API 设计

```
POST /api/doc/upload               → 上传文档，创建 session
GET  /api/doc/{id}/status          → 审查进度
GET  /api/doc/{id}/findings        → 结构化 findings
GET  /api/doc/{id}/report          → Markdown/HTML 报告
POST /api/doc/{id}/feedback        → 提交用户意见（accept/reject/modify）
GET  /api/doc/{id}/segments        → 分段详情
```

## 8. 里程碑

| 阶段 | 内容 | 预估 |
|---|---|---|
| M1 | 文档解析（PDF/DOCX）+ 分块 | 1.5 周 |
| M2 | 合同审查角色配置 + 多模型评审 | 1 周 |
| M3 | 报告生成 + 用户反馈循环 | 1 周 |
| M4 | 论文/技术方案模板 | 1 周 |

## 9. 成功指标

- 法务确认 AI 发现的风险中有真阳性的比例 > 80%
- 审查一份 20 页合同的时间 < 10 分钟（人工约 2-4 小时）
- 用户采纳建议修订的比例 > 40%

## 10. 风险

| 风险 | 应对 |
|---|---|
| PDF 解析质量差 | 多解析引擎 fallback（pdfplumber → OCR） |
| 法律领域 hallucination | 明确声明"仅供参考，不构成法律意见" |
| 文档过长超出上下文 | 分块已有 + 跨块上下文摘要传递 |

## 11. 深度拓展：模板库与行业套件

### 11.1 概念
不同行业有不同的文档审查需求，通过预配置的行业角色模板降低使用门槛。

### 11.2 内置模板
| 行业 | 模板名 | 角色 |
|---|---|---|
| 法务 | NDA 审查 | 条款风险 / 保密范围 / 违约责任 / 期限合理性 |
| 法务 | 劳动合同审查 | 权利义务 / 竞业条款 / 解雇条件 |
| 法务 | 采购合同审查 | 付款条件 / 交付标准 / 违约金 / 争议解决 |
| 金融 | 贷款协议审查 | 利率条款 / 提前还款 / 违约后果 |
| 软件 | 技术方案评审 | 可行性 / 安全性 / 可扩展性 / 成本估算 |
| 软件 | API 设计评审 | 命名规范 / RESTful 合规 / 版本策略 / 向后兼容 |
| 医疗 | 知情同意书审查 | 风险告知完整性 / 患者权利 / 责任划分 |
| 教育 | 课程内容审查 | 事实准确性 / 适龄性 / 版权合规 |

### 11.3 自定义模板
用户可基于模板创建专属审查方案，保存为 YAML，分享给团队。

## 12. 深度拓展：红线标注与协作

### 12.1 Redlining 模式
将 AI 建议的修订以 Word Track Changes 或 PDF annotation 的形式标注在原文档上，用户逐条接受/拒绝，类似 Grammarly 的交互方式。

### 12.2 版本对比
```
上传合同 v1 → 审查 → 输出报告
对方修改后上传 v2 → 系统自动 diff v1/v2 → 只审查变更部分 → 增量报告
```

### 12.3 多人协作
- 法务 + 技术同时查看审查报告，各自标注自己关注的 findings
- findings 可 assign 给不同的人处理
- 处理进度追踪（待处理 / 已处理 / 已忽略）

## 13. 深度拓展：OCR 与多语言

### 13.1 OCR
扫描版 PDF 通过 OCR 引擎（Tesseract / PaddleOCR / Google Vision）提取文字后进入标准流程。

### 13.2 多语言审查
- 英文合同用英文 prompt 审查，输出英文报告
- 中英双语合同同时审查两个语言版本的一致性
- 跨语言合同（中文主体 + 英文附录）检查条款是否对等

## 14. 商业模式

| 层级 | 定价 | 包含功能 |
|---|---|---|
| Free | $0 | 每月 5 份文档，单一角色审查 |
| Pro | $29/月 | 无限文档，多角色审查，红线标注，报告导出 |
| Business | $99/月/团队 | 团队协作，自定义模板，API 接入 |
| Enterprise | 定制 | 私有部署，行业定制，合规审计日志，SLA |

### 差异化
- vs 人工律所：速度快 10-20 倍，成本低 90%，且覆盖面广（不会遗漏）
- vs LawGeex 等竞品：多模型投票而非单模型，覆盖场景更广（不仅限合同）

## 15. 生态集成

| 优先级 | 集成 | 价值 |
|---|---|---|
| P0 | PDF / DOCX 解析 | 基础功能 |
| P1 | DocuSign / Adobe Sign | 审查通过后直接签署 |
| P1 | Salesforce / HubSpot | 合同审批流程集成 |
| P2 | SharePoint / Google Drive | 文档自动同步审查 |
| P3 | iManage / NetDocuments | 法律行业 DMS 集成 |

## 16. 技术路线（Java 技术栈）

### 16.1 基础框架

| 层级 | 组件 | 版本 | 用途 |
|---|---|---|---|
| 运行时 | Java | 21 | LTS |
| 框架 | Spring Boot | 3.3.5 | Web / Validation |
| AI 接入 | Spring AI | 1.1.0 | 多模型 ChatClient |
| 流程编排 | LangGraph4j | 1.6.0 | 文档审查拓扑 |
| 持久化 | SQLite | 3.45.3.0 | sessions / segments / findings |
| 序列化 | Jackson | Spring Boot 管理 | JSON / YAML |

### 16.2 新增组件

| 组件 | 用途 | Maven 坐标 |
|---|---|---|
| Apache PDFBox | PDF 解析 | `org.apache.pdfbox:pdfbox:3.0.2` |
| Apache POI | DOCX 解析 | `org.apache.poi:poi-ooxml:5.2.5` |
| Tika | 通用文档类型检测 | `org.apache.tika:tika-core:2.9.1` |
| PDFLayoutText | PDF 保留布局解析 | `org.apache.pdfbox:pdfbox-tools:3.0.2` |
| iText 7 | PDF annotation / redline | `com.itextpdf:itext7-core:8.0.3` |

### 16.3 核心实现路径

```
src/main/java/com/docreview/
├── config/
│   └── DocReviewConfig.java      // 模板 YAML 解析
├── parser/
│   ├── DocumentParser.java       // 接口
│   ├── PdfParser.java            // PDFBox 实现
│   ├── DocxParser.java           // POI 实现
│   └── OcrParser.java            // Tesseract / PaddleOCR
├── segment/
│   ├── DocumentSegmenter.java    // 按章节/条款分块
│   └── Segment.java
├── graph/
│   ├── DocReviewTopology.java    // LangGraph4j 拓扑
│   └── DocReviewRunner.java
├── reviewer/
│   └── DocReviewerNode.java      // 复用 GenericReviewerNode 模式
├── annotation/
│   └── PdfAnnotator.java         // iText 红线标注
├── report/
│   └── DocReportRenderer.java
└── persistence/
    ├── entity/
    │   ├── DocumentSessionEntity.java
    │   ├── DocumentSegmentEntity.java
    │   └── DocFindingEntity.java
    └── repository/
        ├── DocumentSessionRepository.java  // extends JpaRepository
        ├── DocumentSegmentRepository.java
        └── DocFindingRepository.java
```

### 16.5 JPA 抽象持久化（数据库可切换）

**Entity 示例：**
```java
@Entity
@Table(name = "doc_findings")
public class DocFindingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private Long segmentId;

    private String reviewerRole;
    private String severity;
    private String riskType;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String suggestedRevision;

    private String userAction;        // accepted / rejected / modified

    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

**Repository 示例：**
```java
public interface DocFindingRepository extends JpaRepository<DocFindingEntity, Long> {
    List<DocFindingEntity> findBySessionIdOrderBySeverity(String sessionId);
    List<DocFindingEntity> findBySessionIdAndUserActionIsNull(String sessionId);
}
```

**数据库切换：**
```yaml
# MVP
spring.datasource.url: jdbc:sqlite:./data/docreview.db

# 生产
# spring.datasource.url: jdbc:postgresql://localhost:5432/docreview
```
