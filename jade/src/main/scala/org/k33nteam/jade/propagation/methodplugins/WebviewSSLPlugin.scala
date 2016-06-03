package org.k33nteam.jade.propagation.methodplugins

import org.k33nteam.jade.propagation.base.{IAPICheckPluginImpl, BasicAPICheckPlugin}
import soot.SootMethod
import soot.jimple.Stmt

import scala.collection.JavaConversions._

import org.k33nteam.jade.helpers.SyntaxHelpers._
object WebviewSSLPluginImpl extends IAPICheckPluginImpl{
  private final val DESC: String = "Webview ssl handler impl onReceivedSslError, lead to SSL vulnerability"
  private final val targetMethodSubSig: String = "void onReceivedSslError(android.webkit.WebView,android.webkit.SslErrorHandler,android.net.http.SslError)"
  private final val targetClassSig: String = "android.webkit.WebViewClient"

  protected def checkSpecificMethod(sootMethod: SootMethod): Boolean = {
    for(stmt <- sootMethod.getActiveBody.getUnits; if stmt.containsInvokeExpr)
    {
      if(stmt.getInvokeExpr.getMethod.getName == "proceed")
      {
        return true
      }
    }
    false
  }

  override def getPlugin(): BasicAPICheckPlugin = {
    BasicAPICheckPlugin(DESC, targetClassSig, targetMethodSubSig, checkSpecificMethod)
  }
}