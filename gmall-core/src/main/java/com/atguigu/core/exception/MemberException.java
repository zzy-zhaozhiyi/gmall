package com.atguigu.core.exception;

/**
 * @author zzy
 * @create 2019-12-16 13:56
 */
public class MemberException  extends  RuntimeException{
    public MemberException() {
    }

    public MemberException(String message) {
        super(message);
    }
}
