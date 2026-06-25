# Score 后端 QA 审计报告

> **审计日期**: 2026-06-23
> **审计人**: 资深软件测试专家
> **项目版本**: Spring Boot 3.2.10 + MyBatis-Plus 3.5.7 + Sa-Token 1.45.0
> **审计范围**: `src/main/java/com/score/` 全量代码

---

## 一、审计摘要

| 严重程度 | 数量 | 关键主题 |
|----------|------|---------|
| 🔴 Critical | 5 | Token 泄露、导入必挂、PII 泄露、内存泄漏、SQL 注入面 |
| 🟠 High | 8 | 权限缺失、CSRF、OOM 风险、竞态条件、N+1 查询、无分页、脏数据 |
| 🟡 Medium | 10 | NPE、键碰撞、事务不回滚、代码质量 |
| 🔵 Low | 7 | 监控缺失、限流缺失、配置问题 |

**总体评估**: 功能完整，但存在多个**上线必挂**和**安全漏洞**，需分三个阶段修复后方可投产。

---

## 二、Critical（严重 — 上线必出事）

### C1. 登录接口把 Token 明文返回给前端

- **文件**: `service/impl/ScoreServiceImpl.java:137`
- **代码**:
  ```java
  return ResultVo.success(StpUtil.getTokenValue()+"登录成功");
  ```
- **问题**: Token 值直接拼接到响应体里返回。任何中间件（Nginx 日志、代理、浏览器 DevTools）都能看到完整 Token。攻击者截获后可直接冒充教师登录。
- **修复**: 返回结构化 JSON `ResultVo.success(Map.of("token", tokenValue))`。

---

### C2. 成绩导入双重读 InputStream（必定失败）

- **文件**: `service/impl/ScoreServiceImpl.java:160-166`
- **代码**:
  ```java
  // 第一次读表头
  List<Map<Integer, String>> headList = EasyExcel.read(file.getInputStream()...).doReadSync();
  // 第二次读数据 — 流已耗尽！
  List<Map<Integer, String>> dataList = EasyExcel.read(file.getInputStream()...).doReadSync();
  ```
- **问题**: `MultipartFile.getInputStream()` 是一次性流。第一次读完位置到 EOF，第二次要么返回空数据，要么抛异常。**生产环境导入成绩一定会失败。**
- **修复**: 先把文件内容读入 `byte[]`，然后从 `new ByteArrayInputStream(byte[])` 创建两个独立的流。

---

### C3. 日志里打身份证号（PII 泄露 + 静默失效）

- **文件**: `controller/ScoreCheckController.java:23`
- **代码**:
  ```java
  log.info("学生：{}|查询成绩", bo.getStudentName(), bo.getIdCard());
  ```
- **问题**:
  1. SLF4J 的 `log.info` 只认第一个 `{}` 占位符，`bo.getIdCard()` 作为多余参数被**静默丢弃**——日志根本打不出身份证号，白写了。
  2. 即使打出来也是违规的，身份证号属于敏感个人信息不应落盘。
- **修复**: 去掉身份证号日志，或脱敏后记录：`log.info("学生：{} 查询成绩", bo.getStudentName());`

---

### C4. 验证码 session 属性永不清理（内存泄漏）

- **文件**: `controller/CaptchaController.java:26`
- **代码**:
  ```java
  session.setAttribute("captcha_x_"+captchaId, result.getX());
  ```
- **问题**: `captcha_x_` 属性在验证码校验后**从未删除**。每次请求生成新验证码都会在 session 里残留一个属性。高并发下 session 内存持续增长，最终 OOM。
- **修复**: 校验通过后删除 `captcha_x_` 属性，或给 session 设超时时间。

---

### C5. `${ew.customSqlSegment}` SQL 注入面

- **文件**: `mapper/ScoreMapper.java:23`
- **代码**:
  ```java
  @Select("SELECT * FROM score ${ew.customSqlSegment}")
  ```
