/*
 * Copyright (c) 2015. All rights reserved. Author flankerhe@keencloudtech.
 */

package org.k33nteam.jade.propagation.methodplugins

import org.k33nteam.jade.bean.VulnResult
import org.k33nteam.jade.propagation.base.IAPICheckPlugin
import soot.{Body, Scene}
import scala.collection.JavaConversions._
/**
 * Created by hqd on 7/3/15.
 */
object CheckZipEntry extends IAPICheckPlugin{
  final val SCORE = 1f;
  override def getResult(scene: Scene): Iterable[VulnResult] = {
    scene.getApplicationClasses().filter(!_.getPackageName.contains("android.support"))
      .map(_.getMethods.filter(_.hasActiveBody)
      .filter(sm => checkZipEntry(sm.getActiveBody))
      .map(VulnResult.toMethodAPIVulnResult(_, getDesc, "ZipEntry usage", score = SCORE)))
      .flatten
  }

  def checkZipEntry(body: Body): Boolean = {
    body.getLocals.exists(_.getType.toString == "java.util.zip.ZipEntry") || body.getParameterLocals.exists(_.getType.toString == "java.util.zip.ZipEntry")
  }

  override def getDesc: String = "Scan for ZipEntry vulnerable to unzip directory traversal vulnerability"
}
