# Spark 编程基础

---

## Spark 部分知识要点

### Spark 简介

- 基于`Scala`语言开发

- 能够良好的兼容`Python`

- 兼容`Hadoop`接口，进一步扩展`Map-Reduce`操作

- 强并发性，充分利用多线程资源

- 可基于内存操作，在内存中完成多步数据处理，减少IO开销

- `Spark Tachyon`：Spark基于内存的文件存储系统

- `HDFS`：磁盘文件分布式存储系统

- `Driver`：进程调度管理 -> `Cluster Manager`：计算资源管理（YARN，Mesos）-> 
`Working Nodes`：任务执行单元

- 利用流水线`Pipeline`方式管理数据处理流程，并针对RDD生成依赖图，提供潜在优化方式，避免数据频繁落地

- 基本数据容器RDD无法修改，为只读属性，每次RDD内容更新通过返回新建RDD解决，此机制结合RDD依赖图，
构成历史日志，为Spark提供了良好的追溯能力，由此提高容错能力，减少犯错时计算开销

- 根据依赖图，将`Transformation`和`Action`操作分别join，尽可能保证需要全RDD集合的`Action`操作执行
之前各计算资源的利用率最大化

### Spark 部署

- 启动命令：`spark-shell --master local[*]`：利用用户给出的进程数启动当前Spark-shell，默认采用
与CPU线程数相同的线程数量，为单机节点启动模式，非Cluster集群

- 于`spark://localhost:7077`查看单机Spark-shell工作状态

- 利用`:quit`退出当前Spark运行环境

- `Yarn-Client`：YARN客户端，在此模式下启动，Client将作为任务管理者，将任务发布至集群上执行，适用于
小规模代码调试，Client在任务执行过程中不能关闭或退出

- `Yarn-Cluster`：YARN集群运行模式，将任务发布至集群的Driver，由其控制任务流程，为生产代码部署方式，
任务由Client推送完毕后，Client即可退出或关闭

- `spark-shell --master local --jars [JarSource]/sample.jar`：向Spark运行环境中手动加入依赖，可以
通过将Jar包放入Spark路径的lib下方便快速定位所需Jar包

- `spark-submit`：用于提交打包程序到Spark中运行，参数有：
    - `--class <main-class>`：程序主类，如"Packages.SimpleApp"(编译得到的类)
    - `--master <master-url>`：采用local，yarn-client，yarn-cluster或mesos模式
    - `--deploy-mode <deploy-mode>`： 部署模式
    - `<jars>`：运行需求的其余依赖jar包
    - `<arguments>`：传入应用程序的参数args
    - 可使用的工具如：Maven，SBT 等打包编译工具
    
