package com.cmhteixeira.delegatemacro

import org.scalatest.{FlatSpec, Matchers}

class MethodHasTypeParametersSpec extends FlatSpec with Matchers {

  trait Foo {
    def bar[A](a1: A, a2: A): A
  }

  trait Foo2 {
    def bar[A, B, C](a: A, b: B)(f: (A, B) => C): C
  }

  trait Foo3 {
    def bar[A](a1: A, a2: A): A
    def baz[A](a1: A, a2: A): A
  }

  "The macro" should "delegate methods parameterized by one type" in {
    class FooImpl extends Foo {
      override def bar[A](a1: A, a2: A): A = a1
    }

    @Delegate
    class TestSubject(delegatee: Foo) extends Foo

    new TestSubject(new FooImpl).bar("Qux", "Quux") shouldBe "Qux"
  }

  it should "delegate methods parameterized by more than 1 type" in {
    class FooImpl extends Foo2 {
      override def bar[A, B, C](a: A, b: B)(f: (A, B) => C): C = f(a, b)
    }

    @Delegate
    class TestSubject(delegatee: Foo2) extends Foo2

    new TestSubject(new FooImpl).bar("Foo", true)((a, b) => a + b.toString) shouldBe "Footrue"
  }

  it should "allow the implementation on the Annottee of parameterized methods" in {
    class FooImpl extends Foo3 {
      override def bar[A](a1: A, a2: A): A = a1

      override def baz[A](a1: A, a2: A): A = a1
    }

    @Delegate
    class TestSubject(delegatee: Foo3) extends Foo3 {
      def baz[A](a1: A, a2: A): A = a2
    }

    new TestSubject(new FooImpl).bar(1, 2) shouldBe 1
    new TestSubject(new FooImpl).baz(1, 2) shouldBe 2
  }
}
