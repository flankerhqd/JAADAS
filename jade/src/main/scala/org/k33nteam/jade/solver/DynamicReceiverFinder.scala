/*
 * Copyright (c) 2015. All rights reserved. Author flankerhe@keencloudtech.
 */

package org.k33nteam.jade.solver

import soot.Body
import org.k33nteam.jade.helpers.SyntaxHelpers._
import soot.jimple.Stmt
import soot.toolkits.graph.{DominatorsFinder, MHGDominatorsFinder, BriefUnitGraph}
import soot.toolkits.scalar.{LocalUses, LocalDefs, SimpleLocalUses, SmartLocalDefsPool}
import soot.Value
import scala.collection.JavaConversions._

/**
 * Created by hqd on 5/6/15.
 * scan for registerReceiver, registerReceiverAsUser
 */
case class DynamicReceiverMeta(permissionProtected:Boolean = false, filterActions:List[String] = List())
object DynamicReceiverFinder {

  def scanMethod(body:Body): Unit={
    lazy val graph = new BriefUnitGraph(body)
    lazy val defAnalysis = SmartLocalDefsPool.v().getSmartLocalDefsFor(body)
    lazy val useAnalysis = new SimpleLocalUses(graph, defAnalysis)
    lazy val dominatorAnalysis = new MHGDominatorsFinder(graph)
    val ret = new DynamicReceiverMeta()
    for(stmt <- body.getUnits)
    {
      if(stmt.containsInvokeExpr())
      {
        val name = stmt.getInvokeExpr().getMethod().getName()
        //for registerReceiver, intentFilter is at arg location 1
        //for registerReceiverAsUser, intentFilter is at arg 2
        if(name == "registerReceiver" && stmt.getInvokeExpr().getArgCount() >= 2)
        {
          val filter = stmt.getInvokeExpr().getArg(1)
        }
        else if(name == "registerReceiverAsUser" && stmt.getInvokeExpr().getArgCount() >=3)
        {
          val filter = stmt.getInvokeExpr().getArg(2)
        }

      }
    }
  }

  //def buildDynamicReceiverMeta(filter:Value, stmt:Stmt)(implicit defAnalysis:LocalDefs, useAnalysis:LocalUses, dominatorAnalysis:DominatorsFinder ): List[String] ={
    //for

 // }
}