``` 
在SBT工具启动目录下添加sbt脚本：vim ./sbt

添加以下内容
#!/bin/bash SBT_OPTS="-Xms512M -Xmx1563M -Xss1M -XX:+CMSC;assUnloadingEnabled -XX:MaxPermSize=256M" 
java $SBT_OPTS -jar `dirname $0`/sbt-launch.jar "$@"

增加可执行权限：chmod u+x ./sbt

在应用程序目录下（APP目录下）构建打包脚本：vim ./[AppDir]/packApp.sbt

在文件中给出本项目的相关依赖和内容：
name := "Simple App"
version "= "1.0"
scalaVersion := "2.11.12"
libraryDependencies += "org.apache.spark" %% "spark-core" % "2.4.1"
多个lib替代版本号
libraryDependencies ++= Seq(
  "org,json4s" %% "json4s-native" % "3.2.10",
  "org.json4s" %% "json4s-jsckson" % "3.2.10"
)
规则："Group ID" %% "Artifact ID" % "Version"; 使用%%表示不指定版本号，可替换，使用%表示版本号定死

文件需要具有正确目录结构才可被打包编译：
./src/main/scala/SimpleApp.scala
./packApp.sbt

在SBT文件目录下进行打包： /[SBTDir]/sbt package

利用Maven进行代码打包：vim ./[AppDir]/pom.xml

添加内容：
<project>
    <groupId>cn.edu.xmu</groupId>
    <artifactId>Simple-Project</artifactId>
    <modelVersion>4.00</modelVersion>
    <name>Simple App</name>
    <packaging>jar</packaging>
    <version>1.0</version>
    <repositories>
        <repository>
        <id>jboss</id>
            <name>JBoss Repository</name>
            <url>http://repository.jboss.com/maven2/</url>
        </repository>
    </repositories>
    <dependencies>
        <dependency>
            <groupId>org.apache.spark</groupId>
            <artifactId>spark-core_2.11</artifactId>
            <version>2.1.4</version>
        </dependency>
    </dependencies>
<build>
    <sourceDirectory>[codeSource]</sourceDirectory>
    <plugins>
        <plugin>
            <groupId>org.scala-tools</groupId>
            <artifactId>maven-scala-plugin</artifactId>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <scalaVersion>2.11.14</scalaVersion>
                <args>
                    <arg>-target:jvm-1.5</arg>
                </args>
            </configuration>
        </plugin>
    <plugins>
</build>
</project>

进入应用程序目录下，利用：[MavenDir]/mvn package 进行打包
```

### Spark Hadoop 环境搭建

> 此处于Windows10上使用版本为：`spark-2.4.1-bin-hadoop2.7`以及`hadoop-2.7.1`，搭配`java_11.0.4`与
`scala_2.11.12`使用

- Spark 及 Hadoop 配置

    - Spark 安装：Windows
        - 下载：spark-2.4.1-bin-hadoop2.7.tar.gz 代表本版本中直接集成链接Hadoop2.7系列所需框架
        - 将其解压至任意英文路径目录下，若解压软件报错则直接使用命令行解压（可用`WSL`执行linux命令）
        - 将安装目录下的`bin`文件目录添加到path系统变量中

    - Hadoop 安装：Windows
        - 下载：hadoop-2.7.1.tar.gz
        - 将其解压至任意英文路径目录下，若解压软件报错则直接使用命令行解压（可用`WSL`执行linux命令）
        - 下载Hadoop版本对应的`winutils`，可于github上获取，放入Hadoop安装目录的`bin`文件夹下
        - 在系统环境变量中添加`HADOOP_HOME`，并将`%HADOOP_HOME%\bin;`添加到系统path中

- 配置`slaves`文件：在Spark安装目录下有`slaves.template`配置示例文件

``` 
拷贝至slaves：
cp [SparkDir]/conf/salves.template [SparkDir]/conf/salves

将其中默认内容localhost更改到目标salve节点的网络位置
[Slave1NetworkPosition]
[Slave2NetworkPosition]
[Slave3NetworkPosition]
```

- 配置是`spark-env.sh`文件：在Spark目录下有`spark-env.sh.template`示例文件

``` 
拷贝至spark-env.sh：
cp [SparkDir]/conf/spark-env.sh.template [SparkDir]/conf/spark-env.sh

编辑并添加：Spark与Hadoop的挂接；Hadoop配置目录说明；MasterNode主机IP地址
export SPARK_DIST_CLASSPATH=$([HadoopDir]/bin/hadoopclasspath)
export HADOOP_CONF_DIR=[HadoopDir]/etc/hadoop
export SPARK_MASTER_IP=[MasterNetworkPosition]
```

- 将Master节点主机上的Spark文件夹复制到各个Slave节点上

- 启动Hadoop集群：`sbin/start-all.sh` （于Hadoop目录下）

- 启动Master节点：`sbin/start-master.sh` （于Spark目录下）

- 启动Slave节点：`sbin/start-slaves.sh` （于Master节点Spark目录下）

- 顺序退出三者：`sbin/stop-master.sh`；`sbin/stop-slaves.sh`；`sbin/stop-all.sh`；

