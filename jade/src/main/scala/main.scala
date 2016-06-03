import org.k33nteam.JadeCfg
import org.k33nteam.jade.comm.CommTool
import org.k33nteam.jade.drivers.CheckDriver
import java.io.{FileInputStream, File}

import org.k33nteam.jade.tools.FuzzGenerator


case class Config(mode: String = "vulnanalysis", fastAnalysis: Boolean = false, enableFlowOut: Boolean = false, apkPath: String = "", androidlibPath: String = "", configDirPath: String = "", redis: String = "")

object main {
  def main(args: Array[String]) {

    val parser = new scopt.OptionParser[Config]("jade") {
      head("Joint Application Defect asseEsment - JADA", "1.0alpha, author flanker")

      //general options
      opt[String]('f', "apkFile") required() action { (s, c) =>
        c.copy(apkPath = s)
      } text ("specify the target apkpath")
      opt[String]('p', "androidlib") required() action { (s, c) =>
        c.copy(androidlibPath = s)
      } text ("specify the android platform path: e.g. /home/xxx/adt-bundles/sdk/platforms")

      cmd("vulnanalysis") action { (_, c) =>
        c.copy(mode = "vulnanalysis")
      } text ("do static vulnanalysis") children (
        opt[Unit]("fastanalysis") action { (_, c) =>
          c.copy(fastAnalysis = true)
        } text ("use --fastanalysis to do only fast checks, i.e. API vulnerabilities and Crash Analysis"),
          opt[Unit]("enableFlowOut") action { (_, c) =>
        c.copy(enableFlowOut = true)
      } text ("use --enableFlowOut to do password flowout check, but note this maybe slow and too much memory consumption"),
      opt[String]("redis") action { (s, c) =>
        c.copy(redis = s)
      } text ("store json result in redis"),
      opt[String]('c', "configDirPath") action { (s, c) =>
        c.copy(configDirPath = s)
      } text ("specify config dir path, should contains the following files: ConstantRules.groovy AndroidCallbacks.txt SourcesAndSinks.txt direct.txt")
      )

      cmd("fuzzgen") action { (_, c) =>
      c.copy(mode = "fuzzgen") } text("generate fuzz commands") children(

      )
    }

    parser.parse(args, Config()) match {
      case Some(config) =>
        config.mode match {
          case "vulnanalysis" => {
            val acd = new CheckDriver(config.apkPath, config.androidlibPath, config.configDirPath)

            println("********************************************")
            println("enabled plugins: " + acd.getRegisteredPlugins)
            println("********************************************")

            if(config.fastAnalysis)
            {
              println("********************************************")
              println("enabled modules: constapicheck, crash analysis")
              println("********************************************")
            }
            else
            {
              println("********************************************")
              print("enabled modules: constapicheck, crash analysis, reliableapicheck, reachability check, dataflow analysis, implicit finder")
              if(config.enableFlowOut) println(", sensitive data flowout analysis") else println()
              println("********************************************")
            }

            val vulns = if (config.fastAnalysis) acd.fastentry else acd.entry(enableDataFlowOut = config.enableFlowOut)

            if(config.redis != "")
              CommTool.putResult(config.redis,config.apkPath,vulns)
            else {
              CommTool.putResultIntoFile(config.apkPath, vulns)
              println()
              println("...............................")
              println("risk score: %f".format(CommTool.computeSumScore(vulns)))
              println("analysis finished, see results at output/%s.txt".format(org.apache.commons.codec.digest.DigestUtils.md5Hex(new FileInputStream(new File(config.apkPath)))))
            }

            //remove tmplibs

            try{
              val f = new File(JadeCfg.APK_LIBPATH)
              if(f.isDirectory)
              {
                f.listFiles().foreach(_.delete)
              }
            }
            catch{
              case e:Exception => e.printStackTrace()
            }
          }
        case "fuzzgen" => {
          FuzzGenerator.entry(config.apkPath, config.androidlibPath, config.configDirPath).foreach(println(_))
        }

        case _ => {
          println("Error: unknown mode")
        }
      }

      case None =>
        //bad arg
        println("Oops, bad args! Memeda doesn't like slipper")
    }
    //val acd = new CheckDriver("/tmp/TestWebview.apk", "/Users/hqdvista/android-sdks/platforms")
    //val acd = new CheckDriver("/media/DATA/TestWebview.apk", "/home/hqd/adt-bundle/sdk/platforms", "/home/hqd/Dropbox/keen/Jade-product/")
  }

}
