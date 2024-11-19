# 大数据全套笔记

# 环境搭建

> 部分用到的软件都在当前笔记的release的**software**包中

## Linux前提配置

> 如果使用的是虚拟机,需要配置虚拟网络环境,建议使用vm虚拟并且网络使用NAT模式

1. 查看虚拟网络配置在**编辑 -> 虚拟网络编辑器**
2. 打开之后点击更改设置  
   ![img.png](image/1/img.png)
3. 点击NAT类型,选择NAT设置  
   ![img.png](image/1/img_1.png)

里面包含网关和网段信息,我们需要用在Linux配置中

使用克隆并克隆出一台主机两台副机,分别名为**master、slave1、slave2**

### 静态IP配置

> 此操作需要使用root权限使用su命令切换用户`su root`

```shell
# 使用vim编辑器进行更改内容
vim /etc/sysconfig/network-scripts/ifcfg-ens33
```

下方是源配置文件

```
TYPE="Ethernet"
PROXY_METHOD="none"
BROWSER_ONLY="no"
BOOTPROTO="dhcp"
DEFROUTE="yes"
IPV4_FAILURE_FATAL="no"
IPV6INIT="yes"
IPV6_AUTOCONF="yes"
IPV6_DEFROUTE="yes"
IPV6_FAILURE_FATAL="no"
IPV6_ADDR_GEN_MODE="stable-privacy"
NAME="ens33"
UUID="d9e82b4b-d7c0-4ad4-a44e-1b1fbfa9c598"
DEVICE="ens33"
ONBOOT="yes"
```

添加或修改的值

```
BOOTPROTO="static"      #修改为静态的
IPADDR="192.168.1.102"  #添加静态IP
NETMASK="255.255.255.0" #添加子网掩码
GATEWAY="192.168.1.2"   #添加网关
DNS1="192.168.1.2"      #配置DNS
```

添加配置完之后使用`systemctl restart network`命令重启网络

并对其他节点(主机或者副机)进行添加配置,注意IPADDR的值不能设置一样

### hosts和hostname配置

配置好ip之后可以使用ssh连接终端进行更方便快速的配置了,下图连接master节点  
![img_2.png](image/1/img_2.png)

打开终端输入`vim /etc/hosts`编辑命令进行hosts文件进行编辑

在文件中添加以下内容

```
192.168.1.102 master
192.168.1.103 slave1
192.168.1.104 slave2
```

![img_4.png](image/1/img_4.png)

配置完主机的hosts之后再配置一下主机名称

使用`vim /etc/hostname`编辑命令将里面的内容修改成

```
master
```

![img_3.png](image/1/img_3.png)

> 修改了主机名之后需要重启节点才可以显示更改后的名字

配置完之后再配置其他的节点hosts的内容一致,但是hostname内容分别改成slave1、slave2

由于我们使用的Windows连接的节点,最好也给Windows的hosts文件配置一下

在`C:\Windows\System32\drivers\etc\hosts`路径下

> hosts文件的作用是当你解析域名的时候,会先寻找本电脑上的hosts文件有没有对应ip如果有就返回,域名劫持攻击就是利用了hosts文件

### 配置ssh免密登录和关闭防护墙

> 在hadoop中如果不ssh设置免密登录和关闭防火墙,hadoop就不能通信

在Linux中防火墙服务的名称叫**firewalld**,使用`systemctl status firewalld`查看服务的状态

![img_5.png](image/1/img_5.png)

```shell
# 关闭防火墙
systemctl stop firewlld
# 设置开机不自启
systemctl disable firewlld
```

使用`ssh-keygen -t rsa`命令生成密钥,生成时需要点回车键进行确认,需要在每个节点都执行

执行完密钥生成之后就是执行密钥分发了,使用`ssh-copy-id 地址`命令在每台节点上分发到每个节点

```shell
# 所有主机
ssh-copy-id master
ssh-copy-id slave1
ssh-copy-id slave2
```

> 分发完之后,只是分发当前用户的,如果你只在当前用户分发,也只要当前用户是免密登录,如果需要可以进行多个用户分发

### 编写分发脚本和命令执行脚本

分发脚本用于向其他节点分发文件,使用shell编程

```shell
#!/bin/bash

if [ $# -eq 0 ]; then
        echo "没有文件"
        exit
fi
for i in master slave1 slave2; do
        if [ $HOSTNAME != $i ]; then
                for j in $@; do
                        filepath=$(realpath $j)
                        echo "正在上传${filepath}到$i"
                        rsync $filepath $USER@$i:$filepath
                done
        fi
done
```

命令执行脚本用于多节点执行相同的命令

```shell
#!/bin/bash

if [ $# -eq 0 ]; then
        echo "无命令"
        exit
fi

for i in master slave1 slave2; do
        echo "当前${i}正在执行${*}"
        ssh $USER@$i $*
done
```

## JavaJdk环境搭建

将java的软件包解压到`/opt/module`路径下

```shell
tar -zxvf jdk-8u202-linux-x64.tar.gz -C /opt/module
```

也可以使用命令执行脚本批量执行

```shell
/commandsync.sh tar -zxvf /opt/software/jdk-8u202-linux-x64.tar.gz -C /opt/module/
```

> 最好使用Java8的版本,因为许多框架都是基于Java8如果更换版本可以将无法启动某些框架

配置完之后需要配置JAVA_HOME来指定Java的环境变量

```shell
#在/etc/profile文件下配置，或者在/etc/profile.d/目录下新建一个.sh结尾的脚本
vim /etc/profile
```

在文件中写入Java的路径

