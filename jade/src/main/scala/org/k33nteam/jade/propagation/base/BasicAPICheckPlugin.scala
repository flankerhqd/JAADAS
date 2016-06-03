package org.k33nteam.jade.propagation.base

import org.k33nteam.jade.bean.VulnResult
import soot.{Scene, SootClass, SootMethod}

import scala.collection.JavaConversions._
import scala.collection.immutable.List
trait IAPICheckPlugin
{
  def getResult(scene: Scene): Iterable[VulnResult]
  def getDesc: String
}
trait IAPICheckPluginImpl
{
  def getPlugin: IAPICheckPlugin
}
class BasicAPICheckPlugin (Desc: String, targetClassSig: String, targetMethodSubSig: String, methodCheckFunc: SootMethod => Boolean) extends IAPICheckPlugin
{
  require(targetClassSig != null, "targetClassSig is null")
  require(targetMethodSubSig != null, "targetMethodSubSig is null")
  require(Desc != null, "Desc is null")
  override def getDesc(): String = {
    Desc
  }
  
  override def getResult(scene: Scene): Iterable[VulnResult] = {
    val targetClass = scene.getSootClass(targetClassSig)
    if (targetClass.isPhantom) {
      return List.empty
    }
    val fastHierarchy = scene.getOrMakeFastHierarchy
    if(targetClass.isInterface)
    {
      scene.getFastHierarchy.getAllImplementersOfInterface(targetClass).map(sc => (getVulnMethods(sc) ++ (fastHierarchy.getSubclassesOf(sc).map(subc => getVulnMethods(subc)).flatten))).flatten.map(sm => VulnResult.toMethodAPIVulnResult(sm,getDesc,""))
    }
    else{
      scene.getFastHierarchy.getSubclassesOf(targetClass).map(tc => getVulnMethods(tc)).flatten.map(sm => VulnResult.toMethodAPIVulnResult(sm,getDesc,""))
    }
  }

  private def getVulnMethods(sootClass:SootClass): Seq[SootMethod] ={
     sootClass.getMethods.filter(m => (m.getSubSignature == targetMethodSubSig) && methodCheckFunc(m))
  }
}

object  BasicAPICheckPlugin
{
  def apply(Desc: String, targetClassSig: String, targetMethodSig: String, methodCheckFunc: SootMethod => Boolean) = new BasicAPICheckPlugin(Desc, targetClassSig, targetMethodSig, methodCheckFunc)
}