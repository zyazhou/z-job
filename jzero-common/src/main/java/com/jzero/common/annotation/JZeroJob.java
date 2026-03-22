package com.jzero.common.annotation;

import java.lang.annotation.*;

/**
 * @author: ZhouYingAn
 * @date: 2026/3/21 14:59
 * @Version: 1.0
 * @description: 任务注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
/**
 * 1、RetentionPolicy.SOURCE：注解只保留在源文件，当Java文件编译成class文件的时候，注解被遗弃；
 * 2、RetentionPolicy.CLASS：注解被保留到class文件，但jvm加载class文件时候被遗弃，这是默认的生命周期；
 * 3、RetentionPolicy.RUNTIME：注解不仅被保存到class文件中，jvm加载class文件之后，仍然存在；
 *
 * 这3个生命周期分别对应于：Java源文件(.java文件) —> .class文件 —> 内存中的字节码
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JZeroJob {
    /** * handler名称 */
    String value();
    /** * 初始化策略 */
    int initStrategy() default 0;
    /** * 销毁策略 */
    int destroyStrategy() default 0;
}
