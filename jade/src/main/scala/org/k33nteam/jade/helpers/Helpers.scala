package org.k33nteam.jade.helpers

import java.io.{BufferedInputStream, File, FileInputStream, FileOutputStream}
import java.util.zip.{ZipEntry, ZipInputStream}

import org.apache.commons.codec.digest.DigestUtils
import org.k33nteam.JadeCfg
import soot.jimple.Stmt

import scala.collection.mutable.{ListBuffer, ArrayBuffer}

/**
 * Created by hqd on 1/13/15.
 */
object SyntaxHelpers {
  implicit def unitToStmt(unit:soot.Unit): Stmt = unit.asInstanceOf[Stmt]
}
object FileHelpers {

  def extractAddtionalLibs(path: String):  List[String]= {
    val ret = new ListBuffer[String]

    //prepare output path
    //val md5 = DigestUtils.md5Hex(path)
    val dir = new File(JadeCfg.APK_LIBPATH)
    dir.mkdirs

    val fin = new FileInputStream(path)
    val bin = new BufferedInputStream(fin)
    val zin = new ZipInputStream(bin)
    var ze:ZipEntry = zin.getNextEntry
    while (ze != null) {
      if (ze.getName.endsWith(".jar") || ze.getName.endsWith(".apk")) {
        val target = new File(dir, ze.getName.split('/').last)//path traversal vuln??
        val out = new FileOutputStream(target)
        val buffer = new Array[Byte](8192)
        var len = zin.read(buffer)
        while (len != -1) {
          out.write(buffer, 0, len)
          len = zin.read(buffer)
        }
        out.close

        ret += target.getAbsolutePath
      }
      ze = zin.getNextEntry
    }

    zin.close

    //now extract dexfile out of this archieves
    val fret = new ArrayBuffer[String]

    var cnt = 0
    for(path <- ret)
    {
      val pf = new File(path)
      val fin = new FileInputStream(pf)
      val bin = new BufferedInputStream(fin)
      val zin = new ZipInputStream(bin)
      var ze:ZipEntry = zin.getNextEntry
      while (ze != null) {
        if (ze.getName.endsWith(".dex")) {
          val originName = JadeCfg.APK_LIBPATH + ze.getName.split(".dex").head
          var target = new File(originName+cnt+".dex")
          while(target.exists)
          {
            cnt += 1
            target = new File(originName+cnt+".dex")
          }

          val out = new FileOutputStream(target)
          val buffer = new Array[Byte](8192)
          var len = zin.read(buffer)
          while (len != -1) {
            out.write(buffer, 0, len)
            len = zin.read(buffer)
          }
          out.close

          fret.append(target.getAbsolutePath)
        }
        ze = zin.getNextEntry
      }

      zin.close
      pf.delete//remove archieve
    }
    fret.toList
  }
}
