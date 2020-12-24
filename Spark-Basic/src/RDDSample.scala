import org.apache.hadoop.hbase._
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.{HashPartitioner, Partitioner, SparkConf, SparkContext}

import scala.util.parsing.json.JSON

// --------------------RDD各创建方式以及其基本处理方法---------------------

class RddCreation {

  val memoryCache = Array(("Spark", 2), ("Hadoop", 6), ("Sparks", 4), ("Hadoop", 7))
  val textFileSource = "D:/[FileSource]/word.txt"
  val hdfsFileSource = "hdfs://localhost:9000/[FileSource]"
  val jsonFileSource = "file://user/local/spark/[FileSource]"

  def main(args: Array[String]): Unit = {
    // local情况下SparkConfig设置方式
    val config = new SparkConf().setAppName("RDDSampleApp").setMaster("local")
    val sc = new SparkContext(config)
    // 从内存集合数据中创建RDD
    val rdd = sc.parallelize(memoryCache)
    /*
      针对获得的每个<K, V>对的Value进行操作
      进行Map，将数据构建为如：("Spark", (2, 1))的<K, <V_1, V_2>>对
      按照Key值进行Reduce，将每个Key对应的<V_1, V_2>分别相加，计算总数和条目出现数
      利用MapValues对每个Value进行操作，将其转化为Value._1 / Value._2，即求出每个条目的平均出现数
     */
    rdd.mapValues(x => (x, 1)).reduceByKey((x, y) => (x._1 + y._1, x._2 + y._2)).mapValues(x => (x._1 / x._2)).collect()

    // 从文件中拉取RDD内容
    val textFileRDD = sc.textFile(textFileSource)
    // 分布式文件读写：HDFS文件系统
    val hdfsRDD = sc.textFile(hdfsFileSource)
    // 利用RDD的foreach迭代器，传入函数完成遍历打印
    hdfsRDD.foreach(println)
    // JSON格式文件的读入，同样主要依赖于路径目标指定
    val jsonRDD = sc.textFile(jsonFileSource)
    // 执行JSON字符串解析,利用map对每一项构建<K, ParseResult>对
    val jsonResults = jsonRDD.map(s => JSON.parseFull(s))
    // 执行遍历，传入lambda函数定义表达式，查看parse结果，调用match，尝试根据数据类型/内容与各case相匹配
    jsonResults.foreach({ r => r match {
        // 利用Some返回一个list集合对象，如若数据条目结构为<k, V>对，即将其放入Some中构成list，并将其打印
        case Some(map: Map[String, Any]) => println(map)
        case None => println("Parsing Error")
        case other => println("Run into unknown data structure: " + other)
      }
    })
    // 将内容写回指定文件中，如只有单分区，返回信息仅有：part-00000_SUCCESS
    textFileRDD.saveAsTextFile(textFileSource)

    // 从HBase中拉取RDD数据
    val conf = HBaseConfiguration.create()
    // 设置查询表名
    conf.set(mapreduce.TableInputFormat.INPUT_TABLE, "student")
    // 三项参数均位于org.apache.hadoop.hbase包下，创建RDD
    val stuRdd = sc.newAPIHadoopRDD(conf, classOf[mapreduce.TableInputFormat],
      classOf[io.ImmutableBytesWritable],
      classOf[client.Result])
    // 统计返回数据条目数
    val count = stuRdd.count()
    println(s"Total student number: $count")
    // 将内容放入快存中，使用更多高速存储资源减少RDD处理后续IO开销
    stuRdd.cache()
    // 匹配<K, V>类型数据中的Value部分：HBase数据读出时形式类似(io.ImmutableBytesWritable, Value)，
    // 前半部分不会为我们所用到，故此处会直接用_忽略
    stuRdd.foreach({ case (_, result) =>
      val key = Bytes.toString(result.getRow)
      val name = Bytes.toShort(result.getValue("info".getBytes(), "name".getBytes()))
      val gender = Bytes.toShort(result.getValue("info".getBytes(), "gender".getBytes()))
      val age = Bytes.toShort(result.getValue("info".getBytes(), "age".getBytes()))
      println(s"Row id: $key, Student name: $name, gender: $gender and age: $age")
    })
    // 向HBase中写入RDD数据
    val hbaseSparkConf = new SparkConf().setAppName("WriteHBase").setMaster("local")
    val hbaseSC = new SparkContext(hbaseSparkConf)
    // 选定HBase的输出表
    hbaseSC.hadoopConfiguration.set(mapreduce.TableOutputFormat.OUTPUT_TABLE, "student")
    // 定义任务
    val job = new Job(hbaseSC.hadoopConfiguration)
    job.setOutputKeyClass(classOf[io.ImmutableBytesWritable])
    job.setOutputValueClass(classOf[client.Result])
    job.setOutputFormatClass(classOf[mapreduce.TableOutputFormat[io.ImmutableBytesWritable]])
    // 按照student表的格式定义插入的RDD数据
    val dataRDD = sc.makeRDD(Array("3,Bird,Male.26", "4,Tim,Male,19"))
    /*
      对输入数据条目进行进一步格式映射，使其满足插入HBase所需的<K, V>形式
      调用map，对全部数据条目进行遍历，以逗号为分隔符拆解各条数据构建list
      针对每个list中的数据，利用map遍历进行构造处理
     */
    val insertRDD = dataRDD.map(_.split(",")).map(arr => {
      // 利用Put定义放入数据的头尾内容，将行键id（不在info下）放至头中
      val put = new Put(Bytes.toBytes(arr(0)))
      put.add(Bytes.toBytes("info"), Bytes.toBytes("name"), Bytes.toBytes(arr(1)))
      put.add(Bytes.toBytes("info"), Bytes.toBytes("gender"), Bytes.toBytes(arr(2)))
      put.add(Bytes.toBytes("info"), Bytes.toBytes("age"), Bytes.toBytes(arr(3).toInt))
      // 定义放入最终返回RDD集合的<K, V>对
      (new io.ImmutableBytesWritable(), put)
    })
    // 配置结束，将RDD存入HBase中
    insertRDD.saveAsNewAPIHadoopDataset(job.getConfiguration)
  }

}