- 可于Master主机上：`http://master:8080`查看集群运行状态


### HBase，Hive 与 SparkSQL

- HBase版本选择：1.4.0
    - 将HBase目录下`bin`加入系统变量path中
    - 启动Hadoop：`start-all.sh`后启动HBase：`start-hbase.sh`；`hbase shell`

- Hive 与 Spark SQL
    - 基于数据类型`DataFrame`，可以理解为一个具有表结构的RDD
    - `DataFrame`数据类型适配`MLlib`，可直接用于Spark上的机器学习算法API
    - 支持Scala，Java与Python
    - 两者能够将SQL语句通过其语法树，提取查询块并优化整体效率
        - Hive根据Hadoop的MapReduce过程专门优化从HDFS中取数据的流程效率
        - SparkSQL针对Spark进行了多线程适配，与Hive的优化逻辑在语法树优化后出现差异

---

## RDD 编程

### RDD 创建

- 文件系统加载

    - `local`：本地文件系统
    
    - `hdfs`：分布式多分区文件系统
    
    - `cloud`：云端文件

```
val rdd = sc.textFile(filePath)
```

- 程序内存中已有的集合对象如：`list`/`map` 转化生成

```
val rdd = sc,parallelize(collection)
```

### RDD Transformation 操作

> 消极响应策略，在Action类RDD操作发生前只会被记录，不会被执行，在触发Action操作时，
会汇总全部记录未执行Transformation操作，统一进行优化处理后执行，将可以在不同RDD分片
上预处理的功能进行分析优化处理，以提高整体运行效率，减少IO开销。

- `rdd.filter(Function)`：迭代遍历RDD中全部数据，按照传入函数要求进行过滤

``` 
rdd.filter(item => item.contains("Spark"))
```

- `rdd.map(Function)`：遍历RDD中全部数据，并对每条数据根据传入参数进行修改

``` 
rdd.map(item => item.toInt + 10)
```

- `rdd.flatmap(Function)`：先拆后并，首先根据传入函数将RDD中的数据根据函数原则拆解，
得到一系列数据条目，之后遍历所有数据，并将其展开，并入一维，构建`collection`

``` 
/*
    RDD中原始数据：{"1::4::7", "6::7::9::4"}
    拆解后数据：{{"1", "4", "7"}, {"6", "7", "9", "4"}}
    最终合并后数据：{"1", "4", "7", "6", "7", "9", "4"}
 */
rdd.flatmap(items => items.split("::"))
```

- `rdd.groupByKey()`：应用于数据内容结构为<K, V>对的RDD，将Key相同的Value构建list，
并入一个Key下，返回新数据形式为：<K, Iterable>形式

``` 
/*
    RDD中原始数据：{(1, 5), (1, 7), (4, 3), (3, 4)}
    合并后数据：{(1, {5, 7}), (4, {3}), (3, {4})}
 */
rdd.groupByKey()
```

- `rdd.reduceByKey(Function)`：有`reduceLeft`与`reduceRight`两种Reduce方法，在执行
`rdd.groupByKey()`的前提下进行规约操作，针对得到的新Value按要求进行合并，默认`reduceLeft`。

> 在真正执行Action类操作之前，本方法不会被真正触发，它的执行结果可以被限制于拆解的各个RDD分区
存储之中，在真正需要涉及全体数据处理的Action操作触发之前不需要立刻执行，可在任务拆解中被优化。

``` 
/*
    RDD中原始数据：{(1, 5), (1, 7), (4, 3), (3, 4)}
    合并后数据：{(1, 12), (4, 3), (3, 4)}
 */
rdd.reduceByKey(_ + _)
rdd.reduceByKey(lambda left, right : left + right)
```

### RDD Action 操作

> Action类操作涉及了整个RDD全局数据，单分片上的数据信息无法完成该任务，遇到Action操作时，
全部历史保留操作（Transformation）将被弹栈，Spark自动优化处理命令序列并执行。也就是说，
在遇见Action操作之前，如果Transformation操作有误，其并不会被即刻抛出，而是等待Action执行后才会发生。

