# JZero Framework V1.0 — POM 配置参考

> 版本：1.0  
> 日期：2026-03-21  
> 说明：Maven 多模块项目完整 POM 配置参考

---

## 一、父级 POM 完整配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 项目信息 -->
    <groupId>com.jzero</groupId>
    <artifactId>jzero</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>JZero Framework</name>
    <description>高性能分布式任务调度 + 轻量微服务治理一体化框架</description>
    <url>https://github.com/jzero-framework/jzero</url>

    <!-- 开源协议 -->
    <licenses>
        <license>
            <name>Apache License 2.0</name>
            <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
            <distribution>repo</distribution>
        </license>
    </licenses>

    <!-- 项目成员 -->
    <developers>
        <developer>
            <name>JZero Team</name>
            <email>team@jzero.dev</email>
            <organization>JZero Open Source</organization>
            <organizationUrl>https://jzero.dev</organizationUrl>
        </developer>
    </developers>

    <!-- SCM 配置 -->
    <scm>
        <connection>scm:git:https://github.com/jzero-framework/jzero.git</connection>
        <developerConnection>scm:git:https://github.com/jzero-framework/jzero.git</developerConnection>
        <url>https://github.com/jzero-framework/jzero</url>
    </scm>

    <!-- 模块定义 -->
    <modules>
        <module>jzero-common</module>
        <module>jzero-core</module>
        <module>jzero-scheduler</module>
        <module>jzero-executor</module>
        <module>jzero-gateway</module>
        <module>jzero-registry</module>
        <module>jzero-admin</module>
        <module>jzero-cli</module>
        <module>jzero-spring-boot-starter</module>
        <module>jzero-test</module>
    </modules>

    <!-- 属性定义 -->
    <properties>
        <!-- ========== Java 版本 ========== -->
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>

        <!-- ========== Spring Boot ========== -->
        <spring-boot.version>3.2.0</spring-boot.version>
        <spring-boot-maven-plugin.version>3.2.0</spring-boot-maven-plugin.version>

        <!-- ========== 核心依赖 ========== -->
        <netty.version>4.1.100.Final</netty.version>
        <disruptor.version>3.5.5</disruptor.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <kryo.version>5.5.0</kryo.version>
        <guava.version>32.1.3-jre</guava.version>
        <hutool.version>5.8.22</hutool.version>
        <lombok.version>1.18.30</lombok.version>

        <!-- ========== 数据库 ========== -->
        <mysql.version>8.0.33</mysql.version>
        <hikaricp.version>5.1.0</hikaricp.version>

        <!-- ========== Redis ========== -->
        <redis.version>6.2.12</redis.version>
        <redisson.version>3.24.3</redisson.version>

        <!-- ========== 注册中心 ========== -->
        <nacos.version>2.2.3</nacos.version>
        <consul.version>1.2.8</consul.version>

        <!-- ========== 微服务 ========== -->
        <spring-cloud.version>2023.0.0</spring-cloud.version>
        <spring-cloud-openfeign.version>4.1.0</spring-cloud-openfeign.version>

        <!-- ========== 消息队列 ========== -->
        <rocketmq.version>5.1.4</rocketmq.version>
        <kafka.version>3.6.0</kafka.version>

        <!-- ========== 链路追踪 ========== -->
        <opentelemetry.version>1.32.0</opentelemetry.version>
        <opentelemetry-instrumentation.version>1.32.0</opentelemetry-instrumentation.version>
        <jaeger.version>1.54.0</jaeger.version>

        <!-- ========== 工具类 ========== -->
        <picocli.version>4.7.4</picocli.version>
        <commons-lang3.version>3.14.0</commons-lang3.version>
        <commons-collections4.version>4.4</commons-collections4.version>
        <fastjson2.version>2.0.43</fastjson2.version>

        <!-- ========== 测试 ========== -->
        <junit.version>5.10.1</junit.version>
        <mockito.version>5.7.0</mockito.version>
        <assertj.version>3.24.2</assertj.version>
        <jmh.version>1.37</jmh.version>
        <testcontainers.version>1.19.3</testcontainers.version>

        <!-- ========== Maven 插件 ========== -->
        <maven-compiler-plugin.version>3.11.0</maven-compiler-plugin.version>
        <maven-surefire-plugin.version>3.2.2</maven-surefire-plugin.version>
        <maven-source-plugin.version>3.3.0</maven-source-plugin.version>
        <maven-javadoc-plugin.version>3.6.2</maven-javadoc-plugin.version>
        <maven-gpg-plugin.version>3.1.0</maven-gpg-plugin.version>
        <flatten-maven-plugin.version>1.5.0</flatten-maven-plugin.version>
    </properties>

    <!-- 依赖版本管理 (BOM) -->
    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Spring Cloud BOM -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Netty BOM -->
            <dependency>
                <groupId>io.netty</groupId>
                <artifactId>netty-bom</artifactId>
                <version>${netty.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- OpenTelemetry BOM -->
            <dependency>
                <groupId>io.opentelemetry</groupId>
                <artifactId>opentelemetry-bom</artifactId>
                <version>${opentelemetry.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- JZero 模块 -->
            <dependency>
                <groupId>com.jzero</groupId>
                <artifactId>jzero-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.jzero</groupId>
                <artifactId>jzero-core</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.jzero</groupId>
                <artifactId>jzero-scheduler</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.jzero</groupId>
                <artifactId>jzero-executor</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.jzero</groupId>
                <artifactId>jzero-gateway</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.jzero</groupId>
                <artifactId>jzero-registry</artifactId>
                <version>${project.version}</version>
            </dependency>

            <!-- Disruptor -->
            <dependency>
                <groupId>com.lmax</groupId>
                <artifactId>disruptor</artifactId>
                <version>${disruptor.version}</version>
            </dependency>

            <!-- Kryo -->
            <dependency>
                <groupId>com.esotericsoftware</groupId>
                <artifactId>kryo</artifactId>
                <version>${kryo.version}</version>
            </dependency>

            <!-- Redisson -->
            <dependency>
                <groupId>org.redisson</groupId>
                <artifactId>redisson-spring-boot-starter</artifactId>
                <version>${redisson.version}</version>
            </dependency>

            <!-- MyBatis Plus -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>

            <!-- Picocli (CLI) -->
            <dependency>
                <groupId>info.picocli</groupId>
                <artifactId>picocli</artifactId>
                <version>${picocli.version}</version>
            </dependency>

            <!-- Lombok -->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </dependency>

            <!-- TestContainers -->
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- 插件管理 -->
    <build>
        <pluginManagement>
            <plugins>
                <!-- 编译插件 -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>${maven-compiler-plugin.version}</version>
                    <configuration>
                        <source>${java.version}</source>
                        <target>${java.version}</target>
                        <encoding>${project.build.sourceEncoding}</encoding>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                        </annotationProcessorPaths>
                    </configuration>
                </plugin>

                <!-- Spring Boot 打包插件 -->
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot-maven-plugin.version}</version>
                    <configuration>
                        <excludes>
                            <exclude>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                            </exclude>
                        </excludes>
                    </configuration>
                </plugin>

                <!-- 测试插件 -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>${maven-surefire-plugin.version}</version>
                    <configuration>
                        <includes>
                            <include>**/*Test.java</include>
                            <include>**/*Tests.java</include>
                        </includes>
                        <excludes>
                            <exclude>**/*IntegrationTest.java</exclude>
                        </excludes>
                    </configuration>
                </plugin>

                <!-- Source 插件 -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-source-plugin</artifactId>
                    <version>${maven-source-plugin.version}</version>
                    <executions>
                        <execution>
                            <id>attach-sources</id>
                            <phase>verify</phase>
                            <goals>
                                <goal>jar-no-fork</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>

                <!-- Javadoc 插件 -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-javadoc-plugin</artifactId>
                    <version>${maven-javadoc-plugin.version}</version>
                    <configuration>
                        <source>${java.version}</source>
                        <encoding>${project.build.sourceEncoding}</encoding>
                        <doclint>none</doclint>
                        <additionalparam>-Xdoclint:none</additionalparam>
                    </configuration>
                    <executions>
                        <execution>
                            <id>attach-javadocs</id>
                            <phase>verify</phase>
                            <goals>
                                <goal>jar</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>

                <!-- Flatten 插件 (处理 .flattened-pom.xml) -->
                <plugin>
                    <groupId>org.codehaus.mojo</groupId>
                    <artifactId>flatten-maven-plugin</artifactId>
                    <version>${flatten-maven-plugin.version}</version>
                    <configuration>
                        <updatePomFile>true</updatePomFile>
                        <flattenMode>resolveCiFriendliesOnly</flattenMode>
                    </configuration>
                    <executions>
                        <execution>
                            <id>flatten</id>
                            <phase>process-resources</phase>
                            <goals>
                                <goal>flatten</goal>
                            </goals>
                        </execution>
                        <execution>
                            <id>flatten.clean</id>
                            <phase>clean</phase>
                            <goals>
                                <goal>clean</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </pluginManagement>

        <!-- 所有模块共享的插件 -->
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>flatten-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

    <!-- 报告输出编码 -->
    <reporting>
        <outputEncoding>${project.reporting.outputEncoding}</outputEncoding>
    </reporting>

    <!-- 仓库配置 -->
    <repositories>
        <repository>
            <id>central</id>
            <url>https://repo.maven.apache.org/maven2</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
        <repository>
            <id>aliyun</id>
            <url>https://maven.aliyun.com/repository/public</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>

    <!-- 插件仓库配置 -->
    <pluginRepositories>
        <pluginRepository>
            <id>central</id>
            <url>https://repo.maven.apache.org/maven2</url>
        </pluginRepository>
        <pluginRepository>
            <id>aliyun</id>
            <url>https://maven.aliyun.com/repository/public</url>
        </pluginRepository>
    </pluginRepositories>

    <!-- 持续集成配置 (供 CI 使用) -->
    <profiles>
        <!-- Release 发布配置 -->
        <profile>
            <id>release</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-gpg-plugin</artifactId>
                        <version>${maven-gpg-plugin.version}</version>
                        <executions>
                            <execution>
                                <id>sign-artifacts</id>
                                <phase>verify</phase>
                                <goals>
                                    <goal>sign</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>

        <!-- JDK 21+ 支持 -->
        <profile>
            <id>jdk21+</id>
            <activation>
                <jdk>[21,)</jdk>
            </activation>
            <properties>
                <maven.compiler.source>21</maven.compiler.source>
                <maven.compiler.target>21</maven.compiler.target>
            </properties>
        </profile>
    </profiles>

</project>
```

---

## 二、子模块 POM 模板

### 2.1 jzero-common (公共层)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.jzero</groupId>
        <artifactId>jzero</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>jzero-common</artifactId>
    <packaging>jar</packaging>

    <name>JZero Common</name>
    <description>公共层：RPC 消息体、枚举、工具类</description>

    <!-- 无其他模块依赖 -->

    <dependencies>
        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <!-- Kryo 序列化 -->
        <dependency>
            <groupId>com.esotericsoftware</groupId>
            <artifactId>kryo</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- 工具类 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

### 2.2 jzero-core (核心抽象层)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.jzero</groupId>
        <artifactId>jzero</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>jzero-core</artifactId>
    <packaging>jar</packaging>

    <name>JZero Core</name>
    <description>核心抽象层：调度、注册中心、熔断器、负载均衡</description>

    <dependencies>
        <!-- 依赖公共层 -->
        <dependency>
            <groupId>com.jzero</groupId>
            <artifactId>jzero-common</artifactId>
        </dependency>

        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

### 2.3 jzero-scheduler (调度中心)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.jzero</groupId>
        <artifactId>jzero</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>jzero-scheduler</artifactId>
    <packaging>jar</packaging>

    <name>JZero Scheduler</name>
    <description>调度中心：时间轮、Disruptor、Netty RPC、选举</description>

    <dependencies>
        <!-- 依赖核心层 -->
        <dependency>
            <groupId>com.jzero</groupId>
            <artifactId>jzero-core</artifactId>
        </dependency>

        <!-- Netty -->
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-all</artifactId>
        </dependency>

        <!-- Disruptor -->
        <dependency>
            <groupId>com.lmax</groupId>
            <artifactId>disruptor</artifactId>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
        </dependency>

        <!-- 数据库 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

### 2.4 jzero-executor (执行器 SDK)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.jzero</groupId>
        <artifactId>jzero</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>jzero-executor</artifactId>
    <packaging>jar</packaging>

    <name>JZero Executor</name>
    <description>执行器 SDK：注解、Handler、RPC 客户端</description>

    <dependencies>
        <!-- 依赖核心层 -->
        <dependency>
            <groupId>com.jzero</groupId>
            <artifactId>jzero-core</artifactId>
        </dependency>

        <!-- Netty -->
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-all</artifactId>
        </dependency>

        <!-- Disruptor -->
        <dependency>
            <groupId>com.lmax</groupId>
            <artifactId>disruptor</artifactId>
        </dependency>

        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

### 2.5 jzero-gateway (API 网关)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.jzero</groupId>
        <artifactId>jzero</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>jzero-gateway</artifactId>
    <packaging>jar</packaging>

    <name>JZero Gateway</name>
    <description>API 网关：路由、过滤器、限流、熔断</description>

    <dependencies>
        <!-- 依赖核心层 -->
        <dependency>
            <groupId>com.jzero</groupId>
            <artifactId>jzero-core</artifactId>
        </dependency>

        <!-- Spring WebFlux (响应式 Web) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>

        <!-- Netty -->
        <dependency>
            <groupId>io.projectreactor.netty</groupId>
            <artifactId>reactor-netty</artifactId>
        </dependency>

        <!-- Redis (用于限流) -->
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

### 2.6 jzero-spring-boot-starter (自动装配)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.jzero</groupId>
        <artifactId>jzero</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>jzero-spring-boot-starter</artifactId>
    <packaging>jar</packaging>

    <name>JZero Spring Boot Starter</name>
    <description>Spring Boot 自动装配</description>

    <dependencies>
        <!-- 依赖所有核心模块 -->
        <dependency>
            <groupId>com.jzero</groupId>
            <artifactId>jzero-scheduler</artifactId>
        </dependency>
        <dependency>
            <groupId>com.jzero</groupId>
            <artifactId>jzero-executor</artifactId>
        </dependency>
        <dependency>
            <groupId>com.jzero</groupId>
            <artifactId>jzero-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>com.jzero</groupId>
            <artifactId>jzero-registry</artifactId>
        </dependency>

        <!-- Spring Boot 配置处理器 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

</project>
```

---

## 三、常用命令

```bash
# 1. 清理并安装所有模块
mvn clean install -DskipTests

# 2. 只编译不测试
mvn clean compile

# 3. 运行测试
mvn test

# 4. 打包 (跳过测试和签名)
mvn clean package -DskipTests

# 5. 发布到本地仓库
mvn clean install

# 6. 完整 Release 发布
mvn clean package -P release source:jar javadoc:jar gpg:sign

# 7. 查看依赖树
mvn dependency:tree

# 8. 检查依赖冲突
mvn dependency:analyze

# 9. 更新依赖版本
mvn versions:display-dependency-updates

# 10. 单模块构建
cd jzero-scheduler && mvn clean install -DskipTests
```

---

## 四、常见问题

### Q1: 如何添加新的子模块？

```bash
# 1. 在父 pom.xml <modules> 中添加
<module>jzero-new-module</module>

# 2. 创建模块目录
mkdir -p jzero-new-module/src/main/java/com/jzero
mkdir -p jzero-new-module/src/test/java/com/jzero

# 3. 创建 pom.xml，继承父模块
# (参考 2.1-2.6 模板)
```

### Q2: 如何排除依赖冲突？

```xml
<!-- 方式1: 排除 -->
<dependency>
    <groupId>xxx</groupId>
    <artifactId>xxx</artifactId>
    <exclusions>
        <exclusion>
            <groupId>冲突的包</groupId>
            <artifactId>冲突的包</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 方式2: 显式声明版本 -->
<dependency>
    <groupId>xxx</groupId>
    <artifactId>xxx</artifactId>
    <version>正确版本</version>
</dependency>
```

### Q3: 如何配置私有仓库？

```xml
<!-- Maven settings.xml 中配置 -->
<servers>
    <server>
        <id>nexus-releases</id>
        <username>admin</username>
        <password>admin123</password>
    </server>
    <server>
        <id>nexus-snapshots</id>
        <username>admin</username>
        <password>admin123</password>
    </server>
</servers>
```

### Q4: 如何跳过某些模块的构建？

```bash
# 跳过指定模块
mvn clean install -pl '!jzero-admin,!jzero-cli' -DskipTests
```

---

## 五、项目目录结构

```
jzero/
├── pom.xml                    # 父级 POM (本文件)
│
├── jzero-common/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/jzero/common/
│       └── test/java/com/jzero/common/
│
├── jzero-core/
│   ├── pom.xml
│   └── src/
│
├── jzero-scheduler/
│   ├── pom.xml
│   └── src/
│
├── jzero-executor/
│   ├── pom.xml
│   └── src/
│
├── jzero-gateway/
│   ├── pom.xml
│   └── src/
│
├── jzero-registry/
│   ├── pom.xml
│   └── src/
│
├── jzero-admin/
│   ├── pom.xml
│   └── src/
│
├── jzero-cli/
│   ├── pom.xml
│   └── src/
│
├── jzero-spring-boot-starter/
│   ├── pom.xml
│   └── src/
│
└── jzero-test/
    ├── pom.xml
    └── src/
```

---

**文档版本**: 1.0.0  
**最后更新**: 2026-03-21
