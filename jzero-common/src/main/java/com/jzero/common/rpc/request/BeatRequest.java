package com.jzero.common.rpc.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @author: ZhouYingAn
 * @date: 2026/3/21 15:10
 * @Version: 1.0
 * @description: 心跳请求
 */

@Data
public class BeatRequest implements Serializable {
    private String executorAddress;
    private String appName;
    private String uuid;  // 执行器唯一标识
    private Map<String, String> registryData;  // 注册数据
}
