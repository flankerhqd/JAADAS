/**
 * Models a CONSTANT_InvokeDynamic entry
 *
 * @author Eric Bodden
 */

package jas;

import java.io.DataOutputStream;
import java.io.IOException;

public class InvokeDynamicCP extends CP implements RuntimeConstants
{
  MethodHandleCP bsm;
  NameTypeCP method;
  int bsmTableIndex;

  /**
   * @param bsmClassName name of class holding the boostrap method
   * @param bsmName name of bootstrap method
   * @param bsmSig Signature of bootstrap method
   * @param bsmName uninterpreted name of called method
   * @param bsmSig Signature of called method
   * @param bsmTableIndex index to the entry for the related bootstrap method in the BootstrapMethods attribute table
   */
  public InvokeDynamicCP(String bsmClassName, String bsmName, String bsmSig, String methodName, String methodSig, int bsmTableIndex)
  {
    this.bsmTableIndex = bsmTableIndex;
    uniq = (bsmClassName + "fv0\u00A4" + bsmName + "&%$91&" + bsmSig+ "*(012$" + methodName + "dfg8932" + methodSig).intern();
    bsm = new MethodHandleCP(
        MethodHandleCP.STATIC_METHOD_KIND, //bootstrap methods are always static methods 
        bsmClassName, 
        bsmName, 
        bsmSig);
    method = new NameTypeCP(methodName, methodSig);
  }

  void resolve(ClassEnv e)
  {
    e.addCPItem(bsm);
    e.addCPItem(method);
  }

  void write(ClassEnv e, DataOutputStream out)
    throws IOException, jasError
  {
    out.writeByte(CONSTANT_INVOKE_DYNAMIC);
    out.writeShort(bsmTableIndex);
    out.writeShort(e.getCPIndex(method));
  }
}