```
JAVA_HOME=/opt/module/jdk1.8.0_202
PATH=$PATH:$JAVA_HOME/bin
```

写入完之后需要用` . /etc/profile`命令刷新下脚本,可以使用分发脚本分发/etc/profile文件到每个节点中

> .点是脚本执行的一个命令

## Hadoop环境搭建

使用命令执行脚本执行`/commandsync.sh tar -zxvf /opt/software/hadoop-3.3.1.tar.gz -C /opt/module/`
命令解压hadoop到/opt/module文件夹

~~获取hadoop的路径位置在/etc/profile中添加HADOOP_HOME环境变量~~

> 经测试 通过profile的全局环境变量会让ssh用户无法使用jps,可无视这段话

使用`vim ~/.bashrc`命令添加环境变量

```
# MyPath
export JAVA_HOME=/opt/module/jdk1.8.0_202
export PATH=$PATH:$JAVA_HOME/bin
export HADOOP_HOME=/opt/module/hadoop-3.3.1
export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
```

~~用` . /etc/profile`命令刷新下脚本~~

用` . ~/.bashrc`命令刷新下脚本

配置hadoop,分别配置六个文件

> xml配置,我恨你

1. core-site.xml
   ```xml
   <configuration>
        <property>
                <!--配置hdfs端口信息-->
                <name>fs.defaultFS</name>
                <value>hdfs://master:8020</value>
        </property>
        <property>
                <!--设置hadoop文件路径-->
                <name>hadoop.tmp.dir</name>
                <value>/opt/module/hadoop-3.3.1/data</value>
        </property>
   </configuration>
   ```
2. hdfs-site.xml
   ```xml
      <configuration>
        <property>
                <!--配置NameNode地址-->
                <name>dfs.namenode.http-address</name>
                <value>master:9870</value>
        </property>
        <property>
                <!--配置SecondaryNode地址-->
                <name>dfs.namenode.secondary.http-address</name>
                <!--如果配置到master可能无法启动，因为NameNode也在Master-->
                <value>slave1:9868</value>
        </property>
      </configuration>
   ```
3. yarn-site.xml
   ```xml
      <configuration>
        <property>
                <!--配置ResourceManager地址-->
                <name>yarn.resourcemanager.hostname</name>
                <value>master</value>
        </property>
        <property>
                <!--配置ResourceManager网络地址-->
                 <name>yarn.resourcemanager.webapp.address</name>
                <value>master:8088</value>
        </property>
        <property>
                <name>yarn.nodemanager.aux-services</name>
                <value>mapreduce_shuffle</value>
        </property>
      </configuration>
   ```
4. mapred-site.xml
   ```xml
   <configuration>
        <property>
                <!--配置mapreduce为yarn-->
                <name>mapreduce.framework.name</name>
                <value>yarn</value>
        </property>
   </configuration>
   ```
5. workers

   在workers文件中配置节点地址
   ```
   master
   slave1
   slave2
   ```
6. hadoop-env.sh

   按需求配置如果启动报错缺少什么什么就来这个文件配置

配置完毕之后要先执行`hdfs namenode -format`进行hdfs数据初始化

初始化完毕之后便可以启动了

启动hdfs`start-dfs.sh`启动yarn`start-yarn.sh`

关闭hdfs`stop-dfs.sh`关闭yarn`stop-yarn.sh`

### 其他配置

在工作完之后,并没有运行历史记录,这就要配置一下历史服务器了

在mapred-site.xml中配置以下内容

```xml

<configuration>
    <property>
        <name>mapreduce.framework.name</name>
        <value>yarn</value>
    </property>
    <property>
        <name>mapreduce.jobhistory.address</name>
        <value>master:10020</value>
    </property>
    <property>
        <name>mapreduce.jobhistory.webapp.address</name>
        <value>master:19888</value>
    </property>
</configuration>
```

分发之后就可以使用`mapred --daemon start historyserver`命令进行启动了

为了更方便的查看节点上的日志功能,建议开启日志聚集服务器,在**yarn-site.xml**配置文件中配置以下内容

```
<configuration>
    <property>
        <name>yarn.resourcemanager.hostname</name>
        <value>master</value>
    </property>
    <property>
        <name>yarn.resourcemanager.webapp.address</name>
        <value>master:8088</value>
    </property>
    <property>
        <name>yarn.nodemanager.aux-services</name>
        <value>mapreduce_shuffle</value>
    </property>
    <property>
        <!--开启日志聚集功能-->
        <name>yarn.log-aggregation-enable</name>
        <value>true</value>
    </property>
    <property>
        <!--设置日志聚集地址-->
        <name>yarn.log.server.url</name>
        <value>http://master:19888/jobhistory/logs</value>
    </property>
    <property>
        <!--设置保留日志为七天-->
        <name>yarn.log-aggregation.retain-seconds</name>
        <value>604800</value>
    </property>
</configuration>
```

### 编写启动集群脚本

如果我们每次一个一个的启动会太麻烦了,但是我们编写一个集群启动脚本就很方便了,以下是shell编程

```shell
#!/bin/bash

if [ $# -eq 0 ]; then
    echo "你未输入参数"
    exit
fi

case $1 in
  "start")
  echo "启动hdfs"
  start-dfs.sh
  echo "启动yarn"
  start-yarn.sh
  echo "启动historyserver"
  mapred --daemon start historyserver
  ;;
  "stop")
  echo "关闭hdfs"
  stop-dfs.sh
  echo "关闭yarn"
  stop-yarn.sh
  echo "关闭historyserver"
  mapred --daemon stop historyserver
  ;;
  "restart")
  echo "关闭hdfs"
  stop-dfs.sh
  echo "关闭yarn"
  stop-yarn.sh
  echo "关闭historyserver"
  mapred --daemon stop historyserver
  echo "启动hdfs"
  start-dfs.sh
  echo "启动yarn"
  start-yarn.sh
  echo "启动historyserver"
  mapred --daemon start historyserver
  ;;
  *)
    echo "参数不是start/stop/restart" 
  ;;
esac
```

