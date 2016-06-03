/*
 * Copyright (c) 2015. All rights reserved. Author flankerhe@keencloudtech.
 */

package org.k33nteam.jade.solver

import org.k33nteam.jade.solver.Types.IntentInfo
import soot.{EquivalentValue, Body, Local, Value}
import soot.jimple._
import soot.toolkits.graph._
import soot.toolkits.scalar._
import scala.collection.JavaConversions._
import scala.collection.immutable.HashMap
import scala.collection.immutable
import scala.collection.mutable
import scala.collection.mutable.Queue
import scala.collection.mutable.ListBuffer

import org.k33nteam.jade.helpers.SyntaxHelpers._

/**
 * Created by hqd on 3/12/15.
 * Intra-analysis to collect intent infos.
 * This is a preserve-style analysis, which means if one-path is implicit, another-path is explicit, will regard as implicit
 * Not used currently, substituted by scanIntra
 *
 * TODO
 */
object Types{
  type IntentInfo = mutable.HashMap[immutable.HashSet[Value], immutable.HashSet[IntentMeta]]
}

class IPCBackwardIntentTrackAnalysis  (graph: DirectedGraph[soot.Unit]) extends ForwardFlowAnalysis[soot.Unit, IntentInfo](graph)
{
  val emptySet = new ArraySparseSet[Local]()
  doAnalysis()

  private def wrapRef(value:Value): Value =
  {
    value match {
      case _:Ref => new EquivalentValue(value)
      case _ => value
    }
  }

  override protected def flowThrough(in: IntentInfo, d: soot.Unit, out: IntentInfo): Unit = {
    /*println("###")

    println("flowing through " + d)
    println("in: " + in)
    println("out: " + in)
    println("***")*/
    out.clear()
    out ++= in
    val stmt = d.asInstanceOf[Stmt]

    def killValue(value: Value): Unit = {
      //kills left, generate a new one
      val leftSetOp = in.keys.find(_.contains(value))
      if(leftSetOp.isDefined)
      {
        out.remove(leftSetOp.get)
      }
    }

    def updateLocalMetas(base: Local, updateFunc: IntentMeta=>IntentMeta): Unit ={
      val leftInSetOp = in.keys.find(_.contains(base))
      //println("in update local meta")
      //println("leftInSetOp: " + leftInSetOp)
     // println("base: " + base)
      leftInSetOp.foreach(localkeys => {
        val newMetas = in.get(localkeys).get.map(updateFunc)
        //println("newMetas: " + newMetas)
        out.remove(localkeys)
        out.put(localkeys, newMetas)
      })
    }

    def processIIE(iie: InstanceInvokeExpr): Unit ={
      val w = updateLocalMetas(iie.getBase.asInstanceOf[Local], _: IntentMeta=>IntentMeta)
      (iie.getMethod.getName,iie.getMethod.getSignature) match {
        case ("setComponent" | "setClass" | "setPackage" | "setClassName", _) => w(_.copy(isImplicit = false))
        case ("<init>", "<android.content.Intent: void <init>(android.content.Context,java.lang.Class)>" |
                        "<android.content.Intent: void <init>(java.lang.String,android.net.Uri,android.content.Context,java.lang.Class)>") => w(_.copy(isImplicit = false))
        case ("<init>", "<android.content.Intent: void <init>(java.lang.String)>") =>w(_.copy(hasAction = true))
        //case ("putExtra", _) => hasExtras = true
        case ("setAction", _) => w(im => {im.copy(hasAction = true)})
        case (_, _) =>
      }
    }

    if(stmt.containsInvokeExpr() && stmt.isInstanceOf[InvokeStmt])
    {
      stmt.getInvokeExpr match {
        case iie:InstanceInvokeExpr => {
            processIIE(iie)
        }
        case _ =>
      }//end invokeExpr check
    }

    stmt match {
      case assignStmt:AssignStmt => {
        (assignStmt.getLeftOp, assignStmt.getRightOp) match {
          case p @ (_:Local|_:Ref, _:Local|_:Ref) => {
            //e.g. $r1 = $r2, kills $r1, add $r1 to $r2 set,
            //we assume $r2 has appeared before
            def processAssignValue(leftV: Value, rightV: Value): Unit ={
              //println("in process Assign Value")
              val left:Value = wrapRef(leftV)
              val right:Value = wrapRef(rightV)

              in.keys.find(_.contains(right)).foreach(rightKeys => {
                in.keys.find(_.contains(left)).foreach(leftKeys => {
                  out.remove(leftKeys)
                  val newKeys = leftKeys - left
                  newKeys.size match {
                    case 0 =>
                    case _ => in.get(leftKeys).foreach(out.put(newKeys,_))
                  }
                })
                in.get(rightKeys).foreach(oldMetas => {
                  out.remove(rightKeys)
                  out.put(rightKeys + left, oldMetas)
                })
              })
            }
            processAssignValue(p._1, p._2)
          }
          case (left:Local, right:NewExpr) => {
            //kills left
            killValue(left)
            if(right.getType.toString == "android.content.Intent")
            {
              //generate a new one
              out.put(immutable.HashSet(left), immutable.HashSet(IntentMeta()))
            }
          }
          case (left:Local, right:InvokeExpr) => {
            //kills left
            killValue(left)
            if(right.getMethod.getReturnType.toString == "android.content.Intent")
            {
              //generate, assume a brand new intent
              out.put(immutable.HashSet(left), immutable.HashSet(IntentMeta()))
            }
          }
          case (left:Local, _) => {
            //???a constant? do kill
            killValue(left)
          }
          case (_,_) => {
            //???what is left?? pass
          }
        }
      }
      case _ =>
    } //end stmt assign check
    //println("after " + d)
    //println("in: " + in)
    //println("out: " + out)
  }

