# JZero Framework V1.0 — 技术设计文档

> 版本：1.0  
> 日期：2026-03-21  
> 作者：研发团队

---

## 一、技术选型

| 层次 | 技术选型 | 说明 |
|------|----------|------|
| 核心框架 | Spring Boot 3.x | 基础容器，自动装配 |
| 网络通信 | Netty 4.x | 高性能异步 RPC，无阻塞 I/O |
| 时间轮调度 | HashedWheelTimer（Netty 内置） | 亚毫秒级调度精度 |
| 高性能队列 | Disruptor（LMAX） | 无锁队列，百万级 TPS |
| 数据存储 | MySQL 8.x + ShardingSphere | 任务元数据 + 分库分表支持 |
| 缓存 / 选举 | Redis 7.x（Redisson） | 分布式锁、限流计数、主从选举 |
| 注册中心 | 内置轻量注册中心 + Nacos 适配 | 开发友好，生产可替换 |
| ORM | MyBatis-Plus | CRUD + 动态 SQL |
| 序列化 | Kryo（RPC）+ Jackson（JSON） | 高速序列化，大小减少 5-10x |
| 构建工具 | Maven（多模块） | Gradle 可选 |
| CLI | Picocli | 代码生成命令行 |
| 测试 | JUnit 5 + Testcontainers + JMH | 微基准压测 |
| 链路追踪 | OpenTelemetry + Jaeger | 标准可观测方案 |

---

## 二、集群架构

### 2.1 整体拓扑

```
                              ┌──────────────────┐
                              │   Admin Console   │
                              │  (Vue3 控制台)     │
                              └────────┬─────────┘
                                       │ REST
                    ┌──────────────────▼──────────────────┐
                    │           JZero Cluster (3-Nodes)      │
                    │                                          │
                    │  ┌─────────────┐   ┌─────────────┐       │
                    │  │ Master Node │◄─►│ Slave Node  │       │
                    │  │  Raft Leader │   │  Follower   │       │
                    │  │  调度触发     │   │  状态同步    │       │
                    │  └──────┬──────┘   └──────┬──────┘       │
                    │         │ Raft/WAL        │              │
                    │  ┌──────▼──────┐   ┌─────────────┐        │
                    │  │ Slave Node  │   │ Slave Node  │        │
                    │  └─────────────┘   └─────────────┘        │
                    └──────────────────────────────────────────┘
                             │ Netty RPC              │ HTTP/gRPC
           ┌─────────────────▼────────┐  ┌─────────────────────▼──────┐
           │    Executor Cluster       │  │    Microservice Cluster   │
           │  [Node-A] [Node-B] [C]   │  │  Gateway | Svc-A | Svc-B  │
           └───────────────────────────┘  └───────────────────────────┘
                             │
           ┌─────────────────▼───────────────────────────┐
           │              基础设施层                        │
           │  MySQL  │  Redis(Sentinel)  │  MQ(Optional)   │
           └──────────────────────────────────────────────┘
```

### 2.2 节点角色定义

| 角色 | 职责 | 数量建议 |
|------|------|----------|
| **Master（Leader）** | 任务调度触发、路由决策、WAL 日志写入 | 1 |
| **Slave（Follower）** | 实时同步 WAL 状态、热备、故障时自动竞选 | ≥ 2 |
| **Executor Node** | 执行业务任务，向调度中心注册，与 Master/ Slave 心跳 | 无上限 |

**节点通信协议**
- Master ↔ Slave：基于 Netty 的 **Raft 简化协议**（心跳 + AppendEntries + Vote）
- Master → Executor：Netty 长连接 RPC（双向心跳保活）
- Executor 内：Disruptor 队列接收任务，线程池执行

---

## 三、主从选举与故障切换

### 3.1 选举算法

采用类 Raft 的 Leader Election，但使用 Redis 实现简化版，降低运维复杂度：

