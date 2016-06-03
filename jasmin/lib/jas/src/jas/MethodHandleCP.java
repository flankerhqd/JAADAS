/**
 * Models a CONSTANT_MethodHandle entry
 *
 * @author Eric Bodden
 */

package jas;

import java.io.DataOutputStream;
import java.io.IOException;

public class MethodHandleCP extends CP implements RuntimeConstants
{
  public final static int STATIC_METHOD_KIND = 6;

  int kind;
  CP fieldOrMethod;

  /**
   * @param cname Class in which method exists
   * @param ownerName name of class owning the method or field
   * @param fieldOrMethodName name of field or method
   * @param sig Signature of field r method
   */
  public MethodHandleCP(int kind, String ownerName, String fieldOrMethodName, String sig)
  {
    uniq = kind + "$gfd\u00A4" + ownerName + "&%$91&" + fieldOrMethodName + "*(012$" + sig;
    if(kind<5) { //first for kinds refer to fields
      fieldOrMethod = new FieldCP(ownerName, fieldOrMethodName, sig);
    } else {
      fieldOrMethod = new MethodCP(ownerName, fieldOrMethodName, sig);
    }
    this.kind = kind;
  }

  void resolve(ClassEnv e)
  {
    e.addCPItem(fieldOrMethod);
  }

  void write(ClassEnv e, DataOutputStream out)
    throws IOException, jasError
  {
    out.writeByte(CONSTANT_METHOD_HANDLE);
    out.writeByte(kind);
    out.writeShort(e.getCPIndex(fieldOrMethod));
  }
}