- **问题**: `${}` 是字符串直接拼接，不是 `#{}` 参数绑定。虽然 MyBatis-Plus 的 Wrapper 通常安全，但配合 `@InterceptorIgnore(tenantLine="true")` 绕过多租户隔离后，如果 Wrapper 被恶意构造，存在注入风险。
- **修复**: 改用 `BaseMapper.selectList()` 或确保 Wrapper 构造路径可控。

---

## 三、High（高危 — 功能缺陷 / 安全风险）

### H1. 无任何权限分级 — 所有登录用户可做所有操作

- **文件**: `config/SaTokenConfig.java:19-31`
- **代码**:
  ```java
  SaRouter.match("/**")
      .notMatch("/login", "/StudentCheck", ...)
      .check(r -> StpUtil.checkLogin());  // 只检查是否登录，不检查角色
  ```
- **问题**: 任何已登录教师都能调用 `creatAdmin`（创建管理员）、`deleteUser`（删别人账号）、`importScore`（导入成绩）。没有 RBAC，一个普通教师可以给自己提权。
- **修复**: 加角色校验，如 `StpUtil.checkRole("admin")` 区分超级管理员和普通教师。

---

### H2. deleteUser 用 GET 方法（CSRF 漏洞）

- **文件**: `controller/ScoreCheckController.java:67-68`
- **代码**:
  ```java
  @GetMapping("/deleteUser")
  public ResultVo<?> deleteUser(@RequestParam String userName)
  ```
- **问题**:
  1. GET 应该是幂等安全的。用 GET 做删除操作违反 REST 规范。
  2. 可被 CSRF 攻击（`<img src="/scoreCheck/deleteUser?userName=admin">`）。
  3. 浏览器预加载可能误删。
- **修复**: 改为 `@DeleteMapping("/deleteUser/{userName}")`。

---

### H3. 成绩导入无文件大小限制

- **文件**: `service/impl/ScoreServiceImpl.java:157`
- **代码**:
  ```java
  public ResultVo<?> importScores(MultipartFile file, ...)
  ```
- **问题**: `application.yml` 没有配置 `spring.servlet.multipart.max-file-size`，代码也没校验 `file.getSize()`。攻击者可上传 GB 级文件导致 OOM。
- **修复**: 加配置 `max-file-size: 10MB` + 代码里校验 `file.getSize()` + 校验文件类型（`content-type` 是否含 `excel`）。

---

### H4. 学生导入竞态条件

- **文件**: `Listener/StudentImportListener.java:64-79`
- **代码**:
  ```java
  Student existing = studentMapper.selectOne(queryWrapper);
  if (existing != null) { /* update */ } else { /* insert */ }
  ```
- **问题**: SELECT-then-INSERT/UPDATE 不是原子操作。两个导入任务同时跑，都可能查到 `existing == null`，然后都执行 INSERT，产生主键冲突或重复数据。
- **修复**: 在数据库层加 `(id_card, class_name)` 唯一约束 + `ON DUPLICATE KEY UPDATE`。

---

### H5. 成绩导入 N+1 查询

- **文件**: `service/impl/ScoreServiceImpl.java:173-176`
- **代码**:
  ```java
  for (Map<Integer, String> data : dataList) {
      Student student = studentMapper.selectOne(queryWrapper);  // 每行一次查询
  ```
- **问题**: Excel 有 1000 行 = 1000 次 SELECT 查询。应该先批量查出该班所有学生到 `Map<String, Student>`，然后在内存里查找。

---

### H6. checkList 无分页，全表加载

- **文件**: `controller/ScoreCheckController.java:33-37`
- **代码**:
  ```java
  @PostMapping("/checkList")
  public ResultVo<?> checkList(@RequestBody StudentQueryScoreBo searchBo)
  ```
- **问题**: `StudentQueryScoreBo` 里有 `pageNum`/`pageSize` 字段但**完全没用**。`checkList()` 用 `selectList()` 查出所有匹配记录再内存分组。一个班 500 学生 × 10 考试 × 8 科 = 40000 条全拉进内存。
- **修复**: 用 MyBatis-Plus 的 `Page<>` 做数据库层分页，或至少加 `LIMIT`。

