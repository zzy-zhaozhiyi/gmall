# gmall
谷粒商城系统
妮好
项目需要启动的软件
1、nacos 是安装在本机中的（127.0.0.1：8848）
2、es、kibana、ik安装咋虚拟机上，systemctl start es.  cd /opt/kibana/bin -> (非阻塞式启动)nohup ./kibana
3、rabbitmq是通过虚拟机的docker镜像拉取，由于java端口冲突由5672变成5673 启动 systemctl start docker -> docker ps -a (查看id) ->docker start id
   各种消息队列的应用，定时关单，库存解锁用的演示队列和死信队列
4、seata 二阶段提交，性能比较低，但用起来方便，只需要配置数据源代理，加两个自己特有的配置文件（目前和spring boot整合的不是很好，所以需要加）
5、两个前端的vue的项目：shop 和 admin  进入其根路径cmd 接下来 安装npm install   npm start
6、分布式锁用的额是redisson，效率高。加上一个配置即可。异步编排对于多个远程调用的处理
7、jwt的应用：yml 的配置，配置类的
8、gatewa的过滤气的实现
9、手写一个注解以及应用到书写一个切面的操作，热点数据放入缓存的应用到
