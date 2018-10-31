# my-mvcframework

手写简洁版springmvc框架，包含ioc，DI等功能

整体架构分为三部分

1、配置阶段
在web.xml中配置请求处理器MDispatcherServlet

2、初始化阶段

在MDispatcherServlet中的init方法中初始化，主要初始化以下内容：

加载配置文件。web.xml中init-param指定的配置文件

扫描所有相关的类

初始化所有扫描出来的类的示例，并保存到IOC容器中

依赖注入

构建url对应的方法映射

3、运行阶段

url请求通过get/post请求后执行doDispatch

doDispatch从handlerMapping中根据请求的url匹配到执行的方法

然后通过反射invoker执行方法

最后通过response.writer输出


