package org.k33nteam.jade.propagation.base

import org.k33nteam.jade.bean.VulnResult
import org.k33nteam.jade.propagation.track.APIVulnManager
import soot.jimple.internal.JAssignStmt
import soot.{Local, Scene}
import soot.toolkits.graph.BriefUnitGraph
import soot.toolkits.scalar.{SimpleLiveLocals, SmartLocalDefs}
import scala.collection.JavaConversions._
import soot.jimple.{StaticFieldRef, Constant}

import scala.collection.mutable.ListBuffer
import org.k33nteam.jade.helpers.SyntaxHelpers._
/**
 * Created by hqd on 12/21/14.
 */
class NaiveAPIChecker(cfgPath: String) {

  val apiManager = new APIVulnManager(cfgPath)

  //need test
  def doCheck(scene: Scene): Iterable[VulnResult] = {
    val result = new ListBuffer[VulnResult]
    for (sootClass <- scene.getApplicationClasses; sm <- sootClass.getMethods; if sm.hasActiveBody; stmt <- sm.getActiveBody.getUnits; if stmt.containsInvokeExpr) {
      val target = apiManager.getVulnParamAsSource(stmt)
      if (target != null) {
        val funcSig: String = stmt.getInvokeExpr.getMethod.getSignature
        val valueString: String = target.toString
        if (target.isInstanceOf[Constant] || target.isInstanceOf[StaticFieldRef]) {
          //ensure constant
          if (apiManager.evaluateResult(funcSig, valueString)) {
            result += VulnResult.toConstantAPIVulnResult(stmt, sm, apiManager.getDescBySig(funcSig), "naive check, may false positive")
          }
        }
        else {
          //we need to do a local backtrack
          val unitGraph = new BriefUnitGraph(sm.getActiveBody)
          val analysis = new SmartLocalDefs(unitGraph, new SimpleLiveLocals(unitGraph))
          target match {
            case local: Local =>
              val defs = analysis.getDefsOfAt(local, stmt)
              defs.find(defunit => defunit.isInstanceOf[JAssignStmt] && apiManager.evaluateResult(funcSig, defunit.asInstanceOf[JAssignStmt].getRightOp.toString)) match {
                case Some(defunit) => result += VulnResult.toConstantAPIVulnResult(stmt, sm, apiManager.getDescBySig(funcSig), "naive check found at defsite")
                case None =>
              }
            case _ =>
          }
        }
      }
    }
    result
  }
}