  override protected def merge(in1: IntentInfo, in2: IntentInfo, out: IntentInfo): Unit = {
    //union-style merge, preserve on *any*
    //println("merging in1:" +in1)
    //println("merging in2:" +in2)
    out.clear()
    //merge IntentMetas
    out ++= in1
    in2.foreach{case(key,value) => {
      out.put(key, value ++ out.getOrElse(key,Set()))
    }}
  }

  override protected def newInitialFlow(): IntentInfo = {
    new IntentInfo()
  }

  override protected def copy(source: IntentInfo, dest: IntentInfo): Unit = {
    dest.clear()
    dest ++= source
  }

  override protected def entryInitialFlow(): IntentInfo = {
    new IntentInfo()
  }
}

case class IntentMeta(isImplicit:Boolean = true, hasExtras:Boolean = false, hasAction:Boolean = false, attrs:Map[String,String] = new HashMap())

object IPCAnalysisIntra {

  final val DEBUG = false

  val IPC_METHODS = Map(
    "sendBroadcast" -> 0,
    "sendBroadcastAsUser" ->0,
    "sendOrderedBroadcast"->0,
    "sendStickyBroadcast"->0,
    "sendStickyBroadcastAsUser"->0,
    "sendStickyOrderedBroadcast"->0,
    "startActivity"->0,
    "startService"->0,
    "getActivity"->2,
    "getBroadcast"->2
  )
  def scanIntra(body:Body): Map[Stmt, IntentMeta] = {
    val graph = new BriefUnitGraph(body)
    val defAnalysis = SmartLocalDefsPool.v().getSmartLocalDefsFor(body)
    val useanalysis = new SimpleLocalUses(graph, defAnalysis)
    body.getUnits.filter(p => {
      p.containsInvokeExpr() && stmtCallMayUsesIntent(p.getInvokeExpr, true) != -1
    }).map(u => u.asInstanceOf[Stmt]->getIntentMeta(stmtCallMayUsesIntent(u.getInvokeExpr, true), u, useanalysis, defAnalysis)).toMap
  }

