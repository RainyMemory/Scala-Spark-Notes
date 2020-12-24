import java.util.Properties

import org.apache.spark.sql.types._
import org.apache.spark.sql.{Row, SparkSession}

// --------------------DataFrame各创建方式以及其基本处理方法---------------------

object CreateDataFrame {

  val fileSource = "[FileSource]"
  val targetSource = "[TargetSource]"

  // 定义case class，用于描述文本数据结构，由此反射构建DataFrame
  // case class无需进行new操作，实现默认Serializable，工厂方法，及伴生对象构建
  case class Person(name : String, age : Int)

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().getOrCreate()
    // 导入需要使用的依赖项，支持RDD与DataFrame之间的隐式转换
    import spark.implicits._
    // 根据读入文件类型的不同，调用不同函数构建DataFrame
    val jsonData = spark.read.json(fileSource)
    val parquetData = spark.read.parquet(fileSource)
    val csvData = spark.read.csv(fileSource)
    // 展示获取的数据
    parquetData.show()
    // 向已有的DataFrame中写入新来源数据
    csvData.write.csv(fileSource)
    // 指定类型读入文件
    val targetData = spark.read.format("json").load(fileSource)
    // 筛选DataFrame中的内容并输出到目标文件
    targetData.select("name", "age").write.format("csv").save(targetSource)
    // 筛选DataFrame中的信息，利用此方法明确col名取出age并+1
    jsonData.filter(jsonData("age") + 1).show()
    // 指定列执行groupBy
    jsonData.groupBy("age").count().show()
    // 指定列执行降序排序
    jsonData.sort(jsonData("age").desc).show()
    // 属性重命名
    jsonData.select(jsonData("name").as("username"), jsonData("age")).show()

    // 观察文本，通过反射机制不指定DataFrame数据类型获取DataFrame数据
    // 本质是通过构建具有类信息的RDD，利用toDF()方法将其转化为DataFrame（依赖于spark.implicits._）
    val textData = spark.sparkContext.textFile(fileSource).map(_.split(","))
      .map(item => Person(item(0), item(1).trim.toInt)).toDF()
    // DataFrame必须注册临时表（View）才可供以sql语句直接查询使用
    textData.createOrReplaceTempView("textView")
    val retrieveRDD = spark.sql("select * from textView where age > 5")
    retrieveRDD.foreach(item => println(s"Name: ${item(0)}, Age: ${item(1)}"))

    // 通过代码创建DataFrame格式（自定义数据结构）
    // 定义各项表头信息
    val fields = Array(
      StructField("name", StringType, true),
      StructField("age", IntegerType, true)
    )
    // 创建表格
    val schema = StructType(fields)
    // 向表格中加载数据
    val peopleRDD = spark.sparkContext.textFile(fileSource)
    val rowData = peopleRDD.map(_.split(",")).map(item => Row(item(0), item(1).trim.toInt))
    val peopleDF = spark.createDataFrame(rowData, schema)
    peopleDF.show()
  }

}

// --------------------链接并使用关系型数据库：MySQL(例)---------------------

object ConnectRelationalDB {

  val spark = SparkSession.builder().getOrCreate()
  // 针对目标数据库JDBC信息进行定义
  val JDBC = spark.read.format("jdbc")
    .option("url", "jdbc:mysql://localhost:3306/[sample_database]")
    .option("driver", "com,mysql,jdbc,Driver")
    .option("dbtable", "sample_table")
    .option("user", "username")
    .option("password", "user_password")
    .load()

  def main(args: Array[String]): Unit = {
    // 构建写入数据库的测试数据
    val readInRDD = spark.sparkContext.parallelize(Array("1:Ryan:Male:24", "3:Mana:Female:19")).map(_.split(":"))
    // 描述数据库表结构
    val schema = StructType(List(
      StructField("id", IntegerType, true),
      StructField("name", StringType, true),
      StructField("gender", StringType, true),
      StructField("age", IntegerType, true)
    ))
    val rowData = readInRDD.map(item => Row(item(0).trim.toInt, item(1), item(2), item(3).trim.toInt))
    // 创建包含目标结构数据的DataFrame
    val peopleDF = spark.createDataFrame(rowData, schema)
    // 向MySQL写入读入的数据
    // 保存JDBC各项参数
    val prop = new Properties()
    prop.put("user", "username")
    prop.put("password", "user_password")
    prop.put("driver", "com,mysql,jdbc,Driver")
    // 连接数据库，采用append模式，向列表尾新增数据
    peopleDF.write.mode("append").jdbc("jdbc:mysql://localhost:3306/[sample_database]", "[sample_database].[sample_table]", prop)
  }

}