package com.atguigu.gmall.index.annotation;

import java.lang.annotation.*;

/**
 * @author zzy
 *  * @create 2019-12-14 10:12
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
//@Inherited // 子类可继承
@Documented
public @interface GmallCache {

    /**
     * 缓存key的前缀
     * @return
     */
    String prefix() default "";
}