- `rdd.count()`：返回RDD中全部条目数量

- `rdd.collect()`：整合全部RDD分片，获取完整RDD

- `rdd.first()`：获取完整RDD中的第一条数据

- `rdd.take(Number)`：获取完整RDD的前`Number`条数据

- `rdd.reduce(Function)`：进行规约操作，默认`reduceLeft`

``` 
rdd.reduce(_ * _)
rdd.reduceLeft(lambda left, right : right + left)
rdd.reduceRight(right - left)
```

- `rdd.foreach(Function)`：Action中最常用的迭代遍历操作，对每个数据条目执行传入方法

``` 
rdd.foreach(println)
```

- `rdd.sortBy(dim, reverse)`：可自选维度的RDD自动化排序定义

``` 
# 根据每组数据第四维，升序排序
rdd.sortBy(_._4, false)
```

- `rdd.join(rdd2)`：将两个RDD根据Key合并为一个并返回，数据构成<K, (Value1, Value2)>对

### RDD 持久化

> 持久化处理将当前RDD放入内存之中，加速处理的同时减少对于磁盘IO的开销占用，缺点为占用大量内存。

- `rdd.cache()`：指定该RDD在第一次遇见Action操作后，会被持久化处理

``` 
/*
    以下命令等效
 */
rdd.cache()
rdd.presist(MEMORY_ONLY)
rdd.presisit()
```

- `rdd.unpresist()`：将当前RDD从内存中移除

### RDD 重分区

> 将RDD重新拆为数个分区，以便于多线程处理或HDFS分布式数据分片存储，因此我们总希望分片与线程数目对应。

- 以分片形式读入RDD，通过参数中指定分片数量

``` 
val rdd = sc.textFile(FileSource, PartitionNumber)
```

- `rdd.glom().collect()`：返回该RDD当前分片数目

- `rdd.repartition(Number)`：重定义当前RDD的分片数目

- 分区方法可自定义，需要定义一个Partition方法：(Scala中定义：[MyPartitioner](./src/RDDSample.scala))

```
# Python
# 假定分为10份
num_of_bins = 10
file_source = "hdfs://localhost:9000/user/[FileSource]"

# 按照RDD中当前数据条目的Key值进行分片
def MyPartitioner(part_key):
    return part_key % num_of_bins

def rePartRdd():
    conf = SparkConf().setAppName("MyPartitioner").setMaster("local")
    sc = SparkContext(conf)
    # 构建RDD，初始化为5个分区
    data = sc.parallelize(range(40), 5)
    # 将各数据映射为<K, 1>对：partitionBy方法依靠Key值重新分片
    data.map(lambda item : (item, 1))\
        # 将目标分区数量和自定义分区方法传入，传入函数处理后返回结果相同的项放入同一分片
        .partitionBy(num_of_bins, MyPartitioner)\
        # 去除不需要的部分，仅留下Key
        .map(lambda item : item[0])\
        .saveAsTextFile(file_source)

if __name__ == '__main__':
    rePartRdd()
```

### RDD <K, V> 形式创建与利用技巧

- 利用`rdd.flatmap(Function)`将RDD拆分后，使用`rdd.map((KEY, VALUE))`进行映射，通常我们将Key映射
为`_`，以方便利用`rdd.groupByKey()`，`rdd.reduceByKey()`与`rdd.sortByKey()`等功能

- `rdd.groupByKey()`：根据Key进行分组整理

- `rdd.reduceByKey()`：根据Key整理所得的各分组进行规约处理

- `rdd.sortByKey()`：根据Key值进行排序，可决定升降规则

- `rdd.keys`与`rdd.values`：返回该RDD的Key或者Value的RDD集合

- `rdd.mapValues(Function)`：对每个Key下的Value执行传入操作

