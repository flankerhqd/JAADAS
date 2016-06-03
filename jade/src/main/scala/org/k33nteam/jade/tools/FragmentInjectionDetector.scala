package org.k33nteam.jade.tools


import soot.Scene

/**
 * Created by hqd on 4/13/15.
 */
object FragmentInjectionDetector {
  def checkComponent(clzName: String): Boolean =
  {
    try
    {
      Scene.v().getOrMakeFastHierarchy()
      val sootClass = Scene.v().getSootClass(clzName)
      val prefClass = Scene.v().getSootClass("android.preference.PreferenceActivity")

      Scene.v().getOrMakeFastHierarchy().isSubclass(sootClass, prefClass)
    }
    catch {
      case e: RuntimeException => {
        System.err.println("error when resolving class in fragmentInjectionDetector")
        e.printStackTrace()
        false
      }
      case _ => false
    }
  }
}
