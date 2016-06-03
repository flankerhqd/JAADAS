package org.k33nteam.jade.bean

import org.k33nteam.jade.bean.VulnKind.vulnKind
import soot.SootMethod
import soot.jimple.Stmt
import spray.json.{JsValue, DefaultJsonProtocol, JsonFormat, JsNumber}

object VulnKind extends Enumeration {
  type vulnKind = Value
  val APIVULN, CRASHVULN, DATAFLOWINVULN, GENERAL, DATAFLOWOUTVULN, REACHABILITYVULN = Value
  val NPE_CRASH = "NPE_CRASH"
  val CAST_CRASH = "CAST_CRASH"
  val INDEXOUTOFBOUND_CRASH = "IDX_OUTOFBOUND_CRASH"
}
class VulnResult  {
  var vulnKind: VulnKind.vulnKind = VulnKind.GENERAL
  var source: (Option[Stmt], Option[SootMethod]) = (None, None)
  var dest: Option[(Stmt, SootMethod)] = None
  var desc: Option[String] = None
  var custom: Option[String] = None
  var path: List[(Stmt, SootMethod)] = List[(Stmt, SootMethod)]()
  var scoring: Float = 1.0f
}
case class VulnRepresent(vulnKind: VulnKind.vulnKind,sourceStmt: String, sourceMethod: String,destStmt: String,destMethod: String,desc: String, paths: List[(String,String)], custom: String)

object VulnRepresent{
  def toVulnRepresent(result: VulnResult): VulnRepresent =
  {
    val fuck = new VulnRepresent(result.vulnKind, result.source._1.getOrElse("").toString, result.source._2.getOrElse("").toString,
      result.dest.getOrElse(("",""))._1.toString,
      result.dest.getOrElse(("",""))._2.toString,
      result.desc.getOrElse(""),
      result.path.map(p => (p._1.toString(), p._2.toString())),
      result.custom.getOrElse("")
    )
    println(fuck)
    fuck
  }
}

object VulnResult{
  def toMethodAPIVulnResult(method: SootMethod, desc: String, custom: String, score: Float=0.5f): VulnResult = {
    val result = new VulnResult
    result.desc = Some(desc)
    result.source = (None, Some(method))
    result.vulnKind = VulnKind.APIVULN
    result.custom = Some(custom)
    result.scoring = score
    result
  }

  def toConstantAPIVulnResult(stmt: Stmt, method: SootMethod, desc: String, custom: String, score: Float = 0.3f): VulnResult = {
    val result = new VulnResult
    result.desc = Some(desc)
    result.source = (Some(stmt), Some(method))
    result.vulnKind = VulnKind.APIVULN
    result.custom = Some(custom)
    result.scoring = score
    result
  }

  def toReachabilityVulnResult(stmt: Stmt, method: SootMethod, score: Float=0): VulnResult={
    val result = new VulnResult
    result.desc = None
    result.source = (Some(stmt), Some(method))
    result.vulnKind = VulnKind.REACHABILITYVULN
    result.scoring = score
    result
  }

  def toCrashVulnResult(stmt: Stmt, method: SootMethod, desc: String, score: Float=0): VulnResult={
    val result = new VulnResult
    result.desc = Some(desc)
    result.vulnKind = VulnKind.CRASHVULN
    result.source = (Option(stmt), Some(method))
    result.scoring = score
    result
  }

  def toDataFlowVuln(sourceStmt: Stmt, sourceMethod: SootMethod, targetStmt: Stmt, targetMethod: SootMethod, desc: String, path: List[(Stmt,SootMethod)] = List(), score: Float=5f): VulnResult={
    val result = new VulnResult
    result.source = (Some(sourceStmt), Some(sourceMethod))
    result.dest = Some(targetStmt, targetMethod)
    result.vulnKind = VulnKind.DATAFLOWINVULN
    result.desc = Some(desc)
    result.scoring = score
    result.path = path
    result
  }

  def toDataFlowOutVuln(sourceStmt: Stmt, sourceMethod: SootMethod, targetStmt: Stmt, targetMethod: SootMethod, desc: String, score: Float=0): VulnResult={
    val result = new VulnResult
    result.source = (Some(sourceStmt), Some(sourceMethod))
    result.dest = Some(targetStmt, targetMethod)
    result.vulnKind = VulnKind.DATAFLOWOUTVULN
    result.desc = Some(desc)
    result.scoring = score
    result
  }

  def toManifestConfigVuln(desc: String): VulnResult = {
    val result = new VulnResult
    result.desc = Some(desc)
    result.scoring = 5.0f
    result
  }
}