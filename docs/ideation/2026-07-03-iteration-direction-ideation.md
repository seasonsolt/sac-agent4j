---
date: 2026-07-03
topic: iteration-direction
focus: review the project and find suitable iteration directions
---

# Ideation: sac-agent4j 迭代方向

## Codebase Context

- **项目**：`sac-agent4j`，一个手写的 Java 21 Maven SWE agent（约 2,700 LOC 主代码，16 个测试类），刻意保持 framework-free。核心循环：build context → 模型返回一个 JSON action → 执行 tool → 记录 observation → 重复。
- **内核构件**：typed `Action` records + `ActionCatalog`、`AgentLoop`/`AgentRun`、`AgentState`（TodoList / VirtualFileSystem / ContextOffloadStore）、`ContextManager` prompt 管线（system/task/state/history/protocol 各自 renderer，`Prompt` 已是 5 段 record）、`ToolRegistry` + `PermissionGate` + `ToolPolicy`（危险 shell 命令黑名单）、`ProcessRunner`、`Workspace` 路径边界、JSONL `TrajectoryLogger`、Pi 风格 `SessionRecorder`（id/parentId 树）。
- **CLI**：session list/summary/tree/note/handoff/fork，以及 `--resume-session`/`--resume-entry`。
- **LLM clients**：JsonLineLlmClient（stdin 手动）、ScriptedLlmClient（测试）、OpenAiCompatibleLlmClient（env 配置）。GraalVM native-image profile 已存在。
- **战略文档**（docs/PRODUCT_DIRECTION.md，2026-06-25）：从学习内核演进为 **"Java/Spring Agent Team Infrastructure"**——session history、tool governance、auditability/replay、Spring/MCP 集成、team skills。路线图：Phase 0 内核已完成；Phase 1 session 产品化刚发布；Phase 2 Spring/MCP；Phase 3 治理硬化；Phase 4 replay/eval；Phase 5 商业化控制平面。
- **关键时序事实**：`codex/agentscope-refactor` 分支当前与 main 指向同一 commit（0308fb0）——**重构尚未开始**，"先建安全网再重构"的窗口还在。

### 批判阶段核实的两个真实 Bug

- **写路径沙箱逃逸（安全）**：`Workspace.resolveForWrite`（workspace/Workspace.java:35）只做 `normalize().toAbsolutePath()`，缺 `resolveExisting`（:26）里有的 `toRealPath()`。workspace 内指向外部的 symlink 能通过 `startsWith(root)` 检查，写操作逃出沙箱。
- **ProcessRunner 管道死锁**：`ProcessRunner.run`（tool/ProcessRunner.java:40-48）先 `waitFor` 再读输出，子进程输出超过 ~64KB 填满 OS 管道 → 假超时 + 输出全丢；`destroyForcibly` 不杀 `sh -lc` 子进程树。`mvn test` 这类输出量必然触发。

### 一个战略缺口（有意记录）

41 个候选里没有一个服务 Pillar 5（团队技能/上下文共享）。现阶段全部想法都在加固单人内核——合理，但应作为**有意识的决策**记录：目前不做"团队"那一半。

## Ranked Ideas

### 1. 立即修两个已核实的 Bug
**Description:** 修 `Workspace.resolveForWrite` 的 symlink 写逃逸（补 `toRealPath()` 校验最深已存在祖先，加回归测试）；修 `ProcessRunner` 的 waitFor-before-read 死锁（并发读 stdout/stderr 或 gobbler 线程，超时时用 `process.descendants()` 杀整个进程树）。各自独立成 commit。
**Rationale:** 两者都在源码里被两个独立 critique agent 核实为真。对一个以"governed tool boundaries"为卖点的项目，写逃逸是存在性尴尬；ProcessRunner 死锁会让任何非平凡 `mvn test` 挂掉。其余一切都建立在内核可信之上。
**Downsides:** 几乎没有；纯修复。symlink 修复需注意不破坏合法的 workspace 内软链。
**Confidence:** 95%
**Complexity:** Low
**Status:** Unexplored

