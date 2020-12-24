import org.apache.spark.sql.functions._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.StructType

val spark = SparkSession.builder().getOrCreate()

import spark.implicits._

// -----------------------Structure Streaming 创建------------------------

object StructuredStreamingCreation {

  def main(args: Array[String]): Unit = {
    // 从主机端口监听获取数据流
    val lines = spark.readStream.format("socket").option("host", "localhost").option("port", "8099").load()
    // 对DataFrame进行转换操作，利用as方式将DataFrame转换为DataSet（DataFrame是DataSet的一种特例）
    val words = lines.as[String].flatMap(_.split(" "))
    val wordCount = words.groupBy("value").count()
    // 执行流查询，利用complete输出模式，将最终信息打印在控制台返回中
    val queryStream = wordCount.writeStream.outputMode("complete").format("console").start()
    queryStream.awaitTermination()
  }

}

// -----------------------Structure Streaming 基于文件流创建------------------------

object StructuredStreamingFileSource {

  // 以json格式文件为例，指定路径，将到该路径下寻找全部新增的json数据文件
  val fileSource = "[FileSource]/"

  def main(args: Array[String]): Unit = {
    /* 相当于：
      org.apache.spark.sql.types.StructType = StructType(
        StructField(name,StringType,true),
        StructField(age,IntegerType,true),
        StructField(hobby,StringType,true)
      )
     */
    val targetSchema = new StructType()
      .add("name", "string")
      .add("age", "integer")
      .add("hobby", "string")
    // 利用readStream读入指定目录下的json文件
    val targetDataFrame = spark.readStream.schema(targetSchema).json(fileSource)
    // 定义DataFrame的系列转换操作
    val userBelow25DF = targetDataFrame.filter($"age" < 25)
    val hoobyDF = userBelow25DF.groupBy("hobby").count()
    // 开启流查询
    val queryStream = hoobyDF.writeStream.outputMode("complete").format("console").start()
    queryStream.awaitTermination()
  }

}