  private def stmtCallMayUsesIntent(invokeExpr:InvokeExpr, frameworkMethodOnly: Boolean =false): Int ={
    //returns calling arg position, -1 means not-intent related
    IPC_METHODS.get(invokeExpr.getMethod().getName).getOrElse({
      if(frameworkMethodOnly) -1
      else{
        //TODO
        -1
      }
    })
  }
  def getIntentMeta(loc:Int, keyStmt:Stmt, useanalysis:SimpleLocalUses, defAnalysis: SmartLocalDefs): IntentMeta = {
    val dominatorAnalysis = new MHGDominatorsFinder(defAnalysis.getGraph)
    val keyValue = keyStmt.getInvokeExpr().getArg(stmtCallMayUsesIntent(keyStmt.getInvokeExpr(), true))
    val relatedStmts = getRelatedStmts(useanalysis, defAnalysis, dominatorAnalysis, keyStmt, keyValue)
    var isImplicit = true
    var hasExtras = false
    var hasAction = false
    //println(relatedStmts)
    for(stmt <- relatedStmts)
    {
      if(stmt.containsInvokeExpr() && stmt.isInstanceOf[InvokeStmt] && stmt.getInvokeExpr.isInstanceOf[InstanceInvokeExpr])
      {
        (stmt.getInvokeExpr.getMethod.getName,stmt.getInvokeExpr.getMethod.getSignature) match {
          case ("setComponent" | "setClass" | "setPackage" | "setClassName", _) => isImplicit = false
          case ("<init>", "<android.content.Intent: void <init>(android.content.Context,java.lang.Class)>" |
                          "<android.content.Intent: void <init>(java.lang.String,android.net.Uri,android.content.Context,java.lang.Class)>") => isImplicit = false
          case ("<init>", "<android.content.Intent: void <init>(java.lang.String)>") => hasAction = true
          case ("putExtra",_) => hasExtras = true
          case ("setAction",_) => hasAction = true
          case (_,_) =>
            //TODO add more put**listExtra stmts
        }
      }
    }
    new IntentMeta(isImplicit, hasExtras, hasAction)
  }

  def getRelatedStmts(useanalysis:SimpleLocalUses, defAnalysis: SmartLocalDefs, dominatorAnalysis:DominatorsFinder[soot.Unit], keyStmt: soot.Unit, keyValue: soot.Value): List[Stmt] = {
    //find all relevent statements
    val expr = keyStmt.getInvokeExpr
    val loc = stmtCallMayUsesIntent(expr)
    if(expr.getArgCount() < loc + 1)
    {
      //whoa? FUCK same method name as framework method
      //println(expr)
      return List()
    }
    if(expr.getArg(loc).getType().toString() != "android.content.Intent")
    {
      //whoa? FUCK arg type isn't Intent
      //println(expr)
      return List()
    }
    //val intent = expr.getArg(stmtCallMayUsesIntent(expr)).asInstanceOf[Local]
    val intent = keyValue
    if(intent.isInstanceOf[Local])
    {
      val queue = new Queue[(Local, Stmt)]()
      val statements = new ListBuffer[Stmt]()
      queue += ((intent.asInstanceOf[Local], keyStmt))

      val hashSet = new mutable.HashSet[(Local,Stmt)]()
      while (!queue.isEmpty) {
        val cur = queue.dequeue()
        val defs = ListBuffer[soot.Unit]()
        if(cur._2.isInstanceOf[AssignStmt] && cur._2.asInstanceOf[AssignStmt].getLeftOp().equivTo(cur._1))
        {
          defs += cur._2
        }
        else
        {
          defs ++= defAnalysis.getDefsOfAt(cur._1, cur._2)
        }

        if(DEBUG) {
          println(cur)
          println(defs)
        }
        //first use found defs to add to related statements
        //check if def statement is an assign statement, if it is, add it to queue
        defs.foreach(defstmt => {
//FIXME: dominatorAnalysis does not work in branch situations.
          statements ++= useanalysis.getUsesOf(defstmt).map(_.getUnit).map(_.asInstanceOf[Stmt]).filter(dominatorAnalysis.isDominatedBy(keyStmt, _))
          //check for assignment at right
          //Local and Field should be treated differently TODO
          if (defstmt.isInstanceOf[AssignStmt]) {
            defstmt.asInstanceOf[AssignStmt].getRightOp match {
              case local:Local => {
                if(!hashSet.contains((local,defstmt)))
                {
                  queue += ((local, defstmt))
                }
                hashSet += ((local, defstmt))
              }
              case fieldRef:FieldRef => {
                //use a naive method to deal with FieldRef: just search upward and found assignStmt to field, use the right-side Local!!!
                val local = findFieldRefDefLocal(fieldRef, defstmt.asInstanceOf[AssignStmt], defAnalysis.getGraph)
                local match {
                  case Some((local, stmt)) => {
                    if(!hashSet.contains((local,stmt)))
                    {
                      queue += ((local, stmt))
                    }
                    hashSet += ((local, stmt))
                  }
                  case _ =>
                }
              }
              case _ =>
            }
          }
        })
      }
      if(DEBUG){
        println(statements)
      }
      statements.toList
    }
    else {
      //the value is a FieldRef mostly, currently for fieldRef we only do assumption
      //however how can an invoke expr's arg be a fieldRef?
      if(DEBUG)
      {
        println(expr)
      }
      new ListBuffer[Stmt]().toList
    }

  }