## Hive环境搭建

使用`tar -zxvf /opt/software/apache-hive-3.1.3-bin.tar.gz -C /opt/module/`解压

在 **/etc/profile**配置hive环境变量

```shell
export HIVE_HOME=/opt/module/apache-hive-3.1.3-bin
```

配置完之后在hive的主目录下的conf文件夹中新建一个**hive-site.xml**文件,添加以下内容,注意在hive的lib文件夹中添加mysql连接器,
本笔记中的jars文件夹中有一个**mysql-connector-java-8.0.25**版本的

> 在/opt/module/apache-hive-3.1.3-bin/conf/这里新建

```
<configuration>
   <property>
      <!--设置连接的mysql地址-->
      <name>javax.jdo.option.ConnectionURL</name>
      <value>jdbc:mysql://123.56.82.129/hive</value>
   </property>
   <property>
      <!--设置连接的驱动类-->
      <name>javax.jdo.option.ConnectionDriverName</name>
      <value>com.mysql.cj.jdbc.Driver</value>
   </property>
   <property>
      <!--设置连接的用户名-->
      <name>javax.jdo.option.ConnectionUserName</name>
      <value>root</value>
   </property>
   <property>
      <!--设置连接的密码-->
      <name>javax.jdo.option.ConnectionPassword</name>
      <value>123456</value>
   </property>
   <property>
      <!--设置创建表的默认路径-->
      <name>hive.metastore.warehouse.dir</name>
      <value>/hive/warehouse</value>
   </property>
</configuration>
```

配置完之后进行hive数据库的初始化,使用`schematool -dbType mysql -initSchema -verbose`命令进行初始化

初始化之后便可以用`hive`命令启动hive

启动hive之后就尝试输入一些命令来查看是否可以运行,创建数据库`create database myhive;`
,创建一张简单的表`create table test(id int,name string);`再尝试插入一条数据`insert into test value(1,'zs');`

如果插入数据失败时候,可能是mapreduce配置错误或版本不对,在此笔记上方的**mapred-site.xml**配置并没有添加这些内容

```
<configuration>
        <property>
                <name>mapreduce.framework.name</name>
                <value>yarn</value>
        </property>
        <property>
                <name>mapreduce.jobhistory.address</name>
                <value>master:10020</value>
        </property>
        <property>
                <name>mapreduce.jobhistory.webapp.address</name>
                <value>master:19888</value>
        </property>
        <property>
                <!--配置mapred的环境位置-->         
                <name>yarn.app.mapreduce.am.env</name>
                <value>HADOOP_MAPRED_HOME=/opt/module/hadoop-3.3.1</value>
        </property>
        <property>
                <!--配置map的路径-->         
                <name>mapreduce.map.env</name>
                <value>HADOOP_MAPRED_HOME=/opt/module/hadoop-3.3.1</value>
        </property>
        <property>
                <!--配置reduce的路径-->         
                <name>mapreduce.reduce.env</name>
                <value>HADOOP_MAPRED_HOME=/opt/module/hadoop-3.3.1</value>
        </property>
</configuration>
```

添加之后分发,再尝试插入数据,如果显示下图这样或者在yarnweb上看到进度完成就代表执行成功了

![img_6.png](image/1/img_6.png)

> 在hive中mysql只是存储hive的元数据,真正的数据内容是存储在hadoop的hdfs中

在hive中还有两个服务,他们在hive中起到了关键的作用

### **hiveserver2**服务

hiveserver2服务提供了远程访问数据的功能,当使用用户访问的时候,就需要一个代理用户,我们可以在hadoop中的core-site.xml文件中进行配置

```
<configuration>
        <property>
                <name>fs.defaultFS</name>
                <value>hdfs://master:8020</value>
        </property>
        <property>
                <name>hadoop.tmp.dir</name>
                <value>/opt/module/hadoop-3.3.1/data</value>
        </property>
        <property>
                <!--配置afei用户可以作为代理用户-->         
                <name>hadoop.proxyuser.afei.hosts</name>
                <value>*</value>
        </property>
        <property>
                <!--配置afei用户能够代理任意组-->         
                <name>hadoop.proxyuser.afei.groups</name>
                <value>*</value>
        </property>
        <property>
                <!--配置afei用户能够代理任意用户-->         
                <name>hadoop.proxyuser.afei.users</name>
                <value>*</value>
        </property>
</configuration>
```

不要忘记分发到其他节点(●'◡'●)

配置完之后还要去配置hive的的配置文件╰(‵□′)╯,配置hive-site.xml文件

```
<configuration>
        <property>
                <name>javax.jdo.option.ConnectionURL</name>
                <value>jdbc:mysql://123.56.82.129/hive</value>
        </property>
        <property>
                <name>javax.jdo.option.ConnectionDriverName</name>
                <value>com.mysql.cj.jdbc.Driver</value>
        </property>
        <property>
                <name>javax.jdo.option.ConnectionUserName</name>
                <value>root</value>
        </property>
        <property>
                <name>javax.jdo.option.ConnectionPassword</name>
                <value>afeibaili233</value>
        </property>
        <property>
                <name>hive.metastore.warehouse.dir</name>
                <value>/hive/warehouse</value>
        </property>
        <property>
                <!--指定hiveserver2的地址-->
                <name>hive.server2.thrift.bind.host</name>
                <value>master</value>
        </property>
        <property>
                <!--指定hiveserver2的端口-->
                <name>hive.server2.thrift.port</name>
                <value>10000</value>
        </property>
</configuration>
```

