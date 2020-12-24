// -------------------------结构流程----------------------------
var counter = 0
def count: Int = {
  counter += 1
  counter
}

import scala.util.control.Breaks.{break, breakable}

breakable {
  for (i <- 1 to 10 by 2) {
    print(count)
    if (i >= 7) {
      println(s"\nBreaks at: $count")
      break()
    }
  }
}

var grade = 'g'
grade match {
  case 'A' | 'a' => println("1秒过去了")
  case 'B' | 'b' => println("2秒过去了")
  case 'C' | 'c' => println("3秒过去了")
  case 'D' | 'd' => println("4秒过去了")
  case 'E' | 'e' => println("5秒过去了")
  case 'F' | 'f' => println("6秒过去了")
  case 'G' | 'g' => println("7秒过去了")
  case 'H' | 'h' => println("8秒过去了")
  case 'I' | 'i' => println("9秒过去了")
  case _ => println("The World!")
}

// -------------------------容器集合----------------------------
// List集合
val myList = List("quick", "brown", "fox", "jumps", "over", "the", "lazy", "cat")
myList.head
myList.tail
var fullList = "a" :: myList
// 向量
var myVector = Vector(0, 4, 8, 2)
var fullVector = 1 +: myVector
fullVector = fullVector :+ 4
for (item <- fullVector) {
  item match {
    case _ if (item % 2 == 0) => print("偶")
    case _ => print("奇")
  }
}
// 等差数列
val myRange = Range(1, 9, 2)
val anotherRange = 1 to 9 by 2
// HashMap
import scala.collection.mutable

var myMap = mutable.Map("YoGa" -> "AMD R7 4800U", "ZenBook" -> "AMD R5 4600U")
myMap += ("SurfaceGo" -> "Intel M3", "ROG14" -> "UNKNOWN") // 添加键值对，不需要mutable
myMap.put("ROG16", "AMD R9 4900HS") // 创建新键值对，需要mutable
myMap.put("ROG14", "AMD R7 4800HS") // 更新已有键值对，需要mutable
if (myMap.contains("ROG14")) {
  for ((k, item) <- myMap) {
    print(s"$k: $item; ")
  }
  println()
}
