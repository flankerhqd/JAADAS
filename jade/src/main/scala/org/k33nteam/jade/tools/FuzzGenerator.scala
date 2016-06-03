package org.k33nteam.jade.tools

import org.k33nteam.jade.drivers.CheckDriver
import soot.jimple.infoflow.android.manifest.{IntentFilter, ProcessManifest}
import soot.jimple.infoflow.entryPointCreators.AndroidEntryPointConstants
import soot.{Scene, SootClass, Local, SootMethod}
import soot.jimple.infoflow.android.manifest.ProcessManifest.ComponentType
import soot.jimple.{AssignStmt, InstanceInvokeExpr, Stmt, StringConstant}
import soot.toolkits.graph.BriefUnitGraph
import soot.toolkits.scalar.{LocalDefs, SimpleLiveLocals, SmartLocalDefs}
import spray.json._

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import org.k33nteam.jade.tools.FuzzCommandJsonProtocol._

import org.k33nteam.jade.helpers.SyntaxHelpers._
object FuzzGenerator {

  val Pattern = "get(.*?)(Extra)?".r
  val getArrayExtraPattern = "get(.*?)Array(Extra)?".r
  val getArrayListExtraPattern = "get(.*?)ArrayList(Extra)?".r

  private def resolveParamConst(stmt:Stmt, analysis:LocalDefs): Option[String] = {
    stmt.getInvokeExpr.getArg(0) match {
      case sc:StringConstant => {
        Option(sc.value)
      }
      case local:Local => {
        val defs = analysis.getDefsOfAt(local, stmt)
        if(defs.size() > 0)
        {
          defs.get(0) match {
            case assignStmt:AssignStmt => {
              assignStmt.getRightOp match {
                case sc:StringConstant =>  {
                  Option(sc.value)
                }
                case _ => None
              }
            }
            case _ => None
          }
        }
        else
        {
          None
        }
      }
      case _ => None
    }
  }
  private def scanMethodAndBuildJSInst(method: SootMethod): List[(String,String)] =
  {
    val extraList: ListBuffer[(String,String)] = ListBuffer()
    val body = method.getActiveBody
    val unitgraph = new BriefUnitGraph(body)
    val localdefs = new SmartLocalDefs(unitgraph, new SimpleLiveLocals(unitgraph))
    for(stmt<-body.getUnits; if stmt.containsInvokeExpr())
    {
      stmt.getInvokeExpr match {
        case iiexpr:InstanceInvokeExpr =>{
          iiexpr.getBase.getType.toString match {
            case "android.content.Intent" => {
              iiexpr.getMethod.getName match {
                case name if name.startsWith("get") => {
                  //resolve get param
                  name match {
                    case "getAction" => {
                      //propertyInt |= 0x2
                    }
                    case "getScheme"=>{
                      //propertyInt |= 0x4
                    }
                    case "getDataString" | "getData"=>{
                      extraList += (("placeholder", "data"))
                    }
                    case "getType" =>{
                      extraList += (("placeholder", "type"))
                    }
                    case "getExtras" => {
                      //pass
                    }
                    case "getFlags" => {
                      //pass
                    }
                    case getArrayListExtraPattern(extraType,_) => {
                      resolveParamConst(stmt, localdefs).foreach(key => {extraList += ((key, extraType+"-array-list"))})
                    }
                    case getArrayExtraPattern(extraType,_) => {
                      resolveParamConst(stmt, localdefs).foreach(key => {extraList += ((key, extraType+"-array"))})
                    }
                    case Pattern(extraType,_) => {
                      println(name)
                      resolveParamConst(stmt, localdefs).foreach(key => {extraList += ((key, extraType))})
                    }
                    case _ => {
                      //wtf?????
                    }
                  }
                }
                case _ =>
              }
            }
            case _ =>
          }
        }
        case _ =>
      }
    }
    extraList.toList
  }

