package org.k33nteam.jade.propagation.methodplugins

import org.k33nteam.jade.bean.VulnResult
import org.k33nteam.jade.propagation.base.IAPICheckPlugin
import org.k33nteam.jade.solver.LocalMustPropagationAnalysis
import soot.jimple.{Constant, IntConstant, Stmt}
import soot.jimple.internal.JArrayRef
import soot._
import soot.toolkits.graph.BriefUnitGraph

import org.k33nteam.jade.helpers.SyntaxHelpers._
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
 * Created by hqd on 12/19/14.
 */
object CheckSignatureFakeID extends IAPICheckPlugin{
  final val SCORE = 0.5f
  override def getResult(scene: Scene): Iterable[VulnResult] = {
    (for(sc <- scene.getApplicationClasses; sm <- sc.getMethods; if sm.hasActiveBody) yield checkSignatureFakeID(sm.getActiveBody)).flatten
  }

  private def checkSignatureFakeID(body: Body): Seq[VulnResult] = {
    val ret = ListBuffer[VulnResult]()
    val idxs = ListBuffer[(Stmt, Value)]()
    for (stmt <- body.getUnits) {
      if (stmt.containsArrayRef) {
        val ref: JArrayRef = stmt.getArrayRef.asInstanceOf[JArrayRef]
        val base: Value = ref.getBase
        val arrayType: ArrayType = base.getType.asInstanceOf[ArrayType]
        val elementType: Type = arrayType.getArrayElementType
        if (elementType.toString == "android.content.pm.Signature") {
          idxs += ((stmt, ref.getIndex))
        }
      }
    }
    lazy val analysis: LocalMustPropagationAnalysis = new LocalMustPropagationAnalysis(new BriefUnitGraph(body))
    for ((stmt, value) <- idxs) {

      value match {
        case intConst: IntConstant =>
          if(intConst.value != 0)
          {
            ret += VulnResult.toConstantAPIVulnResult(stmt, body.getMethod, "fakeid reloaded vuln", "at fix addr not 0", score = SCORE)
          }
        case idxLocal: Local =>
          val sourcevalue: Constant = analysis.getResultAtStmt(idxLocal, stmt)
          sourcevalue match {
            case intConst: IntConstant => {
              if (intConst.value != 0) {
                ret += VulnResult.toConstantAPIVulnResult(stmt, body.getMethod, "fakeid reloaded vuln", "", score = SCORE)
              }
            }
            case _ =>
          }
        case _ =>
      }
    }
    ret.toList
  }
  override def getDesc(): String = "FAKEID reloaded vulnerability"
}