```java
// 选举核心逻辑
public class JZeroElection {

    private static final String LEADER_KEY  = "jzero:cluster:leader";
    private static final String CANDIDATES   = "jzero:cluster:candidates";
    private static final long   ELECTION_TIMEOUT_MS = 3000;
    private static final long   LEADER_TTL_MS       = 5000;

    // 当前节点发起竞选
    public boolean campaign(String nodeId) {
        long now = System.currentTimeMillis();
        
        // 1. 检查当前 leader 是否存活
        String currentLeader = redis.get(LEADER_KEY);
        if (currentLeader != null && isLeaderAlive(currentLeader)) {
            return false;  // 存在活跃 leader，不发起竞选
        }
        
        // 2. 所有候选节点随机等待 0-500ms，降低竞争
        long randomDelay = ThreadLocalRandom.current().nextLong(0, 500);
        Thread.sleep(randomDelay);
        
        // 3. CAS 设置 leader（SET NX EX，谁先成功谁赢）
        Boolean won = redis.setNxEx(
            LEADER_KEY,
            nodeId + ":" + now,
            LEADER_TTL_MS / 1000
        );
        
        if (Boolean.TRUE.equals(won)) {
            becomeLeader(nodeId);
            return true;
        }
        return false;
    }
    
    // Leader 续约（每 2s 执行一次）
    public void extendLease(String nodeId) {
        String holder = redis.get(LEADER_KEY);
        if (nodeId.equals(holder.split(":")[0])) {
            redis.expire(LEADER_KEY, LEADER_TTL_MS / 1000);
        }
    }
    
    // Leader 心跳检测（所有节点每 1s 检测）
    public boolean isLeaderAlive(String leaderId) {
        String lastHeartbeat = redis.get("jzero:node:heartbeat:" + leaderId);
        return lastHeartbeat != null 
            && (System.currentTimeMillis() - Long.parseLong(lastHeartbeat)) < ELECTION_TIMEOUT_MS;
    }
}
```

### 3.2 故障检测与切换

```
Master 宕机
    │
    ▼
所有 Slave 检测到 leader 心跳超时（3s 内）
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

### 3.3 WAL 状态同步

Master 每次任务触发写入 WAL，异步同步到所有 Slave：

```java
// WAL 条目格式
public class WalEntry {
    private long   term;           // 当前任期号
    private long   index;          // 日志序号
    private String type;           // TRIGGER / REGISTRY / CONFIG_CHANGE
    private byte[] data;           // 序列化数据
    private long   prevLogIndex;   // 前一条日志索引
    private long   prevLogTerm;    // 前一条日志任期
    private long   commitIndex;    // 已提交的日志索引
}

// Master 写入 WAL
public class WalStore {
    
    private final RedisTemplate<String, String> redis;
    private final String walKey = "jzero:wal:entries";
    
    public void append(WalEntry entry) {
        // 写入 Redis List（生产环境建议换成 RocksDB 或 Kafka）
        redis.opsForList().rightPush(walKey, KryoUtil.serialize(entry));
        // 异步推送给所有 Slave
        scheduler.execute(() -> replicateToFollowers(entry));
    }
    
    // 推送 WAL 到指定 Slave
    private void replicateToFollowers(WalEntry entry) {
        for (String follower : getActiveFollowers()) {
            nettyClient.sendAppendEntries(follower, entry);
        }
    }
}
```

### 3.4 Executor 重连机制

```
Executor 发现 Master 断开
    │
    ▼
立即尝试连接其他 Slave 节点（获取最新配置）
    │
    ▼
获取 cluster 节点列表，依次尝试连接
    │
    ▼
连接成功后，汇报自身状态（registry + 任务进度）
    │
    ▼
新 Master 下发增量同步（从上次 checkpoint 恢复）
```

---

## 四、任务调度核心

### 4.1 时间轮调度

使用 HashedWheelTimer 实现高精度、低延迟调度：

```java
// 调度器核心
public class JZeroScheduler {
    
    // 时间轮： tickDuration=100ms, ticksPerWheel=360 (36秒一圈)
    private final HashedWheelTimer wheelTimer = new HashedWheelTimer(
        new NamedThreadFactory("jzero-scheduler-timer"),
        100, TimeUnit.MILLISECONDS,
        360
    );
    