配置完hiveserver2就可以使用hive/bin下的**beeline**命令来打开自带的客户端,打开之后的样子

![img_7.png](image/1/img_7.png)

在>beeline中输入`!connect jdbc:hive2://master:10000`回车之后输入配置的用户名和密码

> 由于没开启密码校验,可以不输密码
> 注意ctrl+z会把进程暂停与后台而不是后台运行,后台运行hiveserver2使用`nohup bin/hiveserver2 > /dev/null &`
> 命令,/del/null文件代表空文件

通过idea数据库连接hive

![img_8.png](image/1/img_8.png)

选择hive数据库

![img_9.png](image/1/img_9.png)

配置完主机地址和用户名便可以连接了

![img_10.png](image/1/img_10.png)

### **metastore**服务

metastore服务的作用是为 Hive CLI 或者 Hiveserver2 提供元数据访问接口,
hiveserver2 是客户端连接的入口，而 metastore 是 hive 查询时的元数据提供者,
默认是开启的是嵌入式模式,开启独立运行模式可以减少元数据数据库的访问压力

配置**hive-site.xml**配置文件

```
<configuration>
        <property>
                <name>javax.jdo.option.ConnectionURL</name>
                <value>jdbc:mysql://123.56.82.129/hive</value>
        </property>
        <property>
                <name>javax.jdo.option.ConnectionDriverName</name>
                <value>com.mysql.cj.jdbc.Driver</value>
        </property>
        <property>
                <name>javax.jdo.option.ConnectionUserName</name>
                <value>root</value>
        </property>
        <property>
                <name>javax.jdo.option.ConnectionPassword</name>
                <value>afeibaili233</value>
        </property>
        <property>
                <name>hive.metastore.warehouse.dir</name>
                <value>/hive/warehouse</value>
        </property>
        <property>
                <name>hive.server2.thrift.bind.host</name>
                <value>master</value>
        </property>
        <property>
                <name>hive.server2.thrift.port</name>
                <value>10000</value>
        </property>
        <property>
                <!--配置metastore的通讯地址-->
                <name>hive.metastroe.uris</name>
                <value>thrift://master:9083</value>
        </property>
</configuration>
```

配置完之后便可以启动metastore服务了`nohup /hive/bin/hive --service metastore > /dev/null &`

## Spark环境搭建

Spark是一个离线批处理框架,使用`tar -zxvf /opt/software/spark-3.4.3-bin-hadoop3.tgz /opt/module/`命令解压spark到/opt/module/中

配置yarn的运行模式,打开spark的conf文件夹,更改配置文件名称**spark-env.sh.template -> spark-env.sh**

配置**spark-env.sh**文件,添加以下内容

```shell
# 配置yarn的配置文件路径
YARN_CONF_DIR=/opt/module/hadoop-3.3.1/etc/hadoop/
```

> yarn模式是基于yarn的无须其他配置

配置完之后并分发配置文件,尝试运行以下命令运行,提交一个运行例子

```
bin/spark-submit --class org.apache.spark.examples.SparkPi --master yarn /spark/examples/jars/spark-examples_2.12-3.4.3.jar  10
```

### 配置spark历史服务器

在spark的conf文件夹中的 **spark-defaults.conf.template重命名为spark-defaults.conf**
并添加以下内容

```
# 开启spark日志服务
spark.eventLog.enabled           true
# 配置hdfs日志文件路径,在hdfs文件系统中必须包含此文件夹
spark.eventLog.dir               hdfs://master:8020/spark
# 配置历史服务器主机节点和端口
spark.yarn.historyServer.address=hadoop102:18080
# 配置端口
spark.history.ui.port=18080
```

再次配置**spark-env.sh**文件,添加以下内容

```shell
YARN_CONF_DIR=/opt/module/hadoop-3.3.1/etc/hadoop/

# 配置spark历史服务选项
export SPARK_HISTORY_OPTS="
-Dspark.history.ui.port=18080
-Dspark.history.fs.logDirectory=hdfs://hadoop102:8020/spark
-Dspark.history.retainedApplications=30"
```

## Flink环境搭建

Flink是一个实时流处理框架

解压文件`sudo /commandsync.sh tar -zxvf /opt/software/flink-1.17.2-bin-scala_2.12.tgz -C /opt/module/`

修改集群配置,在flink/conf/中的**flink-conf.yaml**中修改以下配置

```yaml
# JobManager节点地址
jobmanager.rpc.address: master
jobmanager.bind-host: 0.0.0.0
rest.address: master
rest.bind-address: 0.0.0.0

# TaskManager节点地址
taskmanager.bind.host: 0.0.0.0
taskmanager.host: master
```

配置**workers**文件,添加以下内容

```
master
slave1
slave2
```

配置**masters**文件,修改为以下内容

```
master:8081
```

修改之后将配置文件分发下去,并修改**flink-conf.yaml**中的**taskmanager.host: master**
修改为**taskmanager.host: 当前主机地址名称**

输入`bin/start-cluster.sh`以Standalone模式启动flink集群

> Standalone模式,既是单机运行模式,不依赖其他资源,在真正运行中,建议使用yarn模式

### yarn模式部署

配置环境变量在 **/etc/profile** 中配置

```shell
# 前提配置HADOOP_HOME和PATH,配置hadoop配置文件路径
export HADOOP_CONF_DIR=${HADOOP_HOME}/etc/hadoop
# 配置hadoop classpath
export HADOOP_CLASSPATH=`hadoop classpath`
```

