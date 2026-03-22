package com.jzero.common.model.enums;

/**
 * @author: ZhouYingAn
 * @date: 2026/3/22
 * @Version: 1.0
 * @description: RouteStrategyEnum 枚举定义
 */
public enum RouteStrategyEnum {
    ROUND,      //轮询策略
    RANDOM,     //随机策略
    CONSISTENT_HASH, //一致性哈希
    LEAST_LFU, //Least Frequently Used 最少使用
    FAILOVER,        // 故障转移
    SHARDING,        // 分片广播
    BUSY_TRANSFER,   // 忙碌转移
    WEIGHTED         // 权重轮询
}