    // 调度触发
    public void schedule(JobTask task, long triggerAtMs) {
        long delay = triggerAtMs - System.currentTimeMillis();
        if (delay <= 0) delay = 0;
        
        wheelTimer.newTimeout(timeout -> {
            if (!task.isCancelled()) {
                submitToDisruptor(task);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    // CRON 任务注册
    public void scheduleCron(JobInfo job, String cronExpr) {
        CronExpression cron = new CronExpression(cronExpr);
        long next = cron.getNextValidTimeAfter(new Date()).getTime();
        schedule(new CronJobTask(job, cron), next);
    }
}
```

### 4.2 Disruptor 无锁队列

任务触发后进入 Disruptor 队列，后端线程池消费，保证高吞吐：

```java
public class TaskDisruptor {
    
    // Ring Buffer 大小：2^20 = 1048576
    private static final int RING_BUFFER_SIZE = 1 << 20;
    
    private final Disruptor<TaskEvent> disruptor;
    
    public TaskDisruptor(ExecutorService workerPool) {
        this.disruptor = new Disruptor<>(
            TaskEvent::new,
            RING_BUFFER_SIZE,
            workerPool,
            ProducerType.MULTI,      // 多生产者（多个 TriggerThread）
            new BlockingWaitStrategy() // 阻塞等待，低延迟场景可用 BusySpin
        );
        
        // 消费者：路由 + RPC 发送
        this.disruptor.handleEventsWith(this::routeAndDispatch);
        
        this.disruptor.start();
    }
    
    // 生产者：TriggerThread 提交任务
    public void publishTrigger(JobTriggerEvent event) {
        long sequence = disruptor.publishEvent((e, sequence, evt) -> {
            e.setJobId(evt.getJobId());
            e.setHandler(evt.getHandler());
            e.setParam(evt.getParam());
            e.setLogId(evt.getLogId());
        }, event);
    }
    
    // 消费端：路由选择执行器 → Netty RPC 发送
    private void routeAndDispatch(TaskEvent event, long sequence, boolean endOfBatch) {
        List<String> executors = registry.getAliveExecutors(event.getJobGroup());
        if (executors.isEmpty()) {
            log.warn("No available executor for job: {}", event.getJobId());
            return;
        }
        
        ExecutorRouter router = RouterFactory.getRouter(event.getRouteStrategy());
        String target = router.route(event, executors);
        
        // 异步 RPC 发送，不阻塞
        rpcClient.sendRunRequest(target, event.toRunRequest());
    }
}
```

### 4.3 Netty RPC 通信

执行器与调度中心之间采用高性能 Netty RPC：

```java
// 服务端（调度中心）
public class RpcServer {
    
    private final ServerBootstrap bootstrap = new ServerBootstrap();
    
    public void start(int port) {
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 1024)
            .option(ChannelOption.TCP_NODELAY, true)        // 禁用 Nagle，低延迟
            .option(ChannelOption.SO_KEEPALIVE, true)       // TCP 保活
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline()
                        .addLast(new KryoEncoder())        // Kryo 高速序列化
                        .addLast(new KryoDecoder())
                        .addLast(new IdleStateHandler(30, 0, 0)) // 30s 读空闲检测
                        .addLast(new RpcServerHandler());  // 业务处理
                }
            })
            .bind(port)
            .sync();
    }
}

// RunRequest 消息体
public class RunRequest implements Serializable {
    private long     jobId;
    private long     logId;
    private String    handler;
    private String    param;
    private int       shardIndex;
    private int       shardTotal;
    private String    traceId;     // 链路追踪
    private String    accessToken; // 执行器认证 token
}

// RPC Handler
@ChannelHandler.Sharable
public class RpcServerHandler extends ChannelInboundHandlerAdapter {
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof RunRequest req) {
            // 提交到执行器内部 Disruptor
            executor.submit(req);
        } else if (msg instanceof BeatRequest req) {
            // 心跳
            ctx.writeAndFlush(new BeatResponse(System.currentTimeMillis()));
        } else if (msg instanceof LogReportRequest req) {
            // 日志上报
            logService.append(req.getLogId(), req.getLogContent());
        }
    }
}
```

### 4.4 路由策略实现

```java
public interface ExecutorRouter {
    String route(TriggerContext ctx, List<String> aliveExecutors);
}

// 一致性哈希（保证相同参数的任务落在同一执行器）
public class ConsistentHashRouter implements ExecutorRouter {
    
    private final TreeMap<Long, String> ring = new TreeMap<>();
    private final int virtualNodes;
    
    public ConsistentHashRouter(int virtualNodes) {
        this.virtualNodes = virtualNodes;
    }
    
    @Override
    public String route(TriggerContext ctx, List<String> executors) {
        // 构建哈希环
        ring.clear();
        for (String addr : executors) {
            for (int i = 0; i < virtualNodes; i++) {
                ring.put(hash("VN-" + addr + "-" + i), addr);
            }
        }
        
        // 根据任务 ID + 参数哈希选择节点
        long keyHash = hash(ctx.getJobId() + ":" + ctx.getParam());
        SortedMap<Long, String> tail = ring.tailMap(keyHash);
        String node = tail.isEmpty() ? ring.firstEntry().getValue() : tail.get(tail.firstKey());
        return node;
    }
    
