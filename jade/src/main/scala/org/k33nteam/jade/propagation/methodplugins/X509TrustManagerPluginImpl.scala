package org.k33nteam.jade.propagation.methodplugins

import org.k33nteam.jade.propagation.base.{IAPICheckPluginImpl, BasicAPICheckPlugin}
import soot.SootMethod
import soot.jimple.{ThrowStmt, Stmt}
import scala.collection.JavaConversions._

/*
 * class bv
  implements X509TrustManager
{
  bv(bu parambu) {}

  public void checkClientTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString) {// Do nothing -> accept any certificates}

  public void checkServerTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString) {// Do nothing -> accept any certificates}

  public X509Certificate[] getAcceptedIssuers()
  {
    return null;
  }
}
 */
//potiential false positive: http://stackoverflow.com/questions/19005318/implementing-x509trustmanager-passing-on-part-of-the-verification-to-existing
object X509TrustManagerPluginImpl extends IAPICheckPluginImpl
{
  private final val targetClassSig: String = "javax.net.ssl.X509TrustManager"
  private final val targetMethodSubSig: String = "void checkServerTrusted(java.security.cert.X509Certificate[],java.lang.String)"
  private final val DESC: String = "X509TrustManager empty impl, lead to SSL vulnerability"

  protected def checkSpecificMethod(sootMethod: SootMethod): Boolean = {
    !sootMethod.getActiveBody.getUnits.exists(u => u.isInstanceOf[ThrowStmt])
  }
  override def getPlugin(): BasicAPICheckPlugin = {
    BasicAPICheckPlugin(DESC, targetClassSig, targetMethodSubSig, checkSpecificMethod)
  }

}