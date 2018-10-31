package com.app.my.framework.core.servlet;

import com.app.my.framework.core.annotation.*;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

/**
 * 重写HttpServlet
 * @Author: liuxun
 * @CreateDate: 2018/10/29 下午11:35
 * @Version: 1.0
 */
public class MDispatcherServlet extends HttpServlet {
    /**保存所有配置信息**/
    private Properties p=new Properties();
    /**保存所有被扫描到的相关类名**/
    private List<String> className=new ArrayList<>();
    /**IOC容器**/
    private Map<String,Object> ioc=new HashMap<>();
    /**用于保存URL对应的方法的映射关系**/
    private Map<String,Method> handlerMapping=new HashMap<>();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }
    /**
     * 初始化
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        /**1、加载配置文件。web.xml中init-param指定的配置文件**/
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        /**2、扫描所有相关的类**/
        doScanner(p.getProperty("scanPackge"));

        /**3、初始化所有扫描出来的类的示例，并保存到IOC容器中**/
        doInstance();

        /**4、依赖注入**/
        doAutowired();

        /**5、构建url对应的方法映射**/
        initHandlerMapping();
    }

    /**
     * 解析初始化配置
     * @param location
     */
    private void doLoadConfig(String location){
        InputStream fis=null;
        try {
            fis = this.getClass().getClassLoader().getResourceAsStream(location);
            p.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (fis!=null){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 扫描指定包下面的的类
     * @param packageName
     */
    private void doScanner(String packageName){
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir=new File(url.getFile());
        for (File file:dir.listFiles()){
            if (file.isDirectory()){
                doScanner(packageName+"."+file.getName());
            }else{
                //把扫描出来的路径+类名存入类存储器
                className.add(packageName+"."+file.getName().replace(".class","").trim());
            }
        }
    }

    /**
     * 初始化所有扫描出来的类的示例，并保存到IOC容器中
     */
    private void doInstance(){
        if (className.size()==0) return;
        try {
            for (String cName:className){
                Class<?> clasz = Class.forName(cName);
                if (clasz.isAnnotationPresent(MController.class)){
                    String beanName=lowerFirstCase(clasz.getSimpleName());
                    ioc.put(beanName,clasz.newInstance());
                }
                if (clasz.isAnnotationPresent(MService.class)){
                    MService service = clasz.getAnnotation(MService.class);
                    String beanName = service.value();
                    if (!beanName.equals("")){
                        ioc.put(beanName,clasz.newInstance());
                        continue;
                    }
                    Class<?>[] interfaces = clasz.getInterfaces();
                    for (Class<?> anInterface : interfaces) {
                        ioc.put(anInterface.getName(),clasz.newInstance());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 首字母小写
     * @param str
     * @return
     */
    private String lowerFirstCase(String str){
        char[] chars = str.toCharArray();
        chars[0] +=32;
        return String.valueOf(chars);
    }
    /**
     * 依赖注入
     * **/
    private void doAutowired(){
        if (ioc.isEmpty()) return;
        for (Map.Entry<String,Object> entry:ioc.entrySet()){
            Field[] fieds = entry.getValue().getClass().getDeclaredFields();
            for (Field field:fieds){
                if (!field.isAnnotationPresent(MAutowired.class)) continue;
                MAutowired autowired = field.getAnnotation(MAutowired.class);
                String beanName=autowired.value().trim();
                if ("".equals(beanName)){
                    beanName=lowerFirstCase(field.getType().getSimpleName());
                }
                //设置访问私有属性权限
                field.setAccessible(true);
                try {
                    //给属性赋值
                    Object instance = ioc.get(beanName);
                    field.set(entry.getValue(),instance);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    /**
     * 构建url对应的方法映射
     */
    private void initHandlerMapping(){
        if (ioc.isEmpty()) return;
        for (Map.Entry<String,Object> entry:ioc.entrySet()){
            Class<?> clasz = entry.getValue().getClass();
            if (!clasz.isAnnotationPresent(MController.class)) continue;

            //获取url对应的method
            String baseUrl="";
            if (clasz.isAnnotationPresent(MRequestMapping.class)){
                MRequestMapping requestMapping=clasz.getAnnotation(MRequestMapping.class);
                baseUrl=requestMapping.value();
            }

            Method[] method = clasz.getMethods();
            for (Method method1 : method) {
                if(!method1.isAnnotationPresent(MRequestMapping.class)) continue;
                MRequestMapping methodMapping=method1.getAnnotation(MRequestMapping.class);
                String url=baseUrl+methodMapping.value().replaceAll("/+","/");
                handlerMapping.put(url,method1);
                System.out.println("url is "+url+",method is "+method1);
            }
        }
    }

    /**
     * 将请求转发到对应的方法上去
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (handlerMapping.isEmpty()) return;
        //返回url
        String url=req.getRequestURI();
        //可返回站点的根路径
        String contextPath = req.getContextPath();

        url=url.replace(contextPath,"").replaceAll("/+","/");
        if (!handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found!");
            return;
        }

        //获取url对应的方法
        Method method = handlerMapping.get(url);
        //获取方法对应的参数
        Class<?>[] paramsTypes = method.getParameterTypes();
        //获取请求url中的参数
        Map<String,String[]> urlParams = req.getParameterMap();
//        Object[] paramValues=new Object[cm.getParameterTypes().length];
        List<Object> paramVals=new ArrayList<>();
        //给已知的参数赋值
        for (int i = 0; i < paramsTypes.length; i++) {
            Class<?> paramsType = paramsTypes[i];
            if (paramsType == HttpServletRequest.class){
//                paramValues[i]=req;
                paramVals.add(req);
                continue;
            }
            if (paramsType == HttpServletResponse.class){
//                paramValues[i]=resp;
                paramVals.add(resp);
                continue;
            }
        }
        //从url参数列表中获取参数名对应的值
        for (Map.Entry<String,String[]> entry:urlParams.entrySet()) {
            paramVals.add(entry.getValue()[0]);
        }
        /**执行方法**/
        try {
            String beanName=lowerFirstCase(method.getDeclaringClass().getSimpleName());
            Object[] p=paramVals.toArray();
            method.invoke(ioc.get(beanName),p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
