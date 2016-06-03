package org.k33nteam.jade.solver

import org.k33nteam.jade.bean.VulnResult
import soot.jimple.internal.{JCastExpr, JAssignStmt, AbstractStmt}
import soot.jimple.toolkits.pointer.{LocalTypeSet, LocalMayAliasAnalysis}
import soot.toolkits.graph.{ExceptionalUnitGraph, ExceptionalBlockGraph, BriefUnitGraph}
import soot._
import soot.jimple.Stmt
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

import org.k33nteam.jade.helpers.SyntaxHelpers._

/**
 * Created by hqd on 12/19/14.
 */
object CastCrashDetector {

  /*
  checkCast flag determines whether we should add class-cast exception in results. Typically for exported components, we set checkCast to true, for all components, we set checkCast to false

  to cover nested-intent to maximum
   */
  def checkCast(body: Body, checkCast: Boolean): Iterable[VulnResult] =
  {
    val ret = ListBuffer[VulnResult]()
    val flowfacts = ListBuffer[(Stmt, Local)]()
    for(unit <- body.getUnits; if unit.isInstanceOf[JAssignStmt]; if unit.asInstanceOf[JAssignStmt].containsInvokeExpr)
    {

      def stmt = unit.asInstanceOf[JAssignStmt]
      stmt.getInvokeExpr.getMethod.getName match {
        case "getParcelableExtra" | "getSerializableExtra" | "getParcelable" | "getSerializable" | "getParcelableArrayExtra" =>
        stmt.getLeftOp match {
          case parcelable: Local => {
            flowfacts += ((stmt, parcelable))
          }
        }
        case _ =>
      }
    }

    lazy val graph = new BriefUnitGraph(body)
    lazy val analysis = new LocalMayAliasAnalysisWithArray(graph)
    lazy val typeAnalysis = new MyCastChecker(graph)
    lazy val classcastexception = Scene.v.getSootClass("java.lang.ClassCastException")
    for( (stmt, local)  <- flowfacts; checkStmt <- body.getUnits)
    {
      if(checkStmt.containsInvokeExpr && checkStmt.getInvokeExpr.getArgCount > 0 && (checkStmt.getInvokeExpr.getMethod.getName.contains("startActivity") || checkStmt.getInvokeExpr.getMethod.getName.contains("startService")))
      {
        val target = checkStmt.getInvokeExpr.getArg(0).asInstanceOf[Local]
        if(!checkCast && analysis.mayAlias(local, target, checkStmt))
        {
          ret += VulnResult.toDataFlowVuln(stmt, body.getMethod, checkStmt, body.getMethod, "nested intent vuln")
        }
      }
      checkStmt match {
        case stmt: JAssignStmt if stmt.getRightOp.isInstanceOf[JCastExpr] =>
          val castExpr = stmt.getRightOp.asInstanceOf[JCastExpr]
          castExpr.getOp match {
            case base: Local =>
              if (checkCast && analysis.mayAlias(local, base, checkStmt)) {
                //we have to determine if there is isinstanceof check, fuck
                val casttype: RefType = castExpr.getCastType.asInstanceOf[RefType]
                val typeset: LocalTypeSet = typeAnalysis.getFlowBefore(checkStmt)
                if (!typeset.get(typeset.indexOf(base, casttype))) //&& !TrapManager.isExceptionCaughtAt(classcastexception,checkStmt, body)) //trap exception check {
                  //only presenting this vuln if no typeflow from isInstanceOf
                  //also check if the stmt is NOT in try-catch block, TODO not adding this now
                  ret += VulnResult.toCrashVulnResult(checkStmt, body.getMethod, "cast crash exception")
              }
            case _ =>
          }

          if (!checkCast && castExpr.getCastType.toString.contains("Intent")) {
            ret += VulnResult.toConstantAPIVulnResult(checkStmt, body.getMethod, "cast to Intent", "")
          }
        case _ =>
      }
    }
    ret.toList
  }

}
