// -------------------------函数定义----------------------------
def counter(param: Int): Int = {
  param + 1
}

val tenAdder: Int => Int = (param: Int) => param + 10 // Int => Int 标明函数类型，多数情况可与输入参数类型二选一保留

val multiply = (_: Int) * (_: Int) // 参数简略写法,等效计算 a * b

(value: Int) => value * 2 // lambda匿名函数

val list = List(3, 4, 5, 6, 7)
val mappedList = list.map(_ * 10)
println(mappedList)

// -------------------------高阶函数----------------------------
def function(func: Int => Int, start: Int, end: Int): Int = { // 传入函数作为参数，可用已定义函数或者lambda表达式
  if (start > end) {
    0
  } else {
    func(start) + function(func, start + 1, end)
  }
}

val squareSum: Int => Int = (param: Int) => { // 将函数定义为变量形式
  if (param <= 0) {
    1
  } else {
    2 * squareSum(param - 1)
  }
}

println(function(x => x * x, 1, 6)) // 使用lambda表达式作为参数
println(function(squareSum, 1, 6)) // 使用已定义函数

// -------------------------容器使用方法----------------------------
val myList = List("a", "quick", "brown", "fox", "jumped", "over", "the", "lazy", "dog")
val iterFunc: String => Unit = (i: String) => print(s"$i ")
myList.foreach(iterFunc) // foreach本身为高阶函数，通过传入的参数定义每个元素的操作

var myMap = Map("YoGa" -> "AMD R7 4800U", "ZenBook" -> "AMD R5 4600U", "SurfaceGo" -> "Intel M3", "ROG14" -> "AMD R7 4800HS")
myMap.foreach(kvPair => print(kvPair._1 + ": " + kvPair._2 + "; "))
myMap.foreach(item => item match {
  case (key, value) => print(s"$key - $value; ") // 调用Map的unapply方法拆解kv值
})

myList.map(str => str.toUpperCase) // 对String类型进行操作的lambda表达式
myList.map(str => str.length)
myList.flatMap(str => str.toUpperCase.toList).sorted.distinct // 将字符串拆解，构成Map，升序排序后去重
myMap.filter(kvPair => kvPair._2.contains("Intel")) // 利用filter过滤器找出满足需求的item集合

myList.reduce(_ + _) // 规约操作，按顺序逐个累积操作，此处为累加 a + b
myList.reduceLeft((left, right) => s"L($left, $right)") // 一般默认为reduceLeft规则
myList.reduceRight((left, right) => s"R($left, $right)")

val foldList = List(1, 2, 3, 4, 5)
foldList.fold(10)(_ - _) // 默认执行foldLeft
foldList.foldRight(10)(_ - _)
foldList.foldLeft(10)(_ - _)