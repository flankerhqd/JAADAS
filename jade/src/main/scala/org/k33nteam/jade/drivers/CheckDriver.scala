package org.k33nteam.jade.drivers

import java.io._
import java.util._

import heros.solver.IFDSSolver
import org.k33nteam.JadeCfg
import org.k33nteam.jade.bean.{VulnKind, VulnResult}
import org.k33nteam.jade.helpers.SyntaxHelpers._
import org.k33nteam.jade.propagation.base.NaiveAPIChecker
import org.k33nteam.jade.propagation.methodplugins._
import org.k33nteam.jade.propagation.track.{APIVulnManager, IFDSReachingConstantDefinitions}
import org.k33nteam.jade.solver.{CastCrashDetector, ImplicitIntentFinder, NPEDetector}
import org.k33nteam.jade.tools.FragmentInjectionDetector
import soot.jimple.InvokeExpr
import soot.jimple.infoflow.IInfoflow.CodeEliminationMode
import soot.jimple.infoflow.android.SetupApplication
import soot.jimple.infoflow.android.manifest.ProcessManifest
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.LayoutMatchingMode
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory.PathBuilder
import soot.jimple.infoflow.solver.cfg.{BackwardsInfoflowCFG, IInfoflowCFG}
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper
import soot.jimple.toolkits.callgraph.ReachableMethods
import soot.options.Options
import soot.util.queue.QueueReader
import soot.{Body, G, MethodOrMethodContext, PackManager, Scene, SootMethod}

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

class CheckDriver (apkPath: String, platformPath: String, cfgDir: String){

  var sourceSinkDataFlowIn:String = cfgDir + File.separator + "SourcesAndSinks.txt"
  var sourceSinkDataFlowOut:String = cfgDir + File.separator + "sourcesinks-dataout.txt"
  var reachabilityFile:String = cfgDir + File.separator + "direct.txt"
  var taintwrapperFile:String = cfgDir + File.separator + "EasyTaintWrapperSource.txt"
  var constantRuleFile:String = cfgDir + File.separator + "ConstantRules.groovy"

  JadeCfg.setCallback_file(cfgDir + File.separator + "AndroidCallbacks.txt")

  private def methodPlugins = Array(
    CustomVerifierPluginImpl.getPlugin,
    WebviewFileDomainPlugin,
    WebviewSSLPluginImpl.getPlugin,
    X509TrustManagerPluginImpl.getPlugin,
    CheckSignatureFakeID,
    WebviewSavePwdPlugin,
    CheckZipEntry
  )

  def getRegisteredPlugins: String= {
    methodPlugins.map(_.getDesc).mkString("\n")
  }
  def prepareMethodTraversal(): Unit= {
    Options.v.set_src_prec(Options.src_prec_apk)
    Options.v.set_output_format(Options.output_format_jimple)
    if(JadeCfg.isEnable_apklibs)
    {
      val pathArray = new ArrayList[String]
      pathArray.add(apkPath)

      val addtionalLibPath = new File(JadeCfg.APK_LIBPATH)
      if(addtionalLibPath.isDirectory)
      {
        pathArray.addAll(addtionalLibPath.listFiles.map(_.getAbsolutePath).toList)
      }
      Options.v.set_process_dir(pathArray)
      Options.v.set_force_android_jar(Scene.v.getAndroidJarPath(platformPath, apkPath))
    }
    else
    {
      Options.v.set_process_dir(Arrays.asList(apkPath))
      Options.v.set_android_jars(platformPath)
    }
    Options.v.set_allow_phantom_refs(true)
    Options.v.set_exclude(Arrays.asList("java", "sun", "wlc", "com.taobao.dp","dxoptimizer"))
    Scene.v.loadNecessaryClasses
    PackManager.v.runPacks
  }

  def entry(enableDataFlowOut: Boolean): Iterable[VulnResult]={
    G.reset
    prepareMethodTraversal

    var mid = doNaiveAPIScan ++ doAPIMethodScan ++ doCrashAnalysis ++ doManifestAnalysis ++ doImplicitScan
    implicit val icfg = prepareFatCfg
    mid = mid ++ doConstantAPIScan ++ doReachabilityAnalysis ++ doSensitiveInfoFlow
    if(enableDataFlowOut){
      System.gc
      System.gc
      mid = mid ++ doSensitiveInfoFlowOut //very slow!
    }
    mid
  }

  def fastentry: Iterable[VulnResult]={
    G.reset
    prepareMethodTraversal
    doNaiveAPIScan ++ doAPIMethodScan ++ doCrashAnalysis ++ doManifestAnalysis ++ doImplicitScan
  }

  private def doAPIMethodScan: Iterable[VulnResult] ={
    methodPlugins.map(p => p.getResult(Scene.v)).flatten.toIterable
  }

