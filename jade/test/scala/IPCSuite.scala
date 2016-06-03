
import java.util.Arrays

import org.k33nteam.jade.solver.{PendingIntentScanner, IntentMeta, IPCAnalysisIntra}
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
class IPCSuite  extends FunSuite {

  test("pendingIntent scanner")
  {
    G.reset()
    prepareMethodTraversal()
    for(sc <- Scene.v().getClasses) {
      if (sc.getName.equals("PendingIntentActivity")) {

        for (sm <- sc.getMethods; if sm.hasActiveBody) //explicit, setComponentName
        {
          sm.getName() match {
            case "test1" => {
              println(PendingIntentScanner.scanIntra(sm.getActiveBody))
            }
            case "test2" => {
              println(PendingIntentScanner.scanIntra(sm.getActiveBody))
            }
            case _ =>
          }
        }
      }
    }
  }

  test("implicit intent analysis") {
    G.reset()
    prepareMethodTraversal()


    for(sc <- Scene.v().getClasses)
    {
      if(sc.getName.equals("TestActivity"))
      {

        for(sm <- sc.getMethods; if sm.hasActiveBody) //explicit, setComponentName
        {
          sm.getName() match {
            case "test1" => {
              checkMetaSingleMatch(IPCAnalysisIntra.scanIntra(sm.getActiveBody).values, true, sm.getName())
            }
            case "test2" | "test3" | "test4" => {
              checkMetaSingleMatch(IPCAnalysisIntra.scanIntra(sm.getActiveBody).values, false, sm.getName())
            }
            case "test5" => {
              //println(sm.getActiveBody)
              val attrs = IPCAnalysisIntra.scanIntra(sm.getActiveBody).values
              assert(attrs.size == 2)
              assert(attrs.head.isImplicit == true)
              assert(attrs.last.isImplicit == false)
            }
            case "test7" => {
              //println(sm.getActiveBody)
              val attrs = IPCAnalysisIntra.scanIntra(sm.getActiveBody).values
              assert(attrs.size == 1)
              assert(attrs.last.isImplicit == false)
            }
            case "test8" => {//implicit field test
              //println(sm.getActiveBody)
              val attrs = IPCAnalysisIntra.scanIntra(sm.getActiveBody).values
              assert(attrs.size == 1)
              assert(attrs.last.isImplicit == true)
            }
            case "test9" => {//first implicit, then explicit
              val attrs = IPCAnalysisIntra.scanIntra(sm.getActiveBody).values
              assert(attrs.size == 2)
              assert(attrs.head.isImplicit == true)
              assert(attrs.last.isImplicit == false)
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