### 2. 重构安全网：先于 AgentScope 重构落地
**Description:** (a) Replay-as-JUnit harness——用 `JsonlSessionReader` 加载录制的 session，经 `ScriptedLlmClient` 回放模型输出，对 actions/observations/最终状态断言，让任何录制运行变成确定性回归测试；(b) 一组敌意输入单测（坏 JSON、超大输出、截断 session）；(c) SESSION_FORMAT.md 规范 + `session validate`（entry 已有 `version` 字段，缺的是规范文档与校验命令）。
**Rationale:** `SessionReplay` 和 `ScriptedLlmClient` 都已存在，(a) 基本是胶水代码。分支名宣告的重构尚未开始——安全网还来得及先落地。**这是门槛：安全网不存在，重构不该合并。**
**Downsides:** 会消耗一部分重构前的时间预算；规范文档对单读者格式略有 ceremony 成分（可裁剪到 schemaVersion + 少量校验）。
**Confidence:** 90%
**Complexity:** Medium
**Status:** Unexplored

### 3. Session 无损化 + 日常人体工学（Phase 1 收尾债务）
**Description:** (a) 修 offload 数据丢失——`JsonlSessionRecorder`（:137）只存 `contentChars`，`SessionReplay`（:66）静默跳过 null-content offload，导致 resume/fork/handoff "悄悄失忆"；payload 存 sidecar blob 文件（可 content-addressed by sha256）并在回放时 rehydrate `ContextOffloadStore`。(b) `continue` 动词 + git 风格短 session id 前缀（现在每个命令要完整文件路径，`--max-steps` 默认 8 让"步数耗尽后续跑"成为最常见工作流）。(c) `session show`——list/tree/handoff 都有，唯独不能直接读一次运行的完整转写。
**Rationale:** offload 丢失让刚发布的 Phase 1"无损 resume/handoff"承诺失真——这是最坏的 operator 惊喜（越长越值得 resume 的运行，失忆越严重）。short-id/continue 保护 Phase 1 工作的 ROI。
**Downsides:** blob 侧车文件引入清理/GC 的小问题；`session show` 的"literate teaching"变体要裁掉（那是藏在子命令里的第二个产品）。
**Confidence:** 85%
**Complexity:** Low–Medium
**Status:** Unexplored

### 4. LLM 传输层韧性
**Description:** 给 `OpenAiCompatibleLlmClient` 加有界指数退避重试（429/5xx/IOException 可重试，尊重 Retry-After）+ 错误分类（401、context-too-long 立即失败），硬失败时打印最后 session 文件路径作为 resume 提示。
**Rationale:** 现在没有任何重试——长运行恰恰是限流和网络抖动发生的时候，一次 429 就烧掉整个 run 的进度和 API 花费。与协议重构正交，现在做安全。配合已有的 resume 机制，网络失败从"致命"变"非事件"。
**Downsides:** 重试逻辑需小心不放大重复副作用（但 LLM 调用本身幂等，风险低）；resume-hint 那半依赖方向 3 的 short-id 人体工学。
**Confidence:** 85%
**Complexity:** Low
**Status:** Unexplored

### 5. AgentScope 重构本体：协议现代化
**Description:** 引入 `ActionCodec` 抽象：同一套 typed `Action` records 既能走 JSON-in-text，也能走 OpenAI/Anthropic 原生 tool-calling（从 sealed `Action` 层级生成 function schema，近乎免费）+ message-list 上下文（`Prompt.java` 已是 5 段 record，**已完成一半**；剩余是把 `LlmClient` 从单字符串接口改成 role-tagged 消息列表，顺带获得 cache-stable prefix）。同时把方向 1 中的坏 JSON 恢复作为最小 catch-and-observe 实现（原生 tool-calling 结构性消灭大部分坏 JSON 失败类别）。
**Rationale:** 这就是分支名宣告的工作。原生 tool-calling 比 JSON-in-text 可靠得多，解锁 prompt 缓存和并行调用；因 `Action` 已是 sealed records，schema 生成成本低。让教学内核教真实世界用的协议。
**Downsides:** **需显式决策**：并行 tool call 是否破坏 README"每回合一个动作"的教学契约；维护两套 codec 会翻倍协议表面（建议规划为迁移而非长期分叉）。严格排在方向 1、2 之后。
**Confidence:** 75%
**Complexity:** High
**Status:** Unexplored