---

### H7. 成绩导入硬编码列索引

- **文件**: `service/impl/ScoreServiceImpl.java:170`
- **代码**:
  ```java
  String studentName = data.get(1); // 假设姓名在第2列
  for (int i = 2; i < headMap.size(); i++) { ... }
  ```
- **问题**: 列索引硬编码。Excel 模板稍有变化（如多了一列"学号"或删除了"学号"），导入会静默出错——数据不对但不报错。
- **修复**: 从表头建立 `columnName -> columnIndex` 的 Map，用列名匹配而不是列索引。

---

### H8. importScores 捕获异常后事务不回滚

- **文件**: `service/impl/ScoreServiceImpl.java:155, 203-206`
- **代码**:
  ```java
  @Transactional(rollbackFor = Exception.class)
  @Override
  public ResultVo<?> importScores(...) {
      try {
          // ... 插入数据
      } catch (Exception e) {
          log.error("解析成绩异常", e);
          return ResultVo.error("解析Excel失败：" + e.getMessage());
      }
  }
  ```
- **问题**: `@Transactional` 依赖异常传播来触发回滚。这里 catch 了 Exception 并返回错误结果，**异常没有抛出**，前面已经插入的数据**不会回滚**，产生脏数据。
- **修复**: 手动调用 `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()`，或直接让异常往外抛。

---

## 四、Medium（中等 — 潜在 Bug / 代码质量）

### M1. queryScore 空指针风险

- **文件**: `service/impl/ScoreServiceImpl.java:65`
- **代码**:
  ```java
  BigDecimal totalScore = scores.stream()
      .map(Score::getScore)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
  ```
- **问题**: 如果某条记录的 `score` 字段为 null，`map` 会产生 null 元素，`BigDecimal::add` 遇到 null 直接 NPE。`checkList` 里做了 null 处理（第 91 行），但 `queryScore` 没做。
- **修复**: `.map(s -> s.getScore() != null ? s.getScore() : BigDecimal.ZERO)`

---

### M2. checkList 分组键碰撞

- **文件**: `service/impl/ScoreServiceImpl.java:81`
- **代码**:
  ```java
  String rowKey = s.getStudentName()+"_"+s.getExamName();
  ```
- **问题**: 用字符串拼接做 Map 的 key。如果学生名叫"张_三"或考试名"期末_上"，可能和其他组合产生碰撞（如"张三"+"期末_上" vs "张_三"+"期末"+"_上"）。
- **修复**: 用自定义对象（如 `record(StudentExamKey(String name, String exam))`）做 key。

---

### M3. login 方法先操作 session 后校验参数

- **文件**: `service/impl/ScoreServiceImpl.java:106-116`
- **代码**:
  ```java
  // 先查 session、删 session 属性
  Boolean verified = (Boolean) session.getAttribute("captcha_verified_"+captchaId);
  session.removeAttribute("captcha_verified_"+captchaId);
  // 后才校验 bo 是否为 null
  if (bo == null || !StringUtils.hasText(bo.getUserName())) { ... }
  ```
- **问题**: 即使 `bo` 为 null，session 属性已经被删除了。输入校验应该在副作用操作之前。
- **修复**: 把 `bo == null` 检查移到最前面。

---

### M4. 登录接口返回格式粗糙

- **文件**: `service/impl/ScoreServiceImpl.java:137`
- **代码**:
  ```java
  return ResultVo.success(StpUtil.getTokenValue()+"登录成功");
  ```
- **问题**: Token 和中文拼成一个字符串让前端自己 parse。前端还要写 `replace("登录成功", "").trim()` 来提取。不符合 API 设计规范。
- **修复**: `ResultVo.success(Map.of("token", tokenValue))`。

---

### M5. 无 Bean Validation 注解