    private long hash(String key) {
        return MurmurHash.hash64(key);
    }
}
```

---

## 五、微服务治理

### 5.1 API 网关

```java
// 路由定义
@Data
public class RouteDefinition {
    private String id;
    private String path;          // /api/user/**
    private String serviceId;     // user-service
    private int    weight = 100;
    private List<String> filters; // ["Auth", "RateLimit=2000"]
    private Map<String, String> metadata;
}

// 网关处理器
@Component
public class JZeroGatewayHandler {
    
    private final RouteMatcher     routeMatcher;
    private final LoadBalancer     loadBalancer;
    private final MiddlewareChain   middlewareChain;
    private final ServiceDiscovery  discovery;
    
    public Mono<Void> handle(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        
        RouteDefinition route = routeMatcher.match(path);
        if (route == null) {
            return notFound(exchange);
        }
        
        ServiceInstance instance = loadBalancer.select(
            route.getServiceId(),
            route.getWeight(),
            LoadBalancerStrategy.WEIGHTED_ROUND_ROBIN
        );
        
        if (instance == null) {
            return serviceUnavailable(exchange);
        }
        
        // 构建中间件链并执行
        Handler finalHandler = ctx -> proxyToBackend(ctx, instance);
        Handler handler = middlewareChain.build(route.getFilters(), finalHandler);
        
        return Mono.fromRunnable(() -> {
            try {
                handler.handle(new GatewayContext(exchange));
            } catch (Exception e) {
                handleError(exchange, e);
            }
        });
    }
}
```

### 5.2 服务注册与发现

```java
// 服务实例
@Data @AllArgsConstructor
public class ServiceInstance {
    private String instanceId;
    private String serviceId;
    private String host;
    private int    port;
    private String version = "1.0";
    private String group   = "DEFAULT";
    private int    weight  = 100;
    private Map<String, String> metadata;
    private long   lastHeartbeat;
    private InstanceStatus status;
}

// 注册中心抽象
public interface ServiceRegistry {
    void register(ServiceInstance instance);
    void deregister(String serviceId, String instanceId);
    void heartbeat(String serviceId, String instanceId);
    List<ServiceInstance> getInstances(String serviceId);
    List<ServiceInstance> getInstances(String serviceId, String version);
    void subscribe(String serviceId, Consumer<List<ServiceInstance>> listener);
}

// 内置注册中心（Redis 实现）
@Component
public class RedisServiceRegistry implements ServiceRegistry {
    
    private static final String REGISTRY_PREFIX = "jzero:registry:";
    private static final String INSTANCES_KEY    = "jzero:instances:";
    
    @Override
    public void register(ServiceInstance instance) {
        String key = INSTANCES_KEY + instance.getServiceId();
        redisTemplate.opsForHash().put(key, instance.getInstanceId(), toJson(instance));
        redisTemplate.expire(key, 40, TimeUnit.SECONDS);  // TTL 40s
    }
    
    @Override
    public void heartbeat(String serviceId, String instanceId) {
        String key = INSTANCES_KEY + serviceId;
        String json = (String) redisTemplate.opsForHash().get(key, instanceId);
        if (json != null) {
            ServiceInstance inst = fromJson(json);
            inst.setLastHeartbeat(System.currentTimeMillis());
            redisTemplate.opsForHash().put(key, instanceId, toJson(inst));
            redisTemplate.expire(key, 40, TimeUnit.SECONDS);  // 续期
        }
    }
    
    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
        String key = INSTANCES_KEY + serviceId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        return entries.values().stream()
            .map(v -> fromJson((String) v))
            .filter(i -> System.currentTimeMillis() - i.getLastHeartbeat() < 30_000)
            .collect(Collectors.toList());
    }
}
```

### 5.3 熔断器

```java
public class CircuitBreaker {
    
    private final AtomicReference<CircuitState> state = 
        new AtomicReference<>(CircuitState.CLOSED);
    private final AtomicLong        failureCount = new AtomicLong(0);
    private final AtomicLong        successCount = new AtomicLong(0);
    private final CircuitConfig     config;
    private volatile long            lastFailureTime;
    
    public enum CircuitState { CLOSED, OPEN, HALF_OPEN }
    