// --------------------RDD自定义重分区方法---------------------

class MyPartitioner(part_num : Int) extends Partitioner {
  // 重写分片数目
  override def numPartitions : Int = part_num
  // 重写分区分片逻辑方法
  override def getPartition(Key : Any): Int = {
    Key.toString.toInt % numPartitions
  }
}

object TestMyPartitioner {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("MyPartitioner").setMaster("local")
    val sc = new SparkContext(conf)
    // 模拟5个分片RDD
    val data = sc.parallelize(1 to 50, 5)
    /*
      重新Map到10个RDD分片:
      将每个数据条目转为<K, FIXED_VALUE>对
      利用自定义Partition方法，将其重新分为10份
      利用Map取出不需要的FIXED_VALUE部分
     */
    data.map((_, "FIXED_VALUE")).partitionBy(
      new MyPartitioner(10)
    ).map(_._1).saveAsTextFile("[FileSource]")
  }
}

// --------------------案例一：RDD求取TOP值---------------------

object RankRetrieveTopN {

  // 假定数据类型为：[1,177,40,341] => [orderId, userId, payment, productId]
  val filePath = "hdfs://localhost:9000/user/[FileSource]"
  val targetTopN = 5

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("TopN").setMaster("local")
    val sc = new SparkContext(conf)
    // 设置日志等级，即ERROR及以上错误会被自动记入日志输出
    sc.setLogLevel("ERROR")
    // 假定我们利用hdfs分布式文件系统，且当前文件被分为两份存储在两个slave节点中
    // Spark为单机模式（local）并不妨碍其去访问基于hdfs的分布式文件存储系统
    val loadData = sc.textFile(filePath, 2)
    // 设定变量var，用于次序标记
    var num = 0
    /*
      利用filter进行数据迭代过滤，对每个条目执行传入的函数
      首先验证每个条目是否为空，非空情况下是否数据完整：由四部分构成，筛出可用数据
      取出全部Payment作为Key，构建<K, EmptyKey>对，并且按Key依降序排序（不将其转化为Int前，String无法直接排序）
      之后将目标个数的前Key值单独取出，构成list
     */
    val lines = loadData.filter(line => (line.trim().length > 0) &&
      (line.split(",").length == 4))
      .map(_.split(",")(2))
      .map(x => (x.toInt, ""))
      .sortByKey(false)
      .map(x => x._1).take(targetTopN)
      .foreach(x => {
        num = num + 1
        println(s"$num\t$x")
      })
  }

}

// --------------------案例二：RDD求取最大最小值---------------------

object MaxMinRetrieve {

  val filePath = "hdfs://localhost:9000/user/[FileSource]"

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("MaxMin").setMaster("local")
    val sc = new SparkContext(conf)
    // 设置日志等级，即ERROR及以上错误会被自动记入日志输出
    sc.setLogLevel("ERROR")
    val loadData = sc.textFile(filePath, 2)
    /*
      利用filter将非空数据条目取出
      将其映射入<K, V>形式，Key值固定
      利用groupByKey将全部数据汇总于一个list集合之中
      调用map遍历其中所有<K, V>对
      利用for循环，遍历每个Value（list对象）
      从中取出最大最小值，构建<K, V>对并保存
      最终调用collect，并遍历输出结果
     */
    val result = loadData.filter(_.trim().length > 0)
      .map(line => ("K", line.trim.toInt))
      .groupByKey().map(x => {
      var min = Integer.MAX_VALUE
      var max = Integer.MIN_VALUE
      for (num <- x._2) {
        if (num > max) {
          max = num
        }
        if (num < min) {
          min = num
        }
      }
      (max, min)
    }).collect.foreach(x => {
      println(s"Max:\t${x._1}")
      println(s"Min:\t${x._2}")
    })
  }

}

