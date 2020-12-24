import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.{DecisionTreeClassifier, LogisticRegression, LogisticRegressionModel}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature._
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.sql.{Row, SparkSession}

// -----------------Sample: Logistic Regression---------------------
val spark = SparkSession.builder().getOrCreate()
// 构建训练用的简单数据
val trainingData = spark.createDataFrame(Seq(
  (0L, "a b c d e spark", 1.0),
  (1L, "a b c", 0.0),
  (2L, "a spark d g k l", 1.0),
  (3L, "a d e spk", 0.0),
  (4L, "a boring d kate", 0.0),
  (5L, "spark damn data", 1.0),
  (6L, "a hadoop map reduce spy", 0.0)
)).toDF("id", "text", "label")
// 构建测试数据集
val testingData = spark.createDataFrame(Seq(
  (0L, "c d spark"),
  (1L, "a b c")
)).toDF("id", "text")

object SimpleLogisticRegression {

  def main(args: Array[String]): Unit = {
    // Transformer：将输入text中的token拆开
    val myTokenizer = new Tokenizer().setInputCol("text").setOutputCol("words")
    // Transformer：将获取的各个Token利用Hashing映射到1000维度的Vector上
    val myHashingTF = new HashingTF().setNumFeatures(1000).setInputCol(myTokenizer.getOutputCol).setOutputCol("features")
    // Estimator：选择算法，配置其迭代次数，学习率等参数
    val myAlgorithm = new LogisticRegression().setMaxIter(20).setRegParam(0.01)
    // Pipeline：开始构建整体工作流程
    val myPipeline = new Pipeline().setStages(Array(myTokenizer, myHashingTF, myAlgorithm))
    // 将数据传入Pipeline，调用fit方法
    val model = myPipeline.fit(trainingData)
    // 利用Model推测传入无Label数据的结果
    model.transform(testingData).select("id", "text", "probability", "prediction").collect().foreach({
      case Row(id: Long, text: String, prob: Vector, prediction: Double) => println(s"($id, $text): probability: $prob, prediction: $prediction")
    })
  }

}

// -----------------Sample: TF-IDF权重---------------------

object TfIdfSample {

  def main(args: Array[String]): Unit = {
    // Transformer：进行tokens提取
    val myTokenizer = new Tokenizer().setInputCol("text").setOutputCol("words")
    val wordsDataFrame = myTokenizer.transform(trainingData)
    // Transformer：进行Hashing，将tokens映射入2000个哈希桶中
    val myHashingTF = new HashingTF().setInputCol("words").setOutputCol("features").setNumFeatures(2000)
    val featuresDataFrame = myHashingTF.transform(wordsDataFrame)
    // Estimator：利用idf算法调整权重
    val idfFunc = new IDF().setInputCol("features").setOutputCol("features-idf")
    val idfFeatureModel = idfFunc.fit(featuresDataFrame)
    // 提取训练集中各个token的tf-idf权重
    val rescaledDataFrame = idfFeatureModel.transform(featuresDataFrame)
    rescaledDataFrame.select("features", "label").take(3).foreach(println)
  }

}

// -----------------Sample: Word2Vec---------------------

// 利用Tuple1.apply辅助Array<String>转化为DataFrame
val docDataFrame = spark.createDataFrame(Seq(
  "READY TO FIGHT FOR UNION".split(" "),
  "FOR MOTHER RUSSIA".split(" "),
  "READY TO CRASH".split(" ")
).map(Tuple1.apply)).toDF("text")

object Word2VecSample {

  def main(args: Array[String]): Unit = {
    // 仅统计出现次数大于0的单词，对其进行自动的Word2Vec语义空间映射
    val word2Vec = new Word2Vec().setInputCol("text").setOutputCol("result").setVectorSize(5).setMinCount(0)
    val word2VecModel = word2Vec.fit(docDataFrame)
    val wordEmbedding = word2VecModel.transform(docDataFrame)
    wordEmbedding.select("result").take(5).foreach(println)
  }

}

// -----------------Sample: CountVectorizer---------------------

val tokensDataFrame = spark.createDataFrame(Seq(
  (0, Array("a", "b", "c")),
  (1, Array("tik", "tac", "toe")),
  (2, Array("tik", "tac", "c", "crop", "b", "b")),
  (3, Array("kick", "stars", "crop"))
)).toDF("id", "text")

object CountVecSample {

  def main(args: Array[String]): Unit = {
    // 直接通过向定义的Estimator中传入数据进行fit来构建Model：MinDF代表token至少出现在的文档数目
    val cvModel: CountVectorizerModel = new CountVectorizer().setInputCol("text").setOutputCol("features").setVocabSize(5).setMinDF(2).fit(tokensDataFrame)
    // 查看有多少词汇被作为特征保存
    cvModel.vocabulary.foreach(token => print(s"$token; "))
    cvModel.transform(tokensDataFrame).show(false)
    // 可以通过已经处理得到的先验字典直接使用，从而跳过fit步骤（使用pre-train字典模型）
    val cvModelFromPreTrain = new CountVectorizerModel(Array("a", "d", "crop")).setInputCol("text").setOutputCol("features")
    cvModelFromPreTrain.transform(tokensDataFrame).show(false)
  }

}