- **文件**: `entity/bo/AdminBo.java`, `entity/bo/StudentQueryScoreBo.java`
- **问题**: 所有 Controller 方法接收的参数都没有 `@Valid` / `@NotBlank` / `@Size` 等注解。空值检测全靠手动 `StringUtils.hasText()`，不一致且容易遗漏。
- **修复**: BO 上加 `@Data @Validated`，字段上加 `@NotBlank(message = "xxx不能为空")`。

---

### M6. Admin.password 被 @Data.toString() 包含

- **文件**: `entity/Admin.java:17`
- **代码**:
  ```java
  @Data
  ```
- **问题**: `@Data` 自动生成 `toString()` 包含所有字段，包括 `password`。如果任何地方 `log.info("admin: {}", admin)`，密码直接打出来。
- **修复**: `Admin.password` 加 `@JsonIgnore`，或自定义 `toString()`。

---

### M7. 死代码 — ScoreImportExcelVo 从未使用

- **文件**: `entity/vo/ScoreImportExcelVo.java`
- **问题**: 定义了 `studentNo`、`studentName`、`subjectScores` 三个字段，但 `importScores()` 完全没用这个类，而是自己用 `Map<Integer, String>` 重写了 Excel 解析。
- **修复**: 删除此文件。

---

### M8. 无输入格式校验

- **文件**: `entity/bo/StudentQueryScoreBo.java`
- **问题**: 身份证号没有格式校验（应为 18 位），姓名没有长度限制。恶意传超长字符串或特殊字符可能导致意外行为。
- **修复**: `@Pattern(regexp = "^\\d{17}[\\dX]$")` 校验身份证号。

---

### M9. 未配置 spring.servlet.multipart

- **文件**: `resources/application.yml`
- **问题**: 没有 `max-file-size`、`max-request-size` 配置，Spring Boot 默认 1MB。Excel 稍大就上传失败。
- **修复**: 加 `spring.servlet.multipart.max-file-size: 10MB`。

---

### M10. 重复依赖

- **文件**: `pom.xml:33-36` 和 `pom.xml:47-50`
- **代码**:
  ```xml
  <!-- spring-boot-starter-web 声明了两次 -->
  ```
- **问题**: 无功能性危害，但说明代码审查不严。
- **修复**: 删除重复声明。

---

## 五、Low（低优 — 改进建议）

| # | 问题 | 建议 |
|---|------|------|
| L1 | 全局异常直接返回 `e.getMessage()` | 前端只看到"系统异常"，详情记日志 |
| L2 | 无 `/actuator/health` | 加 `spring-boot-starter-actuator` |
| L3 | 无速率限制 | 登录接口限制 5 次/分钟，查询接口限制 20 次/分钟 |
| L4 | `sa-token.is-log: true` | 生产环境评估是否关闭 |
| L5 | `checkList` 依赖租户插件隐式过滤 | 应显式校验 session 中 `className` 存在 |
| L6 | `CaptchaUtils` 未使用的 import | 删除 `BatchDataSourceScriptDatabaseInitializer` |
| L7 | `StudentQueryScoreBo` 有 `pageNum/pageSize` 但 `checkList` 不用 | 移除或实现分页 |

---

## 六、修复优先级路线图

### Phase 1 — 止血（修致命 Bug）

| # | 问题 | 文件 | 预计工作量 |
|---|------|------|-----------|
| C2 | 成绩导入双重读 InputStream | `ScoreServiceImpl.java` | 30 min |
| H8 | 事务不回滚 | `ScoreServiceImpl.java` | 15 min |
| C3 | 日志 PII 泄露 | `ScoreCheckController.java` | 5 min |
| C4 | session 内存泄漏 | `CaptchaController.java` | 10 min |
| M1 | queryScore NPE | `ScoreServiceImpl.java` | 5 min |
| M7 | 删除死代码 ScoreImportExcelVo | 删除文件 | 2 min |
| M10 | 删除 pom.xml 重复依赖 | `pom.xml` | 2 min |
| L6 | 删除未使用 import | `CaptchaUtils.java` | 2 min |

