package com.atguigu.core.exception;

/**
 * @author zzy
 * @create 2019-12-20 10:15
 */
public class OrderException extends  RuntimeException {
    public OrderException() {
    }

    public OrderException(String message) {
        super(message);
    }
}
