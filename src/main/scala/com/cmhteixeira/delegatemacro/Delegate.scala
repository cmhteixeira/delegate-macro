package com.cmhteixeira.delegatemacro

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import scala.util.Success

/** Macro to proxy implementation of abstract methods to a dependency.
  *
  * ==Motivation==
  * Apply this macro to a class that implements an interface with abstract methods. At compile-time, the macro will
  * implement the interface methods on your class using a dependency that you inject on that class, with the exception
  * of the methods you implement manually on the source code.
  * The value is in saving you from the tedious task of doing that yourself. It is the more usefull the more methods
  * in the interface there are.
  *
  * ==Example==
  * {{{
  * trait Connection {
  *   def method1(a: String): String
  *   def method2(a: String): String
  *   // 96 other abstract methods
  *   def method100(a: String): String
  * }
  *
  * @Delegate
  * class MyConnection(delegatee: Connection) extends Connection {
  *   def method10(a: String): String = "Only method I want to implement manually"
  * }
  *
  * // The source code above would be equivalent, after the macro expansion, to the code below
  * class MyConnection(delegatee: Connection) extends Connection {
  *   def method1(a: String): String = delegatee.method1(a)
  *   def method2(a: String): String = delegatee.method2(a)
  *   def method10(a: String): String = "Only method I need to implement manually"
  *   // 96 other methods that are proxied to the dependency delegatee
  *   def method100(a: String): String = = delegatee.method100(a)
  * }
  *
  * }}}
  *
  *
  */
@compileTimeOnly("enable macro paradise to expand macro annotations")
class Delegate(verbose: Boolean = false) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro delegateMacro.impl
}

object delegateMacro {

  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val isVerbose = c.prefix.tree match {
      case Apply(_, q"verbose = $foo" :: Nil) =>
        foo match {
          case Literal(Constant(verbose: Boolean)) => verbose
          case _ =>
            c.warning(
              c.enclosingPosition,
              "The value provided for 'verbose' must be a constant (true or false) and not an expression (e.g. 2 == 1 + 1). Verbose set to false."
            )
            false
        }
      case _ => false
    }

    val annotateeClass: ClassDef = annottees.map(_.tree).toList match {
      case (claz: ClassDef) :: Nil => claz
      case _ =>
        throw new Exception(
          "Unexpected annottee. This annotation applies only to class definitions."
        )
    }

    val (annotteeClassParents, annotteeClassParams, annotteeClassDefinitions) =
      annotateeClass match {
        case q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns } with ..$parents { $self => ..$stats }" =>
          (parents, paramss, stats)
      }

    val superClassTypedTree = annotteeClassParents.toList match {
      case head :: _ =>
        c.typecheck(head, mode = c.TYPEmode)
    }

    val delegateeParamOpt = annotteeClassParams
      .asInstanceOf[List[List[Tree]]]
      .flatten
      .flatMap {
        case tree @ q"$mods val $tname1: $tpt = $expr" =>
          val theTypeOpt = scala.util.Try { c.typecheck(tpt, mode = c.TYPEmode).tpe }.toOption
          theTypeOpt.flatMap { theType =>
            if (theType <:< superClassTypedTree.tpe)
              Some((theType, tree.asInstanceOf[ValDef]))
            else None
          }
        case _ => None
      }
      .headOption

    val (delegateeType, delegateeParamTree) = delegateeParamOpt.getOrElse {
      c.abort(
        c.enclosingPosition,
        s"Annottee does not contain any parameter of type ${superClassTypedTree.tpe.termSymbol.fullName} to which to delegate."
      )
    }

    val declarationsInterface = superClassTypedTree.tpe.decls.toList.collect {
      case i if i.isMethod => i.asMethod
    }

    val methodsOfAnnotatee: Seq[DefDef] = annotteeClassDefinitions.collect {
      case defDef: DefDef => defDef
    }

    val interfaceMethods =
      declarationsInterface
        .filter(_.isAbstract)
        .filter(interfaceDecl =>
          !methodsOfAnnotatee
            .exists(methodOfAnnotatee => helper(c)(methodOfAnnotatee, interfaceDecl, delegateeType))
        )
        .map { delegateeMethod =>
          val paramss = delegateeMethod
            .infoIn(delegateeType)
            .paramLists
            .map(
              _.map(param => q"${param.name.toTermName}: ${param.typeSignature}")
            )
          val tparams = delegateeMethod.typeParams.map(i => internal.typeDef(i))
          val resultType = delegateeMethod.typeSignatureIn(delegateeType).finalResultType

          q"def ${delegateeMethod.name}[..$tparams](...$paramss): $resultType = ${delegateeParamTree.name}.${delegateeMethod.name}(...${delegateeMethod.paramLists
            .map(_.map(_.name.toTermName))})"
        }

    val resTree = annotateeClass match {
      case q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns } with ..$parents { $self => ..$stats }" =>
        q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns } with ..$parents { $self => ..${stats.toList ::: interfaceMethods} }"
    }

    c.info(
      c.enclosingPosition,
      "\n###### Expanded macro ######\n" + resTree.toString() + "\n###### Expanded macro ######\n",
      force = isVerbose
    )

    c.Expr[Any](resTree)
  }

  private def helper(c: whitebox.Context)(
      annotateeMethod: c.universe.DefDef,
      delegateeMethod: c.universe.MethodSymbol,
      delegateeType: c.Type
  ): Boolean = {
    import c.universe._
    val sameName = annotateeMethod.name == delegateeMethod.name

    // we remove the body because it can contain references that, at this stage, the type checker could not verify.
    val annotteeMethodNoBody = annotateeMethod match {
      case DefDef(modifiers, name, tparams, vparamss, tpt, rhs) =>
        DefDef(modifiers, name, tparams, vparamss, tpt, EmptyTree)
    }

    val signatureAnnotatee = (scala.util.Try { c.typecheck(annotteeMethodNoBody, mode = c.TERMmode) } match {
      case Success(defdef: DefDef) => Some(defdef)
      case _ => None
    }).map(_.symbol.asMethod.typeSignature).getOrElse(NoType)

    val sameSignature = delegateeMethod.asMethod.infoIn(delegateeType) =:= signatureAnnotatee

    sameName && sameSignature
  }

}
