package org.k33nteam.jade.comm

import org.k33nteam.jade.bean.{VulnRepresent, VulnKind}
import spray.json
import spray.json.{JsNumber, JsValue, JsonFormat, DefaultJsonProtocol}

/**
 * Created by hqdvista on 12/18/14.
 */

object VulnRepresentJsonProtocol extends json.DefaultJsonProtocol
{
  implicit val kindJsonFormat = new JsonFormat[VulnKind.vulnKind]{
    override def write(kind: VulnKind.vulnKind) = JsNumber(kind.id)

    override def read(json: JsValue): VulnKind.vulnKind = null
  }
  implicit val vulnRepresentFormat = jsonFormat(VulnRepresent.apply, "vulnKind", "sourceStmt", "sourceMethod", "destStmt", "destMethod", "desc", "paths", "custom")
  implicit val staticResultForAPKFormat = jsonFormat(StaticResultForAPK, "score", "md5hash", "results")
}