    public <T> T execute(Supplier<T> action, Supplier<T> fallback) {
        if (state.get() == CircuitState.OPEN) {
            if (shouldAttemptReset()) {
                // 尝试半开
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
    
    private void recordSuccess() {
        successCount.incrementAndGet();
        failureCount.set(0);
        if (state.get() == CircuitState.HALF_OPEN) {
            // 连续成功则关闭
            if (successCount.get() >= config.getHalfOpenSuccessThreshold()) {
                state.set(CircuitState.CLOSED);
                successCount.set(0);
            }
        }
    }
    
    private void recordFailure() {
        lastFailureTime = System.currentTimeMillis();
        if (state.get() == CircuitState.HALF_OPEN) {
            state.set(CircuitState.OPEN);  // 半开下失败立刻打开
        } else if (failureCount.incrementAndGet() >= config.getFailureThreshold()) {
            state.set(CircuitState.OPEN);
        }
    }
    
    private boolean shouldAttemptReset() {
        return System.currentTimeMillis() - lastFailureTime >= config.getRecoveryTimeoutMs();
    }
}
```

### 5.4 限流

```java
// 令牌桶限流（Redis 原子实现，支持集群）
public class RedisRateLimiter {
    
    private final RedisTemplate<String, String> redis;
    private final String                        key;
    private final long                          permitsPerSecond;
    private final long                          burstCapacity;
    
    public RedisRateLimiter(RedisTemplate<String, String> redis, 
                           String key, long permitsPerSecond, long burstCapacity) {
        this.redis = redis;
        this.key = "jzero:ratelimit:" + key;
        this.permitsPerSecond = permitsPerSecond;
        this.burstCapacity = burstCapacity;
    }
    
    // Lua 脚本保证原子性
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
        local allowed = 0
        
        if new_tokens >= requested then
            new_tokens = new_tokens - requested
            allowed = 1
        end
        
        redis.call('HMSET', key, 'tokens', new_tokens, 'last_time', now)
        redis.call('EXPIRE', key, 60)
        
        return {allowed, new_tokens}
        """;
    
    public boolean tryAcquire(int permits) {
        String[] keys = { key };
        String[] args = {
            String.valueOf(permitsPerSecond),
            String.valueOf(burstCapacity),
            String.valueOf(System.currentTimeMillis() / 1000.0),
            String.valueOf(permits)
        };
        Long[] result = redis.execute(
            (RedisCallback<Long[]>) conn -> 
                (Long[]) conn.eval(SCRIPT, ReturnType.MULTI, 1, keys, args)
        );
        return result != null && result[0] == 1L;
    }
}
```

### 5.5 中间件链

```java
// 中间件接口
@FunctionalInterface
public interface Middleware {
    GatewayHandler apply(GatewayHandler next);
}

// 日志中间件
public class LoggingMiddleware implements Middleware {
    @Override
    public GatewayHandler apply(GatewayHandler next) {
        return ctx -> {
            long start = System.nanoTime();
            ctx.response().addCompleteListener(e -> {
                long cost = (System.nanoTime() - start) / 1_000_000;
                log.info("{} {} {}ms {}", 
                    ctx.request().method(),
                    ctx.request().path(),
                    cost,
                    ctx.response().status()
                );
            });
            next.handle(ctx);
        };
    }
}

// 认证中间件
public class AuthMiddleware implements Middleware {
    private final JwtUtil jwtUtil;
    
    @Override
    public GatewayHandler apply(GatewayHandler next) {
        return ctx -> {
            String token = ctx.request().header("Authorization");
            if (token == null || !jwtUtil.verify(token)) {
                ctx.response().setStatus(401);
                ctx.response().write("Unauthorized");
                return;
            }
            next.handle(ctx);
        };
    }
}

// 链式组合
GatewayHandler businessHandler = exchange -> proxyToBackend(exchange);
GatewayHandler chain = new MiddlewareChain()
    .use(new TracingMiddleware())
    .use(new LoggingMiddleware())
    .use(new AuthMiddleware(jwt))
    .use(new RateLimitMiddleware(limiter))
    .build(businessHandler);
```

---

## 六、执行器 SDK

### 6.1 核心注解与处理器

```java
// 任务注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JZeroJob {
    String value();                            // handler 名称
    int    initStrategy() default 0;           // 初始化策略
    int    destroyStrategy() default 0;       // 销毁策略
}

// 任务上下文
@Data
public class JobContext {
    private final long   jobId;
    private final long   logId;
    private final String param;
    private final int    shardIndex;
    private final int    shardTotal;
    private final String traceId;
}

// 任务基类
public abstract class JobHandler {
    
    public ReturnT<String> execute(JobContext ctx) throws Exception {
        return execute0(ctx);
    }
    
    protected abstract ReturnT<String> execute0(JobContext ctx) throws Exception;
    
    public void init()  {}
    public void destroy() {}
}

// 业务使用
@Component
public class OrderSyncHandler extends JobHandler {
    
    @JZeroJob("orderSync")
    @Override
    protected ReturnT<String> execute0(JobContext ctx) {
        int shard = ctx.getShardIndex();
        int total = ctx.getShardTotal();
        log.info("处理分片 {}/{}", shard, total);
        
        List<Order> orders = orderService.fetchPending(shard, total);
        for (Order o : orders) {
            syncToEs(o);
        }
        return ReturnT.SUCCESS;
    }
}
```

### 6.2 执行器启动流程

```
Spring Boot 启动
    │
    ▼
读取 @JZeroJob 注解的方法，注册 handler 映射
    │
    ▼
启动 Netty RPC Client，连接到调度中心（Master 或任意 Slave）
    │
    ▼
注册自身（appName + 地址 + handler 列表 + 分片数量）
    │
    ▼
启动心跳线程（每 30s 发送一次 Beat）
    │
    ▼
启动日志上报线程（批量异步上报执行日志）
    │
    ▼
等待接收 RunRequest
    │
    ▼
接收 → Disruptor 队列 → 线程池执行 → 返回结果 → 批量上报日志
```

---

## 七、数据库设计

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
    block_strategy      VARCHAR(20) DEFAULT 'SERIAL' COMMENT 'SERIAL|DISCARD|COVER',
    trigger_status      TINYINT DEFAULT 0,
    trigger_last_time   BIGINT DEFAULT 0,
    trigger_next_time   BIGINT DEFAULT 0,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_trigger_next (trigger_next_time, trigger_status)
);

-- 执行日志
CREATE TABLE jz_job_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_group       INT NOT NULL,
    job_id          INT NOT NULL,
    executor_address VARCHAR(255) DEFAULT '',
    executor_handler VARCHAR(255) NOT NULL,
    executor_param  VARCHAR(1024) DEFAULT '',
    trigger_time    DATETIME(3),
    trigger_code    INT DEFAULT 0,
    trigger_msg     TEXT,
    handle_time     DATETIME(3),
    handle_code     INT DEFAULT 0,
    handle_msg      TEXT,
    alarm_status    TINYINT DEFAULT 0,
    trace_id        VARCHAR(64) DEFAULT '',
    INDEX idx_job_id (job_id),
    INDEX idx_trigger_time (trigger_time)
);

-- 服务注册表（心跳）
CREATE TABLE jz_service_registry (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_id   VARCHAR(64) NOT NULL,
    instance_id  VARCHAR(128) NOT NULL,
    host         VARCHAR(64) NOT NULL,
    port         INT NOT NULL,
    version      VARCHAR(32) DEFAULT '1.0',
    `group`      VARCHAR(32) DEFAULT 'DEFAULT',
    weight       INT DEFAULT 100,
    metadata     TEXT COMMENT 'JSON 格式标签',
    status       TINYINT DEFAULT 1 COMMENT '1=UP 0=DOWN',
    heartbeat_at BIGINT NOT NULL,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_instance (instance_id),
    INDEX idx_service (service_id, status)
);
```

---

## 八、Spring Boot Starter

```yaml
# application.yml
jzero:
  enabled: true
  
  # 节点配置
  node:
    id: ${HOSTNAME:node-1}      # 节点唯一 ID
    cluster:
      mode: cluster             # standalone / cluster
      election: redis           # redis / raft
      registry:
        - host: 10.0.0.1:6379
        - host: 10.0.0.2:6379
        - host: 10.0.0.3:6379
  
  # 调度中心
  scheduler:
    enabled: true
    port: 7777                  # Netty RPC 端口
  
  # 执行器（业务应用）
  executor:
    enabled: false
    app-name: ${spring.application.name}
    admin-addresses: http://10.0.0.1:7777,http://10.0.0.2:7777,http://10.0.0.3:7777
    port: 0                     # 0=不占用额外端口，使用共享 Netty 连接
    log-path: /data/logs/jzero
    token: ${JZERO_TOKEN:}
  
  # 微服务网关
  gateway:
    enabled: false
    port: 8888
  
  # 服务注册
  registry:
    type: redis                 # redis / nacos / embedded
```

```java
// 自动配置
@Configuration
@ConditionalOnProperty(prefix = "jzero", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JZeroAutoConfiguration {
    
    @Bean
    public JZeroScheduler jZeroScheduler(JZeroProperties p) {
        return new JZeroScheduler(p.getScheduler());
    }
    
    @Bean
    public ExecutorClient executorClient(JZeroProperties p) {
        return new ExecutorClient(p.getExecutor());
    }
    
    @Bean
    public JZeroGateway jZeroGateway(JZeroProperties p) {
        return new JZeroGateway(p.getGateway());
    }
    
    @Bean
    public ServiceRegistry serviceRegistry(JZeroProperties p) {
        return new RedisServiceRegistry(p.getRegistry());
    }
}
```

---

## 九、模块工程结构

```
jzero/
├── pom.xml                          # 父 pom
│
├── jzero-common/                    # 公共层（所有模块依赖）
│   ├── annotation/                  # 注解（@JZeroJob 等）
│   ├── model/                       # DTO / VO / 枚举
│   ├── rpc/                         # RPC 消息体（RunRequest / BeatRequest）
│   └── util/                        # 工具类
│
├── jzero-core/                      # 核心抽象
│   ├── scheduler/                   # 调度抽象（TriggerParam / RouteStrategy）
│   ├── registry/                    # 注册中心抽象（ServiceRegistry SPI）
│   ├── circuit/                     # 熔断器
│   └── middleware/                  # 中间件接口
│
├── jzero-scheduler/                 # 调度中心模块
│   ├── trigger/                     # 时间轮调度
│   ├── election/                    # 主从选举
│   ├── wal/                         # WAL 日志同步
│   ├── rpc-server/                  # Netty RPC 服务端
│   └── log/                         # 执行日志服务
│
├── jzero-executor/                  # 执行器 SDK
│   ├── handler/                     # JobHandler 基类
│   ├── client/                      # Netty RPC 客户端
│   ├── netty/                       # Disruptor + 线程池
│   └── annotation-processor/       # 编译时扫描 @JZeroJob
│
├── jzero-gateway/                   # API 网关
│   ├── router/                      # 动态路由
│   ├── filter/                      # 网关过滤器
│   └── loadbalancer/                # 负载均衡策略
│
├── jzero-registry/                  # 注册中心
│   ├── embedded/                    # 内置轻量实现
│   └── redis/                       # Redis 实现（生产推荐）
│
├── jzero-admin/                     # 控制台后端
│
├── jzero-cli/                       # CLI 代码生成
│
├── jzero-spring-boot-starter/       # Spring Boot 自动装配
│
└── jzero-test/                      # 测试工具
```

---

## 十、开发计划

| 阶段 | 时间 | 里程碑 |
|------|------|--------|
| **Phase 1** | 第 1-3 月 | 分布式集群核心：Raft 选举 + WAL + 节点通信 |
| **Phase 2** | 第 2-4 月 | 调度核心：时间轮 + Disruptor + Netty RPC + 8 种路由 |
| **Phase 3** | 第 3-4 月 | 执行器 SDK + Spring Boot Starter |
| **Phase 4** | 第 4-5 月 | 微服务治理：网关 + 注册发现 + 熔断 + 限流 + 中间件链 |
| **Phase 5** | 第 5-6 月 | 控制台 + CLI + 链路追踪 + 告警 |
| **Phase 6** | 第 6-7 月 | 压测 + 性能优化 + 文档 + Release |

---

## 十一、关键技术指标承诺

| 指标 | 目标 | 压测方法 |
|------|------|----------|
| 调度延迟 P99 | < 10ms | 1 万并发任务 / 秒 |
| RPC 延迟 P99 | < 5ms | JMH 微基准测试 |
| 集群故障切换 | < 3s | 强制 kill Master 进程 |
| 网关吞吐 | > 50,000 QPS | wrk / hey 压测 |
| 日志写入吞吐 | > 100,000 条/秒 | 异步批量写入 |
| 内存占用 | < 512MB（空载） | JMX 监控 |

---

*文档维护：研发团队 | 最后更新：2026-03-21*
