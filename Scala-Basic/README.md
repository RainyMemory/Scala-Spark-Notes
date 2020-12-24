# Scala Note

---

## Scala 安装

- 此处选择开发组合为：Intellj & Scala

- JDK版本为`1.8`或`11`

- Intellj在 Preference -> Plugins 下安装 Scala 扩展

- 可让Intellj中Scala插件自行下载可用Scala版本，此处引用已经下载完毕的外部Scala，
版本为：`2.11.12`（引入时注意Scala插件可支持的当前Scala版本范围）

## Scala 基本语法及程序逻辑运算流程

> 本部分代码示例：[ScalaStream](./src/ScalaStream.sc)

### 基础语法要点

- Scala中各项类型皆为类

- 富包装类：实现如`max`等操作的类别
    - 一般类都具有其向对应的富包装类的映射，如：`Int` -> `RichInt`

- 函数式编程方式，定义的`val`类型值无法更改（再赋值）

- `Var`类型做为变量可被重复赋值

- Scala中可以对参数进行类型声明：`myString : String = "MyString"`

- 格式化输出
    - s字符串输出：利用`${variable}`将参数插入对应输出位置
    - f字符串输出：相比s字符串多出利用`%*d %*f`控制输出参数精度的功能
    
- 合法赋值语句示例：`val a = if (x > 0) 1 else -1`

- 类似Python，可以利用关键字`yield`将多返回信息构筑为`list`后返回

- 异常处理：`try{~} catch{case ex : [exception] => {~}} finally{~}`

- 在`control flow`中，没有`continue`以及`beak`关键字操作
    - 利用`breakable`关键字构建可跳出`control flow`
    
### 部分数据类型简介

- `Tuple`：结构如(Element1, Element2, Element3)

- `Array`：结构如[Element1, Element2, Element3]

- Container容器：`collection`，被Scala分为`mutable`和`immutable`两类，注意依赖引用
    - `Vector`：可用`+:`或`:+`在其头尾执行新元素链接
    - `List`：与Array类似
    - `Set`：默认HashSet，利用hashing进行元素的快速索引
    - `Map`：数据结构形如<K, V>，可以利用`+=(<K, V>, <K, V>)`添加新成员

---

## Scala 函数

> 本部分代码示例：[ScalaFunction](./src/ScalaFunctions.sc)

- 函数定义基本形式：`def function(param : Any) : Unit = {~}`

- Scala支持匿名函数：可将lambda表达式作为参数

- `_`关键字可代表当前传入的一个参数：`val multiply = (_: Int) * (_: Int)`

- Scala支持将函数作为参数传入另一个函数使用（高阶函数）

---

## Scala 类编程

> 本部分代码示例：[ScalaObject](./src/ScalaObject.sc)

- `Class`与`Object`在Scala中的区别：
    - `Class`：需要利用关键字`new`创建新对象
    - `Object`：单例模式，无需新建，直接被写入内存访问调用
    
- 当`Class`与`Object`同名时，构成伴生关系，彼此之间可直接访问对方内部成员而不需再次定义
    - 构成伴生关系后两者可以实现`Apply`与`Unapply`方法，增加代码灵活度
    
- Scala具有继承类或者接口`trait`的能力