> \`hadoop classpath\` 反引号代表执行里面的内容,
> ${HADOOP_HOME}/etc/hadoop ${}代表读取里面的变量

基于yarn模式运行要先启动hadoop集群

在flink文件夹中输入`bin/yarn-session`启动yarn的会话模式,启动之后在yarn任务中会有一个任务正在进行

![img_12.png](image/1/img_12.png)

单作业模式可以直接启动flink集群,使用`flink run`命令

| 参数 | 描述          |
|----|-------------|
| -t | 指定运行模式      |
| -c | 指定全类名       |
| -d | 分离模式,防止终端阻塞 |
| -D | 指定yarnId    |

> 分离模式可能抛出一个检查类加载器的异常

```shell
bin/flink run -t yarn-per-job -c com.afeibaili.FlinkDemo FlinkDemo.jar
```

我们可以使用以下命令查看运行或取消运行作业

```shell
# 查看运行任务
bin/flink list -t yarn-per-job -Dyarn.application.id=application_XXX_XXX
# 根据运行任务Id取消任务
bin/flink cancel -t yarn-per-job -Dyarn.application.id=application_XXX_XXX <JobID>
```

应用模式和但作业模式类似,但有些区别

```shell
# 他们的区别 run -> run-application，yarn-per-job -> yarn-application
bin/flink run-application -t yarn-application -c com.afeibaili.FlinkDemo FlinkDemo.jar
```

> 单作业模式为每个任务都开启一个集群,开销比较大 应用程序模式只开启一个集群

还可以把包放到hdfs上进行运行,把flink的lib文件夹和plugins放到hdfs,通过以下参数命令进行运行

> lib和plugins放到一个文件夹中

```shell
# 前面两个参数就不多说了，-Dyarn.provided.lib.dirs 指定hdfs上库的位置，后面跟上全类名和jar包在hdfs上的位置
bin/flink run-application -t yarn-application -Dyarn.provided.lib.dirs="hdfs://master:8020/flink" -c com.afeibaili.FlinkDemo hdfs://master:8020/flink/jar/FlinkDemo.jar
```

### flink历史服务器配置

配置**flink/conf/flink-conf.yaml**文件,修改以下配置参数

注意创建hdfs服务器上的文件夹

```yaml
jobmanager.archive.fs.dir: hdfs://master:8020/flink/log
historyserver.web.address: master
historyserver.web.port: 8082
historyserver.archive.fs.dir: hdfs://master:8020/flink/log
historyserver.archive.fs.refresh-interval: 10000
```

![img_13.png](image/1/img_13.png)

使用`bin/historyserver.sh start`启动flink历史服务器,start更换stop为关闭历史服务器

## Kafka环境搭建

kafka是一款高吞吐,高可用消息队列,kafka有两种部署模式,一种是**Zookeeper**,另一种是**KRaft**部署模式

### zookeeper环境搭建

使用命令`tar -zxvf /opt/software/apache-zookeeper-3.8.1-bin.tar.gz -C /opt/module/`解压

解压之后创建数据文件夹在zookeeper文件夹中

![img_14.png](image/1/img_14.png)

在创建的文件夹中添加一个名为myid的文件里面只写一个1,代表节点1,可以通过命令快捷生成`echo "1" > /opt/module/apache-zookeeper-3.8.1-bin/data/myid`

打开zookeeper的配置文件夹,找到**zoo_sample.cfg**命名为**zoo.cfg**并配置以下内容

```
# 配置zookeeper的数据路径
dataDir=/opt/module/apache-zookeeper-3.8.1-bin/data

# 配置zookeeper的节点端口和选举端口(选举管理用的)
server.1=master:2888:3888
server.2=slave1:2888:3888
server.3=slave2:2888:3888
```

![img_15.png](image/1/img_15.png)

然后分发集群并修改`data/myid`文件的节点,master为1,slave1修改为2,slave2修改为3

修改完之后便可以启动zookeeper了,启动zookeeper需要在每台主机上都启动,写一个启动脚本会方便许多

```shell
#!/bin/bash

case $1 in
"start")
        echo "zookeeper开启"
        /commandsync.sh /opt/module/apache-zookeeper-3.8.1-bin/bin/zkServer.sh start
;;
"stop")
        echo "zookeeper关闭"
        /commandsync.sh /opt/module/apache-zookeeper-3.8.1-bin/bin/zkServer.sh stop
;;
"restart")
        echo "zookeeper关闭"
        /commandsync.sh /opt/module/apache-zookeeper-3.8.1-bin/bin/zkServer.sh stop
        echo "zookeeper开启"
        /commandsync.sh /opt/module/apache-zookeeper-3.8.1-bin/bin/zkServer.sh start
;;
"status")
        echo "zookeeper状态"
        /commandsync.sh /opt/module/apache-zookeeper-3.8.1-bin/bin/zkServer.sh status
;;
*)
        echo "请输入(start|stop|restart|status)"
;;
esac
```

启动命令`bin/zkServer.sh start`,关闭命令`bin/zkServer.sh stop`,更改为start status变成查看运行状态

### 基于zookeeper的kafka环境搭建

搭建好zookeeper后解压kafka安装包`tar -zxvf /opt/software/kafka_2.12-3.8.1.tgz -C /opt/module/`

解压之后开始配置**kafka/config/server.properties**文件,更改以下内容

```
# 更改id为zookeeper配置myid,统一起来
broker.id=1
# 更改对外暴露的地址端口
advertised.listeners=PLAINTEXT://master:9092
# 配置持久化路径
log.dirs=/opt/module/kafka_2.12-3.8.1/data
# 配置zookeeper连接
zookeeper.connect=master:2181,slave1:2181,slave2:2181
```

分发之后,更改其他节点的配置文件信息**broker.id**和**advertised.listeners**

启动kafka`bin/kafka-server-start.sh --daemon config/server.properties`命令和关闭kafka`bin/kafka-server-stop.sh`命令,
分别在集群启动kafka很麻烦,可以编写一个启动脚本,用于快速启动kafka

```shell
#!/bin/bash

