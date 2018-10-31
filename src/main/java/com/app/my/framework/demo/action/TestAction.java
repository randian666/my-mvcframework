package com.app.my.framework.demo.action;

import com.app.my.framework.core.annotation.MAutowired;
import com.app.my.framework.core.annotation.MController;
import com.app.my.framework.core.annotation.MRequestMapping;
import com.app.my.framework.core.annotation.MRequestParam;
import com.app.my.framework.demo.service.TestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @Author: liuxun
 * @CreateDate: 2018/10/30 下午2:42
 * @Version: 1.0
 */
@MController
@MRequestMapping("/mtest")
public class TestAction {
    @MAutowired
    private TestService testService;

    @MRequestMapping("/getMsg.action")
    public void getMsg(HttpServletRequest request,
                       HttpServletResponse response,
                       @MRequestParam("name") String name,
                       String remark){
        System.out.println(remark);
        String result = testService.getHello(name);
        PrintWriter out=null;
        try {
            out = response.getWriter();
            out.write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (out!=null){
                out.close();
            }
        }
    }
}
