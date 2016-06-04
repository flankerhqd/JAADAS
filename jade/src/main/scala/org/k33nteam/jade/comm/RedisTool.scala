package org.k33nteam.jade.comm

import java.io.{PrintWriter, FileWriter, File, FileInputStream}

import com.redis._
import org.k33nteam.jade.bean.{VulnKind, VulnResult, VulnRepresent}
import spray.json._
import org.k33nteam.jade.comm.VulnRepresentJsonProtocol._

/**
 * Created by hqdvista on 12/17/14.
 */
case class StaticResultForAPK(score: Float, md5hash: String, vulns: Iterable[VulnRepresent])

object CommTool {

  def putResult(redishost:String, filepath:String, results:Iterable[VulnResult]): Unit =
  {
    val r = new RedisClient(redishost,6379)
    val fis = new FileInputStream(new File(filepath))
    val md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis)
    r.rpush("static-results",StaticResultForAPK(results.map(_.scoring).sum, md5, results.map(VulnRepresent.toVulnRepresent(_))).toJson)
  }

  def putResultIntoFile(dirpath:String = "output", filepath:String, results:Iterable[VulnResult]): Unit = {
    val fis = new FileInputStream(new File(filepath))
    val md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis)
    val file = new File(dirpath)
    file.mkdir
    val writer = new FileWriter(new File(dirpath, md5 + ".txt"))
    writer.write(StaticResultForAPK(results.map(_.scoring).sum, md5, results.map(VulnRepresent.toVulnRepresent(_))).toJson.prettyPrint)
    writer.close
  }

  def computeSumScore(results:Iterable[VulnResult]): Float = {
    results.map(_.scoring).sum
  }
}