// -----------------Sample: 利用Logistic Regression处理Iris训练数据集---------------------
import spark.implicits._

object LogRegOnIris {

  case class Iris(features: Vector, label: String)

  val IrisFilePath = "[IrisFileSource]"

  def main(args: Array[String]): Unit = {
    val loadData = spark.sparkContext.textFile(IrisFilePath).map(_.split(" "))
      .map(item => Iris(Vectors.dense(item(0).toDouble, item(1).toDouble, item(2).toDouble, item(3).toDouble), item(4))).toDF()
    // 创建临时表，方便数据处理
    loadData.createOrReplaceTempView("iris")
    val irisDataFrame = spark.sql("select * from iris where lable != 'Iris-setosa'")
    // 创建Indexer，将Label和Feature向量进行向量空间投影
    val labelIndexer = new StringIndexer().setInputCol("label").setOutputCol("indexedLabel").fit(irisDataFrame)
    val featureIndexer = new VectorIndexer().setInputCol("features").setOutputCol("indexedFeatures").fit(irisDataFrame)
    // 训练集，测试集切分
    val Array(trainSet, testSet) = irisDataFrame.randomSplit(Array(0.7, 0.3))
    // 创建Logistic Regression的Estimator
    val logistic = new LogisticRegression().setLabelCol("indexedLabel").setFeaturesCol("indexedFeatures").setMaxIter(20).setRegParam(0.3).setElasticNetParam(0.8)
    // 将Prediction结果从IndexedLabel转化回String，使结果与输入集对应
    val labelConverter = new IndexToString().setInputCol("prediction").setOutputCol("predictLabel").setLabels(labelIndexer.labels)
    // 创建Pipeline，定义工作流
    val logisticPipeline = new Pipeline().setStages(Array(labelIndexer, featureIndexer, logistic, labelConverter))
    val logisticModel = logisticPipeline.fit(trainSet)
    val predictions = logisticModel.transform(testSet)
    predictions.select("predictLabel", "label", "features", "probability").collect().foreach({
      case Row(predict: String, label: String, features: Vector, prob: Vector) => println(s"($label : $predict) with probability: $prob and features : $features")
    })
    // 模型评估
    val evaluator = new MulticlassClassificationEvaluator().setLabelCol("indexedLabel").setPredictionCol("prediction")
    val accuracy = evaluator.evaluate(predictions)
    print(s"Test accuracy: $accuracy")
    // 模型参数获取
    val modelParameters = logisticModel.stages(2).asInstanceOf[LogisticRegressionModel]
  }

}

// -----------------Sample: 利用决策树处理Iris训练数据集---------------------

object DecisionTreeSample {

  case class Iris(features: Vector, label: String)

  val IrisFilePath = "[IrisFileSource]"

  def main(args: Array[String]): Unit = {
    val loadData = spark.sparkContext.textFile(IrisFilePath).map(_.split(" "))
      .map(item => Iris(Vectors.dense(item(0).toDouble, item(1).toDouble, item(2).toDouble, item(3).toDouble), item(4))).toDF()
    loadData.createOrReplaceTempView("iris")
    val irisDataFrame = spark.sql("select features, label from iris")
    val labelIndexer = new StringIndexer().setInputCol("label").setOutputCol("indexedLabel").fit(irisDataFrame)
    // 决策树最佳使用Nominal/Categorical数据，需要指定类别总数
    val featureIndexer = new VectorIndexer().setInputCol("features").setOutputCol("indexedFeatures").setMaxCategories(4).fit(irisDataFrame)
    val Array(trainSet, testSet) = irisDataFrame.randomSplit(Array(0.7, 0.3))
    // 决策树构建
    val dtClassifier = new DecisionTreeClassifier().setLabelCol("indexedLabel").setFeaturesCol("indexedFeatures")
    val labelConverter = new IndexToString().setInputCol("prediction").setOutputCol("predictLabel").setLabels(labelIndexer.labels)
    val dtPipeline = new Pipeline().setStages(Array(labelIndexer, featureIndexer, dtClassifier, labelConverter))
    // 训练模型
    val dtModel = dtPipeline.fit(trainSet)
    val predictions = dtModel.transform(testSet)
    predictions.select("predictLabel", "label", "features", "probability").collect().foreach({
      case Row(predict: String, label: String, features: Vector, prob: Vector) => println(s"($label : $predict) with probability: $prob and features : $features")
    })
  }

}