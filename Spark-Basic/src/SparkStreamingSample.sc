import java.io.PrintWriter
import java.net.ServerSocket

import org.apache.log4j.{Level, Logger}
import org.apache.spark.internal.Logging
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming._
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable
import scala.io.Source

// --------------------DStream的创建与基本操作---------------------

object CreateDStream {

  val fileSource = "[FileSource]"

  def main(args: Array[String]): Unit = {
    val config = new SparkConf().setAppName("DStreamCreation").setMaster("local")
    val sc = new SparkContext(config)
    // 从sc中接受数据，StreamSC将以10s为batch时间单位创建DStream
    val StreamSC = new StreamingContext(sc, Seconds(10))

    // 文件流DStream创建， 每十秒会读取一次本文件并依靠其内容向lines传递数据
    val lines = StreamSC.textFileStream(fileSource)
    val words = lines.flatMap(_.split(" "))
    val wordCount = words.map((_, 1)).reduceByKey(_ + _)
    wordCount.print()
    // 启动StreamSC，开始读取数据，等待程序退出信号（遇异常停止）
    StreamSC.start()
    StreamSC.awaitTermination()
    // StreamSC.stop()：打断工作流
  }

}

// --------------------从Socket通信中监听创建DStream----------------------

object NetworkSocketListener {

  // 这代表我们需要在启动命令中spark-submit尾部添加主机号及监听端口号，如：localhost 8899
  def main(args: Array[String]): Unit = {
    // 判定传入参数格式正误
    if (args.length < 2) {
      System.err.println("Lacking parameters, should be: <hostname> <port>")
      System.exit(1)
    }
    // 设置Streaming日志等级
    StreamingExamples.setStreamingLogLevels()
    val StreamSC = new StreamingContext(new SparkConf().setAppName("WordCountStreaming").setMaster("local"), Seconds(2))
    // 从Socket端口渡入数据，参数注明数据存储策略
    val lines = StreamSC.socketTextStream(args(0), args(1).trim.toInt, StorageLevel.MEMORY_AND_DISK_SER)
    val words = lines.flatMap(_.split(" "))
    val wordCount = words.map((_, 1)).reduceByKey(_ + _)
    wordCount.print()
    // 启动任务
    StreamSC.start()
    StreamSC.awaitTermination()
  }

}

// 用于给出适合的Log日志输出级别设置
object StreamingExamples extends Logging {

  def setStreamingLogLevels(): Unit = {
    // 如果尚未初始化，则设定为WARN级别log日志输出
    val log4jInitialized = Logger.getRootLogger.getAllAppenders.hasMoreElements
    if (!log4jInitialized) {
      logInfo("Setting log level to [WARN]")
      Logger.getRootLogger.setLevel(Level.WARN)
    }
  }

}

// 自定义Socket数据源
object DataSourceSocket {

  // 返回随机行数index
  def index(range : Int) = {
    val rand = new java.util.Random()
    rand.nextInt(range)
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 3) {
      System.err.println("Parameters Exception. Expect: <fileSource> <port> <millisecond>")
      System.exit(1)
    }
    val lines = Source.fromFile(args(0)).getLines().toList
    val listener = new ServerSocket(args(1).trim.toInt)
    // 直到被打断前不断启动线程监听目标端口
    while (true) {
      val socReceive = listener.accept()
      new Thread() {
        override def run(): Unit = {
          println(s"Connect to: ${socReceive.getInetAddress}")
          // 创建写出工具
          val out = new PrintWriter(socReceive.getOutputStream, true)
          // 随机获取文件中的一行数据
          Thread.sleep(args(2).trim.toInt)
          val content = lines(index(lines.length))
          // 将其向目标端口发送
          out.write(content + "\n")
          out.flush()
        }
      }
    }
  }

}

// --------------------创建RDD队列流----------------------

object QueueStream {

