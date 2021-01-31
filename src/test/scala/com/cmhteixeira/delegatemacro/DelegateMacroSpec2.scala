package com.cmhteixeira.delegatemacro

import org.scalatest.{FlatSpec, Matchers}
import com.cmhteixeira.delegatemacro.foo.bar.Animal

class DelegateMacroSpec2 extends FlatSpec with Matchers {

  "Interfaces that do not inherit and only abstract methods, and classes with no declarations" should "delegate to member" in {
    class Dog extends Animal {
      override def makeNoise(): String = "woof woof"
      override def eat(food: String): String = s"Dog eats delicious $food"
    }

    @Delegate
    class TestSubject(anotherAnimal: Animal) extends Animal

    val dog = new TestSubject(new Dog)
    dog.makeNoise() shouldBe "woof woof"
    dog.eat("steak") shouldBe "Dog eats delicious steak"
  }

}