// --------------------案例三：RDD多文件整合排序---------------------

object FileSort {

  val filePath = "hdfs://localhost:9000/user/[FileSource]"
  val targetPath = "hdfs://localhost:9000/user/[FileSource]"

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("FileSort")
    val sc = new SparkContext(conf)
    val loadData = sc.textFile(filePath, 5)
    // 用于进行新的数据条目index/id标记
    var index = 0
    /*
      同样，首先利用filter去空
      利用map将数据放入<K, V>对
      将读入的来自于五个分区的数据加载入一个分区中（partitionBy），使其不会为多个数据区在多个线程上各自Transfer
      即重新将所有数据分入仅一个分区之中，使得整体数据有序
      根据Key进行GroupBy，将重复去除
      通过map，为新构建数据进行index标记
     */
    val result = loadData.filter(_.trim.length > 0)
      .map(n => (n.trim.toInt, ""))
      .partitionBy(new HashPartitioner(1))
      .sortByKey().map(x => {
        index += 1
        (index, x._1)
    })
    result.saveAsTextFile(targetPath)
  }

}

// --------------------案例三：RDD自定义可排序Key结构(可直接作为<K, V>中Key调用sortByKey)---------------------

class ArrangeableKey(val first_key : Int, val second_key : Int) extends Ordered[ArrangeableKey] with Serializable {

  override def compare(that: ArrangeableKey): Int = {
    if (this.first_key - that.first_key != 0) {
      this.first_key - that.first_key
    } else {
      this.second_key - that.second_key
    }
  }

}

object SecondarySort {

  val filePath = "hdfs://localhost:9000/user/[FileSource]"
  val targetPath = "hdfs://localhost:9000/user/[FileSource]"

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("SecondarySort")
    val sc = new SparkContext(conf)
    val loadData = sc.textFile(filePath, 5)
    // 类似FileSort执行的逻辑，不过此处使用自定义的ArrangeableKey进行sortByKey处理
    loadData.filter(_.trim.length > 0)
      // 将Key构建为ArrangeableKey，数据整体结构为<ArrangeableKey, Value>，执行降序排序
      .map(item => (new ArrangeableKey(item.split(",")(0).toInt, item.split(",")(1).toInt), item))
      .sortByKey(false)
      .map(item => item._2)
      .saveAsTextFile(targetPath)
  }

}

// --------------------案例四：RDD连接操作---------------------

object FileJoin {

  val minStars = 4.0

  def main(args: Array[String]): Unit = {
    // 示例如何从运行jar包时的命令行要求参数提供
    if (args.length != 3) {
      println(s"Input invalid, check the form of the parameters: WordCount <Rating> <Movie> <Output>")
      return
    }
    val conf = new SparkConf().setAppName("FileJoin").setMaster("local")
    val sc = new SparkContext(conf)
    // 文件位置作为参数传入（第一个参数）
    val loadData = sc.textFile(args(0))
    // 提取解析数据：<MovieId, Rating>
    val rating = loadData.map(line => {
      val fields = line.split("::")
      (fields(1).toInt, fields(2).toDouble)
    })
    // 获取数据：<MovieId, AvgRating>
    val movieScore = rating.groupByKey()
      .map(line => {
        val avg = line._2.sum / line._2.size
        (line._1, avg)
      })
    // 若从HDFS文件系统中读取数据（第二个参数）
    val loadMovie = sc.textFile(args(1))
    // 进行解析，利用keyBy为当前每个条目构建新的<K, V>对，利用之前的Key作为新的Key构建<K. <K, V>>对
    val movieKey = loadMovie.map(line => {
      val fields = line.split("::")
      (fields(0).toInt, fields(1))
    }).keyBy(tup => tup._1)
    /*
      对结果进行计算
      利用keyBy构建新的键值对<K, <K, MovieAvgScore>>
      利用join，将两个RDD中Key相同的条目的Value进行直接整合：<MovieId, <<MovieId, AvgRating>, <MovieId, MovieName>>>
      利用filter过滤出超过最低分的电影，位置如上述
      最后取出元组<MovieId, AvgRating, MovieName>作为结果
      */
    val result = movieScore.keyBy(tup => tup._1)
      .join(movieKey)
      .filter(item => item._2._1._2 > minStars)
      .map(item => (item._1, item._2._1._2, item._2._2._2))
    // 将结果输出到目标位置(第三个参数）
    result.saveAsTextFile(args(2))
  }

}