### 6. 治理 v2：现在出设计，Phase 3 实现
**Description:** 把 allowlist、dry-run、HITL 审批、审计记录设计成**一个机制**而非四个特性：许可决策结构化记录进 session（含匹配规则/风险等级/裁决）；`ToolPolicy` 从黑名单翻转为 allow/deny/ask 规则管线（shell 默认 deny，`defaultPolicy()` 作兼容预设）；HITL 做成"挂起 = session 里一条 pending-approval entry，审批后走现有 `--resume-entry` 恢复"；`policy check <command>` CLI 作为决策的解释入口。
**Rationale:** 治理是战略护城河（Phase 3），后续所有功能（审批、dry-run、审计、商业控制平面）都挂在决策点上。一次设计避免建两遍治理。
**Downsides:** 两处修正：dry-run 拦截点应在 `Workspace`/`ApplyPatchTool` 层（**不是** VirtualFileSystem——那是 agent 草稿状态，真实写不经过它）；HITL 依赖方向 3 的无损 resume 先落地。整体是 Phase 3 工作，现在只出设计文档。
**Confidence:** 70%
**Complexity:** High
**Status:** Unexplored

## Rejection Summary

| # | Idea | Reason Rejected |
|---|------|-----------------|
| 1 | spawn_subagent 子代理 | PRODUCT_DIRECTION 明确警告勿跳到 AI-hub 形态（Phase 5）；"已有 parentId 树"说法不成立（那是单文件内 entry 分支，非 run 层级）；最狠地破坏"tiny one-loop agent"身份 |
| 2 | Maven Central 发布 + 完整 Spring starter 模块拆分 | 重构前发布会冻结即将改变的 API——最好听的想法遇上最坏时机；零外部消费者。仅保留 `Agent.builder()` facade，作为重构交付物 |
| 3 | 运行路径自动上下文压缩 | 会重写重构即将重写的 ContextManager/run 路径，必然返工；default max-steps=8 下上下文很少爆掉，无疼痛证据 |
| 4 | Eval 评测框架（fixtures + scoring + baseline diff） | 比路线图早两个阶段；单维护者项目无 prompt 变更速度支撑；应合并进方向 2 的 replay 基础之后 |
| 5 | MCP client 适配器 | 战略契合高（Phase 2 明列）但 stdio JSON-RPC 生命周期 + schema 映射工程量大，零用户拉动，且挂在重构可能改动的 ToolRegistry 上；重构后作为 spike 重启 |
| 6 | doctor + 分层配置文件 | 服务尚不存在的用户群；单维护者已知自己的 env vars；layered config 撑破"tiny handwritten"身份。诊断可日后并入 Spring starter 的 Actuator |
| 7 | 版本化 prompt 资源包（i18n/A-B） | 对一个消费者、一个维护者是过早产品化；唯一承重的一丝——把 prompt 版本戳进 session——并入方向 2 的格式规范即可（git 已版本化 prompt） |
| 8 | 运行级成本/时间熔断器 | 单用户尚不需要经济熔断；保留 per-turn usage 计数（廉价、有审计价值），砍掉熔断器 |
| 9 | Crash-safe sessions 的 write-ahead intent-entry 那半 | 与方向 6 的 HITL 机制重复；torn-line 容错本身有价值但需先核实 `JsonlSessionReader` 是否已容忍，仅保留容错 + interrupted 标记 |

## Session Log
- 2026-07-03: 初次 ideation — 5 个视角并行生成 41 个原始想法（operator 痛点 / 缺失能力 / 杠杆复利 / 假设挑战 / 边界可靠性），合并去重为 25 个独立候选 + 5 个跨视角组合；2 个 critique agent（YAGNI-成本 / 战略-时序）在源码核实主张并攻击清单；orchestrator 统一评分，6 个方向幸存。核实出 2 个真实 bug（Workspace symlink 写逃逸、ProcessRunner 管道死锁）。关键时序发现：agentscope-refactor 分支仍在 main tip，安全网可先行。
