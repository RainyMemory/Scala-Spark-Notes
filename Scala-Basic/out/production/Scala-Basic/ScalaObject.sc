// -------------------------类的编写----------------------------
class SampleClass {
  private var privateValue = 0
  private var initValue = 0
  private var initMul = 1

  def this(init: Int) = { // 构造器定义
    this()
    initValue = init
    privateValue = init
  }

  def this(init: Int, mul: Int) = {
    this(init)
    initMul = mul
    privateValue *= mul
  }

  def increment(step: Int): Unit = { // 需要传入参数
    privateValue += step
  }

  def show: String = { // 无需传入参数
    s"Value is: $privateValue"
  }

  def value_=(newValue: Int) { // 赋值函数通常定义方式
    if (newValue > 0) {
      privateValue = newValue
    }
  }
}

val myTuple = (114, "%", "可以上了", 2) // 元祖构建

val intArr = new Array[Int](9) // 长度为9的Int数组，所有元素初始值为0
intArr(0) = 99
intArr(7) = 1000

val myClass = new SampleClass(myTuple._1, myTuple._4)
myClass.increment(intArr(0))
println(myClass.show)
myClass.value_=(intArr(7))
println(myClass.show)

// -------------------------伴生关系----------------------------
class Person {
  private var personName: String = ""
  private val personNo: Int = Person.addNewMember

  def this(name: String) = {
    this
    personName = name
  }

  def personInfo: Unit = {
    println(s"New member: $personName, with member id: $personNo")
  }

  def apply(param: String): Unit = {
    println(s"Apply method called by: $param")
  }
}

object Person { // 完全静态，单例模式，无需初始化
  private var personNo = 0

  def addNewMember: Int = {
    personNo += 1
    personNo
  }
}

var personA = new Person("Delay")
var personB = new Person("Tiffany")
personA.personInfo
personB.personInfo
personA("Rock N Roll")

// -------------------------Apply伴生使用----------------------------
object Member {
  def apply(param: String, id: Int): Member = new Member(param, id)

  def unapply(arg: Member): Option[(String, Int)] = {
    Some((arg.param, arg.id))
  }
}

class Member(val param: String, val id: Int) {
  def getInfo: Unit = {
    println(s"New member name: $param, id: $id")
  }
}

val freshMan = Member("Ricky", 29) // 不利用new，代表使用object，此时object调用apply方法来提供class Member的实例
freshMan.getInfo
val Member(name, id) = Member("Rudy", 30) // unapply被调用
println(s"New member id: $id, name: $name")

// -------------------------Extends继承----------------------------
abstract class Car {
  val brand: String

  def carInfo()

  def show: Unit = {
    println(s"Nice")
  }
}

class Auto extends Car {
  override val brand: String = "AUTO"

  def carInfo(): Unit = {
    println(s"$brand, at your service.")
  }
}

class Tesla extends Car {
  override val brand: String = "TESLA"

  def carInfo(): Unit = {
    println(s"$brand, at your service.")
  }
}

val myCarA = new Auto
val myCarT = new Tesla
myCarA.show
myCarT.show
myCarA.carInfo()
myCarT.carInfo()

// -------------------------Trait接口----------------------------
trait Flyable {
  var maxFlyHigh: Int

  def fly

  def rotate(angle: Int)
}

trait PaperMade {
  var size: Int

  def showSize: Unit = {
    println(s"Made with paper of size $size x $size (cm^2)")
  }
}

class Toy(val param: Int) {
  def info: Unit = {
    println(s"This is a toy suitable for kids under: $param")
  }
}

class PaperPlane(flyHeight: Int) extends Toy(12) with Flyable with PaperMade {
  override var maxFlyHigh: Int = flyHeight
  override var size: Int = 16
  private var currentHeight = 0
  private var currentAngle = 0

  override def fly: Unit = {
    if (currentHeight < maxFlyHigh) {
      currentHeight += 1
    }
    println(s"Current height is: $currentHeight")
  }

  override def rotate(angle: Int): Unit = {
    currentAngle = (currentAngle + angle) % 360
    println(s"Now fly to: $currentAngle")
  }

  def setSize(param: Int): Unit = {
    size = param
  }
}

val myPlane = new PaperPlane(14)
myPlane.rotate(7700)
myPlane.fly
myPlane.info
myPlane.setSize(22)
myPlane.showSize