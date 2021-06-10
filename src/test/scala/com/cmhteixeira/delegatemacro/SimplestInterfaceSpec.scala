package com.cmhteixeira.delegatemacro

import org.scalatest.{FlatSpec, Matchers}

class SimplestInterfaceSpec extends FlatSpec with Matchers {

  trait DBService {
    def retrieveRecord(id: Int): String
  }

  trait Foo {
    def bar(a1: Int, a2: String)(b1: String, b2: Int): String
    def baz(a: Int, b: String): String
    def barBaz(a: String): String = s"Default-Implementation-$a"
  }

  "The annotation" should "delegate to simple member" in {
    class DBServiceImpl extends DBService {
      override def retrieveRecord(id: Int): String = "FooBar"
    }

    @Delegate
    class TestSubject(dbService: DBService) extends DBService

    new TestSubject(new DBServiceImpl).retrieveRecord(0) shouldBe "FooBar"
  }

  "Annottee" should "still contain declared member not belonging to interface" in {
    class DBServiceImpl extends DBService {
      override def retrieveRecord(id: Int): String = "FooBar"
    }

    @Delegate
    class TestSubject(dbService: DBService) extends DBService {
      def methodA(foo: Int, bar: String): Boolean = true
    }

    new TestSubject(new DBServiceImpl)
      .methodA(0, "<not-relevant>") shouldBe true
  }

  "Annottee" should "be able to contain parameters other than the proxy" in {
    class DBServiceImpl extends DBService {
      override def retrieveRecord(id: Int): String = "FooBar"
    }

    @Delegate
    class TestSubject(
        dbService: DBService,
        val otherMember: String = "<not-relevant>"
    ) extends DBService {
      def methodA(foo: Int, bar: String): Boolean = true
    }

    val instance = new TestSubject(new DBServiceImpl)
    instance.methodA(0, "<not-relevant>") shouldBe true
    instance.retrieveRecord(0) shouldBe "FooBar"
    instance.otherMember shouldBe "<not-relevant>"
  }

  it should "be able to contain such parameters before the delegatee" in {
    class DBServiceImpl extends DBService {
      override def retrieveRecord(id: Int): String = "FooBar"
    }

    @Delegate
    class TestSubject(otherMember: String, dbService: DBService) extends DBService {
      def methodA(foo: Int, bar: String): Boolean = true
    }

    val instance = new TestSubject("<not-relevant>", new DBServiceImpl)
    instance.methodA(0, "<not-relevant>") shouldBe true
    instance.retrieveRecord(0) shouldBe "FooBar"
  }

  "Annottee" should "be able to provide its own implementations" in {
    class FooImpl extends Foo {
      override def bar(a1: Int, a2: String)(b1: String, b2: Int): String =
        "FooImpl.bar"

      override def baz(a: Int, b: String): String = "FooImpl.baz"
    }

    @Delegate
    class TestSubject(arg: String, delegatee: Foo) extends Foo {
      def baz(a: Int, b: String): String = arg
    }

    val testSubject = new TestSubject("Eureka", new FooImpl)
    testSubject.baz(0, "<not-relevant>") shouldBe "Eureka"
    testSubject.bar(0, "<not-relevant>")("<not-relevant>", 0) shouldBe "FooImpl.bar"
  }

  it should "be able to provide also an overloaded method" in {
    class DBServiceImpl extends DBService {
      override def retrieveRecord(id: Int): String = "FooBar"
    }

    @Delegate
    class TestSubject(a: DBService) extends DBService {
      def retrieveRecord(id: String): String = "Overloaded-Method"
    }

    new TestSubject(new DBServiceImpl).retrieveRecord(2) shouldBe "FooBar"
    new TestSubject(new DBServiceImpl).retrieveRecord("irrelevant") shouldBe "Overloaded-Method"
  }

  "Delegation" should "be possible for methods with multi parameter lists" in {
    class FooImpl extends Foo {
      override def bar(a1: Int, a2: String)(b1: String, b2: Int): String =
        s"FirstList(param1: $a1, param2: $a2); SecondList(param1: $b1, param2: $b2)"

      override def baz(a: Int, b: String): String = "not-relevant"
    }

    @Delegate
    class TestSubject(delegatee: Foo) extends Foo

    new TestSubject(new FooImpl).bar(1, "Carlos")("Manuel", 2) shouldBe
      "FirstList(param1: 1, param2: Carlos); SecondList(param1: Manuel, param2: 2)"
  }

  "Annottee" should "use the interface's non-abstract declaration and not the delegatee's" in {
    class FooImpl extends Foo {
      def bar(a1: Int, a2: String)(b1: String, b2: Int): String =
        s"FirstList(param1: $a1, param2: $a2); SecondList(param1: $b1, param2: $b2)"

      def baz(a: Int, b: String): String = "not-relevant"

      override def barBaz(a: String): String = s"FooImpl-$a"
    }

    @Delegate
    class TestSubject(delegatee: Foo) extends Foo

    new TestSubject(new FooImpl)
      .barBaz("Hello") shouldBe "Default-Implementation-Hello"
  }

  // For some reason (I think scope of each unit test), this class can't be inside the unit test where it is used.
  class SpecificFooImpl extends Foo {

    def bar(a1: Int, a2: String)(b1: String, b2: Int): String =
      s"FirstList(param1: $a1, param2: $a2); SecondList(param1: $b1, param2: $b2)"

    def baz(a: Int, b: String): String = "not-relevant"

    override def barBaz(a: String): String = s"FooImpl-$a"
  }

  "Delegatee type" should "be able to be a subtype of the interface" in {

    @Delegate
    class TestSubject(delegatee: SpecificFooImpl) extends Foo

    new TestSubject(new SpecificFooImpl)
      .barBaz("Hello") shouldBe "Default-Implementation-Hello"
  }

  trait Foo2 extends Foo {
    def doesNotExistOnFoo[A](i: String)(j: A): String
  }

  "Delegation" should "also occur for all abstract declarations along the class hierarchy of the interface" in {
    class FooImpl extends Foo2 {
      def bar(a1: Int, a2: String)(b1: String, b2: Int): String =
        s"FirstList(param1: $a1, param2: $a2); SecondList(param1: $b1, param2: $b2)"

      def baz(a: Int, b: String): String = "not-relevant"

      override def barBaz(a: String): String = s"FooImpl=$a"

      override def doesNotExistOnFoo[A](i: String)(j: A): String = s"FooImpl.doesNotExistOnFoo=${i + j.toString}"
    }

    @Delegate
    class MyFoo2(delegatee: Foo2) extends Foo2

    new MyFoo2(new FooImpl).doesNotExistOnFoo("Monkey")(true) shouldBe s"FooImpl.doesNotExistOnFoo=Monkeytrue"
    new MyFoo2(new FooImpl).barBaz("Monkey") shouldBe s"Default-Implementation-Monkey"
    new MyFoo2(new FooImpl).baz(30, "Baz") shouldBe "not-relevant"
    new MyFoo2(new FooImpl).bar(30, "Bar")("Qux", 35) shouldBe
      s"FirstList(param1: 30, param2: Bar); SecondList(param1: Qux, param2: 35)"
  }

}
