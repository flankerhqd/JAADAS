package org.k33nteam.jade.propagation.methodplugins

import org.k33nteam.jade.propagation.base.IAPICheckPlugin
import org.k33nteam.jade.helpers.SyntaxHelpers._
import org.k33nteam.jade.bean.VulnResult
import soot.Scene
import soot.SootMethod
import scala.collection.mutable.Map
import soot.jimple.InstanceInvokeExpr
import soot.jimple.IntConstant
import soot.jimple.Stmt

import scala.collection.JavaConversions._

object WebviewFileDomainPlugin extends IAPICheckPlugin{
  private val ALLOW_FILE_ACCESS: Int = 0x1
  private val ALLOW_UNIVERSAL_ACCESS_FROM_FILE_URLS: Int = 0x10
  private val ALLOW_FILE_ACCESS_FROM_FILE_URLS: Int = 0x100
  private val collections: Map[String, Integer] = Map(
    "void setAllowFileAccess(boolean)" -> 0x1,
    "void setAllowFileAccessFromFileURLs(boolean)" -> 0x10,
    "void setAllowUniversalAccessFromFileURLs(boolean)" -> 0x100
  )

  private def translateLevelToString(level: Int): String = {
    val builder: StringBuilder = new StringBuilder
    if ((level & ALLOW_FILE_ACCESS) != 0) {
      builder.append(" allow-file-access\n")
    }
    if ((level & ALLOW_UNIVERSAL_ACCESS_FROM_FILE_URLS) != 0) {
      builder.append(" allow-universal-access-from-file-urls\n")
    }
    if ((level & ALLOW_FILE_ACCESS_FROM_FILE_URLS) != 0) {
      builder.append(" allow-file-access-from-file-urls\n")
    }
    return builder.toString
  }

  def check(scene: Scene) : Map[SootMethod, (String,Int)] = {
    val results= Map[SootMethod, (String,Int)]()
    for (sootClass <- scene.getApplicationClasses) {
      for (sootMethod <- sootClass.getMethods; if sootMethod.hasActiveBody) {
        var ret: Int = 0
        for (stmt <- sootMethod.getActiveBody.getUnits) {
          if (stmt.containsInvokeExpr && stmt.getInvokeExpr.isInstanceOf[InstanceInvokeExpr]) {
            val instanceInvokeExpr: InstanceInvokeExpr = stmt.getInvokeExpr.asInstanceOf[InstanceInvokeExpr]
            val invokeName: String = instanceInvokeExpr.getMethod.getSubSignature
            if ((collections contains invokeName) && instanceInvokeExpr.getArgCount == 1 && instanceInvokeExpr.getArg(0).isInstanceOf[IntConstant]) {
              val intConstant: IntConstant = instanceInvokeExpr.getArg(0).asInstanceOf[IntConstant]
              if (intConstant.value == 1) {
                ret = ret | collections.getOrElse[Integer](invokeName, 0)
              }
            }
          }
        }
        if (ret != 0) {
          results += (sootMethod -> (translateLevelToString(ret),ret))
        }
      }
    }

    results
  }

  override def getResult(scene: Scene): Iterable[VulnResult] =
  {
    val methodmaps = check(scene)
    methodmaps.map({
      case (method, (string, weight)) => VulnResult.toMethodAPIVulnResult(method, getDesc, string, score = weight*0.1f)
    })
  }


  override def getDesc(): String = "Webview js file access misconfigurations"

}
