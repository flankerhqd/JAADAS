import java.util.Arrays

import org.k33nteam.jade.solver.IPCAnalysisIntra
import org.scalatest.FunSuite
import soot.jimple.internal.JAssignStmt
import soot.jimple.{Stmt, AssignStmt}
import soot.jimple.toolkits.pointer.LocalMayEquivValueAnalysis
import soot.toolkits.graph.BriefUnitGraph
import soot.{Local, PackManager, Scene}
import soot.options.Options

import scala.collection.JavaConversions._
class SetSuite extends FunSuite {

  test("An empty Set should have size 0") {
    assert(Set.empty.size == 0)
  }

  test("Invoking head on an empty Set should produce NoSuchElementException") {
    intercept[NoSuchElementException] {
      Set.empty.head
    }
  }
}

class CustomSuite extends FunSuite {

  test("scan for field ref") {
    prepareMethodTraversal()
    for(sc <- Scene.v().getClasses)
    {
      if(sc.getName.equals("TestActivity")) {

        for (sm <- sc.getMethods; if sm.hasActiveBody) //explicit, setComponentName {
          sm.getName() match {
            case "test7" => {
              val localEqualAnalysis = new LocalMayEquivValueAnalysis(new BriefUnitGraph(sm.getActiveBody()))

              for(u <- sm.getActiveBody().getUnits())
              {
                for(local <- sm.getActiveBody().getLocals){
                  println("checking stmt: " + u)
                  println("checking local: " + local)
                  println(localEqualAnalysis.mayAliases(local, u.asInstanceOf[Stmt]))
                }
                if(u.isInstanceOf[JAssignStmt])
                {
                  val rightVal = u.asInstanceOf[JAssignStmt].getRightOp()
                  val leftVal =  u.asInstanceOf[JAssignStmt].getLeftOp()
                  if(leftVal.isInstanceOf[Local])
                  {
                    val local = leftVal.asInstanceOf[Local]
                    println(u)
                    println(local)
                    println(localEqualAnalysis.mayAliases(local, u.asInstanceOf[Stmt]))
                  }
                  if(rightVal.isInstanceOf[Local])
                  {
                    val local = rightVal.asInstanceOf[Local]
                    println(u)
                    println(local)
                    println(localEqualAnalysis.mayAliases(local, u.asInstanceOf[Stmt]))
                  }
                }

              }
            }
            case _ =>
          }
      }
    }
    for(sc <- Scene.v().getClasses)
    {
      if(sc.getName.equals("TestActivity"))
      {

        for(sm <- sc.getMethods; if sm.hasActiveBody) //explicit, setComponentName
        {
          sm.getName() match {
            case "test1" => {
              //checkMetaSingleMatch(IPCAnalysisIntra.scanIntra(sm.getActiveBody).values, true)
            }
            case "test2" | "test3" | "test4" => {
              //checkMetaSingleMatch(IPCAnalysisIntra.scanIntra(sm.getActiveBody).values, false)
            }
            case "test5" => {
              //println(sm.getActiveBody)
              //checkMetaSingleMatch(IPCAnalysisIntra.scanIntra(sm.getActiveBody).values, false)
            }
            case "test7" => {//THIS TEST DOESNOT PASS, have to FIX
              println(sm.getActiveBody)
              val graph = new BriefUnitGraph(sm.getActiveBody())
              for(u <- sm.getActiveBody.getUnits)
              {
                if(u.isInstanceOf[AssignStmt])
                {
                  val assignStmt = u.asInstanceOf[AssignStmt]
                  if(assignStmt.containsFieldRef())
                  {
                    println("fucking "+assignStmt)
                    println(IPCAnalysisIntra.findFieldRefDefLocal(assignStmt.getFieldRef, assignStmt, graph))
                  }

                }

              }

            }
            case _ =>
          }
        }
      }
    }
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