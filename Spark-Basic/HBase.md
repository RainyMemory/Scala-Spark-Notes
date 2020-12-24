# HBase 简单操作

---

## 启动 HBase

`需要切换至hbase/bin目录下，或将hbase命令加入系统环境变量中`

- 启动 HBase 服务

```
start-hbase.sh
```

- 启动 HBase Shell

```
hbase shell
```

## 创建列族信息

- 停用/清除无用表格

```
disable `student`
drop 'student'
```

- 创建表

```
create `student`, 'info'
```

- 新建数据

```
put 'student','1','info:name','Ryan'
put 'student','1','info:gender','Male'
put 'student','1','info:age','24'
```

## 配置并与Spark协作

- 引入依赖jars

```
cp [path]/hbase/lib/hbase*.jar [path]/spark/jars
cp [path]/hbase/lib/guava-12.0.1.jar [path]/spark/jars
cp [path]/hbase/lib/htrace-core-3.1.0-incubating.jar [path]/spark/jars
cp [path]/hbase/lib/protobuf-java-2.5.0.jar [path]/spark/jars
```