``` 
rdd.mapValues(value => value.toInt + 10)
```

- `mapValues`，`sortByKey`，`reduceByKey`与`groupByKey`等在执行时，有这专门的指定对象，因此为其编写
lambda表达式时，需要注意使用场景

---

## 各类文件的读写要点

> 本部分样例详细参考代码在：[RDDSample](./src/RDDSample.scala)

### 分区文件

- 在使用`sc.saveAsTextFile([FileSource])`等命令时，其返回信息通过`part-000X`指出当且文件被写入的
分区，若为0000代表整体文件不采用分区存储机制

### JSON文件操作

- 文件的每一行都会被作为RDD的一个`element`读入，也就是说以一般的文本流形式载入，通过`JSON.parseFULL(string)`
来对每一行进行JSON的数据翻译，一般在`match`-`case`语句中利用`Some`返回合乎Json规则的一系列数据

### 读写HBase数据

- HBase利用BigTable，在写入后数据不再更新，只会不断添加新数据和读取数据，通常时间戳也为其主键之一

- 利用四维度定位方式：行键（Main Key），列族（大属性，如Info）， 列中限定符（列内属性，如Name属于
Info），时间戳（TimeStep）

---

## RDD 部分高级操作

> 此部分列出一些RDD的常用较复杂操作，多数实现方式已经在：[RDDSample](./src/RDDSample.scala) 中的
几个样例中编码展示用法，

- 自定义分片Hash，`MyPartitioner`：用户自定义RDD内容重分片

- 自定义排序方法，可用于`sortByKey()`，如实现二次排序等功能

- `rdd.keyBy(item => item._1)`：利用指定的维度作为Key，将RDD中各数据构建成<K, V>对形式

---

## DataFrame 编程

> 其类型可理解为带有表结构的RDD数据块，即每个数据根据其在其条目中存在位置和类型不同具有不同实际含义

### 创建DataFrame

- 从已有文件/内存数据中提取

    - 从文本读入指定格式的数据：
    ``` 
    val jsonData = sc.read.json(fileSource)
    val parquetData = sc.read.parquet(fileSource)
    val csvData = sc.read.csv(fileSource)
    ```
    
    - 通过反射方式获取指定结构的数据,调用`toDF`转化为DataFrame
    
    - 通过对已有的RDD执行Row转化放入定义结构中，调用`sc.createDataFrame(RowRDD, Schema)`方法生成DataFrame

- 从数据库中获取
    
    - 通过JDBC对数据库进行访问
    
    - 需要定义表结构
    
    - 需求定义`Properties`标明数据库各项参数

### DataFrame 基本数据处理和使用

- 数据选取：Select
``` 
targetData.select("name", "age").show()
```

- 数据过滤：Filter
``` 
jsonData.filter(jsonData("age") + 1).show()
```

- 数据聚合：GroupBy
``` 
jsonData.groupBy("age").count().show()
```

- 数据排序：Sort
``` 
jsonData.sort(jsonData("age").desc).show()
```

- 数据指定格式输出：WriteFormat
``` 
targetData.select("name", "age").write.format("csv").save(targetSource)
```

---

## Spark Streaming 流数据处理

### Spark Streaming 简介

- 订阅推送信息流：无需主动发出请求确认是否有新数据流入，如果产生新数据，则数据会被自动推入Spark

- 兼容多种数据源如：
    - Kafka
    - Flume
    - HDFS
    - TCP Socket
    
- `Batch Training`方式，以时间为度量单位切割创建批处理数据（使Spark Streaming的实时响应能力降低，
响应缓慢问题在Structure Streaming中得到缓解）

- 每个`Batch`的数据实际上本质抽象为`RDD`，向上提高到`DStream`序列，实际执行与`RDD`相同性质的`Transformation`
与`Action`操作

- `DStream`通过持续监听传入流数据生成，在设定后会根据要求不断构成新`DStream`传入处理流程中

### DStream 监听的一些方法

