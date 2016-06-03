package org.k33nteam.jade.tools

import spray.json.DefaultJsonProtocol

/**
 * Created by hqdvista on 2/3/15.
 *
 * 1. scan for all exported components
 * 2. in each entrypoint, scan for intent/bundle calls, get their key and usage
 * 3. generate json instruction, format:
 * {
 *  "component": "xxx",
 *  "action":"xxx", //actually should be list
 *  "category":"xxx", //actually should be list
 *  "package":"xxx",
 *  "class":"xxx",
 *  "extras":[
 *    'type':'key',//boolean:Xxx
 *    'type':'key',//string:Yyy
 *  ]
 *  'event': 'activity',
 *  'repeatCnt':cnt
 * }
 *
 * Entrypoints: onCreate, onStartCommand, onResume,
 */
case class FuzzCommand(pkg: String, component: String, action: Option[String], category: Option[String], extras: List[(String,String)], event: String, repeatCnt: Int)


object FuzzCommandJsonProtocol extends DefaultJsonProtocol{
  implicit val fuzzCmd = jsonFormat7(FuzzCommand)
}