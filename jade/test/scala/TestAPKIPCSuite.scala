import org.k33nteam.jade.drivers.CheckDriver
import org.k33nteam.jade.solver.IPCAnalysisIntra
import org.scalatest.FunSuite
import soot.Scene
import soot.options.Options

import scala.collection.JavaConversions._
/*
 * Copyright (c) 2015. All rights reserved. Author flankerhe@keencloudtech.
 */

/**
 * Created by hqd on 5/5/15.
 */
class TestAPKIPCSuite   extends FunSuite {

  test("test real apk IPC:"){
    val checker = new CheckDriver("/tmp/test.apk","/home/hqd/adt-bundle/sdk/platforms/","/home/hqd/Dropbox/keen/Jade-devs/jade/config/")
    Options.v().set_whole_program(true)
    checker.prepareMethodTraversal()
    for(sc <- Scene.v().getClasses)
    {
      for(sm <- sc.getMethods; if sm.hasActiveBody){
        println("processing " + sm.getSignature)
        val attrs = IPCAnalysisIntra.scanIntra(sm.getActiveBody).values
        println(attrs)
      }
    }
  }
}
