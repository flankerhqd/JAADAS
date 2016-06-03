package org.k33nteam.jade.propagation.methodplugins

import org.k33nteam.jade.propagation.base.{BasicAPICheckPlugin, IAPICheckPluginImpl, IAPICheckPlugin}
import soot.SootMethod

object CustomVerifierPluginImpl extends IAPICheckPluginImpl{
  private val DESC: String = "implements custom verifier that always return true"
  private val targetClassSig: String = "javax.net.ssl.HostnameVerifier"
  private val targetMethodSubSig: String = "boolean verify(java.lang.String,javax.net.ssl.SSLSession)"
  private final val func:SootMethod => Boolean = (method : SootMethod) => true

  override def getPlugin(): IAPICheckPlugin = {
    BasicAPICheckPlugin(DESC, targetClassSig, targetMethodSubSig, func)
  }
}