- 消息订阅服务中，利用`Kafka`作为Spark Streaming数据源时需要`Zookeeper`服务支持
    - 使用`Kafka`版本为：`kafka_2.11-2.4.1`
    - 自带ZK设置为启动：修改`config/zookeeperpropeties`配置文件
    
- Spark Streaming部分代码示例：[Spark Streaming](./src/SparkStreamingSample.sc)

### DStream 无状态转换操作

- 与RDD相比，DStream的`Transformation`类别操作可以直接执行`Repartition`进行DS重分区而不需要触发
`Action`，由于其本身就是根据时间戳进行分片，因此重分区实际上类似于Join合并操作

- 其与RDD的操作分类基本类似：
    - `map(Funtion)`
    - `flatMap(Function)`
    - `filter(Function)`
    - `repartition(numPartitions)`
    - `reduce(Function)`
    - `count()`
    - `union(otherStream)`：两者取并集
    - `countByValue()`：对元素类型为K（值为K）的DStream进行数量统计
    - `reduceByKey(Function, [numTasks])`：Key相同聚合/规约
    - `join(otherStream, [numTasks])`：Key相同的合并：<K, <V1, V2>>
    - `cogroup(otherStream, [numTasks])`：Key相同的重构为：<K, Seq(V1), Seq(V2)>元组形式
    - `transform(Function)`：作用在RDD-to-RDD函数转化，使得到结果为DStream
    
### DStream 有状态转换操作

- 相当于`Action`类别转换，代表不仅考虑当前DStream，还考虑之前的数个DStreams加入分析

- 滑动窗口转换：每个窗口包含一段时间序列，即每次包含一系列连续时刻上不同的DStreams
    - 需要定义窗口长度和间隔：`(windowLength, sildeInterval)`
    - `window(windowLength, sildeInterval)`：滑动窗口定义
    - `countByWindow(windowLength, sildeInterval)`：将count范围变为指定窗口
    - `reduceByWindow(Funtion, windowLength, sildeInterval)`：针对一个窗口内全部DStreams进行规约
    - `reduceByKeyAndWindow(Function, windowLength, sildeInterval, [numTasks])`
    - `reduceByKeyAndWindow(Function, invFunction, windowLength, sildeInterval, [numTasks])`：
    相比上述添加了逆函数，能够帮助提高程序整体效率，在滑动窗口滑动时有效减少需要计算的新进入成员量
    （保留共有部分结果/部分结果可还原），由此帮助增量计算

- `updateStateByKey`操作：实现跨批次的结果维护，执行多批次累加任务
    - 实现`updateFunc(values : Seq[Int], state : Option[Int])`方法，从而累加更新已知DS内容
    
---

## Structured Streaming 流数据处理

### Structured Streaming 简介

- 基于`DataFrame`而非`RDD`，因此每个数据帧具有相应结构，各数据元素本身具有相应含义

- 在内存中根据数据结构不同维护不同数据表，新进入数据帧即作为新数据插入对应表中，由此加速数据处理速度

- `MLlib`操作也主要基于`DataFrame`数据形式开展，使得Structured Streaming能够更好的契合数据分析工作

- 秉持流数据会且仅会被分析处理一次的逻辑构建

- 相对于老版Spark Streaming大幅降低了延迟，能够应对毫秒级别的响应要求

### Structured Streaming 策略

- 在内存中采用`unbounded table`，新加入的数据由于为`DataFrame`形式，可以直接新增进入表中

- 三类数据输出策略：
    - Complete Mode：完整输出，将内存中所有数据一次性写入磁盘
    - Append Mode：根据数据进入时间，只将新加入数据写入磁盘
    - Update Mode：每次更新结果集，只有被更新的结果写入磁盘（Spark2.0暂不支持）

