/**
 * Some instructions are perniticky enough that its simpler
 * to write them separately instead of smushing them with
 * all the rest. the invokedynamic instruction is one of them.
 * @author Eric Bodden
 */

package jas;



public class InvokedynamicInsn extends Insn implements RuntimeConstants
{
  public InvokedynamicInsn(CP cpe, int nargs)
  {
    opc = opc_invokeinterface;
    operand = new InvokeinterfaceOperand(cpe, nargs);
  }
}