case $1 in
"start")
        echo "zookeeper开启"
        /commandsync.sh /opt/module/apache-zookeeper-3.8.1-bin/bin/zkServer.sh start
        echo "kafka开启"
        /commandsync.sh /opt/module/kafka_2.12-3.8.1/bin/kafka-server-start.sh -daemon /opt/module/kafka_2.12-3.8.1/config/server.properties
;;
"stop")
        echo "kafka关闭"
        /commandsync.sh /opt/module/kafka_2.12-3.8.1/bin/kafka-server-stop.sh
        echo "zookeeper关闭"
        /commandsync.sh /opt/module/apache-zookeeper-3.8.1-bin/bin/zkServer.sh stop
;;
"restart")
        echo "kafka关闭"
        /commandsync.sh /opt/module/kafka_2.12-3.8.1/bin/kafka-server-stop.sh
        echo "zookeeper关闭"
        /commandsync.sh /opt/module/apache-zookeeper-3.8.1-bin/bin/zkServer.sh stop
                echo "zookeeper开启"
        /commandsync.sh /opt/module/apache-zookeeper-3.8.1-bin/bin/zkServer.sh start
        echo "kafka开启"
        /commandsync.sh /opt/module/kafka_2.12-3.8.1/bin/kafka-server-start.sh -daemon /opt/module/kafka_2.12-3.8.1/config/server.propertie
;;
*)
        echo "请输入(start|stop|restart)"
;;
esac
```

走到这里就是配置成功了可以用 IDEA BigData 插件来尝试连接

## Flume环境搭建

使用`wget https://dlcdn.apache.org/flume/1.11.0/apache-flume-1.11.0-bin.tar.gz `命令来获取Flume

获取之后进行解压`tar -zxvf apache-flume-1.11.0-bin.tar.gz -C /opt/module/`

通常运行flume都需要编写一个特定的配置文件用来运行时指定这个配置文件,下面是一个基于Socket数据源的文件编写

```
# 第一部分 声明变量,a1指的是agent在编写运行命令是传入a1
a1.sources = r1
a1.sinks = k1
a1.channels = c1

# 第二部分 指定输入源
a1.sources.r1.type = netcat
a1.sources.r1.bind = localhost
a1.sources.r1.port = 33393

# 第三部分 指定输出源
a1.sinks.k1.type = logger

# 第四部分 配置缓存
a1.channels.c1.type = memory
a1.channels.c1.capacity = 1000
a1.channels.c1.transactionCapacity = 100

# 第五部分 配置输入输出的缓存
a1.sources.r1.channels = c1
a1.sinks.k1.channel = c1
```

配置好之后使用`bin/flume-ng agent -c conf -f job/net-flume-logger.conf -n a1`命令进行启动,
再启动nc使用`nc localhost 33393`命令启动

> 如果不配置conf目录下的log4j.properties配置**flume.root.logger=INFO,LOGFILE,console**
> 会无法打印出控制台,只在日志中打印

## HBase环境搭建

可使用`wget https://dlcdn.apache.org/hbase/2.5.10/hbase-2.5.10-bin.tar.gz `命令来获取HBase

获取完之后进行解压`sudo tar -zxvf hbase-2.5.10-bin.tar.gz -C /opt/module/`

解压完之后配置hbase-env.sh,找到

```
# 将一下配置改成false 配置描述:是否使用hbase自带的zookeeper,我们给他改成自己的zookeeper
export HBASE_MANAGES_ZK=false
```

修改conf下的hbase-site.xml文件

```
   <property>
      <!--是否为集群模式-->
      <name>hbase.cluster.distributed</name>
      <value>true</value>
   </property>
   <property>
      <!--zookeeper的节点地址-->
      <name>hbase.zookeeper.quorum</name>
      <value>master,slave1,slave2</value>
   </property>
   <property>
      <!--配置主文件夹的位置-->
      <name>hbase.rootdir</name>
      <value>hdfs://master:8020/hbase</value>
   </property>
```

配置**regionservers**文件,用来配置hbase的集群节点

```
master
slave1
slave2
```

配置以上信息后就可以分发hbase到其他节点了

> 如果运行时报错可以尝试在报错中添加以下配置

```
<property>
   <name>hbase.wal.provider</name>
   <value>filesystem</value>
</property>
```

### 其他配置

配置高可用

在conf/下创建**backup-masters**文件,可以使用`echo slave1 > backup-masters`命令快速写入内容,再进行分发

# 语言学习

# 框架学习

## Hadoop

## MySql

## Hive

## HBase

## Flume

## Kafka

## Spark

## Flink

Apache Flink 是一个框架和分布式处理引擎，用于对无界和有界数据流进行有状态计算

### 窗口

Flink主要处理无界数据流,数据源源不断,窗口的出现就是将源源不断的数据流切分成一块一块的数据,
窗口包含两大类窗口一种是计数窗口(TimeWindow),另一种是时间窗口(CountWindow)

> 假如数据就是一个源源不断的水龙头🚰,窗口就是水龙头底下的水桶🪣,接完一桶再来一桶

