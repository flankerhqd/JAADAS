
import java.util.Arrays

import org.k33nteam.jade.solver._
import org.scalatest.FunSuite
import soot.jimple.Stmt
import soot.jimple.internal.JAssignStmt
import soot.jimple.toolkits.pointer.{LocalMayEquivValueAnalysis, LocalMayAliasAnalysis, LocalMustAliasAnalysis}
import soot.toolkits.graph.BriefUnitGraph
import soot.{G, Local, PackManager, Scene}
import soot.options.Options
import scala.collection.JavaConversions._
/**
 * Created by hqd on 3/12/15.
 */
class IntraIPCSuite  extends FunSuite {

  test("intra intent analysis")
  {
    G.reset()
    prepareMethodTraversal()
    for(sc <- Scene.v().getClasses) {
      if (sc.getName.equals("TestActivity"))
      {

        for (sm <- sc.getMethods; if sm.hasActiveBody) //explicit, setComponentName
        {
          val graph = new BriefUnitGraph(sm.getActiveBody)
          sm.getName() match {
            case "test4" => {
              println(sm.getActiveBody)
              val analysis = new IPCBackwardIntentTrackAnalysis(graph)
              val tail = graph.getTails.head
              val ret = analysis.getFlowAfter(tail)
              assert(ret.size == 1)
              assert(ret.keys.head.size == 2)
              assert(ret.values.head.size == 2)
              val metas = ret.values.head
              assert(metas.head.isImplicit == false)
              assert(metas.last.isImplicit == true)
              //println(PendingIntentScanner.scanIntra(sm.getActiveBody))
            }
            case "test2" => {
              val analysis = new IPCBackwardIntentTrackAnalysis(graph)
              val tail = graph.getTails.head
              val ret = analysis.getFlowAfter(tail)
              println(sm.getActiveBody)
              assert(ret.size == 1)
              assert(ret.keys.head.size == 2)
              assert(ret.values.head.size == 1)
              val metas = ret.values.head
              assert(metas.head.isImplicit == false)

              val finderResult = ImplicitIntentFinder.scanIntra(graph.getBody)
              println(finderResult)
              assert(finderResult.size == 0)
              //println(PendingIntentScanner.scanIntra(sm.getActiveBody))
            }
            case "test3" => {
              val analysis = new IPCBackwardIntentTrackAnalysis(graph)
              val tail = graph.getTails.head
              val ret = analysis.getFlowAfter(tail)
              assert(ret.size == 1)
              assert(ret.values.head.size == 1)
              val metas = ret.values.head
              assert(metas.head.isImplicit == false)
              //println(PendingIntentScanner.scanIntra(sm.getActiveBody))
            }
            case "testAction" => {
              val analysis = new IPCBackwardIntentTrackAnalysis(graph)
              val tail = graph.getTails.head
              val ret = analysis.getFlowAfter(tail)
              assert(ret.size == 1)
              assert(ret.values.head.size == 1)
              val metas = ret.values.head
              assert(metas.head.hasAction == true)
              assert(metas.head.isImplicit == true)
              //println(PendingIntentScanner.scanIntra(sm.getActiveBody))
            }
            case "testAction1" => {
              val analysis = new IPCBackwardIntentTrackAnalysis(graph)
              val tail = graph.getTails.head
              val ret = analysis.getFlowAfter(tail)
              assert(ret.size == 1)
              assert(ret.values.head.size == 1)
              val metas = ret.values.head
              assert(metas.head.hasAction == true)
              assert(metas.head.isImplicit == true)
              //println(PendingIntentScanner.scanIntra(sm.getActiveBody))
            }
            case "test5" => {
              val analysis = new IPCBackwardIntentTrackAnalysis(graph)
              val tail = graph.getTails.head
              //find first startActivity
              val startActivities = sm.getActiveBody.getUnits.map(_.asInstanceOf[Stmt]).filter(_.containsInvokeExpr()).filter(_.getInvokeExpr.getMethod.getName == "startActivity")
              val firstS = startActivities.head
              val lastS = startActivities.last

              var ret = analysis.getFlowAfter(firstS)
              assert(ret.size == 1)
              assert(ret.values.head.size == 1)
              var metas = ret.values.head
              assert(metas.head.hasAction == false)
              assert(metas.head.isImplicit == true)

              ret = analysis.getFlowAfter(lastS)
              assert(ret.size == 1)
              assert(ret.values.head.size == 1)
              metas = ret.values.head
              assert(metas.head.hasAction == false)
              assert(metas.head.isImplicit == false)
              //println(PendingIntentScanner.scanIntra(sm.getActiveBody))
            }
            case "test1" => {
              val finderResult = ImplicitIntentFinder.scanIntra(graph.getBody)
              assert(finderResult.size == 1)
            }
            case _ =>
          }
        }
      }
    }
  }


  def checkMetaSingleMatch(attrs: Iterable[IntentMeta], bool:Boolean, methodName:String): Unit ={
    assert(attrs.size == 1, methodName + " size match")
    attrs.foreach(u => assert(u.isImplicit == bool, methodName + " result match"))
  }

  def prepareMethodTraversal(): Unit= {
    Options.v.set_output_format(Options.output_format_jimple)
    Options.v.set_process_dir(Arrays.asList("/home/hqd/Dropbox/keen/Jade-devs/jade/build/classes/test"))
    Options.v.set_allow_phantom_refs(true)
    Options.v.set_exclude(Arrays.asList("java", "sun", "wlc", "com.taobao.dp","dxoptimizer"))
    Scene.v.loadNecessaryClasses
    PackManager.v.runPacks
  }
}