- `watermark`：延时消息处理机制，判定一个延迟到达消息是否在当前数据流中仍旧具有价值
    - 令其真实落入推流的最晚数据项发生时间为`t`，`watermark`的阈值为`n`
    - 若其真实发生时间晚于：`t-n`，则代表该数据仍为失去时效性，应当被考虑补入之前计算（Update）
    - 如若已经失效，则将其舍去，不纳入计算
    - 在Append模式下，每个计算结果输出会根据推流最晚项目落入实际时间，延迟`watermark`个时间单位后再输出
    因此相比尽快更新的Update模式，会出现更高的延迟，但能够应对部分不允许更新操作的数据池
    - 在Complete模式下，`watermark`即为无穷：不会有数据被舍弃

    ```
    val windowedCounts = words.groupBy(
      window($"timestamp", "10 minutes", "5 minutes"),
      $"word"
    ).count()
    ```

### Structured Streaming 流程

- 创建SparkSession实例

- 创建DataFrame，利用其所具有的Row结构形容每条数据的形式

- 通过DataFrame操作分析求取所需数据

- 创建`StreamingQuery`并开启流查询

- 利用`StreamingQuery.awaitTermination()`等待查询结束

- 流程本身和Spark Streaming类似，主要更改为底部逻辑结构，其重新定义了Spark的流计算：[示例代码](./src/StructuredStreamingSample.sc)

---

## MLlib Spark 机器学习API

### MLlib简介

- 与一般的机器学习框架相比，MLlib目标为集群上的数据训练，要应对超大规模的数据样本进行模型构建（注意，
与多节点分布式的构建大规模NN有所区别）

- 提供了部分机器学习算法的分布式实现，仅需要对数据正确预处理和封装定义，并传入对应API即可

- ML流：`pipeline`工具，流程化定义整体机器学习过程

- Spark中，`mllib`为老版，基于RDD的抽象；`ml`库基于DataFrame；注意依赖不同实现抽象数据基准不同
    - SparkSQL同样基于DataFrame，为HBase中的文件管理提供便利

### MLlib 工作流程

- Transformer：数据转换器，用于表示和存储模型结果，将输入数据转化为模型学习后的输出数据
    - 流程：Data -> Input(DataFrame) -> Transformer -> Output(DataFrame)

- Estimator：算法评估器，相当于具体Algorithm的评测器，求取当前模型的Loss
    - 流程：Data -> Input(DataFrame) -> Estimator(fit()) -> Loss

- Pipeline：工程流水线，用于定义整体学习流程，每个`Pipeline stage`为一个`Estimator`或者`Transformer`
    - 从更高抽象的角度看来，一个完整的Pipeline也相当于一个Model，输出模型的训练结果
    - 模型示例流程：InputText -> Tokenizer(Transformer) -> HashingTF(Transformer) -> Algorithm(Estimator)
    -> OutPutResults (整体构成一个完整的Model)

- 示例代码：[MLlibSample](./src/MLlibSample.sc)

---

## Spark GraphX 图结构模型处理

### Spark GraphX 简介

- 图处理技术主要包括图数据库及其相关操作，图数据分析以及数据可视化
    - Spark GraphX 侧重于图数据分析
    
- Spark GraphX引入了属性图，其与RDD一样，创建后只读且不可更改，如发生变动需新建属性图，但好在其能够将未受到
影响的部分直接在新图中重用，由此大量减小了图分析过程中的计算资源开销
    - 属性图作为有向多重图，由节点`Vertices`和边`Edges`构成，支持平行边（非Simple Graph）
    
    ``` 
    class Graph[VD, ED] {
      val vertices: VertexRDD[VD]
      val edges: EdgeRDD[ED]
    }
    ```
    
- Spark GraphX 能够完成对定义的节点，边及两者构成的全部三元组实现遍历

- Spark GraphX 能够直接反映出各节点的`in degree`与`out degree`等属性

- Spark GraphX 实现了一些常用图算法如：`Page Rank with dump factor`

- 参考资料：[Spark入门教程（Scala版）](http://dblab.xmu.edu.cn/blog/spark/)