package com.cmhteixeira.delegatemacro

import org.scalatest.{FlatSpec, Matchers}

trait MonkeyBiz[Req, Res] {
  def get(url: String): Res
  def post(req: Req): Res
}

trait MonkeyBiz2[Req, Res] {
  def delete[A](a: A): Option[Res]
  def delete2[A](a: A): Option[Res]
  def get(url: String): Res
  def post(req: Req): Res
}

class InterfaceHasTypeParametersSpec extends FlatSpec with Matchers {
  it should "work when the interface has type parameters" in {
    class Delegatee extends MonkeyBiz[String, String] {
      override def get(url: String): String = "Delegatee.get"
      override def post(req: String): String = "Delegatee.post"
    }

    @Delegate
    class Annotatee(delegatee: MonkeyBiz[String, String]) extends MonkeyBiz[String, String]

    new Annotatee(new Delegatee).get("https://oss.sonatype.com") shouldBe "Delegatee.get"
    new Annotatee(new Delegatee).post("Body of Request") shouldBe "Delegatee.post"
  }

  it should "work if the annotatee has type parameters, provided the interface is fully instantiated" in {
    class Delegatee extends MonkeyBiz[String, String] {
      override def get(url: String): String = "Delegatee.get"
      override def post(req: String): String = "Delegatee.post"
    }

    case class Person(name: String, age: Int)

    @Delegate
    class Annotatee[Foo](foo: Foo, delegatee: MonkeyBiz[String, String]) extends MonkeyBiz[String, String] {
      def giveMeFoo: Foo = foo
    }

    new Annotatee[Person](foo = Person("Michael", 35), new Delegatee)
      .get("https://oss.sonatype.com") shouldBe "Delegatee.get"
    new Annotatee[Person](foo = Person("Michael", 35), new Delegatee).post("Body Of Request") shouldBe "Delegatee.post"
    new Annotatee[Person](foo = Person("Michael", 35), new Delegatee).giveMeFoo shouldBe Person("Michael", 35)

  }

  it should "work for methods which are parameterized" in {
    class Delegatee extends MonkeyBiz2[String, String] {
      override def get(url: String): String = "Delegatee.get"
      override def post(req: String): String = "Delegatee.post"
      override def delete[A](a: A): Option[String] = Some("Delegatee.delete")
      override def delete2[A](a: A): Option[String] = Some("Delegatee.delete2")
    }

    case class Person(name: String, age: Int)

    @Delegate
    class Annotatee[Foo](foo: Foo, delegatee: MonkeyBiz2[String, String]) extends MonkeyBiz2[String, String] {
      def giveMeFoo: Foo = foo

      override def delete2[A](a: A): Option[String] = Some(foo.toString)
    }

    new Annotatee[Person](foo = Person("Michael", 35), new Delegatee).delete("irrelevant") shouldBe
      Some("Delegatee.delete")

    new Annotatee[Person](foo = Person("Michael", 35), new Delegatee).delete2("irrelevant") shouldBe
      Some("""Person(Michael,35)""")

  }
}
