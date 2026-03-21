package com.jzero.common.rpc.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: ZhouYingAn
 * @date: 2026/3/21 15:10
 * @Version: 1.0
 * @description: 任务执行请求
 */

@Data
public class RunRequest implements Serializable {
    /**job的全局ID */
    private long jobId;
    private long logId;
    private String handler;
    private String param;
    private int shardIndex;
    private int shardTotal;
    private String traceId;
    private String accessToken;
}
