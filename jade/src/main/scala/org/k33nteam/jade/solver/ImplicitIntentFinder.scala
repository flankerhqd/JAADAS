/*
 * Copyright (c) 2015. All rights reserved. Author flankerhe@keencloudtech.
 */

package org.k33nteam.jade.solver

import org.k33nteam.jade.bean.VulnResult
import org.k33nteam.jade.solver.Types.IntentInfo
import soot.{Scene, Value, Body}
import soot.jimple.{StringConstant, Stmt}

import scala.collection.JavaConversions._
import org.k33nteam.jade.helpers.SyntaxHelpers._
import soot.toolkits.graph.BriefUnitGraph
import soot.toolkits.scalar.{SimpleLocalUses, SmartLocalDefsPool}

import scala.collection.mutable.ListBuffer

/**
 * Created by hqd on 6/29/15.
 */
object ImplicitIntentFinder {

  val IPC_METHODS = Map(
    "<android.content.Context: void sendBroadcast(android.content.Intent)"->0,
    "<android.content.Context: void sendBroadcastAsUser(android.content.Intent,android.os.UserHandle)"->0,
    "<android.content.Context: void sendStickyBroadcast(android.content.Intent)"->0,
    "<android.content.Context: void sendStickyBroadcastAsUser(android.content.Intent,android.os.UserHandle)"->0,
    "<android.content.Context: void sendStickyOrderedBroadcast(android.content.Intent,android.content.BroadcastReceiver,android.os.Handler,int,java.lang.String,android.os.Bundle)"->0,
    "<android.content.Context: void sendStickyOrderedBroadcastAsUser(android.content.Intent,android.os.UserHandle,android.content.BroadcastReceiver,android.os.Handler,int,java.lang.String,android.os.Bundle)"->0,
    "<android.content.Context: void startActivity(android.content.Intent)"->0,
    "<android.content.Context: void startService(android.content.Intent)"->0
  )
  val IPC_METHODS_WITH_PERM = Map(
    "<android.content.Context: void sendBroadcast(android.content.Intent,java.lang.String)"->(0,1),
    "<android.content.Context: void sendBroadcastAsUser(android.content.Intent,android.os.UserHandle,java.lang.String)"->(0,2),
    "<android.content.Context: void sendOrderedBroadcast(android.content.Intent,java.lang.String,android.content.BroadcastReceiver,android.os.Handler,int,java.lang.String,android.os.Bundle)"->(0,1),
    "<android.content.Context: void sendOrderedBroadcast(android.content.Intent,java.lang.String)"->(0,1),
    "<android.content.Context: void sendOrderedBroadcastAsUser(android.content.Intent,android.os.UserHandle,java.lang.String,android.content.BroadcastReceiver,android.os.Handler,int,java.lang.String,android.os.Bundle)"->(0,2)
    )

  val RECEIVER_METHOD = Map(
    "<android.content.Context: android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter)>"->(-1),
    "<android.content.Context: android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter,java.lang.String,android.os.Handler)>"->2
  )

  def doAnalysis(scene:Scene): Iterable[VulnResult] ={
    val result = ListBuffer[VulnResult]()
    for (sootClass <- scene.getApplicationClasses; if !sootClass.getPackageName.contains("android.support")) {
      for (sootMethod <- sootClass.getMethods; if sootMethod.hasActiveBody) {
        val ret = scanIntra(sootMethod.getActiveBody())
        result ++= ret.map(VulnResult.toConstantAPIVulnResult(_, sootMethod, "implicit intent or receiver", "", 10))
      }
    }
    result.toList
  }

  def scanIntra(body:Body): List[Stmt] = {
    if(body.getMethod().getSignature().contains("android.support"))
    {
      return List()
    }
    lazy val graph = new BriefUnitGraph(body)
    lazy val defAnalysis = SmartLocalDefsPool.v().getSmartLocalDefsFor(body)
    lazy val useanalysis = new SimpleLocalUses(graph, defAnalysis)

    val ret = ListBuffer[Stmt]()
    val analysis = new IPCBackwardIntentTrackAnalysis(graph)
    for(stmt<-body.getUnits)
    {
      if(stmt.containsInvokeExpr() && !stmt.getInvokeExpr().getMethod().getSignature().contains("LocalBroadcastManager"))
      {
        val sig = stmt.getInvokeExpr().getMethod().getSignature()
        for( (ipcSig, intentLoc) <- IPC_METHODS)
        {
          val useSig = ipcSig.split(":").last//TODO fixthis I'm just too lazy
          if(sig.contains(useSig))
          {
            val intentVal = stmt.getInvokeExpr().getArg(intentLoc)
            val analysisRet = analysis.getFlowBefore(stmt)
            if(judgeImplicit(analysisRet,intentVal))
            {
              //we find an IPC method with implicit intent, record it
              ret += stmt
            }
          }
        }
        for( (ipcSig, (intentLoc, permLoc)) <- IPC_METHODS_WITH_PERM)
        {
          val useSig = ipcSig.split(":").last//TODO fixthis I'm just too lazy
          if(sig.contains(useSig))
          {
            val intentVal = stmt.getInvokeExpr().getArg(intentLoc)
            val permVal = stmt.getInvokeExpr().getArg(permLoc)
            val analysisRet = analysis.getFlowBefore(stmt)
            if(!permVal.isInstanceOf[StringConstant] && judgeImplicit(analysisRet,intentVal))
            {
              //we find an IPC method with implicit intent and no permission protected, record it
              ret += stmt
            }
          }
        }
        for( (ipcSig, permLoc) <- RECEIVER_METHOD)
        {
          val useSig = ipcSig.split(":").last//TODO fixthis I'm just too lazy
          if(sig.contains(useSig))
          {
            if(permLoc == -1)
            {
              ret += stmt
            }
            else {
              val permVal = stmt.getInvokeExpr().getArg(permLoc)
              if(!permVal.isInstanceOf[StringConstant])
              {
                ret += stmt
              }
            }
          }
        }
      }
    }
    ret.toList
  }

  private def judgeImplicit(analysisRet:IntentInfo, value:Value): Boolean ={
    for((values, metas) <- analysisRet)
    {
      if(values.contains(value))
      {
        return metas.exists(_.isImplicit)
      }
    }
    return false
  }
}
