# JZero Framework V1.0 — 技术设计文档

> 版本：1.0  
> 日期：2026-03-21  
> 作者：研发团队  
> 状态：开发指导文档

---

## 文档目录

1. [技术选型与架构理念](#一技术选型与架构理念)
2. [整体架构设计](#二整体架构设计)
3. [模块工程结构](#三模块工程结构)
4. [核心模块详细设计](#四核心模块详细设计)
5. [任务调度核心](#五任务调度核心)
6. [微服务治理模块](#六微服务治理模块)
7. [执行器-sdk](#七执行器-sdk)
8. [集群高可用设计](#八集群高可用设计)
9. [数据库设计](#九数据库设计)
10. [配置与接入](#十配置与接入)
11. [性能优化策略](#十一性能优化策略)
12. [开发计划与里程碑](#十二开发计划与里程碑)

---

## 一、技术选型与架构理念

### 1.1 技术选型总览

| 层次 | 技术选型 | 版本 | 说明 |
|------|----------|------|------|
| 基础框架 | Spring Boot | 3.x | 基础容器，自动装配，生态丰富 |
| 网络通信 | Netty | 4.1.x | 高性能异步 RPC，无阻塞 I/O |
| 时间轮调度 | HashedWheelTimer | Netty 内置 | 亚毫秒级调度精度，无锁设计 |
| 高性能队列 | Disruptor | 3.5.x | 无锁队列，百万级 TPS，LMAX 开源 |
| 数据存储 | MySQL | 8.x | 任务元数据、调度日志 |
| 缓存/选举 | Redis | 7.x | 分布式锁、限流计数、注册发现 |
| 注册中心 | 内置 + Nacos 适配 | - | 开发友好，生产可替换 Nacos |
| ORM | MyBatis-Plus | 3.5.x | CRUD + 动态 SQL，少配置 |
| 序列化 | Kryo + Jackson | 5.5.x | Kryo 用于 RPC 高速序列化，比 JSON 快 5-10 倍 |
| 构建工具 | Maven | 3.8+ | 多模块项目结构 |
| CLI | Picocli | 4.x | 代码生成命令行工具 |
| 链路追踪 | OpenTelemetry | 1.x | 标准可观测方案，对接 Jaeger/Zipkin |
| 测试 | JUnit 5 + JMH | 5.x | 单元测试 + 微基准压测 |

### 1.2 架构设计理念

```
┌─────────────────────────────────────────────────────────────────┐
│                      JZero 架构理念                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 性能优先                                                   │
│     ├── 时间轮调度 → 亚毫秒级精度，<5ms 延迟                    │
│     ├── Disruptor 无锁队列 → 百万级 TPS                        │
│     ├── Netty NIO → 万级并发连接                               │
│     └── Kryo 序列化 → 减少网络开销 5-10 倍                     │
│                                                                 │
│  2. 轻量设计                                                   │
│     ├── 核心依赖 < 10 个                                        │
│     ├── 启动时间 < 10 秒                                       │
│     └── 内存占用 < 200MB（空载）                                │
│                                                                 │
│  3. 模块化架构                                                 │
│     ├── 调度、执行、网关可独立使用                               │
│     ├── SPI 机制扩展                                           │
│     └── 按需开启功能                                           │
│                                                                 │
│  4. 生产级特性                                                 │
│     ├── 集群高可用（<3s 故障切换）                              │
│     ├── 多语言执行器支持（gRPC）                                │
│     └── 可观测性（OpenTelemetry）                              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 1.3 性能指标承诺

| 指标 | 目标值 | 对比 XXL-JOB |
|------|--------|-------------|
| 调度吞吐量 | > 100,000 任务/秒 | 10 倍 |
| 调度延迟 P99 | < 5ms | 20 倍 |
| RPC 延迟 P99 | < 3ms | - |
| 网关吞吐 | > 30,000 QPS | - |
| 启动时间 | < 10 秒 | 3 倍 |
| 内存占用 | < 200MB | 减少 40% |

---

## 二、整体架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                              JZero                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌───────────────────────┐     ┌───────────────────────┐            │
│  │      JZero Admin      │     │       JZero CLI       │            │
│  │     (Web 控制台)      │     │    (代码生成工具)     │            │
│  │  任务|服务|日志|监控   │     │  一键生成项目骨架      │            │
│  └───────────┬───────────┘     └───────────┬───────────┘            │
│              │                             │                         │
│              └─────────────┬───────────────┘                         │
│                            │                                         │
│              ┌─────────────▼─────────────┐                           │
│  ┌──────────┤        JZero Core         ├──────────┐               │
│  │          └─────────────┬─────────────┘          │               │
│  │                        │                         │               │
│  │  ┌─────────────────────┼─────────────────────┐ │               │
│  │  │                     │                       │ │               │
│  │  ▼                     ▼                       ▼ │               │
│  │ ┌──────────┐    ┌──────────┐    ┌──────────┐   │               │
│  │ │Scheduler │    │ Executor │    │  Gateway  │   │               │
│  │ │(调度中心) │    │ (执行器)  │    │   (网关)  │   │               │
│  │ │          │    │          │    │           │   │               │
│  │ │时间轮调度 │    │ 任务执行 │    │  路由转发  │   │               │
│  │ │路由策略   │    │  RPC客户端│    │  限流熔断  │   │               │
│  │ │选举管理   │    │ 心跳管理 │    │  鉴权限流  │   │               │
│  │ └────┬─────┘    └────┬─────┘    └────┬─────┘   │               │
│  │      │               │               │          │               │
│  │ ┌────┴───────────────┴───────────────┴────┐   │               │
│  │ │              核心能力层                    │   │               │
│  │ │  时间轮 │ Disruptor │ Netty RPC │ 熔断限流  │   │               │
│  │ │  选举 │ WAL │ 服务注册 │ 负载均衡          │   │               │
│  │ └──────────────────────────────────────────┘   │               │
│  │                        │                         │               │
│  └────────────────────────┼────────────────────────┘               │
│                           │                                          │
│  ┌────────────────────────┼───────────────────────────────────────┐ │
│  │                   基础设施层                                     │ │
│  │  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐              │ │
│  │  │ MySQL  │  │ Redis  │  │ Nacos  │  │  MQ    │              │ │
│  │  │ 元数据 │  │ 选举/限流│  │ 注册发现│  │ 可选   │              │ │
│  │  └────────┘  └────────┘  └────────┘  └────────┘              │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### 2.2 核心组件通信

```
                    ┌──────────────┐
                    │  Scheduler   │
                    │   Master     │
                    └──────┬───────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ▼               ▼               ▼
     ┌──────────┐   ┌──────────┐   ┌──────────┐
     │ Executor │   │ Executor │   │ Executor │
     │   A      │   │   B      │   │   C      │
     │ (Java)   │   │  (Go)    │   │ (Python) │
     └──────────┘   └──────────┘   └──────────┘

通信协议：
├── Scheduler → Executor: gRPC / Netty（任务下发）
├── Executor → Scheduler: gRPC / Netty（心跳 + 结果上报）
└── 内部: Netty + Kryo（高性能）/ gRPC（多语言）
```

### 2.3 节点角色定义

| 角色 | 职责 | 数量建议 | 可运行模式 |
|------|------|----------|------------|
| **Master（Leader）** | 任务调度触发、路由决策、WAL 日志写入、集群协调 | 1 | 仅在集群模式 |
| **Slave（Follower）** | 实时同步 WAL 状态、热备、故障时自动竞选 | ≥ 2 | 仅在集群模式 |
| **Scheduler** | 调度中心进程，可同时运行 Master/Slave 角色 | 3-N | 集群模式 |
| **Executor** | 执行业务任务，向调度中心注册 | 无上限 | 独立进程 |

---

## 三、模块工程结构

### 3.1 目录结构

```
jzero/
├── pom.xml                                    # 父 POM，统一依赖版本管理
│
├── jzero-common/                              # 公共层，所有模块依赖
│   ├── pom.xml
│   └── src/main/java/com/jzero/common/
│       ├── annotation/                        # 注解定义
│       │   ├── JZeroJob.java                 # 任务注解
│       │   └── JZeroMapper.java              # Mapper 扫描注解
│       ├── model/                            # 数据模型
│       │   ├── dto/                          # 数据传输对象
│       │   ├── vo/                           # 视图对象
│       │   ├── enums/                        # 枚举类
│       │   └── constant/                     # 常量定义
│       ├── rpc/                               # RPC 消息体
│       │   ├── request/                       # 请求消息
│       │   │   ├── RunRequest.java           # 任务执行请求
│       │   │   ├── BeatRequest.java          # 心跳请求
│       │   │   ├── LogReportRequest.java     # 日志上报请求
│       │   │   ├── RegistryRequest.java      # 注册请求
│       │   │   └── DeregistryRequest.java    # 下线请求
│       │   └── response/                      # 响应消息
│       │       ├── RunResponse.java          # 执行结果响应
│       │       └── BeatResponse.java         # 心跳响应
│       ├── exception/                         # 异常定义
│       └── util/                              # 工具类
│           ├── KryoUtil.java                  # Kryo 序列化
│           └── NetUtil.java                   # 网络工具
│
├── jzero-core/                                # 核心抽象层
│   ├── pom.xml
│   └── src/main/java/com/jzero/core/
│       ├── scheduler/                         # 调度核心抽象
│       │   ├── JobTrigger.java               # 任务触发器接口
│       │   ├── TriggerContext.java           # 触发上下文
│       │   ├── RouteStrategy.java            # 路由策略接口
│       │   └── enums/
│       │       ├── RouteStrategyEnum.java    # 路由策略枚举
│       │       ├── ScheduleTypeEnum.java     # 调度类型枚举
│       │       └── BlockStrategyEnum.java    # 阻塞策略枚举
│       ├── registry/                         # 注册中心抽象
│       │   ├── ServiceRegistry.java          # 注册中心接口
│       │   └── ServiceInstance.java          # 服务实例
│       ├── circuit/                           # 熔断器
│       │   ├── CircuitBreaker.java           # 熔断器接口
│       │   └── CircuitConfig.java            # 熔断配置
│       └── loadbalancer/                     # 负载均衡抽象
│           └── LoadBalancer.java
│
├── jzero-scheduler/                           # 调度中心模块
│   ├── pom.xml
│   └── src/main/java/com/jzero/scheduler/
│       ├── JZeroSchedulerApplication.java    # 启动类
│       ├── config/
│       │   ├── SchedulerConfig.java          # 调度配置
│       │   └── NettyServerConfig.java        # Netty 服务端配置
│       ├── election/                         # 主从选举
│       │   ├── ElectionManager.java          # 选举管理器
│       │   ├── RedisElection.java            # Redis 实现
│       │   └── NodeRole.java                 # 节点角色
│       ├── wal/                              # WAL 日志
│       │   ├── WalStore.java                 # WAL 存储接口
│       │   └── WalEntry.java                 # WAL 条目
│       ├── trigger/                          # 触发器
│       │   ├── JobTrigger.java               # 触发器实现
│       │   ├── CronTrigger.java              # CRON 触发
│       │   ├── FixedRateTrigger.java         # 固定频率触发
│       │   └── OneTimeTrigger.java           # 一次性触发
│       ├── router/                           # 路由策略实现
│       │   ├── ExecutorRouter.java           # 路由接口
│       │   ├── RoundRobinRouter.java         # 轮询
│       │   ├── RandomRouter.java             # 随机
│       │   ├── ConsistentHashRouter.java     # 一致性哈希
│       │   ├── LeastLFRouter.java            # 最不经常使用
│       │   ├── FailoverRouter.java          # 故障转移
│       │   ├── ShardingRouter.java          # 分片广播
│       │   ├── BusyTransferRouter.java      # 忙碌转移
│       │   └── WeightedRouter.java           # 权重轮询
│       ├── rpc-server/                       # Netty RPC 服务端
│       │   ├── RpcServer.java                # 服务端主类
│       │   ├── RpcServerHandler.java         # 消息处理器
│       │   ├── KryoEncoder.java              # Kryo 编码器
│       │   └── KryoDecoder.java              # Kryo 解码器
│       ├── registry/                         # 内置注册中心
│       │   ├── EmbeddedRegistry.java         # 内嵌实现
│       │   └── EmbeddedRegistryCache.java    # 本地缓存
│       └── log/                              # 执行日志
│           └── JobLogService.java            # 日志服务
│
├── jzero-executor/                           # 执行器 SDK
│   ├── pom.xml
│   └── src/main/java/com/jzero/executor/
│       ├── annotation/
│       │   └── JZeroJob.java                 # 任务注解
│       ├── handler/
│       │   ├── JobHandler.java               # 任务基类
│       │   ├── JobContext.java               # 任务上下文
│       │   └── ReturnT.java                  # 返回结果
│       ├── client/                           # RPC 客户端
│       │   ├── ExecutorClient.java           # 客户端主类
│       │   ├── RpcClientHandler.java         # 消息处理
│       │   └── NettyClientPool.java          # 连接池
│       ├── netty/                            # Netty 客户端配置
│       │   └── ClientChannelInitializer.java
│       ├── disruptor/
│       │   ├── TaskDisruptor.java            # Disruptor 队列
│       │   ├── TaskEvent.java                # 任务事件
│       │   └── TaskConsumer.java             # 任务消费者
│       └── spi/
│           └── JobHandlerRegistry.java       # Handler 注册表
│
├── jzero-gateway/                            # API 网关模块
│   ├── pom.xml
│   └── src/main/java/com/jzero/gateway/
│       ├── JZeroGatewayApplication.java
│       ├── router/
│       │   ├── RouteDefinition.java          # 路由定义
│       │   ├── RouteMatcher.java             # 路由匹配
│       │   └── RouteDefinitionLoader.java    # 路由加载
│       ├── filter/
│       │   ├── GatewayFilter.java            # 过滤器接口
│       │   ├── FilterChain.java              # 过滤器链
│       │   ├── LoggingFilter.java            # 日志过滤器
│       │   ├── AuthFilter.java               # 认证过滤器
│       │   ├── RateLimitFilter.java          # 限流过滤器
│       │   └── CircuitFilter.java            # 熔断过滤器
│       └── loadbalancer/
│           ├── RandomLoadBalancer.java
│           └── RoundRobinLoadBalancer.java
│
├── jzero-registry/                          # 注册中心模块
│   ├── pom.xml
│   └── src/main/java/com/jzero/registry/
│       ├── redis/
│       │   └── RedisRegistry.java            # Redis 实现
│       └── nacos/
│           └── NacosRegistry.java            # Nacos 适配
│
├── jzero-admin/                             # 控制台后端
│   ├── pom.xml
│   └── src/main/java/com/jzero/admin/
│       └── controller/
│           ├── JobController.java            # 任务管理
│           └── JobLogController.java         # 日志查询
│
├── jzero-cli/                               # CLI 代码生成
│   ├── pom.xml
│   └── src/main/java/com/jzero/cli/
│       └── JZeroCli.java                    # 主入口
│
├── jzero-spring-boot-starter/               # Spring Boot 自动装配
│   ├── pom.xml
│   └── src/main/java/com/jzero/starter/
│       ├── JZeroAutoConfiguration.java
│       ├── JZeroProperties.java             # 配置属性
│       └── JZeroJobScanner.java             # 注解扫描
│
└── jzero-test/                            # 测试工具
    └── src/test/java/com/jzero/
        └── benchmark/                       # 基准测试
```

### 3.2 依赖关系

```
                    ┌─────────────────┐
                    │   jzero-admin   │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
    ┌──────────────┐ ┌────────────┐ ┌──────────────┐
    │jzero-scheduler│ │jzero-gateway│ │jzero-executor│
    └──────┬───────┘ └──────┬─────┘ └──────┬───────┘
           │                 │              │
           └────────────┬────┴──────────────┘
                        │
              ┌─────────┴─────────┐
              │     jzero-core     │
              └─────────┬─────────┘
                        │
              ┌─────────┴──────────┐
              │    jzero-common    │
              └────────────────────┘
```

---

## 四、核心模块详细设计

### 4.1 主从选举模块

#### 4.1.1 模块职责

| 职责 | 说明 |
|------|------|
| 选举算法实现 | 基于 Redis CAS 的类 Raft 简化选举 |
| 故障检测 | 实时检测 Master 心跳，3秒超时判定死亡 |
| 自动切换 | Master 宕机后自动触发选举，< 3s 完成 |
| 角色管理 | 管理节点角色状态（Master/Slave/Candidate） |
| 状态通知 | 选举结果变更时通知所有相关组件 |

#### 4.1.2 核心类设计

```java
// 节点角色枚举
public enum NodeRole {
    MASTER,    // 主节点，负责调度
    SLAVE,     // 从节点，热备
    CANDIDATE  // 候选节点，正在竞选
}

// 选举管理器接口
public interface ElectionManager {
    void start();
    void stop();
    NodeRole getCurrentRole();
    String getMasterNodeId();
    boolean isMaster();
    void triggerElection();
    void addListener(ElectionListener listener);
}

// 选举监听器
public interface ElectionListener {
    void onBecomeMaster(String nodeId);
    void onBecomeSlave(String nodeId);
    void onElectionFailed(String nodeId, String reason);
}
```

#### 4.1.3 Redis 选举实现

```java
@Configuration
@ConditionalOnProperty(name = "jzero.cluster.election", havingValue = "redis")
public class RedisElectionManager implements ElectionManager {
    
    // Redis Key 定义
    private static final String LEADER_KEY      = "jzero:cluster:leader";
    private static final String CANDIDATES_KEY   = "jzero:cluster:candidates";
    private static final String HEARTBEAT_PREFIX = "jzero:node:heartbeat:";
    
    // 时间配置
    private static final long ELECTION_TIMEOUT_MS = 3000;  // 3秒无心跳认为 leader 死亡
    private static final long LEADER_TTL_MS = 5000;        // leader 租约 5 秒
    private static final long HEARTBEAT_INTERVAL_MS = 1000; // 心跳间隔 1 秒
    
    private final AtomicReference<NodeRole> currentRole = new AtomicReference<>(NodeRole.SLAVE);
    private final CopyOnWriteArrayList<ElectionListener> listeners = new CopyOnWriteArrayList<>();
    private final String currentNodeId;
    
    @Override
    public void start() {
        // 1. 注册当前节点为候选者
        registerAsCandidate();
        
        // 2. 启动心跳发送线程
        startHeartbeatSender();
        
        // 3. 启动 Master 检测线程
        startMasterMonitor();
        
        // 4. 尝试参与选举
        tryParticipateInElection();
    }
    
    @Override
    public boolean isMaster() {
        String currentLeader = redisTemplate.opsForValue().get(LEADER_KEY);
        if (currentLeader == null) return false;
        return currentLeader.startsWith(currentNodeId + ":");
    }
    
    @Override
    public void triggerElection() {
        if (isMaster()) return;
        
        currentRole.set(NodeRole.CANDIDATE);
        
        try {
            // 随机等待 0-500ms，降低竞争
            Thread.sleep(ThreadLocalRandom.current().nextLong(0, 500));
            
            // 检查当前 leader 是否存活
            String currentLeader = redisTemplate.opsForValue().get(LEADER_KEY);
            if (currentLeader != null && isLeaderAlive(currentLeader)) {
                currentRole.set(NodeRole.SLAVE);
                return;
            }
            
            // CAS 设置 leader
            String newLeaderValue = currentNodeId + ":" + System.currentTimeMillis();
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                LEADER_KEY, newLeaderValue, LEADER_TTL_MS, TimeUnit.MILLISECONDS);
            
            if (Boolean.TRUE.equals(success)) {
                becomeMaster();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void becomeMaster() {
        currentRole.set(NodeRole.MASTER);
        log.info("节点 {} 当选为 Master", currentNodeId);
        
        // 通知所有监听器
        for (ElectionListener listener : listeners) {
            listener.onBecomeMaster(currentNodeId);
        }
    }
    
    private void startMasterMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!isMaster()) {
                String currentLeader = redisTemplate.opsForValue().get(LEADER_KEY);
                if (currentLeader != null && !isLeaderAlive(currentLeader)) {
                    triggerElection();
                }
            }
        }, HEARTBEAT_INTERVAL_MS / 2, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
}
```

---

### 4.2 WAL 日志模块

#### 4.2.1 模块职责

| 职责 | 说明 |
|------|------|
| 状态记录 | 记录所有任务触发和状态变更 |
| 状态同步 | 异步同步到所有 Slave 节点 |
| 故障恢复 | 支持故障恢复时的状态重建 |
| 日志回放 | 提供日志回放能力 |

#### 4.2.2 核心类设计

```java
// WAL 条目
@Data
public class WalEntry implements Serializable {
    private long   term;           // 当前任期号
    private long   index;          // 日志序号
    private String type;           // 条目类型
    private byte[] data;           // 序列化数据
    private long   prevLogIndex;  // 前一条日志索引
    private long   commitIndex;    // 已提交的日志索引
    
    public enum Type {
        TRIGGER,         // 任务触发
        REGISTRY,        // 注册/下线
        CONFIG_CHANGE,   // 配置变更
        LEADERSHIP_CHANGE // 领导权变更
    }
}

// WAL 存储接口
public interface WalStore {
    long append(WalEntry entry);
    WalEntry getEntry(long index);
    List<WalEntry> getEntriesAfter(long startIndex);
    void commit(long index);
    long getCommitIndex();
}
```

---

## 五、任务调度核心

### 5.1 时间轮调度

```java
public class JZeroScheduler {
    
    // 时间轮配置: tick=100ms, ticks=360 (36秒一圈)
    private final HashedWheelTimer wheelTimer = new HashedWheelTimer(
        new NamedThreadFactory("jzero-scheduler"),
        100, TimeUnit.MILLISECONDS, 360);
    
    // 调度触发
    public void schedule(JobTask task, long triggerAtMs) {
        long delay = triggerAtMs - System.currentTimeMillis();
        wheelTimer.newTimeout(timeout -> submitToDisruptor(task), 
            Math.max(0, delay), TimeUnit.MILLISECONDS);
    }
    
    // CRON 任务注册
    public void scheduleCron(JobInfo job, String cronExpr) {
        CronExpression cron = new CronExpression(cronExpr);
        long next = cron.getNextValidTimeAfter(new Date()).getTime();
        schedule(new CronJobTask(job, cron), next);
    }
}
```

### 5.2 Disruptor 无锁队列

```java
public class TaskDisruptor {
    
    // Ring Buffer 大小: 2^20 = 1048576
    private static final int RING_BUFFER_SIZE = 1 << 20;
    
    private final Disruptor<TaskEvent> disruptor;
    
    public TaskDisruptor(ExecutorService workerPool) {
        this.disruptor = new Disruptor<>(
            TaskEvent::new,
            RING_BUFFER_SIZE,
            workerPool,
            ProducerType.MULTI,
            new BlockingWaitStrategy());
        
        this.disruptor.handleEventsWith(this::routeAndDispatch);
        this.disruptor.start();
    }
    
    // 任务分发
    private void routeAndDispatch(TaskEvent event, long sequence, boolean endOfBatch) {
        List<String> executors = registry.getAliveExecutors(event.getJobGroup());
        ExecutorRouter router = RouterFactory.getRouter(event.getRouteStrategy());
        String target = router.route(event, executors);
        rpcClient.sendRunRequest(target, event.toRunRequest());
    }
}
```

### 5.3 路由策略实现

```java
// 8 种路由策略
public enum RouteStrategyEnum {
    ROUND,                    // 轮询
    RANDOM,                  // 随机
    CONSISTENT_HASH,         // 一致性哈希
    LEAST_LFU,              // 最不经常使用
    FAILOVER,               // 故障转移
    SHARDING,               // 分片广播
    BUSY_TRANSFER,          // 忙碌转移
    WEIGHTED                // 权重轮询
}
```

---

## 六、微服务治理模块

### 6.1 API 网关

```java
// 路由定义
@Data
public class RouteDefinition {
    private String id;
    private String path;          // /api/**
    private String serviceId;     // target-service
    private int weight = 100;
    private List<String> filters; // ["Auth", "RateLimit"]
}

// 网关处理器
@Component
public class JZeroGatewayHandler {
    
    public Mono<Void> handle(ServerWebExchange exchange) {
        // 1. 路由匹配
        RouteDefinition route = routeMatcher.match(exchange.getRequest().getPath());
        
        // 2. 负载均衡
        ServiceInstance instance = loadBalancer.select(route.getServiceId());
        
        // 3. 过滤器链执行
        Handler handler = middlewareChain.build(route.getFilters(), 
            ctx -> proxyToBackend(ctx, instance.getUrl()));
        
        return Mono.fromRunnable(() -> handler.handle(exchange));
    }
}
```

### 6.2 熔断器

```java
public class CircuitBreaker {
    
    private final AtomicReference<CircuitState> state = 
        new AtomicReference<>(CircuitState.CLOSED);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final CircuitConfig config;
    
    public enum CircuitState { CLOSED, OPEN, HALF_OPEN }
    
    public <T> T execute(Supplier<T> action, Supplier<T> fallback) {
        if (state.get() == CircuitState.OPEN) {
            if (shouldAttemptReset()) {
                state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN);
            } else {
                return fallback.get();
            }
        }
        
        try {
            T result = action.get();
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFailure();
            return fallback.get();
        }
    }
}
```

### 6.3 限流器

```java
// 令牌桶限流（Redis 原子实现）
public class RedisRateLimiter {
    
    private static final String SCRIPT = """
        local key = KEYS[1]
        local rate = tonumber(ARGV[1])
        local capacity = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])
        local requested = tonumber(ARGV[4])
        
        local data = redis.call('HMGET', key, 'tokens', 'last_time')
        local tokens = tonumber(data[1]) or capacity
        local last_time = tonumber(data[2]) or now
        
        local elapsed = math.max(0, now - last_time)
        local new_tokens = math.min(capacity, tokens + elapsed * rate)
        local allowed = new_tokens >= requested and 1 or 0
        
        redis.call('HMSET', key, 'tokens', new_tokens - allowed, 'last_time', now)
        return {allowed, new_tokens}
        """;
    
    public boolean tryAcquire(int permits) {
        // Lua 脚本保证原子性
        return redis.execute(...);
    }
}
```

---

## 七、执行器 SDK

### 7.1 任务注解

```java
// 任务注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JZeroJob {
    String value();  // handler 名称
}

// 使用示例
@Component
public class OrderSyncHandler extends JobHandler {
    
    @JZeroJob("orderSync")
    @Override
    protected ReturnT<String> execute0(JobContext ctx) throws Exception {
        // 业务逻辑
        return ReturnT.SUCCESS;
    }
}
```

### 7.2 执行器启动流程

```
Spring Boot 启动
    │
    ▼
读取 @JZeroJob 注解的方法，注册 handler 映射
    │
    ▼
启动 Netty RPC Client，连接到调度中心
    │
    ▼
注册自身（appName + 地址 + handler 列表）
    │
    ▼
启动心跳线程（每 30s 发送一次 Beat）
    │
    ▼
启动日志上报线程（批量异步上报）
    │
    ▼
等待接收 RunRequest → Disruptor → 线程池执行 → 返回结果
```

---

## 八、集群高可用设计

### 8.1 故障切换流程

```
Master 宕机
    │
    ▼
所有 Slave 检测到 leader 心跳超时（3s）
    │
    ▼
触发 electionTimeout，最早醒来的节点发起 campaign
    │
    ▼
Redis CAS 竞争获胜 → 成为新 Master
    │
    ▼
广播 LeaderChange 事件（Netty 推送通知所有 Executor）
    │
    ▼
新 Master 接管调度（< 3s 完成）
    │
    ▼
旧 Master 恢复后降为 Slave
```

### 8.2 数据一致性

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| 最终一致性 | WAL 异步同步，性能高 | 大多数场景 |
| 强一致性 | 同步复制 | 金融/支付场景（可选） |

---

## 九、数据库设计

### 9.1 核心表结构

```sql
-- 执行器组
CREATE TABLE jz_job_group (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    app_name     VARCHAR(64) NOT NULL,
    title        VARCHAR(64) NOT NULL,
    address_type TINYINT DEFAULT 0 COMMENT '0=自动注册 1=手动',
    address_list TEXT,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 任务信息
CREATE TABLE jz_job_info (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_group           INT NOT NULL,
    job_desc            VARCHAR(255) NOT NULL,
    schedule_type       VARCHAR(20) NOT NULL COMMENT 'CRON|FIX_RATE|FIX_DELAY|ONCE',
    schedule_conf       VARCHAR(128) NOT NULL,
    executor_route_strategy VARCHAR(50) DEFAULT 'ROUND',
    executor_handler    VARCHAR(255) NOT NULL,
    executor_param      VARCHAR(1024) DEFAULT '',
    executor_timeout    INT DEFAULT 0,
    executor_retry      INT DEFAULT 0,
    block_strategy      VARCHAR(20) DEFAULT 'SERIAL',
    trigger_status      TINYINT DEFAULT 0,
    trigger_next_time   BIGINT DEFAULT 0,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 执行日志
CREATE TABLE jz_job_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_group       INT NOT NULL,
    job_id          INT NOT NULL,
    executor_address VARCHAR(255) DEFAULT '',
    executor_handler VARCHAR(255) NOT NULL,
    trigger_time    DATETIME(3),
    trigger_code    INT DEFAULT 0,
    handle_time     DATETIME(3),
    handle_code     INT DEFAULT 0,
    handle_msg      TEXT,
    alarm_status    TINYINT DEFAULT 0,
    INDEX idx_job_id (job_id),
    INDEX idx_trigger_time (trigger_time)
);

-- 服务注册表
CREATE TABLE jz_service_registry (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_id   VARCHAR(64) NOT NULL,
    instance_id  VARCHAR(128) NOT NULL,
    host         VARCHAR(64) NOT NULL,
    port         INT NOT NULL,
    version      VARCHAR(32) DEFAULT '1.0',
    weight       INT DEFAULT 100,
    status       TINYINT DEFAULT 1,
    heartbeat_at BIGINT NOT NULL,
    UNIQUE KEY uk_instance (instance_id),
    INDEX idx_service (service_id, status)
);
```

---

## 十、配置与接入

### 10.1 Maven 依赖

```xml
<dependency>
    <groupId>com.jzero</groupId>
    <artifactId>jzero-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 10.2 配置示例

```yaml
jzero:
  enabled: true
  
  # 节点配置
  node:
    id: ${HOSTNAME:node-1}
    cluster:
      mode: cluster  # standalone / cluster
      election: redis  # redis / raft
  
  # 执行器配置
  executor:
    app-name: ${spring.application.name}
    admin-addresses: 
      - http://localhost:7777
  
  # 网关配置
  gateway:
    enabled: false
    port: 8888
```

### 10.3 快速接入

```java
@SpringBootApplication
@EnableJZero
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}

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

---

## 十一、性能优化策略

### 11.1 网络优化

| 优化点 | 实现方式 |
|--------|----------|
| 连接复用 | Netty 长连接池 |
| 序列化 | Kryo 二进制序列化 |
| 零拷贝 | Netty FileRegion |
| TCP 参数 | TCP_NODELAY, SO_KEEPALIVE |

### 11.2 内存优化

| 优化点 | 实现方式 |
|--------|----------|
| 对象池 | Disruptor RingBuffer