  private def doImplicitScan: Iterable[VulnResult] ={
    ImplicitIntentFinder.doAnalysis(Scene.v())
  }
  def prepareFatCfg: IInfoflowCFG={
    val app: SetupApplication = new SetupApplication(platformPath, apkPath)
    app.setStopAfterFirstFlow(false)
    app.setEnableImplicitFlows(true)
    app.setEnableStaticFieldTracking(true)
    app.setEnableCallbacks(false)
    app.setEnableExceptionTracking(false)
    app.setCodeEliminationMode(CodeEliminationMode.PropagateConstants)

    JadeCfg.setJcustom_clinit_enabled(true)
    JadeCfg.setcallbacks_enabled(false)
    JadeCfg.setfield_taint_propagate_enabled(false)//I don't care for constant api scan
    JadeCfg.setintent_special_wrapper_tacking_enabled(false) //don't care neither
    JadeCfg.setpublic_component_only_enabled(false)

    val easyTaintWrapper: EasyTaintWrapper = new EasyTaintWrapper(taintwrapperFile)
    app.setTaintWrapper(easyTaintWrapper)
    app.calculateSourcesSinksEntrypoints(sourceSinkDataFlowOut)
    System.out.println("Running data flow analysis...")
    app.generateInfoflowCFG
  }

  private def doNaiveAPIScan: Iterable[VulnResult]={
    val checker = new NaiveAPIChecker(constantRuleFile)
    checker.doCheck(Scene.v)
  }
  def doConstantAPIScan(implicit icfg: IInfoflowCFG): Iterable[VulnResult]={
    println("doing constant scan")
    val manager = new APIVulnManager(constantRuleFile)
    val problem = new IFDSReachingConstantDefinitions(new BackwardsInfoflowCFG(icfg), manager)
    val solver = new IFDSSolver(problem)
    solver.solve

    //getResult from manager

    val ret = manager.getAnalysisResult
    println(ret)
    ret
  }

  def doSensitiveInfoFlowOut: Iterable[VulnResult]={
    G.reset
    JadeCfg.setEnhance_callback_body(true)
    JadeCfg.setpublic_component_only_enabled(false)
    JadeCfg.setfield_taint_propagate_enabled(true)
    JadeCfg.setJcustom_clinit_enabled(false)
    val app = new SetupApplication(platformPath, apkPath)
    app.setStopAfterFirstFlow(false)
    app.setEnableImplicitFlows(false)
    app.setEnableStaticFieldTracking(true)
    app.setEnableCallbacks(true)
    app.setEnableCallbackSources(false)
    app.setEnableExceptionTracking(false)
    app.setAccessPathLength(3)
    app.setLayoutMatchingMode(LayoutMatchingMode.MatchSensitiveOnly)
    app.setFlowSensitiveAliasing(true)
    app.setPathBuilder(PathBuilder.ContextInsensitiveSourceFinder)
    app.setComputeResultPaths(true)

    val easyTaintWrapper: EasyTaintWrapper = new EasyTaintWrapper(taintwrapperFile)
    app.setTaintWrapper(easyTaintWrapper)
    app.calculateSourcesSinksEntrypoints(sourceSinkDataFlowOut)

    val infoflowresults = app.runInfoflow
    val ret = new ListBuffer[VulnResult]
    if(infoflowresults == null)
    {
      return ret.toList
    }
    val icfg = app.getIcfg
    infoflowresults.getResults.foreach {
      case (sinkinfo, sourceinfos) =>
        sourceinfos.foreach {
          case sourceinfo =>
            ret.append(VulnResult.toDataFlowOutVuln(sourceinfo.getSource,icfg.getMethodOf(sourceinfo.getSource), sinkinfo.getSink, icfg.getMethodOf(sinkinfo.getSink),"sensitive data flowout"))
        }
    }
    ret.toList
  }