Flink有多种窗口规则分别是**滚动窗口、滑动窗口、会话窗口、全局窗口**

滚动窗口: 数据首尾相接,不包含重复数据
滑动窗口: 数据有重叠,包含重复数据
会话窗口: 不会重叠,长度不固定,只能在时间窗口(TimeWindow)中出现
全局窗口: 窗口默认不会触发计算,需要自定义窗口触发器

### 窗口API

在窗口API中有两种窗口API一种是`windowAll()`一种是经过keyBy之后的`keyBy(...).window()`

window: 数据根据keyBy分区运行,每个key都开一个窗口独立运行
windowAll: 将传入进来的数据运行在一个分区上

```java
public static void main(String[] args) {
    StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
    ArrayList<String> strings = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        strings.add(i + "");
    }
    //使用windowAll()
    environment.fromCollection(strings).windowAll();
}
```

下面是WindowAll API

```java
    public static void main(String[] args) throws Exception {
    StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
    ArrayList<String> strings = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        strings.add(i + "");
    }
    DataStreamSource<String> streamSource = environment.fromCollection(strings);
//        streamSource.windowAll(SlidingProcessingTimeWindows.of(Time.seconds(10), Time.seconds(2))); 基于时间的滑动窗口
//        streamSource.windowAll(TumblingProcessingTimeWindows.of(Time.seconds(10))); 基于时间的滚动窗口
//        streamSource.windowAll(ProcessingTimeSessionWindows.withGap(Time.seconds(10))); 基于时间的会话窗口

//        streamSource.countWindowAll(2); 基于计数的滚动窗口
//        streamSource.countWindowAll(2,1); 基于计数的滑动窗口

    environment.execute();
}
```

在转换windowApi中调用这些方法`reduce()`、`aggregate()`、`apply()`、`process()`**可转回DataStream**

reduce方法

```java
public static void main(String[] args) throws Exception {
    //创建执行环境
    StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
    //模拟数据流
    ArrayList<String> strings = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        strings.add(i + "");
    }
    //获取DataSteam
    DataStreamSource<String> streamSource = environment.fromCollection(strings);
    //使用计数窗口转换为WindowStream并将窗口大小设置为2
    AllWindowedStream<String, GlobalWindow> countWindowAll = streamSource.countWindowAll(2);
    //进行reduce操作
    SingleOutputStreamOperator<String> reduce = countWindowAll.reduce(new ReduceFunction<String>() {
        @Override
        //value1、value2: 传进来的值 返回值: 返回一个String
        public String reduce(String value1, String value2) throws Exception {
            return Integer.parseInt(value1) + Integer.parseInt(value2) + "";
        }
    });
    //打印输出
    reduce.print();
    //执行
    environment.execute();
}
```

> 在大部分场景中使用reduce就可以做到了,但是使用aggregate方法可以指定输出类型

aggregate方法

```java
 public static void main(String[] args) throws Exception {
    StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
    ArrayList<String> strings = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        strings.add(i + "");
    }
    DataStreamSource<String> streamSource = environment.fromCollection(strings);
    AllWindowedStream<String, GlobalWindow> countWindowAll = streamSource.countWindowAll(5);
    //使用aggregate方法第一个泛型是输入类型，第二个泛型是累加器类型，第三个泛型是输出类型
    countWindowAll.aggregate(new AggregateFunction<String, Integer, Integer>() {
        @Override
        //创建累加器调用
        public Integer createAccumulator() {
            System.out.println("创建累加器");
            return 0;
        }

        @Override
        //传进来的值就会走这个方法
        public Integer add(String value, Integer accumulator) {
            System.out.println("相加");
            return Integer.parseInt(value) + accumulator;
        }

        @Override
        //返回结果时调用
        public Integer getResult(Integer accumulator) {
            System.out.println("返回结果");
            return accumulator;
        }

        @Override
        public Integer merge(Integer a, Integer b) {
            System.out.println("只有会话底层才会用到");
            return 0;
        }
    }).print();
    environment.execute();
}
```

全窗口函数

如果我们的要求需要窗口的上下文中的一些信息,聚合方法是做不到的,就需要窗口函数

```java
 public static void main(String[] args) throws Exception {
    StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
    ArrayList<String> strings = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        strings.add(i + "");
    }
    DataStreamSource<String> streamSource = environment.fromCollection(strings);
    AllWindowedStream<String, GlobalWindow> countWindowAll = streamSource.countWindowAll(5);
    //过时的窗口方法
    countWindowAll.apply(new AllWindowFunction<String, Integer, GlobalWindow>() {
        @Override
        public void apply(GlobalWindow window, Iterable<String> values, Collector<Integer> out) throws Exception {
            values.forEach(value -> out.collect(Integer.parseInt(value)));
        }
    }).print();
    environment.execute();
}
```

> 使用`process()`功能比apply更加齐全

```java
 public static void main(String[] args) throws Exception {
    StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
    ArrayList<String> strings = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        strings.add(i + "");
    }
    DataStreamSource<String> streamSource = environment.fromCollection(strings);
    AllWindowedStream<String, GlobalWindow> countWindowAll = streamSource.countWindowAll(5);
    countWindowAll.process(new ProcessAllWindowFunction<String, String, GlobalWindow>() {
        /**
         * 如果是keyBy之后的window会有个key的形参
         * @param context 窗口的上下文
         * @param elements 数据的集合
         * @param out 采集器对象
         */
        @Override
        public void process(ProcessAllWindowFunction<String, String, GlobalWindow>.Context context, Iterable<String> elements, Collector<String> out) throws Exception {
            int count = 0;
            for (String element : elements) {
                count += Integer.parseInt(element);
            }
            out.collect(count + ":包含这些数据=>" + elements);
        }
    }).print();
    environment.execute();
}
```

