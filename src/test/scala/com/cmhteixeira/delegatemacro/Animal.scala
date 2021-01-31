package com.cmhteixeira.delegatemacro {

  package foo {

    package bar {

      trait Animal {
        def makeNoise(): String
        def eat(food: String): String
      }
    }
  }
}
