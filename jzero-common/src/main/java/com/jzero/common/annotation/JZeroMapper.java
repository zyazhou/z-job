package com.jzero.common.annotation;

/**
 * @author: ZhouYingAn
 * @date: 2026/3/22
 * @Version: 1.0
 * @description: 配置声明: Mapper 扫描注解，用于扫描并注册继承自 BaseMapper 的 Mapper 接口
 */
import org.springframework.context.annotation.Import;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
/**
 * 把指定类（或动态生成的类）注册到 Spring 容器中
 */
@Import(JZeroMapperRegistrar.class)
public @interface JZeroMapper {
    /** * Mapper 接口所在的包路径 */
    String[] value() default {};
    /** * Mapper 接口所在的包路径（value 的别名） */
    String[] basePackages() default {};
    /** * 扫描限定表达式 * <p>支持 * 通配符，如 com.jzero.*.mapper</p> */
    String[] basePackageClasses() default {};
}