聚合函数占用内存小,但是拿不到上下文,使用全窗口函数虽然可以使用上下文,
但是占用内存大,我们可以使用全窗口函数结合聚合函数来使用他们的优点

```java
public static void main(String[] args) throws Exception {
    StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
    ArrayList<String> strings = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        strings.add(i + "");
    }
    DataStreamSource<String> streamSource = environment.fromCollection(strings);
    AllWindowedStream<String, GlobalWindow> countWindowAll = streamSource.countWindowAll(5);
    //使用aggregate(new AggregateFunction(),new ProcessAllWindowFunction)来实现他们用于更强灵活的运算
    countWindowAll.aggregate(new AggregateFunction<String, Integer, Integer>() {
        @Override
        public Integer createAccumulator() {
            return 0;
        }

        @Override
        public Integer add(String value, Integer accumulator) {
            return 0;
        }

        @Override
        public Integer getResult(Integer accumulator) {
            return 0;
        }

        @Override
        public Integer merge(Integer a, Integer b) {
            return 0;
        }
    }, new ProcessAllWindowFunction<Integer, Integer, GlobalWindow>() {
        @Override
        public void process(ProcessAllWindowFunction<Integer, Integer, GlobalWindow>.Context context, Iterable<Integer> elements, Collector<Integer> out) throws Exception {

        }
    });
    environment.execute();
}
```

> 使用全窗口执行函数,进行聚合之后调用全窗口函数,所以导致只有一个元素,但是拿到了上下文

### 水位线

在数据流中有两个时间戳,分别是事件时间和处理时间,事件时间指的是数据的生产时间,
而处理时间指的是数据的进行处理操作的时间

水位线和时间语义是息息相关的,在窗口的处理过程中,我们可以基于数据的时间戳,自定义一个逻辑时钟
逻辑时钟不会跟着时间推动,他是根据数据的时间进行推动

在flink中用来衡量事件时间的标记,就被称为水位线(Watermark)

每条数据并不都会生成水位线,而是每小段时间生成水位线,
有序的状态下就像以下这样子的,水位线在有序的情况下逐渐增大

> => |17 15 |14 13 12 |11 10 9 8 |7 =>

在实际的过程中,数据接收可能会发生延迟,导致顺序接收混乱,这就是**乱序数据**,乱序数据的处理方案很简单,
水位线只会前进不会后退,就像时间一样,假设以下每条数据都包含水位线,在插入水位线中判断是否比前面的水位线大,
如果大再插入,否者就停止插入水位线直到比当前水位线大的水位线再插入

> => 15 |17 13 |14 |12 10 |11 |9 |8 |7 =>

在乱序和数据量大的情况下,也可以生成水位线,在生成水位线之前判断传进来的时间戳
,找到最大的时间戳并生成水位线

> => |=17 15 17 |=14 13 14 12 |=11 11 9 =>

**迟到的数据**加乱序也可以处理,为了让窗口能够收集到正确的数据,我们也可以等待一段时间用来收集迟到的数据,
比如减去两秒,等待两秒后再进行处理,假如数据走到了九秒,水位线查找的最大数据也是九秒,
通过减去两秒那么水位线指向的就是七秒,等待九秒内没有收集到的数据等待两秒

> => |=(17-2) 15 17 |=14-2 13 14 12 |=(11-2) 11 9 =>

> 注意每个窗口会找到自己的数据,并不会减去两秒之后处理不属于这两秒的数据

在DataStreamAPI中有一个生成水位线的方法`assignTimestampsAndWatermarks()`

```java
public static void main(String[] args) throws Exception {
    StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
    ArrayList<Integer> strings = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        strings.add(i);
    }
    DataStreamSource<Integer> streamSource = environment.fromCollection(strings);
    //定义水位线
    WatermarkStrategy<Integer> integerWatermarkStrategy = WatermarkStrategy
            //单调递增水位线
            .<Integer>forMonotonousTimestamps()
            //创建时间戳分配器，从分配器中提取数据
            .withTimestampAssigner(new SerializableTimestampAssigner<Integer>() {
                @Override
                public long extractTimestamp(Integer element, long recordTimestamp) {
                    System.out.println("Element: " + element + ", Event Time: " + element);
                    //返回事件时间
                    return element * 1000L;
                }
            });
    //配置水位线
    streamSource.assignTimestampsAndWatermarks(integerWatermarkStrategy)
            //使用水位线时必须要使用事件时间窗口
            .windowAll(TumblingEventTimeWindows.of(Time.seconds(1)))
            .reduce(Integer::sum)
            .print();
    environment.execute();
}
```

乱序的水位线API

```java
public static void main(String[] args) throws Exception {
    StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
    ArrayList<Integer> strings = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        strings.add(i);
    }
    DataStreamSource<Integer> streamSource = environment.fromCollection(strings);
    //设置等待时间为两秒
    WatermarkStrategy<Integer> integerWatermarkStrategy = WatermarkStrategy.<Integer>forBoundedOutOfOrderness(Duration.ofSeconds(2))
            .withTimestampAssigner(new SerializableTimestampAssigner<Integer>() {
                @Override
                public long extractTimestamp(Integer element, long recordTimestamp) {
                    return element * 1000L;
                }
            });
    streamSource.assignTimestampsAndWatermarks(integerWatermarkStrategy)
            .windowAll(TumblingEventTimeWindows.of(Time.seconds(5)))
            .reduce(Integer::sum)
            .print();
    environment.execute();
}
```