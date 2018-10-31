package com.app.my.framework.demo.service.impl;

import com.app.my.framework.core.annotation.MService;
import com.app.my.framework.demo.service.TestService;

/**
 * @Author: liuxun
 * @CreateDate: 2018/10/30 下午2:43
 * @Version: 1.0
 */
@MService("testService")
public class TestServiceImpl implements TestService {
    @Override
    public String getHello(String msg) {
        return "hello "+msg;
    }
}