  def main(args: Array[String]): Unit = {
    val StreamSC = new StreamingContext(new SparkConf().setAppName("Queue").setMaster("local[2]"), Seconds(2))
    // 创建RDD序列容器
    val rddQueue = new mutable.SynchronizedQueue[RDD[Int]]()
    val queueStream = StreamSC.queueStream(rddQueue)
    val handledStream = queueStream.map(item => (item.toInt % 10, 1)).reduceByKey(_ + _)
    handledStream.print()
    // 开始工作流
    StreamSC.start()
    // 循环创建RDD序列，DStream将不断从RDD列中获取新数据
    for (index <- 1 to 20) {
      rddQueue += StreamSC.sparkContext.makeRDD(1 to 100, 2)
      Thread.sleep(1000)
    }
    // 终止工作流
    StreamSC.stop()
  }

}

/* --------------------一个利用Kafka订阅数据流的示例----------------------
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord, KafkaProducer}
import org.apache.spark.streaming.kafka._

// Kafka消息生成机
object MyKafkaProducer {

  def main(args: Array[String]): Unit = {
    if (args.length < 4) {
      System.err.println("Parameters: <metaDataBrokerList> <topic> <msgPerSecond> <wordsPerSecond>")
      System.exit(1)
    }
    val Array(brkers, topic, messagesPerSec, wordsPerMessage) = args
    // ZK properties
    val props = new mutable.HashMap[String, Object]()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brkers)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    val producer = new KafkaProducer[String, String](props)
    // 发出信息
    while (true) {
      (1 to wordsPerMessage.toInt).foreach(message_num => {
        val str = (1 to message_num).map(_ => scala.util.Random.nextInt(10).toString).mkString(" ")
        print(str)
        println()
        val message = new ProducerRecord[String, String](topic, null, str)
        producer.send(message)
      })
      Thread.sleep(1000)
    }
    producer.close()
  }

}

// Consumer，Kafka消息接收
object KafkaWordCount {

  // 检查点路径
  val checkPoint = "[checkPointSource]/checkpoint"

  def main(args: Array[String]): Unit = {
    // 借用之前定义的Log设置执行输出级别配置
    StreamingExamples.setStreamingLogLevels()
    val StreamSC = new StreamingContext(new SparkConf().setMaster("local"), Seconds(10))
    // 不设置检查点可能会导致信息的传送中丢失
    StreamSC.checkpoint(checkPoint)
    val zkQuorum = "localhost:2181"
    val group = "test-group-name"
    val tpoics = "test-topic-name1,test-topic-name2"
    val numThreads = 1 // 每个topic的分区数目
    val topicMap = tpoics.split(",").map((_, numThreads.toInt)).toMap
    val lineMap = KafkaUtils.createStream(StreamSC, zkQuorum, group, topicMap)
    val lines = lineMap.map(_._2)
    val words = lines.flatMap(_.split(" "))
    val pair = words.map(x => (x, 1))
    // 本身为DStream，两分钟内，每个十秒钟进行词频统计
    val wordCounts = pair.reduceByKeyAndWindow(_ + _, _ - _, Minutes(2), Seconds(10), 2)
    wordCounts.print()
    StreamSC.start()
    StreamSC.awaitTermination()
  }

}*/

// --------------------DStream：updateStateByKey----------------------

object NetworkWordCountStateful {

  val checkPoint = "[checkPointSource]/checkpoint"

  def main(args: Array[String]): Unit = {
    val updateFunc = (values : Seq[Int], state : Option[Int]) => {
      // 将序列向左规约，计算当前词频
      val currentCount = values.foldLeft(0)(_ + _)
      val previousCount = state.getOrElse(0)
      Some(currentCount + previousCount)
    }
    StreamingExamples.setStreamingLogLevels()
    val StreamSC = new StreamingContext(new SparkConf().setMaster("local[2]"), Seconds(5))
    StreamSC.checkpoint(checkPoint)
    // 从指定的Socket中获取数据
    val lines = StreamSC.socketTextStream("localhost", 9099)
    val wordDS = lines.flatMap(_.split(" ")).map((_, 1))
    // 将历史状不断更新进入stateDS中
    val stateDS = wordDS.updateStateByKey[Int](updateFunc)
    stateDS.print()
    StreamSC.start()
    StreamSC.awaitTermination()
  }

}