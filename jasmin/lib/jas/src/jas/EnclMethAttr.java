/**
 * EnclMeth attributes are embedded into class files
 * and used for further ???
 * @author $Author: Jennifer Lhotak$
 * @version $Revision: 1.1 $
 */

package jas;

import java.io.*;
import java.util.*;

public class EnclMethAttr {

    static CP attr = new AsciiCP("EnclosingMethod");
    ClassCP cls;
    NameTypeCP meth;

    void resolve(ClassEnv e){
        e.addCPItem(attr);
        e.addCPItem(cls);
        if (null != meth)
        	e.addCPItem(meth);
    }

    /**
    * Note: An enclosing method attr is associated with a <em>class</em>,
    * so you need to create a new EnclMethAttr for each anon or
    * local class you create
    */
    public EnclMethAttr(String a, String b, String c) { //
        cls = new ClassCP(a);
        if (!b.isEmpty() && !c.isEmpty())
        	meth = new NameTypeCP(b, c);
    }

    int size(){
        return 4;
    }


    void write(ClassEnv e, DataOutputStream out)
        throws IOException, jasError
        {
        
        out.writeShort(e.getCPIndex(attr));
        out.writeInt(4); // fixed length
        out.writeShort(e.getCPIndex(cls));
        out.writeShort(null == meth ? 0 : e.getCPIndex(meth));
    }
}