**Phase 1 小计**: 约 1.5 小时

---

### Phase 2 — 加固（安全 + 性能）

| # | 问题 | 文件 | 预计工作量 |
|---|------|------|-----------|
| C1 | Token 泄露 | `ScoreServiceImpl.java` | 15 min |
| M4 | 登录返回格式 | `ScoreServiceImpl.java` | 10 min |
| H1 | 权限分级 | `SaTokenConfig.java` + `ScoreServiceImpl.java` | 1 h |
| H2 | deleteUser 改 DELETE | `ScoreCheckController.java` | 10 min |
| H3 | 文件上传限制 | `application.yml` + `ScoreServiceImpl.java` | 20 min |
| M5 | Bean Validation | `AdminBo.java` + `StudentQueryScoreBo.java` + Controller | 30 min |
| M6 | 密码不打印 | `Admin.java` | 5 min |
| H5 | N+1 查询优化 | `ScoreServiceImpl.java` | 30 min |
| H6 | checkList 分页 | `ScoreCheckController.java` + `ScoreServiceImpl.java` | 1 h |
| H7 | 列索引改为列名匹配 | `ScoreServiceImpl.java` | 30 min |
| M8 | 身份证号格式校验 | `StudentQueryScoreBo.java` | 10 min |
| M9 | multipart 配置 | `application.yml` | 5 min |

**Phase 2 小计**: 约 4 小时

---

### Phase 3 — 完善

| # | 问题 | 建议 |
|---|------|------|
| C5 | SQL 注入面 | 改用 `BaseMapper.selectList()` |
| H4 | 学生导入竞态 | 数据库唯一约束 + upsert |
| M2 | 分组键碰撞 | 用 Record 做 Map Key |
| M3 | 参数校验顺序 | 调整 login 方法逻辑顺序 |
| L1 | 全局异常处理 | 统一错误码 + 通用消息 |
| L2 | Actuator 健康检查 | 加 `spring-boot-starter-actuator` |
| L3 | 接口限流 | Sa-Token 限流或 Redis 计数器 |
| L5 | 显式 className 校验 | 登录时校验 session 中 className |
| — | 单元测试 | 覆盖 Service 层核心逻辑 |
| — | 数据库索引 | `score(student_name, id_card)`、`student(id_card, class_name) UNIQUE` |

**Phase 3 小计**: 约 4-6 小时

---

## 七、附录：修改文件清单

| 文件 | 需要修改 | 需要删除 |
|------|---------|---------|
| `ScoreServiceImpl.java` | C1, C2, H5, H6, H7, H8, M1, M3, M4 | — |
| `ScoreCheckController.java` | C3, H2 | — |
| `CaptchaController.java` | C4 | — |
| `SaTokenConfig.java` | H1 | — |
| `AdminBo.java` | M5 | — |
| `StudentQueryScoreBo.java` | M5, M8 | — |
| `Admin.java` | M6 | — |
| `ScoreMapper.java` | C5 | — |
| `StudentImportListener.java` | H4 | — |
| `application.yml` | H3, M9 | — |
| `pom.xml` | M10 | — |
| `ScoreImportExcelVo.java` | — | M7 |
| `CaptchaUtils.java` | L6 | — |

---

## 八、审计结论

本项目功能完整度较高，核心业务流程（登录 → 查询 → 导入 → 用户管理）均已实现。但在以下方面存在明显不足：

1. **存在 2 个必定触发的 Bug**（C2 导入失败、M1 空指针），上线即现
2. **存在 3 个安全漏洞**（C1 Token 泄露、H1 无权限分级、H2 CSRF），可直接被利用
3. **存在 1 个数据一致性问题**（H8 事务不回滚），会导致脏数据
4. **性能瓶颈明显**（H5 N+1 查询、H6 无分页），数据量增长后必然变慢

**建议**: 严格按照 Phase 1 → Phase 2 → Phase 3 的顺序修复，每个阶段修复完成后进行回归测试，确认无回归后再进入下一阶段。
