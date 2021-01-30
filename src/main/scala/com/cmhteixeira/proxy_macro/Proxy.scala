package com.cmhteixeira.proxy_macro

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

/**
  * Bla bla bla
  */
@compileTimeOnly("enable macro paradise to expand macro annotations")
class Proxy extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro identityMacro.impl
}

object identityMacro {

  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val annotteeClass: ClassDef = annottees.map(_.tree).toList match {
      case (claz: ClassDef) :: Nil => claz
      case _ =>
        throw new Exception(
          "Unexpected annottee. This annotation applies only to class definitions."
        )
    }

    val (annotteeClassParents, annotteeClassParams, annotteeClassDefinitions) =
      annotteeClass match {
        case q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns } with ..$parents { $self => ..$stats }" =>
          (parents, paramss, stats)
      }

    val superClassTypedTree = annotteeClassParents.toList match {
      case head :: _ =>
        c.typecheck(head, mode = c.TYPEmode)
    }

    val delegateParamOpt: Option[(Type, ValDef)] = annotteeClassParams
      .asInstanceOf[List[List[Tree]]]
      .flatten
      .flatMap {
        case tree @ q"$mods val $tname1: $tpt = $expr" =>
          val theType = c.typecheck(tpt, mode = c.TYPEmode).tpe
          if (theType =:= superClassTypedTree.tpe)
            Some((theType, tree.asInstanceOf[ValDef]))
          else None
        case _ => None
      }
      .headOption

    val (delegateParamType, delegateParamTree) = delegateParamOpt.getOrElse(
      throw new Exception(
        s"Annottee does not contain any parameter of type ${superClassTypedTree.tpe.termSymbol.fullName}"
      )
    )

    val declarationsInterface = delegateParamType.decls.toList.collect {
      case i if i.isMethod => i.asMethod
    }

    val methodsOfAnnottee: Seq[DefDef] = annotteeClassDefinitions.collect {
      case defDef: DefDef => defDef
    }
    val interfaceMethods =
      declarationsInterface
        .filter(_.isAbstract)
        .filter(i =>
          !methodsOfAnnottee
            .exists(methodOfAnnottee => helper(c)(methodOfAnnottee, i))
        )
        .map { methodDecl =>
          val paramss = methodDecl.paramLists.map(
            _.map(param => q"${param.name.toTermName}: ${param.typeSignature}")
          )
          val tparams = methodDecl.typeParams.map(i => internal.typeDef(i))

          q"def ${methodDecl.name}[..$tparams](...$paramss): ${methodDecl.returnType} = ${delegateParamTree.name}.${methodDecl.name}(...${methodDecl.paramLists
            .map(_.map(_.name.toTermName))})"
        }

    val resTree = annotteeClass match {
      case q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns } with ..$parents { $self => ..$stats }" =>
        q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns } with ..$parents { $self => ..${stats.toList ::: interfaceMethods} }"
    }

    c.Expr[Any](resTree)
  }

  private def helper(c: whitebox.Context)(
      annotteeMethod: c.universe.DefDef,
      interfaceMethod: c.universe.MethodSymbol
  ): Boolean = {
    import c.universe._
    val sameName = annotteeMethod.name == interfaceMethod.name

    // we remove the body because it can contain references that, at this score, the type checker could not verify.
    val annotteeMethodNoBody = annotteeMethod match {
      case DefDef(modifiers, name, tparams, vparamss, tpt, rhs) =>
        DefDef(modifiers, name, tparams, vparamss, tpt, EmptyTree)
    }

    val sameSignature = interfaceMethod.asMethod.typeSignature =:= (c.typecheck(annotteeMethodNoBody, mode = c.TERMmode) match {
      case a: DefDef => a
    }).symbol.asMethod.typeSignature

    sameName && sameSignature
  }

}
