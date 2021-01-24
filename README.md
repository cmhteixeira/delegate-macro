# Proxy Macro &emsp; [![Build Status](https://www.travis-ci.com/cmhteixeira/delegate-macro.svg?branch=master)](https://www.travis-ci.com/cmhteixeira/delegate-macro)

## Usage

```scala
trait Foo {
  def bar(a1: Int, a2: String)(b1: String, b2: Int): String
  def baz(a: Int, b: String): String
  def barBaz(a: String): String = s"DefaultImplementation-$a"
}
```

```scala
class FooImpl extends Foo {
  def bar(a1: Int, a2: String)(b1: String, b2: Int): String = "FooImpl-bar"
  def baz(a: Int, b: String): String = "FooImpl-baz"
  override def barBaz(a: String): String = s"FooImpl-barBaz"
}
```

```scala
@Proxy
class YourClass(delegatee: Foo) extends Foo {
  def baz(a: Int, b: String): String = "YourClass-baz"
}
```

The `Proxy` annotation will, at compile time, expand your class to effectively
```scala
class YourClass(delegatee: Foo) extends Foo {
  def bar(a1: Int, a2: String)(b1: String, b2: Int): String = delegatee.bar(a1, a2)(b1, b2)
  def baz(a: Int, b: String): String = "YourClass-baz"
  def barBaz(a: String): String = s"DefaultImplementation-$a"
}
```

## Intellij

Intellij won't be able to expand the macro.   
It will indicate your class has an error whereby it needs to implement abstract members.  
This is aesthetically unpleaseant, but of no real consequence. The solution would be to develop a plugin for Intellij for this macro. 