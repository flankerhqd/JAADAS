package org.k33nteam.jade.propagation.methodplugins

import org.k33nteam.jade.bean.VulnResult
import org.k33nteam.jade.propagation.base.IAPICheckPlugin
import soot.jimple.{IntConstant, Stmt}
import soot.{SootMethod, SootClass, Scene}

import scala.collection.JavaConversions._
/**
 * Created by hqd on 12/22/14.
 */
object WebviewSavePwdPlugin extends IAPICheckPlugin{
  val getsettings_sig = "<android.webkit.WebView: android.webkit.WebSettings getSettings()>"
  val setpwd_sig = "<android.webkit.WebSettings: void setSavePassword(boolean)>"
  final val SCORE = 0.1f
  override def getResult(scene: Scene): Iterable[VulnResult] = {
    scene.getApplicationClasses.filter(checkClassSuspicious).filter(!checkClassNotVulnerable(_)).map(sc => VulnResult.toMethodAPIVulnResult(findGetSettingsMethod(sc),getDesc,"", score = SCORE))
  }


  private def checkClassSuspicious(sootClass:SootClass): Boolean =
  {
    for(sm <- sootClass.getMethods; if sm.hasActiveBody; u <- sm.getActiveBody.getUnits; if u.asInstanceOf[Stmt].containsInvokeExpr) {
      u match {
        case stmt: Stmt =>
          //System.out.println("check class suspicious: " + stmt.getInvokeExpr.getMethod.getSignature)
          if (stmt.getInvokeExpr.getMethod.getSignature == getsettings_sig) {
            //System.out.println("found getsettings sig")
            return true
          }
        case _ =>
      }
    }
    false
  }

  private def checkClassNotVulnerable(sootClass: SootClass): Boolean =
  {
    for(sm <- sootClass.getMethods; if sm.hasActiveBody; u <- sm.getActiveBody.getUnits; if u.asInstanceOf[Stmt].containsInvokeExpr) {
      u match {
        case stmt: Stmt =>
          if (stmt.getInvokeExpr.getMethod.getSignature == setpwd_sig) {
            stmt.getInvokeExpr.getArg(0) match {
              case intval: IntConstant =>
                if (intval.value == 0) {
                  return true
                }
              case _ =>
            }
          }
        case _ =>
      }
    }
    false
  }

  private def findGetSettingsMethod(sootClass: SootClass): SootMethod ={
    sootClass.getMethods.filter(_.hasActiveBody).find(_.getActiveBody.getUnits.exists {
      case stmt: Stmt => {
        stmt.containsInvokeExpr && stmt.getInvokeExpr.getMethod.getSignature == getsettings_sig
      }
      case _ => false
    }).get
  }

  override def getDesc(): String = "Check webview save password disabled or not"
}
