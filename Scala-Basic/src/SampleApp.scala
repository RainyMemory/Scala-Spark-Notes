import org.apache.spark.{SparkConf, SparkContext}

object SampleApp {

  def main(args: Array[String]): Unit = {
    // 说明文件路径，可从HDFS文件系统中获取
    val filePath = "D:/CodeBase-User/IntelliJ-workspace/Scala/HelloWorld/src/AppLog"
    // 对Spark头文件进行设置
    val config = new SparkConf().setAppName("SampleSparkApp")
    val sc = new SparkContext(config)
    // 将文件读入
    val lines = sc.textFile(filePath)
    // 利用flatMap拆解所得内容为list，之后调用map方法并利用reduce进行迭代的两两计数，将结果转化为我们需要的<K, V>数据对
    val wordCount = lines.flatMap(line => line.split(" ")).map(word => (word, 1)).reduceByKey((a, b) => a + b)
    // 调用collect整合结果
    wordCount.collect()
    // 利用foreach迭代器，将println方法作为参数传入，输出结果查看
    wordCount.foreach(println)
  }

}
