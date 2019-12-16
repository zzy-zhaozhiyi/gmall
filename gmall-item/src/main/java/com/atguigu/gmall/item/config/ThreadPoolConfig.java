package com.atguigu.gmall.item.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * @author zzy
 * @create 2019-12-16 16:38
 */
@Configuration
public class ThreadPoolConfig {
    @Bean
    public ThreadPoolExecutor getThreadPool() {
        return new ThreadPoolExecutor(10, 100, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100), Executors.defaultThreadFactory(),new ThreadPoolExecutor.AbortPolicy());
    }
}
