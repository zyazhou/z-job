# JZero Framework V1.0 — 开发指南

> 版本：1.0  
> 日期：2026-03-21  
> 目的：指导开发团队完成 JZero 框架 V1.0 版本开发  

---

## 文档目录

1. [开发准备](#一开发准备)
2. [模块开发顺序](#二模块开发顺序)
3. [核心模块开发指导](#三核心模块开发指导)
4. [功能模块开发指导](#四功能模块开发指导)
5. [基础设施开发指导](#五基础设施开发指导)
6. [开发规范与约束](#六开发规范与约束)
7. [测试策略](#七测试策略)
8. [常见问题与解决方案](#八常见问题与解决方案)
9. [里程碑与验收标准](#九里程碑与验收标准)

---

## 一、开发准备

### 1.1 开发环境

```
┌─────────────────────────────────────────────────────────────────────┐
│                        开发环境要求                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  IDE: IntelliJ IDEA 2023+ / Eclipse 2023+                          │
│  JDK: JDK 17+ (推荐 JDK 21)                                        │
│  Maven: 3.8+                                                        │
│  MySQL: 8.0+ (开发用 5.7+ 也可)                                    │
│  Redis: 7.0+ (开发用 5.0+ 也可)                                    │
│  Git: 2.30+                                                        │
│                                                                      │
│  可选：                                                             │
│  ├── Docker: 20.10+ (用于容器化测试)                               │
│  ├── Nacos: 2.0+ (可选，用于注册中心测试)                          │
│  └── Jaeger: 1.4+ (可选，用于链路追踪测试)                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 初始化项目

```bash
# 1. 克隆项目
git clone https://github.com/jzero-framework/jzero.git
cd jzero

# 2. 导入 IDE
# IntelliJ IDEA: File -> Open -> 选择 pom.xml -> Open as Project

# 3. 确认 Maven 依赖下载完成
mvn clean install -DskipTests

# 4. 初始化数据库（执行 sql/init.sql）
mysql -u root -p jzero < sql/init.sql

# 5. 启动 Redis
docker run -d -p 6379:6379 redis:7
```

### 1.3 项目结构概览

```
jzero/
├── pom.xml                          # 父 POM
├── sql/                             # 数据库脚本
│   └── init.sql
├── docs/                            # 文档
│   ├── java-framework-v1-product.md
│   ├── java-framework-v1-tech.md
│   └── java-framework-v1-usercase.md
└── jzero-*/                         # 各模块
    ├── jzero-common/                 # 公共层
    ├── jzero-core/                  # 核心抽象
    ├── jzero-scheduler/             # 调度中心
    ├── jzero-executor/              # 执行器
    ├── jzero-gateway/               # API 网关
    ├── jzero-registry/              # 注册中心
    ├── jzero-admin/                 # 控制台
    ├── jzero-cli/                   # CLI 工具
    └── jzero-spring-boot-starter/   # Spring Boot Starter
```

---

## 二、模块开发顺序

### 2.1 推荐开发顺序

```
开发阶段划分：

┌─────────────────────────────────────────────────────────────────────┐
│  Phase 1: 基础设施 (第 1-2 周)                                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. jzero-common                                                   │
│     ├── 定义 RPC 消息体                                             │
│     ├── 定义枚举和常量                                               │
│     └── 工具类（序列化、网络等）                                    │
│                                                                      │
│  2. jzero-core                                                     │
│     ├── 调度核心抽象                                                │
│     ├── 注册中心抽象                                                │
│     └── 熔断器、负载均衡抽象                                        │
│                                                                      │
│  目标：建立公共基础，其他模块依赖                                    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  Phase 2: 调度核心 (第 3-6 周)                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  3. jzero-scheduler                                                │
│     ├── 主从选举模块 (第 3 周)                                      │
│     │   └── Redis 选举实现                                          │
│     ├── WAL 日志模块 (第 3 周)                                      │
│     ├── 时间轮调度 (第 4 周)                                        │
│     │   └── CronTrigger, FixedRateTrigger                         │
│     ├── 路由策略 (第 4 周)                                          │
│     │   └── 8 种路由实现                                            │
│     ├── Netty RPC 服务端 (第 5 周)                                  │
│     └── 执行日志 (第 6 周)                                          │
│                                                                      │
│  目标：完成调度中心核心功能                                          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  Phase 3: 执行器 SDK (第 7-8 周)                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  4. jzero-executor                                                 │
│     ├── 注解定义 @JZeroJob (第 7 周)                                │
│     ├── JobHandler 基类                                             │
│     ├── Netty RPC 客户端                                            │
│     ├── Disruptor 队列                                              │
│     └── 注解扫描与注册                                               │
│                                                                      │
│  目标：完成执行器 SDK 开发                                           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  Phase 4: 微服务治理 (第 9-11 周)                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  5. jzero-gateway                                                   │
│     ├── 动态路由 (第 9 周)                                          │
│     ├── 负载均衡                                                    │
│     ├── 过滤器链 (第 10 周)                                         │
│     │   ├── 日志、鉴权、限流、熔断                                  │
│     └── HTTP 代理                                                   │
│                                                                      │
│  6. jzero-registry                                                 │
│     ├── 内置注册中心实现 (第 11 周)                                  │
│     └── Nacos 适配                                                  │
│                                                                      │
│  目标：完成网关和注册中心                                            │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  Phase 5: 辅助模块 (第 12-13 周)                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  7. jzero-spring-boot-starter                                       │
│     └── 自动装配配置                                                │
│                                                                      │
│  8. jzero-admin                                                    │
│     └── 基础管理接口                                                │
│                                                                      │
│  9. jzero-cli                                                      │
│     └── CLI 代码生成工具                                            │
│                                                                      │
│  目标：完成辅助模块开发                                              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  Phase 6: 集成测试与优化 (第 14-16 周)                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  • 集成测试                                                         │
│  • 性能压测                                                         │
│  • Bug 修复                                                         │
│  • 文档完善                                                         │
│  • Release 准备                                                     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘

总周期: 约 16 周 (4 个月)
```

### 2.2 模块依赖关系

```
依赖顺序（必须按顺序开发）：

┌─────────────────────────────────────────────────────────────────────┐
│                                                                      │
│   Level 0: 无依赖模块                                               │
│   ───────────────────                                               │
│   jzero-common                                                      │
│       │                                                            │
│       ▼                                                            │
│   Level 1: 依赖 common                                            │
│   ───────────────────                                               │
│   jzero-core                                                        │
│       │                                                            │
│       ▼                                                            │
│   Level 2: 依赖 core                                              │
│   ───────────────────                                               │
│   jzero-scheduler  ◄────────────┐                                 │
│   jzero-executor   ◄────────────┤                                 │
│   jzero-gateway    ◄────────────┤                                 │
│   jzero-registry  ◄────────────┤  (并行开发)                      │
│                              ───┤                                 │
│   Level 3: 依赖 上述模块                  │                         │
│   ──────────────────────────────────                             │
│   jzero-spring-boot-starter  ◄────────────┐                        │
│   jzero-admin              ◄──────────────┤                        │
│   jzero-cli                ◄──────────────┘                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 三、核心模块开发指导

### 3.1 jzero-common 模块

#### 开发目标
建立 RPC 消息格式和公共工具，是所有其他模块的基础。

#### 核心内容

```java
// 1. RPC 消息定义
// request/RunRequest.java
@Data
public class RunRequest implements Serializable {
    private long jobId;
    private long logId;
    private String handler;
    private String param;
    private int shardIndex;
    private int shardTotal;
    private String traceId;
    private String accessToken;
}

// request/BeatRequest.java
@Data
public class BeatRequest implements Serializable {
    private String executorAddress;
    private String appName;
    private String uuid;  // 执行器唯一标识
    private Map<String, String> registryData;  // 注册数据
}

// 2. 枚举定义
// RouteStrategyEnum.java
public enum RouteStrategyEnum {
    ROUND,           // 轮询
    RANDOM,          // 随机
    CONSISTENT_HASH, // 一致性哈希
    LEAST_LFU,       // 最不经常使用
    FAILOVER,        // 故障转移
    SHARDING,        // 分片广播
    BUSY_TRANSFER,   // 忙碌转移
    WEIGHTED         // 权重轮询
}

// 3. 工具类
// KryoUtil.java - 序列化工具
// NetUtil.java - 网络工具
// StringUtil.java - 字符串工具
```

#### 开发要点
- 所有可序列化对象实现 Serializable
- 枚举类要完整定义所有值
- 工具类要线程安全

---

### 3.2 主从选举模块

#### 核心逻辑

```java
// 选举流程：
// 1. 所有节点启动时注册为候选者
// 2. 定期检测 Master 心跳
// 3. Master 失联后，随机延迟 0-500ms
// 4. Redis CAS 竞争，成为新 Master

public class RedisElectionManager implements ElectionManager {
    
    private static final String LEADER_KEY = "jzero:cluster:leader";
    private static final long ELECTION_TIMEOUT_MS = 3000;
    
    // 触发选举
    @Override
    public void triggerElection() {
        // 1. 检查是否已是 Master
        if (isMaster()) return;
        
        // 2. 随机延迟
        Thread.sleep(random(0, 500));
        
        // 3. 检查当前 Master 是否存活
        if (isLeaderAlive()) return;
        
        // 4. CAS 竞争
        Boolean success = redis.setIfAbsent(LEADER_KEY, nodeId, 5, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(success)) {
            becomeMaster();
        }
    }
}
```

#### 关键实现点

| 要点 | 实现方式 |
|------|----------|
| 心跳检测 | Redis Key + TTL，30s 过期 |
| 选举触发 | 定期检测 + 随机延迟 |
| 角色变更通知 | 监听器模式 |
| 故障切换 | < 3s 完成 |

#### 自测要点
- [ ] 单节点启动能成为 Master
- [ ] 多节点竞争，只有一个成为 Master
- [ ] Master 宕机后，Slave 能自动选举
- [ ] 切换时间 < 3s

---

### 3.3 时间轮调度模块

#### 核心逻辑

```java
public class JZeroScheduler {
    
    private final HashedWheelTimer wheelTimer;
    private final ConcurrentHashMap<Long, JobInfo> jobCache;
    
    // 注册 CRON 任务
    public void registerCronJob(JobInfo job) {
        jobCache.put(job.getId(), job);
        scheduleCronJob(job);
    }
    
    // 计算下次触发时间
    private long calculateNextTriggerTime(String cronExpr) {
        try {
            CronExpression cron = new CronExpression(cronExpr);
            return cron.getNextValidTimeAfter(new Date()).getTime();
        } catch (ParseException e) {
            throw new RuntimeException("CRON 表达式解析失败: " + cronExpr);
        }
    }
    
    // 时间轮调度
    private void scheduleCronJob(JobInfo job) {
        long nextTrigger = calculateNextTriggerTime(job.getScheduleConf());
        long delay = nextTrigger - System.currentTimeMillis();
        
        wheelTimer.newTimeout(timeout -> {
            // 触发任务
            trigger(job);
            // 注册下一次
            scheduleCronJob(job);
        }, delay, TimeUnit.MILLISECONDS);
    }
}
```

#### 配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| tickDuration | 100ms | 时间轮 tick 精度 |
| ticksPerWheel | 360 | 时间轮一圈 tick 数 |
| maxTimeout | 1小时 | 最大延迟任务 |

---

### 3.4 Disruptor 无锁队列

#### 核心逻辑

```java
public class TaskDisruptor {
    
    private static final int RING_BUFFER_SIZE = 1 << 20;  // 100万
    
    private final Disruptor<TaskEvent> disruptor;
    
    public TaskDisruptor(ExecutorService executor) {
        this.disruptor = new Disruptor<>(
            TaskEvent::new,
            RING_BUFFER_SIZE,
            executor,
            ProducerType.MULTI,           // 多生产者
            new BlockingWaitStrategy()     // 阻塞等待
        );
        
        disruptor.handleEventsWith(this::processTask);
        disruptor.start();
    }
    
    // 任务处理
    private void processTask(TaskEvent event, long sequence, boolean endOfBatch) {
        // 1. 获取执行器列表
        List<String> executors = registry.getAliveExecutors(event.getJobGroup());
        
        // 2. 路由选择
        Router router = RouterFactory.get(event.getRouteStrategy());
        String target = router.route(event, executors);
        
        // 3. RPC 发送
        rpcClient.send(target, event.toRunRequest());
    }
}
```

---

### 3.5 路由策略实现

#### 8 种路由策略

```
┌─────────────────────────────────────────────────────────────────────┐
│                        8 种路由策略实现                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. ROUND (轮询)                                                    │
│     实现：AtomicInteger.incrementAndGet() % size                    │
│     场景：均匀分发，通用场景                                         │
│                                                                      │
│  2. RANDOM (随机)                                                   │
│     实现：ThreadLocalRandom.current().nextInt(size)                 │
│     场景：简单负载均衡                                               │
│                                                                      │
│  3. CONSISTENT_HASH (一致性哈希)                                    │
│     实现：TreeMap<Long, String> 哈希环                              │
│     场景：有状态任务，相同参数路由到同一节点                         │
│                                                                      │
│  4. LEAST_LFU (最不经常使用)                                        │
│     实现：ConcurrentHashMap<String, AtomicLong> 统计               │
│     场景：避免热点节点                                               │
│                                                                      │
│  5. FAILOVER (故障转移)                                             │
│     实现：遍历列表，选择第一个存活的                                 │
│     场景：主节点故障自动切换                                         │
│                                                                      │
│  6. SHARDING (分片广播)                                             │
│     实现：广播到所有执行器，每个执行器根据 shardIndex 处理           │
│     场景：大数据量并行处理                                           │
│                                                                      │
│  7. BUSY_TRANSFER (忙碌转移)                                        │
│     实现：选择负载最低的节点                                         │
│     场景：执行器负载不均                                             │
│                                                                      │
│  8. WEIGHTED (权重轮询)                                             │
│     实现：权重比例分发                                               │
│     场景：节点性能差异大                                             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 四、功能模块开发指导

### 4.1 执行器 SDK

#### 核心注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JZeroJob {
    
    String value();  // handler 名称
    
    int initStrategy() default 0;    // 初始化策略
    
    int destroyStrategy() default 0;  // 销毁策略
}
```

#### 基类实现

```java
public abstract class JobHandler {
    
    protected JobContext context;
    
    public final ReturnT<String> execute(JobContext ctx) {
        this.context = ctx;
        try {
            init();
            return execute0(ctx);
        } catch (Exception e) {
            return ReturnT.FAIL.put("msg", e.getMessage());
        } finally {
            destroy();
        }
    }
    
    protected abstract ReturnT<String> execute0(JobContext ctx);
    
    protected void init() {}      // 可选初始化
    protected void destroy() {}   // 可选销毁
}
```

#### 使用示例

```java
@Component
public class MyJobHandler extends JobHandler {
    
    @JZeroJob("myJob")
    @Override
    protected ReturnT<String> execute0(JobContext ctx) {
        // 业务逻辑
        return ReturnT.SUCCESS;
    }
}
```

### 4.2 API 网关

#### 过滤器链实现

```java
public class FilterChain {
    
    private final List<GatewayFilter> filters = new ArrayList<>();
    
    public FilterChain add(GatewayFilter filter) {
        filters.add(filter);
        return this;
    }
    
    public Mono<Void> filter(ServerWebExchange exchange) {
        return filters.stream()
            .reduce(
                Mono::chain,
                (mono, filter) -> mono.then(filter.filter(exchange))
            );
    }
}
```

#### 内置过滤器

| 过滤器 | 功能 | 优先级 |
|--------|------|--------|
| LoggingFilter | 请求日志 | 100 |
| AuthFilter | 认证鉴权 | 200 |
| RateLimitFilter | 限流 | 300 |
| CircuitFilter | 熔断 | 400 |
| ProxyFilter | 代理转发 | 500 |

---

## 五、基础设施开发指导

### 5.1 数据库设计

#### 核心表

```sql
-- 1. 执行器组
CREATE TABLE jz_job_group (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    app_name     VARCHAR(64) NOT NULL COMMENT '执行器应用名',
    title        VARCHAR(64) NOT NULL COMMENT '执行器组名称',
    address_type TINYINT DEFAULT 0 COMMENT '0=自动注册 1=手动',
    address_list TEXT COMMENT '手动地址列表',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. 任务信息
CREATE TABLE jz_job_info (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_group           INT NOT NULL COMMENT '执行器组ID',
    job_desc            VARCHAR(255) NOT NULL COMMENT '任务描述',
    schedule_type       VARCHAR(20) NOT NULL COMMENT '调度类型',
    schedule_conf       VARCHAR(128) COMMENT '调度配置',
    executor_route_strategy VARCHAR(50) DEFAULT 'ROUND' COMMENT '路由策略',
    executor_handler    VARCHAR(255) NOT NULL COMMENT '执行器方法',
    executor_param      VARCHAR(1024) DEFAULT '' COMMENT '执行参数',
    executor_timeout    INT DEFAULT 0 COMMENT '超时时间(秒)',
    executor_retry      INT DEFAULT 0 COMMENT '重试次数',
    block_strategy      VARCHAR(20) DEFAULT 'SERIAL' COMMENT '阻塞策略',
    trigger_status      TINYINT DEFAULT 0 COMMENT '触发状态 0=停止 1=运行',
    trigger_next_time   BIGINT DEFAULT 0 COMMENT '下次触发时间',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_trigger_next (trigger_next_time, trigger_status)
);

-- 3. 执行日志
CREATE TABLE jz_job_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id          BIGINT NOT NULL COMMENT '任务ID',
    executor_address VARCHAR(255) COMMENT '执行器地址',
    executor_handler VARCHAR(255) NOT NULL COMMENT '执行器方法',
    trigger_time    DATETIME(3) COMMENT '触发时间',
    trigger_code    INT DEFAULT 0 COMMENT '触发结果代码',
    handle_time     DATETIME(3) COMMENT '处理时间',
    handle_code     INT DEFAULT 0 COMMENT '处理结果代码',
    handle_msg      TEXT COMMENT '处理消息',
    INDEX idx_job_id (job_id),
    INDEX idx_trigger_time (trigger_time)
);
```

### 5.2 配置中心

#### 配置属性类

```java
@Data
@ConfigurationProperties(prefix = "jzero")
public class JZeroProperties {
    
    private boolean enabled = true;
    
    private NodeProperties node = new NodeProperties();
    
    private SchedulerProperties scheduler = new SchedulerProperties();
    
    private ExecutorProperties executor = new ExecutorProperties();
    
    private GatewayProperties gateway = new GatewayProperties();
    
    @Data
    public static class NodeProperties {
        private String id;
        private ClusterProperties cluster = new ClusterProperties();
    }
    
    @Data
    public static class ClusterProperties {
        private String mode = "standalone";  // standalone / cluster
        private String election = "redis";   // redis / raft
        private List<NodeAddress> nodes;
    }
}
```

---

## 六、开发规范与约束

### 6.1 命名规范

```
包命名：
├── com.jzero.common        # 公共层
├── com.jzero.core          # 核心抽象
├── com.jzero.scheduler     # 调度中心
├── com.jzero.executor      # 执行器
├── com.jzero.gateway       # 网关
└── com.jzero.registry     # 注册中心

类命名：
├── 接口: XxxInterface / XxxService
├── 实现: XxxImpl
├── 抽象: AbstractXxx
├── 枚举: XxxEnum
└── 异常: XxxException
```

### 6.2 日志规范

```java
// 使用 Lombok @Slf4j
@Slf4j

// 日志级别：
// ERROR: 需要关注的异常
// WARN:  需要注意但不影响功能
// INFO:  重要业务日志
// DEBUG: 开发调试用

// 示例
@Slf4j
public class JobTrigger {
    
    public void trigger(JobInfo job) {
        log.info("触发任务 jobId={}, handler={}", job.getId(), job.getExecutorHandler());
        
        try {
            // 业务逻辑
        } catch (Exception e) {
            log.error("任务触发失败 jobId={}", job.getId(), e);
            throw e;
        }
    }
}
```

### 6.3 异常处理规范

```java
// 1. 业务异常使用自定义异常
public class JZeroException extends RuntimeException {
    private final int code;
    
    public JZeroException(String message) {
        super(message);
        this.code = 500;
    }
    
    public JZeroException(int code, String message) {
        super(message);
        this.code = code;
    }
}

// 2. 不要捕获 Throwable
try {
    // 业务逻辑
} catch (Exception e) {  // 只捕获 Exception
    // 处理
}

// 3. 异常必须记录日志
catch (Exception e) {
    log.error("xxx 失败", e);
    throw new JZeroException("xxx 失败: " + e.getMessage());
}
```

---

## 七、测试策略

### 7.1 单元测试

```java
// 使用 JUnit 5 + Mockito
@ExtendWith(MockitoExtension.class)
public class RoundRobinRouterTest {
    
    @Mock
    private RegistryService registryService;
    
    private RoundRobinRouter router;
    
    @BeforeEach
    void setUp() {
        router = new RoundRobinRouter();
    }
    
    @Test
    void shouldSelectNextExecutor() {
        // given
        List<String> executors = Arrays.asList("192.168.1.1", "192.168.1.2", "192.168.1.3");
        TriggerContext ctx = TriggerContext.builder().build();
        
        // when & then
        String result1 = router.route(ctx, executors);
        String result2 = router.route(ctx, executors);
        String result3 = router.route(ctx, executors);
        
        // 轮询验证
        Assertions.assertEquals("192.168.1.1", result1);
        Assertions.assertEquals("192.168.1.2", result2);
        Assertions.assertEquals("192.168.1.3", result3);
    }
}
```

### 7.2 集成测试

```java
@SpringBootTest
@AutoConfigureTestDatabase
class SchedulerIntegrationTest {
    
    @Autowired
    private JobInfoService jobInfoService;
    
    @Test
    void shouldCreateJob() {
        JobInfo job = new JobInfo();
        job.setJobGroup(1);
        job.setJobDesc("测试任务");
        job.setScheduleType("CRON");
        job.setScheduleConf("0/5 * * * * ?");
        job.setExecutorHandler("testHandler");
        
        Long id = jobInfoService.create(job);
        
        Assertions.assertNotNull(id);
    }
}
```

### 7.3 性能测试

```java
// 使用 JMH
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class RouterBenchmark {
    
    private RoundRobinRouter router;
    private List<String> executors;
    
    @Setup
    public void setup() {
        router = new RoundRobinRouter();
        executors = IntStream.range(0, 10)
            .mapToObj(i -> "192.168.1." + i)
            .collect(Collectors.toList());
    }
    
    @Benchmark
    public String route() {
        return router.route(TriggerContext.builder().build(), executors);
    }
}
```

---

## 八、常见问题与解决方案

### 8.1 选举问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 多节点同时成为 Master | 随机延迟不够 | 增加随机延迟范围 |
| 选举太频繁 | 检测太灵敏 | 调整心跳超时时间 |
| Redis 不可用 | 网络抖动 | 增加重试机制 |

### 8.2 调度问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 任务触发延迟大 | 时间轮 tick 太大 | 减小 tick 到 10ms |
| 任务丢失 | Disruptor 队列满 | 增大队列或限流 |
| Cron 解析失败 | 表达式错误 | 使用 CronExpression 库 |

### 8.3 性能问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| RPC 延迟高 | 序列化慢 | 使用 Kryo 代替 JSON |
| 内存占用高 | 对象重复创建 | 使用对象池 |
| CPU 使用率高 | 锁竞争 | 使用 Disruptor 无锁队列 |

---

## 九、里程碑与验收标准

### 9.1 开发里程碑

```
┌─────────────────────────────────────────────────────────────────────┐
│                       V1.0 开发里程碑                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  M1: 第 2 周结束 - 基础设施完成                                     │
│  ✅ jzero-common 核心 RPC 消息定义完成                              │
│  ✅ jzero-core 抽象接口定义完成                                    │
│  📋 验收：其他模块能正常依赖编译                                    │
│                                                                      │
│  M2: 第 6 周结束 - 调度核心完成                                    │
│  ✅ 主从选举模块                                                    │
│  ✅ 时间轮调度                                                      │
│  ✅ 8 种路由策略                                                   │
│  ✅ Netty RPC 服务端                                               │
│  📋 验收：能完成基本任务调度                                       │
│                                                                      │
│  M3: 第 8 周结束 - 执行器 SDK 完成                                 │
│  ✅ 注解定义和扫描                                                 │
│  ✅ JobHandler 基类                                                │
│  ✅ RPC 客户端                                                     │
│  📋 验收：能接收并执行业务任务                                     │
│                                                                      │
│  M4: 第 11 周结束 - 网关和注册中心完成                             │
│  ✅ API 网关（路由 + 限流 + 熔断）                                 │
│  ✅ 注册中心                                                       │
│  📋 验收：能进行基本的请求转发和服务注册                            │
│                                                                      │
│  M5: 第 13 周结束 - 辅助模块完成                                   │
│  ✅ Spring Boot Starter                                            │
│  ✅ 基础管理接口                                                   │
│  📋 验收：能通过 Starter 快速接入                                  │
│                                                                      │
│  M6: 第 16 周结束 - 测试与 Release                                 │
│  ✅ 集成测试通过                                                   │
│  ✅ 性能达标                                                       │
│  ✅ 文档完善                                                       │
│  📋 验收：能发布可用版本                                           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 9.2 验收标准

```
功能验收清单：

□ 任务调度
  □ CRON 表达式任务能准时触发
  □ 固定频率任务能按间隔触发
  □ 固定延迟任务能按设定延迟触发
  □ 一次性任务只触发一次

□ 路由策略
  □ 8 种路由策略都能正常工作
  □ 分片任务能正确分配

□ 执行器
  □ 能正确注册到调度中心
  □ 能接收并执行任务
  □ 心跳保持正常
  □ 失败能正确重试

□ 集群高可用
  □ 单节点能正常工作
  □ 多节点能自动选举 Master
  □ Master 宕机能在 3s 内切换

□ API 网关
  □ 路由能正常工作
  □ 限流能正确拦截
  □ 熔断能正确触发

□ 注册中心
  □ 服务能正常注册
  □ 能正常发现服务
  □ 健康检查能工作
```

---

## 附录

### A. Git 分支策略

```
main:       主分支，只接受合并
develop:    开发分支
feature/*:  功能分支
bugfix/*:   Bug 修复分支
release/*:  发布分支
```

### B. Commit 规范

```
<type>(<scope>): <subject>

# 类型
feat:     新功能
fix:      Bug 修复
docs:     文档
refactor: 重构
test:     测试
chore:    构建/工具

# 示例
feat(scheduler): 添加时间轮调度支持
fix(election): 修复选举竞争问题
docs(readme): 更新 README
```

---

**文档版本**: 1.0.0  
**维护团队**: JZero 开发团队  
**最后更新**: 2026-03-21