  private def doSensitiveInfoFlow: Iterable[VulnResult]={
    G.reset
    JadeCfg.setEnhance_callback_body(false)
    JadeCfg.setpublic_component_only_enabled(true)
    JadeCfg.setintent_special_wrapper_tacking_enabled(true)
    val app = new SetupApplication(platformPath, apkPath)
    app.setStopAfterFirstFlow(false)
    app.setEnableImplicitFlows(false)
    app.setEnableStaticFieldTracking(false)
    app.setEnableExceptionTracking(false)
    app.setEnableCallbackSources(false)
    app.setEnableCallbacks(false)
    app.setAccessPathLength(3)
    app.setLayoutMatchingMode(LayoutMatchingMode.NoMatch)
    app.setFlowSensitiveAliasing(true)
    println("using contextinsentisivesourcefinder")
    app.setPathBuilder(PathBuilder.ContextInsensitive)
    app.setComputeResultPaths(true)
    app.setCodeEliminationMode(CodeEliminationMode.RemoveSideEffectFreeCode)

    val easyTaintWrapper: EasyTaintWrapper = new EasyTaintWrapper(taintwrapperFile)
    app.setTaintWrapper(easyTaintWrapper)
    app.calculateSourcesSinksEntrypoints(sourceSinkDataFlowIn)


    val ret = new ListBuffer[VulnResult]
    val infoflowresults = app.runInfoflow()
    if(infoflowresults == null)
    {
      return ret.toList
    }
    val icfg = app.getIcfg
    infoflowresults.getResults.foreach {
      case (sinkinfo, sourceinfos) =>
        sourceinfos.foreach {
          case sourceinfo =>
            ret.append(VulnResult.toDataFlowVuln(sourceinfo.getSource,icfg.getMethodOf(sourceinfo.getSource), sinkinfo.getSink, icfg.getMethodOf(sinkinfo.getSink),"sensitive data flow", sourceinfo.getPath.map(s => (s, icfg.getMethodOf(s))).toList))
        }
    }
    ret.toList
  }

  def doCrashAnalysis(): Iterable[VulnResult]={
    val processMan = new ProcessManifest(apkPath)
    val exportedComponents = processMan.getAccessibleComponents

    val ret = ListBuffer[VulnResult]()
    for(sootClass <- Scene.v.getApplicationClasses) {
      for (sootMethod <- sootClass.getMethods; if sootMethod.hasActiveBody) {
        //collect crashes only on exported components
        //if(sootMethod.getName.equals("jumpEvent"))
        if (exportedComponents.contains(sootClass.toString)) {
          ret ++= NPEDetector.checkPotentialNPE(sootMethod)(0).map(pi => VulnResult.toCrashVulnResult(pi.getO1, pi.getO2, VulnKind.NPE_CRASH))
          ret ++= NPEDetector.checkPotentialNPE(sootMethod)(1).map(pi => VulnResult.toCrashVulnResult(pi.getO1, pi.getO2, VulnKind.INDEXOUTOFBOUND_CRASH))
          ret ++= CastCrashDetector.checkCast(sootMethod.getActiveBody, checkCast = true)
        }

        //collect intent-vulns on all components
        val className = sootClass.toString
        if(!className.contains("android.support") && !className.contains("org.mozilla") && !className.contains("org.chromium"))
        {
          ret ++= CastCrashDetector.checkCast(sootMethod.getActiveBody, checkCast = false)
        }
      }
    }
    ret.toList
  }

  private def doReachabilityAnalysis(implicit icfg: IInfoflowCFG): Iterable[VulnResult]={
    val methods: ReachableMethods = Scene.v.getReachableMethods
    val reader: QueueReader[MethodOrMethodContext] = methods.listener
    val directReader: BufferedReader = new BufferedReader(new FileReader(new File(reachabilityFile)))
    val lists: Set[String] = new HashSet[String]
    val ret = new ListBuffer[VulnResult]()
    var string: String = directReader.readLine
    while (string != null) {
      lists.add(string.trim)
      string = directReader.readLine
    }
    while (reader.hasNext) {
      val method: SootMethod = reader.next.method
      if (method.hasActiveBody) {
        val body: Body = method.getActiveBody
        for (stmt <- body.getUnits) {
          if (stmt.containsInvokeExpr) {
            val expr: InvokeExpr = stmt.getInvokeExpr
            val relateMethod: SootMethod = expr.getMethod
            if (lists.contains(relateMethod.getSignature)) {
              System.out.println("find capability leak!")
              System.out.println(relateMethod.getSignature + " @ " + method)
              ret.append(VulnResult.toReachabilityVulnResult(stmt, method))
            }
          }
        }
      }
    }
    directReader.close
    ret.toList
  }

  def doManifestAnalysis() : Iterable[VulnResult]={
    val ret = new ListBuffer[VulnResult]()

    val processManifest = new ProcessManifest(apkPath)
    val targetSDK = processManifest.targetSdkVersion()
    if(targetSDK < 17 )//jelly bean
    {
      ret.append(VulnResult.toManifestConfigVuln("targetSDK level < 17, affect webview vulnerabilities"))
    }
    if(processManifest.isApplicationDebuggable())
    {
      ret.append(VulnResult.toManifestConfigVuln("application is debuggable"))
    }
    if(!processManifest.isApplicationDisallowBackup())
    {
      ret.append(VulnResult.toManifestConfigVuln("application doesn't disable backup"))
    }

    for(exportedComp <- processManifest.getExportedComponents)
    {
      if(FragmentInjectionDetector.checkComponent(exportedComp))
      {
        ret.append(VulnResult.toManifestConfigVuln(exportedComp + " FragmentInjection exist! (before API 17)"))
      }
    }
    ret
    //
  }
}
