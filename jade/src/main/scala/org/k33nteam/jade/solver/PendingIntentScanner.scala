/*
 * Copyright (c) 2015. All rights reserved. Author flankerhe@keencloudtech.
 */

package org.k33nteam.jade.solver

import soot.Body
import soot.jimple.Stmt
import scala.collection.JavaConversions._
import org.k33nteam.jade.helpers.SyntaxHelpers._
import soot.toolkits.graph.BriefUnitGraph
import soot.toolkits.scalar.{SimpleLocalUses, SmartLocalDefsPool}

/**
 * Created by hqd on 5/11/15.
 */
object PendingIntentScanner {

  def scanIntra(body:Body): List[Stmt] = {
    lazy val graph = new BriefUnitGraph(body)
    lazy val defAnalysis = SmartLocalDefsPool.v().getSmartLocalDefsFor(body)
    lazy val useanalysis = new SimpleLocalUses(graph, defAnalysis)

    body.getUnits.filter(stmt => {
      stmt.containsInvokeExpr() &&
      // && stmt.getInvokeExpr().getMethod().getSignature() == "<android.app.PendingIntent: getActivity ()>")
      {
        stmt.getInvokeExpr().getMethod().getSignature() match {
          case "<android.app.PendingIntent: android.app.PendingIntent getActivity(android.content.Context,int,android.content.Intent,int)>"
               | "<android.app.PendingIntent: android.app.PendingIntent getBroadcast(android.content.Context,int,android.content.Intent,int)>"
            | "<android.app.PendingIntent: android.app.PendingIntent getService(android.content.Context,int,android.content.Intent,int)>"
               //| "<android.app.PendingIntent: getActivitities(android.content.Context, int, android.content.Intent[], int, android.os.Bundle)>"
            | "<android.app.PendingIntent: android.app.PendingIntent getActivity(android.content.Context,int,android.content.Intent,int,android.os.Bundle)>" => {
            val intentMeta = IPCAnalysisIntra.getIntentMeta(2, stmt, useanalysis, defAnalysis)
            intentMeta.isImplicit &&
            {
              //found an vuln! now need to strip off cases in NotificationManager
              //find usage of this local
              val uses = useanalysis.getUsesOf(stmt).filter(p => {
                !(p.getUnit.containsInvokeExpr() && p.getUnit.getInvokeExpr.getMethod.getName == "setContentIntent")
              }).map(_.getUnit.asInstanceOf[Stmt])
              uses.size > 0
            }
          }
          case _ => false
        }
        //expr.getMethod().get
      }
    }).map(_.asInstanceOf[Stmt]).toList
  }
}