  def findFieldRefDefLocal(target: FieldRef, stmt: AssignStmt, graph: UnitGraph): Option[(Local,Stmt)] ={
    //This is a backward search, found first equal-local of a specific fieldRef
    val visited = new mutable.HashSet[soot.Unit]()
    val queue = new Queue[soot.Unit]()
    queue ++= graph.getPredsOf(stmt)
    while(!queue.isEmpty)
    {
      val cur = queue.dequeue()
      if(visited.contains(cur))
      {
        //seems we're in a loop and still not found. return now
        return None
      }
      visited.add(cur)

      cur match {
        case assignStmt:AssignStmt => {
          //This is for $r1 = <field_ref>
          assignStmt.getRightOp match {
            case fieldRef:FieldRef => {
              if(fieldRef.equivTo(target))
              {
                //found! return now, should be a local
                return Option((assignStmt.getLeftOp().asInstanceOf[Local], cur))
              }
            }
            case _ =>
          }

          //For <field_ref> = $r1, which is commonly seen in modification on field, like after $r1-><init>
          assignStmt.getLeftOp match {
            case fieldRef:FieldRef => {
              if(fieldRef.equivTo(target))
              {
                return Option((assignStmt.getRightOp()).asInstanceOf[Local], cur)
              }
            }
            case _ =>
          }
        }
        case _ =>
      }
      //build queue
      queue ++= graph.getPredsOf(cur)
    }
    None
  }

  private def isValuableInsts(stmts:List[Stmt]): Boolean ={
//    stmts.exists(stmt => {
//      if(stmt.containsInvokeExpr() && stmt.isInstanceOf[InvokeStmt] && stmt.getInvokeExpr.isInstanceOf[InstanceInvokeExpr])
//      {
//        (stmt.getInvokeExpr.getMethod.getName,stmt.getInvokeExpr.getMethod.getSignature) match {
//          case ("putExtra", _) => true
//          case ("<init>", "<android.content.Intent: void <init>(android.content.Context,java.lang.Class)>" |
//                          "<android.content.Intent: void <init>(java.lang.String,android.net.Uri,android.content.Context,java.lang.Class)>") => true
//          case (_,_) => false
//        }
//      }
//      else{
//        false
//      }
//    })
    true
  }

}