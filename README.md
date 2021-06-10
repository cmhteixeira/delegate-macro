# Delegate Macro &emsp; [![Build Status](https://www.travis-ci.com/cmhteixeira/delegate-macro.svg?branch=master)](https://www.travis-ci.com/cmhteixeira/delegate-macro) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.cmhteixeira/delegate-macro_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.cmhteixeira/delegate-macro_2.13)

## Motivation
This macro enables you to delegate/proxy the implementation of an interface to an injected dependency in a very straightforward way. It saves you from the tedious work of doing it manually.

Apply this macro to a class that implements an interface. At compile-time, the macro will
implement the interface methods on your class using a dependency that you inject on that class, with the exception
of the methods you implement manually on the source code.
The value is in saving you from the tedious task of doing that yourself. It is the more usefull the more methods
in the interface there are.

### Example

```scala
trait Connection {
  def method1(a: String): String
  def method2(a: String): String
  // 96 other abstract methods
  def method100(a: String): String
}

@Delegate
class MyConnection(delegatee: Connection) extends Connection {
  def method10(a: String): String = "Only method I want to implement manually"
}

// The source code above would be equivalent, after the macro expansion, to the code below
class MyConnection(delegatee: Connection) extends Connection {
  def method1(a: String): String = delegatee.method1(a)
  def method2(a: String): String = delegatee.method2(a)
  def method10(a: String): String = "Only method I need to implement manually"
  // 96 other methods that are proxied to the dependency delegatee
  def method100(a: String): String = delegatee.method100(a)
}
```

## Support

The artefacts have been uploaded to Maven Central. Alternatively, they are also available on the GitHub registry. 

| Library Version | Scala 2.11 | Scala 2.12 | Scala 2.13 |
|---------|------------|------------|------------|
| 0.3.0   | [![Maven Central](https://img.shields.io/maven-central/v/com.cmhteixeira/delegate-macro_2.11/0.3.0)](https://search.maven.org/artifact/com.cmhteixeira/delegate-macro_2.11/0.3.0/jar)        | [![Maven Central](https://img.shields.io/maven-central/v/com.cmhteixeira/delegate-macro_2.12/0.3.0)](https://search.maven.org/artifact/com.cmhteixeira/delegate-macro_2.12/0.3.0/jar)        | [![Maven Central](https://img.shields.io/maven-central/v/com.cmhteixeira/delegate-macro_2.13/0.3.0)](https://search.maven.org/artifact/com.cmhteixeira/delegate-macro_2.13/0.3.0/jar)        |
| 0.2.0   | [![Maven Central](https://img.shields.io/maven-central/v/com.cmhteixeira/delegate-macro_2.11/0.2.0)](https://search.maven.org/artifact/com.cmhteixeira/delegate-macro_2.11/0.2.0/jar)        | [![Maven Central](https://img.shields.io/maven-central/v/com.cmhteixeira/delegate-macro_2.12/0.2.0)](https://search.maven.org/artifact/com.cmhteixeira/delegate-macro_2.12/0.2.0/jar)        | [![Maven Central](https://img.shields.io/maven-central/v/com.cmhteixeira/delegate-macro_2.13/0.2.0)](https://search.maven.org/artifact/com.cmhteixeira/delegate-macro_2.13/0.2.0/jar)        |
| 0.1.0   | [![Maven Central](https://img.shields.io/maven-central/v/com.cmhteixeira/delegate-macro_2.11/0.1.0)](https://search.maven.org/artifact/com.cmhteixeira/delegate-macro_2.11/0.1.0/jar)        | [![Maven Central](https://img.shields.io/maven-central/v/com.cmhteixeira/delegate-macro_2.12/0.1.0)](https://search.maven.org/artifact/com.cmhteixeira/delegate-macro_2.12/0.1.0/jar)        | [![Maven Central](https://img.shields.io/maven-central/v/com.cmhteixeira/delegate-macro_2.13/0.1.0)](https://search.maven.org/artifact/com.cmhteixeira/delegate-macro_2.13/0.1.0/jar)        |

Importing the library into your build system (e.g gradle, sbt), is not enough. Before Scala 3, support for macros is a bit clunky. You need to follow an extra step.  

| Scala 2.11                                                  | Scala 2.12                                                 | Scala 2.13                                                           |
|-------------------------------------------------------------|------------------------------------------------------------|----------------------------------------------------------------------|
| Import macro paradise plugin  | Import macro paradise plugin | Enable compiler flag `-Ymacro-annotations` required |

### Using macro paradise plugin

Link to macro repo: https://github.com/scalamacros/paradise  

#### gradle
Add the following 3 portions to your build

```gradle
// build.gradle
.....
configurations {
   scalaCompilerPlugin
}

dependencies {
  scalaCompilerPlugin "org.scalamacros:paradise_<your-scala-version>:<plugin-version>"
}

tasks.withType(ScalaCompile) {
  scalaCompileOptions.additionalParameters = [
    "-Xplugin:" + configurations.scalaCompilerPlugin.asPath
  ]
}
.... 
```
where `<your-scala-version>` must be the full scala version. For example `2.12.13`, and not `2.12`.

If that doesn't work, google for alternatives.
#### sbt
It should be quite straightforward.  
Add the following line to your build. 
```
addCompilerPlugin("org.scalamacros" % "paradise_<your-scala-version>" % "<plugin-version>")
``` 
Where `<your-scala-version>` must be the full scala version. For example `2.12.13`, and not `2.12`. 

If that doesn't work, google for alternatives.

### Enabling `-Ymacro-annotations`

In version `2.13`, the functionality of macro paradise has been included in the scala compiler directly. However, you must still enable the compiler flag `-Ymacro-annotations`.



## IntelliJ IDEA

There is no IntelliJ support.  
This means, regardless of your Scala version, your IDE won't be able to expand the macro. Therefore, it will underline your annotated class with those red squiggly lines, stating your class does not implement all methods of the interface.  
Don't worry about that. It is aesthetically unpleaseant, but of no real consequence.   
The solution would be to develop a public plugin for Intellij for this macro.

![](./documentation/ExampleRedLinesIntellijSupport.png)


## Debug

Knowing what the macro does, either to increase your confidence that it is doing what you meant, or for debugging, you can compile your code in debug mode.
This will log what the macro expanded your class into.  
Achieving this depends on which build system you are using. If using gradle, run `gradlew compileScala -i`. The `-i` is for info. Check more information at the logging section of the gradle documentation.  
You can try and force debug mode with `@Delegate(verbose = true)`. However, this might not work if you build system is hiding the logs (as is the case for gradle without `-i`).  
Alternatively, you can use flag `-Ymacro-debug-verbose`. This logs even more detailed information. I believe in this scenario everything is dumped to standard output, so it might overcome any logging limitations of your build system.