  def scanManifest(manifest: ProcessManifest): List[FuzzCommand] = {
    println(manifest.getExportedComponents.toList)
    manifest.getExportedComponents.toList.map(comp => {
      println(manifest.getComponentTypeByFullName(comp))
      manifest.getComponentTypeByFullName(comp) match {
        case ComponentType.Activity => {
          (scanCompClass(manifest.getPackageName, comp, AndroidEntryPointConstants.ACTIVITY_ONCREATE, "activity", manifest.getFilters.get(comp).toList, onlyMainEntry = true) ++
          scanCompClass(manifest.getPackageName, comp, AndroidEntryPointConstants.ACTIVITY_ONCREATE, "activity", manifest.getFilters.get(comp).toList, onlyMainEntry = false))
        }
        case ComponentType.BroadcastReceiver => {
          (scanCompClass(manifest.getPackageName, comp, AndroidEntryPointConstants.BROADCAST_ONRECEIVE, "receiver", manifest.getFilters.get(comp).toList,onlyMainEntry = true) ++
          scanCompClass(manifest.getPackageName, comp, AndroidEntryPointConstants.BROADCAST_ONRECEIVE, "receiver", manifest.getFilters.get(comp).toList,onlyMainEntry = false))
        }
        case ComponentType.Service => {
          (scanCompClass(manifest.getPackageName, comp, AndroidEntryPointConstants.SERVICE_ONSTART2, "service", manifest.getFilters.get(comp).toList,onlyMainEntry = true) ++
          scanCompClass(manifest.getPackageName, comp, AndroidEntryPointConstants.SERVICE_ONSTART2, "service", manifest.getFilters.get(comp).toList,onlyMainEntry = false))
        }
        case _ => List()
      }
    }).flatten
  }

  private def scanCompClass(pkg:String, name: String, methodName: String, compType: String, filters: List[IntentFilter], onlyMainEntry: Boolean): Option[FuzzCommand] =
  {
    val sootClass = Scene.v().getSootClass(name)
    println(sootClass)
    //val onCreate = findMethod(sootClass, methodName)

    //notice we may not success in resolving targetclass, lookout for potential exception
    //if(onCreate == null || !onCreate.hasActiveBody)
    //  return None
    val extras = onlyMainEntry match {
      case true => {
        val onCreate = findMethod(sootClass, methodName)

        //notice we may not success in resolving targetclass, lookout for potential exception
        if(onCreate != null && onCreate.hasActiveBody)
          scanMethodAndBuildJSInst(onCreate)
        else
          ListBuffer()
      }
      case false => {
        scanAllMethodAndBuildJSInst(sootClass)
      }
    }

    val (action: Option[String], category: Option[String]) = extractActionAndCategory(filters)
    Option(FuzzCommand(pkg, sootClass.getName, action, category, extras.toList, compType, 1))
    //val onNewIntent = findMethod(sootClass, AndroidEntryPointConstants.ACTIVITY_ONNEWINTENT)
    //scanMethodAndBuildJSInst(onNewIntent)
  }

  private def scanAllMethodAndBuildJSInst(sootClass: SootClass):ListBuffer[(String,String)] ={
    val extraList: ListBuffer[(String,String)] = ListBuffer()
    for(method <- sootClass.getMethods; if method.hasActiveBody){
      extraList ++= scanMethodAndBuildJSInst(method)
    }
    //if(sootClass.hasSuperclass)
    //{
    //  extraList ++= scanAllMethodAndBuildJSInst(sootClass.getSuperclass)
    //}
    extraList
  }
  private def extractActionAndCategory(filters: List[IntentFilter]): (Option[String], Option[String]) = {
    // if Some, return intentFilter.action , else return None
    val (action, category) = filters.headOption match {
      case Some(intentFilter) => {
        if (intentFilter.getActions.size() > 0 && intentFilter.getCategories.size() > 0) {
          (Option[String](intentFilter.getActions.get(0)), Option[String](intentFilter.getCategories.get(0)))
        }
        else if (intentFilter.getActions.size() > 0) {
          (Option[String](intentFilter.getActions.get(0)), None)
        }
        else if (intentFilter.getCategories.size() > 0) {
          (None, Option[String](intentFilter.getCategories.get(0)))
        }
        else {
          (None, None)
        }
      }
      case None => (None, None)
    }
    (action, category)
  }

  private def findMethod (currentClass: SootClass, subsignature: String):SootMethod = {
    if (currentClass.declaresMethod(subsignature)) {
      return currentClass.getMethod(subsignature)
    }
    if (currentClass.hasSuperclass) {
      return findMethod(currentClass.getSuperclass, subsignature)
    }
    return null
  }

  def entry(apkPath:String, platformPath:String, cfgPath:String):List[String] ={
    val processMan = new ProcessManifest(apkPath)
    val driver =  new CheckDriver(apkPath, platformPath, cfgPath)
    driver.prepareMethodTraversal()
    FuzzGenerator.scanManifest(processMan).map(cmd =>{ cmd.toJson.toString()})
  }
}
